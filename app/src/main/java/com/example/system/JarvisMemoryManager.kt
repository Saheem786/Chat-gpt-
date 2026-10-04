package com.example.system

import android.content.Context
import android.util.Log

class JarvisMemoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_cognitive_memory", Context.MODE_PRIVATE)

    init {
        Log.d("JarvisMemoryManager", "Memory system initialized.")
    }

    /**
     * Store a memory fact or preference
     */
    fun storeMemory(key: String, value: String) {
        Log.d("JarvisMemoryManager", "Storing memory link: $key -> $value")
        prefs.edit().putString(key.lowercase().trim(), value).apply()
    }

    /**
     * Retrieve a memory fact or preference
     */
    fun retrieveMemory(key: String): String? {
        val value = prefs.getString(key.lowercase().trim(), null)
        Log.d("JarvisMemoryManager", "Retrieving memory for '$key': $value")
        return value
    }

    /**
     * Get all currently saved cognitive memories
     */
    fun getAllMemories(): Map<String, String> {
        val all = prefs.all
        val result = mutableMapOf<String, String>()
        for ((k, v) in all) {
            if (v is String) {
                result[k] = v
            }
        }
        return result
    }

    /**
     * Delete a specific memory
     */
    fun deleteMemory(key: String) {
        Log.d("JarvisMemoryManager", "Purging memory: $key")
        prefs.edit().remove(key.lowercase().trim()).apply()
    }

    /**
     * Clear all database cognitive memories
     */
    fun clearAllMemories() {
        Log.d("JarvisMemoryManager", "Wiping cognitive memory matrix completely.")
        prefs.edit().clear().apply()
    }
}
