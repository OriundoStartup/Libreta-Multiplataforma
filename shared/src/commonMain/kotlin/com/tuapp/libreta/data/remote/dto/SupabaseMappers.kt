package com.tuapp.libreta.data.remote.dto

import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.data.util.AuditOrigin
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.model.CourseAssignment
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.model.JustificationStatus
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.model.School
import com.tuapp.libreta.domain.model.Student

private fun String.toUuidOrLog(field: String, flow: String): UuidString? {
    val uuid = this.toUuidOrNull()
    if (uuid == null) {
        AppLogger.auditInvalidUuid(flow, field, this, AuditOrigin.DATA)
    }
    return uuid
}

fun AttendanceSupabaseDto.toDomain() = Attendance(
    id              = id.toUuidOrNull(),
    studentId       = UuidString(studentId),
    date            = date,
    status          = runCatching { AttendanceStatus.valueOf(status.uppercase()) }.getOrElse { AttendanceStatus.ABSENT },
    justificationId = null // Se puede extender si hay ID de justificación en la tabla
)

fun StudentSupabaseDto.toDomain() = Student(
    id                   = UuidString(id ?: ""),
    fullName             = "$firstName $lastName",
    courseId             = UuidString(classId ?: ""),
    parentId             = UuidString(parentId ?: ""),
    attendancePercentage = 0.0
)

fun SchoolSupabaseDto.toDomain() = School(UuidString(id ?: ""), name, address ?: "Sin dirección")

fun CourseAssignmentSupabaseDto.toDomain() = CourseAssignment(
    id = id.toUuidOrNull(),
    teacherId = UuidString(teacherId),
    courseId = UuidString(courseId),
    schoolId = UuidString(schoolId ?: ""),
    isHeadTeacher = false // Ajustar si el DTO tiene este campo
)

fun JustificationSupabaseDto.toDomain() = Justification(
    id        = id.toUuidOrNull(),
    studentId = UuidString(studentId),
    date      = date.toLongOrNull() ?: 0L,
    reason    = reason,
    status    = runCatching { JustificationStatus.valueOf(status.uppercase()) }.getOrElse { JustificationStatus.PENDING }
)

fun MessageSupabaseDto.toDomain() = Message(
    id = id.toUuidOrNull(),
    senderId = senderId?.toUuidOrNull() ?: UuidString("00000000-0000-0000-0000-000000000000"),
    receiverId = receiverId.toUuidOrNull(),
    content = messageText ?: ""
)

fun CommunicationSupabaseDto.toDomain() = Message(
    id         = id.toUuidOrNull(),
    senderId   = UuidString(senderId),
    receiverId = null,
    content    = messageText
)

fun Attendance.toSupabaseDto() = AttendanceSupabaseDto(
    id              = id?.value,
    studentId       = studentId.value,
    date            = date,
    status          = status.name,
    courseId        = null // Se puede llenar si es necesario
)

fun Student.toSupabaseDto() = StudentSupabaseDto(
    id                   = id.value,
    firstName            = fullName.split(" ").firstOrNull() ?: "",
    lastName             = fullName.split(" ").drop(1).joinToString(" "),
    classId              = courseId.value,
    parentId             = parentId.value
)
