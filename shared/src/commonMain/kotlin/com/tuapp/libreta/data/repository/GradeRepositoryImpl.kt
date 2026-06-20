package com.tuapp.libreta.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.tuapp.libreta.data.mapper.toDomain
import com.tuapp.libreta.db.GradeEntity
import com.tuapp.libreta.data.sync.SyncManager
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.random
import com.tuapp.libreta.db.LibretaAppQueries
import com.tuapp.libreta.domain.model.Grade
import com.tuapp.libreta.domain.model.SyncStatus
import com.tuapp.libreta.domain.repository.GradeRepository
import com.tuapp.libreta.util.getIoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GradeRepositoryImpl(
    private val queries: LibretaAppQueries,
    private val syncManager: SyncManager
) : GradeRepository {

    private val scope = CoroutineScope(SupervisorJob() + getIoDispatcher())

    override fun getByStudent(studentId: UuidString): Flow<List<Grade>> =
        queries.getGradesByStudent(studentId.value)
            .asFlow()
            .mapToList(getIoDispatcher())
            .map { list: List<GradeEntity> -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override fun getByCourse(courseId: UuidString): Flow<List<Grade>> =
        queries.getGradesByCourse(courseId.value)
            .asFlow()
            .mapToList(getIoDispatcher())
            .map { list: List<GradeEntity> -> list.map { it.toDomain() } }
            .catch { emit(emptyList()) }

    override suspend fun save(grade: Grade) {
        withContext(getIoDispatcher()) {
            val now = currentEpochMs()
            val gradeId = grade.id ?: UuidString.random()
            
            queries.insertOrReplaceGrade(
                id = gradeId.value,
                student_id = grade.studentId.value,
                course_id = grade.courseId.value,
                title = grade.title,
                score = grade.score,
                weight = grade.weight,
                term = grade.term,
                subject = grade.subject,
                server_version = 1,
                is_deleted = 0,
                sync_status = SyncStatus.PENDING_INSERT.name,
                created_at = now,
                updated_at = now
            )

            scope.launch {
                syncManager.syncAll()
            }
        }
    }

    override suspend fun delete(id: UuidString) {
        withContext(getIoDispatcher()) {
            queries.markGradeAsPendingDelete(
                updated_at = currentEpochMs(),
                id = id.value
            )
            // syncManager.syncAll()
        }
    }
}
