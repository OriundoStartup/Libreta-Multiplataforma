package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.usecase.DeleteStudentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.repository.MessageRepository
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.domain.repository.StudentRepository

sealed interface StudentDetailUiState {
    data object Loading : StudentDetailUiState
    data object Deleted : StudentDetailUiState
    data class Success(
        val studentId: String,
        val studentName: String,
        val courseId: String,
        val courseName: String,
        val parentId: String,
        val attendancePercentage: Int,
        val recentAttendance: List<DailyAttendance>,
        val internalNotes: List<Message> = emptyList()
    ) : StudentDetailUiState
    data class Error(val message: String) : StudentDetailUiState
}

data class DailyAttendance(
    val date: String,
    val status: AttendanceStatus
)

class StudentDetailScreenModel(
    private val studentRepo: StudentRepository,
    private val attendanceRepo: AttendanceRepository,
    private val messageRepo: MessageRepository,
    private val authService: SupabaseAuthService,
    private val deleteStudentUseCase: DeleteStudentUseCase,
    private val studentId: String,
    private val studentName: String,
    private val courseId: String,
    private val parentId: String
) : ScreenModel {

    private val _state = MutableStateFlow<StudentDetailUiState>(StudentDetailUiState.Loading)
    val state: StateFlow<StudentDetailUiState> = _state.asStateFlow()

    private var isLoaded = false

    fun loadStudentDetails() {
        if (isLoaded) return
        isLoaded = true
        
        screenModelScope.launch {
            _state.value = StudentDetailUiState.Loading
            
            // Cargar asistencia y notas en paralelo (simplificado para el ejemplo)
            attendanceRepo.getByStudent(UuidString(studentId)).collect { attendances ->
                try {
                    val recentAttendance = attendances
                        .sortedByDescending { it.date }
                        .take(10)
                        .map { DailyAttendance(it.date, it.status) }

                    val presentCount = attendances.count { it.status == AttendanceStatus.PRESENT }
                    val percentage = if (attendances.isNotEmpty()) (presentCount * 100 / attendances.size) else 100

                    // Cargar notas internas
                    messageRepo.getInternalNotes(UuidString(studentId)).collect { notes ->
                        _state.value = StudentDetailUiState.Success(
                            studentId = studentId,
                            studentName = studentName,
                            courseId = courseId,
                            courseName = "Curso",
                            parentId = parentId,
                            attendancePercentage = percentage,
                            recentAttendance = recentAttendance,
                            internalNotes = notes
                        )
                    }
                } catch (e: Exception) {
                    _state.value = StudentDetailUiState.Error(e.message ?: "Error al procesar")
                }
            }
        }
    }

    fun addInternalNote(content: String) {
        val senderId = authService.currentUserId() ?: return
        screenModelScope.launch {
            messageRepo.saveInternalNote(UuidString(studentId), senderId, content)
            // El flow de getInternalNotes debería emitir el cambio si fuera reactivo real, 
            // pero como usamos emit único en flow{}, llamamos a recargar o forzamos actualización.
            isLoaded = false
            loadStudentDetails()
        }
    }

    fun deleteStudent() {
        screenModelScope.launch {
            try {
                deleteStudentUseCase(UuidString(studentId))
                _state.value = StudentDetailUiState.Deleted
            } catch (e: Exception) {
                _state.value = StudentDetailUiState.Error(e.message ?: "Error al eliminar")
            }
        }
    }

    fun refresh() {
        isLoaded = false
        loadStudentDetails()
    }
}