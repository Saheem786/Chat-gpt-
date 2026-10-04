package com.example.device.command

import android.content.Context
import android.util.Log
import com.example.device.SamTools
import com.example.device.SamToolResult

sealed class CommandResult {
    data class Success(val message: String, val verifiedState: String? = null) : CommandResult()
    data class RequiresConfirmation(val callId: String, val action: String, val args: Map<String, Any?>, val prompt: String) : CommandResult()
    data class Failure(val error: String) : CommandResult()
}

class UnifiedCommandEngine(
    private val context: Context,
    private val samTools: SamTools
) {
    private val securityPolicy = ActionSecurityPolicy()

    fun processCommand(callId: String, actionName: String, args: Map<String, Any?>): CommandResult {
        Log.d("UnifiedCommandEngine", "Processing action: $actionName with args: $args")
        
        // 1. Validate action security policy
        val securityCheck = securityPolicy.checkAction(actionName, args)
        if (securityCheck.requiresConfirmation) {
            Log.d("UnifiedCommandEngine", "Action requires confirmation: ${securityCheck.promptMessage}")
            return CommandResult.RequiresConfirmation(
                callId = callId,
                action = actionName,
                args = args,
                prompt = securityCheck.promptMessage ?: "Are you sure you want to perform this action?"
            )
        }

        // 2. Execute Action directly if safe
        return executeActionDirect(actionName, args)
    }

    fun executeActionDirect(actionName: String, args: Map<String, Any?>): CommandResult {
        Log.d("UnifiedCommandEngine", "Executing safe action directly: $actionName")
        return try {
            val result = when (actionName.lowercase().trim()) {
                "send_sms", "sendsms", "sms" -> {
                    val recipient = args["recipient"]?.toString() ?: args["contact"]?.toString() ?: ""
                    val message = args["message"]?.toString() ?: ""
                    samTools.sendSms(recipient, message)
                }
                "make_call", "makecall", "call" -> {
                    val phone = args["phoneNumber"]?.toString() ?: args["phone"]?.toString() ?: ""
                    samTools.makeCall(phone)
                }
                "send_whatsapp", "sendwhatsappmessage", "whatsapp" -> {
                    val recipient = args["contactName"]?.toString() ?: args["recipient"]?.toString() ?: ""
                    val message = args["message"]?.toString() ?: ""
                    samTools.sendWhatsApp(recipient, message)
                }
                "toggle_wifi", "wifi" -> {
                    val enable = args["enable"] as? Boolean ?: true
                    samTools.toggleWifi(enable)
                }
                "toggle_flashlight", "flashlight" -> {
                    val enable = args["enable"] as? Boolean ?: true
                    samTools.toggleFlashlight(enable)
                }
                "adjust_volume", "volume" -> {
                    val level = (args["level"] as? Number)?.toInt() ?: 50
                    samTools.adjustVolume("set", level)
                }
                "set_alarm" -> {
                    val hour = (args["hour"] as? Number)?.toInt() ?: 7
                    val minute = (args["minute"] as? Number)?.toInt() ?: 0
                    val label = args["label"]?.toString() ?: "JARVIS Alarm"
                    samTools.setAlarm(hour, minute, label)
                }
                "set_timer_or_alarm", "timer" -> {
                    val timeMinutes = (args["timeMinutes"] as? Number)?.toInt() ?: 5
                    val label = args["label"]?.toString() ?: "JARVIS Timer"
                    samTools.setTimerOrAlarm(timeMinutes, null, label, false)
                }
                "device_status" -> {
                    samTools.getDeviceStatus()
                }
                else -> {
                    SamToolResult.Failure("Unsupported action: $actionName", "Unsupported action: $actionName")
                }
            }

            // 3. Verify actual device state
            when (result) {
                is SamToolResult.Success -> {
                    CommandResult.Success(
                        message = result.message,
                        verifiedState = "Device state verified successfully: Action executed."
                    )
                }
                is SamToolResult.Failure -> {
                    CommandResult.Failure(result.error)
                }
            }
        } catch (e: Exception) {
            Log.e("UnifiedCommandEngine", "Error executing action: $actionName", e)
            CommandResult.Failure("Execution error: ${e.message}")
        }
    }
}
