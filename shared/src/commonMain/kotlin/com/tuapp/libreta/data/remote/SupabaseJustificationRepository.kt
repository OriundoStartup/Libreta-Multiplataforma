package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.JustificationSupabaseDto
import com.tuapp.libreta.data.remote.dto.toDomain
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.repository.JustificationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseJustificationRepository(private val supabase: SupabaseClient) : JustificationRepository {

    override fun getByStudent(studentId: UuidString): Flow<List<Justification>> = flow {
        emit(supabase.from("justifications")
            .select { filter { eq("student_id", studentId.value) } }
            .decodeList<JustificationSupabaseDto>().map { it.toDomain() })
    }

    override suspend fun save(justification: Justification) {
        supabase.from("justifications").upsert(
            JustificationSupabaseDto(
                id        = justification.id?.value,
                studentId = justification.studentId.value,
                date      = justification.date,
                reason    = justification.reason,
                status    = justification.status.name
            )
        )
    }

    override suspend fun delete(id: UuidString) {
        supabase.from("justifications").delete { filter { eq("id", id.value) } }
    }
}
