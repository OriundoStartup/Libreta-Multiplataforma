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
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    @SerialName("class_id") val classId: String?,
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
    val status: String = "PENDING"
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
    @SerialName("student_id") val studentId: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("claimed_by") val claimedBy: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null
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
    @SerialName("message_text") val messageText: String,
    val category: String? = null,
    @SerialName("is_internal") val isInternal: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class MessageSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("sender_id") val senderId: String? = null,
    @SerialName("receiver_id") val receiverId: String? = null,
    @SerialName("student_id") val studentId: String? = null,
    @SerialName("course_id") val courseId: String? = null,
    @SerialName("message_text") val messageText: String? = null,
    val category: String? = null,
    @SerialName("is_internal") val isInternal: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("read_at") val readAt: String? = null
)
