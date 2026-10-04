package com.example.device.intent

import android.content.Context
import android.content.Intent

/**
 * Categorical intent types for natural language function mapping.
 */
enum class IntentType(val displayName: String, val icon: String) {
    CALL("Phone Call", "📞"),
    ALARM("Clock Alarm", "⏰"),
    TIMER("Countdown Timer", "⏱️"),
    SMS("SMS Messaging", "✉️"),
    WHATSAPP("WhatsApp Message", "💬"),
    APP_LAUNCH("Launch App", "📱"),
    YOUTUBE("YouTube Player", "▶️"),
    NAVIGATION("GPS Navigation", "🧭"),
    WEB_SEARCH("Web Search", "🔍"),
    SYSTEM_CONTROL("System Control", "⚙️")
}

/**
 * Result of executing an actionable Android Intent.
 */
sealed class IntentExecutionResult(val isSuccess: Boolean, open val message: String) {
    data class Success(
        override val message: String,
        val targetIntent: Intent? = null,
        val details: Map<String, Any?> = emptyMap()
    ) : IntentExecutionResult(true, message)

    data class Failure(
        override val message: String,
        val error: String? = null
    ) : IntentExecutionResult(false, message)

    data class PermissionNeeded(
        val permission: String,
        val rationale: String
    ) : IntentExecutionResult(false, rationale)
}

/**
 * Actionable Android Intent generated from natural language intent mapping.
 */
data class ActionableIntent(
    val type: IntentType,
    val rawQuery: String,
    val actionName: String,
    val summary: String,
    val confirmationSpeech: String,
    val parameters: Map<String, Any?>,
    val androidIntent: Intent?,
    val executionLambda: (Context) -> IntentExecutionResult
) {
    /**
     * Executes the actionable intent on the given Android context.
     */
    fun execute(context: Context): IntentExecutionResult {
        return try {
            executionLambda(context)
        } catch (e: Exception) {
            IntentExecutionResult.Failure("Failed to execute $actionName: ${e.message}", e.message)
        }
    }
}
