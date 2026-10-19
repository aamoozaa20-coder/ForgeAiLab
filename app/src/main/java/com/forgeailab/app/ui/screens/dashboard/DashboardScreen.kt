package com.forgeailab.app.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgeailab.app.ForgeApp

@Composable
fun DashboardScreen(
    onOpenAnalysis: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenComponents: () -> Unit,
    onOpenRebuild: () -> Unit,
    onOpenSign: () -> Unit,
    onOpenStudio: () -> Unit,
    onOpenReports: () -> Unit
) {
    val project by ForgeApp.instance.workspace.current.collectAsStateWithLifecycle()
    val info = project?.apkInfo
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("داشبورد هوشمند", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            if (info != null) "پروژه فعال: ${info.appLabel.ifBlank { info.packageName }}"
            else "برای شروع یک فایل APK انتخاب کنید",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (info != null) {
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Text(info.packageName, style = MaterialTheme.typography.titleMedium)
                    Text("v${info.versionName} (${info.versionCode})  •  SDK ${info.minSdk}→${info.targetSdk}")
                    Text("مجوز: ${info.permissions.size}  |  Activity: ${info.activities.size}  |  SDK: ${info.knownSdks.size}")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val tiles = listOf(
            DashTile("تحلیل بسته", Icons.Filled.Search, onOpenAnalysis),
            DashTile("دستیار AI", Icons.Filled.Info, onOpenAi),
            DashTile("استودیو منابع", Icons.Filled.Edit, onOpenStudio),
            DashTile("اجزاء و SDK", Icons.Filled.Extension, onOpenComponents),
            DashTile("بازسازی", Icons.Filled.Build, onOpenRebuild),
            DashTile("امضا و نصب", Icons.Filled.Share, onOpenSign),
            DashTile("پروژه‌ها", Icons.Filled.List, onOpenProjects),
            DashTile("گزارش‌ها", Icons.Filled.Description, onOpenReports)
        )
        LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(tiles) { tile ->
                Card(modifier = Modifier.fillMaxWidth().height(100.dp).clickable { tile.onClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(tile.icon, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(tile.title, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

private data class DashTile(val title: String, val icon: ImageVector, val onClick: () -> Unit)
