package com.tuapp.libreta.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.db.StudentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay

actual class LocalDataBridge actual constructor(
    private val driver: SqlDriver,
    private val queries: LibretaAppQueries
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
            bindString(0, id)
            bindString(1, fullName)
            bindString(2, studentRut)
            bindString(3, courseId)
            bindString(4, parentId)
            bindLong(5, serverVersion)
            bindLong(6, isDeleted)
            bindString(7, syncStatus)
            bindLong(8, createdAt)
            bindLong(9, updatedAt)
        }.await()
    }

    /**
     * Versión Simplificada para Wasm: Carga bajo demanda sin listener 
     * para evitar errores de firma del driver.
     */
    actual fun getStudentsByParent(parentId: String): Flow<List<StudentEntity>> = flow {
        val sql = "SELECT * FROM StudentEntity WHERE parent_id = ? AND is_deleted = 0 AND sync_status != 'PENDING_DELETE'"
        
        while(true) {
            val result = mutableListOf<StudentEntity>()
            val queryResult = driver.executeQuery(
                identifier = null,
                sql = sql,
                mapper = { cursor -> QueryResult.Value(cursor) },
                parameters = 1,
                binders = { bindString(0, parentId) }
            )
            
            val cursor = queryResult.await()
            while (cursor.next().await()) {
                result.add(
                    StudentEntity(
                        id = cursor.getString(0)!!,
                        full_name = cursor.getString(1)!!,
                        student_rut = cursor.getString(2),
                        course_id = cursor.getString(3)!!,
                        parent_id = cursor.getString(4)!!,
                        server_version = cursor.getLong(5)!!,
                        is_deleted = cursor.getLong(6)!!,
                        sync_status = cursor.getString(7)!!,
                        created_at = cursor.getLong(8)!!,
                        updated_at = cursor.getLong(9)!!
                    )
                )
            }
            emit(result)
            delay(5000) // Polling temporal mientras estabilizamos el Listener
        }
    }
}
