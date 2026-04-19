package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.now
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.StudentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class StudentRepositoryImpl(private val queries: LibretaAppQueries) : StudentRepository {

    override fun getStudentsByClass(classId: UuidString): Flow<List<Student>> =
        queries.getStudentsByClass(classId.value).asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override fun getStudentsByParent(parentId: UuidString): Flow<List<Student>> =
        queries.getStudentsByParent(parentId.value).asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override suspend fun saveStudent(student: Student) {
        val now = now()
        queries.insertOrReplaceStudent(
            id = student.id.value,
            full_name = student.fullName,
            course_id = student.courseId.value,
            parent_id = student.parentId.value,
            sync_status = SyncStatus.PENDING_INSERT.name,
            created_at = now,
            updated_at = now
        )
    }

    override suspend fun deleteStudent(id: UuidString) {
        queries.markStudentAsPendingDelete(updated_at = now(), id = id.value)
    }
}
