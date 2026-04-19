package com.tuapp.libreta.data.remote

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.epochMsToIso
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.model.InvitationCode
import com.tuapp.libreta.domain.repository.InvitationCodeRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
        val code      = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
        val expiresAt = epochMsToIso(currentEpochMs() + 7 * 24 * 3600 * 1000L)
        supabase.from("invitation_codes").upsert(InvitationCodeInsertDto(code, studentId.value, teacherId.value, null, expiresAt))
        return InvitationCode(code, studentId, teacherId, null, Instant.parse(expiresAt).toEpochMilliseconds())
    }

    override suspend fun claim(code: String, parentId: UuidString): Result<InvitationCode> = runCatching {
        val existing = supabase.from("invitation_codes")
            .select { filter { eq("code", code) } }
            .decodeSingle<InvitationCodeDto>()
        val domain = existing.toDomain()
        check(existing.claimedBy == null) { "Código ya utilizado" }
        check(domain.expiresAt > currentEpochMs()) { "Código expirado" }
        supabase.from("invitation_codes").update({ set("claimed_by", parentId.value) }) { filter { eq("code", code) } }
        domain.copy(claimedBy = parentId)
    }

    override fun getByTeacher(teacherId: UuidString): Flow<List<InvitationCode>> = flow {
        emit(supabase.from("invitation_codes")
            .select { filter { eq("teacher_id", teacherId.value) } }
            .decodeList<InvitationCodeDto>().map { it.toDomain() })
    }
}
