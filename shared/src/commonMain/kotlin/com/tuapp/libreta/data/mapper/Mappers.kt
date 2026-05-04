package com.tuapp.libreta.data.mapper

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.db.AttendanceEntity
import com.tuapp.libreta.db.CommunicationEntity
import com.tuapp.libreta.db.CourseEntity
import com.tuapp.libreta.db.GradeEntity
import com.tuapp.libreta.db.InvitationCodeEntity
import com.tuapp.libreta.db.JustificationEntity
import com.tuapp.libreta.db.MessageEntity
import com.tuapp.libreta.db.ProfileEntity
import com.tuapp.libreta.db.SchoolEntity
import com.tuapp.libreta.db.StudentEntity
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.model.Communication
import com.tuapp.libreta.domain.model.Course
import com.tuapp.libreta.domain.model.Grade
import com.tuapp.libreta.domain.model.InvitationCode
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.model.JustificationStatus
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.model.Profile
import com.tuapp.libreta.domain.model.School
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.model.UserRole

fun now(): Long = currentEpochMs()

fun ProfileEntity.toDomain() = Profile(UuidString(id), UserRole.valueOf(role), full_name)

fun CourseEntity.toDomain() = Course(
    id = id,
    teacherId = teacher_id,
    name = name,
    description = description,
    subject = subject,
    grade = grade,
    schoolName = school_id,
    inviteCode = invite_code ?: "",
    isActive = is_active == 1L,
    createdAt = ""
)

fun StudentEntity.toDomain() = Student(
    id = UuidString(id),
    fullName = full_name,
    courseId = UuidString(course_id),
    parentId = UuidString(parent_id),
    attendancePercentage = 0.0
)

fun AttendanceEntity.toDomain() = Attendance(
    id = UuidString(id),
    studentId = UuidString(student_id),
    date = date,
    status = runCatching { AttendanceStatus.valueOf(status) }.getOrElse { AttendanceStatus.ABSENT },
    justificationId = null
)

fun JustificationEntity.toDomain() = Justification(
    id = UuidString(id),
    studentId = UuidString(student_id),
    studentName = student_name,
    courseName = course_name,
    date = date.toLongOrNull() ?: 0L,
    reason = reason,
    status = runCatching { JustificationStatus.valueOf(status) }.getOrElse { JustificationStatus.PENDING }
)

fun MessageEntity.toDomain() = Message(
    id = UuidString(id),
    senderId = UuidString(sender_id),
    receiverId = UuidString(receiver_id),
    content = message_text
)

fun CommunicationEntity.toDomain() = Communication(
    id = UuidString(id),
    senderId = UuidString(sender_id),
    courseId = UuidString(course_id),
    content = message_text,
    category = category,
    createdAt = created_at
)

fun InvitationCodeEntity.toDomain() = InvitationCode(
    code = code,
    studentId = UuidString(student_id),
    teacherId = UuidString(teacher_id),
    claimedBy = claimed_by?.let { UuidString(it) },
    expiresAt = expires_at
)

fun GradeEntity.toDomain() = Grade(
    id = UuidString(id),
    studentId = UuidString(student_id),
    courseId = UuidString(course_id),
    title = title,
    score = score,
    weight = weight,
    term = term,
    subject = subject,
    date = created_at
)

fun SchoolEntity.toDomain() = School(UuidString(id), name, address ?: "")
