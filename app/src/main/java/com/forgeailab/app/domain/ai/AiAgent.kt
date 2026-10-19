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

/**
 * AI agent with real executable tools over the current workspace.
 * Commands work offline; Gemini is optional for free-form chat.
 */
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

        // 1) Real tool commands (always preferred)
        val toolReply = runTools(userText, info)
        if (toolReply != null) {
            append(AiRole.ASSISTANT, toolReply)
            return toolReply
        }

        // 2) Offline Q&A
        val offline = tryOffline(userText, info)
        if (offline != null) {
            append(AiRole.ASSISTANT, offline)
            return offline
        }

        // 3) Gemini free-form
        val context = buildString {
            appendLine("تو عامل FORGE AI LAB هستی. فارسی جواب بده.")
            appendLine("ابزارهای واقعی: مجوزها، اسپلش، SDK، رشته‌ها، فایل‌ها، جایگزینی متن، بازسازی.")
            if (info != null) {
                appendLine("پکیج: ${info.packageName} نسخه: ${info.versionName}")
                appendLine("مجوزها: ${info.permissions.size} SDKها: ${info.knownSdks.joinToString { it.name }}")
            } else appendLine("APK بارگذاری نشده.")
        }
        val result = gemini.chat(apiKey, context, userText)
        val reply = result.getOrElse { e ->
            val msg = "خطای Gemini: ${e.message}\nاز دستورات محلی استفاده کنید:\n• مجوزها\n• جایگزینی متن قدیم به جدید\n• بازسازی\n• فایل‌ها"
            append(AiRole.ASSISTANT, msg)
            return msg
        }
        append(AiRole.ASSISTANT, reply)
        return reply
    }

    private suspend fun runTools(text: String, info: ApkInfo?): String? {
        val t = text.trim()
        val lower = t.lowercase()

        // REPLACE: جایگزینی متن X به Y  OR  replace X -> Y
        val replaceFa = Regex("""جایگزینی\s+متن\s+(.+?)\s+به\s+(.+)""", RegexOption.IGNORE_CASE)
        val replaceEn = Regex("""replace\s+(.+?)\s*(?:->|to)\s*(.+)""", RegexOption.IGNORE_CASE)
        val rm = replaceFa.find(t) ?: replaceEn.find(t)
        if (rm != null) {
            if (info == null) return "اول یک برنامه انتخاب کنید."
            val old = rm.groupValues[1].trim().trim('"', ''')
            val neu = rm.groupValues[2].trim().trim('"', ''')
            return replaceInWorkspace(old, neu)
        }

        if (lower.contains("بازسازی") || lower == "rebuild") {
            if (info == null) return "اول یک برنامه انتخاب کنید."
            val result = rebuildEngine.rebuild()
            return if (result.success)
                "بازسازی موفق.\nخروجی: ${result.outputPath}\nاز صفحه امضا/نصب می‌توانید نصب کنید."
            else
                "بازسازی ناموفق:\n" + result.errors.joinToString("\n")
        }

        if (lower.startsWith("فایل ") || lower.startsWith("read ")) {
            val path = t.substringAfter(' ').trim()
            val content = workspace.readTextFile(path)
            return content?.take(4000) ?: "فایل خوانده نشد: $path"
        }

        if (lower.contains("فایل‌ها") || lower.contains("لیست فایل") || lower == "files") {
            if (info == null) return "اول یک برنامه انتخاب کنید."
            val nodes = workspace.listFiles("")
            return "ریشه استخراج‌شده (${nodes.size}):\n" +
                nodes.take(40).joinToString("\n") { n ->
                    if (n.isDirectory) "📁 ${n.name}/" else "📄 ${n.name} (${n.size}B)"
                }
        }

        return null
    }

    private suspend fun replaceInWorkspace(old: String, neu: String): String {
        if (old.isEmpty()) return "متن قدیمی خالی است."
        val project = workspace.current.value ?: return "پروژه‌ای باز نیست."
        val root = java.io.File(project.workDir, "extracted")
        var count = 0
        val changed = mutableListOf<String>()
        root.walkTopDown().maxDepth(14).forEach { f ->
            if (!f.isFile) return@forEach
            if (f.length() > 2_000_000) return@forEach
            val name = f.name.lowercase()
            if (!(name.endsWith(".xml") || name.endsWith(".json") || name.endsWith(".txt") ||
                        name.endsWith(".smali") || name.endsWith(".html") || name.endsWith(".js") ||
                        name.endsWith(".css") || name.endsWith(".properties"))) return@forEach
            try {
                val text = f.readText(Charsets.UTF_8)
                if (text.contains(old)) {
                    f.writeText(text.replace(old, neu), Charsets.UTF_8)
                    count++
                    changed.add(f.relativeTo(root).path)
                }
            } catch (_: Exception) {
            }
        }
        return if (count == 0) "هیچ فایلی حاوی «$old» نبود."
        else "جایگزینی انجام شد: $count فایل\n" + changed.take(25).joinToString("\n") { "• $it" } +
                "\nحالا بگویید «بازسازی» تا APK جدید ساخته شود."
    }

    private fun tryOffline(text: String, info: ApkInfo?): String? {
        val t = text.lowercase()
        if (info == null) {
            if (t.contains("تحلیل") || t.contains("مجوز") || t.contains("بررسی") || t.contains("برنامه"))
                return "هنوز برنامه‌ای انتخاب نشده. از صفحه تحلیل، «انتخاب از برنامه‌های نصب‌شده» را بزنید."
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
                else info.knownSdks.joinToString("\n") { "• ${it.name} [${it.category}] ${(it.confidence * 100).toInt()}%" }
            }
            t.contains("ساختار") || t.contains("بررسی") || t.contains("گزارش") || t.contains("خلاصه") ->
                "پکیج: ${info.packageName}\nنسخه: ${info.versionName}\nSDK: ${info.minSdk}->${info.targetSdk}\nمجوز: ${info.permissions.size}\nActivity: ${info.activities.size}\nرشته‌های UI: ${info.strings.count { it.isUiLikely }}"
            t.contains("رشته") -> info.strings.filter { it.isUiLikely }.take(40).joinToString("\n") { "• ${it.value}" }
                .ifBlank { "رشته‌ای نیست" }
            t.contains("کمک") || t.contains("help") || t.contains("دستور") ->
                """دستورات واقعی:
• مجوزها
• SDKها / تبلیغات
• اسپلش
• رشته‌ها
• فایل‌ها
• فایل res/values/strings.xml
• جایگزینی متن سلام به درود
• بازسازی
بعد از بازسازی از صفحه امضا دکمه نصب را بزنید.""".trimIndent()
            else -> null
        }
    }

    private fun append(role: AiRole, content: String) {
        _messages.value = _messages.value + AiMessage(UUID.randomUUID().toString(), role, content)
    }
}
