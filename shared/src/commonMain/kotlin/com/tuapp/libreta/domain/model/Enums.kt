package com.tuapp.libreta.domain.model

enum class UserRole { TEACHER, PARENT }
enum class AttendanceStatus { PRESENT, ABSENT, LATE }
enum class JustificationStatus { PENDING, APPROVED, REJECTED }
enum class SyncStatus { SYNCED, PENDING_INSERT, PENDING_UPDATE, PENDING_DELETE }

enum class NoticeCategory(val label: String, val emoji: String) {
    URGENT("Urgente",     "🚨"),
    INFO("Informativo",   "ℹ️"),
    ACADEMIC("Académico", "📚")
}
