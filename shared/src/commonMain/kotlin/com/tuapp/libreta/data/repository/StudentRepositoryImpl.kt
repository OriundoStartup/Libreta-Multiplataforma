package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.now
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.StudentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class StudentRepositoryImpl(private val queries: LibretaAppQueries) : StudentRepository {

    override fun getStudentsByClass(classId: String): Flow<List<Student>> =
        queries.getStudentsByClass(classId).asFlow().mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override suspend fun saveStudent(student: Student) = try {
        val now = now()
        queries.insertOrReplaceStudent(student.id, student.rut, student.firstName, student.lastName,
            student.parentId, student.classId, SyncStatus.PENDING_INSERT.name, now, now)
    } catch (e: Exception) { throw RuntimeException("Error al guardar alumno: ${e.message}", e) }

    override suspend fun deleteStudent(id: String) = try {
        queries.markStudentAsPendingDelete(updated_at = now(), id = id)
    } catch (e: Exception) { throw RuntimeException("Error al eliminar alumno: ${e.message}", e) }
}
