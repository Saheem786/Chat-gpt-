package com.example.ai

import com.example.gemini.AiEngineMode
import com.example.gemini.GeminiLiveService

class GeminiProviderClient(private val geminiService: GeminiLiveService) : AiProviderClient {
    override suspend fun generateChatResponse(
        prompt: String,
        base64Image: String?,
        isDeepResearch: Boolean,
        preferredModel: String?,
        engineMode: AiEngineMode,
        onChunkAvailable: ((chunk: String, isFirstChunk: Boolean) -> Unit)?
    ): Pair<String, String?> {
        return geminiService.generateChatResponse(
            prompt = prompt,
            base64Image = base64Image,
            isDeepResearch = isDeepResearch,
            preferredModel = preferredModel,
            engineMode = engineMode,
            onChunkAvailable = onChunkAvailable
        )
    }

    override suspend fun listModels(): List<String> {
        return listOf(
            "Autonomous Smart Routing",
            "gemini-3.8-flash",
            "gemini-3.1-pro-preview",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite"
        )
    }

    override suspend fun testConnection(): ConnectionTestResult {
        return try {
            val (resp, _) = geminiService.generateChatResponse(
                prompt = "Hello",
                engineMode = AiEngineMode.QUICK
            )
            if (resp.contains("⚠️") || resp.contains("kripya", ignoreCase = true)) {
                ConnectionTestResult(
                    isConnected = false,
                    serverUrl = "Google Gemini API",
                    availableModelsCount = 5,
                    errorMessage = resp
                )
            } else {
                ConnectionTestResult(
                    isConnected = true,
                    serverUrl = "Google Gemini API",
                    availableModelsCount = 5
                )
            }
        } catch (e: Exception) {
            ConnectionTestResult(
                isConnected = false,
                serverUrl = "Google Gemini API",
                availableModelsCount = 0,
                errorMessage = e.localizedMessage ?: e.message
            )
        }
    }
}
