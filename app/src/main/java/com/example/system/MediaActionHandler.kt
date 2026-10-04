package com.example.system

import android.content.Context
import android.content.Intent
import android.net.Uri

object MediaActionHandler {
    private val MUSIC_KEYWORDS = listOf("play", "chalao", "sunao", "bajaao", "gaana", "song", "video", "baja")

    fun isMediaRequest(prompt: String): Boolean {
        val lower = prompt.lowercase()
        return MUSIC_KEYWORDS.any { lower.contains(it) }
    }

    fun extractQuery(prompt: String): String {
        return prompt.replace(Regex("(?i)\\b(play|chalao|sunao|bajaao|gaana|song|video|youtube par|on youtube|lagao|please|baja)\\b"), "")
            .trim()
    }

    fun playOnYouTube(context: Context, searchQuery: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", searchQuery)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(searchQuery)}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }
}
