package com.example.data

import com.example.ui.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepository(private val dao: ChatMessageDao) {

    val allMessages: Flow<List<ChatMessage>> = dao.getAllMessages().map { entities ->
        entities.map { it.toChatMessage() }
    }

    val recentMessages: Flow<List<ChatMessage>> = dao.getRecentMessages(6).map { entities ->
        entities.map { it.toChatMessage() }
    }

    suspend fun saveMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        dao.insertMessage(ChatMessageEntity.fromChatMessage(message))
    }

    suspend fun saveMessages(messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        dao.insertMessages(messages.map { ChatMessageEntity.fromChatMessage(it) })
    }

    suspend fun deleteMessage(id: String) = withContext(Dispatchers.IO) {
        dao.deleteMessageById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearAllMessages()
    }

    suspend fun getMessageCount(): Int = withContext(Dispatchers.IO) {
        dao.getMessageCount()
    }
}
