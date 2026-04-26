package com.tuapp.libreta.data.repository

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Profile
import com.tuapp.libreta.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class InMemoryProfileRepository : ProfileRepository {
    private val store = MutableStateFlow<List<Profile>>(emptyList())

    override fun getAll(): Flow<List<Profile>> = store

    override suspend fun save(profile: Profile) {
        store.update { list ->
            val idx = list.indexOfFirst { it.id == profile.id }
            if (idx >= 0) list.toMutableList().also { it[idx] = profile }
            else list + profile
        }
    }

    override suspend fun delete(id: UuidString) {
        store.update { list -> list.filter { it.id != id } }
    }
}
