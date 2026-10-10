package com.example.data

import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>> {
        return memoryDao.getMemoriesByCategory(category)
    }

    suspend fun saveMemory(
        category: String,
        summary: String,
        importanceScore: Int = 1,
        confidenceScore: Float = 1.0f,
        metadata: String? = null
    ) {
        val memory = MemoryEntity(
            category = category,
            summary = summary,
            importanceScore = importanceScore,
            confidenceScore = confidenceScore,
            isConfirmed = true,
            metadata = metadata,
            updatedAt = System.currentTimeMillis()
        )
        memoryDao.insertMemory(memory)
    }

    suspend fun updateMemory(memory: MemoryEntity) {
        memoryDao.updateMemory(memory.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteMemory(memory: MemoryEntity) {
        memoryDao.deleteMemory(memory)
    }

    suspend fun clearAll() {
        memoryDao.deleteAll()
    }

    suspend fun searchMemories(query: String): List<MemoryEntity> {
        return memoryDao.searchMemories(query)
    }
}
