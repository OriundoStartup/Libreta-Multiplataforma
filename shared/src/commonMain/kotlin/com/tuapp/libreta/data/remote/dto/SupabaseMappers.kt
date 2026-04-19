package com.tuapp.libreta.data.remote.dto

import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.data.util.AuditOrigin
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.model.*

private fun String.toUuidOrLog(field: String, flow: String): UuidString? {
    val uuid = this.toUuidOrNull()
    if (uuid == null) {
        AppLogger.auditInvalidUuid(flow, field, this, AuditOrigin.DATA)
    }
    return uuid
}

fun AttendanceDto.toDomain() = Attendance(
    id              = id.toUuidOrNull(),
    studentId       = UuidString(studentId),
    date            = date,
    status          = runCatching { AttendanceStatus.valueOf(status.uppercase()) }.getOrElse { AttendanceStatus.ABSENT },
    justificationId = justificationId.toUuidOrNull()
)

fun StudentDto.toDomain() = Student(
    id                   = UuidString(id),
    fullName             = fullName,
    courseId             = UuidString(courseId),
    parentId             = UuidString(parentId),
    attendancePercentage = attendancePercentage
)

fun SchoolDto.toDomain()           = School(UuidString(id), name, address)
fun CourseAssignmentDto.toDomain() = CourseAssignment(
    id = id.toUuidOrNull(),
    teacherId = UuidString(teacherId),
    courseId = UuidString(courseId),
    schoolId = UuidString(schoolId),
    isHeadTeacher = isHeadTeacher
)

fun JustificationSupabaseDto.toDomain() = Justification(
    id        = id.toUuidOrNull(),
    studentId = UuidString(studentId),
    date      = date,
    reason    = reason,
    status    = runCatching { JustificationStatus.valueOf(status.uppercase()) }.getOrElse { JustificationStatus.PENDING }
)

fun MessageSupabaseDto.toDomain() = Message(
    id = id.toUuidOrNull(),
    senderId = UuidString(senderId),
    receiverId = receiverId.toUuidOrNull(),
    content = content
)

fun CommunicationDto.toDomain() = Message(
    id         = id.toUuidOrNull(),
    senderId   = UuidString(senderId),
    receiverId = receiverId.toUuidOrNull(),
    content    = messageText
)

// Domain → DTO
fun Attendance.toDto() = AttendanceDto(
    id              = id?.value,
    studentId       = studentId.value,
    date            = date,
    status          = status.name,
    justificationId = justificationId?.value
)

fun Student.toDto() = StudentDto(
    id                   = id.value,
    fullName             = fullName,
    courseId             = courseId.value,
    parentId             = parentId.value,
    attendancePercentage = attendancePercentage
)
