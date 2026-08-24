package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.tuapp.libreta.data.db.LocalDataBridge
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.toDomainList
import com.tuapp.libreta.data.sync.SyncManager
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.db.StudentEntity
import com.tuapp.libreta.di.dbReady
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.StudentRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentRepositoryImpl(
    private val queries: LibretaAppQueries,
    private val syncManager: SyncManager,
    private val bridge: LocalDataBridge
) : StudentRepository {

    private val scope = CoroutineScope(SupervisorJob() + getIoDispatcher())

    private suspend fun waitForDb() {
        runCatching {
            kotlinx.coroutines.withTimeout(5000) { dbReady.await() }
        }.onFailure { 
            AppLogger.e("StudentRepo", "Database ready timeout: ${it.message}")
        }
    }

    override fun getStudentsByClass(classId: UuidString): Flow<List<Student>> = flow {
        waitForDb()
        queries.getStudentsByCourse(classId.value).toDomainList { it.toDomain() }.collect { emit(it) }
    }

    override fun getStudentsByParent(parentId: UuidString): Flow<List<Student>> = flow {
        waitForDb()
        bridge.getStudentsByParent(parentId.value)
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }
            .collect { emit(it) }
    }

    override suspend fun saveStudent(student: Student) {
        waitForDb()
        withContext(getIoDispatcher()) {
            bridge.insertOrReplaceStudent(
                id = student.id.value,
                fullName = student.fullName,
                studentRut = null,
                courseId = student.courseId.value,
                parentId = student.parentId.value,
                serverVersion = 1,
                isDeleted = 0,
                syncStatus = SyncStatus.SYNCED.name,
                createdAt = currentEpochMs(),
                updatedAt = currentEpochMs()
            )
            scope.launch { syncManager.syncAll() }
        }
    }

    override suspend fun updateStudentEnrollment(id: UuidString, name: String, rut: String?): Result<Unit> = withContext(getIoDispatcher()) {
        waitForDb()
        runCatching {
            val current: StudentEntity? = queries.getStudentById(id.value)
                .asFlow()
                .mapToOneOrNull(getIoDispatcher())
                .first()

            if (current != null) {
                bridge.insertOrReplaceStudent(
                    id = id.value,
                    fullName = name,
                    studentRut = rut,
                    courseId = current.course_id,
                    parentId = current.parent_id,
                    serverVersion = current.server_version,
                    isDeleted = current.is_deleted,
                    syncStatus = SyncStatus.PENDING_UPDATE.name,
                    createdAt = current.created_at,
                    updatedAt = currentEpochMs()
                )
                scope.launch { syncManager.syncAll() }
                Unit
            } else {
                throw Exception("Student not found")
            }
        }
    }

    override suspend fun deleteStudent(id: UuidString) {
        waitForDb()
        withContext(getIoDispatcher()) {
            queries.markStudentAsPendingDelete(updated_at = currentEpochMs(), id = id.value)
            scope.launch { syncManager.syncAll() }
        }
    }
}
