package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.model.JustificationStatus
import com.tuapp.libreta.test.FakeJustificationRepository
import com.tuapp.libreta.test.FakeMessageRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewJustificationUseCaseTest {

    private val justificationRepo = FakeJustificationRepository()
    private val messageRepo       = FakeMessageRepository()
    private val useCase           = ReviewJustificationUseCase(justificationRepo, messageRepo)

    private val pending = Justification("j1", "s1", 1_000_000L, "Salud: fiebre", JustificationStatus.PENDING)

    @Test
    fun `approve changes status to APPROVED`() = runTest {
        justificationRepo.justifications.value = listOf(pending)

        useCase(pending, approved = true, parentId = "p1")

        val updated = justificationRepo.justifications.value.first { it.id == "j1" }
        assertEquals(JustificationStatus.APPROVED, updated.status)
    }

    @Test
    fun `reject changes status to REJECTED`() = runTest {
        justificationRepo.justifications.value = listOf(pending)

        useCase(pending, approved = false, parentId = "p1")

        val updated = justificationRepo.justifications.value.first { it.id == "j1" }
        assertEquals(JustificationStatus.REJECTED, updated.status)
    }

    @Test
    fun `approve sends silent notification to parent`() = runTest {
        useCase(pending, approved = true, parentId = "p1")

        val messages = messageRepo.messages.value
        assertEquals(1, messages.size)
        assertEquals("system", messages.first().senderId)
        assertEquals("p1", messages.first().receiverId)
        assertTrue(messages.first().content.contains("aprobada"))
    }

    @Test
    fun `reject sends silent notification with rejected text`() = runTest {
        useCase(pending, approved = false, parentId = "p1")

        val msg = messageRepo.messages.value.first()
        assertTrue(msg.content.contains("rechazada"))
    }
}
