package com.tuapp.libreta.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.db.StudentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import app.cash.sqldelight.async.coroutines.await

actual class LocalDataBridge actual constructor(
    private val driver: SqlDriver,
    private val queries: LibretaAppQueries,
    private val syncQueries: com.tuapp.libreta.db.SyncMetadataQueries
) {
    actual suspend fun insertOrReplaceStudent(
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
    ) {
        val sql = "INSERT OR REPLACE INTO StudentEntity (id, full_name, student_rut, course_id, parent_id, server_version, is_deleted, sync_status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        driver.execute(null, sql, 10) {
            bindString(0, id); bindString(1, fullName); bindString(2, studentRut); bindString(3, courseId)
            bindString(4, parentId); bindLong(5, serverVersion); bindLong(6, isDeleted); bindString(7, syncStatus)
            bindLong(8, createdAt); bindLong(9, updatedAt)
        }.await()
    }

    actual fun getStudentsByParent(parentId: String): Flow<List<StudentEntity>> = flow {
        val sql = "SELECT * FROM StudentEntity WHERE parent_id = ? AND is_deleted = 0 AND sync_status != 'PENDING_DELETE'"
        while(true) {
            val result = mutableListOf<StudentEntity>()
            driver.executeQuery(null, sql, { cursor ->
                QueryResult.AsyncValue {
                    while (cursor.next().await()) {
                        result.add(StudentEntity(
                            cursor.getString(0)!!, cursor.getString(1)!!, cursor.getString(2), cursor.getString(3)!!,
                            cursor.getString(4)!!, cursor.getLong(5)!!, cursor.getLong(6)!!, cursor.getString(7)!!,
                            cursor.getLong(8)!!, cursor.getLong(9)!!
                        ))
                    }
                }
            }, 1, { bindString(0, parentId) }).await()
            emit(result)
            delay(5000)
        }
    }

    actual suspend fun getLastPullAt(tableName: String): Long? {
        val sql = "SELECT last_pull_at FROM SyncMetadataEntity WHERE table_name = ?"
        return driver.executeQuery(null, sql, { cursor ->
            QueryResult.AsyncValue {
                if (cursor.next().await()) cursor.getLong(0) else null
            }
        }, 1, { bindString(0, tableName) }).await()
    }

    actual suspend fun getUnsyncedStudentEntities(): List<StudentEntity> {
        val sql = "SELECT * FROM StudentEntity WHERE sync_status != 'SYNCED'"
        val result = mutableListOf<StudentEntity>()
        driver.executeQuery(null, sql, { cursor ->
            QueryResult.AsyncValue {
                while (cursor.next().await()) {
                    result.add(StudentEntity(
                        cursor.getString(0)!!, cursor.getString(1)!!, cursor.getString(2), cursor.getString(3)!!,
                        cursor.getString(4)!!, cursor.getLong(5)!!, cursor.getLong(6)!!, cursor.getString(7)!!,
                        cursor.getLong(8)!!, cursor.getLong(9)!!
                    ))
                }
            }
        }, 0).await()
        return result
    }
}
