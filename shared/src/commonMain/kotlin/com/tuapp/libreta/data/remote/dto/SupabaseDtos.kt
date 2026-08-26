package com.tuapp.libreta.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val email: String? = null,
    val role: String? = null,
    @SerialName("course_id") val courseId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ClassRoomSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    val name: String,
    val grade: String,
    val section: String,
    @SerialName("school_id") val schoolId: String? = null
)

@Serializable
data class StudentSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    // Schema real (002_normalize_3nf): la tabla students usa full_name + course_id.
    // No existen first_name/last_name/class_id — escribirlos provoca 400 PGRST204.
    @SerialName("full_name") val fullName: String,
    @SerialName("student_rut") @EncodeDefault(EncodeDefault.Mode.NEVER) val studentRut: String? = null,
    @SerialName("course_id") val courseId: String? = null,
    @SerialName("parent_id") val parentId: String? = null
)

@Serializable
data class AttendanceSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("student_id") val studentId: String,
    val date: String,
    val status: String,
    @SerialName("course_id") val courseId: String? = null
)

@Serializable
data class JustificationSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("student_id") val studentId: String,
    val date: String,
    val reason: String,
    val status: String = "PENDING",
    @SerialName("document_url") val documentUrl: String? = null
)

@Serializable
data class CourseSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    val name: String,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val grade: String? = null,
    val description: String? = null,
    val subject: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("school_name") val schoolName: String? = null
)

@Serializable
data class EnrollmentSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("course_id") val courseId: String,
    @SerialName("parent_id") val parentId: String,
    @SerialName("student_name") val studentName: String,
    @SerialName("student_rut") val studentRut: String? = null,
    @SerialName("enrolled_at") val enrolledAt: String? = null
)

@Serializable
data class InvitationCodeSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    val code: String,
    @SerialName("student_id") val studentId: String? = null,
    @SerialName("course_id") val courseId: String? = null,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("claimed_by") val claimedBy: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("target_role") val targetRole: String = "PARENT"
)

@Serializable
data class CourseAssignmentSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("course_id") val courseId: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("school_id") val schoolId: String? = null
)

@Serializable
data class SchoolSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    val name: String,
    val address: String? = null
)

@Serializable
data class CommunicationSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("sender_id") val senderId: String,
    @SerialName("course_id") val courseId: String,
    @SerialName("content") val content: String, // Cambiado de message_text a content
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val category: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("is_internal") val isInternal: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class MessageSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("receiver_id") val receiverId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("student_id") val studentId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("course_id") val courseId: String? = null,
    @SerialName("content") val content: String? = null, // Cambiado de message_text a content
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val category: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("is_internal") val isInternal: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("created_at") val createdAt: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("read_at") val readAt: String? = null
)
