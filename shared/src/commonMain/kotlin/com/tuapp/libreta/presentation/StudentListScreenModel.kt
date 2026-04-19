package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.epochMsToIso
import com.tuapp.libreta.data.util.random
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.usecase.DeleteStudentUseCase
import com.tuapp.libreta.domain.usecase.GetStudentsByClassUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface StudentListUiState {
    data object Loading                              : StudentListUiState
    data object Empty                                : StudentListUiState
    data class  Success(val students: List<Student>) : StudentListUiState
    data class  Error(val message: String)           : StudentListUiState
}

sealed interface StudentListEvent {
    data class LoadClass(val classId: String)    : StudentListEvent
    data class ToggleAttendance(val id: UuidString)  : StudentListEvent
    data class DeleteStudent(val id: UuidString)     : StudentListEvent
}

class StudentListScreenModel(
    private val getStudents: GetStudentsByClassUseCase,
    private val deleteStudent: DeleteStudentUseCase,
    private val attendanceRepo: AttendanceRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow<StudentListUiState>(StudentListUiState.Loading)
    val uiState: StateFlow<StudentListUiState> = _uiState.asStateFlow()

    // Track which students are marked present today
    private val presentToday = mutableSetOf<UuidString>()

    fun onEvent(event: StudentListEvent) {
        when (event) {
            is StudentListEvent.LoadClass        -> load(event.classId)
            is StudentListEvent.DeleteStudent    -> delete(event.id)
            is StudentListEvent.ToggleAttendance -> toggleAttendance(event.id)
        }
    }

    private fun load(classId: String) {
        val classUuid = classId.toUuidOrNull() ?: run {
            _uiState.value = StudentListUiState.Error("ID de clase inválido")
            return
        }

        getStudents(classUuid)
            .onEach { list ->
                _uiState.value = if (list.isEmpty()) StudentListUiState.Empty
                                 else StudentListUiState.Success(list)
            }
            .catch { e -> _uiState.value = StudentListUiState.Error(e.message ?: "Error") }
            .launchIn(screenModelScope)
    }

    private fun toggleAttendance(id: UuidString) {
        val studentUuid = id.toUuidOrNull()

        val status = if (presentToday.contains(studentUuid)) {
            presentToday.remove(studentUuid)
            AttendanceStatus.ABSENT
        } else {
            presentToday.add(studentUuid)
            AttendanceStatus.PRESENT
        }
        screenModelScope.launch {
            runCatching {
                attendanceRepo.save(
                    Attendance(
                        id        = UuidString.random(),
                        studentId = studentUuid,
                        date      = epochMsToIso(currentEpochMs()).take(10),
                        status    = status
                    )
                )
            }
        }
    }

    private fun delete(id: UuidString) {
        val studentUuid = id.toUuidOrNull()
        
        val current = _uiState.value
        if (current is StudentListUiState.Success) {
            val updated = current.students.filter { it.id != studentUuid }
            _uiState.value = if (updated.isEmpty()) StudentListUiState.Empty
                             else StudentListUiState.Success(updated)
        }
        screenModelScope.launch {
            runCatching { deleteStudent(studentUuid) }
                .onFailure { e -> _uiState.value = StudentListUiState.Error(e.message ?: "Error") }
        }
    }
}
