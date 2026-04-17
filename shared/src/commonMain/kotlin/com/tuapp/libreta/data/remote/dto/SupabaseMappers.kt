package com.tuapp.libreta.data.remote.dto

import com.tuapp.libreta.domain.model.*

fun AttendanceDto.toDomain() = Attendance(
    id        = id,
    studentId = studentId,
    date      = date,
    status    = AttendanceStatus.valueOf(status)
)

fun StudentDto.toDomain() = Student(
    id        = id,
    rut       = rut,
    firstName = firstName,
    lastName  = lastName,
    parentId  = parentId,
    classId   = classId
)

fun MessageDto.toDomain() = Message(
    id         = id,
    senderId   = senderId,
    receiverId = receiverId,
    content    = content
)

// Domain → DTO (para inserts remotos)
fun Attendance.toDto() = AttendanceDto(id, studentId, date, status.name)
fun Message.toDto()    = MessageDto(id, senderId, receiverId, content)
