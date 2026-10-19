package com.forgeailab.app.domain.ai

import com.forgeailab.app.data.model.*
import com.forgeailab.app.domain.apk.ApkAnalyzer
import com.forgeailab.app.domain.engine.RebuildEngine
import com.forgeailab.app.domain.engine.SignManager
import com.forgeailab.app.domain.workspace.WorkspaceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AiAgent(
    private val workspace: WorkspaceManager,
    private val analyzer: ApkAnalyzer,
    private val rebuildEngine: RebuildEngine,
    private val signManager: SignManager,
    private val gemini: GeminiClient = GeminiClient()
) {
    private val _messages = MutableStateFlow<List<AiMessage>>(emptyList())
    val messages: StateFlow<List<AiMessage>> = _messages.asStateFlow()

    suspend fun handle(userText: String, apiKey: String): String {
        append(AiRole.USER, userText)
        val info = workspace.current.value?.apkInfo
        val offline = tryOffline(userText, info)
        if (offline != null) { append(AiRole.ASSISTANT, offline); return offline }
        val context = buildString {
            appendLine("تو عامل FORGE AI LAB هستی. فارسی جواب بده.")
            if (info != null) {
                appendLine("پکیج: ${info.packageName} نسخه: ${info.versionName}")
                appendLine("مجوزها: ${info.permissions.size} SDKها: ${info.knownSdks.joinToString { it.name }}")
            } else appendLine("APK بارگذاری نشده.")
        }
        val result = gemini.chat(apiKey, context, userText)
        val reply = result.getOrElse { e ->
            val msg = "خطای Gemini: ${e.message}. از دستورات محلی مثل «مجوزها» استفاده کنید."
            append(AiRole.ASSISTANT, msg); return msg
        }
        append(AiRole.ASSISTANT, reply); return reply
    }

    private fun tryOffline(text: String, info: ApkInfo?): String? {
        val t = text.lowercase()
        if (info == null) {
            if (t.contains("تحلیل") || t.contains("مجوز") || t.contains("بررسی"))
                return "هنوز APK انتخاب نشده. از صفحه تحلیل یک فایل وارد کنید."
            return null
        }
        return when {
            t.contains("مجوز") -> "مجوزها (${info.permissions.size}):\n" + info.permissions.joinToString("\n") { "• $it" }
            t.contains("اسپلش") || t.contains("آغاز") || t.contains("splash") -> {
                val s = workspace.findSplashResources()
                if (s.isEmpty()) "منبع اسپلش یافت نشد" else "اسپلش:\n" + s.take(20).joinToString("\n") { "• ${it.path}" }
            }
            t.contains("sdk") || t.contains("تبلیغ") -> {
                if (info.knownSdks.isEmpty()) "SDK یافت نشد"
                else info.knownSdks.joinToString("\n") { "• ${it.name} [${it.category}] ${(it.confidence*100).toInt()}%" }
            }
            t.contains("ساختار") || t.contains("بررسی") || t.contains("گزارش") ->
                "پکیج: ${info.packageName}\nنسخه: ${info.versionName}\nSDK: ${info.minSdk}->${info.targetSdk}\nمجوز: ${info.permissions.size}\nActivity: ${info.activities.size}"
            t.contains("رشته") -> info.strings.filter { it.isUiLikely }.take(30).joinToString("\n") { "• ${it.value}" }.ifBlank { "رشته‌ای نیست" }
            else -> null
        }
    }

    private fun append(role: AiRole, content: String) {
        _messages.value = _messages.value + AiMessage(UUID.randomUUID().toString(), role, content)
    }
}
