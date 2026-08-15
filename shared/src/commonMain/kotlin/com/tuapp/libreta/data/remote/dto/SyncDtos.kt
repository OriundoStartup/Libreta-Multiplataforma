package com.tuapp.libreta.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs para la Sincronización Bidireccional (PULL).
 * Estos objetos representan la "Fuente de la Verdad" del servidor.
 */

@Serializable
data class ProfileSyncDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val role: String?,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class CourseSyncDto(
    val id: String,
    val name: String,
    val description: String?,
    val subject: String?,
    val grade: String?,
    val section: String?,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("school_id") val schoolId: String?,
    @SerialName("invite_code") val inviteCode: String?,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class StudentSyncDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("student_rut") val studentRut: String?,
    @SerialName("course_id") val courseId: String,
    @SerialName("parent_id") val parentId: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class AttendanceSyncDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    val date: String,
    val status: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class JustificationSyncDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    val date: String,
    val reason: String,
    val status: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class GradeSyncDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("course_id") val courseId: String,
    val title: String,
    val score: Double,
    val weight: Double,
    val term: String?,
    val subject: String?,
    @SerialName("updated_at") val updatedAt: String
)
