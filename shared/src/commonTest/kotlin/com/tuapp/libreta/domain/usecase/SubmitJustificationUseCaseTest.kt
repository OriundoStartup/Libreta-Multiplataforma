package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.domain.model.JustificationStatus
import com.tuapp.libreta.test.FakeJustificationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubmitJustificationUseCaseTest {

    private val repo    = FakeJustificationRepository()
    private val useCase = SubmitJustificationUseCase(repo)

    @Test
    fun `justification is saved with PENDING status`() = runTest {
        val pastDate = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - 86_400_000L // ayer

        useCase("s1", pastDate, "Salud: fiebre")

        val saved = repo.justifications.value.first()
        assertEquals(JustificationStatus.PENDING, saved.status)
        assertEquals("s1", saved.studentId)
        assertEquals("Salud: fiebre", saved.reason)
    }

    @Test
    fun `future date is accepted by use case`() = runTest {
        val futureDate = 9_999_999_999_999L  // year 2286 — always in the future

        useCase("s1", futureDate, "Trámite: cita médica programada")

        val saved = repo.justifications.value.first()
        assertEquals(JustificationStatus.PENDING, saved.status)
        assertEquals(futureDate, saved.date)
    }

    @Test
    fun `multiple justifications for same student are all saved`() = runTest {
        val date = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        useCase("s1", date - 86_400_000L, "Salud: fiebre")
        useCase("s1", date - 172_800_000L, "Personal: viaje familiar")

        val all = repo.getByStudent("s1").first()
        assertEquals(2, all.size)
        assertTrue(all.all { it.status == JustificationStatus.PENDING })
    }
}
