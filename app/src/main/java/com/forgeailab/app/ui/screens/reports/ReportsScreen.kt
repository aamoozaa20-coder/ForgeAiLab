package com.forgeailab.app.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgeailab.app.ForgeApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ReportsScreen() {
    val project by ForgeApp.instance.workspace.current.collectAsStateWithLifecycle()
    var json by remember { mutableStateOf("") }
    var savedPath by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("گزارش‌ها", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val info = project?.apkInfo ?: return@Button
                scope.launch {
                    val report = withContext(Dispatchers.Default) {
                        ForgeApp.instance.analyzer.generateReport(info)
                    }
                    json = report.json
                    val dir = File(project!!.workDir, "reports").also { it.mkdirs() }
                    val htmlFile = File(dir, "report_${System.currentTimeMillis()}.html")
                    htmlFile.writeText(report.html)
                    File(dir, "report_${System.currentTimeMillis()}.json").writeText(report.json)
                    savedPath = htmlFile.absolutePath
                }
            },
            enabled = project?.apkInfo != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text("تولید گزارش JSON + HTML") }
        savedPath?.let {
            Spacer(Modifier.height(8.dp))
            Text("ذخیره شد: $it", style = MaterialTheme.typography.bodySmall)
        }
        if (json.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                Text(json, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
