package com.tuapp.libreta.data.db

import app.cash.sqldelight.db.SqlDriver
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.db.StudentEntity
import kotlinx.coroutines.flow.Flow

expect class LocalDataBridge(
    driver: SqlDriver,
    queries: LibretaAppQueries
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
}
