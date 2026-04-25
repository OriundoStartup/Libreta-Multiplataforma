package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.epochMsToIso
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.model.InvitationCode
import com.tuapp.libreta.domain.repository.InvitationCodeRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class InvitationCodeDto(
    val code: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("claimed_by") val claimedBy: String? = null,
    @SerialName("expires_at") val expiresAt: String
)

@Serializable
private data class InvitationCodeInsertDto(
    val code: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("claimed_by") val claimedBy: String? = null,
    @SerialName("expires_at") val expiresAt: String
)

private fun InvitationCodeDto.toDomain(): InvitationCode {
    val expiresMs = runCatching { Instant.parse(expiresAt).toEpochMilliseconds() }.getOrElse { 0L }
    return InvitationCode(
        code = code,
        studentId = UuidString(studentId),
        teacherId = UuidString(teacherId),
        claimedBy = claimedBy?.toUuidOrNull(),
        expiresAt = expiresMs
    )
}

class SupabaseInvitationRepository(private val supabase: SupabaseClient) : InvitationCodeRepository {

    override suspend fun generate(studentId: UuidString, teacherId: UuidString): InvitationCode {
        val code = supabase.postgrest.rpc("generate_invite_code").decodeAs<String>()
        val expiresAt = epochMsToIso(currentEpochMs() + 7 * 24 * 3600 * 1000L)
        supabase.from("invitation_codes").upsert(
            InvitationCodeInsertDto(code, studentId.value, teacherId.value, null, expiresAt)
        )
        return InvitationCode(code, studentId, teacherId, null, Instant.parse(expiresAt).toEpochMilliseconds())
    }

    override suspend fun claim(code: String, parentId: UuidString): Result<InvitationCode> = runCatching {
        val params = buildJsonObject {
            put("p_code", code.uppercase())
            put("p_parent_id", parentId.value)
        }
        val claimed = supabase.postgrest.rpc(
            "claim_invitation_code",
            params
        ).decodeAs<InvitationCodeDto>()
        claimed.toDomain()
    }

    override fun getByTeacher(teacherId: UuidString): Flow<List<InvitationCode>> = flow {
        try {
            emit(
                supabase.from("invitation_codes")
                    .select { filter { eq("teacher_id", teacherId.value) } }
                    .decodeList<InvitationCodeDto>()
                    .map { it.toDomain() }
            )
        } catch (_: Exception) {
            emit(emptyList())
        }
    }
}
