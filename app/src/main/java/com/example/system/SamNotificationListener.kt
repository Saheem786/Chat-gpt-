package com.example.system

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import android.util.Log

class SamNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d("SamNotificationListener", "Notification from $packageName: $title - $text")

        // Broadcast notification details to SamViewModel
        val intent = Intent("com.example.SAM_NOTIFICATION_RECEIVED").apply {
            putExtra("package", packageName)
            putExtra("title", title)
            putExtra("text", text)
        }
        sendBroadcast(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Option to handle cleared notifications if needed
    }
}
