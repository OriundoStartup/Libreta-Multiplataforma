package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.CommunicationSupabaseDto
import com.tuapp.libreta.data.remote.dto.toDomain
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.repository.CommunicationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseCommunicationRepository(private val supabase: SupabaseClient) : CommunicationRepository {

    override suspend fun sendGeneralNotice(senderId: UuidString, classId: UuidString, content: String) {
        supabase.from("communications").insert(
            CommunicationSupabaseDto(
                senderId    = senderId.value,
                courseId    = classId.value,
                messageText = content,
                category    = "INFO",
                isInternal  = false
            )
        )
    }

    override fun getByClass(classId: UuidString): Flow<List<Message>> = flow {
        emit(supabase.from("communications")
            .select { filter { eq("course_id", classId.value) } }
            .decodeList<CommunicationSupabaseDto>()
            .map { it.toDomain() })
    }
}
