package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class MessageThread(
    val contactId: UuidString,
    val contactName: String,
    val lastMessage: String,
    val unread: Boolean
)

class GetInboxUseCase(private val repository: MessageRepository) {
    operator fun invoke(userId: UuidString): Flow<List<MessageThread>> =
        repository.getByReceiver(userId).map { messages ->
            messages.groupBy { it.senderId }.map { (senderId, msgs) ->
                MessageThread(senderId, "Contacto ${senderId.value.takeLast(4)}", msgs.maxBy { it.id?.value ?: "" }.content, true)
            }
        }
}

class GetConversationUseCase(private val repository: MessageRepository) {
    operator fun invoke(userId: UuidString, contactId: UuidString): Flow<List<Message>> =
        repository.getByReceiver(userId).map { it.filter { m -> m.senderId == contactId } }
}

class SendMessageUseCase(private val repository: MessageRepository) {
    suspend operator fun invoke(senderId: UuidString, receiverId: UuidString, content: String) {
        require(content.isNotBlank()) { "El mensaje no puede estar vacío" }
        repository.save(Message(null, senderId, receiverId, content.trim()))
    }
}
