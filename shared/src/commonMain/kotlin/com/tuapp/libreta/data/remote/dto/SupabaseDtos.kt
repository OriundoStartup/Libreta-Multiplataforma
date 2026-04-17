package com.tuapp.libreta.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AttendanceDto(
    val id: String,
    @SerialName("student_id")  val studentId: String,
    val date: Long,
    val status: String,
    @SerialName("sync_status") val syncStatus: String = "SYNCED",
    @SerialName("created_at")  val createdAt: Long = 0L,
    @SerialName("updated_at")  val updatedAt: Long = 0L
)

@Serializable
data class StudentDto(
    val id: String,
    val rut: String,
    @SerialName("first_name")  val firstName: String,
    @SerialName("last_name")   val lastName: String,
    @SerialName("parent_id")   val parentId: String,
    @SerialName("class_id")    val classId: String,
    @SerialName("sync_status") val syncStatus: String = "SYNCED",
    @SerialName("created_at")  val createdAt: Long = 0L,
    @SerialName("updated_at")  val updatedAt: Long = 0L
)

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("sender_id")   val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    val content: String,
    @SerialName("sync_status") val syncStatus: String = "SYNCED",
    @SerialName("created_at")  val createdAt: Long = 0L,
    @SerialName("updated_at")  val updatedAt: Long = 0L
)
