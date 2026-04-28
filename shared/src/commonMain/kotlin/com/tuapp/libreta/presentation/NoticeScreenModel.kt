package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Course
import com.tuapp.libreta.domain.model.NoticeCategory
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.model.UserRole
import com.tuapp.libreta.data.remote.CoursesRepository
import com.tuapp.libreta.domain.repository.CommunicationRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import com.tuapp.libreta.domain.usecase.GetStudentsByClassUseCase
import com.tuapp.libreta.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn

enum class ComposeMode { GENERAL, DIRECT }

sealed interface NoticeUiState {
    data object Idle    : NoticeUiState
    data object Sending : NoticeUiState
    data object Sent    : NoticeUiState
    data class  Error(val message: String) : NoticeUiState
}

class NoticeScreenModel(
    private val coursesRepo: CoursesRepository,
    private val communicationRepo: CommunicationRepository,
    private val studentRepo: StudentRepository,
    private val getStudents: GetStudentsByClassUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val authService: SupabaseAuthService
) : ScreenModel {

    private val _state = MutableStateFlow<NoticeUiState>(NoticeUiState.Idle)
    val state: StateFlow<NoticeUiState> = _state.asStateFlow()

    private val _userRole = MutableStateFlow<UserRole?>(null)
    val userRole: StateFlow<UserRole?> = _userRole.asStateFlow()

    private val _classes = MutableStateFlow<List<Course>>(emptyList())
    val classes: StateFlow<List<Course>> = _classes.asStateFlow()

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private val _isStudentsLoading = MutableStateFlow(false)
    val isStudentsLoading: StateFlow<Boolean> = _isStudentsLoading.asStateFlow()

    private val _composeMode = MutableStateFlow(ComposeMode.GENERAL)
    val composeMode: StateFlow<ComposeMode> = _composeMode.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        screenModelScope.launch {
            val uid = authService.currentUserId() ?: return@launch
            val role = authService.getUserRole(uid.value)
            _userRole.value = role

            if (role == UserRole.TEACHER) {
                // Obtenemos los cursos directamente de Supabase para evitar "Cargando curso..."
                coursesRepo.getTeacherCourses()
                    .onSuccess { _classes.value = it }
                    .onFailure { println("ERROR loading courses: ${it.message}") }
            } else {
                _composeMode.value = ComposeMode.DIRECT
                studentRepo.getStudentsByParent(uid).collect { studentsList ->
                    val courseIds = studentsList.map { it.courseId.value }.distinct()
                    val allCoursesResult = coursesRepo.getTeacherCourses() // Ajustar si hay getCoursesByIds
                    val allCourses = allCoursesResult.getOrNull() ?: emptyList()
                    _classes.value = allCourses.filter { it.id in courseIds }
                }
            }
        }
    }

    fun loadStudents(classId: UuidString) {
        _isStudentsLoading.value = true
        getStudents(classId)
            .distinctUntilChanged()
            .onEach { 
                _students.value = it 
                _isStudentsLoading.value = false
                println("DEBUG NoticeModel: Loaded ${it.size} students for $classId")
            }
            .catch { 
                _students.value = emptyList() 
                _isStudentsLoading.value = false
            }
            .launchIn(screenModelScope)
    }

    fun sendNotice(classId: UuidString, content: String, category: NoticeCategory) {
        if (content.isBlank()) return
        screenModelScope.launch {
            _state.value = NoticeUiState.Sending
            val senderUuid = authService.currentUserId() ?: return@launch
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

    fun sendDirectMessage(classId: UuidString, parentId: UuidString?, content: String, category: NoticeCategory? = null) {
        if (content.isBlank()) return
        screenModelScope.launch {
            _state.value = NoticeUiState.Sending
            val role = _userRole.value
            val receiverId = if (role == UserRole.PARENT) {
                _classes.value.find { it.id == classId.value }?.teacherId?.let { UuidString(it) }
            } else {
                parentId
            }

            if (receiverId == null) {
                _state.value = NoticeUiState.Error("No se pudo identificar al destinatario")
                return@launch
            }

            val finalContent = if (category != null) "[${category.emoji} ${category.label}] $content" else content

            sendMessageUseCase(receiverId = receiverId, content = finalContent)
                .onSuccess { _state.value = NoticeUiState.Sent }
                .onFailure { e -> _state.value = NoticeUiState.Error(e.message ?: "Error al enviar") }
        }
    }

    fun setComposeMode(mode: ComposeMode) { _composeMode.value = mode }
    fun resetState() { _state.value = NoticeUiState.Idle }
}
