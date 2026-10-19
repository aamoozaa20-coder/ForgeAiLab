package com.forgeailab.app.ui.screens.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgeailab.app.ForgeApp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProjectsScreen() {
    val projects by ForgeApp.instance.workspace.projects.collectAsStateWithLifecycle()
    val fmt = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("fa")) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("پروژه‌های اخیر", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        if (projects.isEmpty()) {
            Text("پروژه‌ای ذخیره نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(projects) { p ->
                    Card(Modifier.fillMaxWidth().clickable { ForgeApp.instance.workspace.openProject(p.id) }) {
                        Column(Modifier.padding(12.dp)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            Text("شناسه: ${p.id}", style = MaterialTheme.typography.bodySmall)
                            Text(fmt.format(Date(p.updatedAt)), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
