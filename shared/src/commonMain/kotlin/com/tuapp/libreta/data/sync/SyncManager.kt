package com.tuapp.libreta.data.sync

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.data.util.epochMsToIso
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.util.getIoDispatcher
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * SyncManager - Versión Robusta para Wasm.
 * Se eliminan los genéricos complejos para evitar fallos de enlazado en el compilador IR.
 */
class SyncManager(
    private val queries: LibretaAppQueries,
    private val supabase: SupabaseClient
) {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    suspend fun syncAll() = withContext(getIoDispatcher()) {
        if (_isSyncing.value) return@withContext
        _isSyncing.value = true
        AppLogger.d("SyncManager", "Starting bidirectional synchronization (PUSH + PULL)...")

        try {
            // 1. PUSH: Subir cambios locales al servidor
            syncAttendance()
            syncStudents()
            syncProfiles()
            syncGrades()
            syncCourses()
            syncJustifications()
            
            // 2. PULL: Descargar cambios nuevos desde el servidor
            pullAll()
            
            AppLogger.d("SyncManager", "Bidirectional synchronization completed successfully.")
        } catch (e: Exception) {
            AppLogger.e("SyncManager", "Sync failed: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Descarga incremental de todas las tablas desde Supabase.
     */
    suspend fun pullAll() = withContext(getIoDispatcher()) {
        AppLogger.d("SyncManager", "Starting PULL phase...")
        val tables = listOf("profiles", "courses", "students", "attendance", "justifications", "grades")
        
        tables.forEach { table ->
            runCatching { pullTable(table) }.onFailure { e ->
                AppLogger.e("SyncManager", "Pull failed for table $table: ${e.message}")
                queries.recordSyncError(e.message, table)
            }
        }
    }

    private suspend fun pullTable(tableName: String) {
        val lastPullIso = queries.getLastPullAt(tableName).executeAsOneOrNull()?.let { 
            if (it > 0) epochMsToIso(it) else "1970-01-01T00:00:00Z"
        } ?: "1970-01-01T00:00:00Z"

        AppLogger.d("SyncManager", "Pulling $tableName since $lastPullIso")

        val query = supabase.from(tableName).select {
            filter { gt("updated_at", lastPullIso) }
        }

        when (tableName) {
            "profiles" -> {
                val remote = query.decodeList<com.tuapp.libreta.data.remote.dto.ProfileSyncDto>()
                remote.forEach { dto ->
                    queries.insertOrReplaceProfile(
                        dto.id, dto.fullName, dto.role ?: "PARENT", 
                        1, 0, SyncStatus.SYNCED.name, 
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt),
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt)
                    )
                }
            }
            "courses" -> {
                val remote = query.decodeList<com.tuapp.libreta.data.remote.dto.CourseSyncDto>()
                remote.forEach { dto ->
                    queries.insertOrReplaceCourse(
                        dto.id, dto.name, dto.description, dto.subject, dto.grade,
                        dto.section, dto.teacherId, dto.schoolId, dto.inviteCode,
                        if (dto.isActive) 1 else 0, 1, 0, SyncStatus.SYNCED.name,
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt),
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt)
                    )
                }
            }
            "students" -> {
                val remote = query.decodeList<com.tuapp.libreta.data.remote.dto.StudentSyncDto>()
                remote.forEach { dto ->
                    queries.insertOrReplaceStudent(
                        dto.id, dto.fullName, dto.studentRut, dto.courseId, dto.parentId,
                        1, 0, SyncStatus.SYNCED.name,
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt),
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt)
                    )
                }
            }
            "attendance" -> {
                val remote = query.decodeList<com.tuapp.libreta.data.remote.dto.AttendanceSyncDto>()
                remote.forEach { dto ->
                    queries.insertOrReplaceAttendance(
                        dto.id, dto.studentId, dto.date, dto.status,
                        1, 0, SyncStatus.SYNCED.name,
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt),
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt)
                    )
                }
            }
            "justifications" -> {
                val remote = query.decodeList<com.tuapp.libreta.data.remote.dto.JustificationSyncDto>()
                remote.forEach { dto ->
                    queries.insertOrReplaceJustification(
                        dto.id, dto.studentId, null, null, dto.date, dto.reason, dto.status,
                        1, 0, SyncStatus.SYNCED.name,
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt),
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt)
                    )
                }
            }
            "grades" -> {
                val remote = query.decodeList<com.tuapp.libreta.data.remote.dto.GradeSyncDto>()
                remote.forEach { dto ->
                    queries.insertOrReplaceGrade(
                        dto.id, dto.studentId, dto.courseId, dto.title, dto.score,
                        dto.weight, dto.term, dto.subject, 1, 0, SyncStatus.SYNCED.name,
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt),
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt)
                    )
                }
            }
        }

        queries.setLastPullAt(tableName, com.tuapp.libreta.data.util.currentEpochMs(), tableName)
    }

    private suspend fun syncAttendance() {
        val pending = queries.getUnsyncedAttendanceEntities().asFlow().mapToList(getIoDispatcher()).first()
        if (pending.isEmpty()) return

        val (toUpsert, toDelete) = pending.partition { it.sync_status != SyncStatus.PENDING_DELETE.name }

        // Bulk Upsert
        if (toUpsert.isNotEmpty()) {
            val dtos = toUpsert.map { entity ->
                mapOf(
                    "id" to entity.id,
                    "student_id" to entity.student_id,
                    "date" to entity.date,
                    "status" to entity.status,
                    "updated_at" to epochMsToIso(entity.updated_at)
                )
            }
            try {
                supabase.from("attendance").upsert(dtos)
                toUpsert.forEach { entity ->
                    queries.insertOrReplaceAttendance(
                        entity.id, entity.student_id, entity.date, entity.status,
                        entity.server_version, entity.is_deleted, SyncStatus.SYNCED.name,
                        entity.created_at, entity.updated_at
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Bulk upsert attendance failed: ${e.message}")
            }
        }

        // Bulk Delete
        if (toDelete.isNotEmpty()) {
            try {
                val ids = toDelete.map { it.id }
                supabase.from("attendance").delete { filter { isIn("id", ids) } }
                ids.forEach { queries.deleteAttendanceEntity(it) }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Bulk delete attendance failed: ${e.message}")
            }
        }
    }

    private suspend fun syncStudents() {
        val pending = queries.getUnsyncedStudentEntities().asFlow().mapToList(getIoDispatcher()).first()
        if (pending.isEmpty()) return

        val (toUpsert, toDelete) = pending.partition { it.sync_status != SyncStatus.PENDING_DELETE.name }

        if (toUpsert.isNotEmpty()) {
            val dtos = toUpsert.map { entity ->
                val names = entity.full_name.split(" ")
                mapOf(
                    "id" to entity.id,
                    "first_name" to (names.firstOrNull() ?: ""),
                    "last_name" to names.drop(1).joinToString(" "),
                    "course_id" to entity.course_id,
                    "parent_id" to entity.parent_id,
                    "updated_at" to epochMsToIso(entity.updated_at)
                )
            }
            try {
                supabase.from("students").upsert(dtos)
                toUpsert.forEach { entity ->
                    queries.insertOrReplaceStudent(
                        entity.id, entity.full_name, entity.student_rut,
                        entity.course_id, entity.parent_id,
                        entity.server_version, entity.is_deleted,
                        SyncStatus.SYNCED.name, entity.created_at, entity.updated_at
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Bulk upsert students failed: ${e.message}")
            }
        }

        if (toDelete.isNotEmpty()) {
            try {
                val ids = toDelete.map { it.id }
                supabase.from("students").delete { filter { isIn("id", ids) } }
                ids.forEach { queries.deleteStudentEntity(it) }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Bulk delete students failed: ${e.message}")
            }
        }
    }

    private suspend fun syncProfiles() {
        val pending = queries.getUnsyncedProfileEntities().asFlow().mapToList(getIoDispatcher()).first()
        if (pending.isEmpty()) return

        val (toUpsert, toDelete) = pending.partition { it.sync_status != SyncStatus.PENDING_DELETE.name }

        if (toUpsert.isNotEmpty()) {
            val dtos = toUpsert.map { entity ->
                mapOf(
                    "id" to entity.id,
                    "full_name" to entity.full_name,
                    "role" to entity.role,
                    "updated_at" to epochMsToIso(entity.updated_at)
                )
            }
            try {
                supabase.from("profiles").upsert(dtos)
                toUpsert.forEach { entity ->
                    queries.insertOrReplaceProfile(
                        entity.id, entity.full_name, entity.role,
                        entity.server_version, entity.is_deleted,
                        SyncStatus.SYNCED.name, entity.created_at, entity.updated_at
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Bulk upsert profiles failed: ${e.message}")
            }
        }

        if (toDelete.isNotEmpty()) {
            try {
                val ids = toDelete.map { it.id }
                supabase.from("profiles").delete { filter { isIn("id", ids) } }
                ids.forEach { queries.deleteProfileEntity(it) }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Bulk delete profiles failed: ${e.message}")
            }
        }
    }

    private suspend fun syncGrades() {
        val pending = queries.getUnsyncedGradeEntities().asFlow().mapToList(getIoDispatcher()).first()
        if (pending.isEmpty()) return

        val (toUpsert, toDelete) = pending.partition { it.sync_status != SyncStatus.PENDING_DELETE.name }

        if (toUpsert.isNotEmpty()) {
            val dtos = toUpsert.map { entity ->
                mapOf(
                    "id" to entity.id,
                    "student_id" to entity.student_id,
                    "course_id" to entity.course_id,
                    "title" to entity.title,
                    "score" to entity.score,
                    "weight" to entity.weight,
                    "term" to entity.term,
                    "subject" to entity.subject,
                    "updated_at" to epochMsToIso(entity.updated_at)
                )
            }
            try {
                supabase.from("grades").upsert(dtos)
                toUpsert.forEach { entity ->
                    queries.insertOrReplaceGrade(
                        entity.id, entity.student_id, entity.course_id,
                        entity.title, entity.score, entity.weight,
                        entity.term, entity.subject,
                        entity.server_version, entity.is_deleted,
                        SyncStatus.SYNCED.name,
                        entity.created_at, entity.updated_at
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Bulk upsert grades failed: ${e.message}")
            }
        }

        if (toDelete.isNotEmpty()) {
            try {
                val ids = toDelete.map { it.id }
                supabase.from("grades").delete { filter { isIn("id", ids) } }
                ids.forEach { queries.deleteGradeEntity(it) }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Bulk delete grades failed: ${e.message}")
            }
        }
    }

    private suspend fun syncCourses() {
        val pending = queries.getUnsyncedCourseEntities().asFlow().mapToList(getIoDispatcher()).first()
        if (pending.isEmpty()) return

        val (toUpsert, toDelete) = pending.partition { it.sync_status != SyncStatus.PENDING_DELETE.name }

        if (toUpsert.isNotEmpty()) {
            val dtos = toUpsert.map { entity ->
                mapOf(
                    "id" to entity.id,
                    "name" to entity.name,
                    "description" to entity.description,
                    "subject" to entity.subject,
                    "grade" to entity.grade,
                    "teacher_id" to entity.teacher_id,
                    "invite_code" to entity.invite_code,
                    "updated_at" to epochMsToIso(entity.updated_at)
                )
            }
            try {
                supabase.from("courses").upsert(dtos)
                toUpsert.forEach { entity ->
                    queries.insertOrReplaceCourse(
                        entity.id, entity.name, entity.description, entity.subject,
                        entity.grade, entity.section, entity.teacher_id, entity.school_id,
                        entity.invite_code, entity.is_active, entity.server_version,
                        entity.is_deleted, SyncStatus.SYNCED.name, entity.created_at,
                        entity.updated_at
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Bulk upsert courses failed: ${e.message}")
            }
        }

        if (toDelete.isNotEmpty()) {
            try {
                val ids = toDelete.map { it.id }
                supabase.from("courses").delete { filter { isIn("id", ids) } }
                ids.forEach { queries.deleteCourseEntity(it) }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Bulk delete courses failed: ${e.message}")
            }
        }
    }

    private suspend fun syncJustifications() {
        val pending = queries.getUnsyncedJustificationEntities().asFlow().mapToList(getIoDispatcher()).first()
        if (pending.isEmpty()) return

        val (toUpsert, toDelete) = pending.partition { it.sync_status != SyncStatus.PENDING_DELETE.name }

        if (toUpsert.isNotEmpty()) {
            val dtos = toUpsert.map { entity ->
                mapOf(
                    "id" to entity.id,
                    "student_id" to entity.student_id,
                    "date" to entity.date,
                    "reason" to entity.reason,
                    "status" to entity.status,
                    "updated_at" to epochMsToIso(entity.updated_at)
                )
            }
            try {
                supabase.from("justifications").upsert(dtos)
                toUpsert.forEach { entity ->
                    queries.insertOrReplaceJustification(
                        entity.id, entity.student_id, entity.student_name,
                        entity.course_name, entity.date, entity.reason,
                        entity.status, entity.server_version, entity.is_deleted,
                        SyncStatus.SYNCED.name, entity.created_at, entity.updated_at
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Bulk upsert justifications failed: ${e.message}")
            }
        }

        if (toDelete.isNotEmpty()) {
            try {
                val ids = toDelete.map { it.id }
                supabase.from("justifications").delete { filter { isIn("id", ids) } }
                ids.forEach { queries.deleteJustificationEntity(it) }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Bulk delete justifications failed: ${e.message}")
            }
        }
    }
}
