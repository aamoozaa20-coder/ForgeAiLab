package com.forgeailab.app.ui.screens.studio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgeailab.app.ForgeApp

@Composable
fun StudioScreen() {
    val project by ForgeApp.instance.workspace.current.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val strings = project?.apkInfo?.strings?.filter { it.isUiLikely || it.value.length in 2..80 } ?: emptyList()
    val splash = remember(project) { if (project != null) ForgeApp.instance.workspace.findSplashResources() else emptyList() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("استودیوی سفارشی‌سازی", style = MaterialTheme.typography.headlineSmall)
        if (project == null) { Text("پروژه‌ای باز نیست."); return }
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("رشته‌ها") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("اسپلش") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Manifest") })
        }
        Spacer(Modifier.height(12.dp))
        when (tab) {
            0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(strings.take(80)) { s ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text(s.value); Text(s.source, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            1 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(splash) { f ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) { Text(f.path); Text("${f.size} B", style = MaterialTheme.typography.labelSmall) }
                    }
                }
                if (splash.isEmpty()) item { Text("موردی یافت نشد") }
            }
            2 -> Card(Modifier.fillMaxSize()) {
                Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                    Text(project?.apkInfo?.manifestRaw?.take(15000) ?: "خالی", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
