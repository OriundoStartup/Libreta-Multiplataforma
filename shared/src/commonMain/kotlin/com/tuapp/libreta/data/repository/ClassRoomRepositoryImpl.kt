package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.now
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.ClassRoom
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.ClassRoomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class ClassRoomRepositoryImpl(private val queries: LibretaAppQueries) : ClassRoomRepository {

    override fun getAll(): Flow<List<ClassRoom>> =
        queries.getAllClasses().asFlow().mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override suspend fun save(classRoom: ClassRoom) = try {
        val now = now()
        queries.insertOrReplaceClass(classRoom.id, classRoom.classCode, classRoom.name,
            classRoom.teacherId, SyncStatus.PENDING_INSERT.name, now, now)
    } catch (e: Exception) { throw RuntimeException("Error al guardar clase: ${e.message}", e) }

    override suspend fun delete(id: String) = try {
        queries.markClassAsPendingDelete(updated_at = now(), id = id)
    } catch (e: Exception) { throw RuntimeException("Error al eliminar clase: ${e.message}", e) }
}
