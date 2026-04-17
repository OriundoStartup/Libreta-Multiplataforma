package com.tuapp.libreta.data.mapper

import com.tuapp.libreta.db.*
import com.tuapp.libreta.domain.model.*
import kotlinx.datetime.Clock

fun now() = Clock.System.now().toEpochMilliseconds()

// ── Profile ───────────────────────────────────────────────────────────────────
fun ProfileEntity.toDomain() = Profile(id, UserRole.valueOf(role), first_name, last_name, email)

// ── ClassRoom ─────────────────────────────────────────────────────────────────
fun ClassEntity.toDomain() = ClassRoom(id, class_code, name, teacher_id)

// ── Student ───────────────────────────────────────────────────────────────────
fun StudentEntity.toDomain() = Student(id, rut, first_name, last_name, parent_id, class_id)

// ── Attendance ────────────────────────────────────────────────────────────────
fun AttendanceEntity.toDomain() = Attendance(id, student_id, date, AttendanceStatus.valueOf(status))

// ── Justification ─────────────────────────────────────────────────────────────
fun JustificationEntity.toDomain() = Justification(id, student_id, date, reason, JustificationStatus.valueOf(status))

// ── Message ───────────────────────────────────────────────────────────────────
fun MessageEntity.toDomain() = Message(id, sender_id, receiver_id, content)
