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
    val scope = rememberCoroutineScope()
    val instructions = remember { ForgeApp.instance.signManager.signingInstructions() }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("مرکز امضا و نصب", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val path = project?.originalApkPath ?: return@Button
                scope.launch {
                    busy = true
                    val result = ForgeApp.instance.signManager.inspectSignature(File(path))
                    certInfo = result.certificateInfo; message = result.message; busy = false
                }
            },
            enabled = project != null && !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "بررسی…" else "بررسی امضای APK") }
        if (message.isNotBlank()) Text(message)
        if (certInfo.isNotBlank()) Card(Modifier.fillMaxWidth()) { Text(certInfo, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(16.dp))
        Text("راهنمای امضا", style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth()) { Text(instructions, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) }
    }
}
