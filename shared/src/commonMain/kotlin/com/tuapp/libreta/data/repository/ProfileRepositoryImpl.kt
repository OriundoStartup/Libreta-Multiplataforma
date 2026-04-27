package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.sync.SyncManager
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Profile
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.ProfileRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileRepositoryImpl(
    private val queries: LibretaAppQueries,
    private val syncManager: SyncManager
) : ProfileRepository {

    private val scope = CoroutineScope(SupervisorJob() + getIoDispatcher())

    override fun getAll(): Flow<List<Profile>> =
        queries.getAllProfiles().asFlow().mapToList(getIoDispatcher())
            .map { list -> list.map { it.toDomain() } }.catch { emit(emptyList()) }

    override suspend fun save(profile: Profile) {
        withContext(getIoDispatcher()) {
            val now = currentEpochMs()
            queries.insertOrReplaceProfile(
                id = profile.id.value,
                role = profile.role.name,
                full_name = profile.fullName,
                sync_status = SyncStatus.PENDING_INSERT.name,
                created_at = now,
                updated_at = now
            )
            scope.launch { syncManager.syncAll() }
        }
    }
    override suspend fun delete(id: UuidString) {
        withContext(getIoDispatcher()) {
            queries.markProfileAsPendingDelete(updated_at = currentEpochMs(), id = id.value)
            scope.launch { syncManager.syncAll() }
        }
    }
}
