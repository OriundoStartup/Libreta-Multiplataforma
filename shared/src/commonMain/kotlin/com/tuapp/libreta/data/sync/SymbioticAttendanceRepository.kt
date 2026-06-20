package com.tuapp.libreta.data.sync

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.random
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SymbioticAttendanceRepository(
    private val queries: LibretaAppQueries,
    private val syncManager: SyncManager
) : AttendanceRepository {

    private val scope = CoroutineScope(SupervisorJob() + getIoDispatcher())

    override fun getByStudent(studentId: UuidString): Flow<List<Attendance>> =
        queries.getAttendanceByStudent(studentId.value)
            .asFlow()
            .mapToList(getIoDispatcher())
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override fun getByCourse(courseId: UuidString): Flow<List<Attendance>> =
        queries.getAttendanceByCourse(courseId.value)
            .asFlow()
            .mapToList(getIoDispatcher())
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override suspend fun save(attendance: Attendance) {
        withContext(getIoDispatcher()) {
            val now = currentEpochMs()
            val attendanceId = attendance.id ?: UuidString.random()
            
            // 1. Save locally (Inside withContext to prevent UI freeze)
            queries.insertOrReplaceAttendance(
                id = attendanceId.value,
                student_id = attendance.studentId.value,
                date = attendance.date,
                status = attendance.status.name,
                server_version = 1, // Valor inicial para nuevos registros locales
                is_deleted = 0,
                sync_status = SyncStatus.PENDING_INSERT.name,
                created_at = now,
                updated_at = now
            )

            // 2. Trigger sync in background
            scope.launch {
                syncManager.syncAll()
            }
        }
    }

    override suspend fun delete(id: UuidString) {
        withContext(getIoDispatcher()) {
            // 1. Mark as pending delete locally
            queries.markAttendanceAsPendingDelete(
                updated_at = currentEpochMs(),
                id = id.value
            )

            // 2. Trigger sync
            scope.launch {
                syncManager.syncAll()
            }
        }
    }
}
