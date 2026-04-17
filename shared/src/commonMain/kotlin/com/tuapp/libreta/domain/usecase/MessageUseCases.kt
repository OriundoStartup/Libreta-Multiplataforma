package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

data class MessageThread(
    val contactId: String,
    val contactName: String,
    val lastMessage: String,
    val unread: Boolean
)

class GetInboxUseCase(private val repository: MessageRepository) {
    operator fun invoke(userId: String): Flow<List<MessageThread>> =
        repository.getByReceiver(userId).map { messages ->
            messages.groupBy { it.senderId }.map { (senderId, msgs) ->
                MessageThread(
                    contactId   = senderId,
                    contactName = "Contacto ${senderId.takeLast(4)}",
                    lastMessage = msgs.maxBy { it.id }.content,
                    unread      = true
                )
            }
        }
}

class GetConversationUseCase(private val repository: MessageRepository) {
    operator fun invoke(userId: String, contactId: String): Flow<List<Message>> =
        repository.getByReceiver(userId).map { it.filter { m -> m.senderId == contactId } }
}

class SendMessageUseCase(private val repository: MessageRepository) {
    suspend operator fun invoke(senderId: String, receiverId: String, content: String) {
        require(content.isNotBlank()) { "El mensaje no puede estar vacío" }
        repository.save(
            Message(
                id         = Clock.System.now().toEpochMilliseconds().toString(),
                senderId   = senderId,
                receiverId = receiverId,
                content    = content.trim()
            )
        )
    }
}
