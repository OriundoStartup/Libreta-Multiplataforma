package com.tuapp.libreta.data.remote.dtos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class CourseDto(
    @SerialName("id") val id: String? = null,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("subject") val subject: String? = null,
    @SerialName("grade") val grade: String? = null,
    // school_name fue eliminado de la tabla courses en 002_normalize_3nf (viola 3NF).
    // @Transient → nunca se envía/recibe a Postgres; solo vive en memoria para display.
    @Transient val schoolName: String? = null,
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class EnrollmentDto(
    @SerialName("id") val id: String? = null,
    @SerialName("course_id") val courseId: String = "",
    @SerialName("student_name") val studentName: String = "",
    @SerialName("parent_id") val parentId: String = "",
    @SerialName("student_rut") val studentRut: String? = null,
    @SerialName("enrolled_at") val enrolledAt: String? = null
)
