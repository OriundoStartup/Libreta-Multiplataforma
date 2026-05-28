package com.tuapp.libreta.data.sync

import kotlin.test.Test
import kotlin.test.Ignore

/**
 * FASE 5 — Tests de SyncManager v2.
 *
 * Cubrir:
 *   1. PULL trae solo filas > last_pull_at.
 *   2. MERGE con server_version mayor → reemplaza local.
 *   3. MERGE con server_version menor → ignora.
 *   4. PUSH con last_known_server_version válido → SUCCESS.
 *   5. PUSH con last_known_server_version stale → CONFLICT P0001.
 *   6. CLEANUP elimina filas locales con is_deleted=1 confirmadas remotas.
 *   7. Soft-delete remoto se propaga a local.
 *   8. Re-entrada concurrente (dos llamadas a syncAll) → segunda no-op.
 *
 * TODO[FASE-5]:
 *   - Crear `FakeSupabaseClient` que devuelva listas controladas por RPC mock.
 *   - Usar SQLDelight in-memory driver para queries locales.
 */
class SyncManagerV2Test {

    @Test
    @Ignore // FASE-5: implementar
    fun `PULL fetches only rows after last_pull_at`() {
    }

    @Test
    @Ignore
    fun `MERGE applies server-wins LWW by server_version`() {
    }

    @Test
    @Ignore
    fun `PUSH propagates SYNC_CONFLICT to SyncState`() {
    }

    @Test
    @Ignore
    fun `syncAll is reentrant-safe`() {
    }
}
