package com.forgeailab.app.domain.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    /** Try newest models first; fall back automatically. */
    private val models = listOf(
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-2.0-flash-001",
        "gemini-1.5-flash",
        "gemini-1.5-flash-latest",
        "gemini-pro"
    )

    suspend fun chat(
        apiKey: String,
        systemPrompt: String,
        userMessage: String,
        model: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("API key not set"))
        }
        val tryModels = if (model != null) listOf(model) + models else models
        var lastError: Exception? = null
        for (m in tryModels.distinct()) {
            val r = callModel(apiKey, m, systemPrompt, userMessage)
            if (r.isSuccess) return@withContext r
            lastError = r.exceptionOrNull() as? Exception ?: Exception(r.exceptionOrNull()?.message)
            val msg = lastError?.message.orEmpty()
            // only retry on 404 / model not found
            if (!msg.contains("404") && !msg.contains("not found", true) && !msg.contains("no longer available", true)) {
                return@withContext r
            }
        }
        Result.failure(lastError ?: Exception("همه مدل‌های Gemini ناموفق بودند"))
    }

    private fun callModel(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userMessage: String
    ): Result<String> {
        return try {
            val body = JsonObject().apply {
                add("contents", gson.toJsonTree(listOf(
                    mapOf(
                        "role" to "user",
                        "parts" to listOf(mapOf("text" to "$systemPrompt\n\n---\n\n$userMessage"))
                    )
                )))
                add("generationConfig", gson.toJsonTree(mapOf(
                    "temperature" to 0.3,
                    "maxOutputTokens" to 4096
                )))
            }
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                .post(body.toString().toRequestBody(jsonMedia))
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return Result.failure(Exception("Gemini ($model) ${response.code}: ${text.take(400)}"))
                }
                val root = gson.fromJson(text, JsonObject::class.java)
                val content = root.getAsJsonArray("candidates")?.get(0)?.asJsonObject
                    ?.getAsJsonObject("content")?.getAsJsonArray("parts")?.get(0)?.asJsonObject
                    ?.get("text")?.asString
                    ?: return Result.failure(Exception("Empty Gemini response ($model)"))
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
