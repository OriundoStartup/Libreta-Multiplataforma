package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.repository.StudentRepository
import com.tuapp.libreta.domain.usecase.GetPendingJustificationsUseCase
import com.tuapp.libreta.domain.usecase.ReviewJustificationUseCase
import com.tuapp.libreta.domain.usecase.SubmitJustificationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val reviewUseCase: ReviewJustificationUseCase,
    private val studentRepo: StudentRepository
) : ScreenModel {

    private val _formState = MutableStateFlow<JustificationFormState>(JustificationFormState.Idle)
    val formState: StateFlow<JustificationFormState> = _formState.asStateFlow()

    private val _reviewState = MutableStateFlow<JustificationReviewState>(JustificationReviewState.Loading)
    val reviewState: StateFlow<JustificationReviewState> = _reviewState.asStateFlow()

    fun submitJustification(
        studentId: String,
        parentId: String,
        teacherId: String,
        dateEpoch: Long,
        reason: JustificationReason,
        description: String,
        fileBytes: ByteArray? = null,
        fileName: String? = null
    ) {
        val studentUuid = studentId.toUuidOrNull() ?: run {
            _formState.value = JustificationFormState.Error("ID de estudiante inválido")
            return
        }

        screenModelScope.launch {
            _formState.value = JustificationFormState.Sending
            runCatching { 
                submitUseCase(
                    studentId = studentUuid, 
                    dateEpoch = dateEpoch, 
                    reason = "${reason.label}: $description",
                    fileBytes = fileBytes,
                    fileName = fileName
                ) 
            }
                .onSuccess { _formState.value = JustificationFormState.Sent }
                .onFailure { e -> _formState.value = JustificationFormState.Error(e.message ?: "Error") }
        }
    }

    // Used by teacher: loads pending justifications for ALL students in a course
    fun loadPending(classId: String) {
        val classUuid = classId.toUuidOrNull() ?: return

        screenModelScope.launch {
            _reviewState.value = JustificationReviewState.Loading
            runCatching {
                val students = studentRepo.getStudentsByClass(classUuid).first()
                val pending = students.flatMap { student ->
                    getPendingUseCase(student.id).first()
                }
                _reviewState.value = if (pending.isEmpty()) JustificationReviewState.Empty
                                     else JustificationReviewState.Success(pending)
            }.onFailure { _reviewState.value = JustificationReviewState.Empty }
        }
    }

    fun approve(justification: Justification, parentId: String) {
        val parentUuid = parentId.toUuidOrNull() ?: return
        screenModelScope.launch {
            runCatching { reviewUseCase(justification, approved = true, parentId = parentUuid) }
            // Refresh after review
            val current = _reviewState.value
            if (current is JustificationReviewState.Success) {
                val updated = current.pending.filter { it.id != justification.id }
                _reviewState.value = if (updated.isEmpty()) JustificationReviewState.Empty
                                     else JustificationReviewState.Success(updated)
            }
        }
    }

    fun reject(justification: Justification, parentId: String) {
        val parentUuid = parentId.toUuidOrNull() ?: return
        screenModelScope.launch {
            runCatching { reviewUseCase(justification, approved = false, parentId = parentUuid) }
            val current = _reviewState.value
            if (current is JustificationReviewState.Success) {
                val updated = current.pending.filter { it.id != justification.id }
                _reviewState.value = if (updated.isEmpty()) JustificationReviewState.Empty
                                     else JustificationReviewState.Success(updated)
            }
        }
    }

    suspend fun getSignedUrl(path: String): String {
        return submitUseCase.getAttachmentUrl(path)
    }
}
