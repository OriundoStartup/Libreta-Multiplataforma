package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.repository.JustificationRepository
import com.tuapp.libreta.domain.repository.MessageRepository
import com.tuapp.libreta.domain.usecase.ReviewJustificationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface GlobalJustificationUiState {
    data object Loading : GlobalJustificationUiState
    data class Success(val pending: List<Justification>) : GlobalJustificationUiState
    data class Error(val message: String) : GlobalJustificationUiState
}

class GlobalJustificationScreenModel(
    private val justificationRepo: JustificationRepository,
    private val reviewUseCase: ReviewJustificationUseCase,
    private val authService: SupabaseAuthService
) : ScreenModel {

    private val _state = MutableStateFlow<GlobalJustificationUiState>(GlobalJustificationUiState.Loading)
    val state: StateFlow<GlobalJustificationUiState> = _state.asStateFlow()

    fun load() {
        screenModelScope.launch {
            _state.value = GlobalJustificationUiState.Loading
            val user = authService.currentUser() ?: return@launch
            
            justificationRepo.getPendingByTeacher(UuidString(user.id))
                .catch { e -> _state.value = GlobalJustificationUiState.Error(e.message ?: "Error") }
                .collect { items ->
                    _state.value = GlobalJustificationUiState.Success(items.sortedBy { it.date })
                }
        }
    }

    fun review(justification: Justification, approved: Boolean) {
        screenModelScope.launch {
            try {
                // El reviewUseCase requiere parentId para notificar. 
                // En un sistema real lo obtendríamos del enrollment.
                // Por ahora usamos el studentId.parentId si estuviera disponible.
                // Simulamos parentId para la notificación.
                reviewUseCase(justification, approved, justification.studentId)
                load()
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
}
