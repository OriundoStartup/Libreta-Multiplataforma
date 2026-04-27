package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Course
import com.tuapp.libreta.domain.model.NoticeCategory
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.repository.ClassRoomRepository
import com.tuapp.libreta.domain.repository.CommunicationRepository
import com.tuapp.libreta.domain.usecase.GetStudentsByClassUseCase
import com.tuapp.libreta.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

enum class ComposeMode { GENERAL, DIRECT }

sealed interface NoticeUiState {
    data object Idle    : NoticeUiState
    data object Sending : NoticeUiState
    data object Sent    : NoticeUiState
    data class  Error(val message: String) : NoticeUiState
}

class NoticeScreenModel(
    private val classRepo: ClassRoomRepository,
    private val communicationRepo: CommunicationRepository,
    private val getStudents: GetStudentsByClassUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val authService: SupabaseAuthService
) : ScreenModel {

    private val _state = MutableStateFlow<NoticeUiState>(NoticeUiState.Idle)
    val state: StateFlow<NoticeUiState> = _state.asStateFlow()

    // Expose courses directly
    private val _classes = MutableStateFlow<List<Course>>(emptyList())
    val classes: StateFlow<List<Course>> = _classes.asStateFlow()

    private val _composeMode = MutableStateFlow(ComposeMode.GENERAL)
    val composeMode: StateFlow<ComposeMode> = _composeMode.asStateFlow()

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    init {
        authService.currentUserId()?.let { uid ->
            classRepo.getByTeacher(uid)
                .distinctUntilChanged()
                .onEach { courses ->
                    _classes.value = courses
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

    fun setComposeMode(mode: ComposeMode) {
        _composeMode.value = mode
    }

    fun loadStudents(classId: UuidString) {
        getStudents(classId)
            .distinctUntilChanged()
            .onEach { _students.value = it }
            .catch { _students.value = emptyList() }
            .launchIn(screenModelScope)
    }

    fun sendDirectMessage(parentId: UuidString, content: String) {
        if (content.isBlank()) { _state.value = NoticeUiState.Error("El mensaje no puede estar vacío"); return }
        screenModelScope.launch {
            _state.value = NoticeUiState.Sending
            sendMessageUseCase(receiverId = parentId, content = content)
                .onSuccess { _state.value = NoticeUiState.Sent }
                .onFailure { e -> _state.value = NoticeUiState.Error(e.message ?: "Error al enviar") }
        }
    }
}
