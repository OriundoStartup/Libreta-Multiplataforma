package com.tuapp.libreta.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * FASE 1 — DTOs alineados con el schema real post-`002_normalize_3nf`.
 *
 * Coexisten con [SupabaseDtos.kt] (legacy) durante la migración.
 * El plan es:
 *   1. Migrar repos uno por uno a los DTOs V2 (un PR por repo).
 *   2. Cuando ningún caller use los DTOs viejos → eliminar `SupabaseDtos.kt`.
 *
 * Reglas:
 *   - Nombre del data class V2 sin sufijo "Supabase" para distinguir.
 *   - `@EncodeDefault(NEVER)` en TODOS los nullable opcionales para evitar
 *     mandar `null` en columnas con default o columnas inexistentes.
 *   - NUNCA incluir campos que el schema 002 eliminó (course_id en attendance,
 *     school_name en courses, course_id en profiles).
 *
 * Fuente de verdad: supabase/CLAUDE.md (no inventar columnas).
 */

// ── students ─────────────────────────────────────────────────────────────────
@Serializable
data class StudentDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null,
    @SerialName("full_name") val fullName: String,
    @SerialName("student_rut") @EncodeDefault(EncodeDefault.Mode.NEVER) val studentRut: String? = null,
    @SerialName("course_id") val courseId: String,
    @SerialName("parent_id") val parentId: String,
    @SerialName("created_at") @EncodeDefault(EncodeDefault.Mode.NEVER) val createdAt: String? = null
)

// ── attendance ───────────────────────────────────────────────────────────────
@Serializable
data class AttendanceDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null,
    @SerialName("student_id") val studentId: String,
    val date: String, // ISO YYYY-MM-DD
    val status: String, // PRESENT | ABSENT | LATE
    @SerialName("created_at") @EncodeDefault(EncodeDefault.Mode.NEVER) val createdAt: String? = null
)

// ── courses ──────────────────────────────────────────────────────────────────
@Serializable
data class CourseDtoV2(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null,
    val name: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val description: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val subject: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val grade: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val section: String? = null,
    @SerialName("class_code") @EncodeDefault(EncodeDefault.Mode.NEVER) val classCode: String? = null,
    @SerialName("school_id") @EncodeDefault(EncodeDefault.Mode.NEVER) val schoolId: String? = null,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("invite_code") @EncodeDefault(EncodeDefault.Mode.NEVER) val inviteCode: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") @EncodeDefault(EncodeDefault.Mode.NEVER) val createdAt: String? = null
)

// ── profiles ─────────────────────────────────────────────────────────────────
@Serializable
data class ProfileDtoV2(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null,
    @SerialName("full_name") @EncodeDefault(EncodeDefault.Mode.NEVER) val fullName: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val email: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val role: String? = null,
    @SerialName("created_at") @EncodeDefault(EncodeDefault.Mode.NEVER) val createdAt: String? = null
)

// ── messages ─────────────────────────────────────────────────────────────────
@Serializable
data class MessageDtoV2(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    @SerialName("message_text") val messageText: String, // schema usa message_text, no content
    @SerialName("read_at") @EncodeDefault(EncodeDefault.Mode.NEVER) val readAt: String? = null,
    @SerialName("created_at") @EncodeDefault(EncodeDefault.Mode.NEVER) val createdAt: String? = null
)

// ── communications ───────────────────────────────────────────────────────────
@Serializable
data class CommunicationDtoV2(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null,
    @SerialName("sender_id") val senderId: String,
    @SerialName("course_id") val courseId: String,
    @SerialName("message_text") val messageText: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val category: String? = null,
    @SerialName("created_at") @EncodeDefault(EncodeDefault.Mode.NEVER) val createdAt: String? = null
)

// ── enrollments ──────────────────────────────────────────────────────────────
@Serializable
data class EnrollmentDtoV2(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null,
    @SerialName("course_id") val courseId: String,
    @SerialName("parent_id") val parentId: String,
    @SerialName("student_id") @EncodeDefault(EncodeDefault.Mode.NEVER) val studentId: String? = null,
    @SerialName("student_name") val studentName: String,
    @SerialName("student_rut") @EncodeDefault(EncodeDefault.Mode.NEVER) val studentRut: String? = null,
    @SerialName("enrolled_at") @EncodeDefault(EncodeDefault.Mode.NEVER) val enrolledAt: String? = null
)

// ── invitation_codes ─────────────────────────────────────────────────────────
// NOTA: schema NO tiene course_id ni target_role. Removidos en V2.
@Serializable
data class InvitationCodeDtoV2(
    val code: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("claimed_by") @EncodeDefault(EncodeDefault.Mode.NEVER) val claimedBy: String? = null,
    @SerialName("expires_at") @EncodeDefault(EncodeDefault.Mode.NEVER) val expiresAt: String? = null,
    @SerialName("created_at") @EncodeDefault(EncodeDefault.Mode.NEVER) val createdAt: String? = null
)

// ── course_assignments ───────────────────────────────────────────────────────
// is_head_teacher añadido (estaba ausente en V1 a pesar de existir en BD).
@Serializable
data class CourseAssignmentDtoV2(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("course_id") val courseId: String,
    @SerialName("school_id") val schoolId: String,
    @SerialName("is_head_teacher") val isHeadTeacher: Boolean = false,
    @SerialName("created_at") @EncodeDefault(EncodeDefault.Mode.NEVER) val createdAt: String? = null
)

// ── justifications ───────────────────────────────────────────────────────────
@Serializable
data class JustificationDtoV2(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null,
    @SerialName("student_id") val studentId: String,
    val date: String, // ISO YYYY-MM-DD
    val reason: String,
    val status: String = "PENDING",
    @SerialName("document_url") @EncodeDefault(EncodeDefault.Mode.NEVER) val documentUrl: String? = null,
    @SerialName("created_at") @EncodeDefault(EncodeDefault.Mode.NEVER) val createdAt: String? = null
)

// ── schools ──────────────────────────────────────────────────────────────────
@Serializable
data class SchoolDtoV2(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val id: String? = null,
    val name: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val address: String? = null,
    @SerialName("created_at") @EncodeDefault(EncodeDefault.Mode.NEVER) val createdAt: String? = null
)

// TODO[FASE-1]:
//   1. Crear SupabaseMappersV2.kt con extension fun para domain ↔ V2.
//   2. Migrar SupabaseStudentRepository → usar StudentDto + EnrollmentDtoV2.
//   3. Migrar SupabaseAttendanceDataSource → usar AttendanceDto (sin courseId).
//   4. Migrar SupabaseMessageRepository → usar MessageDtoV2 (message_text, no content).
//   5. ... seguir hasta que ningún caller use SupabaseDtos.kt.
//   6. Marcar SupabaseDtos.kt como @Deprecated y borrar en cleanup final.
