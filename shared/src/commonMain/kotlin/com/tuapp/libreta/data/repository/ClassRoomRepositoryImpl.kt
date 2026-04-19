package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.now
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.random
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.ClassRoom
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.ClassRoomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class ClassRoomRepositoryImpl(private val queries: LibretaAppQueries) : ClassRoomRepository {

    override fun getAll(): Flow<List<ClassRoom>> =
        queries.getAllClasses().asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }

    override suspend fun save(classRoom: ClassRoom) {
        val now = now()
        queries.insertOrReplaceClass(
            id = classRoom.id.value,
            class_code = classRoom.classCode,
            name = classRoom.name,
            teacher_id = classRoom.teacherId.value,
            sync_status = SyncStatus.PENDING_INSERT.name,
            created_at = now,
            updated_at = now
        )
    }

    override suspend fun delete(id: UuidString) {
        queries.markClassAsPendingDelete(updated_at = now(), id = id.value)
    }
}
