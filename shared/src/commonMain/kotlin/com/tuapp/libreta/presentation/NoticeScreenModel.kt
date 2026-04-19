package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.ClassRoom
import com.tuapp.libreta.domain.repository.CommunicationRepository
import com.tuapp.libreta.domain.repository.CourseAssignmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

enum class NoticeCategory(val label: String, val emoji: String) {
    URGENT("Urgente",     "🚨"),
    INFO("Informativo",   "ℹ️"),
    ACADEMIC("Académico", "📚")
}

sealed interface NoticeUiState {
    data object Idle    : NoticeUiState
    data object Sending : NoticeUiState
    data object Sent    : NoticeUiState
    data class  Error(val message: String) : NoticeUiState
}

class NoticeScreenModel(
    private val courseRepo: CourseAssignmentRepository,
    private val communicationRepo: CommunicationRepository,
    private val authService: SupabaseAuthService
) : ScreenModel {

    private val _state = MutableStateFlow<NoticeUiState>(NoticeUiState.Idle)
    val state: StateFlow<NoticeUiState> = _state.asStateFlow()

    // Expose courses as ClassRoom-like objects from course_assignments
    private val _classes = MutableStateFlow<List<ClassRoom>>(emptyList())
    val classes: StateFlow<List<ClassRoom>> = _classes.asStateFlow()

    init {
        authService.currentUserId()?.let { uid ->
            courseRepo.getByTeacher(uid)
                .onEach { assignments ->
                    _classes.value = assignments.map { a ->
                        // courseId.value gives us a string for display/code purposes
                        ClassRoom(
                            id = a.courseId,
                            classCode = a.courseId.value.takeLast(6).uppercase(), 
                            name = "Curso ${a.courseId.value.take(4)}", 
                            teacherId = a.teacherId
                        )
                    }
                }
                .catch { }
                .launchIn(screenModelScope)
        }
    }

    fun sendNotice(classId: UuidString, content: String, category: NoticeCategory) {
        if (content.isBlank()) { _state.value = NoticeUiState.Error("El mensaje no puede estar vacío"); return }
        val senderUuid = authService.currentUserId() ?: run {
            _state.value = NoticeUiState.Error("Sesión no válida")
            return
        }
        screenModelScope.launch {
            _state.value = NoticeUiState.Sending
            runCatching {
                communicationRepo.sendGeneralNotice(
                    senderId = senderUuid,
                    classId  = classId,
                    content  = "[${category.emoji} ${category.label}] $content"
                )
            }.onSuccess { _state.value = NoticeUiState.Sent }
             .onFailure { e -> _state.value = NoticeUiState.Error(e.message ?: "Error al enviar") }
        }
    }

    fun resetState() { _state.value = NoticeUiState.Idle }
}
