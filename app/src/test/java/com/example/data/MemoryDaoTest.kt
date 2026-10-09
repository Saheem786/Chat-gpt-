package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MemoryDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var memoryDao: MemoryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        memoryDao = db.memoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveMemory() = runBlocking {
        val memory = MemoryEntity(category = "PREFERENCE", summary = "Likes Hindi")
        memoryDao.insertMemory(memory)
        
        val allMemories = memoryDao.searchMemories("Hindi")
        assertEquals(1, allMemories.size)
        assertEquals("Likes Hindi", allMemories[0].summary)
    }
}
