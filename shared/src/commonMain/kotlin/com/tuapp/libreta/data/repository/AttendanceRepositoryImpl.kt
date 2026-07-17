package com.tuapp.libreta.data.repository

import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.toDomainList
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.random
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AttendanceRepositoryImpl(private val queries: LibretaAppQueries) : AttendanceRepository {

    override fun getByStudent(studentId: UuidString): Flow<List<Attendance>> =
        queries.getAttendanceByStudent(studentId.value).toDomainList { it.toDomain() }

    override fun getByCourse(courseId: UuidString): Flow<List<Attendance>> =
        queries.getAttendanceByCourse(courseId.value).toDomainList { it.toDomain() }

    override suspend fun save(attendance: Attendance) {
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
                sync_status = "PENDING_INSERT",
                created_at = now,
                updated_at = now
            )
        }
    }

    override suspend fun delete(id: UuidString) {
        withContext(getIoDispatcher()) {
            queries.markAttendanceAsPendingDelete(
                updated_at = currentEpochMs(),
                id = id.value
            )
        }
    }
}
