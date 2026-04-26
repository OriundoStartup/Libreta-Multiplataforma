package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Profile
import com.tuapp.libreta.domain.repository.ProfileRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(private val queries: LibretaAppQueries) : ProfileRepository {

    override fun getAll(): Flow<List<Profile>> =
        queries.getAllProfiles().asFlow().mapToList(getIoDispatcher())
            .map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }

    override suspend fun save(profile: Profile) {
        val now = currentEpochMs()
        queries.insertOrReplaceProfile(
            id = profile.id.value,
            role = profile.role.name,
            full_name = profile.fullName,
            sync_status = "PENDING_INSERT",
            created_at = now,
            updated_at = now
        )
    }
    override suspend fun delete(id: UuidString) {
        queries.markProfileAsPendingDelete(updated_at = currentEpochMs(), id = id.value)
    }
}