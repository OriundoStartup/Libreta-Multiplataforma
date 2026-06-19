package com.tuapp.libreta.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.sync.SyncManager
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.StudentRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentRepositoryImpl(
    private val queries: LibretaAppQueries,
    private val syncManager: SyncManager
) : StudentRepository {

    private val scope = CoroutineScope(SupervisorJob() + getIoDispatcher())

    override fun getStudentsByClass(classId: UuidString): Flow<List<Student>> =
        queries.getStudentsByCourse(classId.value).asFlow().mapToList(getIoDispatcher())
            .map { list -> list.map { it.toDomain() } }
            .catch { 
                println("ERROR SQLDelight getStudentsByClass: ${it.message}")
                emit(emptyList()) 
            }

    override fun getStudentsByParent(parentId: UuidString): Flow<List<Student>> =
        queries.getStudentsByParent(parentId.value).asFlow().mapToList(getIoDispatcher())
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override suspend fun saveStudent(student: Student) {
        withContext(getIoDispatcher()) {
            queries.insertOrReplaceStudent(
                id = student.id.value,
                full_name = student.fullName,
                student_rut = null,
                course_id = student.courseId.value,
                parent_id = student.parentId.value,
                sync_status = SyncStatus.PENDING_INSERT.name,
                created_at = currentEpochMs(),
                updated_at = currentEpochMs()
            )
            scope.launch { syncManager.syncAll() }
        }
    }

    override suspend fun updateStudentEnrollment(id: UuidString, name: String, rut: String?): Result<Unit> = withContext(getIoDispatcher()) {
        runCatching {
            val current = queries.getStudentById(id.value).awaitAsOneOrNull()
            if (current != null) {
                queries.insertOrReplaceStudent(
                    id = id.value,
                    full_name = name,
                    student_rut = rut,
                    course_id = current.course_id,
                    parent_id = current.parent_id,
                    sync_status = SyncStatus.PENDING_UPDATE.name,
                    created_at = current.created_at,
                    updated_at = currentEpochMs()
                )
                scope.launch { syncManager.syncAll() }
                Unit
            } else {
                throw Exception("Student not found")
            }
        }
    }

    override suspend fun deleteStudent(id: UuidString) {
        withContext(getIoDispatcher()) {
            queries.markStudentAsPendingDelete(updated_at = currentEpochMs(), id = id.value)
            scope.launch { syncManager.syncAll() }
        }
    }
}
