package com.tuapp.libreta.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileSyncDto(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    val role: String? = null,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class CourseSyncDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val subject: String? = null,
    val grade: String? = null,
    val section: String? = null,
    @SerialName("teacher_id") val teacherId: String = "",
    @SerialName("school_id") val schoolId: String? = null,
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class StudentSyncDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("student_rut") val studentRut: String? = null,
    @SerialName("course_id") val courseId: String,
    @SerialName("parent_id") val parentId: String,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class AttendanceSyncDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    val date: String,
    val status: String,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class JustificationSyncDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    val date: String,
    val reason: String,
    val status: String,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class GradeSyncDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("course_id") val courseId: String,
    val title: String,
    val score: Double,
    val weight: Double,
    val term: String? = null,
    val subject: String? = null,
    @SerialName("updated_at") val updatedAt: String = ""
)
