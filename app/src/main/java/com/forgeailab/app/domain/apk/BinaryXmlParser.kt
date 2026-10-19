package com.forgeailab.app.domain.apk

import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryXmlParser {
    data class ManifestData(
        val packageName: String = "", val versionName: String = "", val versionCode: Long = 0,
        val minSdk: Int = 0, val targetSdk: Int = 0, val compileSdk: Int = 0, val appLabel: String = "",
        val permissions: List<String> = emptyList(),
        val activities: List<ComponentNode> = emptyList(),
        val services: List<ComponentNode> = emptyList(),
        val receivers: List<ComponentNode> = emptyList(),
        val providers: List<ComponentNode> = emptyList(),
        val features: List<String> = emptyList(), val libraries: List<String> = emptyList(),
        val rawText: String = ""
    )
    data class ComponentNode(val name: String, val exported: Boolean = false, val enabled: Boolean = true, val permission: String? = null)

    private class Builder {
        var packageName = ""; var versionName = ""; var versionCode = 0L
        var minSdk = 0; var targetSdk = 0; var compileSdk = 0; var appLabel = ""
        val permissions = mutableListOf<String>()
        val activities = mutableListOf<ComponentNode>()
        val services = mutableListOf<ComponentNode>()
        val receivers = mutableListOf<ComponentNode>()
        val providers = mutableListOf<ComponentNode>()
        val features = mutableListOf<String>(); val libraries = mutableListOf<String>()
        val rawText = StringBuilder()
        fun build() = ManifestData(packageName, versionName, versionCode, minSdk, targetSdk, compileSdk, appLabel,
            permissions.toList(), activities.toList(), services.toList(), receivers.toList(), providers.toList(),
            features.toList(), libraries.toList(), rawText.toString())
    }

    fun parse(bytes: ByteArray): ManifestData = try { parseInternal(bytes) } catch (e: Exception) {
        ManifestData(rawText = "parse error: ${e.message}")
    }

    private fun parseInternal(bytes: ByteArray): ManifestData {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (buf.remaining() < 8) return ManifestData(rawText = "too short")
        val magic = buf.int
        if (magic != 0x00080003) {
            val text = String(bytes, Charsets.UTF_8)
            if (text.trimStart().startsWith("<?xml") || text.trimStart().startsWith("<manifest")) return parsePlainXml(text)
            return ManifestData(rawText = "unknown format")
        }
        buf.int
        val stringPool = readStringPool(buf) ?: return ManifestData(rawText = "no string pool")
        val b = Builder()
        while (buf.remaining() >= 8) {
            val chunkType = buf.short.toInt() and 0xFFFF
            buf.short; val chunkSize = buf.int
            if (chunkSize < 8 || chunkSize - 8 > buf.remaining()) break
            when (chunkType) {
                0x0102 -> {
                    buf.int; buf.int; buf.int
                    val nameIdx = buf.int
                    buf.short; buf.short
                    val attrCount = buf.short.toInt() and 0xFFFF
                    buf.short; buf.short; buf.short
                    val tagName = stringPool.getOrNull(nameIdx) ?: "unknown"
                    val attrs = mutableMapOf<String, String>()
                    repeat(attrCount) {
                        buf.int; val attrNameIdx = buf.int; val attrRaw = buf.int; val attrType = buf.int; val attrData = buf.int
                        val attrName = stringPool.getOrNull(attrNameIdx) ?: return@repeat
                        attrs[attrName] = resolve(attrType, attrData, attrRaw, stringPool)
                    }
                    b.rawText.append("<$tagName>\n")
                    applyTag(b, tagName, attrs)
                }
                0x0103 -> { buf.int; buf.int; buf.int; buf.int }
                else -> buf.position(buf.position() + chunkSize - 8)
            }
        }
        return b.build()
    }

    private fun applyTag(b: Builder, tag: String, attrs: Map<String, String>) {
        when (tag) {
            "manifest" -> { attrs["package"]?.let { b.packageName = it }; attrs["versionName"]?.let { b.versionName = it }; attrs["versionCode"]?.toLongOrNull()?.let { b.versionCode = it } }
            "uses-sdk" -> { attrs["minSdkVersion"]?.toIntOrNull()?.let { b.minSdk = it }; attrs["targetSdkVersion"]?.toIntOrNull()?.let { b.targetSdk = it } }
            "uses-permission", "uses-permission-sdk-23" -> attrs["name"]?.takeIf { it.isNotBlank() }?.let { b.permissions.add(it) }
            "activity", "activity-alias" -> attrs["name"]?.let { b.activities.add(ComponentNode(it, attrs["exported"].equals("true", true), attrs["enabled"]?.equals("false", true) != true, attrs["permission"])) }
            "service" -> attrs["name"]?.let { b.services.add(ComponentNode(it, attrs["exported"].equals("true", true), attrs["enabled"]?.equals("false", true) != true, attrs["permission"])) }
            "receiver" -> attrs["name"]?.let { b.receivers.add(ComponentNode(it, attrs["exported"].equals("true", true), attrs["enabled"]?.equals("false", true) != true, attrs["permission"])) }
            "provider" -> attrs["name"]?.let { b.providers.add(ComponentNode(it, attrs["exported"].equals("true", true), attrs["enabled"]?.equals("false", true) != true, attrs["permission"])) }
            "uses-feature" -> attrs["name"]?.let { b.features.add(it) }
            "uses-library" -> attrs["name"]?.let { b.libraries.add(it) }
            "application" -> attrs["label"]?.takeIf { !it.startsWith("@") }?.let { b.appLabel = it }
        }
    }

    private fun resolve(typeWord: Int, data: Int, raw: Int, pool: List<String>): String {
        val type = (typeWord ushr 24) and 0xFF
        return when (type) {
            0x03 -> pool.getOrNull(data) ?: pool.getOrNull(raw) ?: ""
            0x10 -> data.toString()
            0x12 -> if (data != 0) "true" else "false"
            else -> pool.getOrNull(raw)?.takeIf { it.isNotBlank() } ?: data.toString()
        }
    }

    private fun readStringPool(buf: ByteBuffer): List<String>? {
        val start = buf.position()
        if ((buf.short.toInt() and 0xFFFF) != 0x0001) { buf.position(start); return null }
        buf.short; val chunkSize = buf.int; val stringCount = buf.int; val styleCount = buf.int
        val flags = buf.int; val stringsStart = buf.int; buf.int
        val isUtf8 = (flags and (1 shl 8)) != 0
        val offsets = IntArray(stringCount) { buf.int }
        if (styleCount > 0) repeat(styleCount) { buf.int }
        val poolStart = start + stringsStart
        val strings = mutableListOf<String>()
        for (i in 0 until stringCount) {
            val off = poolStart + offsets[i]
            if (off < 0 || off >= buf.limit()) { strings.add(""); continue }
            buf.position(off)
            strings.add(if (isUtf8) readUtf8(buf) else readUtf16(buf))
        }
        buf.position(start + chunkSize)
        return strings
    }

    private fun readUtf16(buf: ByteBuffer): String {
        var len = buf.short.toInt() and 0xFFFF
        if (len and 0x8000 != 0) { val high = len and 0x7FFF; val low = buf.short.toInt() and 0xFFFF; len = (high shl 16) or low }
        val chars = CharArray(len.coerceAtMost(buf.remaining() / 2))
        for (i in chars.indices) chars[i] = buf.short.toInt().toChar()
        if (buf.remaining() >= 2) buf.short
        return String(chars)
    }

    private fun readUtf8(buf: ByteBuffer): String {
        var u16 = buf.get().toInt() and 0xFF
        if (u16 and 0x80 != 0) u16 = ((u16 and 0x7F) shl 8) or (buf.get().toInt() and 0xFF)
        var byteLen = buf.get().toInt() and 0xFF
        if (byteLen and 0x80 != 0) byteLen = ((byteLen and 0x7F) shl 8) or (buf.get().toInt() and 0xFF)
        val bytes = ByteArray(byteLen.coerceAtMost(buf.remaining())); buf.get(bytes)
        if (buf.remaining() > 0) buf.get()
        return String(bytes, Charsets.UTF_8)
    }

    private fun parsePlainXml(text: String): ManifestData {
        val b = Builder(); b.rawText.append(text)
        Regex("""package\\s*=\\s*\"([^\"]+)\"""").find(text)?.groupValues?.get(1)?.let { b.packageName = it }
        Regex("""android:versionName\\s*=\\s*\"([^\"]+)\"""").find(text)?.groupValues?.get(1)?.let { b.versionName = it }
        Regex("""android:versionCode\\s*=\\s*\"([^\"]+)\"""").find(text)?.groupValues?.get(1)?.toLongOrNull()?.let { b.versionCode = it }
        Regex("""android:minSdkVersion\\s*=\\s*\"([^\"]+)\"""").find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { b.minSdk = it }
        Regex("""android:targetSdkVersion\\s*=\\s*\"([^\"]+)\"""").find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { b.targetSdk = it }
        Regex("""<uses-permission[^>]*android:name\\s*=\\s*\"([^\"]+)\"""").findAll(text).forEach { b.permissions.add(it.groupValues[1]) }
        return b.build()
    }
}
