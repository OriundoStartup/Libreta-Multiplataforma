package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.CoursesRepository
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.repository.MessageRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentSummary(
    val id: UuidString,
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
    val state: StateFlow<ParentDashboardState> = _state
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5_000), ParentDashboardState())

    private val _uiState = MutableStateFlow<ParentDashboardUiState>(ParentDashboardUiState.Loading)
    val uiState: StateFlow<ParentDashboardUiState> = _uiState

    private var currentParentId: String? = null
    private var loadJob: kotlinx.coroutines.Job? = null

    init {
        screenModelScope.launch {
            val userId = authService.currentUserId()
            println("DEBUG init: userId=$userId")
            if (userId != null) {
                load(userId.value)
            } else {
                println("DEBUG init: NO HAY SESIÓN - userId es null")
                _uiState.value = ParentDashboardUiState.Error("No se encontró una sesión activa.")
            }
        }
    }

    fun load(parentId: String) {
        currentParentId = parentId
        loadJob?.cancel()
        
        println("DEBUG load: iniciando con parentId=$parentId")
        val parentUuid = parentId.toUuidOrNull() ?: run {
            println("DEBUG load: parentId inválido")
            return
        }

        loadJob = studentRepo.getStudentsByParent(parentUuid)
            .onEach { students ->
                println("DEBUG load: recibidos ${students.size} alumnos")
                if (students.isEmpty()) {
                    _uiState.value = ParentDashboardUiState.NoStudents
                    _state.update { it.copy(uiState = ParentDashboardUiState.NoStudents) }
                    return@onEach
                }
                val summaries = students.map { student ->
                    val attendance = attendanceRepo.getByStudent(student.id).first()
                    val total   = attendance.size.coerceAtLeast(1)
                    val present = attendance.count { it.status == AttendanceStatus.PRESENT }
                    val msgs    = messageRepo.getByReceiver(parentUuid).first().size
                    StudentSummary(
                        id                = student.id,
                        name              = student.fullName,
                        attendancePercent = present * 100 / total,
                        lastNote          = "Sin anotaciones recientes",
                        pendingMessages   = msgs
                    )
                }
                val successState = ParentDashboardUiState.Success(
                    students      = summaries,
                    selectedIndex = 0,
                    timeline      = buildTimeline(students.first().id, parentUuid)
                )
                _uiState.value = successState
                _state.update { it.copy(uiState = successState) }
            }
            .catch { e -> 
                println("ERROR load: ${e.message}")
                val errorState = ParentDashboardUiState.Error(e.message ?: "Error")
                _uiState.value = errorState
                _state.update { it.copy(uiState = errorState) }
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
        val parentId = currentParentId ?: return
        
        screenModelScope.launch {
            _state.update { it.copy(isActionLoading = true, error = null) }
            
            try {
                // 1. Obtener el course_id. 
                val parentUuid = parentId.toUuidOrNull() ?: throw Exception("ID de apoderado inválido")
                val existingStudents = studentRepo.getStudentsByParent(parentUuid).first()
                
                val courseId: String = if (existingStudents.isNotEmpty()) {
                    existingStudents.first().courseId.value
                } else {
                    // Si no tiene alumnos, buscamos el curso en el perfil.
                    val profile = authService.getProfile(parentId)
                    profile?.courseId ?: throw Exception("No se encontró un curso vinculado a su cuenta. Contacte al profesor.")
                }

                coursesRepo.enrollStudent(courseId, name, rut)
                    .onSuccess {
                        _state.update { it.copy(
                            showAddStudentDialog = false, 
                            isActionLoading = false,
                            successMessage = "¡${name} fue registrado exitosamente!"
                        ) }
                        load(parentId) // Recargar lista
                    }
                    .onFailure { e ->
                        _state.update { it.copy(isActionLoading = false, error = e.message) }
                    }
            } catch (e: Exception) {
                _state.update { it.copy(isActionLoading = false, error = e.message) }
            }
        }
    }

    fun selectStudent(index: Int) {
        val current = _uiState.value as? ParentDashboardUiState.Success ?: return
        val parentUuid = authService.currentUserId() ?: return
        
        screenModelScope.launch {
            _uiState.value = current.copy(
                selectedIndex = index,
                timeline      = buildTimeline(current.students[index].id, parentUuid)
            )
        }
    }

    private suspend fun buildTimeline(studentId: UuidString, parentId: UuidString): List<TimelineEvent> {
        val attendanceEvents = attendanceRepo.getByStudent(studentId).first()
            .takeLast(5).mapIndexed { i, record ->
                val present = record.status == AttendanceStatus.PRESENT
                TimelineEvent(
                    id       = "att-$i",
                    type     = if (present) TimelineEventType.ATTENDANCE_PRESENT else TimelineEventType.ATTENDANCE_ABSENT,
                    title    = "Asistencia",
                    subtitle = if (present) "Presente" else "Ausente",
                    date     = "Registro ${i + 1}"
                )
            }
        
        val messageEvents = messageRepo.getByReceiver(parentId).first().takeLast(3).mapIndexed { i, msg ->
            TimelineEvent(
                id       = "msg-$i",
                type     = TimelineEventType.MESSAGE,
                title    = "Mensaje recibido",
                subtitle = msg.content.take(60),
                date     = "Mensaje ${i + 1}"
            )
        }

        return (attendanceEvents + messageEvents).sortedByDescending { it.id }
    }
}
