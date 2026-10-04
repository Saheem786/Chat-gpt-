package com.example.system

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import java.util.Calendar
import java.util.TimeZone

class CalendarReminderManager(private val context: Context) {

    fun scheduleEvent(title: String, startMillis: Long, durationMinutes: Int, notes: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + (durationMinutes * 60 * 1000))
                putExtra(CalendarContract.Events.DESCRIPTION, notes)
                putExtra(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("CalendarReminderManager", "Error scheduling event", e)
            false
        }
    }

    fun getTodayEvents(): List<String> {
        val eventsList = mutableListOf<String>()
        val hasPermission = context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            Log.w("CalendarReminderManager", "READ_CALENDAR permission not granted.")
            return listOf("Permission READ_CALENDAR is required to query calendar events.")
        }

        try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startOfDay = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endOfDay = calendar.timeInMillis

            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART
            )

            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
            val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())

            val uri: Uri = CalendarContract.Events.CONTENT_URI
            val cursor: Cursor? = context.contentResolver.query(uri, projection, selection, selectionArgs, "${CalendarContract.Events.DTSTART} ASC")

            cursor?.use {
                val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startIndex = it.getColumnIndex(CalendarContract.Events.DTSTART)
                while (it.moveToNext()) {
                    val title = if (titleIndex != -1) it.getString(titleIndex) else "Untitled Event"
                    val startTime = if (startIndex != -1) it.getLong(startIndex) else 0L
                    val cal = Calendar.getInstance().apply { timeInMillis = startTime }
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    val timeString = String.format("%02d:%02d", hour, minute)
                    eventsList.add("- $title at $timeString")
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarReminderManager", "Error reading calendar", e)
            eventsList.add("Error loading today's events: ${e.message}")
        }

        if (eventsList.isEmpty()) {
            eventsList.add("No events scheduled for today.")
        }
        return eventsList
    }
}
