package com.tuapp.libreta.util

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.*
import kotlin.random.Random

/**
 * TestDataFactory - Centraliza la generación de datos para pruebas.
 * Evita el hardcoding de IDs y strings, permitiendo tests más robustos y dinámicos.
 */
object TestDataFactory {

    fun randomUuid() = UuidString("00000000-0000-0000-0000-${Random.nextInt(100000, 999999)}")

    fun makeStudent(
        id: UuidString = randomUuid(),
        courseId: UuidString = randomUuid(),
        parentId: UuidString = randomUuid(),
        fullName: String = "Student ${Random.nextInt(100)}"
    ) = Student(
        id = id,
        fullName = fullName,
        courseId = courseId,
        parentId = parentId
    )

    fun makeAttendance(
        studentId: UuidString = randomUuid(),
        date: String = "2024-01-01",
        status: AttendanceStatus = AttendanceStatus.PRESENT
    ) = Attendance(
        studentId = studentId,
        date = date,
        status = status
    )

    fun makeProfile(
        id: UuidString = randomUuid(),
        fullName: String = "User ${Random.nextInt(100)}",
        role: UserRole? = UserRole.PARENT
    ) = Profile(
        id = id,
        fullName = fullName,
        role = role
    )

    fun makeCourse(
        id: String = randomUuid().value,
        name: String = "Course ${Random.nextInt(100)}",
        inviteCode: String = "CODE${Random.nextInt(10, 99)}"
    ) = Course(
        id = id,
        teacherId = randomUuid().value,
        name = name,
        description = "Description",
        subject = "Math",
        grade = "1st",
        schoolName = "School",
        inviteCode = inviteCode,
        isActive = true,
        createdAt = "2024-01-01"
    )
}
