package com.forgeailab.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgeailab.app.ForgeApp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val apiKey by ForgeApp.instance.settings.apiKey.collectAsStateWithLifecycle(initialValue = "")
    val dark by ForgeApp.instance.settings.darkTheme.collectAsStateWithLifecycle(initialValue = true)
    var keyInput by remember(apiKey) { mutableStateOf(apiKey) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it; saved = false },
            label = { Text("کلید Gemini API") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            supportingText = { Text("کلید فقط روی دستگاه ذخیره می‌شود.") }
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { scope.launch { ForgeApp.instance.settings.setApiKey(keyInput); saved = true } }, modifier = Modifier.fillMaxWidth()) {
            Text(if (saved) "ذخیره شد" else "ذخیره کلید")
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("حالت تاریک")
            Switch(checked = dark, onCheckedChange = { v -> scope.launch { ForgeApp.instance.settings.setDarkTheme(v) } })
        }
        Spacer(Modifier.height(24.dp))
        Text("FORGE AI LAB v1.0.0", style = MaterialTheme.typography.bodySmall)
    }
}
