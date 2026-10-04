package com.example.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.BatteryManager
import android.util.Log
import com.example.MainActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SamProactiveReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SamProactiveReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Proactive broadcast received: $action")

        val prefs: SharedPreferences = context.getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)

        if (action == Intent.ACTION_USER_PRESENT) {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

            // Check if it's the first unlock of the day between 06:00 AM and 10:00 AM
            if (currentHour in 6..10) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val lastBriefingDate = prefs.getString("last_proactive_briefing_date", "")

                if (lastBriefingDate != todayStr) {
                    Log.d(TAG, "First unlock of the day detected. Preparing proactive morning briefing.")
                    prefs.edit()
                        .putString("last_proactive_briefing_date", todayStr)
                        .putBoolean("pending_morning_briefing", true)
                        .apply()

                    // Launch MainActivity to deliver the briefing live!
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("trigger_morning_briefing", true)
                    }
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                }
            }
        } else if (action == Intent.ACTION_BATTERY_LOW || action == Intent.ACTION_BATTERY_CHANGED) {
            // Monitor battery level
            val batteryStatus = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else -1

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            Log.d(TAG, "Battery Level: $batteryPct%, Charging: $isCharging")

            if (batteryPct in 1..15 && !isCharging) {
                val lastBatteryAlertTime = prefs.getLong("last_battery_alert_time", 0L)
                val currentTime = System.currentTimeMillis()

                // Alert at most once every 30 minutes to prevent fatigue
                if (currentTime - lastBatteryAlertTime > 30 * 60 * 1000L) {
                    prefs.edit().putLong("last_battery_alert_time", currentTime).apply()

                    // Broadcast to running ViewModel
                    val alertIntent = Intent("com.example.SAM_BATTERY_ALERT").apply {
                        putExtra("battery_pct", batteryPct)
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(alertIntent)

                    // Also open app in Voice Mode to verbally alert
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("trigger_battery_alert", true)
                        putExtra("battery_pct", batteryPct)
                    }
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                }
            }
        }
    }
}
