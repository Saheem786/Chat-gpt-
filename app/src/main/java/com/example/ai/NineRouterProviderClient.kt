package com.example.ai

import android.util.Log
import com.example.gemini.AiEngineMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class NineRouterProviderClient(
    private var baseUrl: String,
    private var apiKey: String,
    private var modelName: String
) : AiProviderClient {

    companion object {
        private const val TAG = "NineRouterProvider"
        
        fun normalizeBaseUrl(rawUrl: String): String {
            var clean = rawUrl.trim()
            if (clean.isBlank()) clean = "http://localhost:20128"
            while (clean.endsWith("/")) {
                clean = clean.substring(0, clean.length - 1)
            }
            if (clean.endsWith("/v1", ignoreCase = true)) {
                clean = clean.substring(0, clean.length - 3)
            }
            return clean
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun updateConfig(newBaseUrl: String, newApiKey: String, newModel: String) {
        this.baseUrl = newBaseUrl
        this.apiKey = newApiKey
        this.modelName = newModel
    }

    private val endpointUrl: String
        get() {
            val normalized = normalizeBaseUrl(baseUrl)
            return "$normalized/v1/chat/completions"
        }

    private val modelsUrl: String
        get() {
            val normalized = normalizeBaseUrl(baseUrl)
            return "$normalized/v1/models"
        }

    override suspend fun generateChatResponse(
        prompt: String,
        base64Image: String?,
        isDeepResearch: Boolean,
        preferredModel: String?,
        engineMode: AiEngineMode,
        onChunkAvailable: ((chunk: String, isFirstChunk: Boolean) -> Unit)?
    ): Pair<String, String?> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Pair("⚠️ 9Router API Key is missing! Please configure it in Settings.", null)
        }

        val effectiveModel = if (!preferredModel.isNullOrBlank() && !preferredModel.equals("Autonomous Smart Routing", ignoreCase = true)) {
            preferredModel
        } else if (modelName.isNotBlank()) {
            modelName
        } else {
            "default"
        }

        val jsonBody = JSONObject().apply {
            put("model", effectiveModel)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are JARVIS, an advanced, personal, human-like AI assistant inspired by Myraa and Tony Stark's AI. Be natural, concise, and helpful.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("messages", messages)
            put("temperature", engineMode.temperature.toDouble())
            put("max_tokens", engineMode.maxOutputTokens)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val bodyString = response.body?.string().orEmpty()

                when (code) {
                    200 -> {
                        val root = JSONObject(bodyString)
                        val choices = root.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val firstChoice = choices.getJSONObject(0)
                            val messageObj = firstChoice.optJSONObject("message")
                            val content = messageObj?.optString("content") ?: ""
                            onChunkAvailable?.invoke(content, true)
                            return@withContext Pair(content, null)
                        } else {
                            return@withContext Pair("⚠️ 9Router returned an empty response.", null)
                        }
                    }
                    401 -> Pair("⚠️ 9Router Authentication Error (401): Invalid API Key.", null)
                    403 -> Pair("⚠️ 9Router Access Denied (403).", null)
                    404 -> Pair("⚠️ 9Router Endpoint Not Found (404): Check Base URL.", null)
                    429 -> Pair("⚠️ 9Router Rate Limited (429): Too many requests.", null)
                    in 500..599 -> Pair("⚠️ 9Router Server/Provider Error ($code).", null)
                    else -> Pair("⚠️ 9Router error: HTTP $code - ${bodyString.take(100)}", null)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network timeout or connection failure to 9Router", e)
            Pair("⚠️ 9Router connection failed: Server unreachable or timeout.", null)
        } catch (e: Exception) {
            Log.e(TAG, "Parsing error from 9Router response", e)
            Pair("⚠️ 9Router response parsing error: ${e.message}", null)
        }
    }

    override suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()

        val request = Request.Builder()
            .url(modelsUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string().orEmpty()
                    val root = JSONObject(bodyString)
                    val data = root.optJSONArray("data") ?: JSONArray()
                    val models = mutableListOf<String>()
                    for (i in 0 until data.length()) {
                        val obj = data.getJSONObject(i)
                        val id = obj.optString("id")
                        if (id.isNotBlank()) models.add(id)
                    }
                    return@withContext models
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list 9Router models", e)
        }
        emptyList()
    }

    override suspend fun testConnection(): ConnectionTestResult = withContext(Dispatchers.IO) {
        val normalized = normalizeBaseUrl(baseUrl)
        if (apiKey.isBlank()) {
            return@withContext ConnectionTestResult(
                isConnected = false,
                serverUrl = normalized,
                availableModelsCount = 0,
                errorMessage = "401 -> Invalid API key"
            )
        }

        val request = Request.Builder()
            .url(modelsUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val bodyString = response.body?.string().orEmpty()
                when (code) {
                    200 -> {
                        val root = JSONObject(bodyString)
                        val data = root.optJSONArray("data") ?: JSONArray()
                        val count = data.length()
                        ConnectionTestResult(
                            isConnected = true,
                            serverUrl = normalized,
                            availableModelsCount = count
                        )
                    }
                    401 -> ConnectionTestResult(false, normalized, 0, "401 -> Invalid API key")
                    403 -> ConnectionTestResult(false, normalized, 0, "403 -> Access denied")
                    404 -> ConnectionTestResult(false, normalized, 0, "404 -> Incorrect endpoint")
                    429 -> ConnectionTestResult(false, normalized, 0, "429 -> Rate limited")
                    in 500..599 -> ConnectionTestResult(false, normalized, 0, "$code -> 9Router/server/provider error")
                    else -> ConnectionTestResult(false, normalized, 0, "HTTP $code -> ${bodyString.take(80)}")
                }
            }
        } catch (e: IOException) {
            ConnectionTestResult(false, normalized, 0, "timeout -> Server unreachable")
        } catch (e: Exception) {
            ConnectionTestResult(false, normalized, 0, "Error -> ${e.localizedMessage ?: e.message}")
        }
    }
}
