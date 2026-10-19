package com.forgeailab.app.domain.engine

import com.forgeailab.app.data.model.RebuildResult
import com.forgeailab.app.data.model.RebuildStep
import com.forgeailab.app.data.model.StepStatus
import com.forgeailab.app.domain.workspace.WorkspaceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

class RebuildEngine(
    private val workspace: WorkspaceManager,
    private val signManager: SignManager? = null
) {

    suspend fun rebuild(
        sign: Boolean = true,
        onStep: (RebuildStep) -> Unit = {}
    ): RebuildResult = withContext(Dispatchers.IO) {
        val steps = mutableListOf<RebuildStep>()
        val errors = mutableListOf<String>()
        val project = workspace.current.value
            ?: return@withContext RebuildResult(false, errors = listOf("هیچ پروژه‌ای باز نیست"))

        suspend fun runStep(name: String, block: suspend () -> Unit): Boolean {
            val start = System.currentTimeMillis()
            onStep(RebuildStep(name, StepStatus.RUNNING))
            return try {
                block()
                val step = RebuildStep(name, StepStatus.SUCCESS, durationMs = System.currentTimeMillis() - start)
                steps.add(step); onStep(step); true
            } catch (e: Exception) {
                val step = RebuildStep(name, StepStatus.FAILED, e.message ?: "خطا", System.currentTimeMillis() - start)
                steps.add(step); errors.add("$name: ${e.message}"); onStep(step); false
            }
        }

        if (!runStep("بررسی فایل ورودی") {
            val original = File(project.originalApkPath)
            require(original.exists()) { "فایل اصلی موجود نیست" }
            ZipFile(original).use { z -> require(z.getEntry("AndroidManifest.xml") != null) }
        }) return@withContext RebuildResult(false, steps = steps, errors = errors)

        if (!runStep("آماده‌سازی فضای کاری") {
            require(File(project.workDir, "extracted").isDirectory) { "پوشه extracted نیست" }
        }) return@withContext RebuildResult(false, steps = steps, errors = errors)

        var output: File? = null
        if (!runStep("بسته‌بندی مجدد") {
            output = workspace.packageWorkingCopy("rebuild_${System.currentTimeMillis()}.apk")
            require(output != null && output!!.exists()) { "خروجی ساخته نشد" }
        }) return@withContext RebuildResult(false, steps = steps, errors = errors)

        if (!runStep("بررسی خروجی") {
            ZipFile(output!!).use { z ->
                require(z.entries().asSequence().any { it.name.equals("AndroidManifest.xml", true) })
            }
        }) return@withContext RebuildResult(false, outputPath = output?.absolutePath, steps = steps, errors = errors)

        var signed = false
        var finalPath = output!!.absolutePath
        if (sign && signManager != null) {
            if (!runStep("امضای APK") {
                val signedFile = File(output!!.parentFile, output!!.nameWithoutExtension + "-signed.apk")
                val sr = signManager.signApk(output!!, signedFile)
                require(sr.success && sr.outputPath != null) { sr.message }
                finalPath = sr.outputPath!!
                signed = true
            }) {
                // signing failed but keep unsigned path
            }
        }

        RebuildResult(
            success = errors.isEmpty() || signed || output != null,
            outputPath = finalPath,
            steps = steps,
            errors = errors,
            signed = signed
        )
    }
}
