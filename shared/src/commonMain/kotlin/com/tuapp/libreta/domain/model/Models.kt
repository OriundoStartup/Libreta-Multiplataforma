package com.tuapp.libreta.domain.model

data class Profile(
    val id: String,
    val role: UserRole,
    val firstName: String,
    val lastName: String,
    val email: String
)

data class ClassRoom(
    val id: String,
    val classCode: String,
    val name: String,
    val teacherId: String
)

data class Student(
    val id: String,
    val rut: String,
    val firstName: String,
    val lastName: String,
    val parentId: String,
    val classId: String
)

data class Attendance(
    val id: String,
    val studentId: String,
    val date: Long,
    val status: AttendanceStatus
)

data class Justification(
    val id: String,
    val studentId: String,
    val date: Long,
    val reason: String,
    val status: JustificationStatus
)

data class Message(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String
)
