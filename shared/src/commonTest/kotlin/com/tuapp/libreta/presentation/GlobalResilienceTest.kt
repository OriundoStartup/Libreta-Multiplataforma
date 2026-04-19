package com.tuapp.libreta.presentation

import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.usecase.GetConversationUseCase
import com.tuapp.libreta.domain.usecase.GetInboxUseCase
import com.tuapp.libreta.domain.usecase.SendMessageUseCase
import io.mockative.Mock
import io.mockative.every
import io.mockative.mock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GlobalResilienceTest {

    @Mock
    private val authService = mock(SupabaseAuthService::class)
    @Mock
    private val getInbox = mock(GetInboxUseCase::class)
    @Mock
    private val getConversation = mock(GetConversationUseCase::class)
    @Mock
    private val sendMessage = mock(SendMessageUseCase::class)

    private val screenModel by lazy {
        MessageScreenModel(getInbox, getConversation, sendMessage, authService)
    }

    @Test
    fun `resilience test - passing empty string as UUID to ScreenModel should not crash`() = runTest {
        val invalidId = ""
        val result = invalidId.toUuidOrNull()
        
        assertNull(result, "Empty string should result in null UuidString")
        
        // Simular llamada de UI con este ID
        screenModel.loadConversation(invalidId)
        
        // Verificar que el estado de la conversación es Success(emptyList) y no Loading o Error
        assertEquals(ConversationUiState.Success(emptyList()), screenModel.conversation.value)
    }

    @Test
    fun `resilience test - passing malformed UUID string should not crash`() = runTest {
        val malformedId = "abc-123-not-uuid"
        
        screenModel.loadConversation(malformedId)
        
        assertEquals(ConversationUiState.Success(emptyList()), screenModel.conversation.value)
    }

    @Test
    fun `resilience test - domain model initialization with invalid UUID should throw exception`() {
        // Esto valida que la CAPA DE DATOS y DOMINIO están protegidas si la UI falla en filtrar
        assertFailsWith<IllegalArgumentException> {
            UuidString("invalid-id")
        }
    }

    @Test
    fun `resilience test - null current user should be handled gracefully`() = runTest {
        every { authService.currentUserId() }.returns(null)
        
        screenModel.loadInbox()
        
        assertEquals(InboxUiState.Empty, screenModel.inbox.value)
    }
}
