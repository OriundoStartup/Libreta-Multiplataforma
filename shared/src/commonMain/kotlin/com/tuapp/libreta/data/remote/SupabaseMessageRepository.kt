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
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator

private const val INBOX_LIMIT        = 100
private const val CONVERSATION_LIMIT = 200

class SupabaseMessageRepository(private val supabase: SupabaseClient) : MessageRepository {

    override suspend fun getInbox(currentUserId: String): List<MessageThread> {
        return try {
            val messages = supabase.from("messages")
                .select(columns = Columns.raw("id,sender_id,receiver_id,message_text,created_at,read_at")) {
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

            messages
                .groupBy { msg ->
                    if (msg.senderId == currentUserId) msg.receiverId ?: "" else msg.senderId ?: ""
                }
                .filter { it.key.isNotBlank() }
                .map { (contactId, msgs) ->
                    val lastMsg    = msgs.first()
                    val unreadCount = msgs.count { it.receiverId == currentUserId && it.readAt == null }
                    val profile    = getProfileName(contactId)
                    MessageThread(
                        contactId   = UuidString(contactId),
                        contactName = profile?.fullName ?: "Usuario",
                        lastMessage = lastMsg.messageText ?: "",
                        unread      = unreadCount > 0
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
                .select(columns = Columns.raw("id,sender_id,receiver_id,message_text,created_at,read_at")) {
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
                messageText = content.trim()
            )
            supabase.from("messages").insert(dto)
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

    override suspend fun save(message: Message) {
        val dto = MessageSupabaseDto(
            senderId    = message.senderId.value,
            receiverId  = message.receiverId?.value,
            messageText = message.content
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
