package com.forgeailab.app.domain.engine

import android.content.Context
import com.forgeailab.app.data.model.SignResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.jar.JarFile

class SignManager(private val context: Context) {

    private val keyDir = File(context.filesDir, "keys").also { it.mkdirs() }

    suspend fun inspectSignature(apk: File): SignResult = withContext(Dispatchers.IO) {
        if (!apk.exists()) return@withContext SignResult(false, message = "فایل یافت نشد")
        try {
            JarFile(apk, true).use { jar ->
                val entries = jar.entries()
                var cert: X509Certificate? = null
                while (entries.hasMoreElements()) {
                    val e = entries.nextElement()
                    jar.getInputStream(e).use { it.readBytes() }
                    val certs = e.certificates
                    if (certs != null && certs.isNotEmpty()) {
                        cert = certs[0] as? X509Certificate
                        break
                    }
                }
                if (cert == null) {
                    SignResult(false, message = "امضایی یافت نشد")
                } else {
                    val sha256 = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
                        .joinToString(":") { "%02X".format(it) }
                    val info = buildString {
                        appendLine("Subject: ${cert.subjectX500Principal.name}")
                        appendLine("Issuer: ${cert.issuerX500Principal.name}")
                        appendLine("SHA-256: $sha256")
                    }
                    SignResult(true, certificateInfo = info, message = "امضا خوانده شد", outputPath = apk.absolutePath)
                }
            }
        } catch (e: Exception) {
            SignResult(false, message = "خطا: ${e.message}")
        }
    }

    fun signingInstructions(): String = """
        برای امضا از apksigner استفاده کنید:
        apksigner sign --ks forge-release.jks --out signed.apk unsigned.apk
        کلید خصوصی را در Git قرار ندهید.
    """.trimIndent()
}
