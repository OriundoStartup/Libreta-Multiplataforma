package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.usecase.GetPendingJustificationsUseCase
import com.tuapp.libreta.domain.usecase.ReviewJustificationUseCase
import com.tuapp.libreta.domain.usecase.SubmitJustificationUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class JustificationReason(val label: String) {
    HEALTH("Salud"), PERSONAL("Personal"), ERRAND("Trámite"), OTHER("Otro")
}

sealed interface JustificationFormState {
    data object Idle    : JustificationFormState
    data object Sending : JustificationFormState
    data object Sent    : JustificationFormState
    data class  Error(val message: String) : JustificationFormState
}

sealed interface JustificationReviewState {
    data object Loading : JustificationReviewState
    data object Empty   : JustificationReviewState
    data class  Success(val pending: List<Justification>) : JustificationReviewState
}

class JustificationScreenModel(
    private val submitUseCase: SubmitJustificationUseCase,
    private val getPendingUseCase: GetPendingJustificationsUseCase,
    private val reviewUseCase: ReviewJustificationUseCase
) : ScreenModel {

    private val _formState = MutableStateFlow<JustificationFormState>(JustificationFormState.Idle)
    val formState: StateFlow<JustificationFormState> = _formState.asStateFlow()

    private val _reviewState = MutableStateFlow<JustificationReviewState>(JustificationReviewState.Loading)
    val reviewState: StateFlow<JustificationReviewState> = _reviewState.asStateFlow()

    fun submitJustification(studentId: String, parentId: String, teacherId: String,
                            dateEpoch: Long, reason: JustificationReason, description: String) {
        screenModelScope.launch {
            _formState.value = JustificationFormState.Sending
            runCatching { submitUseCase(studentId, dateEpoch, "${reason.label}: $description") }
                .onSuccess { _formState.value = JustificationFormState.Sent }
                .onFailure { e -> _formState.value = JustificationFormState.Error(e.message ?: "Error") }
        }
    }

    fun loadPending(studentId: String) {
        getPendingUseCase(studentId)
            .onStart { _reviewState.value = JustificationReviewState.Loading }
            .onEach  { list ->
                _reviewState.value = if (list.isEmpty()) JustificationReviewState.Empty
                                     else JustificationReviewState.Success(list)
            }
            .catch   { _reviewState.value = JustificationReviewState.Empty }
            .launchIn(screenModelScope)
    }

    fun approve(justification: Justification, parentId: String) {
        screenModelScope.launch {
            runCatching { reviewUseCase(justification, approved = true, parentId = parentId) }
        }
    }

    fun reject(justification: Justification, parentId: String) {
        screenModelScope.launch {
            runCatching { reviewUseCase(justification, approved = false, parentId = parentId) }
        }
    }
}
