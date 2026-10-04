package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.ChatMessage
import com.example.ui.MessageSender
import java.util.UUID

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAction: Boolean = false,
    val actionName: String? = null,
    val actionDetail: String? = null,
    val imageUrl: String? = null,
    val imageBase64: String? = null,
    val videoUrl: String? = null,
    val videoDuration: String? = null
) {
    fun toChatMessage(): ChatMessage {
        val messageSender = try {
            MessageSender.valueOf(sender)
        } catch (e: Exception) {
            if (sender == "USER") MessageSender.USER else MessageSender.ASSISTANT
        }
        return ChatMessage(
            id = id,
            sender = messageSender,
            text = text,
            timestamp = timestamp,
            isAction = isAction,
            actionName = actionName,
            actionDetail = actionDetail,
            imageUrl = imageUrl,
            imageBase64 = imageBase64,
            videoUrl = videoUrl,
            videoDuration = videoDuration
        )
    }

    companion object {
        fun fromChatMessage(msg: ChatMessage): ChatMessageEntity {
            return ChatMessageEntity(
                id = msg.id,
                sender = msg.sender.name,
                text = msg.text,
                timestamp = msg.timestamp,
                isAction = msg.isAction,
                actionName = msg.actionName,
                actionDetail = msg.actionDetail,
                imageUrl = msg.imageUrl,
                imageBase64 = msg.imageBase64,
                videoUrl = msg.videoUrl,
                videoDuration = msg.videoDuration
            )
        }
    }
}
