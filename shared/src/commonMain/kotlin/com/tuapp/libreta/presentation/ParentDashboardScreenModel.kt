package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.CoursesRepository
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.repository.MessageRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class StudentSummary(
    val id: UuidString,
    val courseId: UuidString,
    val name: String,
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
        println("DEBUG init: ParentDashboardScreenModel creado")
        load()
    }

    fun load() {
        loadJob?.cancel()
        val userId = authService.currentUserId()
        
        if (userId == null) {
            println("DEBUG load: Sesión nula")
            _state.update { it.copy(uiState = ParentDashboardUiState.Error("No hay sesión activa")) }
            return
        }

        println("DEBUG load: Cargando alumnos para $userId")
        loadJob = studentRepo.getStudentsByParent(userId)
            .onEach { students ->
                println("DEBUG load: Recibidos ${students.size} alumnos del repo")
                
                if (students.isEmpty()) {
                    println("DEBUG load: Cambiando a estado NoStudents")
                    _state.update { it.copy(uiState = ParentDashboardUiState.NoStudents) }
                    return@onEach
                }

                try {
                    // Procesar datos adicionales
                    val summaries = students.map { student ->
                        val attendance = attendanceRepo.getByStudent(student.id).first()
                        val total = attendance.size.coerceAtLeast(1)
                        val present = attendance.count { it.status == AttendanceStatus.PRESENT }
                        val msgs = messageRepo.getInbox(userId.value)
                            .count { it.unread }
                        
                        StudentSummary(
                            id = student.id,
                            courseId = student.courseId,
                            name = student.fullName,
                            attendancePercent = present * 100 / total,
                            lastNote = "Sin anotaciones recientes",
                            pendingMessages = msgs
                        )
                    }

                    val success = ParentDashboardUiState.Success(
                        students = summaries,
                        selectedIndex = 0,
                        timeline = buildTimeline(students.first().id, userId)
                    )
                    
                    println("DEBUG load: Cambiando a estado SUCCESS con ${summaries.size} alumnos")
                    _state.update { it.copy(uiState = success) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("ERROR load processing: ${e.message}")
                    _state.update { it.copy(uiState = ParentDashboardUiState.Error(e.message ?: "Error al procesar datos")) }
                }
            }
            .catch { e ->
                if (e is CancellationException) {
                    println("DEBUG load: Flow cancelado correctamente")
                    throw e
                }
                println("ERROR load: ${e.message}")
                _state.update { it.copy(uiState = ParentDashboardUiState.Error(e.message ?: "Error desconocido")) }
            }
            .launchIn(screenModelScope)
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
                        _state.update { it.copy(
                            showAddStudentDialog = false, 
                            isActionLoading = false,
                            successMessage = "Registrado: $name"
                        ) }
                        load()
                    }
                    .onFailure { e ->
                        _state.update { it.copy(isActionLoading = false, error = e.message) }
                    }
            } catch (e: CancellationException) {
                throw e
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("ERROR selectStudent: ${e.message}")
            }
        }
    }

    private suspend fun buildTimeline(studentId: UuidString, parentId: UuidString): List<TimelineEvent> {
        val attendanceEvents = attendanceRepo.getByStudent(studentId).first()
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
}
