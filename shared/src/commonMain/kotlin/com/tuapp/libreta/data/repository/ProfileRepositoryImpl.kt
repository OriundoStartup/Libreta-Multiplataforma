package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.now
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Profile
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(private val queries: LibretaAppQueries) : ProfileRepository {

    override fun getAll(): Flow<List<Profile>> =
        queries.getAllProfiles().asFlow().mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override suspend fun save(profile: Profile) = try {
        val now = now()
        queries.insertOrReplaceProfile(profile.id, profile.role.name, profile.firstName,
            profile.lastName, profile.email, SyncStatus.PENDING_INSERT.name, now, now)
    } catch (e: Exception) { throw RuntimeException("Error al guardar perfil: ${e.message}", e) }

    override suspend fun delete(id: String) = try {
        queries.markProfileAsPendingDelete(updated_at = now(), id = id)
    } catch (e: Exception) { throw RuntimeException("Error al eliminar perfil: ${e.message}", e) }
}
