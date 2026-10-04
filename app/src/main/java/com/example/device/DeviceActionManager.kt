package com.example.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

class DeviceActionManager(private val context: Context) {

    companion object {
        private const val TAG = "SamDeviceActions"

        // Safe allowlist of common applications mapped to known package names or intents
        private val ALLOWED_APPS = mapOf(
            "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
            "youtube" to listOf("com.google.android.youtube"),
            "instagram" to listOf("com.instagram.android"),
            "maps" to listOf("com.google.android.apps.maps"),
            "google maps" to listOf("com.google.android.apps.maps"),
            "chrome" to listOf("com.android.chrome"),
            "browser" to listOf("com.android.chrome"),
            "camera" to listOf("android.media.action.IMAGE_CAPTURE"),
            "calculator" to listOf("com.google.android.calculator", "com.android.calculator2"),
            "settings" to listOf("android.settings.SETTINGS"),
            "clock" to listOf("com.google.android.deskclock", "com.android.deskclock"),
            "alarm" to listOf("com.google.android.deskclock", "com.android.deskclock"),
            "spotify" to listOf("com.spotify.music"),
            "phone" to listOf("android.intent.action.DIAL"),
            "dialer" to listOf("android.intent.action.DIAL")
        )
    }

    /**
     * Foolproof application opener with native package launch and web/URI fallback
     */
    fun launchApp(appName: String): Boolean {
        val cleanName = appName.trim().lowercase()
            .replace(Regex("(?i)^(open|launch|start|kholo|chalao)\\s*"), "")
            .replace(Regex("(?i)\\s*(kholo|chalao|open karo|launch karo)$"), "")
            .trim()

        // Intercept YouTube Search & Play queries
        if (cleanName.contains("youtube") && cleanName.length > 7) {
            val query = cleanName
                .replace("youtube", "")
                .replace("par", "")
                .replace("pe", "")
                .replace("play", "")
                .replace("search", "")
                .replace("karo", "")
                .replace("chalao", "")
                .replace(Regex("\\s+"), " ")
                .trim()
            
            if (query.isNotEmpty()) {
                Log.d(TAG, "Intercepted YouTube query: '$query'")
                return try {
                    val appIntent = Intent(Intent.ACTION_SEARCH).apply {
                        setPackage("com.google.android.youtube")
                        putExtra("query", query)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(appIntent)
                    true
                } catch (e: Exception) {
                    try {
                        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                        val webIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(webIntent)
                        true
                    } catch (err: Exception) {
                        false
                    }
                }
            }
        }

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
            "camera" to "android.media.action.IMAGE_CAPTURE"
        )

        val matchedKey = packageMap.keys.firstOrNull { cleanName.contains(it) || it.contains(cleanName) } ?: cleanName
        val pkg = packageMap[matchedKey]

        return try {
            if (pkg == "android.media.action.IMAGE_CAPTURE") {
                val camIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(camIntent)
                return true
            }

            val launchIntent = pkg?.let { context.packageManager.getLaunchIntentForPackage(it) }
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                // Web/URI Fallback
                val fallbackUri = when (matchedKey) {
                    "youtube" -> Uri.parse("https://www.youtube.com")
                    "whatsapp" -> Uri.parse("whatsapp://send")
                    "instagram" -> Uri.parse("https://www.instagram.com")
                    "maps", "google maps" -> Uri.parse("https://maps.google.com")
                    "spotify" -> Uri.parse("https://open.spotify.com")
                    "chrome", "browser", "google" -> Uri.parse("https://www.google.com")
                    else -> Uri.parse("https://www.google.com/search?q=$cleanName")
                }
                val intent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "launchApp failed for $appName", e)
            false
        }
    }

    /**
     * Open WhatsApp directly or via deep link
     */
    fun openWhatsApp(): DeviceActionResult {
        Log.d(TAG, "Executing openWhatsApp action")
        val success = launchApp("whatsapp")
        return if (success) {
            Log.i(TAG, "WhatsApp opened successfully")
            DeviceActionResult.Success("openWhatsApp", "WhatsApp opened successfully")
        } else {
            DeviceActionResult.Failure("openWhatsApp", "Could not open WhatsApp")
        }
    }

    /**
     * Safely open an application with package launch and fallback
     */
    fun openApp(rawAppName: String): DeviceActionResult {
        Log.d(TAG, "Executing openApp for '$rawAppName'")
        val success = launchApp(rawAppName)
        return if (success) {
            Log.i(TAG, "Opened $rawAppName successfully")
            DeviceActionResult.Success("openApp", "Opened $rawAppName successfully")
        } else {
            DeviceActionResult.Failure("openApp", "Could not open $rawAppName")
        }
    }

    /**
     * Safely open a validated URL in the browser
     */
    fun openWebsite(rawUrl: String): DeviceActionResult {
        Log.d(TAG, "Executing openWebsite for '$rawUrl'")
        var cleanUrl = rawUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        return try {
            val uri = Uri.parse(cleanUrl)
            if (uri.scheme != "http" && uri.scheme != "https") {
                return DeviceActionResult.Failure("openWebsite", "Invalid URL scheme. Only HTTP and HTTPS are permitted.")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.i(TAG, "Opened website: $cleanUrl")
            DeviceActionResult.Success("openWebsite", "Opened $cleanUrl in browser")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open website $cleanUrl", e)
            DeviceActionResult.Failure("openWebsite", "Unable to open website: ${e.message}")
        }
    }

    /**
     * Initiate phone dialer with phone number
     */
    fun makeCall(rawPhoneNumber: String): DeviceActionResult {
        Log.d(TAG, "Executing makeCall for '$rawPhoneNumber'")
        val cleanNumber = rawPhoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isEmpty()) {
            return DeviceActionResult.Failure("makeCall", "Invalid phone number provided")
        }

        return try {
            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            val action = if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(action, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val actionMsg = if (hasCallPermission) "Calling $cleanNumber" else "Opening dialer for $cleanNumber"
            Log.i(TAG, actionMsg)
            DeviceActionResult.Success("makeCall", actionMsg)
        } catch (e: SecurityException) {
            Log.w(TAG, "CALL_PHONE permission denied at runtime, falling back to ACTION_DIAL", e)
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                DeviceActionResult.Success("makeCall", "Opening dialer for $cleanNumber")
            } catch (ex: Exception) {
                DeviceActionResult.Failure("makeCall", "Unable to open dialer: ${ex.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch call/dialer", e)
            DeviceActionResult.Failure("makeCall", "Could not start call: ${e.message}")
        }
    }

    /**
     * Search contacts by name and initiate call
     */
    fun callContact(contactName: String): DeviceActionResult {
        Log.d(TAG, "Executing callContact for query '$contactName'")
        val cleanQuery = contactName.trim()
        if (cleanQuery.isEmpty()) {
            return DeviceActionResult.Failure("callContact", "Please specify a contact name")
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "READ_CONTACTS permission is required to search contacts")
            return DeviceActionResult.PermissionRequired(
                android.Manifest.permission.READ_CONTACTS,
                "Permission needed to access contacts to call $cleanQuery"
            )
        }

        val matchingContacts = searchContacts(cleanQuery)
        Log.d(TAG, "Found ${matchingContacts.size} contacts matching '$cleanQuery'")

        return when {
            matchingContacts.isEmpty() -> {
                DeviceActionResult.ContactNotFound(cleanQuery)
            }
            matchingContacts.size == 1 -> {
                val contact = matchingContacts.first()
                makeCall(contact.phoneNumber)
                DeviceActionResult.ContactFound(
                    contact,
                    "Calling ${contact.name} (${contact.phoneNumber})"
                )
            }
            else -> {
                // Multiple matching contacts: Never guess! Let Sam ask the user
                DeviceActionResult.MultipleContactsFound(matchingContacts, cleanQuery)
            }
        }
    }

    /**
     * Send SMS or open SMS composer with recipient and message
     */
    fun sendSms(rawRecipient: String, rawMessage: String): DeviceActionResult {
        Log.d(TAG, "Executing sendSms for '$rawRecipient' with message '$rawMessage'")
        val cleanQuery = rawRecipient.trim()
        val cleanMsg = rawMessage.trim()

        val digits = cleanQuery.filter { it.isDigit() || it == '+' }
        var targetPhoneNumber = digits
        var recipientDisplayName = cleanQuery

        // If not a pure phone number, try to resolve via contacts
        if (digits.length < 7 && cleanQuery.isNotBlank() && cleanQuery.any { it.isLetter() }) {
            val contacts = searchContacts(cleanQuery)
            if (contacts.isNotEmpty()) {
                targetPhoneNumber = contacts.first().phoneNumber
                recipientDisplayName = "${contacts.first().name} ($targetPhoneNumber)"
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
            val successMsg = if (targetPhoneNumber.isNotBlank()) {
                "SMS composer open kar diya hai for $recipientDisplayName! ✉️"
            } else {
                "SMS composer open kar diya hai! ✉️"
            }
            Log.i(TAG, successMsg)
            DeviceActionResult.Success("sendSms", successMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open SMS composer", e)
            try {
                // Generic send fallback
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    type = "vnd.android-dir/mms-sms"
                    if (cleanMsg.isNotBlank()) putExtra("sms_body", cleanMsg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                DeviceActionResult.Success("sendSms", "SMS app open kar diya hai! ✉️")
            } catch (err: Exception) {
                DeviceActionResult.Failure("sendSms", "Could not open SMS: ${e.message}")
            }
        }
    }

    fun searchContacts(query: String): List<ContactItem> {
        val results = mutableListOf<ContactItem>()
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        try {
            contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext() && results.size < 10) {
                    val id = if (idIdx != -1) cursor.getString(idIdx) ?: "" else ""
                    val name = if (nameIdx != -1) cursor.getString(nameIdx) ?: "" else ""
                    val number = if (numIdx != -1) cursor.getString(numIdx) ?: "" else ""
                    if (name.isNotBlank() && number.isNotBlank()) {
                        results.add(ContactItem(id = id, name = name, phoneNumber = number))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts", e)
        }

        return results.distinctBy { it.phoneNumber }
    }
}
