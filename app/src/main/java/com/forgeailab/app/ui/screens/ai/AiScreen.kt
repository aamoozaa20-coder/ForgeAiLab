package com.forgeailab.app.ui.screens.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forgeailab.app.ForgeApp
import com.forgeailab.app.data.model.AiRole
import kotlinx.coroutines.launch

@Composable
fun AiScreen() {
    val messages by ForgeApp.instance.aiAgent.messages.collectAsStateWithLifecycle()
    val apiKey by ForgeApp.instance.settings.apiKey.collectAsStateWithLifecycle(initialValue = "")
    val project by ForgeApp.instance.workspace.current.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("دستیار هوش مصنوعی", style = MaterialTheme.typography.headlineSmall)
        Text(
            buildString {
                if (project?.apkInfo != null) append("پروژه: ${project!!.apkInfo!!.packageName}  |  ")
                else append("بدون پروژه  |  ")
                if (apiKey.isBlank()) append("دستورات محلی فعال") else append("Gemini + ابزارهای واقعی")
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("دستورات واقعی:")
                            Text("• مجوزها / SDKها / اسپلش / رشته‌ها")
                            Text("• فایل‌ها")
                            Text("• جایگزینی متن Hello به درود")
                            Text("• بازسازی")
                            Text("بعد از بازسازی از صفحه امضا نصب کنید.")
                        }
                    }
                }
            }
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == AiRole.USER
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.Start else Arrangement.End
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth(0.92f)
                    ) { Text(msg.content, Modifier.padding(12.dp)) }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("دستور فارسی…") },
                enabled = !busy,
                maxLines = 4
            )
            IconButton(
                onClick = {
                    val text = input.trim()
                    if (text.isEmpty()) return@IconButton
                    input = ""
                    scope.launch {
                        busy = true
                        try {
                            ForgeApp.instance.aiAgent.handle(text, apiKey)
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && input.isNotBlank()
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Icon(Icons.AutoMirrored.Filled.Send, "ارسال")
            }
        }
    }
}
