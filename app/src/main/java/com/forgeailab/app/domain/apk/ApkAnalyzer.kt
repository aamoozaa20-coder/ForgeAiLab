package com.forgeailab.app.domain.apk

import com.forgeailab.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

class ApkAnalyzer {
    private val manifestParser = BinaryXmlParser()
    private val sdkDetector = SdkDetector()
    private val stringExtractor = DexStringExtractor()

    suspend fun analyze(file: File, onProgress: (String, Float) -> Unit = { _, _ -> }): ApkInfo =
        withContext(Dispatchers.IO) {
            require(file.exists() && file.canRead()) { "APK unreadable" }
            onProgress("hash", 0.1f)
            val md5 = hash(file, "MD5")
            val sha1 = hash(file, "SHA-1")
            val sha256 = hash(file, "SHA-256")
            onProgress("zip", 0.2f)
            ZipFile(file).use { zip ->
                val entries = zip.entries().toList()
                onProgress("manifest", 0.3f)
                val manifestBytes = entries.find { it.name.equals("AndroidManifest.xml", true) }
                    ?.let { zip.getInputStream(it).readBytes() }
                val manifest = if (manifestBytes != null) manifestParser.parse(manifestBytes) else BinaryXmlParser.ManifestData()
                val resources = mutableListOf<ResourceEntry>()
                val assets = mutableListOf<AssetEntry>()
                val nativeLibs = mutableListOf<NativeLibInfo>()
                val dexFiles = mutableListOf<DexInfo>()
                for (entry in entries) {
                    if (entry.isDirectory) continue
                    val name = entry.name
                    val size = entry.size.coerceAtLeast(0)
                    when {
                        name.startsWith("res/") -> resources.add(ResourceEntry(name, classify(name), size, null))
                        name.startsWith("assets/") -> assets.add(AssetEntry(name, size))
                        name.startsWith("lib/") && name.endsWith(".so") -> {
                            val abi = name.split("/").getOrNull(1) ?: "unknown"
                            nativeLibs.add(NativeLibInfo(name, abi, size))
                        }
                        name == "classes.dex" || (name.startsWith("classes") && name.endsWith(".dex")) ->
                            dexFiles.add(DexInfo(name, size))
                    }
                }
                onProgress("dex", 0.6f)
                val strings = mutableListOf<ExtractedString>()
                for (dex in dexFiles) {
                    val e = zip.getEntry(dex.name) ?: continue
                    try {
                        zip.getInputStream(e).use { strings.addAll(stringExtractor.extract(it, dex.name, 500)) }
                    } catch (_: Exception) {
                    }
                }
                onProgress("sdk", 0.8f)
                val knownSdks = sdkDetector.detect(
                    manifest.packageName,
                    manifest.permissions,
                    (manifest.activities + manifest.services + manifest.receivers + manifest.providers).map { it.name },
                    strings.map { it.value },
                    resources.map { it.path },
                    nativeLibs.map { it.path }
                )
                ApkInfo(
                    packageName = manifest.packageName,
                    versionName = manifest.versionName,
                    versionCode = manifest.versionCode,
                    minSdk = manifest.minSdk,
                    targetSdk = manifest.targetSdk,
                    compileSdk = manifest.compileSdk,
                    appLabel = manifest.appLabel.ifBlank { file.nameWithoutExtension },
                    fileSize = file.length(),
                    filePath = file.absolutePath,
                    fileName = file.name,
                    md5 = md5,
                    sha1 = sha1,
                    sha256 = sha256,
                    permissions = manifest.permissions.distinct().sorted(),
                    activities = manifest.activities.map {
                        ComponentInfo(it.name, it.exported, it.enabled, permission = it.permission)
                    },
                    services = manifest.services.map {
                        ComponentInfo(it.name, it.exported, it.enabled, permission = it.permission)
                    },
                    receivers = manifest.receivers.map {
                        ComponentInfo(it.name, it.exported, it.enabled, permission = it.permission)
                    },
                    providers = manifest.providers.map {
                        ComponentInfo(it.name, it.exported, it.enabled, permission = it.permission)
                    },
                    features = manifest.features.distinct(),
                    libraries = manifest.libraries.distinct(),
                    nativeLibs = nativeLibs.sortedBy { it.path },
                    dexFiles = dexFiles,
                    resources = resources.sortedBy { it.path },
                    assets = assets.sortedBy { it.path },
                    knownSdks = knownSdks,
                    strings = strings.distinctBy { it.value }.take(500),
                    manifestRaw = manifest.rawText
                )
            }
        }

    fun compare(a: ApkInfo, b: ApkInfo): CompareResult {
        val permAdded = b.permissions - a.permissions.toSet()
        val permRemoved = a.permissions - b.permissions.toSet()
        return CompareResult(
            a.packageName == b.packageName,
            "${a.versionName} -> ${b.versionName}",
            permAdded,
            permRemoved,
            emptyList(),
            b.fileSize - a.fileSize,
            a.sha256 == b.sha256,
            "compare ${a.fileName} vs ${b.fileName}"
        )
    }

    fun generateReport(info: ApkInfo): AnalysisReport {
        val json =
            """{"packageName":"${info.packageName}","versionName":"${info.versionName}","versionCode":${info.versionCode},"minSdk":${info.minSdk},"targetSdk":${info.targetSdk},"permissions":${info.permissions.size},"sha256":"${info.sha256}"}"""
        val html =
            "<html dir=rtl><body><h1>${info.packageName}</h1><p>v${info.versionName}</p></body></html>"
        return AnalysisReport(System.currentTimeMillis(), info, html, json)
    }

    private fun classify(path: String): ResourceType {
        val p = path.lowercase()
        return when {
            "drawable" in p -> ResourceType.DRAWABLE
            "mipmap" in p -> ResourceType.MIPMAP
            "layout" in p -> ResourceType.LAYOUT
            "values" in p -> ResourceType.VALUES
            p.endsWith(".xml") -> ResourceType.XML
            else -> ResourceType.OTHER
        }
    }

    private fun hash(file: File, algo: String): String {
        val md = MessageDigest.getInstance(algo)
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            var n: Int
            while (input.read(buf).also { n = it } > 0) md.update(buf, 0, n)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
