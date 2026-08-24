package com.tuapp.libreta.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.db.SyncMetadataQueries
import com.tuapp.libreta.db.StudentEntity
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.flow.Flow

actual class LocalDataBridge actual constructor(
    private val driver: SqlDriver,
    private val queries: LibretaAppQueries,
    private val syncQueries: SyncMetadataQueries
) {
    actual suspend fun insertOrReplaceStudent(
        id: String, fullName: String, studentRut: String?, courseId: String, parentId: String,
        serverVersion: Long, isDeleted: Long, syncStatus: String, createdAt: Long, updatedAt: Long
    ) {
        queries.insertOrReplaceStudent(id, fullName, studentRut, courseId, parentId, serverVersion, isDeleted, syncStatus, createdAt, updatedAt)
    }

    actual suspend fun insertOrReplaceProfile(
        id: String, fullName: String, role: String?, 
        serverVersion: Long, isDeleted: Long, syncStatus: String, createdAt: Long, updatedAt: Long
    ) {
        queries.insertOrReplaceProfile(id, fullName, role, serverVersion, isDeleted, syncStatus, createdAt, updatedAt)
    }

    actual suspend fun insertOrReplaceCourse(
        id: String, name: String, description: String?, subject: String?, grade: String?,
        section: String?, teacherId: String, schoolId: String?, inviteCode: String?,
        isActive: Long, serverVersion: Long, isDeleted: Long, syncStatus: String,
        createdAt: Long, updatedAt: Long
    ) {
        queries.insertOrReplaceCourse(id, name, description, subject, grade, section, teacherId, schoolId, inviteCode, isActive, serverVersion, isDeleted, syncStatus, createdAt, updatedAt)
    }

    actual fun getStudentsByParent(parentId: String): Flow<List<StudentEntity>> =
        queries.getStudentsByParent(parentId).asFlow().mapToList(getIoDispatcher())

    actual suspend fun getLastPullAt(tableName: String): Long? =
        syncQueries.getLastPullAt(tableName).executeAsOneOrNull()

    actual suspend fun setLastPullAt(tableName: String, timestamp: Long) {
        syncQueries.setLastPullAt(tableName, timestamp, tableName)
    }

    actual suspend fun recordSyncError(errorMessage: String?, tableName: String) {
        syncQueries.recordSyncError(errorMessage, tableName)
    }

    actual suspend fun deleteAllSyncMetadata() {
        syncQueries.deleteAllSyncMetadata()
    }

    actual suspend fun getUnsyncedStudentEntities(): List<StudentEntity> =
        queries.getUnsyncedStudentEntities().executeAsList()
}
