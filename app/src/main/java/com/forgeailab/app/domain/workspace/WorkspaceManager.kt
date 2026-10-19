package com.forgeailab.app.domain.workspace

import android.content.Context
import com.forgeailab.app.data.model.*
import com.forgeailab.app.domain.apk.ApkAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class WorkspaceManager(private val context: Context) {
    private val analyzer = ApkAnalyzer()
    private val rootDir = File(context.filesDir, "workspaces").also { it.mkdirs() }
    private val _current = MutableStateFlow<WorkspaceProject?>(null)
    val current: StateFlow<WorkspaceProject?> = _current.asStateFlow()
    private val _projects = MutableStateFlow<List<WorkspaceProject>>(emptyList())
    val projects: StateFlow<List<WorkspaceProject>> = _projects.asStateFlow()

    init { refreshProjectList() }

    suspend fun importApk(source: File, onProgress: (String, Float) -> Unit = { _, _ -> }): WorkspaceProject =
        withContext(Dispatchers.IO) {
            onProgress("تحلیل", 0.1f)
            val info = analyzer.analyze(source, onProgress)
            val id = UUID.randomUUID().toString().take(8)
            val workDir = File(rootDir, id).also { it.mkdirs() }
            val originalCopy = File(workDir, "original.apk")
            source.copyTo(originalCopy, overwrite = true)
            val extracted = File(workDir, "extracted").also { it.mkdirs() }
            onProgress("استخراج", 0.7f)
            extractZip(originalCopy, extracted)
            val project = WorkspaceProject(id, info.appLabel.ifBlank { info.packageName.ifBlank { source.name } },
                originalCopy.absolutePath, workDir.absolutePath, info, System.currentTimeMillis(), System.currentTimeMillis())
            File(workDir, "meta.json").writeText("id=$id\nname=${project.name}\n")
            _current.value = project; refreshProjectList(); onProgress("آماده", 1f); project
        }

    fun openProject(id: String): WorkspaceProject? {
        val dir = File(rootDir, id); if (!dir.exists()) return null
        val meta = File(dir, "meta.json")
        val name = if (meta.exists()) meta.readText().lines().find { it.startsWith("name=") }?.substringAfter("=") ?: id else id
        val project = WorkspaceProject(id, name, File(dir, "original.apk").absolutePath, dir.absolutePath, null, meta.lastModified(), System.currentTimeMillis())
        _current.value = project; return project
    }

    fun listFiles(relativePath: String = ""): List<FileNode> {
        val project = _current.value ?: return emptyList()
        val base = File(File(project.workDir, "extracted"), relativePath)
        if (!base.exists()) return emptyList()
        return base.listFiles()?.map { f ->
            FileNode(f.name, if (relativePath.isEmpty()) f.name else "$relativePath/${f.name}", f.isDirectory, if (f.isFile) f.length() else 0)
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
    }

    fun readTextFile(relativePath: String, maxBytes: Int = 512000): String? {
        val project = _current.value ?: return null
        val file = File(File(project.workDir, "extracted"), relativePath)
        if (!file.exists() || !file.isFile) return null
        if (file.length() > maxBytes) return "file too large"
        return try { file.readText(Charsets.UTF_8) } catch (_: Exception) { null }
    }

    fun search(query: String, maxResults: Int = 100): List<FileNode> {
        val project = _current.value ?: return emptyList()
        val root = File(project.workDir, "extracted"); val q = query.lowercase()
        val results = mutableListOf<FileNode>()
        root.walkTopDown().maxDepth(12).forEach { f ->
            if (results.size >= maxResults) return@forEach
            if (f.isFile && f.name.lowercase().contains(q))
                results.add(FileNode(f.name, f.relativeTo(root).path, false, f.length()))
        }
        return results
    }

    fun findSplashResources(): List<FileNode> {
        val project = _current.value ?: return emptyList()
        val root = File(project.workDir, "extracted")
        val keywords = listOf("splash", "launch", "welcome", "boot", "ic_launcher")
        return root.walkTopDown().maxDepth(10).filter { f ->
            f.isFile && keywords.any { (f.name + f.path).lowercase().contains(it) }
        }.map { FileNode(it.name, it.relativeTo(root).path, false, it.length()) }.toList()
    }

    suspend fun replaceFile(relativePath: String, newBytes: ByteArray, description: String): Boolean =
        withContext(Dispatchers.IO) {
            val project = _current.value ?: return@withContext false
            val file = File(File(project.workDir, "extracted"), relativePath)
            file.parentFile?.mkdirs(); file.writeBytes(newBytes); true
        }

    suspend fun writeTextFile(relativePath: String, content: String, description: String): Boolean =
        replaceFile(relativePath, content.toByteArray(Charsets.UTF_8), description)

    suspend fun packageWorkingCopy(outputName: String = "modified.apk"): File? = withContext(Dispatchers.IO) {
        val project = _current.value ?: return@withContext null
        val extracted = File(project.workDir, "extracted"); val out = File(project.workDir, outputName)
        ZipOutputStream(out.outputStream()).use { zos ->
            extracted.walkTopDown().filter { it.isFile }.forEach { file ->
                zos.putNextEntry(ZipEntry(file.relativeTo(extracted).path.replace('\\', '/')))
                file.inputStream().use { it.copyTo(zos) }; zos.closeEntry()
            }
        }
        out
    }

    private fun extractZip(zipFile: File, dest: File) {
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val out = File(dest, entry.name)
                if (entry.isDirectory) out.mkdirs()
                else {
                    out.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input -> out.outputStream().use { output -> input.copyTo(output) } }
                }
            }
        }
    }

    private fun refreshProjectList() {
        _projects.value = rootDir.listFiles()?.filter { it.isDirectory }?.mapNotNull { dir ->
            val meta = File(dir, "meta.json"); if (!meta.exists()) return@mapNotNull null
            WorkspaceProject(dir.name, meta.readText().lines().find { it.startsWith("name=") }?.substringAfter("=") ?: dir.name,
                File(dir, "original.apk").absolutePath, dir.absolutePath, null, meta.lastModified(), dir.lastModified())
        }?.sortedByDescending { it.updatedAt } ?: emptyList()
    }

    data class FileNode(val name: String, val path: String, val isDirectory: Boolean, val size: Long)
}
