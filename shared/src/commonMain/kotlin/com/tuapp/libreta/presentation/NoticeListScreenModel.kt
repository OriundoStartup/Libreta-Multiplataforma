package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.repository.CommunicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed interface NoticeListUiState {
    data object Loading : NoticeListUiState
    data class Success(val notices: List<Message>) : NoticeListUiState
    data class Error(val message: String) : NoticeListUiState
}

class NoticeListScreenModel(
    private val communicationRepo: CommunicationRepository
) : ScreenModel {

    private val _state = MutableStateFlow<NoticeListUiState>(NoticeListUiState.Loading)
    val state: StateFlow<NoticeListUiState> = _state.asStateFlow()

    fun loadNotices(classId: UuidString) {
        screenModelScope.launch {
            _state.value = NoticeListUiState.Loading
            communicationRepo.getByClass(classId)
                .onEach { notices ->
                    _state.value = NoticeListUiState.Success(notices)
                }
                .catch { e ->
                    _state.value = NoticeListUiState.Error(e.message ?: "Error al cargar avisos")
                }
                .collect {}
        }
    }
}
