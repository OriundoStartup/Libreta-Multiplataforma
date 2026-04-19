package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.MessageSupabaseDto
import com.tuapp.libreta.data.remote.dto.toDomain
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.repository.MessageRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseMessageRepository(private val supabase: SupabaseClient) : MessageRepository {

    override fun getByReceiver(receiverId: UuidString): Flow<List<Message>> = flow {
        emit(supabase.from("messages")
            .select { filter { eq("receiver_id", receiverId.value) } }
            .decodeList<MessageSupabaseDto>().map { it.toDomain() })
    }

    override suspend fun save(message: Message) {
        supabase.from("messages").upsert(
            MessageSupabaseDto(
                id         = message.id?.value,
                senderId   = message.senderId.value,
                receiverId = message.receiverId?.value,
                content    = message.content
            )
        )
    }

    override suspend fun delete(id: UuidString) {
        supabase.from("messages").delete { filter { eq("id", id.value) } }
    }
}
