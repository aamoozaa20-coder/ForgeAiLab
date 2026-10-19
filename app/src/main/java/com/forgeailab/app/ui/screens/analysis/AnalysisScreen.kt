package com.forgeailab.app.ui.screens.analysis

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgeailab.app.ForgeApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AnalysisScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val project by ForgeApp.instance.workspace.current.collectAsStateWithLifecycle()
    var progress by remember { mutableStateOf<Pair<String, Float>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true; error = null
            try {
                val cache = File(context.cacheDir, "import_${System.currentTimeMillis()}.apk")
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        cache.outputStream().use { output -> input.copyTo(output) }
                    } ?: error("cannot read")
                }
                ForgeApp.instance.workspace.importApk(cache) { msg, p -> progress = msg to p }
            } catch (e: Exception) { error = e.message } finally { busy = false; progress = null }
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("تحلیل بسته APK", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { picker.launch(arrayOf("application/vnd.android.package-archive", "*/*")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp))
            Text(if (busy) "در حال تحلیل…" else "انتخاب فایل APK")
        }
        progress?.let { (msg, p) -> LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth()); Text(msg) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        val info = project?.apkInfo
        if (info != null) {
            Spacer(Modifier.height(16.dp))
            listOf(
                "پکیج" to info.packageName, "نسخه" to "${info.versionName} (${info.versionCode})",
                "SDK" to "min=${info.minSdk} target=${info.targetSdk}", "حجم" to "${info.fileSize}",
                "SHA-256" to info.sha256, "مجوزها" to "${info.permissions.size}",
                "Activities" to "${info.activities.size}", "Services" to "${info.services.size}",
                "SDKها" to info.knownSdks.joinToString { it.name }.ifBlank { "—" }
            ).forEach { (t, v) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(t, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(v)
                    }
                }
            }
        } else if (!busy) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("APKای بارگذاری نشده", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
