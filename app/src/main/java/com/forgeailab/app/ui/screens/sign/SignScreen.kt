package com.forgeailab.app.ui.screens.sign

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgeailab.app.ForgeApp
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SignScreen() {
    val project by ForgeApp.instance.workspace.current.collectAsStateWithLifecycle()
    var certInfo by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var rebuiltPath by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val instructions = remember { ForgeApp.instance.signManager.signingInstructions() }

    LaunchedEffect(project) {
        val dir = project?.workDir?.let { File(it) } ?: return@LaunchedEffect
        rebuiltPath = dir.listFiles()
            ?.filter { it.extension == "apk" && (it.name.contains("signed") || it.name.startsWith("rebuild_")) }
            ?.maxByOrNull { it.lastModified() }
            ?.absolutePath
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("بازسازی، امضا و نصب", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        if (project?.apkInfo != null) {
            Text(
                "پروژه: ${project!!.apkInfo!!.packageName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text("ابتدا از صفحه تحلیل یک برنامه انتخاب کنید.", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                scope.launch {
                    busy = true
                    message = "در حال بازسازی و امضا…"
                    val result = ForgeApp.instance.rebuildEngine.rebuild(sign = true)
                    if (result.outputPath != null) {
                        rebuiltPath = result.outputPath
                        message = buildString {
                            append(if (result.signed) "آماده نصب (امضا شده).\n" else "ساخته شد.\n")
                            append(result.outputPath)
                            if (result.errors.isNotEmpty()) append("\n" + result.errors.joinToString())
                            append("\n\nقبل از نصب، برنامه اصلی را حذف کنید.")
                        }
                    } else {
                        message = "ناموفق: " + result.errors.joinToString()
                    }
                    busy = false
                }
            },
            enabled = project != null && !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "صبر کنید…" else "۱) بازسازی + امضا") }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val path = rebuiltPath ?: return@Button
                val r = ForgeApp.instance.installer.install(File(path))
                message = if (r.isSuccess)
                    "پنجره نصب باز شد. اگر خطای امضا دیدید، اول برنامه اصلی را Uninstall کنید."
                else
                    "خطا: ${r.exceptionOrNull()?.message}"
            },
            enabled = rebuiltPath != null && !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("۲) نصب نسخه اصلاح‌شده") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val path = project?.originalApkPath ?: return@OutlinedButton
                scope.launch {
                    busy = true
                    val result = ForgeApp.instance.signManager.inspectSignature(File(path))
                    certInfo = result.certificateInfo
                    message = result.message
                    busy = false
                }
            },
            enabled = project != null && !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("بررسی امضای APK اصلی") }

        if (rebuiltPath != null) {
            Spacer(Modifier.height(8.dp))
            Text("فایل خروجی:\n$rebuiltPath", style = MaterialTheme.typography.bodySmall)
        }
        if (message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Text(message, Modifier.padding(12.dp))
            }
        }
        if (certInfo.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth()) {
                Text(certInfo, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("راهنما", style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth()) {
            Text(
                """مسیر کامل:
1) تحلیل → انتخاب برنامه نصب‌شده
2) منابع → دیدن فایل‌های استخراج‌شده
3) دستیار AI → مثلاً:
   جایگزینی متن Settings به تنظیمات
4) اینجا: بازسازی + امضا → نصب

$instructions""".trimIndent(),
                Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
