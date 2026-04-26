package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.AttendanceSupabaseDto
import com.tuapp.libreta.data.remote.dto.StudentSupabaseDto
import com.tuapp.libreta.data.remote.dto.toDomain
import com.tuapp.libreta.data.remote.dto.toSupabaseDto
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.repository.AttendanceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseAttendanceDataSource(private val supabase: SupabaseClient) : AttendanceRepository {

    override fun getByStudent(studentId: UuidString): Flow<List<Attendance>> = flow {
        try {
            val result = supabase.from("attendance")
                .select {
                    filter { eq("student_id", studentId.value) }
                }
                .decodeList<AttendanceSupabaseDto>()
                .map { it.toDomain() }
            emit(result)
        } catch (e: Exception) {
            AppLogger.e("AttendanceDataSource", "Error loading attendance: ${e.message}")
            emit(emptyList())
        }
    }

    override fun getByCourse(courseId: UuidString): Flow<List<Attendance>> = flow {
        try {
            val students = supabase.from("students")
                .select { filter { eq("course_id", courseId.value) } }
                .decodeList<StudentSupabaseDto>()
            val studentIds = students.mapNotNull { it.id }
            
            if (studentIds.isEmpty()) {
                emit(emptyList())
                return@flow
            }
            
            val result = supabase.from("attendance")
                .select {
                    filter { isIn("student_id", studentIds) }
                }
                .decodeList<AttendanceSupabaseDto>()
                .map { it.toDomain() }
            emit(result)
        } catch (e: Exception) {
            AppLogger.e("AttendanceDataSource", "Error loading attendance by course: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun save(attendance: Attendance) {
        try {
            supabase.from("attendance").upsert(attendance.toSupabaseDto())
        } catch (e: Exception) {
            AppLogger.e("AttendanceDataSource", "Error saving attendance: ${e.message}")
            throw e
        }
    }

    override suspend fun delete(id: UuidString) {
        try {
            supabase.from("attendance").delete { filter { eq("id", id.value) } }
        } catch (e: Exception) {
            AppLogger.e("AttendanceDataSource", "Error deleting attendance: ${e.message}")
            throw e
        }
    }
}