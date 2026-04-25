package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.now
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.random
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Implementación de [AttendanceRepository] que gestiona la persistencia local
 * mediante SQLDelight, asegurando el cumplimiento del tipado fuerte de UUID.
 */
class AttendanceRepositoryImpl(private val queries: LibretaAppQueries) : AttendanceRepository {

    override fun getByStudent(studentId: UuidString): Flow<List<Attendance>> =
        queries.getAttendanceByStudent(studentId.value)
            .asFlow()
            .mapToList(getIoDispatcher())
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override suspend fun save(attendance: Attendance) {
        val now = now()
        val attendanceId = attendance.id ?: UuidString.random() // Generar ID si es nuevo
        
        queries.insertOrReplaceAttendance(
            id = attendanceId.value,
            student_id = attendance.studentId.value,
            date = attendance.date,
            status = attendance.status.name,
            justification_id = attendance.justificationId?.value,
            sync_status = SyncStatus.PENDING_INSERT.name,
            created_at = now,
            updated_at = now
        )
    }

    override suspend fun delete(id: UuidString) {
        // Implementación de borrado lógico para sincronización posterior
        queries.markAttendanceAsPendingDelete(
            updated_at = now(),
            id = id.value
        )
    }
}
