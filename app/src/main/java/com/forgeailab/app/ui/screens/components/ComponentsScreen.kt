package com.forgeailab.app.ui.screens.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgeailab.app.ForgeApp
import com.forgeailab.app.data.model.SdkCategory

@Composable
fun ComponentsScreen() {
    val project by ForgeApp.instance.workspace.current.collectAsStateWithLifecycle()
    val info = project?.apkInfo
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("آزمایشگاه اجزاء و وابستگی‌ها", style = MaterialTheme.typography.headlineSmall)
        if (info == null) {
            Spacer(Modifier.height(24.dp))
            Text("ابتدا APK را تحلیل کنید.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("SDKهای شناسایی‌شده", style = MaterialTheme.typography.titleMedium) }
            items(info.knownSdks) { sdk ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${sdk.name}  •  ${sdk.category}")
                        LinearProgressIndicator(progress = { sdk.confidence }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                        sdk.evidence.forEach {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (info.knownSdks.isEmpty()) item { Text("موردی یافت نشد.") }
            item { Spacer(Modifier.height(8.dp)); Text("Native libs (${info.nativeLibs.size})", style = MaterialTheme.typography.titleMedium) }
            items(info.nativeLibs.take(40)) { lib ->
                Text("• ${lib.path} [${lib.abi}]", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
