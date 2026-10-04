package com.example.ui

import java.util.UUID

enum class MessageSender {
    USER,
    ASSISTANT
}

enum class AppMode {
    VOICE,
    CHAT
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender = MessageSender.ASSISTANT,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isAction: Boolean = false,
    val actionName: String? = null,
    val actionDetail: String? = null,
    val imageUrl: String? = null,
    val imageBase64: String? = null,
    val videoUrl: String? = null,
    val videoDuration: String? = null,
    val isGeneratingImage: Boolean = false
) {
    val isUser: Boolean get() = sender == MessageSender.USER
}
