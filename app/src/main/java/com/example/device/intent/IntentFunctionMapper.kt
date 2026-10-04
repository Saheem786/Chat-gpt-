package com.example.device.intent

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import java.net.URLEncoder

/**
 * Metadata descriptor for supported natural language intent types.
 */
data class SupportedIntentInfo(
    val type: IntentType,
    val title: String,
    val exampleQueries: List<String>,
    val targetAndroidAction: String
)

/**
 * Intelligent Natural Language to Actionable Android Intent Mapping System.
 *
 * Translates multi-lingual user commands (English, Hindi, Hinglish) like
 * 'call Mom', 'set alarm for 7:30 AM', 'send SMS to Rahul', 'navigate to Airport'
 * into fully-formed, executable Android Intents.
 */
object IntentFunctionMapper {

    private const val TAG = "IntentFunctionMapper"

    private val KNOWN_APPS = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "camera" to "android.media.action.IMAGE_CAPTURE",
        "settings" to "android.settings.SETTINGS",
        "calculator" to "com.google.android.calculator",
        "spotify" to "com.spotify.music",
        "instagram" to "com.instagram.android",
        "maps" to "com.google.android.apps.maps",
        "chrome" to "com.android.chrome",
        "clock" to "com.google.android.deskclock",
        "gallery" to "com.google.android.apps.photos"
    )

    /**
     * Translates a natural language query into an ActionableAndroidIntent.
     * Returns null if no intent pattern matches with high confidence.
     */
    fun mapNaturalLanguageToIntent(rawQuery: String, context: Context): ActionableIntent? {
        val cleanQuery = rawQuery.trim()
        if (cleanQuery.isBlank()) return null

        val lower = cleanQuery.lowercase()
            .replace(Regex("(?i)^(hey\\s+)?(arushi|sam|assistant)(\\s+ai)?[,\\s]*"), "")
            .trim()

        // 1. Phone Call Intent ("call Mom", "dial 9876543210", "call Papa", "Mom ko call karo")
        mapCallIntent(cleanQuery, lower, context)?.let { return it }

        // 2. Alarm Intent ("set alarm for 7:30 am", "6 baje ka alarm lagao", "wake me up at 8")
        mapAlarmIntent(cleanQuery, lower)?.let { return it }

        // 3. Timer Intent ("set timer for 10 minutes", "5 minute ka timer lagao")
        mapTimerIntent(cleanQuery, lower)?.let { return it }

        // 4. SMS Intent ("send SMS to Rahul hello", "Mom ko SMS bhejo I am on the way")
        mapSmsIntent(cleanQuery, lower, context)?.let { return it }

        // 5. WhatsApp Intent ("WhatsApp Mom reached safely", "open WhatsApp", "Rahul ko WhatsApp karo")
        mapWhatsAppIntent(cleanQuery, lower, context)?.let { return it }

        // 6. YouTube Song/Video Intent ("play Believer on YouTube", "Arijit Singh song chalao")
        mapYouTubeIntent(cleanQuery, lower)?.let { return it }

        // 7. Navigation Intent ("navigate to Connaught Place", "directions to Airport", "Delhi ka rasta")
        mapNavigationIntent(cleanQuery, lower)?.let { return it }

        // 8. App Launch Intent ("open YouTube", "launch Camera", "open Calculator", "open Settings")
        mapAppLaunchIntent(cleanQuery, lower, context)?.let { return it }

        // 9. System Control Intent ("turn on flashlight", "volume up", "mute", "battery status", "wifi on")
        mapSystemControlIntent(cleanQuery, lower, context)?.let { return it }

        // 10. Web Search Intent ("search for quantum mechanics", "google search current weather")
        mapWebSearchIntent(cleanQuery, lower)?.let { return it }

        return null
    }

    /**
     * Map Call Intent ("call Mom", "call +919876543210", "dial 100", "Mom ko phone lagao")
     */
    private fun mapCallIntent(rawQuery: String, lower: String, context: Context): ActionableIntent? {
        val isCallQuery = (lower.startsWith("call") || lower.startsWith("dial") ||
                lower.startsWith("phone") || lower.contains("ko call") ||
                lower.contains("ko phone") || lower.contains("call karo") ||
                lower.contains("phone lagao")) && !lower.contains("whatsapp")

        if (!isCallQuery) return null

        // Extract contact name or phone number
        var target = cleanQueryForTarget(lower, listOf("call", "dial", "phone lagao", "ko call karo", "ko call", "ko phone", "phone"))
        if (target.isBlank()) return null

        val digits = target.filter { it.isDigit() || it == '+' }
        var resolvedNumber = digits
        var displayName = target

        // If contact name specified (e.g. "Mom", "Rahul"), resolve from ContactsContract
        if (digits.length < 5 && target.any { it.isLetter() }) {
            val contact = lookupContact(target, context)
            if (contact != null) {
                resolvedNumber = contact.second
                displayName = "${contact.first} ($resolvedNumber)"
            }
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intentAction = if (hasCallPermission && resolvedNumber.isNotBlank()) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val dataUri = if (resolvedNumber.isNotBlank()) Uri.parse("tel:$resolvedNumber") else Uri.parse("tel:")

        val androidIntent = Intent(intentAction, dataUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val confirmationMsg = if (resolvedNumber.isNotBlank()) {
            "$displayName ko call lagaya ja raha hai! 📞"
        } else {
            "Phone dialer open kar diya hai! 📞"
        }

        return ActionableIntent(
            type = IntentType.CALL,
            rawQuery = rawQuery,
            actionName = "call_contact",
            summary = "Call $displayName",
            confirmationSpeech = confirmationMsg,
            parameters = mapOf(
                "target" to target,
                "phoneNumber" to resolvedNumber,
                "hasDirectCallPermission" to hasCallPermission
            ),
            androidIntent = androidIntent,
            executionLambda = { ctx ->
                try {
                    ctx.startActivity(androidIntent)
                    IntentExecutionResult.Success(confirmationMsg, androidIntent)
                } catch (e: Exception) {
                    IntentExecutionResult.Failure("Call launch failed: ${e.message}", e.message)
                }
            }
        )
    }

    /**
     * Map Alarm Intent ("set alarm for 7:30 AM", "6 baje ka alarm lagao", "wake me up at 8 am")
     */
    private fun mapAlarmIntent(rawQuery: String, lower: String): ActionableIntent? {
        val isAlarmQuery = lower.contains("alarm") || lower.contains("wake me up") ||
                lower.contains("jaga dena") || lower.contains("jagana") ||
                lower.contains("baje uthana")

        if (!isAlarmQuery) return null

        var hour = -1
        var minute = 0
        var isPm = lower.contains("pm") || lower.contains("sham") || lower.contains("shaam") ||
                lower.contains("raat") || lower.contains("dopahar") || lower.contains("evening") || lower.contains("night")
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

        // Default to 7:00 AM if no time found but alarm requested
        if (hour == -1) {
            hour = 7
            minute = 0
            isAm = true
        }

        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0

        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)

        val label = when {
            lower.contains("medicine") || lower.contains("dawa") -> "Medicine"
            lower.contains("meeting") -> "Meeting"
            lower.contains("gym") || lower.contains("workout") -> "Workout"
            lower.contains("office") -> "Office"
            else -> "Alarm"
        }

        val formattedTime = String.format("%02d:%02d", safeHour, safeMinute)
        val confirmationMsg = "Alarm set ho gaya hai for $formattedTime ('$label')! ⏰"

        val androidIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, safeHour)
            putExtra(AlarmClock.EXTRA_MINUTES, safeMinute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return ActionableIntent(
            type = IntentType.ALARM,
            rawQuery = rawQuery,
            actionName = "set_alarm",
            summary = "Set Alarm for $formattedTime ($label)",
            confirmationSpeech = confirmationMsg,
            parameters = mapOf(
                "hour" to safeHour,
                "minute" to safeMinute,
                "label" to label,
                "formattedTime" to formattedTime
            ),
            androidIntent = androidIntent,
            executionLambda = { ctx ->
                try {
                    ctx.startActivity(androidIntent)
                    IntentExecutionResult.Success(confirmationMsg, androidIntent)
                } catch (e: Exception) {
                    IntentExecutionResult.Failure("Alarm launch failed: ${e.message}", e.message)
                }
            }
        )
    }

    /**
     * Map Timer Intent ("set timer for 10 minutes", "timer 5 min ka lagao")
     */
    private fun mapTimerIntent(rawQuery: String, lower: String): ActionableIntent? {
        val isTimerQuery = lower.contains("timer")
        if (!isTimerQuery) return null

        var durationSeconds = 300 // default 5 min
        val match = Regex("(\\d+)\\s*(minute|min|sec|second|ghanta|hour)").find(lower)
        if (match != null) {
            val amount = match.groupValues[1].toIntOrNull() ?: 5
            val unit = match.groupValues[2]
            durationSeconds = when {
                unit.startsWith("sec") -> amount
                unit.startsWith("hour") || unit.startsWith("ghanta") -> amount * 3600
                else -> amount * 60
            }
        }

        val confirmationMsg = "${durationSeconds / 60} minute ka timer set kar diya hai! ⏱️"

        val androidIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Sam Timer")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return ActionableIntent(
            type = IntentType.TIMER,
            rawQuery = rawQuery,
            actionName = "set_timer",
            summary = "Set Timer for $durationSeconds seconds",
            confirmationSpeech = confirmationMsg,
            parameters = mapOf("durationSeconds" to durationSeconds),
            androidIntent = androidIntent,
            executionLambda = { ctx ->
                try {
                    ctx.startActivity(androidIntent)
                    IntentExecutionResult.Success(confirmationMsg, androidIntent)
                } catch (e: Exception) {
                    IntentExecutionResult.Failure("Timer failed: ${e.message}", e.message)
                }
            }
        )
    }

    /**
     * Map SMS Intent ("send SMS to Rahul hello I am outside", "message 9876543210 reached safely")
     */
    private fun mapSmsIntent(rawQuery: String, lower: String, context: Context): ActionableIntent? {
        val isSmsQuery = (lower.contains("sms") || lower.startsWith("text") ||
                lower.contains("message bhejo") || lower.contains("msg bhejo")) &&
                !lower.contains("whatsapp")

        if (!isSmsQuery) return null

        val (recipient, body) = extractSmsDetails(rawQuery, context)
        val digits = recipient.filter { it.isDigit() || it == '+' }
        val targetPhone = if (digits.isNotBlank()) digits else ""

        val uri = if (targetPhone.isNotBlank()) Uri.parse("smsto:$targetPhone") else Uri.parse("smsto:")
        val androidIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
            if (body.isNotBlank()) {
                putExtra("sms_body", body)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val confirmationMsg = if (recipient.isNotBlank()) {
            "SMS composer open kar diya hai for $recipient! ✉️"
        } else {
            "SMS composer open kar diya hai! ✉️"
        }

        return ActionableIntent(
            type = IntentType.SMS,
            rawQuery = rawQuery,
            actionName = "send_sms",
            summary = "Send SMS to $recipient",
            confirmationSpeech = confirmationMsg,
            parameters = mapOf("recipient" to recipient, "phoneNumber" to targetPhone, "body" to body),
            androidIntent = androidIntent,
            executionLambda = { ctx ->
                try {
                    ctx.startActivity(androidIntent)
                    IntentExecutionResult.Success(confirmationMsg, androidIntent)
                } catch (e: Exception) {
                    IntentExecutionResult.Failure("SMS failed: ${e.message}", e.message)
                }
            }
        )
    }

    /**
     * Map WhatsApp Intent ("WhatsApp Mom reached safely", "open WhatsApp", "Rahul ko WhatsApp karo")
     */
    private fun mapWhatsAppIntent(rawQuery: String, lower: String, context: Context): ActionableIntent? {
        if (!lower.contains("whatsapp")) return null

        val msgClean = rawQuery
            .replace(Regex("(?i)^(open|launch|kholo|chalao|send|bhejo)\\s*"), "")
            .replace(Regex("(?i)\\bwhatsapp\\b"), "")
            .replace(Regex("(?i)\\b(pe|par|ko|message|msg|karo|bhejo)\\b"), "")
            .trim()

        val uri = if (msgClean.isNotBlank()) {
            Uri.parse("whatsapp://send?text=${URLEncoder.encode(msgClean, "UTF-8")}")
        } else {
            Uri.parse("whatsapp://send")
        }

        val androidIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val confirmationMsg = "WhatsApp open kar diya hai! 💬"

        return ActionableIntent(
            type = IntentType.WHATSAPP,
            rawQuery = rawQuery,
            actionName = "send_whatsapp",
            summary = "Open WhatsApp",
            confirmationSpeech = confirmationMsg,
            parameters = mapOf("message" to msgClean),
            androidIntent = androidIntent,
            executionLambda = { ctx ->
                try {
                    ctx.startActivity(androidIntent)
                    IntentExecutionResult.Success(confirmationMsg, androidIntent)
                } catch (e: Exception) {
                    // Fallback to launch intent without package constraint
                    try {
                        val launchIntent = ctx.packageManager.getLaunchIntentForPackage("com.whatsapp")
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ctx.startActivity(launchIntent)
                            IntentExecutionResult.Success(confirmationMsg, launchIntent)
                        } else {
                            IntentExecutionResult.Failure("WhatsApp is not installed on this device")
                        }
                    } catch (err: Exception) {
                        IntentExecutionResult.Failure("Could not open WhatsApp: ${err.message}")
                    }
                }
            }
        )
    }

    /**
     * Map YouTube Intent ("play Believer on YouTube", "Arijit Singh song chalao")
     */
    private fun mapYouTubeIntent(rawQuery: String, lower: String): ActionableIntent? {
        val isYouTubeQuery = lower.contains("youtube") ||
                ((lower.contains("song") || lower.contains("gana") || lower.contains("music") || lower.contains("track")) &&
                        (lower.contains("play") || lower.contains("chalao") || lower.contains("baja") || lower.contains("suno")))

        if (!isYouTubeQuery) return null

        val cleanQuery = rawQuery
            .replace(Regex("(?i)^(play|search|chalao|suno|baja|sunao)\\s*"), "")
            .replace(Regex("(?i)\\b(on youtube|youtube par|youtube pe|youtube|gana|song|music|track|kholo|chalao|karo|baja|sunao)\\b"), "")
            .trim()

        val searchQuery = if (cleanQuery.isNotBlank()) cleanQuery else "trending music"
        val confirmationMsg = "YouTube par '$searchQuery' chala raha hoon! 🎵"

        val androidIntent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", searchQuery)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return ActionableIntent(
            type = IntentType.YOUTUBE,
            rawQuery = rawQuery,
            actionName = "play_youtube",
            summary = "Play '$searchQuery' on YouTube",
            confirmationSpeech = confirmationMsg,
            parameters = mapOf("query" to searchQuery),
            androidIntent = androidIntent,
            executionLambda = { ctx ->
                try {
                    ctx.startActivity(androidIntent)
                    IntentExecutionResult.Success(confirmationMsg, androidIntent)
                } catch (e: Exception) {
                    try {
                        val webIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.youtube.com/results?search_query=${URLEncoder.encode(searchQuery, "UTF-8")}")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        ctx.startActivity(webIntent)
                        IntentExecutionResult.Success(confirmationMsg, webIntent)
                    } catch (err: Exception) {
                        IntentExecutionResult.Failure("Failed to play YouTube video: ${err.message}")
                    }
                }
            }
        )
    }

    /**
     * Map Navigation Intent ("navigate to Connaught Place", "directions to Airport", "Delhi ka rasta dikhao")
     */
    private fun mapNavigationIntent(rawQuery: String, lower: String): ActionableIntent? {
        val isNavQuery = lower.contains("navigate") || lower.contains("directions") ||
                lower.contains("rasta dikhao") || lower.contains("rasta batao") ||
                lower.contains("take me to") || lower.contains("route to")

        if (!isNavQuery) return null

        val dest = rawQuery
            .replace(Regex("(?i)^(navigate to|directions to|route to|take me to|rasta dikhao|rasta batao)\\s*"), "")
            .replace(Regex("(?i)\\b(ka rasta dikhao|ka rasta batao|le chalo|tak rasta)\\b"), "")
            .trim()

        val destination = if (dest.isNotBlank()) dest else "Current Location"
        val encodedDest = URLEncoder.encode(destination, "UTF-8")
        val uri = Uri.parse("google.navigation:q=$encodedDest")

        val androidIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val confirmationMsg = "$destination ke liye navigation start ho raha hai! 🧭"

        return ActionableIntent(
            type = IntentType.NAVIGATION,
            rawQuery = rawQuery,
            actionName = "navigate_location",
            summary = "Navigate to $destination",
            confirmationSpeech = confirmationMsg,
            parameters = mapOf("destination" to destination),
            androidIntent = androidIntent,
            executionLambda = { ctx ->
                try {
                    ctx.startActivity(androidIntent)
                    IntentExecutionResult.Success(confirmationMsg, androidIntent)
                } catch (e: Exception) {
                    try {
                        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encodedDest")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        ctx.startActivity(fallback)
                        IntentExecutionResult.Success(confirmationMsg, fallback)
                    } catch (err: Exception) {
                        IntentExecutionResult.Failure("Maps navigation failed: ${err.message}")
                    }
                }
            }
        )
    }

    /**
     * Map App Launch Intent ("open YouTube", "launch Camera", "open Settings", "open Calculator")
     */
    private fun mapAppLaunchIntent(rawQuery: String, lower: String, context: Context): ActionableIntent? {
        val isOpenCommand = lower.startsWith("open") || lower.startsWith("launch") ||
                lower.startsWith("start") || lower.endsWith("kholo") || lower.endsWith("chalao")

        if (!isOpenCommand) return null

        val appTarget = lower
            .replace(Regex("(?i)^(open|launch|start|kholo|chalao)\\s*"), "")
            .replace(Regex("(?i)\\s*(kholo|chalao|open karo|launch karo)$"), "")
            .trim()

        if (appTarget.isBlank()) return null

        val matchedEntry = KNOWN_APPS.entries.firstOrNull { appTarget.contains(it.key) || it.key.contains(appTarget) }
        val pkgOrAction = matchedEntry?.value ?: appTarget

        val androidIntent: Intent = when (pkgOrAction) {
            "android.media.action.IMAGE_CAPTURE" -> Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            "android.settings.SETTINGS" -> Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            else -> context.packageManager.getLaunchIntentForPackage(pkgOrAction)?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(appTarget, "UTF-8")}")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        }

        val confirmationMsg = "$appTarget open kar diya hai! 📱"

        return ActionableIntent(
            type = IntentType.APP_LAUNCH,
            rawQuery = rawQuery,
            actionName = "open_app",
            summary = "Open App: $appTarget",
            confirmationSpeech = confirmationMsg,
            parameters = mapOf("appName" to appTarget, "packageOrAction" to pkgOrAction),
            androidIntent = androidIntent,
            executionLambda = { ctx ->
                try {
                    ctx.startActivity(androidIntent)
                    IntentExecutionResult.Success(confirmationMsg, androidIntent)
                } catch (e: Exception) {
                    IntentExecutionResult.Failure("Failed to launch $appTarget: ${e.message}")
                }
            }
        )
    }

    /**
     * Map System Control Intent (Flashlight, Volume, Battery, Wi-Fi)
     */
    private fun mapSystemControlIntent(rawQuery: String, lower: String, context: Context): ActionableIntent? {
        // Flashlight
        if (lower.contains("flashlight") || lower.contains("torch")) {
            val enable = !(lower.contains("off") || lower.contains("band") || lower.contains("disable"))
            val msg = if (enable) "Flashlight chalu kar di hai! 🔦" else "Flashlight band kar di hai! 🔦"
            return ActionableIntent(
                type = IntentType.SYSTEM_CONTROL,
                rawQuery = rawQuery,
                actionName = "toggle_flashlight",
                summary = if (enable) "Turn ON Flashlight" else "Turn OFF Flashlight",
                confirmationSpeech = msg,
                parameters = mapOf("enabled" to enable),
                androidIntent = null,
                executionLambda = { ctx ->
                    try {
                        val camManager = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                        val camId = camManager.cameraIdList.firstOrNull()
                        if (camId != null) {
                            camManager.setTorchMode(camId, enable)
                            IntentExecutionResult.Success(msg)
                        } else {
                            IntentExecutionResult.Failure("Camera torch not available")
                        }
                    } catch (e: Exception) {
                        IntentExecutionResult.Failure("Flashlight error: ${e.message}")
                    }
                }
            )
        }

        // Volume
        if (lower.contains("volume") || lower.contains("awaaz") || lower.contains("awaz")) {
            val isMute = lower.contains("mute") || lower.contains("silent") || lower.contains("band")
            val isUp = lower.contains("up") || lower.contains("increase") || lower.contains("badhao") || lower.contains("tez")
            val isDown = lower.contains("down") || lower.contains("decrease") || lower.contains("kam") || lower.contains("dheeme")
            val summary = if (isMute) "Mute Volume" else if (isUp) "Increase Volume" else "Decrease Volume"
            val msg = if (isMute) "Volume mute kar diya hai! 🔇" else if (isUp) "Volume badha diya hai! 🔊" else "Volume kam kar diya hai! 🔉"

            return ActionableIntent(
                type = IntentType.SYSTEM_CONTROL,
                rawQuery = rawQuery,
                actionName = "adjust_volume",
                summary = summary,
                confirmationSpeech = msg,
                parameters = mapOf("isUp" to isUp, "isMute" to isMute),
                androidIntent = null,
                executionLambda = { ctx ->
                    try {
                        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        if (isMute) {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                        } else if (isUp) {
                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                        } else {
                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                        }
                        IntentExecutionResult.Success(msg)
                    } catch (e: Exception) {
                        IntentExecutionResult.Failure("Volume adjustment failed: ${e.message}")
                    }
                }
            )
        }

        // Battery
        if (lower.contains("battery") || lower.contains("charging")) {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = batteryManager.isCharging
            val status = if (isCharging) "charging ho rahi hai" else "charging par nahi hai"
            val msg = "Battery $capacity% hai aur $status! 🔋"

            return ActionableIntent(
                type = IntentType.SYSTEM_CONTROL,
                rawQuery = rawQuery,
                actionName = "check_battery",
                summary = "Battery Status: $capacity%",
                confirmationSpeech = msg,
                parameters = mapOf("capacity" to capacity, "isCharging" to isCharging),
                androidIntent = null,
                executionLambda = { _ -> IntentExecutionResult.Success(msg) }
            )
        }

        // Wi-Fi
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            val settingsIntent = Intent(Settings.ACTION_WIFI_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            val msg = "Wi-Fi settings open kar di hai! 📶"
            return ActionableIntent(
                type = IntentType.SYSTEM_CONTROL,
                rawQuery = rawQuery,
                actionName = "toggle_wifi",
                summary = "Open Wi-Fi Settings",
                confirmationSpeech = msg,
                parameters = emptyMap(),
                androidIntent = settingsIntent,
                executionLambda = { ctx ->
                    try {
                        ctx.startActivity(settingsIntent)
                        IntentExecutionResult.Success(msg, settingsIntent)
                    } catch (e: Exception) {
                        IntentExecutionResult.Failure("Wi-Fi settings launch failed: ${e.message}")
                    }
                }
            )
        }

        return null
    }

    /**
     * Map Web Search Intent ("search for quantum mechanics", "google search current weather")
     */
    private fun mapWebSearchIntent(rawQuery: String, lower: String): ActionableIntent? {
        val isSearch = lower.startsWith("search") || lower.startsWith("google") ||
                lower.startsWith("find") || lower.contains("search karo")

        if (!isSearch) return null

        val query = rawQuery
            .replace(Regex("(?i)^(search for|search|google search|google|find)\\s*"), "")
            .replace(Regex("(?i)\\bsearch karo\\b"), "")
            .trim()

        if (query.isBlank()) return null

        val androidIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val confirmationMsg = "Web search start kar diya hai for '$query'! 🔍"

        return ActionableIntent(
            type = IntentType.WEB_SEARCH,
            rawQuery = rawQuery,
            actionName = "web_search",
            summary = "Search Web: '$query'",
            confirmationSpeech = confirmationMsg,
            parameters = mapOf("query" to query),
            androidIntent = androidIntent,
            executionLambda = { ctx ->
                try {
                    ctx.startActivity(androidIntent)
                    IntentExecutionResult.Success(confirmationMsg, androidIntent)
                } catch (e: Exception) {
                    try {
                        val viewIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")
                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        ctx.startActivity(viewIntent)
                        IntentExecutionResult.Success(confirmationMsg, viewIntent)
                    } catch (err: Exception) {
                        IntentExecutionResult.Failure("Web search failed: ${err.message}")
                    }
                }
            }
        )
    }

    private fun cleanQueryForTarget(query: String, prefixes: List<String>): String {
        var result = query
        for (p in prefixes) {
            result = result.replace(Regex("(?i)\\b$p\\b"), " ")
        }
        return result.replace(Regex("\\s+"), " ").trim()
    }

    private fun extractSmsDetails(rawQuery: String, context: Context): Pair<String, String> {
        val clean = rawQuery.replace(Regex("(?i)^(hey\\s+)?(arushi|sam|assistant)\\s*"), "").trim()
        val toMatch = Regex("(?i)(?:send\\s+)?(?:sms|message|msg|text)\\s+(?:to\\s+)?([^,\\s]+(?:\\s+[^,\\s]+)?)(?:\\s+(?:saying|that|message)?\\s*(.*))?").find(clean)
        if (toMatch != null) {
            val recipient = toMatch.groupValues[1].trim()
            val body = toMatch.groupValues.getOrNull(2)?.trim() ?: ""
            return Pair(recipient, body)
        }
        val koMatch = Regex("(?i)([^,\\s]+(?:\\s+[^,\\s]+)?)\\s+ko\\s+(?:sms|message|msg|text)\\s*(?:bhejo|karo)?\\s*(.*)").find(clean)
        if (koMatch != null) {
            val recipient = koMatch.groupValues[1].trim()
            val body = koMatch.groupValues.getOrNull(2)?.trim() ?: ""
            return Pair(recipient, body)
        }
        return Pair("", clean)
    }

    private fun lookupContact(nameQuery: String, context: Context): Pair<String, String>? {
        return try {
            val hasPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) return null

            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$nameQuery%"),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0) ?: nameQuery
                    val number = cursor.getString(1) ?: ""
                    Pair(name, number)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns a registry of all supported natural language intent categories with example prompts.
     */
    fun getSupportedIntents(): List<SupportedIntentInfo> {
        return listOf(
            SupportedIntentInfo(
                type = IntentType.CALL,
                title = "Phone Calling",
                exampleQueries = listOf("call Mom", "dial 9876543210", "Papa ko call karo"),
                targetAndroidAction = Intent.ACTION_CALL
            ),
            SupportedIntentInfo(
                type = IntentType.ALARM,
                title = "Set Clock Alarm",
                exampleQueries = listOf("set alarm for 7:30 AM", "wake me up at 6 am", "alarm 7 baje ka lagao"),
                targetAndroidAction = AlarmClock.ACTION_SET_ALARM
            ),
            SupportedIntentInfo(
                type = IntentType.TIMER,
                title = "Countdown Timer",
                exampleQueries = listOf("set timer for 10 minutes", "5 minute ka timer lagao"),
                targetAndroidAction = AlarmClock.ACTION_SET_TIMER
            ),
            SupportedIntentInfo(
                type = IntentType.SMS,
                title = "Send SMS Message",
                exampleQueries = listOf("send SMS to Rahul hello", "Mom ko SMS bhejo on the way"),
                targetAndroidAction = Intent.ACTION_SENDTO
            ),
            SupportedIntentInfo(
                type = IntentType.WHATSAPP,
                title = "WhatsApp Chat",
                exampleQueries = listOf("WhatsApp Mom reached home", "open WhatsApp"),
                targetAndroidAction = Intent.ACTION_VIEW
            ),
            SupportedIntentInfo(
                type = IntentType.APP_LAUNCH,
                title = "Launch Application",
                exampleQueries = listOf("open YouTube", "open Camera", "open Calculator", "open Settings"),
                targetAndroidAction = Intent.ACTION_MAIN
            ),
            SupportedIntentInfo(
                type = IntentType.YOUTUBE,
                title = "YouTube Playback",
                exampleQueries = listOf("play Believer on YouTube", "Arijit Singh song chalao"),
                targetAndroidAction = Intent.ACTION_SEARCH
            ),
            SupportedIntentInfo(
                type = IntentType.NAVIGATION,
                title = "Turn-by-Turn GPS Navigation",
                exampleQueries = listOf("navigate to Airport", "directions to Central Mall"),
                targetAndroidAction = Intent.ACTION_VIEW
            ),
            SupportedIntentInfo(
                type = IntentType.SYSTEM_CONTROL,
                title = "Hardware & Settings Controls",
                exampleQueries = listOf("turn on flashlight", "mute volume", "check battery", "wifi settings"),
                targetAndroidAction = "System Services"
            ),
            SupportedIntentInfo(
                type = IntentType.WEB_SEARCH,
                title = "Web Search",
                exampleQueries = listOf("search for quantum computing", "google search today weather"),
                targetAndroidAction = Intent.ACTION_WEB_SEARCH
            )
        )
    }
}
