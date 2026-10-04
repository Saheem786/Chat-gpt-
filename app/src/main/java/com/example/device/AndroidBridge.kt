package com.example.device

import android.webkit.JavascriptInterface
import org.json.JSONObject

class AndroidBridge(
    private val actionManager: DeviceActionManager,
    private val onLog: (String) -> Unit
) {
    @JavascriptInterface
    fun openWhatsApp(): String {
        val result = actionManager.openWhatsApp()
        val success = result is DeviceActionResult.Success
        val msg = if (result is DeviceActionResult.Success) result.message else (result as? DeviceActionResult.Failure)?.error ?: ""
        return JSONObject().apply {
            put("success", success)
            put("action", "openWhatsApp")
            put("message", msg)
        }.toString()
    }

    @JavascriptInterface
    fun openApp(appName: String): String {
        val result = actionManager.openApp(appName)
        val success = result is DeviceActionResult.Success
        val msg = if (result is DeviceActionResult.Success) result.message else (result as? DeviceActionResult.Failure)?.error ?: ""
        return JSONObject().apply {
            put("success", success)
            put("action", "openApp")
            put("appName", appName)
            put("message", msg)
        }.toString()
    }

    @JavascriptInterface
    fun openWebsite(url: String): String {
        val result = actionManager.openWebsite(url)
        val success = result is DeviceActionResult.Success
        val msg = if (result is DeviceActionResult.Success) result.message else (result as? DeviceActionResult.Failure)?.error ?: ""
        return JSONObject().apply {
            put("success", success)
            put("action", "openWebsite")
            put("url", url)
            put("message", msg)
        }.toString()
    }

    @JavascriptInterface
    fun makeCall(phoneNumber: String): String {
        val result = actionManager.makeCall(phoneNumber)
        val success = result is DeviceActionResult.Success
        val msg = if (result is DeviceActionResult.Success) result.message else (result as? DeviceActionResult.Failure)?.error ?: ""
        return JSONObject().apply {
            put("success", success)
            put("action", "makeCall")
            put("phoneNumber", phoneNumber)
            put("message", msg)
        }.toString()
    }

    @JavascriptInterface
    fun callContact(contactName: String): String {
        val result = actionManager.callContact(contactName)
        return when (result) {
            is DeviceActionResult.ContactFound -> {
                JSONObject().apply {
                    put("success", true)
                    put("contact", result.contact.name)
                    put("phoneNumber", result.contact.phoneNumber)
                }.toString()
            }
            is DeviceActionResult.MultipleContactsFound -> {
                JSONObject().apply {
                    put("success", false)
                    put("error", "Multiple matching contacts found")
                }.toString()
            }
            else -> {
                JSONObject().apply {
                    put("success", false)
                    put("error", "Contact not found")
                }.toString()
            }
        }
    }

    @JavascriptInterface
    fun onDiagnosticLog(message: String) {
        onLog(message)
    }
}
