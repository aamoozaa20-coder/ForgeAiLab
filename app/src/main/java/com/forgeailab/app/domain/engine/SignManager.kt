package com.forgeailab.app.domain.engine

import android.content.Context
import android.util.Base64
import com.android.apksig.ApkSigner
import com.forgeailab.app.data.model.SignResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.jar.JarFile

/**
 * On-device signing of rebuilt APKs so they can be installed.
 * Uses an embedded FORGE-debug PKCS12 keystore.
 * Because the signature differs from the original app, the user must uninstall the original package first.
 */
class SignManager(private val context: Context) {

    private val keyDir = File(context.filesDir, "keys").also { it.mkdirs() }
    private val keystoreFile = File(keyDir, "forge-debug.p12")
    private val alias = "forge"
    private val storePass = "forge1234".toCharArray()
    private val keyPass = "forge1234".toCharArray()

    // PKCS12: alias=forge, password=forge1234, CN=FORGE-Debug (debug only)
    private val embeddedPkcs12B64 =
        "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"

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
                    SignResult(false, message = "امضایی یافت نشد (unsigned)")
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

    suspend fun signApk(
        input: File,
        output: File = File(input.parentFile, input.nameWithoutExtension + "-signed.apk")
    ): SignResult = withContext(Dispatchers.IO) {
        try {
            ensureKeystore()
            val ks = KeyStore.getInstance("PKCS12")
            keystoreFile.inputStream().use { ks.load(it, storePass) }
            val privateKey = ks.getKey(alias, keyPass) as PrivateKey
            val cert = ks.getCertificate(alias) as X509Certificate

            val config = ApkSigner.SignerConfig.Builder("FORGE", privateKey, listOf(cert)).build()
            ApkSigner.Builder(listOf(config))
                .setInputApk(input)
                .setOutputApk(output)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .setV3SigningEnabled(true)
                .build()
                .sign()

            SignResult(
                success = true,
                message = "امضا شد. قبل از نصب، نسخه قبلی همان برنامه را حذف کنید.",
                outputPath = output.absolutePath,
                certificateInfo = cert.subjectX500Principal.name
            )
        } catch (e: Exception) {
            SignResult(false, message = "خطای امضا: ${e.message}")
        }
    }

    private fun ensureKeystore() {
        if (keystoreFile.exists() && keystoreFile.length() > 100) return
        val bytes = Base64.decode(embeddedPkcs12B64, Base64.DEFAULT)
        keystoreFile.writeBytes(bytes)
        // verify load
        val ks = KeyStore.getInstance("PKCS12")
        ByteArrayInputStream(bytes).use { ks.load(it, storePass) }
        require(ks.containsAlias(alias)) { "keystore alias missing" }
    }

    fun signingInstructions(): String = """
        APK اصلاح‌شده با کلید FORGE-debug امضا می‌شود.
        چون امضا با نسخه اصلی فرق دارد، ابتدا برنامه اصلی را حذف کنید،
        بعد «نصب نسخه اصلاح‌شده» را بزنید.
    """.trimIndent()
}
