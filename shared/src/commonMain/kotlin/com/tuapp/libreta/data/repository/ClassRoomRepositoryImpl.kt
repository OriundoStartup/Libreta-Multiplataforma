package com.tuapp.libreta.data.repository

import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.toDomainList
import com.tuapp.libreta.data.sync.SyncManager
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.di.dbReady
import com.tuapp.libreta.domain.model.Course
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.ClassRoomRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClassRoomRepositoryImpl(
    private val queries: LibretaAppQueries,
    private val syncManager: SyncManager
) : ClassRoomRepository {

    private val scope = CoroutineScope(SupervisorJob() + getIoDispatcher())

    private suspend fun waitForDb() {
        runCatching {
            kotlinx.coroutines.withTimeout(5000) { dbReady.await() }
        }.onFailure { 
            AppLogger.e("ClassRoomRepo", "Database ready timeout: ${it.message}")
        }
    }

    override fun getAll(): Flow<List<Course>> = kotlinx.coroutines.flow.flow {
        waitForDb()
        queries.getAllCourses().toDomainList { it.toDomain() }.collect { emit(it) }
    }

    override fun getByTeacher(teacherId: UuidString): Flow<List<Course>> = kotlinx.coroutines.flow.flow {
        waitForDb()
        queries.getCoursesByTeacher(teacherId.value).toDomainList { it.toDomain() }.collect { emit(it) }
    }

    override suspend fun save(classRoom: Course) {
        waitForDb()
        withContext(getIoDispatcher()) {
            val now = currentEpochMs()
            queries.insertOrReplaceCourse(
                id = classRoom.id,
                name = classRoom.name,
                description = classRoom.description,
                subject = classRoom.subject,
                grade = classRoom.grade,
                section = null,
                teacher_id = classRoom.teacherId,
                school_id = classRoom.schoolName,
                invite_code = classRoom.inviteCode,
                is_active = if (classRoom.isActive) 1 else 0,
                server_version = 1,
                is_deleted = 0,
                sync_status = SyncStatus.PENDING_INSERT.name,
                created_at = now,
                updated_at = now
            )
            scope.launch { syncManager.syncAll() }
        }
    }

    override suspend fun delete(id: UuidString) {
        waitForDb()
        withContext(getIoDispatcher()) {
            queries.markCourseAsPendingDelete(updated_at = currentEpochMs(), id = id.value)
            scope.launch { syncManager.syncAll() }
        }
    }
}
