package com.example.ai

import android.content.Context
import com.example.gemini.AiEngineMode
import com.example.gemini.GeminiLiveService

class JarvisAIClient(
    context: Context,
    private val geminiService: GeminiLiveService
) {
    private val secureStorage = SecureStorageManager(context)
    
    private val geminiProvider = GeminiProviderClient(geminiService)
    private val nineRouterProvider = NineRouterProviderClient(
        baseUrl = secureStorage.getNineRouterBaseUrl(),
        apiKey = secureStorage.getNineRouterApiKey(),
        modelName = secureStorage.getNineRouterModel()
    )

    fun getActiveProvider(): AiProviderClient {
        val providerType = secureStorage.getAiProvider()
        return if (providerType.equals("NINEROUTER", ignoreCase = true)) {
            nineRouterProvider.updateConfig(
                secureStorage.getNineRouterBaseUrl(),
                secureStorage.getNineRouterApiKey(),
                secureStorage.getNineRouterModel()
            )
            nineRouterProvider
        } else {
            geminiProvider
        }
    }

    suspend fun generateChatResponse(
        prompt: String,
        base64Image: String? = null,
        isDeepResearch: Boolean = false,
        preferredModel: String? = null,
        engineMode: AiEngineMode = AiEngineMode.QUICK,
        onChunkAvailable: ((chunk: String, isFirstChunk: Boolean) -> Unit)? = null
    ): Pair<String, String?> {
        return getActiveProvider().generateChatResponse(
            prompt = prompt,
            base64Image = base64Image,
            isDeepResearch = isDeepResearch,
            preferredModel = preferredModel,
            engineMode = engineMode,
            onChunkAvailable = onChunkAvailable
        )
    }

    suspend fun listModels(): List<String> {
        return getActiveProvider().listModels()
    }

    suspend fun testConnection(): ConnectionTestResult {
        return getActiveProvider().testConnection()
    }
}
