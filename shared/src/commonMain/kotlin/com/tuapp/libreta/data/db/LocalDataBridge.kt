package com.tuapp.libreta.data.db

import app.cash.sqldelight.db.SqlDriver
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.db.SyncMetadataQueries
import com.tuapp.libreta.db.StudentEntity
import kotlinx.coroutines.flow.Flow

expect class LocalDataBridge(
    driver: SqlDriver,
    queries: LibretaAppQueries,
    syncQueries: SyncMetadataQueries
) {
    suspend fun insertOrReplaceStudent(
        id: String, fullName: String, studentRut: String?, courseId: String, parentId: String,
        serverVersion: Long, isDeleted: Long, syncStatus: String, createdAt: Long, updatedAt: Long
    )

    suspend fun insertOrReplaceProfile(
        id: String, fullName: String, role: String?, 
        serverVersion: Long, isDeleted: Long, syncStatus: String, createdAt: Long, updatedAt: Long
    )

    suspend fun insertOrReplaceCourse(
        id: String, name: String, description: String?, subject: String?, grade: String?,
        section: String?, teacherId: String, schoolId: String?, inviteCode: String?,
        isActive: Long, serverVersion: Long, isDeleted: Long, syncStatus: String,
        createdAt: Long, updatedAt: Long
    )

    fun getStudentsByParent(parentId: String): Flow<List<StudentEntity>>
    suspend fun getLastPullAt(tableName: String): Long?
    suspend fun setLastPullAt(tableName: String, timestamp: Long)
    suspend fun recordSyncError(errorMessage: String?, tableName: String)
    suspend fun deleteAllSyncMetadata()
    suspend fun getUnsyncedStudentEntities(): List<StudentEntity>
}
