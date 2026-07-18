package com.tuapp.libreta.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.toDomainList
import com.tuapp.libreta.data.sync.SyncManager
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.db.StudentEntity
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.StudentRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentRepositoryImpl(
    private val queries: LibretaAppQueries,
    private val syncManager: SyncManager
) : StudentRepository {

    private val scope = CoroutineScope(SupervisorJob() + getIoDispatcher())

    override fun getStudentsByClass(classId: UuidString): Flow<List<Student>> =
        queries.getStudentsByCourse(classId.value).toDomainList { it.toDomain() }

    override fun getStudentsByParent(parentId: UuidString): Flow<List<Student>> =
        queries.getStudentsByParent(parentId.value).toDomainList { it.toDomain() }

    override suspend fun saveStudent(student: Student) {
        withContext(getIoDispatcher()) {
            queries.insertOrReplaceStudent(
                id = student.id.value,
                full_name = student.fullName,
                student_rut = null,
                course_id = student.courseId.value,
                parent_id = student.parentId.value,
                server_version = 1,
                is_deleted = 0,
                sync_status = SyncStatus.SYNCED.name,
                created_at = currentEpochMs(),
                updated_at = currentEpochMs()
            )
            scope.launch { syncManager.syncAll() }
        }
    }

    override suspend fun updateStudentEnrollment(id: UuidString, name: String, rut: String?): Result<Unit> = withContext(getIoDispatcher()) {
        runCatching {
            val current: StudentEntity? = queries.getStudentById(id.value).awaitAsOneOrNull()
            if (current != null) {
                queries.insertOrReplaceStudent(
                    id = id.value,
                    full_name = name,
                    student_rut = rut,
                    course_id = current.course_id,
                    parent_id = current.parent_id,
                    server_version = current.server_version,
                    is_deleted = current.is_deleted,
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
