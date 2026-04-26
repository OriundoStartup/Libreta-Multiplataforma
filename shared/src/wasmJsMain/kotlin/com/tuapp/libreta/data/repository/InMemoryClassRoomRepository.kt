package com.tuapp.libreta.data.repository

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Course
import com.tuapp.libreta.domain.repository.ClassRoomRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class InMemoryClassRoomRepository : ClassRoomRepository {
    private val store = MutableStateFlow<List<Course>>(emptyList())

    override fun getAll(): Flow<List<Course>> = store

    override fun getByTeacher(teacherId: UuidString): Flow<List<Course>> = store

    override suspend fun save(classRoom: Course) {
        store.update { list ->
            val idx = list.indexOfFirst { it.id == classRoom.id }
            if (idx >= 0) list.toMutableList().also { it[idx] = classRoom }
            else list + classRoom
        }
    }

    override suspend fun delete(id: UuidString) {
        store.update { list -> list.filter { it.id != id.value } }
    }
}