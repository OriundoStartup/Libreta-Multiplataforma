package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Course
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.ClassRoomRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class ClassRoomRepositoryImpl(private val queries: LibretaAppQueries) : ClassRoomRepository {

    override fun getAll(): Flow<List<Course>> =
        queries.getAllCourses().asFlow().mapToList(getIoDispatcher())
            .map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }

    override fun getByTeacher(teacherId: UuidString): Flow<List<Course>> =
        queries.getCoursesByTeacher(teacherId.value).asFlow().mapToList(getIoDispatcher())
            .map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }

    override suspend fun save(classRoom: Course) {
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
            sync_status = SyncStatus.PENDING_INSERT.name,
            created_at = now,
            updated_at = now
        )
    }

    override suspend fun delete(id: UuidString) {
        queries.markCourseAsPendingDelete(updated_at = currentEpochMs(), id = id.value)
    }
}