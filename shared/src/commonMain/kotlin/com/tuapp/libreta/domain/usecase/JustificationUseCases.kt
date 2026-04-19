package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.model.JustificationStatus
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.repository.JustificationRepository
import com.tuapp.libreta.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SubmitJustificationUseCase(private val repository: JustificationRepository) {
    suspend operator fun invoke(studentId: UuidString, dateEpoch: Long, reason: String) =
        repository.save(Justification(
            id        = null,   // Supabase generates UUID
            studentId = studentId,
            date      = dateEpoch,
            reason    = reason,
            status    = JustificationStatus.PENDING
        ))
}

class GetPendingJustificationsUseCase(private val repository: JustificationRepository) {
    operator fun invoke(studentId: UuidString): Flow<List<Justification>> =
        repository.getByStudent(studentId).map { it.filter { j -> j.status == JustificationStatus.PENDING } }
}

class ReviewJustificationUseCase(
    private val justificationRepo: JustificationRepository,
    private val messageRepo: MessageRepository
) {
    suspend operator fun invoke(justification: Justification, approved: Boolean, parentId: UuidString) {
        val newStatus = if (approved) JustificationStatus.APPROVED else JustificationStatus.REJECTED
        justificationRepo.save(justification.copy(status = newStatus))

        // Format date from epoch without kotlinx-datetime
        val totalDays = justification.date / 86_400_000L
        val dateStr   = "día $totalDays"   // simplified — UI shows full date from the form
        val icon = if (approved) "✅" else "❌"
        val verb = if (approved) "aprobada" else "rechazada"

        messageRepo.save(Message(
            id         = null,
            senderId   = justification.studentId,
            receiverId = parentId,
            content    = "$icon Su justificación ($dateStr) ha sido $verb."
        ))
    }
}
