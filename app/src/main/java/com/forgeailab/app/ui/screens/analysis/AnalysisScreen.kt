package com.forgeailab.app.ui.screens.analysis

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgeailab.app.ForgeApp
import com.forgeailab.app.domain.apk.InstalledApp
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
    var showInstalled by remember { mutableStateOf(false) }
    var installed by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    fun importFile(file: File) {
        scope.launch {
            busy = true; error = null; progress = null
            try {
                ForgeApp.instance.workspace.importApk(file) { msg, p -> progress = msg to p }
            } catch (e: Exception) {
                error = e.message ?: "خطا در تحلیل"
            } finally {
                busy = false; progress = null
            }
        }
    }

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
                importFile(cache)
            } catch (e: Exception) {
                error = e.message; busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("تحلیل بسته APK", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { picker.launch(arrayOf("application/vnd.android.package-archive", "*/*")) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.FolderOpen, null)
            Spacer(Modifier.width(8.dp))
            Text("انتخاب فایل APK از حافظه")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                scope.launch {
                    busy = true; error = null
                    try {
                        installed = ForgeApp.instance.installedApps.load(includeSystem = false)
                        showInstalled = true
                    } catch (e: Exception) {
                        error = e.message
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Apps, null)
            Spacer(Modifier.width(8.dp))
            Text(if (busy && !showInstalled) "در حال خواندن برنامه‌ها…" else "انتخاب از برنامه‌های نصب‌شده")
        }
        progress?.let { (msg, p) ->
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
            Text(msg)
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        val info = project?.apkInfo
        if (info != null) {
            Spacer(Modifier.height(16.dp))
            Text("نتیجه تحلیل", style = MaterialTheme.typography.titleMedium)
            Column(Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false)) {
                listOf(
                    "نام" to info.appLabel,
                    "پکیج" to info.packageName,
                    "نسخه" to "${info.versionName} (${info.versionCode})",
                    "SDK" to "min=${info.minSdk} target=${info.targetSdk}",
                    "حجم" to "${info.fileSize} بایت",
                    "SHA-256" to info.sha256,
                    "مجوزها" to "${info.permissions.size}",
                    "Activities" to "${info.activities.size}",
                    "Services" to "${info.services.size}",
                    "SDKها" to info.knownSdks.joinToString { it.name }.ifBlank { "—" }
                ).forEach { (t, v) ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(t, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(v)
                        }
                    }
                }
            }
        } else if (!busy) {
            Spacer(Modifier.height(24.dp))
            Text("هنوز برنامه‌ای انتخاب نشده.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showInstalled) {
        AlertDialog(
            onDismissRequest = { showInstalled = false },
            title = { Text("برنامه‌های نصب‌شده") },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("جست‌وجو…") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    val filtered = remember(query, installed) {
                        if (query.isBlank()) installed
                        else installed.filter {
                            it.label.contains(query, true) || it.packageName.contains(query, true)
                        }
                    }
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            ListItem(
                                headlineContent = { Text(app.label) },
                                supportingContent = {
                                    Text("${app.packageName}\nv${app.versionName}  •  ${app.apkSize / 1024} KB")
                                },
                                modifier = Modifier.clickable {
                                    showInstalled = false
                                    scope.launch {
                                        busy = true; error = null
                                        try {
                                            val apk = ForgeApp.instance.installedApps.copyApkToCache(app)
                                            importFile(apk)
                                        } catch (e: Exception) {
                                            error = e.message; busy = false
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInstalled = false }) { Text("بستن") }
            }
        )
    }
}
