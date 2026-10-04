package com.example.device

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

sealed class SamToolResult(open val message: String) {
    data class Success(override val message: String, val data: Map<String, Any?> = emptyMap()) : SamToolResult(message)
    data class Failure(override val message: String, val error: String) : SamToolResult(message)
}

class SamTools(private val context: Context) {

    companion object {
        private const val TAG = "SamTools"

        fun getSamToolDeclarationsJson(): JSONArray {
            val funcs = JSONArray()

            // 1. open_app
            funcs.put(JSONObject().apply {
                put("name", "open_app")
                put("description", "Launches any installed app on the user's Android phone (e.g. YouTube, WhatsApp, Camera, Settings, Gallery, Calculator, Spotify, Chrome, Maps, Instagram).")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("appName", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Name of the application to open (e.g. YouTube, WhatsApp, Camera, Settings, Gallery, Spotify, Maps, Calculator).")
                        })
                    })
                    put("required", JSONArray().put("appName"))
                })
            })

            // 2. play_youtube
            funcs.put(JSONObject().apply {
                put("name", "play_youtube")
                put("description", "Searches and plays a song, music video, track, or artist directly on YouTube (e.g. 'Arijit Singh song chalao', 'play Believer on YouTube').")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("searchQuery", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Song name, artist, music track, or video title to search and play on YouTube.")
                        })
                    })
                    put("required", JSONArray().put("searchQuery"))
                })
            })

            // 2. send_whatsapp
            funcs.put(JSONObject().apply {
                put("name", "send_whatsapp")
                put("description", "Opens WhatsApp chat or share screen with a pre-filled message text to a contact or phone number.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("contactName", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Recipient contact name or phone number.")
                        })
                        put("message", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "The message text to send.")
                        })
                    })
                    put("required", JSONArray().put("message"))
                })
            })

            // 3. set_timer_or_alarm
            funcs.put(JSONObject().apply {
                put("name", "set_timer_or_alarm")
                put("description", "Sets a native Android countdown timer or system clock alarm with a specified duration/time and label.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("timeMinutes", JSONObject().apply {
                            put("type", "INTEGER")
                            put("description", "Duration in minutes (for timer) or minute of the hour (for alarm).")
                        })
                        put("hour", JSONObject().apply {
                            put("type", "INTEGER")
                            put("description", "Hour in 24-hour format (0-23) if setting a specific alarm time.")
                        })
                        put("label", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Title or description for the alarm/timer.")
                        })
                        put("isAlarm", JSONObject().apply {
                            put("type", "BOOLEAN")
                            put("description", "Set true for a specific alarm time, or false for a countdown timer.")
                        })
                    })
                    put("required", JSONArray().put("label"))
                })
            })

            // 4. device_status
            funcs.put(JSONObject().apply {
                put("name", "device_status")
                put("description", "Checks device hardware status: Battery percentage & charging state, Wi-Fi connection status & SSID, available internal storage, screen brightness, and audio volume.")
            })

            // 5. generate_image (Imagen 3)
            funcs.put(JSONObject().apply {
                put("name", "generate_image")
                put("description", "Generates an AI photo, illustration, wallpaper or artwork using Google Imagen 3. Call whenever the user asks for pictures, photos, or visual generation (e.g. 'photo do', 'billi ki image banao').")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("prompt", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Detailed descriptive visual prompt in English for Imagen 3.")
                        })
                        put("aspectRatio", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Aspect ratio: '1:1', '16:9', '9:16', '4:3', or '3:4'. Default is '1:1'.")
                        })
                    })
                    put("required", JSONArray().put("prompt"))
                })
            })

            // 6. toggle_flashlight
            funcs.put(JSONObject().apply {
                put("name", "toggle_flashlight")
                put("description", "Toggles the phone hardware torch/flashlight ON or OFF.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("enable", JSONObject().apply {
                            put("type", "BOOLEAN")
                            put("description", "True to turn flashlight on, false to turn off.")
                        })
                    })
                    put("required", JSONArray().put("enable"))
                })
            })

            // 7. adjust_volume
            funcs.put(JSONObject().apply {
                put("name", "adjust_volume")
                put("description", "Controls media volume: UP, DOWN, MAX, MUTE, or SET level.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("action", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Action: UP, DOWN, MAX, MUTE, or SET.")
                        })
                        put("level", JSONObject().apply {
                            put("type", "INTEGER")
                            put("description", "Percentage level 0-100 (if action is SET).")
                        })
                    })
                    put("required", JSONArray().put("action"))
                })
            })

            // 8. make_call
            funcs.put(JSONObject().apply {
                put("name", "make_call")
                put("description", "Dials a phone number or opens dialer for a contact.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("phoneNumber", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Phone number to dial.")
                        })
                    })
                    put("required", JSONArray().put("phoneNumber"))
                })
            })

            // 9. send_sms
            funcs.put(JSONObject().apply {
                put("name", "send_sms")
                put("description", "Sends an SMS text message or opens the native SMS app composer with pre-filled recipient and message text. Use whenever the user asks to send an SMS, text someone, or message a contact (e.g. 'Rahul ko SMS bhejo', 'send message to 9876543210', 'text mom I will be late').")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("recipient", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Recipient contact name (e.g. 'Rahul', 'Mom', 'Doctor') or phone number (e.g. '+919876543210', '9876543210').")
                        })
                        put("message", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "SMS text body content to send.")
                        })
                    })
                    put("required", JSONArray().put("message"))
                })
            })

            // 10. toggle_wifi
            funcs.put(JSONObject().apply {
                put("name", "toggle_wifi")
                put("description", "Toggles the device Wi-Fi state or opens the system quick Wi-Fi / Internet connectivity control panel. Call when user asks to turn Wi-Fi on or off, toggle Wi-Fi, or check/manage Wi-Fi settings (e.g. 'Wi-Fi on karo', 'turn off wifi', 'toggle wifi').")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("enable", JSONObject().apply {
                            put("type", "BOOLEAN")
                            put("description", "True to turn Wi-Fi ON, false to turn Wi-Fi OFF, or omit to toggle / open the connectivity panel.")
                        })
                    })
                })
            })

            // 11. set_alarm
            funcs.put(JSONObject().apply {
                put("name", "set_alarm")
                put("description", "Sets a system clock alarm for a specified time and label on the user's Android phone. Call whenever the user asks to set an alarm, wake them up, or alert at a specific time (e.g. 'subah 7 baje ka alarm lagao', 'set alarm for 6:30 AM', 'wake me up at 8 AM', '7 baje alarm set karo').")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("hour", JSONObject().apply {
                            put("type", "INTEGER")
                            put("description", "Hour in 24-hour format (0-23) or 1-12 if amPm is provided.")
                        })
                        put("minute", JSONObject().apply {
                            put("type", "INTEGER")
                            put("description", "Minute of the hour (0-59). Defaults to 0.")
                        })
                        put("amPm", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Optional AM or PM designation ('AM' or 'PM') for 12-hour format.")
                        })
                        put("timeString", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Optional raw time string like '7:30 AM', '06:00', or '8 PM' to parse automatically.")
                        })
                        put("label", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Label, description, or purpose of the alarm (e.g. 'Morning Workout', 'Wake up', 'Medicines').")
                        })
                        put("skipUi", JSONObject().apply {
                            put("type", "BOOLEAN")
                            put("description", "True to set alarm directly in the background (default true), or false to display the clock app.")
                        })
                    })
                    put("required", JSONArray().put("hour"))
                })
            })

            return funcs
        }
    }

    /**
     * Executes native tool actions dispatched by Gemini Tool Calls
     */
    fun execute(toolName: String, args: Map<String, Any?>): SamToolResult {
        val normalizedName = toolName.trim()
        Log.d(TAG, "Executing native tool: $normalizedName with args: $args")

        return when (normalizedName) {
            "open_app", "openApp", "launchApp" -> {
                val appName = (args["appName"] ?: args["name"] ?: args["app"] ?: "").toString().trim()
                openApp(appName)
            }

            "play_youtube", "playSongOnYouTube", "playSong", "playMusic", "play_song", "search_youtube", "youtubeSearch" -> {
                val query = (args["searchQuery"] ?: args["query"] ?: args["song"] ?: args["songName"] ?: args["title"] ?: args["name"] ?: "").toString().trim()
                playSongOnYouTube(context, query)
            }

            "send_whatsapp", "sendWhatsAppMessage", "openWhatsApp", "sendWhatsApp" -> {
                val contact = (args["contactName"] ?: args["recipient"] ?: args["phone"] ?: "").toString().trim()
                val message = (args["message"] ?: args["text"] ?: "Hello from Sam AI!").toString().trim()
                sendWhatsApp(contact, message)
            }

            "set_timer_or_alarm", "setAlarm", "setTimer", "createAlarm" -> {
                val timeMinutes = (args["timeMinutes"] as? Number)?.toInt()
                    ?: (args["minutes"] as? Number)?.toInt()
                    ?: (args["minute"] as? Number)?.toInt()
                val hour = (args["hour"] as? Number)?.toInt()
                val label = (args["label"] ?: args["message"] ?: "Sam AI Alert").toString().trim()
                val isAlarm = (args["isAlarm"] as? Boolean) ?: (hour != null)
                setTimerOrAlarm(timeMinutes, hour, label, isAlarm)
            }

            "device_status", "getBatteryStatus", "getDeviceStatus", "checkStatus" -> {
                getDeviceStatus()
            }

            "toggle_flashlight", "toggleFlashlight", "flashlight" -> {
                val enable = (args["enable"] as? Boolean) ?: true
                toggleFlashlight(enable)
            }

            "adjust_volume", "adjustVolume", "setVolume" -> {
                val action = (args["action"] ?: "UP").toString().trim().uppercase()
                val level = (args["level"] as? Number)?.toInt()
                adjustVolume(action, level)
            }

            "make_call", "makeCall", "dialContact", "callContact" -> {
                val phone = (args["phoneNumber"] ?: args["phone"] ?: args["number"] ?: "").toString().trim()
                makeCall(phone)
            }

            "send_sms", "sendSms", "sms", "textMessage", "sendText", "send_message" -> {
                val recipient = (args["recipient"] ?: args["contact"] ?: args["phoneNumber"] ?: args["phone"] ?: args["to"] ?: "").toString().trim()
                val message = (args["message"] ?: args["text"] ?: args["body"] ?: "").toString().trim()
                sendSms(recipient, message)
            }

            "toggle_wifi", "toggleWifi", "wifi", "wifiSettings", "turnOnWifi", "turnOffWifi" -> {
                val enable = (args["enable"] as? Boolean) ?: when (normalizedName) {
                    "turnOnWifi" -> true
                    "turnOffWifi" -> false
                    else -> null
                }
                toggleWifi(enable)
            }

            "set_alarm", "setAlarm", "create_alarm", "createAlarm", "alarm", "addAlarm" -> {
                val hour = (args["hour"] as? Number)?.toInt()
                val minute = (args["minute"] as? Number)?.toInt()
                val amPm = args["amPm"]?.toString()
                val timeString = (args["timeString"] ?: args["time"] ?: args["at"])?.toString()
                val label = (args["label"] ?: args["message"] ?: args["title"] ?: "Sam AI Alert").toString().trim()
                val skipUi = (args["skipUi"] as? Boolean) ?: true
                val days = (args["days"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }
                setAlarm(hour, minute, label, amPm, timeString, skipUi, days)
            }

            else -> {
                SamToolResult.Failure("Unknown tool: $toolName", "Unsupported tool action")
            }
        }
    }

    fun openApp(appName: String): SamToolResult {
        if (appName.isBlank()) {
            return SamToolResult.Failure("App name was not provided", "Missing appName argument")
        }

        val clean = appName.lowercase().trim()
            .replace(Regex("(?i)^(open|launch|start|kholo|chalao)\\s*"), "")
            .replace(Regex("(?i)\\s*(kholo|chalao|open karo|launch karo)$"), "")
            .trim()

        // YouTube Interception (with search/song query extraction if present)
        if (clean.contains("youtube")) {
            val query = clean.replace("youtube", "").replace("par", "").replace("pe", "")
                .replace("play", "").replace("search", "").replace("karo", "").trim()
            return playSongOnYouTube(context, query)
        }

        // Direct package / intent mapping
        val packageMap = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "google" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "spotify" to "com.spotify.music",
            "calculator" to "com.google.android.calculator",
            "camera" to "android.media.action.IMAGE_CAPTURE",
            "settings" to "android.settings.SETTINGS",
            "gallery" to "com.google.android.apps.photos",
            "photos" to "com.google.android.apps.photos",
            "clock" to "com.google.android.deskclock",
            "alarm" to "com.google.android.deskclock"
        )

        val targetPackage = packageMap.entries.firstOrNull { clean.contains(it.key) || it.key.contains(clean) }?.value

        // Try launching by intent action (e.g. Camera, Settings)
        if (targetPackage == "android.media.action.IMAGE_CAPTURE") {
            return try {
                val camIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(camIntent)
                SamToolResult.Success("Camera open kar diya hai! 📷")
            } catch (e: Exception) {
                SamToolResult.Failure("Camera open karne mein error aaya", e.message ?: "")
            }
        }

        if (targetPackage == "android.settings.SETTINGS") {
            return try {
                val setIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(setIntent)
                SamToolResult.Success("Phone Settings open kar di hain! ⚙️")
            } catch (e: Exception) {
                SamToolResult.Failure("Settings open karne mein error aaya", e.message ?: "")
            }
        }

        // Try launch by specific package
        if (targetPackage != null) {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return SamToolResult.Success("$appName open kar diya hai! 🚀")
            }
        }

        // Search installed applications matching name
        return try {
            val pm = context.packageManager
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val match = installed.firstOrNull { appInfo ->
                val label = pm.getApplicationLabel(appInfo).toString().lowercase()
                label.contains(clean) || clean.contains(label)
            }

            if (match != null) {
                val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return SamToolResult.Success("$appName successfully open ho gaya hai! 🚀")
                }
            }

            // Web fallback
            val webUrl = "https://www.google.com/search?q=${URLEncoder.encode(appName, "UTF-8")}"
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            SamToolResult.Success("'$appName' ke liye web search open kar diya hai! 🌐")
        } catch (e: Exception) {
            SamToolResult.Failure("Failed to open '$appName': ${e.message}", e.message ?: "")
        }
    }

    fun playSongOnYouTube(context: Context, searchQuery: String): SamToolResult {
        val cleanQuery = searchQuery
            .replace(Regex("(?i)(play|chalao|suno|baja|sunao|gana|song|music|on youtube|youtube par|youtube pe|kholo|search|karo|do|lagao|pe|par)"), "")
            .trim()

        val effectiveQuery = if (cleanQuery.isNotBlank()) cleanQuery else searchQuery.trim().ifEmpty { "trending music" }
        val encodedQuery = URLEncoder.encode(effectiveQuery, "UTF-8")
        val url = "https://www.youtube.com/results?search_query=$encodedQuery"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            setPackage("com.google.android.youtube")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            SamToolResult.Success("YouTube par $effectiveQuery chala raha hoon! 🎵")
        } catch (e: ActivityNotFoundException) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                SamToolResult.Success("YouTube par $effectiveQuery chala raha hoon! 🎵")
            } catch (ex: Exception) {
                SamToolResult.Failure("YouTube open karne mein error aaya", ex.message ?: "")
            }
        } catch (e: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                SamToolResult.Success("YouTube par $effectiveQuery chala raha hoon! 🎵")
            } catch (ex: Exception) {
                SamToolResult.Failure("YouTube open karne mein error aaya", ex.message ?: "")
            }
        }
    }

    fun sendWhatsApp(contactNameOrNumber: String, messageText: String): SamToolResult {
        return try {
            val digits = contactNameOrNumber.filter { it.isDigit() }
            val uriString = if (digits.length >= 7) {
                "https://api.whatsapp.com/send?phone=$digits&text=${URLEncoder.encode(messageText, "UTF-8")}"
            } else {
                "https://api.whatsapp.com/send?text=${URLEncoder.encode(messageText, "UTF-8")}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val targetStr = if (contactNameOrNumber.isNotBlank()) " to $contactNameOrNumber" else ""
            SamToolResult.Success("WhatsApp chat$targetStr open kar diya hai with pre-filled message! 💬")
        } catch (e: Exception) {
            SamToolResult.Failure("WhatsApp message dispatch failed", e.message ?: "")
        }
    }

    fun setTimerOrAlarm(timeMinutes: Int?, hour: Int?, label: String, isAlarm: Boolean): SamToolResult {
        return try {
            if (isAlarm && hour != null) {
                val targetMinute = timeMinutes ?: 0
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                    putExtra(AlarmClock.EXTRA_MINUTES, targetMinute.coerceIn(0, 59))
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                SamToolResult.Success("Alarm set ho gaya hai for %02d:%02d ('$label')! ⏰".format(hour, targetMinute))
            } else {
                val seconds = ((timeMinutes ?: 5) * 60).coerceAtLeast(10)
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                SamToolResult.Success("${timeMinutes ?: 5} minutes ka countdown timer start ho gaya hai ('$label')! ⏱️")
            }
        } catch (e: Exception) {
            SamToolResult.Failure("Failed to set timer/alarm: ${e.message}", e.message ?: "")
        }
    }

    fun getDeviceStatus(): SamToolResult {
        return try {
            // 1. Battery
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = batteryManager.isCharging
            val chargeStatus = if (isCharging) "Charging ⚡" else "Discharging 🔋"

            // 2. Wi-Fi & Network
            val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connMgr.activeNetwork
            val capabilities = connMgr.getNetworkCapabilities(activeNetwork)
            val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val netStatus = when {
                isWifi -> "Wi-Fi Connected 📶"
                isCellular -> "Mobile Data (Cellular) 📱"
                capabilities != null -> "Connected 🌐"
                else -> "Offline ❌"
            }

            // 3. Storage
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val bytesTotal = stat.blockCountLong * stat.blockSizeLong
            val freeGb = bytesAvailable / (1024 * 1024 * 1024)
            val totalGb = bytesTotal / (1024 * 1024 * 1024)

            // 4. Audio Volume
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val volPct = (currentVol * 100) / maxVol.coerceAtLeast(1)

            // 5. Screen Brightness
            val brightnessVal = try {
                val b = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
                (b * 100) / 255
            } catch (se: Exception) {
                50
            }

            val statusReport = "📱 Device Status Report:\n• Battery: $batteryPct% ($chargeStatus)\n• Network: $netStatus\n• Storage: ${freeGb}GB Free / ${totalGb}GB Total\n• Brightness: $brightnessVal%\n• Volume: $volPct%"

            SamToolResult.Success(
                message = statusReport,
                data = mapOf(
                    "battery" to batteryPct,
                    "isCharging" to isCharging,
                    "network" to netStatus,
                    "freeStorageGb" to freeGb,
                    "totalStorageGb" to totalGb,
                    "brightnessPercent" to brightnessVal,
                    "volumePercent" to volPct
                )
            )
        } catch (e: Exception) {
            SamToolResult.Failure("Unable to retrieve device status", e.message ?: "")
        }
    }

    fun toggleFlashlight(enabled: Boolean): SamToolResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return SamToolResult.Failure("No camera flash available", "Missing hardware")
            cameraManager.setTorchMode(cameraId, enabled)
            val status = if (enabled) "ON 🔦" else "OFF"
            SamToolResult.Success("Flashlight turn $status kar di hai!")
        } catch (e: Exception) {
            SamToolResult.Failure("Flashlight toggle failed", e.message ?: "")
        }
    }

    fun adjustVolume(direction: String, levelPercent: Int?): SamToolResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

            val msg = when (direction.uppercase()) {
                "UP" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    val currentPercent = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / maxVolume
                    "Volume increased to $currentPercent% 🔊"
                }
                "DOWN" -> {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    val currentPercent = (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100) / maxVolume
                    "Volume decreased to $currentPercent% 🔉"
                }
                "MAX" -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_SHOW_UI)
                    "Volume set to maximum (100%) 🔊"
                }
                "MUTE" -> {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
                    "Volume muted (0%) 🔇"
                }
                "SET" -> {
                    if (levelPercent != null) {
                        val targetVol = (levelPercent.coerceIn(0, 100) * maxVolume) / 100
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI)
                        "Volume set to $levelPercent% 🎚️"
                    } else {
                        "Specific volume level not provided"
                    }
                }
                else -> "Volume adjusted"
            }
            SamToolResult.Success(msg)
        } catch (e: Exception) {
            SamToolResult.Failure("Volume adjustment failed", e.message ?: "")
        }
    }

    fun makeCall(phoneNumber: String): SamToolResult {
        return try {
            val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            SamToolResult.Success("Dialer opened for $cleanPhone 📞")
        } catch (e: Exception) {
            SamToolResult.Failure("Failed to initiate call", e.message ?: "")
        }
    }

    fun sendSms(recipient: String, messageText: String): SamToolResult {
        val cleanRecipient = recipient.trim()
        val cleanMsg = messageText.trim()
        val digits = cleanRecipient.filter { it.isDigit() || it == '+' }
        var targetPhoneNumber = digits
        var recipientDisplayName = cleanRecipient

        if (digits.length < 7 && cleanRecipient.isNotBlank() && cleanRecipient.any { it.isLetter() }) {
            val resolvedNum = lookupContactNumber(cleanRecipient)
            if (resolvedNum != null) {
                targetPhoneNumber = resolvedNum
                recipientDisplayName = "$cleanRecipient ($resolvedNum)"
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
            val info = if (recipientDisplayName.isNotBlank()) " for $recipientDisplayName" else ""
            val preview = if (cleanMsg.length > 40) cleanMsg.take(37) + "..." else cleanMsg
            SamToolResult.Success(
                message = "SMS composer open kar diya hai$info: \"$preview\"! ✉️",
                data = mapOf(
                    "recipient" to recipientDisplayName,
                    "phoneNumber" to targetPhoneNumber,
                    "message" to cleanMsg
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open SMS composer", e)
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    type = "vnd.android-dir/mms-sms"
                    if (targetPhoneNumber.isNotBlank()) {
                        putExtra("address", targetPhoneNumber)
                    }
                    if (cleanMsg.isNotBlank()) {
                        putExtra("sms_body", cleanMsg)
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                SamToolResult.Success("SMS app open kar diya hai! ✉️")
            } catch (err: Exception) {
                SamToolResult.Failure("SMS app open karne mein error aaya: ${err.message ?: "Unknown error"}", err.message ?: "")
            }
        }
    }

    fun toggleWifi(enable: Boolean?): SamToolResult {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val isCurrentlyConnected = cm?.activeNetwork?.let { net ->
                cm.getNetworkCapabilities(net)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            } ?: false

            val desiredAction = when (enable) {
                true -> "ON"
                false -> "OFF"
                null -> if (isCurrentlyConnected) "OFF" else "ON"
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Modern Android (API 29+) uses Settings Panel for zero-permission user Wi-Fi control
                val panelIntent = try {
                    Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } catch (e: Throwable) {
                    try {
                        Intent(Settings.Panel.ACTION_WIFI).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    } catch (e2: Throwable) {
                        Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                }

                try {
                    context.startActivity(panelIntent)
                } catch (e: Exception) {
                    val fallback = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                }

                val currentStatusStr = if (isCurrentlyConnected) "currently connected 🟢" else "currently disconnected ⚪"
                val actionDesc = when (enable) {
                    true -> "Wi-Fi chalu (ON) karne ke liye"
                    false -> "Wi-Fi band (OFF) karne ke liye"
                    null -> "Wi-Fi toggle karne ke liye"
                }

                SamToolResult.Success(
                    message = "$actionDesc quick connectivity panel open kar diya hai ($currentStatusStr)! 📶",
                    data = mapOf(
                        "isCurrentlyConnected" to isCurrentlyConnected,
                        "desiredAction" to desiredAction
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                if (wifiManager != null) {
                    val newState = enable ?: !wifiManager.isWifiEnabled
                    @Suppress("DEPRECATION")
                    wifiManager.isWifiEnabled = newState
                    val stateStr = if (newState) "ON 📶" else "OFF"
                    SamToolResult.Success(
                        message = "Wi-Fi turn $stateStr kar diya hai!",
                        data = mapOf("wifiEnabled" to newState)
                    )
                } else {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    SamToolResult.Success("Wi-Fi settings open kar di hain! 📶")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Wi-Fi toggle failed", e)
            try {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                SamToolResult.Success("Wi-Fi settings open kar di hain! 📶")
            } catch (err: Exception) {
                SamToolResult.Failure("Wi-Fi settings open karne mein error aaya: ${err.message ?: "Unknown error"}", err.message ?: "")
            }
        }
    }

    fun toggleBluetooth(enable: Boolean?): SamToolResult {
        return try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null) {
                return SamToolResult.Failure("Bluetooth toggle failed", "Bluetooth feature not supported on this device.")
            }
            val desiredState = enable ?: !bluetoothAdapter.isEnabled
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                // Since Android 12 (API 31+), direct connect permissions are required. We'll attempt a direct toggle, but fall back to settings panel or general settings.
                try {
                    if (desiredState) {
                        @Suppress("DEPRECATION")
                        bluetoothAdapter.enable()
                    } else {
                        @Suppress("DEPRECATION")
                        bluetoothAdapter.disable()
                    }
                    SamToolResult.Success("Bluetooth ko successfully ${if (desiredState) "ON 🔵" else "OFF ⚪"} kar diya hai!")
                } catch (se: SecurityException) {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    SamToolResult.Success("Bluetooth settings open kar di hain taaki aap isse toggle kar sakein! 📶")
                }
            } else {
                if (desiredState) {
                    @Suppress("DEPRECATION")
                    bluetoothAdapter.enable()
                } else {
                    @Suppress("DEPRECATION")
                    bluetoothAdapter.disable()
                }
                SamToolResult.Success("Bluetooth ko successfully ${if (desiredState) "ON 🔵" else "OFF ⚪"} kar diya hai!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth toggle failed", e)
            try {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                SamToolResult.Success("Bluetooth settings open kar di hain! 📶")
            } catch (err: Exception) {
                SamToolResult.Failure("Bluetooth settings failure: ${err.message}", err.message ?: "")
            }
        }
    }

    fun setAlarm(
        hour: Int? = null,
        minute: Int? = null,
        label: String = "Alarm",
        amPm: String? = null,
        timeString: String? = null,
        skipUi: Boolean = true,
        days: List<Int>? = null
    ): SamToolResult {
        return try {
            val (parsedHour, parsedMinute) = parseHourAndMinute(hour, minute, amPm, timeString)
            val safeHour = parsedHour.coerceIn(0, 23)
            val safeMinute = parsedMinute.coerceIn(0, 59)
            val effectiveLabel = label.ifBlank { "Sam AI Alarm" }

            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, safeHour)
                putExtra(AlarmClock.EXTRA_MINUTES, safeMinute)
                putExtra(AlarmClock.EXTRA_MESSAGE, effectiveLabel)
                putExtra(AlarmClock.EXTRA_SKIP_UI, skipUi)
                if (!days.isNullOrEmpty()) {
                    putIntegerArrayListExtra(AlarmClock.EXTRA_DAYS, ArrayList(days))
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "ACTION_SET_ALARM failed, trying ACTION_SHOW_ALARMS fallback", e)
                try {
                    val showIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(showIntent)
                } catch (fallbackErr: Exception) {
                    val clockIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.deskclock")
                        ?: context.packageManager.getLaunchIntentForPackage("com.sec.android.app.clockpackage")
                    if (clockIntent != null) {
                        clockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(clockIntent)
                    } else {
                        throw fallbackErr
                    }
                }
            }

            val amPmIndicator = if (safeHour >= 12) "PM" else "AM"
            val displayHour12 = when (val h = safeHour % 12) { 0 -> 12 else -> h }
            val formattedTime = String.format("%02d:%02d", safeHour, safeMinute)
            val formatted12h = String.format("%d:%02d %s", displayHour12, safeMinute, amPmIndicator)

            SamToolResult.Success(
                message = "Alarm set ho gaya hai for $formatted12h ($formattedTime) ('$effectiveLabel')! ⏰",
                data = mapOf(
                    "hour24" to safeHour,
                    "minute" to safeMinute,
                    "formattedTime24" to formattedTime,
                    "formattedTime12" to formatted12h,
                    "label" to effectiveLabel
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set alarm", e)
            SamToolResult.Failure("Alarm set karne mein error aaya: ${e.message ?: "Unknown error"}", e.message ?: "")
        }
    }

    private fun parseHourAndMinute(
        hour: Int?,
        minute: Int?,
        amPm: String?,
        timeString: String?
    ): Pair<Int, Int> {
        if (!timeString.isNullOrBlank()) {
            val clean = timeString.trim().uppercase()
            val isPm = clean.contains("PM") || clean.contains("SHAM") || clean.contains("RAAT")
            val isAm = clean.contains("AM") || clean.contains("SUBAH") || clean.contains("DOPAHAR")
            val digitsOnly = clean.replace(Regex("[^0-9:]"), "").trim()
            if (digitsOnly.contains(":")) {
                val parts = digitsOnly.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val adjustedH = when {
                    isPm && h < 12 -> h + 12
                    isAm && h == 12 -> 0
                    else -> h
                }
                return Pair(adjustedH, m)
            } else if (digitsOnly.isNotEmpty()) {
                val h = digitsOnly.toIntOrNull() ?: 7
                val adjustedH = when {
                    isPm && h < 12 -> h + 12
                    isAm && h == 12 -> 0
                    else -> h
                }
                return Pair(adjustedH, minute ?: 0)
            }
        }

        var h = hour ?: 7
        val m = minute ?: 0
        if (!amPm.isNullOrBlank()) {
            val upperAmPm = amPm.trim().uppercase()
            if (upperAmPm == "PM" && h < 12) {
                h += 12
            } else if (upperAmPm == "AM" && h == 12) {
                h = 0
            }
        }
        return Pair(h, m)
    }

    private fun lookupContactNumber(nameQuery: String): String? {
        val cleanQuery = nameQuery.trim()
        if (cleanQuery.isBlank()) return null
        return try {
            val contentResolver = context.contentResolver
            val uri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$cleanQuery%")

            contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val numIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (cursor.moveToFirst() && numIdx != -1) {
                    cursor.getString(numIdx)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lookup contact '$nameQuery'", e)
            null
        }
    }
}
