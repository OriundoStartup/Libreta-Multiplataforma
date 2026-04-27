package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.repository.JustificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface JustificationListUiState {
    data object Loading : JustificationListUiState
    data class Success(val items: List<Justification>) : JustificationListUiState
    data class Error(val message: String) : JustificationListUiState
}

class JustificationListScreenModel(
    private val repository: JustificationRepository
) : ScreenModel {

    private val _state = MutableStateFlow<JustificationListUiState>(JustificationListUiState.Loading)
    val state: StateFlow<JustificationListUiState> = _state.asStateFlow()

    fun load(studentId: UuidString) {
        screenModelScope.launch {
            _state.value = JustificationListUiState.Loading
            repository.getByStudent(studentId)
                .catch { e -> _state.value = JustificationListUiState.Error(e.message ?: "Error") }
                .collect { items ->
                    _state.value = JustificationListUiState.Success(items.sortedByDescending { it.date })
                }
        }
    }
}
