package com.example.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- CLIENT OUTGOING MESSAGES ---

@JsonClass(generateAdapter = true)
data class BidiSetupWrapper(
    val setup: BidiSetup
)

@JsonClass(generateAdapter = true)
data class BidiSetup(
    val model: String,
    val generationConfig: BidiGenerationConfig? = null,
    val systemInstruction: BidiSystemInstruction? = null,
    val tools: List<BidiToolDeclaration>? = null
)

@JsonClass(generateAdapter = true)
data class BidiGenerationConfig(
    val responseModalities: List<String>,
    val speechConfig: BidiSpeechConfig? = null
)

@JsonClass(generateAdapter = true)
data class BidiSpeechConfig(
    val voiceConfig: BidiVoiceConfig
)

@JsonClass(generateAdapter = true)
data class BidiVoiceConfig(
    val prebuiltVoiceConfig: BidiPrebuiltVoiceConfig
)

@JsonClass(generateAdapter = true)
data class BidiPrebuiltVoiceConfig(
    val voiceName: String
)

@JsonClass(generateAdapter = true)
data class BidiSystemInstruction(
    val parts: List<BidiTextPart>
)

@JsonClass(generateAdapter = true)
data class BidiTextPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class BidiToolDeclaration(
    val functionDeclarations: List<BidiFunctionDeclaration>
)

@JsonClass(generateAdapter = true)
data class BidiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: BidiSchema? = null
)

@JsonClass(generateAdapter = true)
data class BidiSchema(
    val type: String,
    val properties: Map<String, BidiPropertySchema>? = null,
    val required: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class BidiPropertySchema(
    val type: String,
    val description: String
)

// Realtime Audio input
@JsonClass(generateAdapter = true)
data class BidiRealtimeInputWrapper(
    val realtimeInput: BidiRealtimeInput
)

@JsonClass(generateAdapter = true)
data class BidiRealtimeInput(
    val mediaChunks: List<BidiMediaChunk>
)

@JsonClass(generateAdapter = true)
data class BidiMediaChunk(
    val mimeType: String,
    val data: String
)

// Client Content (user text prompt if sent)
@JsonClass(generateAdapter = true)
data class BidiClientContentWrapper(
    val clientContent: BidiClientContent
)

@JsonClass(generateAdapter = true)
data class BidiClientContent(
    val turns: List<BidiTurn>,
    val turnComplete: Boolean = true
)

@JsonClass(generateAdapter = true)
data class BidiTurn(
    val role: String,
    val parts: List<BidiTextPart>
)

// Tool response back to Gemini
@JsonClass(generateAdapter = true)
data class BidiToolResponseWrapper(
    val toolResponse: BidiToolResponse
)

@JsonClass(generateAdapter = true)
data class BidiToolResponse(
    val functionResponses: List<BidiFunctionResponse>
)

@JsonClass(generateAdapter = true)
data class BidiFunctionResponse(
    val id: String,
    val response: BidiFunctionResponseOutput
)

@JsonClass(generateAdapter = true)
data class BidiFunctionResponseOutput(
    val output: Map<String, Any>
)

// --- SERVER INCOMING MESSAGES ---

@JsonClass(generateAdapter = true)
data class BidiServerMessage(
    val serverContent: BidiServerContent? = null,
    val toolCall: BidiToolCall? = null,
    val toolCallCancellation: BidiToolCallCancellation? = null,
    val setupComplete: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class BidiServerContent(
    val modelTurn: BidiModelTurn? = null,
    val turnComplete: Boolean? = null,
    val interrupted: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class BidiModelTurn(
    val parts: List<BidiPart>? = null
)

@JsonClass(generateAdapter = true)
data class BidiPart(
    val text: String? = null,
    val inlineData: BidiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class BidiInlineData(
    val mimeType: String? = null,
    val data: String? = null
)

@JsonClass(generateAdapter = true)
data class BidiToolCall(
    val functionCalls: List<BidiFunctionCall>? = null
)

@JsonClass(generateAdapter = true)
data class BidiFunctionCall(
    val id: String,
    val name: String,
    val args: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class BidiToolCallCancellation(
    val ids: List<String>? = null
)
