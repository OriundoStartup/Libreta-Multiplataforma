package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.now
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class AttendanceRepositoryImpl(private val queries: LibretaAppQueries) : AttendanceRepository {

    override fun getByStudent(studentId: String): Flow<List<Attendance>> =
        queries.getAttendanceByStudent(studentId).asFlow().mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override suspend fun save(attendance: Attendance) = try {
        val now = now()
        queries.insertOrReplaceAttendance(attendance.id, attendance.studentId, attendance.date,
            attendance.status.name, SyncStatus.PENDING_INSERT.name, now, now)
    } catch (e: Exception) { throw RuntimeException("Error al guardar asistencia: ${e.message}", e) }

    override suspend fun delete(id: String) = try {
        queries.markAttendanceAsPendingDelete(updated_at = now(), id = id)
    } catch (e: Exception) { throw RuntimeException("Error al eliminar asistencia: ${e.message}", e) }
}
