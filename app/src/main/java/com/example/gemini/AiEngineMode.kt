package com.example.gemini

/**
 * AI Dual-Engine Operational Modes for Sam AI.
 *
 * 1. QUICK: Ultra-fast answers using "gemini-3.8-flash" (or "gemini-3.5-flash")
 *    - temperature: 0.5
 *    - maxOutputTokens: 350
 *    - Tuned for low latency, rapid conversational voice responses, and quick queries.
 *
 * 2. DEEP_THINK: Complex reasoning & step-by-step thinking using "gemini-3.1-pro-preview"
 *    - temperature: 0.7
 *    - maxOutputTokens: 4096
 *    - thinkingConfig enabled with thinkingLevel = high
 *    - Tuned for coding, mathematics, logic, multi-turn architecture, and deep analysis.
 */
enum class AiEngineMode(
    val title: String,
    val modelName: String,
    val icon: String,
    val badgeLabel: String,
    val description: String,
    val temperature: Double,
    val maxOutputTokens: Int,
    val topK: Int? = null
) {
    QUICK(
        title = "Quick Response",
        modelName = "gemini-3.8-flash",
        icon = "⚡",
        badgeLabel = "3.8 Flash • Fast",
        description = "Ultra-fast low latency answers (max 150 tokens, temp 0.4, topK 20)",
        temperature = 0.4,
        maxOutputTokens = 150,
        topK = 20
    ),
    DEEP_THINK(
        title = "Deep Thought",
        modelName = "gemini-3.1-pro-preview",
        icon = "🧠",
        badgeLabel = "3.1 Pro • Thinking",
        description = "Complex step-by-step reasoning & deep analysis (max 4096 tokens, temp 0.7)",
        temperature = 0.7,
        maxOutputTokens = 4096,
        topK = null
    );

    companion object {
        fun fromName(name: String?): AiEngineMode {
            return when (name?.uppercase()) {
                "DEEP_THINK", "DEEP", "THINKING", "PRO" -> DEEP_THINK
                else -> QUICK
            }
        }
    }
}
