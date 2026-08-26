package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.remote.dto.MessageSupabaseDto
import com.tuapp.libreta.data.remote.dto.ProfileSupabaseDto
import com.tuapp.libreta.data.remote.dto.toDomain
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.repository.MessageRepository
import com.tuapp.libreta.domain.usecase.MessageThread
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private const val INBOX_LIMIT        = 100
private const val CONVERSATION_LIMIT = 200

@OptIn(SupabaseExperimental::class)
class SupabaseMessageRepository(private val supabase: SupabaseClient) : MessageRepository {

    override fun observeConversation(currentUserId: String, contactId: String): Flow<List<Message>> {
        return try {
            supabase.from("messages")
                .selectAsFlow(MessageSupabaseDto::id)
                .map { list: List<MessageSupabaseDto> -> 
                    list.map { it.toDomain() }
                        .filter { 
                            (it.senderId.value == currentUserId && it.receiverId?.value == contactId) ||
                            (it.senderId.value == contactId && it.receiverId?.value == currentUserId)
                        }
                        .sortedBy { it.createdAt ?: "" } 
                }
        } catch (e: Exception) {
            AppLogger.e("MessageRepository", "Error en realtime: ${e.message}")
            flowOf(emptyList())
        }
    }

    override suspend fun getInbox(currentUserId: String): List<MessageThread> {
        return try {
            AppLogger.d("MessageRepository", "Fetching inbox for $currentUserId")
            val messages = supabase.from("messages")
                .select {
                    filter {
                        or {
                            eq("sender_id", currentUserId)
                            eq("receiver_id", currentUserId)
                        }
                    }
                    order("created_at", Order.DESCENDING)
                    limit(INBOX_LIMIT.toLong())
                }
                .decodeList<MessageSupabaseDto>()
            
            AppLogger.d("MessageRepository", "Received ${messages.size} messages for inbox")

            val grouped = messages
                .groupBy { msg ->
                    if (msg.senderId == currentUserId) msg.receiverId ?: "" else msg.senderId ?: ""
                }
                .filter { it.key.isNotBlank() }

            // Batch fetch profiles to avoid N+1
            val contactIds = grouped.keys.toList()
            val profiles = if (contactIds.isNotEmpty()) {
                supabase.from("profiles")
                    .select(columns = Columns.raw("id,full_name")) {
                        filter {
                            isIn("id", contactIds)
                        }
                    }
                    .decodeList<ProfileSupabaseDto>()
                    .associateBy { it.id ?: "" }
            } else emptyMap()

            grouped.map { (contactId, msgs) ->
                val lastMsg    = msgs.first()
                val isMine     = lastMsg.senderId == currentUserId
                val unreadCount = msgs.count { it.receiverId == currentUserId && it.readAt == null }
                val profile    = profiles[contactId]
                MessageThread(
                    contactId   = UuidString(contactId),
                    contactName = profile?.fullName ?: "Usuario",
                    lastMessage = lastMsg.content ?: "",
                    unread      = unreadCount > 0,
                    isLastMessageMine = isMine
                )
            }
        } catch (e: Exception) {
            AppLogger.e("MessageRepository", "Error cargando inbox: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getConversation(currentUserId: String, contactId: String): List<Message> {
        return try {
            supabase.from("messages")
                .select {
                    filter {
                        or {
                            and {
                                eq("sender_id", currentUserId)
                                eq("receiver_id", contactId)
                            }
                            and {
                                eq("sender_id", contactId)
                                eq("receiver_id", currentUserId)
                            }
                        }
                    }
                    order("created_at", Order.ASCENDING)
                    limit(CONVERSATION_LIMIT.toLong())
                }
                .decodeList<MessageSupabaseDto>()
                .map { it.toDomain() }
        } catch (e: Exception) {
            AppLogger.e("MessageRepository", "Error cargando conversación: ${e.message}")
            emptyList()
        }
    }

    override suspend fun sendMessage(receiverId: String, content: String): Result<Unit> {
        return runCatching {
            val currentUser = supabase.auth.currentUserOrNull()
                ?: throw Exception("No active session")
            val dto = MessageSupabaseDto(
                senderId    = currentUser.id,
                receiverId  = receiverId,
                content     = content.trim()
            )
            supabase.from("messages").insert(dto)
            AppLogger.d("MessageRepository", "Message sent successfully to $receiverId")
        }.onFailure { e ->
            AppLogger.e("MessageRepository", "Error sending message: ${e.message}")
        }
    }

    override suspend fun markAsRead(senderId: String, currentUserId: String) {
        runCatching {
            supabase.from("messages")
                .update({ set("read_at", "now()") }) {
                    filter {
                        eq("sender_id", senderId)
                        eq("receiver_id", currentUserId)
                        filter("read_at", FilterOperator.IS, "null")
                    }
                }
        }.onFailure { e ->
            AppLogger.e("MessageRepository", "Error marcando como leído: ${e.message}")
        }
    }

    override fun getInternalNotes(studentId: UuidString): Flow<List<Message>> = flow {
        try {
            val response = supabase.from("messages")
                .select {
                    filter {
                        eq("student_id", studentId.value)
                        eq("is_internal", true)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<MessageSupabaseDto>()
            emit(response.map { it.toDomain() })
        } catch (e: Exception) {
            AppLogger.e("MessageRepository", "Error cargando notas: ${e.message}")
            emit(emptyList())
        }
    }

    override suspend fun saveInternalNote(
        studentId: UuidString,
        senderId: UuidString,
        content: String
    ): Result<Unit> = runCatching {
        val dto = MessageSupabaseDto(
            senderId    = senderId.value,
            studentId   = studentId.value,
            content     = content,
            isInternal  = true
        )
        supabase.from("messages").insert(dto)
    }

    override suspend fun save(message: Message) {
        val dto = MessageSupabaseDto(
            senderId    = message.senderId.value,
            receiverId  = message.receiverId?.value,
            content     = message.content
        )
        supabase.from("messages").insert(dto)
    }

    private suspend fun getProfileName(userId: String): ProfileSupabaseDto? {
        return try {
            supabase.from("profiles")
                .select(columns = Columns.raw("id,full_name")) { filter { eq("id", userId) } }
                .decodeSingleOrNull<ProfileSupabaseDto>()
        } catch (_: Exception) {
            null
        }
    }
}
