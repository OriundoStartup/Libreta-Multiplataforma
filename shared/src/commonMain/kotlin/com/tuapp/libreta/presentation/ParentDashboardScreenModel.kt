package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.repository.MessageRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Domain models ─────────────────────────────────────────────────────────────

data class StudentSummary(
    val id: String,
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

// ── UiState ───────────────────────────────────────────────────────────────────

sealed interface ParentDashboardUiState {
    data object Loading : ParentDashboardUiState
    data class Success(
        val students: List<StudentSummary>,
        val selectedIndex: Int,
        val timeline: List<TimelineEvent>
    ) : ParentDashboardUiState
    data class Error(val message: String) : ParentDashboardUiState
}

// ── ScreenModel ───────────────────────────────────────────────────────────────

class ParentDashboardScreenModel(
    private val studentRepo: StudentRepository,
    private val attendanceRepo: AttendanceRepository,
    private val messageRepo: MessageRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow<ParentDashboardUiState>(ParentDashboardUiState.Loading)
    val uiState: StateFlow<ParentDashboardUiState> = _uiState
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5_000), ParentDashboardUiState.Loading)

    fun load(parentId: String) {
        // Obtiene todos los alumnos de la clase y filtra por parentId
        studentRepo.getStudentsByClass("clase-demo")
            .onEach { students ->
                val children = students.filter { it.parentId == parentId }
                if (children.isEmpty()) {
                    _uiState.value = ParentDashboardUiState.Error("No hay alumnos vinculados a este apoderado")
                    return@onEach
                }

                val summaries = children.map { student ->
                    // Calcula % asistencia real desde BD
                    var attendanceList = emptyList<com.tuapp.libreta.domain.model.Attendance>()
                    attendanceRepo.getByStudent(student.id).first().also { attendanceList = it }

                    val total   = attendanceList.size.coerceAtLeast(1)
                    val present = attendanceList.count { it.status == AttendanceStatus.PRESENT }
                    val percent = (present * 100 / total)

                    // Mensajes pendientes
                    var msgCount = 0
                    messageRepo.getByReceiver(parentId).first().also { msgCount = it.size }

                    StudentSummary(
                        id                = student.id,
                        name              = "${student.firstName} ${student.lastName}",
                        attendancePercent = percent,
                        lastNote          = "Sin anotaciones recientes",
                        pendingMessages   = msgCount
                    )
                }

                val timeline = buildTimeline(children.first().id)
                _uiState.value = ParentDashboardUiState.Success(
                    students      = summaries,
                    selectedIndex = 0,
                    timeline      = timeline
                )
            }
            .catch { e -> _uiState.value = ParentDashboardUiState.Error(e.message ?: "Error") }
            .launchIn(screenModelScope)
    }

    fun selectStudent(index: Int) {
        val current = _uiState.value as? ParentDashboardUiState.Success ?: return
        screenModelScope.launch {
            val timeline = buildTimeline(current.students[index].id)
            _uiState.value = current.copy(selectedIndex = index, timeline = timeline)
        }
    }

    private suspend fun buildTimeline(studentId: String): List<TimelineEvent> {
        val attendance = attendanceRepo.getByStudent(studentId).first()
        return attendance.takeLast(6).mapIndexed { i, record ->
            val isPresent = record.status == AttendanceStatus.PRESENT
            TimelineEvent(
                id       = "tl-$i",
                type     = if (isPresent) TimelineEventType.ATTENDANCE_PRESENT else TimelineEventType.ATTENDANCE_ABSENT,
                title    = "Asistencia",
                subtitle = if (isPresent) "Presente" else "Ausente",
                date     = "Día ${i + 1}"
            )
        }
    }
}
