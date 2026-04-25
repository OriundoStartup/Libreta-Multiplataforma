package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.usecase.GetConversationUseCase
import com.tuapp.libreta.domain.usecase.GetInboxUseCase
import com.tuapp.libreta.domain.usecase.MarkAsReadUseCase
import com.tuapp.libreta.domain.usecase.MessageThread
import com.tuapp.libreta.domain.usecase.SendMessageUseCase
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface InboxUiState {
    data object Loading                                   : InboxUiState
    data object Empty                                     : InboxUiState
    data class  Success(
        val threads: List<MessageThread>,
        val searchQuery: String = ""
    ) : InboxUiState {
        val filteredThreads: List<MessageThread>
            get() = if (searchQuery.isBlank()) threads
                    else threads.filter {
                        it.contactName.contains(searchQuery, ignoreCase = true)
                    }
    }
}

sealed interface ConversationUiState {
    data object Loading                              : ConversationUiState
    data class  Success(val messages: List<Message>) : ConversationUiState
}

class MessageScreenModel(
    private val getInbox: GetInboxUseCase,
    private val getConversation: GetConversationUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markAsRead: MarkAsReadUseCase,
    private val authService: SupabaseAuthService,
    private val supabase: SupabaseClient
) : ScreenModel {

    val currentUserId: UuidString?
        get() = authService.currentUserId()

    private val _inbox = MutableStateFlow<InboxUiState>(InboxUiState.Loading)
    val inbox: StateFlow<InboxUiState> = _inbox.asStateFlow()

    private val _conversation = MutableStateFlow<ConversationUiState>(ConversationUiState.Loading)
    val conversation: StateFlow<ConversationUiState> = _conversation.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    fun loadInbox() {
        val uid = currentUserId ?: run {
            _inbox.value = InboxUiState.Empty; return
        }
        screenModelScope.launch {
            _inbox.value = InboxUiState.Loading
            runCatching { getInbox(uid) }
                .onSuccess { threads ->
                    _inbox.value = if (threads.isEmpty()) InboxUiState.Empty
                    else InboxUiState.Success(threads)
                }
                .onFailure {
                    AppLogger.e("MessageScreenModel", "Error loading inbox", it)
                    _inbox.value = InboxUiState.Empty
                }
        }
    }

    fun search(query: String) {
        val current = _inbox.value
        if (current is InboxUiState.Success) {
            _inbox.value = current.copy(searchQuery = query)
        }
    }

    fun openConversation(contactId: UuidString) {
        val uid = currentUserId ?: return
        
        screenModelScope.launch {
            // Carga inicial
            loadConversation(contactId)
            
            // Marcar como leído
            markAsRead(contactId, uid)

            // Realtime暂时禁用 - 需要额外的supabase-realtime模块和正确配置
            // TODO: 实现实时消息更新
        }
    }

    fun loadConversation(contactId: UuidString) {
        val uid = currentUserId ?: return
        screenModelScope.launch {
            _conversation.value = ConversationUiState.Loading
            runCatching { getConversation(uid, contactId) }
                .onSuccess { _conversation.value = ConversationUiState.Success(it) }
                .onFailure {
                    AppLogger.e("MessageScreenModel", "Error loading conversation", it)
                    _conversation.value = ConversationUiState.Success(emptyList())
                }
        }
    }

    fun sendMessage(receiverId: UuidString, content: String) {
        if (content.isBlank()) return
        
        screenModelScope.launch {
            _sending.value = true
            sendMessageUseCase(receiverId, content)
                .onSuccess {
                    loadConversation(receiverId)
                }
            _sending.value = false
        }
    }

    override fun onDispose() {
        // Cleanup if needed
        super.onDispose()
    }
}
