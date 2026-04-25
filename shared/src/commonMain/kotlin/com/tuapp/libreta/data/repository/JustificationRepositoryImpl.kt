package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.now
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.random
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.JustificationRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class JustificationRepositoryImpl(private val queries: LibretaAppQueries) : JustificationRepository {

    override fun getByStudent(studentId: UuidString): Flow<List<Justification>> =
        queries.getJustificationsByStudent(studentId.value).asFlow().mapToList(getIoDispatcher())
            .map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }

    override suspend fun save(justification: Justification) {
        val now = now()
        val justificationId = justification.id ?: UuidString.random()
        queries.insertOrReplaceJustification(
            id = justificationId.value,
            student_id = justification.studentId.value,
            date = justification.date,
            reason = justification.reason,
            status = justification.status.name,
            sync_status = SyncStatus.PENDING_INSERT.name,
            created_at = now,
            updated_at = now
        )
    }

    override suspend fun delete(id: UuidString) {
        queries.markJustificationAsPendingDelete(updated_at = now(), id = id.value)
    }
}
