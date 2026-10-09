package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val category: String, // SEMANTIC, EPISODIC, PREFERENCE
    val summary: String,
    val importanceScore: Int = 1,
    val confidenceScore: Float = 0.5f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val isConfirmed: Boolean = false,
    val metadata: String? = null // JSON for flexibility
)
