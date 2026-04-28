package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.CoursesRepository
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.repository.MessageRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

data class StudentSummary(
    val id: UuidString,
    val courseId: UuidString,
    val name: String,
    val rut: String?,
    val attendancePercent: Int,
    val lastNote: String,
    val pendingMessages: Int
)

data class TimelineEvent(
    val id: String,
    val type: TimelineEventType,
    val title: String,
    val subtitle: String,
    val date: String
)

enum class TimelineEventType { ATTENDANCE_PRESENT, ATTENDANCE_ABSENT, MESSAGE, JUSTIFICATION }

data class ParentDashboardState(
    val uiState: ParentDashboardUiState = ParentDashboardUiState.Loading,
    val showAddStudentDialog: Boolean = false,
    val isActionLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

sealed interface ParentDashboardUiState {
    data object Loading : ParentDashboardUiState
    data object NoStudents : ParentDashboardUiState
    data class Success(
        val students: List<StudentSummary>,
        val selectedIndex: Int,
        val timeline: List<TimelineEvent>
    ) : ParentDashboardUiState
    data class Error(val message: String) : ParentDashboardUiState
}

class ParentDashboardScreenModel(
    private val studentRepo: StudentRepository,
    private val attendanceRepo: AttendanceRepository,
    private val messageRepo: MessageRepository,
    private val coursesRepo: CoursesRepository,
    private val authService: SupabaseAuthService
) : ScreenModel {

    private val _state = MutableStateFlow(ParentDashboardState())
    val state: StateFlow<ParentDashboardState> = _state.asStateFlow()

    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        val userId = authService.currentUserId()
        
        if (userId == null) {
            _state.update { it.copy(uiState = ParentDashboardUiState.Error("No hay sesión activa")) }
            return
        }

        loadJob = studentRepo.getStudentsByParent(userId)
            .onEach { students ->
                if (students.isEmpty()) {
                    _state.update { it.copy(uiState = ParentDashboardUiState.NoStudents) }
                    return@onEach
                }

                try {
                    // 1. Mostrar estado inicial de ÉXITO con datos básicos
                    val initialSummaries = students.map { student ->
                        StudentSummary(
                            id = student.id,
                            courseId = student.courseId,
                            name = student.fullName,
                            rut = student.studentRut,
                            attendancePercent = 100,
                            lastNote = "Cargando...",
                            pendingMessages = 0
                        )
                    }

                    _state.update { it.copy(
                        uiState = ParentDashboardUiState.Success(
                            students = initialSummaries,
                            selectedIndex = 0,
                            timeline = emptyList()
                        )
                    )}

                    // 2. Cargar detalles (asistencia, inbox) en segundo plano
                    enrichStudentData(students, userId)

                } catch (e: Exception) {
                    _state.update { it.copy(uiState = ParentDashboardUiState.Error(e.message ?: "Error al procesar datos")) }
                }
            }
            .catch { e ->
                if (e !is CancellationException) {
                    _state.update { it.copy(uiState = ParentDashboardUiState.Error(e.message ?: "Error de red")) }
                }
            }
            .launchIn(screenModelScope)
    }

    private fun enrichStudentData(students: List<Student>, userId: UuidString) {
        screenModelScope.launch {
            try {
                val summaries = students.map { student ->
                    val attendance = withTimeoutOrNull(3000) {
                        attendanceRepo.getByStudent(student.id).firstOrNull()
                    } ?: emptyList()
                    
                    val total = attendance.size.coerceAtLeast(1)
                    val present = attendance.count { it.status == AttendanceStatus.PRESENT }
                    
                    val msgsCount = try {
                        withTimeoutOrNull(3000) {
                            messageRepo.getInbox(userId.value).count { it.unread }
                        } ?: 0
                    } catch (e: Exception) { 0 }

                    StudentSummary(
                        id = student.id,
                        courseId = student.courseId,
                        name = student.fullName,
                        rut = student.studentRut,
                        attendancePercent = present * 100 / total,
                        lastNote = "Sin anotaciones recientes",
                        pendingMessages = msgsCount
                    )
                }

                val timeline = try {
                    withTimeoutOrNull(3000) {
                        buildTimeline(students.first().id, userId)
                    } ?: emptyList()
                } catch (e: Exception) { emptyList() }

                _state.update { it.copy(
                    uiState = ParentDashboardUiState.Success(
                        students = summaries,
                        selectedIndex = 0,
                        timeline = timeline
                    )
                )}
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    private suspend fun buildTimeline(studentId: UuidString, parentId: UuidString): List<TimelineEvent> {
        val attendanceEvents = (attendanceRepo.getByStudent(studentId).firstOrNull() ?: emptyList())
            .takeLast(5).mapIndexed { i, record ->
                val present = record.status == AttendanceStatus.PRESENT
                TimelineEvent(
                    id = "att-$i",
                    type = if (present) TimelineEventType.ATTENDANCE_PRESENT else TimelineEventType.ATTENDANCE_ABSENT,
                    title = "Asistencia",
                    subtitle = if (present) "Presente" else "Ausente",
                    date = "Registro ${i + 1}"
                )
            }
        
        val inbox = messageRepo.getInbox(parentId.value)
        val messageEvents = inbox.take(3).mapIndexed { i, thread ->
            TimelineEvent(
                id = "msg-$i",
                type = TimelineEventType.MESSAGE,
                title = "Mensaje de ${thread.contactName}",
                subtitle = thread.lastMessage.take(40),
                date = if (thread.unread) "Sin leer" else "Leído"
            )
        }

        return (attendanceEvents + messageEvents).sortedByDescending { it.id }
    }

    fun logout() {
        screenModelScope.launch { authService.signOut() }
    }

    fun onAddStudentClick() {
        _state.update { it.copy(showAddStudentDialog = true) }
    }

    fun onDismissDialog() {
        _state.update { it.copy(showAddStudentDialog = false, error = null) }
    }

    fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
    }

    fun enrollStudent(name: String, rut: String?) {
        val userId = authService.currentUserId() ?: return
        screenModelScope.launch {
            _state.update { it.copy(isActionLoading = true, error = null) }
            try {
                val profile = authService.getProfile(userId.value)
                val courseId = profile?.courseId ?: throw Exception("No se encontró curso vinculado")
                coursesRepo.enrollStudent(courseId, name, rut)
                    .onSuccess {
                        _state.update { it.copy(showAddStudentDialog = false, isActionLoading = false, successMessage = "Registrado: $name") }
                        load()
                    }
                    .onFailure { e -> _state.update { it.copy(isActionLoading = false, error = e.message) } }
            } catch (e: Exception) {
                _state.update { it.copy(isActionLoading = false, error = e.message) }
            }
        }
    }

    fun selectStudent(index: Int) {
        val current = _state.value.uiState as? ParentDashboardUiState.Success ?: return
        val userId = authService.currentUserId() ?: return
        screenModelScope.launch {
            try {
                val updatedSuccess = current.copy(
                    selectedIndex = index,
                    timeline = buildTimeline(current.students[index].id, userId)
                )
                _state.update { it.copy(uiState = updatedSuccess) }
            } catch (e: Exception) {
                println("ERROR selectStudent: ${e.message}")
            }
        }
    }
}
