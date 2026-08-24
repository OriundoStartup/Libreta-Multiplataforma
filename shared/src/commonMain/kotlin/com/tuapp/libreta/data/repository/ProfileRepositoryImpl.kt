package com.tuapp.libreta.data.repository

import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.data.util.toDomainList
import com.tuapp.libreta.data.sync.SyncManager
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.di.dbReady
import com.tuapp.libreta.domain.model.Profile
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.ProfileRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileRepositoryImpl(
    private val queries: LibretaAppQueries,
    private val syncManager: SyncManager
) : ProfileRepository {

    private val scope = CoroutineScope(SupervisorJob() + getIoDispatcher())

    private suspend fun waitForDb() {
        runCatching {
            kotlinx.coroutines.withTimeout(5000) { dbReady.await() }
        }.onFailure { 
            AppLogger.e("ProfileRepo", "Database ready timeout: ${it.message}")
        }
    }

    override fun getAll(): Flow<List<Profile>> = kotlinx.coroutines.flow.flow {
        waitForDb()
        queries.getAllProfiles().toDomainList { it.toDomain() }.collect { emit(it) }
    }

    override suspend fun save(profile: Profile) {
        waitForDb()
        withContext(getIoDispatcher()) {
            val now = currentEpochMs()
            queries.insertOrReplaceProfile(
                id = profile.id.value,
                full_name = profile.fullName,
                role = profile.role.name,
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
            queries.markProfileAsPendingDelete(updated_at = currentEpochMs(), id = id.value)
            scope.launch { syncManager.syncAll() }
        }
    }
}
