package com.forgeailab.app.ui.screens.resources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgeailab.app.ForgeApp

@Composable
fun ResourcesScreen() {
    val project by ForgeApp.instance.workspace.current.collectAsStateWithLifecycle()
    var path by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<String?>(null) }
    val nodes = remember(path, project) { if (project == null) emptyList() else ForgeApp.instance.workspace.listFiles(path) }
    val searchResults = remember(query, project) {
        if (query.length >= 2 && project != null) ForgeApp.instance.workspace.search(query) else emptyList()
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("مرورگر منابع", style = MaterialTheme.typography.headlineSmall)
        if (project == null) { Text("ابتدا یک APK تحلیل کنید."); return }
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("جست‌وجو…") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        if (path.isNotEmpty()) {
            TextButton(onClick = { path = path.substringBeforeLast('/', missingDelimiterValue = ""); preview = null }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null); Text("بالا")
            }
        }
        Text(if (path.isEmpty()) "/extracted" else "/$path", style = MaterialTheme.typography.labelMedium)
        if (preview != null) {
            Card(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                    Text(preview!!, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { preview = null }) { Text("بستن") }
                }
            }
        } else {
            val list = if (searchResults.isNotEmpty()) searchResults else nodes
            LazyColumn(Modifier.weight(1f)) {
                items(list) { node ->
                    ListItem(
                        headlineContent = { Text(node.name) },
                        supportingContent = { Text(if (node.isDirectory) "پوشه" else "${node.size} B") },
                        leadingContent = { Icon(if (node.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile, null) },
                        modifier = Modifier.clickable {
                            if (node.isDirectory) { path = node.path; preview = null }
                            else preview = ForgeApp.instance.workspace.readTextFile(node.path) ?: "no preview"
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
