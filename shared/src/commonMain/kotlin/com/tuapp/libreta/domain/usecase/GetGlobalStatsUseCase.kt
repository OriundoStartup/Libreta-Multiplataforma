package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.repository.JustificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class GlobalStats(
    val pendingJustificationsCount: Int
)

class GetGlobalStatsUseCase(
    private val justificationRepo: JustificationRepository
) {
    operator fun invoke(teacherId: UuidString): Flow<GlobalStats> {
        return justificationRepo.getPendingByTeacher(teacherId).map { justifications ->
            GlobalStats(
                pendingJustificationsCount = justifications.size
            )
        }
    }
}
