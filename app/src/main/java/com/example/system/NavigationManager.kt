package com.example.system

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class NavigationManager(private val context: Context) {

    companion object {
        private const val TAG = "NavigationManager"
    }

    fun navigateBack(): Boolean {
        Log.i(TAG, "Navigating back...")
        val service = SamScreenContextService.getInstance()
        return if (service != null) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        } else {
            false
        }
    }

    fun navigateHome(): Boolean {
        Log.i(TAG, "Navigating home...")
        val service = SamScreenContextService.getInstance()
        return if (service != null) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        } else {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }
    }

    fun openRecents(): Boolean {
        Log.i(TAG, "Opening Recents...")
        val service = SamScreenContextService.getInstance()
        return service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS) ?: false
    }

    fun openNotifications(): Boolean {
        Log.i(TAG, "Opening notifications panel...")
        val service = SamScreenContextService.getInstance()
        return service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) ?: false
    }
}
