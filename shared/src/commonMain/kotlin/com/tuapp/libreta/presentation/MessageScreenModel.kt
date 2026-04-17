package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.usecase.GetConversationUseCase
import com.tuapp.libreta.domain.usecase.GetInboxUseCase
import com.tuapp.libreta.domain.usecase.MessageThread
import com.tuapp.libreta.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface InboxUiState {
    data object Loading                                   : InboxUiState
    data object Empty                                     : InboxUiState
    data class  Success(val threads: List<MessageThread>) : InboxUiState
}

sealed interface ConversationUiState {
    data object Loading                              : ConversationUiState
    data class  Success(val messages: List<Message>) : ConversationUiState
}

class MessageScreenModel(
    private val getInbox: GetInboxUseCase,
    private val getConversation: GetConversationUseCase,
    private val sendMessage: SendMessageUseCase,
    private val currentUserId: String = "user-current"
) : ScreenModel {

    private val _inbox = MutableStateFlow<InboxUiState>(InboxUiState.Loading)
    val inbox: StateFlow<InboxUiState> = _inbox.asStateFlow()

    private val _conversation = MutableStateFlow<ConversationUiState>(ConversationUiState.Loading)
    val conversation: StateFlow<ConversationUiState> = _conversation.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    fun loadInbox() {
        getInbox(currentUserId)
            .onStart { _inbox.value = InboxUiState.Loading }
            .onEach  { threads ->
                _inbox.value = if (threads.isEmpty()) InboxUiState.Empty
                               else InboxUiState.Success(threads)
            }
            .catch   { _inbox.value = InboxUiState.Empty }
            .launchIn(screenModelScope)
    }

    fun loadConversation(contactId: String) {
        getConversation(currentUserId, contactId)
            .onStart { _conversation.value = ConversationUiState.Loading }
            .onEach  { _conversation.value = ConversationUiState.Success(it) }
            .catch   { _conversation.value = ConversationUiState.Success(emptyList()) }
            .launchIn(screenModelScope)
    }

    fun sendMessage(receiverId: String, content: String) {
        screenModelScope.launch {
            _sending.value = true
            runCatching { sendMessage(currentUserId, receiverId, content) }
            _sending.value = false
        }
    }
}
