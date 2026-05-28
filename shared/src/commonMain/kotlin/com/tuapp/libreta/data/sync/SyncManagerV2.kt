package com.tuapp.libreta.data.sync

import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.util.getIoDispatcher
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * FASE 3 — SyncManager v2 con PULL incremental + LWW + optimistic locking.
 *
 * Reemplaza el "blind push" del [SyncManager] v1.
 * Detrás de feature flag `BuildKonfig.ENABLE_SYNC_V2`.
 *
 * Ciclo completo por tabla:
 *   1. PULL  — descarga filas con updated_at > last_pull (RPC sync_pull_*)
 *   2. MERGE — para cada fila remota:
 *        - si local no existe → INSERT
 *        - si local.server_version < remote.server_version → REPLACE local (LWW server-wins)
 *        - si remote.deleted_at NOT NULL → marcar local is_deleted=1
 *   3. PUSH  — para cada fila local con sync_status != SYNCED:
 *        - llama RPC sync_push_* con last_known_server_version
 *        - si CONFLICT → marca PENDING_CONFLICT y notifica UI
 *   4. CLEANUP — borra filas locales con is_deleted=1 confirmadas en server.
 *   5. UPDATE last_pull_at en sync_metadata.
 *
 * TODO[FASE-3]:
 *   1. Definir DTOs internos `AttendanceSyncRow`, `StudentSyncRow`, ... que mapeen
 *      el output de las RPCs sync_pull_*.
 *   2. Implementar mergeAttendance(), mergeStudents(), ... por tabla.
 *   3. Manejar SYNC_CONFLICT (ERRCODE P0001) específicamente y exponer a la UI.
 *   4. Programar syncAll() en background:
 *      - Android: WorkManager periódico
 *      - iOS:     BackgroundTasks
 *      - Wasm:    setInterval(60_000)
 *   5. Realtime: suscribirse a postgres_changes en lugar de polling cuando
 *      la sesión está activa (Supabase Realtime).
 */
class SyncManagerV2(
    private val queries: LibretaAppQueries,
    private val supabase: SupabaseClient
) {

    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state = _state.asStateFlow()

    suspend fun syncAll() = withContext(getIoDispatcher()) {
        if (_state.value is SyncState.Running) return@withContext
        _state.value = SyncState.Running
        AppLogger.d("SyncManagerV2", "Starting v2 sync (PULL → MERGE → PUSH → CLEANUP)")

        try {
            syncAttendance()
            // TODO[FASE-3]: syncStudents(), syncJustifications(), syncGrades(),
            //                syncProfiles(), syncCourses(), syncMessages()
            _state.value = SyncState.Success
            AppLogger.d("SyncManagerV2", "Sync v2 completed")
        } catch (e: Exception) {
            AppLogger.e("SyncManagerV2", "Sync v2 failed: ${e.message}", e)
            _state.value = SyncState.Error(e.message ?: "unknown")
        }
    }

    // ── tabla por tabla ────────────────────────────────────────────────────────

    private suspend fun syncAttendance() {
        // 1. PULL
        // val since = getLastPullAt("attendance")
        // val remoteRows = supabase.postgrest
        //     .rpc("sync_pull_attendance", mapOf("p_since" to since))
        //     .decodeList<AttendanceSyncRow>()

        // 2. MERGE
        // for (row in remoteRows) mergeAttendanceRow(row)

        // 3. PUSH
        // val pending = queries.getUnsyncedAttendanceEntities().executeAsList()
        // for (entity in pending) pushAttendanceRow(entity)

        // 4. CLEANUP
        // queries.deleteAttendanceRowsConfirmed()

        // 5. UPDATE metadata
        // setLastPullAt("attendance", currentEpochMs())

        TODO("FASE-3: implementar ciclo PULL/MERGE/PUSH para attendance")
    }

    // ── helpers de conflict resolution ────────────────────────────────────────

    /**
     * Estrategia por defecto: Last-Write-Wins por server_version.
     * Si el cliente y el servidor cambiaron, gana el servidor y el cliente
     * pierde sus cambios. En FASE 3.5 podemos:
     *   - exponer el conflicto a UI con merge manual
     *   - aplicar Operational Transform para texto
     */
    private fun resolveConflict(local: Any, remote: Any): Any = remote
}

sealed interface SyncState {
    data object Idle : SyncState
    data object Running : SyncState
    data object Success : SyncState
    data class Error(val message: String) : SyncState
    data class Conflict(val tableName: String, val rowId: String) : SyncState
}
