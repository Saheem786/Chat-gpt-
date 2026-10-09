package com.example.ai

import com.example.gemini.AiEngineMode

data class ConnectionTestResult(
    val isConnected: Boolean,
    val serverUrl: String,
    val availableModelsCount: Int,
    val errorMessage: String? = null
)

interface AiProviderClient {
    suspend fun generateChatResponse(
        prompt: String,
        base64Image: String? = null,
        isDeepResearch: Boolean = false,
        preferredModel: String? = null,
        engineMode: AiEngineMode = AiEngineMode.QUICK,
        onChunkAvailable: ((chunk: String, isFirstChunk: Boolean) -> Unit)? = null
    ): Pair<String, String?>

    suspend fun listModels(): List<String>
    suspend fun testConnection(): ConnectionTestResult
}
