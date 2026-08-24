package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.CoursesRepository
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.AppLogger
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

data class ParentProfile(val name: String)

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
    val successMessage: String? = null
)

sealed interface ParentDashboardUiState {
    data object Loading : ParentDashboardUiState
    data object NoStudents : ParentDashboardUiState
    data class Success(
        val profile: ParentProfile,
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
    private val authService: SupabaseAuthService,
    private val syncManager: com.tuapp.libreta.data.sync.SyncManager
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
            AppLogger.e("ParentDashboard", "No hay sesión activa al llamar a load()")
            _state.update { it.copy(uiState = ParentDashboardUiState.Error("No hay sesión activa")) }
            return
        }

        AppLogger.d("ParentDashboard", "Iniciando carga de dashboard para UID: ${userId.value}")

        // DISPARAR PULL: Traer datos nuevos antes de mostrar la lista local
        screenModelScope.launch { 
            AppLogger.d("ParentDashboard", "Disparando syncManager.pullAll() desde load()")
            syncManager.pullAll() 
        }

        loadJob = studentRepo.getStudentsByParent(userId)
            .onEach { students ->
                AppLogger.d("ParentDashboard", "Repositorio devolvió ${students.size} alumnos para mapear con UID: ${userId.value}")
                if (students.isEmpty()) {
                    _state.update { it.copy(uiState = ParentDashboardUiState.NoStudents) }
                    return@onEach
                }

                try {
                    val user = authService.currentUser()
                    AppLogger.d("ParentDashboard", "Loaded students: ${students.size}. Current User: ${user?.id}")
                    val parentName = user?.userMetadata?.get("full_name")
                        ?.let { runCatching { (it as kotlinx.serialization.json.JsonPrimitive).content }.getOrNull() }
                        ?: user?.email ?: "Apoderado"
                    
                    val parentProfile = ParentProfile(parentName)

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
                            profile = parentProfile,
                            students = initialSummaries,
                            selectedIndex = 0,
                            timeline = emptyList()
                        )
                    )}

                    // 2. Cargar detalles (asistencia, inbox) en segundo plano
                    enrichStudentData(students, userId, parentProfile)

                } catch (e: Exception) {
                    AppLogger.e("ParentDashboard", "Error processing data: ${e.message}", e)
                    _state.update { it.copy(uiState = ParentDashboardUiState.Error(e.message ?: "Error al procesar datos")) }
                }
            }
            .catch { e ->
                if (e !is CancellationException) {
                    AppLogger.e("ParentDashboard", "Flow catch error: ${e.message}", e)
                    _state.update { it.copy(uiState = ParentDashboardUiState.Error(e.message ?: "Error de red")) }
                }
            }
            .launchIn(screenModelScope)
    }

    private fun enrichStudentData(students: List<Student>, userId: UuidString, parentProfile: ParentProfile) {
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
                        profile = parentProfile,
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

    fun clearSuccessMessage() {
        _state.update { it.copy(successMessage = null) }
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
