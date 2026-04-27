package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.sync.SyncManager
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.random
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.JustificationRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JustificationRepositoryImpl(
    private val queries: LibretaAppQueries,
    private val syncManager: SyncManager
) : JustificationRepository {

    private val scope = CoroutineScope(SupervisorJob() + getIoDispatcher())

    override fun getByStudent(studentId: UuidString): Flow<List<Justification>> =
        queries.getJustificationsByStudent(studentId.value).asFlow().mapToList(getIoDispatcher())
            .map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }

    override fun getPendingByTeacher(teacherId: UuidString): Flow<List<Justification>> =
        queries.getPendingJustificationsByTeacher(teacherId.value).asFlow().mapToList(getIoDispatcher())
            .map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }

    override suspend fun save(justification: Justification) {
        withContext(getIoDispatcher()) {
            val now = currentEpochMs()
            val justificationId = justification.id ?: UuidString.random()
            queries.insertOrReplaceJustification(
                id = justificationId.value,
                student_id = justification.studentId.value,
                date = justification.date.toString(),
                reason = justification.reason,
                status = justification.status.name,
                sync_status = SyncStatus.PENDING_INSERT.name,
                created_at = now,
                updated_at = now
            )
            scope.launch { syncManager.syncAll() }
        }
    }

    override suspend fun saveWithAttachment(
        justification: Justification,
        fileBytes: ByteArray?,
        fileName: String?
    ): Result<Unit> {
        return withContext(getIoDispatcher()) {
            runCatching {
                save(justification)
                // Note: Attachment upload will be implemented in Phase 5 using Supabase Storage
                Unit
            }
        }
    }

    override suspend fun delete(id: UuidString) {
        withContext(getIoDispatcher()) {
            queries.markJustificationAsPendingDelete(updated_at = currentEpochMs(), id = id.value)
            scope.launch { syncManager.syncAll() }
        }
    }
}
