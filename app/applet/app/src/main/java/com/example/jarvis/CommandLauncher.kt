package com.example.jarvis

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.provider.Settings

object CommandLauncher {
    fun launchApp(context: Context, command: String): Boolean {
        val intent = when {
            command.contains("settings", ignoreCase = true) -> Intent(Settings.ACTION_SETTINGS)
            command.contains("calculator", ignoreCase = true) -> {
                val calcIntent = Intent(Intent.ACTION_MAIN)
                calcIntent.addCategory(Intent.CATEGORY_APP_CALCULATOR)
                calcIntent
            }
            command.contains("camera", ignoreCase = true) -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            else -> null
        }
        
        return try {
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
