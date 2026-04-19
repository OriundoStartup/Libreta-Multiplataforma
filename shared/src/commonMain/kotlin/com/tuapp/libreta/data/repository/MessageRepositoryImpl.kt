package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.now
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.random
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class MessageRepositoryImpl(private val queries: LibretaAppQueries) : MessageRepository {

    override fun getByReceiver(receiverId: UuidString): Flow<List<Message>> =
        queries.getMessagesByReceiver(receiverId.value).asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }

    override suspend fun save(message: Message) {
        val now = now()
        val messageId = message.id ?: UuidString.random()
        
        // Zero Trust: Validamos que el destinatario sea obligatorio para persistencia (Fail-fast)
        val receiverId = requireNotNull(message.receiverId) {
            "Message receiverId cannot be null for database persistence"
        }

        queries.insertOrReplaceMessage(
            id = messageId.value,
            sender_id = message.senderId.value,
            receiver_id = receiverId.value,
            content = message.content,
            sync_status = SyncStatus.PENDING_INSERT.name,
            created_at = now,
            updated_at = now
        )
    }

    override suspend fun delete(id: UuidString) {
        queries.markMessageAsPendingDelete(updated_at = now(), id = id.value)
    }
}
