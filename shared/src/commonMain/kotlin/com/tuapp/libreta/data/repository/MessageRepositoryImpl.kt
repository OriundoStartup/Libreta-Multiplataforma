package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.now
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class MessageRepositoryImpl(private val queries: LibretaAppQueries) : MessageRepository {

    override fun getByReceiver(receiverId: String): Flow<List<Message>> =
        queries.getMessagesByReceiver(receiverId).asFlow().mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override suspend fun save(message: Message) = try {
        val now = now()
        queries.insertOrReplaceMessage(message.id, message.senderId, message.receiverId,
            message.content, SyncStatus.PENDING_INSERT.name, now, now)
    } catch (e: Exception) { throw RuntimeException("Error al guardar mensaje: ${e.message}", e) }

    override suspend fun delete(id: String) = try {
        queries.markMessageAsPendingDelete(updated_at = now(), id = id)
    } catch (e: Exception) { throw RuntimeException("Error al eliminar mensaje: ${e.message}", e) }
}
