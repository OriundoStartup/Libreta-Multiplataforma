package com.tuapp.libreta.data.mapper

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.db.AttendanceEntity
import com.tuapp.libreta.db.ClassEntity
import com.tuapp.libreta.db.JustificationEntity
import com.tuapp.libreta.db.MessageEntity
import com.tuapp.libreta.db.ProfileEntity
import com.tuapp.libreta.db.StudentEntity
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.model.ClassRoom
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.model.JustificationStatus
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.model.Profile
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.model.UserRole

fun now(): Long = currentEpochMs()

fun ProfileEntity.toDomain()       = Profile(UuidString(id), UserRole.valueOf(role), full_name)
fun ClassEntity.toDomain()         = ClassRoom(UuidString(id), class_code, name, UuidString(teacher_id))
fun StudentEntity.toDomain()       = Student(UuidString(id), full_name, UuidString(course_id), UuidString(parent_id))
fun AttendanceEntity.toDomain()    = Attendance(id.toUuidOrNull(), UuidString(student_id), date, AttendanceStatus.valueOf(status), justification_id.toUuidOrNull())
fun JustificationEntity.toDomain() = Justification(id.toUuidOrNull(), UuidString(student_id), date, reason, JustificationStatus.valueOf(status))
fun MessageEntity.toDomain()       = Message(id.toUuidOrNull(), UuidString(sender_id), receiver_id.toUuidOrNull(), content)
