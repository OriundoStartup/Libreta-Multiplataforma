package com.tuapp.libreta.data.sync

import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.toDomainList
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.random
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.di.dbReady
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SymbioticAttendanceRepository(
    private val queries: LibretaAppQueries,
    private val syncManager: SyncManager
) : AttendanceRepository {

    private val scope = CoroutineScope(SupervisorJob() + getIoDispatcher())

    private suspend fun waitForDb() {
        runCatching {
            kotlinx.coroutines.withTimeout(5000) { dbReady.await() }
        }.onFailure { 
            AppLogger.e("AttendanceRepo", "Database ready timeout: ${it.message}")
        }
    }

    override fun getByStudent(studentId: UuidString): Flow<List<Attendance>> = kotlinx.coroutines.flow.flow {
        waitForDb()
        queries.getAttendanceByStudent(studentId.value).toDomainList { it.toDomain() }.collect { emit(it) }
    }

    override fun getByCourse(courseId: UuidString): Flow<List<Attendance>> = kotlinx.coroutines.flow.flow {
        waitForDb()
        queries.getAttendanceByCourse(courseId.value).toDomainList { it.toDomain() }.collect { emit(it) }
    }

    override suspend fun save(attendance: Attendance) {
        waitForDb()
        withContext(getIoDispatcher()) {
            val now = currentEpochMs()
            val attendanceId = attendance.id ?: UuidString.random()
            
            queries.insertOrReplaceAttendance(
                id = attendanceId.value,
                student_id = attendance.studentId.value,
                date = attendance.date,
                status = attendance.status.name,
                server_version = 1,
                is_deleted = 0,
                sync_status = SyncStatus.PENDING_INSERT.name,
                created_at = now,
                updated_at = now
            )

            scope.launch {
                syncManager.syncAll()
            }
        }
    }

    override suspend fun delete(id: UuidString) {
        waitForDb()
        withContext(getIoDispatcher()) {
            queries.markAttendanceAsPendingDelete(
                updated_at = currentEpochMs(),
                id = id.value
            )
            scope.launch {
                syncManager.syncAll()
            }
        }
    }
}
