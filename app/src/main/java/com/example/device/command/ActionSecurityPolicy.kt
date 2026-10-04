package com.example.device.command

enum class SecurityLevel {
    SAFE,
    CONFIRMATION_REQUIRED,
    RESTRICTED
}

data class SecurityCheckResult(
    val level: SecurityLevel,
    val requiresConfirmation: Boolean,
    val promptMessage: String? = null
)

class ActionSecurityPolicy {

    fun checkAction(action: String, args: Map<String, Any?>): SecurityCheckResult {
        val normalizedAction = action.lowercase().trim()
        return when (normalizedAction) {
            "send_sms", "sendsms", "sms" -> {
                val message = args["message"]?.toString() ?: ""
                val recipient = args["recipient"]?.toString() ?: "unknown contact"
                SecurityCheckResult(
                    level = SecurityLevel.CONFIRMATION_REQUIRED,
                    requiresConfirmation = true,
                    promptMessage = "Are you sure you want to send this SMS to $recipient? Message: \"$message\""
                )
            }
            "make_call", "makecall", "call" -> {
                val phoneNumber = args["phoneNumber"]?.toString() ?: args["phone"]?.toString() ?: "unknown number"
                SecurityCheckResult(
                    level = SecurityLevel.CONFIRMATION_REQUIRED,
                    requiresConfirmation = true,
                    promptMessage = "Are you sure you want to call $phoneNumber?"
                )
            }
            "send_whatsapp", "sendwhatsappmessage", "whatsapp" -> {
                val message = args["message"]?.toString() ?: ""
                val contact = args["contactName"]?.toString() ?: "unknown contact"
                SecurityCheckResult(
                    level = SecurityLevel.CONFIRMATION_REQUIRED,
                    requiresConfirmation = true,
                    promptMessage = "Are you sure you want to send this WhatsApp message to $contact? Message: \"$message\""
                )
            }
            "delete_event", "deleteevent", "clear_calendar" -> {
                SecurityCheckResult(
                    level = SecurityLevel.CONFIRMATION_REQUIRED,
                    requiresConfirmation = true,
                    promptMessage = "Are you sure you want to delete calendar events?"
                )
            }
            "toggle_wifi", "toggle_flashlight", "set_alarm", "set_timer_or_alarm", "device_status", "adjust_volume" -> {
                SecurityCheckResult(
                    level = SecurityLevel.SAFE,
                    requiresConfirmation = false
                )
            }
            else -> {
                SecurityCheckResult(
                    level = SecurityLevel.SAFE,
                    requiresConfirmation = false
                )
            }
        }
    }
}
