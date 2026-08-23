package com.tuapp.libreta.data.db

import app.cash.sqldelight.db.SqlDriver
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.db.SyncMetadataQueries
import com.tuapp.libreta.db.StudentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Puente para operaciones de base de datos que requieren comportamiento diferenciado 
 * entre plataformas.
 */
expect class LocalDataBridge(
    driver: SqlDriver,
    queries: LibretaAppQueries,
    syncQueries: SyncMetadataQueries
) {
    suspend fun insertOrReplaceStudent(
        id: String,
        fullName: String,
        studentRut: String?,
        courseId: String,
        parentId: String,
        serverVersion: Long,
        isDeleted: Long,
        syncStatus: String,
        createdAt: Long,
        updatedAt: Long
    )

    fun getStudentsByParent(parentId: String): Flow<List<StudentEntity>>

    // Funciones para el ciclo de vida de sincronización
    suspend fun getLastPullAt(tableName: String): Long?
    suspend fun setLastPullAt(tableName: String, timestamp: Long)
    suspend fun recordSyncError(errorMessage: String?, tableName: String)
    suspend fun deleteAllSyncMetadata()
    suspend fun getUnsyncedStudentEntities(): List<StudentEntity>
}
