package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.AppLogger
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.epochMsToIso
import kotlinx.coroutines.launch

sealed interface AttendanceUiState {
    data object Loading : AttendanceUiState
    data class Success(
        val courseId: UuidString,
        val courseName: String,
        val students: List<AttendanceStudent>,
        val date: String
    ) : AttendanceUiState
    data class Error(val message: String) : AttendanceUiState
}

data class AttendanceStudent(
    val studentId: UuidString,
    val studentName: String,
    val status: AttendanceStatus,
    val hasJustification: Boolean = false
)

class AttendanceScreenModel(
    private val studentRepo: StudentRepository,
    private val attendanceRepo: AttendanceRepository,
    private val syncManager: com.tuapp.libreta.data.sync.SyncManager,
    private val courseId: UuidString,
    private val courseName: String
) : ScreenModel {

    private val _state = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Loading)
    val state: StateFlow<AttendanceUiState> = _state.asStateFlow()

    private val _selectedDate = MutableStateFlow(epochMsToIso(currentEpochMs()).take(10))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    init {
        loadStudents(_selectedDate.value)
    }

    private fun loadStudents(date: String) {
        screenModelScope.launch {
            _state.value = AttendanceUiState.Loading
            try {
                // Usamos firstOrNull para no bloquear el flujo si no hay internet/datos
                val students = studentRepo.getStudentsByClass(courseId).firstOrNull() ?: emptyList()
                
                val attendanceStudents = students.map { student ->
                    val attendance = attendanceRepo.getByStudent(student.id)
                        .firstOrNull()
                        ?.find { it.date == date }
                    
                    AttendanceStudent(
                        studentId = student.id,
                        studentName = student.fullName,
                        status = attendance?.status ?: AttendanceStatus.PRESENT,
                        hasJustification = attendance?.justificationId != null
                    )
                }

                _state.value = AttendanceUiState.Success(
                    courseId = courseId,
                    courseName = courseName,
                    students = attendanceStudents,
                    date = date
                )
            } catch (e: Exception) {
                _state.value = AttendanceUiState.Error(e.message ?: "Error al cargar asistencia")
            }
        }
    }

    fun changeDate(newDate: String) {
        if (_selectedDate.value == newDate) return
        _selectedDate.value = newDate
        loadStudents(newDate)
    }

    fun markAttendance(studentId: UuidString, status: AttendanceStatus) {
        screenModelScope.launch {
            val currentState = _state.value as? AttendanceUiState.Success ?: return@launch
            val currentDate = _selectedDate.value

            val updatedStudents = currentState.students.map { student ->
                if (student.studentId == studentId) {
                    student.copy(status = status)
                } else {
                    student
                }
            }

            _state.value = currentState.copy(students = updatedStudents)

            val attendance = Attendance(
                studentId = studentId,
                date = currentDate,
                status = status
            )
            attendanceRepo.save(attendance)
        }
    }

    fun saveAndSync(onComplete: () -> Unit) {
        screenModelScope.launch {
            _state.value = AttendanceUiState.Loading
            AppLogger.d("Attendance", "Iniciando sincronización de asistencia...")
            try {
                syncManager.syncAll()
                AppLogger.d("Attendance", "Sincronización completada con éxito.")
                onComplete()
            } catch (e: Exception) {
                AppLogger.e("Attendance", "Fallo al sincronizar tras guardar: ${e.message}")
                _state.value = AttendanceUiState.Error("Guardado localmente, pero falló la subida: ${e.message}")
            }
        }
    }

    fun refresh() {
        loadStudents(_selectedDate.value)
    }
}
