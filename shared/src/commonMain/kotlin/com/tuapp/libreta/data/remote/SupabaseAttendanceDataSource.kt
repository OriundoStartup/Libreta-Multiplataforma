package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.AttendanceDto
import com.tuapp.libreta.data.remote.dto.toDomain
import com.tuapp.libreta.data.remote.dto.toDto
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.repository.AttendanceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseAttendanceDataSource(
    private val supabase: SupabaseClient
) : AttendanceRepository {

    override fun getByStudent(studentId: String): Flow<List<Attendance>> = flow {
        val result = supabase.from("AttendanceEntity")
            .select { filter { AttendanceDto::studentId eq studentId } }
            .decodeList<AttendanceDto>()
        emit(result.map { it.toDomain() })
    }

    override suspend fun save(attendance: Attendance) {
        supabase.from("AttendanceEntity")
            .upsert(attendance.toDto())
    }

    override suspend fun delete(id: String) {
        supabase.from("AttendanceEntity")
            .delete { filter { AttendanceDto::id eq id } }
    }
}
