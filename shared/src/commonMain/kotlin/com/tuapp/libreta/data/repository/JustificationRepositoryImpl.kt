package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.now
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.JustificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class JustificationRepositoryImpl(private val queries: LibretaAppQueries) : JustificationRepository {

    override fun getByStudent(studentId: String): Flow<List<Justification>> =
        queries.getJustificationsByStudent(studentId).asFlow().mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override suspend fun save(justification: Justification) = try {
        val now = now()
        queries.insertOrReplaceJustification(justification.id, justification.studentId,
            justification.date, justification.reason, justification.status.name,
            SyncStatus.PENDING_INSERT.name, now, now)
    } catch (e: Exception) { throw RuntimeException("Error al guardar justificación: ${e.message}", e) }

    override suspend fun delete(id: String) = try {
        queries.markJustificationAsPendingDelete(updated_at = now(), id = id)
    } catch (e: Exception) { throw RuntimeException("Error al eliminar justificación: ${e.message}", e) }
}
