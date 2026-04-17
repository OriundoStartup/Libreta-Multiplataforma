package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.model.JustificationStatus
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.repository.JustificationRepository
import com.tuapp.libreta.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class SubmitJustificationUseCase(private val repository: JustificationRepository) {
    suspend operator fun invoke(
        studentId: String,
        dateEpoch: Long,
        reason: String
    ) = repository.save(
        Justification(
            id        = "just-$studentId-$dateEpoch",   // composite key → always unique per student+date
            studentId = studentId,
            date      = dateEpoch,
            reason    = reason,
            status    = JustificationStatus.PENDING
        )
    )
}

class GetPendingJustificationsUseCase(private val repository: JustificationRepository) {
    operator fun invoke(studentId: String): Flow<List<Justification>> =
        repository.getByStudent(studentId)
            .map { list -> list.filter { it.status == JustificationStatus.PENDING } }
}

class ReviewJustificationUseCase(
    private val justificationRepo: JustificationRepository,
    private val messageRepo: MessageRepository
) {
    suspend operator fun invoke(
        justification: Justification,
        approved: Boolean,
        parentId: String
    ) {
        val newStatus = if (approved) JustificationStatus.APPROVED else JustificationStatus.REJECTED
        justificationRepo.save(justification.copy(status = newStatus))

        val dt = Instant.fromEpochMilliseconds(justification.date)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val dateStr = "${dt.dayOfMonth}/${dt.monthNumber}/${dt.year}"
        val icon = if (approved) "✅" else "❌"
        val verb = if (approved) "aprobada" else "rechazada"

        messageRepo.save(
            Message(
                id         = Clock.System.now().toEpochMilliseconds().toString(),
                senderId   = "system",
                receiverId = parentId,
                content    = "$icon Su justificación para la fecha $dateStr ha sido $verb."
            )
        )
    }
}
