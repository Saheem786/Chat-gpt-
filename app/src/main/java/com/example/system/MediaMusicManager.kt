package com.example.system

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.provider.MediaStore
import android.view.KeyEvent
import android.util.Log

class MediaMusicManager(private val context: Context) {

    fun dispatchMediaKey(keyCode: Int): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val eventTime = SystemClock.uptimeMillis()

            val downEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
            audioManager.dispatchMediaKeyEvent(downEvent)

            val upEvent = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)
            audioManager.dispatchMediaKeyEvent(upEvent)

            true
        } catch (e: Exception) {
            Log.e("MediaMusicManager", "Error dispatching media key: $keyCode", e)
            false
        }
    }

    fun playMusic(query: String, platform: String): Boolean {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                putExtra(MediaStore.EXTRA_MEDIA_TITLE, query)
                putExtra(SearchManager_QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (platform.lowercase().contains("spotify")) {
                intent.setPackage("com.spotify.music")
            } else if (platform.lowercase().contains("youtube") || platform.lowercase().contains("yt")) {
                intent.setPackage("com.google.android.apps.youtube.music")
            }

            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("MediaMusicManager", "Error playing music from search", e)
            // Fallback: general launch of standard play intent
            try {
                val fallbackIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    putExtra(SearchManager_QUERY, query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                true
            } catch (ex: Exception) {
                Log.e("MediaMusicManager", "Fallback play music also failed", ex)
                false
            }
        }
    }

    companion object {
        private const val SearchManager_QUERY = "query"
    }
}
