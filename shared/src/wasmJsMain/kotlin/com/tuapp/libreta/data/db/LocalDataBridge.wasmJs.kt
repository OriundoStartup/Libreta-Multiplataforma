package com.tuapp.libreta.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.db.SyncMetadataQueries
import com.tuapp.libreta.db.StudentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import app.cash.sqldelight.async.coroutines.await
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

actual class LocalDataBridge actual constructor(
    private val driver: SqlDriver,
    private val queries: LibretaAppQueries,
    private val syncQueries: SyncMetadataQueries
) {
    actual suspend fun insertOrReplaceStudent(
        id: String, fullName: String, studentRut: String?, courseId: String, parentId: String,
        serverVersion: Long, isDeleted: Long, syncStatus: String, createdAt: Long, updatedAt: Long
    ) {
        val sql = "INSERT OR REPLACE INTO StudentEntity VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        driver.execute(null, sql, 10) {
            bindString(0, id); bindString(1, fullName); bindString(2, studentRut); bindString(3, courseId)
            bindString(4, parentId); bindLong(5, serverVersion); bindLong(6, isDeleted); bindString(7, syncStatus)
            bindLong(8, createdAt); bindLong(9, updatedAt)
        }.await()
    }

    actual suspend fun insertOrReplaceProfile(
        id: String, fullName: String, role: String?, 
        serverVersion: Long, isDeleted: Long, syncStatus: String, createdAt: Long, updatedAt: Long
    ) {
        val sql = "INSERT OR REPLACE INTO ProfileEntity VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        driver.execute(null, sql, 8) {
            bindString(0, id); bindString(1, fullName); bindString(2, role); bindLong(3, serverVersion)
            bindLong(4, isDeleted); bindString(5, syncStatus); bindLong(6, createdAt); bindLong(7, updatedAt)
        }.await()
    }

    actual suspend fun insertOrReplaceCourse(
        id: String, name: String, description: String?, subject: String?, grade: String?,
        section: String?, teacherId: String, schoolId: String?, inviteCode: String?,
        isActive: Long, serverVersion: Long, isDeleted: Long, syncStatus: String,
        createdAt: Long, updatedAt: Long
    ) {
        val sql = "INSERT OR REPLACE INTO CourseEntity VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        driver.execute(null, sql, 15) {
            bindString(0, id); bindString(1, name); bindString(2, description); bindString(3, subject)
            bindString(4, grade); bindString(5, section); bindString(6, teacherId); bindString(7, schoolId)
            bindString(8, inviteCode); bindLong(9, isActive); bindLong(10, serverVersion); bindLong(11, isDeleted)
            bindString(12, syncStatus); bindLong(13, createdAt); bindLong(14, updatedAt)
        }.await()
    }

    actual fun getStudentsByParent(parentId: String): Flow<List<StudentEntity>> = flow {
        val sql = "SELECT * FROM StudentEntity WHERE parent_id = ? AND is_deleted = 0 AND sync_status != 'PENDING_DELETE'"
        while(true) {
            val result = mutableListOf<StudentEntity>()
            driver.executeQuery(null, sql, { cursor -> QueryResult.AsyncValue {
                while (cursor.next().await()) {
                    result.add(StudentEntity(cursor.getString(0)!!, cursor.getString(1)!!, cursor.getString(2), cursor.getString(3)!!, cursor.getString(4)!!, cursor.getLong(5)!!, cursor.getLong(6)!!, cursor.getString(7)!!, cursor.getLong(8)!!, cursor.getLong(9)!!))
                }
            } }, 1, { bindString(0, parentId) }).await()
            emit(result); delay(5000)
        }
    }

    actual suspend fun getLastPullAt(tableName: String): Long? {
        val sql = "SELECT last_pull_at FROM SyncMetadata WHERE table_name = ?"
        return driver.executeQuery(null, sql, { cursor -> QueryResult.AsyncValue {
            if (cursor.next().await()) cursor.getLong(0) else null
        } }, 1, { bindString(0, tableName) }).await()
    }

    actual suspend fun setLastPullAt(tableName: String, timestamp: Long) {
        val sql = "INSERT OR REPLACE INTO SyncMetadata (table_name, last_pull_at, last_push_at) VALUES (?, ?, COALESCE((SELECT last_push_at FROM SyncMetadata WHERE table_name = ?), 0))"
        driver.execute(null, sql, 3) { bindString(0, tableName); bindLong(1, timestamp); bindString(2, tableName) }.await()
    }

    actual suspend fun recordSyncError(errorMessage: String?, tableName: String) {
        val sql = "UPDATE SyncMetadata SET last_error = ? WHERE table_name = ?"
        driver.execute(null, sql, 2) { bindString(0, errorMessage); bindString(1, tableName) }.await()
    }

    actual suspend fun deleteAllSyncMetadata() {
        driver.execute(null, "DELETE FROM SyncMetadata", 0).await()
    }

    actual suspend fun getUnsyncedStudentEntities(): List<com.tuapp.libreta.db.StudentEntity> {
        val sql = "SELECT * FROM StudentEntity WHERE sync_status != 'SYNCED'"
        val result = mutableListOf<com.tuapp.libreta.db.StudentEntity>()
        driver.executeQuery(null, sql, { cursor -> QueryResult.AsyncValue {
            while (cursor.next().await()) {
                result.add(com.tuapp.libreta.db.StudentEntity(cursor.getString(0)!!, cursor.getString(1)!!, cursor.getString(2), cursor.getString(3)!!, cursor.getString(4)!!, cursor.getLong(5)!!, cursor.getLong(6)!!, cursor.getString(7)!!, cursor.getLong(8)!!, cursor.getLong(9)!!))
            }
        } }, 0).await()
        return result
    }

    actual suspend fun getUnsyncedAttendanceEntities(): List<com.tuapp.libreta.db.AttendanceEntity> {
        val sql = "SELECT * FROM AttendanceEntity WHERE sync_status != 'SYNCED'"
        val result = mutableListOf<com.tuapp.libreta.db.AttendanceEntity>()
        driver.executeQuery(null, sql, { cursor -> QueryResult.AsyncValue {
            while (cursor.next().await()) {
                result.add(com.tuapp.libreta.db.AttendanceEntity(
                    cursor.getString(0)!!, cursor.getString(1)!!, cursor.getString(2)!!, 
                    cursor.getString(3)!!, cursor.getLong(4)!!, cursor.getLong(5)!!, 
                    cursor.getString(6)!!, cursor.getLong(7)!!, cursor.getLong(8)!!
                ))
            }
        } }, 0).await()
        return result
    }

    actual suspend fun countStudents(): Long {
        val sql = "SELECT COUNT(*) FROM StudentEntity"
        return driver.executeQuery(null, sql, { cursor -> QueryResult.AsyncValue {
            if (cursor.next().await()) cursor.getLong(0) ?: 0L else 0L
        } }, 0).await() ?: 0L
    }
}
