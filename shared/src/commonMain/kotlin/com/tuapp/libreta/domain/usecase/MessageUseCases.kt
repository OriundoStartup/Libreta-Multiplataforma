package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow

data class MessageThread(
    val contactId: UuidString,
    val contactName: String,
    val lastMessage: String,
    val unread: Boolean,
    val isLastMessageMine: Boolean = false
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

class ObserveConversationUseCase(private val repository: MessageRepository) {
    operator fun invoke(userId: UuidString, contactId: UuidString): Flow<List<Message>> =
        repository.observeConversation(userId.value, contactId.value)
}

class MarkAsReadUseCase(private val repository: MessageRepository) {
    suspend operator fun invoke(senderId: UuidString, currentUserId: UuidString) =
        repository.markAsRead(senderId.value, currentUserId.value)
}
