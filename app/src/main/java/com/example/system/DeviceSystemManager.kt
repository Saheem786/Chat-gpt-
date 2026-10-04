package com.example.system

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import java.net.URLEncoder
import java.util.Calendar

class DeviceSystemManager(private val context: Context) {

    fun toggleFlashlight(enabled: Boolean): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return false
            cameraManager.setTorchMode(cameraId, enabled)
            true
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "Flashlight error", e)
            false
        }
    }

    fun adjustVolume(direction: String, levelPercent: Int?): String {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            
            when (direction.uppercase()) {
                "UP" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    val currentPercent = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / maxVolume
                    "Volume increased to $currentPercent%"
                }
                "DOWN" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    val currentPercent = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / maxVolume
                    "Volume decreased to $currentPercent%"
                }
                "MAX" -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_SHOW_UI)
                    "Volume set to maximum"
                }
                "MUTE" -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                    "Volume muted"
                }
                "SET" -> {
                    if (levelPercent != null) {
                        val targetVol = (levelPercent.coerceIn(0, 100) * maxVolume) / 100
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                        "Volume set to $levelPercent%"
                    } else {
                        "Specific volume level not provided"
                    }
                }
                else -> "Unknown volume action"
            }
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "Volume adjust error", e)
            "Could not adjust volume: ${e.message}"
        }
    }

    fun getBatteryInfo(): String {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val percent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = batteryManager.isCharging
            val statusStr = if (isCharging) "charging" else "discharging"
            "Battery level is at $percent% and currently $statusStr"
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "Battery info error", e)
            "Unable to fetch battery status"
        }
    }

    fun setAlarm(hour: Int, minute: Int, label: String): Boolean {
        return setAlarmWithDetails(hour, minute, label).first
    }

    fun setAlarmWithDetails(hour: Int, minute: Int, label: String = "Alarm"): Pair<Boolean, String> {
        return try {
            val safeHour = hour.coerceIn(0, 23)
            val safeMinute = minute.coerceIn(0, 59)
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, safeHour)
                putExtra(AlarmClock.EXTRA_MINUTES, safeMinute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val formattedTime = String.format("%02d:%02d", safeHour, safeMinute)
            val message = "Alarm set ho gaya hai for $formattedTime ('$label')! ⏰"
            Log.i("DeviceSystemManager", message)
            Pair(true, message)
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "Alarm error", e)
            Pair(false, "Alarm set karne mein error aaya: ${e.message}")
        }
    }

    /**
     * Parses natural voice input in English or Hindi/Hinglish to configure an alarm
     */
    fun parseAndSetAlarm(rawCommand: String): Pair<Boolean, String>? {
        val lower = rawCommand.lowercase().trim()

        val isAlarmIntent = lower.contains("alarm") || lower.contains("wake me up") ||
                lower.contains("jaga dena") || lower.contains("jagana") ||
                lower.contains("baje ka") || lower.contains("baje uthana")

        if (!isAlarmIntent) return null

        var hour = -1
        var minute = 0
        var isPm = lower.contains("pm") || lower.contains("sham") || lower.contains("shaam") ||
                lower.contains("raat") || lower.contains("dopahar")
        var isAm = lower.contains("am") || lower.contains("subah") || lower.contains("morning")

        // 1. Check for HH:MM (am/pm)? e.g. "6:30", "7:45 am", "08:15 pm"
        val timeRegex = Regex("(\\b\\d{1,2}):(\\d{2})\\s*(am|pm)?")
        val matchTime = timeRegex.find(lower)
        if (matchTime != null) {
            hour = matchTime.groupValues[1].toIntOrNull() ?: -1
            minute = matchTime.groupValues[2].toIntOrNull() ?: 0
            val amPmGroup = matchTime.groupValues[3]
            if (amPmGroup == "pm") isPm = true
            if (amPmGroup == "am") isAm = true
        } else {
            // 2. Check for "7 am", "8 pm", "7 baje", "at 7", "for 6"
            val hourRegex = Regex("(\\b\\d{1,2})\\s*(am|pm|baje|o'?clock)?")
            val matches = hourRegex.findAll(lower)
            for (m in matches) {
                val num = m.groupValues[1].toIntOrNull()
                val unit = m.groupValues[2]
                if (num != null && num in 1..24) {
                    if (unit.isNotEmpty() || lower.contains("alarm for $num") || lower.contains("alarm at $num")) {
                        hour = num
                        if (unit == "pm") isPm = true
                        if (unit == "am") isAm = true
                        break
                    }
                }
            }
        }

        if (hour == -1) return null

        // Normalize 12-hour to 24-hour format
        if (isPm && hour < 12) {
            hour += 12
        } else if (isAm && hour == 12) {
            hour = 0
        }

        // Extract potential custom label
        val label = when {
            lower.contains("medicine") || lower.contains("dawa") -> "Medicine"
            lower.contains("meeting") -> "Meeting"
            lower.contains("gym") || lower.contains("workout") -> "Workout"
            lower.contains("office") -> "Office"
            lower.contains("school") || lower.contains("college") -> "Class"
            else -> "Arushi Alarm"
        }

        return setAlarmWithDetails(hour, minute, label)
    }

    /**
     * Sends SMS or opens SMS composer for specific contact / phone number
     */
    fun sendSms(recipient: String, messageText: String): Pair<Boolean, String> {
        val cleanRecipient = recipient.trim()
        val cleanMsg = messageText.trim()
        val digits = cleanRecipient.filter { it.isDigit() || it == '+' }
        var targetPhoneNumber = digits
        var displayName = cleanRecipient

        if (digits.length < 7 && cleanRecipient.isNotBlank() && cleanRecipient.any { it.isLetter() }) {
            val contactNum = lookupContactNumber(cleanRecipient)
            if (contactNum != null) {
                targetPhoneNumber = contactNum
                displayName = "$cleanRecipient ($contactNum)"
            }
        }

        return try {
            val uri = if (targetPhoneNumber.isNotBlank()) {
                Uri.parse("smsto:${targetPhoneNumber.filter { it.isDigit() || it == '+' }}")
            } else {
                Uri.parse("smsto:")
            }
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                if (cleanMsg.isNotBlank()) {
                    putExtra("sms_body", cleanMsg)
                    putExtra(Intent.EXTRA_TEXT, cleanMsg)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val info = if (targetPhoneNumber.isNotBlank()) " for $displayName" else ""
            Pair(true, "SMS composer open kar diya hai$info! ✉️")
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "SMS error", e)
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    type = "vnd.android-dir/mms-sms"
                    if (cleanMsg.isNotBlank()) putExtra("sms_body", cleanMsg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                Pair(true, "SMS app open kar diya hai! ✉️")
            } catch (err: Exception) {
                Pair(false, "SMS open karne mein error aaya: ${err.message}")
            }
        }
    }

    /**
     * Toggles or opens Wi-Fi control panel / settings on Android
     */
    fun toggleWifi(enable: Boolean? = null): Pair<Boolean, String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // On modern Android (API 29+), opening the native internet/Wi-Fi panel is required
                val panelIntent = try {
                    Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } catch (e: Throwable) {
                    Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                try {
                    context.startActivity(panelIntent)
                } catch (e: Exception) {
                    val fallbackIntent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                }
                val actionDesc = when (enable) {
                    true -> "Wi-Fi chalu (ON) karne ke liye"
                    false -> "Wi-Fi band (OFF) karne ke liye"
                    null -> "Wi-Fi toggle karne ke liye"
                }
                Pair(true, "$actionDesc Wi-Fi settings panel open kar diya hai! 📶")
            } else {
                @Suppress("DEPRECATION")
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                if (wifiManager != null) {
                    val newState = enable ?: !wifiManager.isWifiEnabled
                    @Suppress("DEPRECATION")
                    wifiManager.isWifiEnabled = newState
                    val stateStr = if (newState) "ON" else "OFF"
                    Pair(true, "Wi-Fi $stateStr kar diya hai! 📶")
                } else {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Pair(true, "Wi-Fi settings open kar di hain! 📶")
                }
            }
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "Wi-Fi error", e)
            try {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Pair(true, "Wi-Fi settings open kar di hain! 📶")
            } catch (err: Exception) {
                Pair(false, "Could not open Wi-Fi settings: ${err.message}")
            }
        }
    }

    fun lookupContactNumber(nameQuery: String): String? {
        val cleanQuery = nameQuery.trim()
        if (cleanQuery.isBlank()) return null
        return try {
            val contentResolver = context.contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$cleanQuery%")

            contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (cursor.moveToFirst() && numIdx != -1) {
                    cursor.getString(numIdx)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "Failed to lookup contact '$nameQuery'", e)
            null
        }
    }

    fun setTimer(seconds: Int, label: String): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(1))
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "Timer error", e)
            false
        }
    }

    fun sendWhatsAppMessage(phoneNumberOrName: String, messageText: String): Boolean {
        return try {
            val digits = phoneNumberOrName.filter { it.isDigit() }
            val uriString = if (digits.length >= 7) {
                "https://api.whatsapp.com/send?phone=$digits&text=${java.net.URLEncoder.encode(messageText, "UTF-8")}"
            } else {
                "https://api.whatsapp.com/send?text=${java.net.URLEncoder.encode(messageText, "UTF-8")}"
            }
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uriString)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "WhatsApp intent error", e)
            false
        }
    }

    fun initiatePhoneCall(phoneNumber: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:${phoneNumber.filter { it.isDigit() || it == '+' }}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("DeviceSystemManager", "Phone call intent error", e)
            false
        }
    }

    fun searchAndPlayYouTube(query: String): String {
        return try {
            // Attempt 1: Direct YouTube app search intent
            val appIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(appIntent)
            "YouTube par '$query' play kar diya hai!"
        } catch (e: Exception) {
            // Fallback: Direct YouTube web/deep-link URL intent
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                "YouTube par '$query' search kar diya hai!"
            } catch (err: Exception) {
                "Error playing YouTube video: ${err.message}"
            }
        }
    }
}
