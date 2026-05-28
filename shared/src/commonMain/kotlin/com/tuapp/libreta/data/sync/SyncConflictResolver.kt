package com.tuapp.libreta.data.sync

import com.tuapp.libreta.domain.model.SyncStatus

/**
 * Lógica pura de resolución de conflictos para la sincronización.
 */
object SyncConflictResolver {

    /**
     * Determina qué versión de un registro debe prevalecer.
     * Implementa Last-Write-Wins (LWW) basado en server_version.
     *
     * @param localVersion Versión del registro en la base de datos local.
     * @param remoteVersion Versión del registro recibida del servidor.
     * @return true si el registro remoto debe sobreescribir al local.
     */
    fun shouldServerWin(localVersion: Long, remoteVersion: Long): Boolean {
        return remoteVersion > localVersion
    }

    /**
     * Resuelve el estado de sincronización tras un error de conflicto (409).
     */
    fun resolveStatusOnConflict(): SyncStatus {
        return SyncStatus.PENDING_CONFLICT
    }
}
