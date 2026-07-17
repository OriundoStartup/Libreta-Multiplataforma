package com.tuapp.libreta.data.sync

import app.cash.sqldelight.async.coroutines.awaitAsList
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
            syncEntity("attendance", { queries.getUnsyncedAttendanceEntities().awaitAsList() }) { entity ->
                mapOf(
                    "id" to entity.id,
                    "student_id" to entity.student_id,
                    "date" to entity.date,
                    "status" to entity.status,
                    "updated_at" to epochMsToIso(entity.updated_at)
                )
            }
            
            syncEntity("students", { queries.getUnsyncedStudentEntities().awaitAsList() }) { entity ->
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

            syncEntity("profiles", { queries.getUnsyncedProfileEntities().awaitAsList() }) { entity ->
                mapOf(
                    "id" to entity.id,
                    "full_name" to entity.full_name,
                    "role" to entity.role,
                    "updated_at" to epochMsToIso(entity.updated_at)
                )
            }

            syncEntity("grades", { queries.getUnsyncedGradeEntities().awaitAsList() }) { entity ->
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

            AppLogger.d("SyncManager", "Synchronization completed successfully.")
        } catch (e: Exception) {
            AppLogger.e("SyncManager", "Sync failed: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Motor genérico de sincronización por entidad
     */
    private suspend fun <T : Any> syncEntity(
        tableName: String,
        fetchPending: suspend () -> List<T>,
        mapToDto: (T) -> Map<String, Any?>
    ) {
        val pending = fetchPending()
        if (pending.isEmpty()) return

        pending.forEach { entity ->
            try {
                // Usamos reflexión simple o asunciones sobre el esquema común (id, sync_status)
                // Para evitar complejidad excesiva con reflexión en KMP, usamos un helper por entidad
                // pero centralizamos el flujo try-catch y la lógica de estado.
                processEntitySync(tableName, entity, mapToDto)
            } catch (e: Exception) {
                AppLogger.e("SyncManager", "Failed to sync $tableName: ${e.message}")
            }
        }
    }

    private suspend fun <T : Any> processEntitySync(
        tableName: String,
        entity: T,
        mapToDto: (T) -> Map<String, Any?>
    ) {
        // Obtenemos campos comunes vía reflexión manual o casteos seguros si fuera posible.
        // Dado que SQLDelight genera clases distintas, la forma más limpia y segura en KMP 
        // sin depender de librerías de reflexión pesadas es pasar la lógica de persistencia.
        
        when (tableName) {
            "attendance" -> {
                val e = entity as com.tuapp.libreta.db.AttendanceEntity
                handleSync(tableName, e.id, e.sync_status, mapToDto(entity)) {
                    queries.insertOrReplaceAttendance(
                        e.id, e.student_id, e.date, e.status, e.server_version, 
                        e.is_deleted, SyncStatus.SYNCED.name, e.created_at, e.updated_at
                    )
                }
            }
            "students" -> {
                val e = entity as com.tuapp.libreta.db.StudentEntity
                handleSync(tableName, e.id, e.sync_status, mapToDto(entity)) {
                    queries.insertOrReplaceStudent(
                        e.id, e.full_name, e.student_rut, e.course_id, e.parent_id,
                        e.server_version, e.is_deleted, SyncStatus.SYNCED.name, e.created_at, e.updated_at
                    )
                }
            }
            "profiles" -> {
                val e = entity as com.tuapp.libreta.db.ProfileEntity
                handleSync(tableName, e.id, e.sync_status, mapToDto(entity)) {
                    queries.insertOrReplaceProfile(
                        e.id, e.full_name, e.role, e.server_version, e.is_deleted,
                        SyncStatus.SYNCED.name, e.created_at, e.updated_at
                    )
                }
            }
            "grades" -> {
                val e = entity as com.tuapp.libreta.db.GradeEntity
                handleSync(tableName, e.id, e.sync_status, mapToDto(entity)) {
                    queries.insertOrReplaceGrade(
                        e.id, e.student_id, e.course_id, e.title, e.score, e.weight,
                        e.term, e.subject, e.server_version, e.is_deleted,
                        SyncStatus.SYNCED.name, e.created_at, e.updated_at
                    )
                }
            }
        }
    }

    private suspend fun handleSync(
        tableName: String,
        id: String,
        syncStatus: String,
        dto: Map<String, Any?>,
        onSynced: suspend () -> Unit
    ) {
        when (syncStatus) {
            SyncStatus.PENDING_INSERT.name, SyncStatus.PENDING_UPDATE.name -> {
                supabase.from(tableName).upsert(dto)
                onSynced()
            }
            SyncStatus.PENDING_DELETE.name -> {
                supabase.from(tableName).delete { filter { eq("id", id) } }
                when (tableName) {
                    "attendance" -> queries.deleteAttendanceEntity(id)
                    "students"   -> queries.deleteStudentEntity(id)
                    "profiles"    -> queries.deleteProfileEntity(id)
                    "grades"      -> queries.deleteGradeEntity(id)
                }
            }
        }
    }
}
