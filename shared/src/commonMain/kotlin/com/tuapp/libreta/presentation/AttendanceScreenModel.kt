package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val courseId: UuidString,
    private val courseName: String
) : ScreenModel {

    private val _state = MutableStateFlow<AttendanceUiState>(AttendanceUiState.Loading)
    val state: StateFlow<AttendanceUiState> = _state.asStateFlow()

    private val todayDate: String
        get() = epochMsToIso(currentEpochMs()).take(10)  // "YYYY-MM-DD" en UTC, igual que DataSeeder

    init {
        loadStudents()
    }

    private fun loadStudents() {
        screenModelScope.launch {
            _state.value = AttendanceUiState.Loading
            try {
                val students = studentRepo.getStudentsByClass(courseId).first()
                
                val attendanceStudents = students.map { student ->
                    val attendance = attendanceRepo.getByStudent(student.id)
                        .first()
                        .find { it.date == todayDate }
                    
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
                    date = todayDate
                )
            } catch (e: Exception) {
                _state.value = AttendanceUiState.Error(e.message ?: "Error al cargar")
            }
        }
    }

    fun markAttendance(studentId: UuidString, status: AttendanceStatus) {
        screenModelScope.launch {
            val currentState = _state.value
            if (currentState !is AttendanceUiState.Success) return@launch

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
                date = todayDate,
                status = status
            )
            attendanceRepo.save(attendance)
        }
    }

    fun refresh() {
        loadStudents()
    }
}