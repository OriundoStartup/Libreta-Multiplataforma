package com.tuapp.libreta.data.sync

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.db.LocalDataBridge
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.data.util.epochMsToIso
import com.tuapp.libreta.db.LibretaAppDatabase
import com.tuapp.libreta.di.dbReady
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.util.getIoDispatcher
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * SyncManager - Versión Robusta para Wasm con LocalDataBridge.
 */
class SyncManager(
    private val database: LibretaAppDatabase,
    private val supabase: SupabaseClient,
    private val bridge: LocalDataBridge
) {
    private val queries = database.libretaAppQueries
    private val syncQueries = database.syncMetadataQueries
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private suspend fun ensureDatabaseReady() {
        runCatching {
            kotlinx.coroutines.withTimeout(5000) { dbReady.await() }
        }.onFailure { AppLogger.e("SyncManager", "Database initialization timeout") }
    }

    suspend fun syncAll() = withContext(getIoDispatcher()) {
        ensureDatabaseReady()
        if (_isSyncing.value) return@withContext
        _isSyncing.value = true
        AppLogger.d("SyncManager", "Starting bidirectional synchronization (PUSH + PULL)...")

        try {
            syncStudents()
            pullAll()
            AppLogger.d("SyncManager", "Bidirectional synchronization completed successfully.")
        } catch (e: Exception) {
            AppLogger.e("SyncManager", "Sync failed: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun pullAll() = withContext(getIoDispatcher()) {
        AppLogger.d("SyncManager", "Starting PULL phase for tables: profiles, courses, students...")
        val tables = listOf("profiles", "courses", "students", "attendance", "justifications", "grades")
        
        tables.forEach { table ->
            try {
                AppLogger.d("SyncManager", "[TRACE] Calling pullTable for: $table")
                pullTable(table)
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Pull failed for table $table: ${e.message}")
                runCatching { bridge.recordSyncError(e.message, table) }
            }
        }
    }

    private suspend fun pullTable(tableName: String) {
        val lastPullAt: Long? = bridge.getLastPullAt(tableName)
        val lastPullIso = lastPullAt?.let { if (it > 0) epochMsToIso(it) else "1970-01-01T00:00:00Z" } ?: "1970-01-01T00:00:00Z"

        AppLogger.d("SyncManager", "Pulling $tableName since $lastPullIso")
        val query = supabase.from(tableName).select { filter { gt("updated_at", lastPullIso) } }

        when (tableName) {
            "students" -> {
                val remote = query.decodeList<com.tuapp.libreta.data.remote.dto.StudentSyncDto>()
                remote.forEach { dto ->
                    bridge.insertOrReplaceStudent(
                        dto.id, dto.fullName, dto.studentRut, dto.courseId, dto.parentId,
                        1, 0, SyncStatus.SYNCED.name,
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt),
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt)
                    )
                }
            }
            "profiles" -> {
                val remote = query.decodeList<com.tuapp.libreta.data.remote.dto.ProfileSyncDto>()
                remote.forEach { dto ->
                    bridge.insertOrReplaceProfile(
                        dto.id, dto.fullName, dto.role, 1, 0, SyncStatus.SYNCED.name, 
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt),
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt)
                    )
                }
            }
            "courses" -> {
                val remote = query.decodeList<com.tuapp.libreta.data.remote.dto.CourseSyncDto>()
                remote.forEach { dto ->
                    bridge.insertOrReplaceCourse(
                        dto.id, dto.name, dto.description, dto.subject, dto.grade, dto.section, 
                        dto.teacherId, dto.schoolId, dto.inviteCode, 
                        if (dto.isActive) 1 else 0, 1, 0, SyncStatus.SYNCED.name,
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt),
                        com.tuapp.libreta.data.util.sqlDateToEpochMs(dto.updatedAt)
                    )
                }
            }
        }
        bridge.setLastPullAt(tableName, com.tuapp.libreta.data.util.currentEpochMs())
    }

    private suspend fun syncStudents() {
        val pending = bridge.getUnsyncedStudentEntities()
        if (pending.isEmpty()) return

        val (toUpsert, _) = pending.partition { it.sync_status != SyncStatus.PENDING_DELETE.name }

        if (toUpsert.isNotEmpty()) {
            val dtos = toUpsert.map { entity ->
                mapOf("id" to entity.id, "full_name" to entity.full_name, "course_id" to entity.course_id, "parent_id" to entity.parent_id, "updated_at" to epochMsToIso(entity.updated_at))
            }
            try {
                supabase.from("students").upsert(dtos)
                toUpsert.forEach { entity ->
                    bridge.insertOrReplaceStudent(
                        entity.id, entity.full_name, entity.student_rut, entity.course_id, entity.parent_id,
                        entity.server_version, entity.is_deleted, SyncStatus.SYNCED.name, entity.created_at, entity.updated_at
                    )
                }
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Bulk upsert students failed: ${e.message}")
            }
        }
    }

    suspend fun clearAllLocalData() = withContext(getIoDispatcher()) {
        ensureDatabaseReady()
        runCatching { queries.deleteAllProfiles() }
        runCatching { queries.deleteAllCourses() }
        runCatching { queries.deleteAllStudents() }
        runCatching { queries.deleteAllAttendance() }
        runCatching { queries.deleteAllJustifications() }
        runCatching { queries.deleteAllMessages() }
        runCatching { queries.deleteAllCommunications() }
        runCatching { queries.deleteAllInvitationCodes() }
        runCatching { queries.deleteAllSchools() }
        runCatching { queries.deleteAllGrades() }
        runCatching { bridge.deleteAllSyncMetadata() }
    }
}
