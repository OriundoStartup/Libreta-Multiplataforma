package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.repository.MessageRepository

data class MessageThread(
    val contactId: UuidString,
    val contactName: String,
    val lastMessage: String,
    val unread: Boolean
)

class GetInboxUseCase(private val repository: MessageRepository) {
    suspend operator fun invoke(userId: UuidString): List<MessageThread> =
        repository.getInbox(userId.value)
}

class GetConversationUseCase(private val repository: MessageRepository) {
    suspend operator fun invoke(userId: UuidString, contactId: UuidString): List<Message> =
        repository.getConversation(userId.value, contactId.value)
}

class SendMessageUseCase(private val repository: MessageRepository) {
    suspend operator fun invoke(receiverId: UuidString, content: String): Result<Unit> =
        repository.sendMessage(receiverId.value, content)
}

class MarkAsReadUseCase(private val repository: MessageRepository) {
    suspend operator fun invoke(senderId: UuidString, currentUserId: UuidString) =
        repository.markAsRead(senderId.value, currentUserId.value)
}
