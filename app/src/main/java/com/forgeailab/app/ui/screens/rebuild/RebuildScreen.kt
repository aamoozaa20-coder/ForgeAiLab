package com.forgeailab.app.ui.screens.rebuild

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgeailab.app.ForgeApp
import com.forgeailab.app.data.model.RebuildStep
import com.forgeailab.app.data.model.StepStatus
import kotlinx.coroutines.launch

@Composable
fun RebuildScreen() {
    val project by ForgeApp.instance.workspace.current.collectAsStateWithLifecycle()
    var steps by remember { mutableStateOf<List<RebuildStep>>(emptyList()) }
    var resultMsg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("مرکز بازسازی", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                scope.launch {
                    busy = true; steps = emptyList(); resultMsg = null
                    val result = ForgeApp.instance.rebuildEngine.rebuild { step ->
                        steps = steps.filter { it.name != step.name } + step
                    }
                    resultMsg = if (result.success) "موفق: ${result.outputPath}" else "ناموفق: ${result.errors.joinToString()}"
                    busy = false
                }
            },
            enabled = project != null && !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "در حال بازسازی…" else "شروع بازسازی") }
        LazyColumn {
            items(steps) { step ->
                ListItem(
                    headlineContent = { Text(step.name) },
                    supportingContent = { Text(step.status.name + if (step.message.isNotBlank()) ": ${step.message}" else "") },
                    trailingContent = {
                        when (step.status) {
                            StepStatus.RUNNING -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            StepStatus.SUCCESS -> Text("OK")
                            StepStatus.FAILED -> Text("X", color = MaterialTheme.colorScheme.error)
                            else -> {}
                        }
                    }
                )
            }
        }
        resultMsg?.let { Text(it) }
    }
}
