package com.tuapp.libreta.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// attendance: id, student_id, date(date→String), status, justification_id, created_at
@Serializable
data class AttendanceDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("student_id")      val studentId: String,
    val date: String,              // DATE type → "YYYY-MM-DD"
    val status: String,
    @SerialName("justification_id")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val justificationId: String? = null,
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: String? = null
)

// students: id, full_name, course_id, parent_id, attendance_percentage
@Serializable
data class StudentDto(
    val id: String,
    @SerialName("full_name")              val fullName: String,
    @SerialName("course_id")              val courseId: String,
    @SerialName("parent_id")              val parentId: String,
    @SerialName("attendance_percentage")  val attendancePercentage: Double = 0.0
)

// communications: id, sender_id, receiver_id, student_id, course_id, message_text, category, is_internal, created_at
@Serializable
data class CommunicationDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("sender_id")   val senderId: String,
    @SerialName("receiver_id")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val receiverId: String? = null,
    @SerialName("student_id")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val studentId: String? = null,
    @SerialName("course_id")   val courseId: String,
    @SerialName("message_text") val messageText: String,
    val category: String = "AVISO_GENERAL",
    @SerialName("is_internal") val isInternal: Boolean = false,
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: String? = null
)

// courses: id, name, teacher_id, created_at
@Serializable
data class CourseDto(
    val id: String,
    val name: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: String? = null
)

// profiles: id, full_name, role, updated_at
@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    val role: String? = null,
    @SerialName("course_id") val courseId: String? = null,
    @SerialName("updated_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val updatedAt: String? = null
)

@Serializable
data class SchoolDto(
    val id: String,
    val name: String,
    val address: String = ""
)

@Serializable
data class CourseAssignmentDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("teacher_id")      val teacherId: String,
    @SerialName("course_id")       val courseId: String,
    @SerialName("school_id")       val schoolId: String,
    @SerialName("is_head_teacher") val isHeadTeacher: Boolean = false
)

// justifications: id, student_id, parent_id, date(bigint), reason, status, created_at
@Serializable
data class JustificationSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("student_id") val studentId: String,
    @SerialName("parent_id")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val parentId: String? = null,
    val date: Long,
    val reason: String,
    val status: String = "PENDING",
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: String? = null
)

// messages table (direct messages between users)
@Serializable
data class MessageSupabaseDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: String? = null,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String?,
    val content: String
)
