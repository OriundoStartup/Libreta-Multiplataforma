package com.tuapp.libreta.data.sync

import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.data.util.epochMsToIso
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.util.getIoDispatcher
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SyncManager(
    private val queries: LibretaAppQueries,
    private val supabase: SupabaseClient
) {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    suspend fun syncAll() = withContext(getIoDispatcher()) {
        if (_isSyncing.value) return@withContext
        _isSyncing.value = true
        AppLogger.d("SyncManager", "Starting full synchronization...")

        try {
            syncAttendance()
            syncStudents()
            syncProfiles()
            syncGrades()
            AppLogger.d("SyncManager", "Synchronization completed successfully.")
        } catch (e: Exception) {
            AppLogger.e("SyncManager", "Sync failed: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun syncAttendance() {
        val pending = queries.getUnsyncedAttendanceEntities().executeAsList()
        if (pending.isEmpty()) return

        pending.forEach { entity ->
            try {
                when (entity.sync_status) {
                    SyncStatus.PENDING_INSERT.name, SyncStatus.PENDING_UPDATE.name -> {
                        val dto = mapOf(
                            "id" to entity.id,
                            "student_id" to entity.student_id,
                            "date" to entity.date,
                            "status" to entity.status,
                            "updated_at" to epochMsToIso(entity.updated_at)
                        )
                        supabase.from("attendance").upsert(dto)
                        queries.insertOrReplaceAttendance(
                            entity.id, entity.student_id, entity.date, entity.status,
                            SyncStatus.SYNCED.name, entity.created_at, entity.updated_at
                        )
                    }
                    SyncStatus.PENDING_DELETE.name -> {
                        supabase.from("attendance").delete { filter { eq("id", entity.id) } }
                        queries.deleteAttendanceEntity(entity.id)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Failed to sync attendance ${entity.id}: ${e.message}")
            }
        }
    }

    private suspend fun syncStudents() {
        val pending = queries.getUnsyncedStudentEntities().executeAsList()
        if (pending.isEmpty()) return

        pending.forEach { entity ->
            try {
                when (entity.sync_status) {
                    SyncStatus.PENDING_INSERT.name, SyncStatus.PENDING_UPDATE.name -> {
                        val names = entity.full_name.split(" ")
                        val dto = mapOf(
                            "id" to entity.id,
                            "first_name" to (names.firstOrNull() ?: ""),
                            "last_name" to names.drop(1).joinToString(" "),
                            "course_id" to entity.course_id,
                            "parent_id" to entity.parent_id,
                            "updated_at" to epochMsToIso(entity.updated_at)
                        )
                        supabase.from("students").upsert(dto)
                        queries.insertOrReplaceStudent(
                            entity.id, entity.full_name, entity.student_rut, 
                            entity.course_id, entity.parent_id,
                            SyncStatus.SYNCED.name, entity.created_at, entity.updated_at
                        )
                    }
                    SyncStatus.PENDING_DELETE.name -> {
                        supabase.from("students").delete { filter { eq("id", entity.id) } }
                        queries.deleteStudentEntity(entity.id)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Failed to sync student ${entity.id}: ${e.message}")
            }
        }
    }

    private suspend fun syncProfiles() {
        val pending = queries.getUnsyncedProfileEntities().executeAsList()
        if (pending.isEmpty()) return

        pending.forEach { entity ->
            try {
                when (entity.sync_status) {
                    SyncStatus.PENDING_INSERT.name, SyncStatus.PENDING_UPDATE.name -> {
                        val dto = mapOf(
                            "id" to entity.id,
                            "full_name" to entity.full_name,
                            "role" to entity.role,
                            "updated_at" to epochMsToIso(entity.updated_at)
                        )
                        supabase.from("profiles").upsert(dto)
                        queries.insertOrReplaceProfile(
                            entity.id, entity.full_name, entity.role,
                            SyncStatus.SYNCED.name, entity.created_at, entity.updated_at
                        )
                    }
                    SyncStatus.PENDING_DELETE.name -> {
                        supabase.from("profiles").delete { filter { eq("id", entity.id) } }
                        queries.deleteProfileEntity(entity.id)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Failed to sync profile ${entity.id}: ${e.message}")
            }
        }
    }

    private suspend fun syncGrades() {
        val pending = queries.getUnsyncedGradeEntities().executeAsList()
        if (pending.isEmpty()) return

        pending.forEach { entity ->
            try {
                when (entity.sync_status) {
                    SyncStatus.PENDING_INSERT.name, SyncStatus.PENDING_UPDATE.name -> {
                        val dto = mapOf(
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
                        supabase.from("grades").upsert(dto)
                        queries.insertOrReplaceGrade(
                            entity.id, entity.student_id, entity.course_id,
                            entity.title, entity.score, entity.weight,
                            entity.term, entity.subject, SyncStatus.SYNCED.name,
                            entity.created_at, entity.updated_at
                        )
                    }
                    SyncStatus.PENDING_DELETE.name -> {
                        supabase.from("grades").delete { filter { eq("id", entity.id) } }
                        queries.deleteGradeEntity(entity.id)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Failed to sync grade ${entity.id}: ${e.message}")
            }
        }
    }
}
