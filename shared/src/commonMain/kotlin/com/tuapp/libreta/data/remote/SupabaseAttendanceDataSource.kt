package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.AttendanceDto
import com.tuapp.libreta.data.remote.dto.toDomain
import com.tuapp.libreta.data.remote.dto.toDto
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.repository.AttendanceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseAttendanceDataSource(private val supabase: SupabaseClient) : AttendanceRepository {

    override fun getByStudent(studentId: UuidString): Flow<List<Attendance>> = flow {
        emit(supabase.from("attendance")
            .select { filter { eq("student_id", studentId.value) } }
            .decodeList<AttendanceDto>().map { it.toDomain() })
    }

    override suspend fun save(attendance: Attendance) {
        supabase.from("attendance").upsert(attendance.toDto())
    }

    override suspend fun delete(id: UuidString) {
        supabase.from("attendance").delete { filter { eq("id", id.value) } }
    }
}
