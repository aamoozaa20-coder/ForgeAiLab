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

    suspend fun chat(
        apiKey: String,
        systemPrompt: String,
        userMessage: String,
        model: String = "gemini-2.0-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("API key not set"))
        }
        try {
            val body = JsonObject().apply {
                add("contents", gson.toJsonTree(listOf(
                    mapOf(
                        "role" to "user",
                        "parts" to listOf(mapOf("text" to "$systemPrompt\n\n---\n\n$userMessage"))
                    )
                )))
                add("generationConfig", gson.toJsonTree(mapOf(
                    "temperature" to 0.4,
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
                    return@withContext Result.failure(Exception("Gemini error (${response.code}): ${text.take(300)}"))
                }
                val root = gson.fromJson(text, JsonObject::class.java)
                val content = root.getAsJsonArray("candidates")?.get(0)?.asJsonObject
                    ?.getAsJsonObject("content")?.getAsJsonArray("parts")?.get(0)?.asJsonObject
                    ?.get("text")?.asString
                    ?: return@withContext Result.failure(Exception("Empty Gemini response"))
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
