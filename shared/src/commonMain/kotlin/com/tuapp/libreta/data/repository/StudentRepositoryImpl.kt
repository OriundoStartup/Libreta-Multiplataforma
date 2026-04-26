package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.repository.StudentRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class StudentRepositoryImpl(private val queries: LibretaAppQueries) : StudentRepository {

    override fun getStudentsByClass(classId: UuidString): Flow<List<Student>> =
        queries.getStudentsByCourse(classId.value).asFlow().mapToList(getIoDispatcher())
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override fun getStudentsByParent(parentId: UuidString): Flow<List<Student>> =
        queries.getStudentsByParent(parentId.value).asFlow().mapToList(getIoDispatcher())
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override suspend fun saveStudent(student: Student) {
        queries.insertOrReplaceStudent(
            id = student.id.value,
            full_name = student.fullName,
            student_rut = null,
            course_id = student.courseId.value,
            parent_id = student.parentId.value,
            sync_status = "PENDING_INSERT",
            created_at = currentEpochMs(),
            updated_at = currentEpochMs()
        )
    }

    override suspend fun deleteStudent(id: UuidString) {
        queries.markStudentAsPendingDelete(updated_at = currentEpochMs(), id = id.value)
    }
}