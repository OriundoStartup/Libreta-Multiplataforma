package com.tuapp.libreta.domain.model

import com.tuapp.libreta.data.util.UuidString

data class Profile(
    val id: UuidString,
    val role: UserRole,
    val fullName: String
)

data class Course(
    val id: String,
    val teacherId: String,
    val name: String,
    val description: String?,
    val subject: String?,
    val grade: String?,
    val schoolName: String?,
    val inviteCode: String,
    val isActive: Boolean,
    val createdAt: String
)

data class ClassRoom(
    val id: UuidString,
    val classCode: String,
    val name: String,
    val teacherId: UuidString,
    val schoolId: UuidString? = null,
    val isHeadTeacher: Boolean = true
)

data class Student(
    val id: UuidString,
    val fullName: String,
    val studentRut: String? = null,
    val courseId: UuidString,
    val parentId: UuidString,
    val attendancePercentage: Double = 0.0
)

data class Attendance(
    val id: UuidString? = null,
    val studentId: UuidString,
    val date: String,
    val status: AttendanceStatus,
    val justificationId: UuidString? = null
)

data class Justification(
    val id: UuidString? = null,
    val studentId: UuidString,
    val studentName: String? = null,
    val courseName: String? = null, // Agregado para contexto del profesor
    val date: Long,
    val reason: String,
    val status: JustificationStatus,
    val documentUrl: String? = null
)

data class Message(
    val id: UuidString? = null,
    val senderId: UuidString,
    val receiverId: UuidString? = null,
    val content: String,
    val createdAt: String? = null,
    val readAt: String? = null
)

data class School(
    val id: UuidString,
    val name: String,
    val address: String
)

data class CourseAssignment(
    val id: UuidString? = null,
    val teacherId: UuidString,
    val courseId: UuidString,
    val schoolId: UuidString,
    val isHeadTeacher: Boolean
)

data class InvitationCode(
    val code: String,
    val studentId: UuidString,
    val teacherId: UuidString,
    val claimedBy: UuidString?,
    val expiresAt: Long
)

data class Communication(
    val id: UuidString,
    val senderId: UuidString,
    val courseId: UuidString,
    val content: String,
    val category: String?,
    val createdAt: Long
)

data class Grade(
    val id: UuidString? = null,
    val studentId: UuidString,
    val courseId: UuidString,
    val title: String,
    val score: Double,
    val weight: Double = 1.0,
    val term: String? = null,
    val subject: String? = null,
    val date: Long = 0L // Se asocia con currentEpochMs al crear
)

data class SubjectAverage(
    val subject: String,
    val average: Double,
    val grades: List<Grade>
)
