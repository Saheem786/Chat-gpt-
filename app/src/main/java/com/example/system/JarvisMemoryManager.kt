package com.example.system

import android.content.Context
import android.util.Log
import com.example.data.MemoryDao
import com.example.data.MemoryEntity
import com.example.data.MemoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class JarvisMemoryManager(context: Context, private val memoryDao: MemoryDao) {
    private val prefs = context.getSharedPreferences("jarvis_cognitive_memory", Context.MODE_PRIVATE)
    val repository = MemoryRepository(memoryDao)

    init {
        Log.d("JarvisMemoryManager", "Memory system initialized with Room Repository.")
    }

    // Structured storage for user preferences and episodic memory
    fun storePreference(key: String, value: String, scope: CoroutineScope) {
        storeMemory(key, value)
        scope.launch(Dispatchers.IO) {
            repository.saveMemory(
                category = "PREFERENCE",
                summary = "$key: $value",
                importanceScore = 3,
                confidenceScore = 1.0f
            )
        }
    }

    fun storeEpisodicMemory(eventSummary: String, importance: Int = 2, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            repository.saveMemory(
                category = "EPISODIC",
                summary = eventSummary,
                importanceScore = importance,
                confidenceScore = 1.0f
            )
        }
    }

    fun getMemoriesByCategoryFlow(category: String): Flow<List<MemoryEntity>> {
        return repository.getMemoriesByCategory(category)
    }

    fun storeStructuredMemory(entity: MemoryEntity, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            memoryDao.insertMemory(entity)
        }
    }

    // Backward compatibility with SharedPreferences
    fun storeMemory(key: String, value: String) {
        Log.d("JarvisMemoryManager", "Storing memory link: $key -> $value")
        prefs.edit().putString(key.lowercase().trim(), value).apply()
    }

    fun retrieveMemory(key: String): String? {
        val value = prefs.getString(key.lowercase().trim(), null)
        Log.d("JarvisMemoryManager", "Retrieving memory for '$key': $value")
        return value
    }

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

    fun deleteMemory(key: String) {
        Log.d("JarvisMemoryManager", "Purging memory: $key")
        prefs.edit().remove(key.lowercase().trim()).apply()
    }

    fun clearAllMemories() {
        Log.d("JarvisMemoryManager", "Wiping cognitive memory matrix completely.")
        prefs.edit().clear().apply()
    }
}
