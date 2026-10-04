package com.example.system

import android.content.Context
import android.util.Log
import com.example.device.SamTools
import com.example.system.DeviceSystemManager
import kotlinx.coroutines.delay

class ActionPipelineManager(
    private val context: Context,
    private val samTools: SamTools,
    private val deviceSystemManager: DeviceSystemManager
) {
    companion object {
        private const val TAG = "ActionPipelineManager"
    }

    suspend fun executeChainedCommands(text: String): String? {
        val lower = text.lowercase()
        // Split on common conjunctions "aur", "and", "phir", "uske baad" or commas
        val parts = text.split(Regex("(?i)\\s+(?:aur|and|phir|uske\\s+baad)\\s+|[,\\.]"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (parts.size <= 1) return null

        Log.i(TAG, "Chained execution triggered with ${parts.size} steps: $parts")
        val resultsList = mutableListOf<String>()

        for (part in parts) {
            val partLower = part.lowercase()
            var handledMsg: String? = null

            // 1. Wifi
            if (partLower.contains("wifi") || partLower.contains("wi-fi")) {
                val enable = when {
                    partLower.contains("off") || partLower.contains("band") || partLower.contains("disable") -> false
                    partLower.contains("on") || partLower.contains("chalu") || partLower.contains("enable") -> true
                    else -> null
                }
                val res = samTools.toggleWifi(enable)
                handledMsg = res.message
            }
            // 2. Torch/Flashlight
            else if (partLower.contains("torch") || partLower.contains("flashlight") || partLower.contains("flash") || partLower.contains("light")) {
                val enable = when {
                    partLower.contains("off") || partLower.contains("band") || partLower.contains("disable") -> false
                    partLower.contains("on") || partLower.contains("chalu") || partLower.contains("enable") -> true
                    else -> null
                }
                val res = samTools.toggleFlashlight(enable ?: true)
                handledMsg = res.message
            }
            // 3. Bluetooth
            else if (partLower.contains("bluetooth")) {
                val enable = when {
                    partLower.contains("off") || partLower.contains("band") || partLower.contains("disable") -> false
                    partLower.contains("on") || partLower.contains("chalu") || partLower.contains("enable") -> true
                    else -> null
                }
                val res = samTools.toggleBluetooth(enable)
                handledMsg = res.message
            }
            // 4. Volume
            else if (partLower.contains("volume") || partLower.contains("sound") || partLower.contains("awaj") || partLower.contains("awaaz")) {
                val isMax = partLower.contains("full") || partLower.contains("max") || partLower.contains("high") || partLower.contains("badha") || partLower.contains("increase")
                val isMute = partLower.contains("mute") || partLower.contains("silent") || partLower.contains("low") || partLower.contains("kam") || partLower.contains("decrease")
                val direction = when {
                    isMax -> "MAX"
                    isMute -> "MUTE"
                    else -> "UP"
                }
                val res = samTools.adjustVolume(direction, null)
                handledMsg = res.message
            }
            // 5. Alarm
            else if (partLower.contains("alarm") || partLower.contains("wake me up") || partLower.contains("jaga dena")) {
                val parseResult = deviceSystemManager.parseAndSetAlarm(part)
                handledMsg = parseResult?.second ?: run {
                    samTools.setTimerOrAlarm(0, 7, "Sam AI Alarm", true)
                    "Subah 7:00 baje ka alarm set kar diya hai! ⏰"
                }
            }

            if (handledMsg != null) {
                resultsList.add(handledMsg)
            }
            delay(200)
        }

        if (resultsList.isNotEmpty()) {
            return resultsList.joinToString(" aur ")
        }
        return null
    }
}
