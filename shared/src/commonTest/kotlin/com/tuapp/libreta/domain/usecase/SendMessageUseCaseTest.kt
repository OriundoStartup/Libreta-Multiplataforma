package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.test.FakeMessageRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SendMessageUseCaseTest {

    private val repo    = FakeMessageRepository()
    private val useCase = SendMessageUseCase(repo)

    @Test
    fun `message is saved with correct sender and receiver`() = runTest {
        useCase("teacher-1", "parent-1", "Hola, ¿cómo está Sofía?")

        val msg = repo.messages.value.first()
        assertEquals("teacher-1", msg.senderId)
        assertEquals("parent-1",  msg.receiverId)
        assertEquals("Hola, ¿cómo está Sofía?", msg.content)
    }

    @Test
    fun `blank message throws IllegalArgumentException`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            useCase("teacher-1", "parent-1", "   ")
        }
        assertTrue(repo.messages.value.isEmpty())
    }

    @Test
    fun `empty message throws IllegalArgumentException`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            useCase("teacher-1", "parent-1", "")
        }
    }

    @Test
    fun `message content is trimmed before saving`() = runTest {
        useCase("t1", "p1", "  Hola  ")
        assertEquals("Hola", repo.messages.value.first().content)
    }

    @Test
    fun `multiple messages are all saved`() = runTest {
        useCase("t1", "p1", "Mensaje 1")
        useCase("t1", "p1", "Mensaje 2")
        assertEquals(2, repo.messages.value.size)
    }
}
