package com.forgeailab.app.domain.apk

import com.forgeailab.app.data.model.ExtractedString
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DexStringExtractor {

    fun extract(stream: InputStream, sourceName: String, maxStrings: Int = 500): List<ExtractedString> {
        return extractFromBytes(stream.readBytes(), sourceName, maxStrings)
    }

    fun extractFromBytes(bytes: ByteArray, sourceName: String, maxStrings: Int = 500): List<ExtractedString> {
        if (bytes.size < 112) return emptyList()
        if (bytes[0] != 'd'.code.toByte() || bytes[1] != 'e'.code.toByte() || bytes[2] != 'x'.code.toByte()) {
            return emptyList()
        }
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.position(56)
        val stringIdsSize = buf.int
        val stringIdsOff = buf.int
        if (stringIdsSize <= 0 || stringIdsOff <= 0 || stringIdsOff >= bytes.size) return emptyList()
        val results = mutableListOf<ExtractedString>()
        val limit = minOf(stringIdsSize, maxStrings * 3)
        for (i in 0 until limit) {
            val idPos = stringIdsOff + i * 4
            if (idPos + 4 > bytes.size) break
            buf.position(idPos)
            val stringDataOff = buf.int
            if (stringDataOff <= 0 || stringDataOff >= bytes.size) continue
            val str = readMutf8(bytes, stringDataOff) ?: continue
            if (!isInteresting(str)) continue
            results.add(ExtractedString(str, sourceName, looksLikeUi(str)))
            if (results.size >= maxStrings) break
        }
        return results
    }

    private fun readMutf8(data: ByteArray, offset: Int): String? {
        return try {
            var pos = offset
            var len = 0
            var shift = 0
            while (pos < data.size) {
                val b = data[pos].toInt() and 0xFF
                pos++
                len = len or ((b and 0x7F) shl shift)
                if (b and 0x80 == 0) break
                shift += 7
                if (shift > 28) return null
            }
            if (len <= 0 || len > 10000 || pos + len > data.size) return null
            String(data.copyOfRange(pos, pos + len), Charsets.UTF_8).trim { it <= ' ' || it == '\u0000' }
        } catch (_: Exception) { null }
    }

    private fun isInteresting(s: String): Boolean {
        if (s.length < 3 || s.length > 500) return false
        if (s.all { it.isDigit() || it == '.' || it == '-' }) return false
        val printable = s.count { it.code in 32..126 || it.code > 160 }
        return printable.toFloat() / s.length > 0.7f
    }

    private fun looksLikeUi(s: String): Boolean {
        if (s.length in 2..80 && s.any { it.isLetter() }) {
            if (s.any { it.code in 0x0600..0x06FF }) return true
            val lower = s.lowercase()
            return listOf("error", "login", "password", "settings", "cancel", "save", "delete").any { lower.contains(it) }
        }
        return false
    }
}
