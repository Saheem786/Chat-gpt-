package com.example.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.BatteryManager
import android.util.Log
import com.example.MainActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MorningBriefingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d("MorningBriefingReceiver", "User unlocked device, checking if morning briefing should trigger...")
            
            val prefs: SharedPreferences = context.getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

            // Morning window: 6:00 AM to 11:59 AM
            if (currentHour in 6..11) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                val lastBriefingDate = prefs.getString("last_briefing_date", "")

                if (lastBriefingDate != todayStr) {
                    Log.d("MorningBriefingReceiver", "Morning Briefing eligible. Storing pending trigger and launching Sam AI...")
                    
                    // Set flag in shared preferences
                    prefs.edit()
                        .putBoolean("pending_morning_briefing", true)
                        .putString("last_briefing_date", todayStr)
                        .apply()

                    // Launch MainActivity to deliver the briefing live
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        putExtra("trigger_morning_briefing", true)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    context.startActivity(launchIntent)
                } else {
                    Log.d("MorningBriefingReceiver", "Morning Briefing already delivered today.")
                }
            } else {
                Log.d("MorningBriefingReceiver", "Not inside morning briefing hours (6:00 AM - 11:59 AM). Current hour: $currentHour")
            }
        }
    }
}
