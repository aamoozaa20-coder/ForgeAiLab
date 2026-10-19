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

    // Discover existing rebuild outputs
    LaunchedEffect(project) {
        val dir = project?.workDir?.let { File(it) } ?: return@LaunchedEffect
        rebuiltPath = dir.listFiles()?.filter { it.name.startsWith("rebuild_") && it.name.endsWith(".apk") }
            ?.maxByOrNull { it.lastModified() }?.absolutePath
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("مرکز امضا و نصب", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                val path = project?.originalApkPath ?: return@Button
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
        ) { Text(if (busy) "بررسی…" else "بررسی امضای APK اصلی") }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    busy = true; message = "در حال بازسازی…"
                    val result = ForgeApp.instance.rebuildEngine.rebuild()
                    if (result.success && result.outputPath != null) {
                        rebuiltPath = result.outputPath
                        message = "بازسازی موفق: ${result.outputPath}"
                    } else {
                        message = "ناموفق: " + result.errors.joinToString()
                    }
                    busy = false
                }
            },
            enabled = project != null && !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("بازسازی APK اصلاح‌شده") }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val path = rebuiltPath ?: return@Button
                val r = ForgeApp.instance.installer.install(File(path))
                message = if (r.isSuccess) "پنجره نصب باز شد. تأیید کنید."
                else "خطا: ${r.exceptionOrNull()?.message}"
            },
            enabled = rebuiltPath != null && !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("نصب نسخه اصلاح‌شده") }

        if (rebuiltPath != null) {
            Spacer(Modifier.height(8.dp))
            Text("آخرین خروجی: $rebuiltPath", style = MaterialTheme.typography.bodySmall)
        }
        if (message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(message)
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
                """1) در صفحه تحلیل، برنامه نصب‌شده را انتخاب کنید.
2) در دستیار AI دستور بدهید مثلاً:
   جایگزینی متن Hello به درود
3) بگویید بازسازی
4) اینجا «نصب نسخه اصلاح‌شده» را بزنید.

توجه: برنامه‌های سیستم‌امضا ممکن است بدون امضای اصلی نصب نشوند. برای تست از اپ‌های کاربر استفاده کنید.

$instructions""".trimIndent(),
                Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
