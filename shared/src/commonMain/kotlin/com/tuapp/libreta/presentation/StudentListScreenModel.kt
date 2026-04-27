package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.epochMsToIso
import com.tuapp.libreta.data.util.random
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.usecase.DeleteStudentUseCase
import com.tuapp.libreta.domain.usecase.GetStudentsByClassUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed interface StudentListUiState {
    data object Loading                              : StudentListUiState
    data object Empty                                : StudentListUiState
    data class  Success(
        val students: List<Student>,
        val searchQuery: String = ""
    ) : StudentListUiState {
        val filteredStudents: List<Student> = if (searchQuery.isBlank()) students
                                              else students.filter { it.fullName.contains(searchQuery, ignoreCase = true) }
    }
    data class  Error(val message: String)           : StudentListUiState
}

sealed interface StudentListEvent {
    data class LoadClass(val classId: String)    : StudentListEvent
    data class ToggleAttendance(val id: UuidString)  : StudentListEvent
    data class DeleteStudent(val id: UuidString)     : StudentListEvent
    data class Search(val query: String)         : StudentListEvent
}

class StudentListScreenModel(
    private val getStudents: GetStudentsByClassUseCase,
    private val deleteStudent: DeleteStudentUseCase,
    private val attendanceRepo: AttendanceRepository,
    private val authService: SupabaseAuthService
) : ScreenModel {

    private val _uiState = MutableStateFlow<StudentListUiState>(StudentListUiState.Loading)
    val uiState: StateFlow<StudentListUiState> = _uiState.asStateFlow()

    private var currentClassId: String? = null

    // Track which students are marked present today
    private val presentToday = mutableSetOf<UuidString>()

    fun onEvent(event: StudentListEvent) {
        when (event) {
            is StudentListEvent.LoadClass        -> load(event.classId)
            is StudentListEvent.DeleteStudent    -> delete(event.id)
            is StudentListEvent.ToggleAttendance -> toggleAttendance(event.id)
            is StudentListEvent.Search          -> search(event.query)
        }
    }

    private fun search(query: String) {
        val current = _uiState.value
        if (current is StudentListUiState.Success) {
            _uiState.value = current.copy(searchQuery = query)
        }
    }

    private fun load(classId: String) {
        if (currentClassId == classId) return
        currentClassId = classId

        val classUuid = classId.toUuidOrNull() ?: run {
            _uiState.value = StudentListUiState.Error("ID de clase inválido")
            return
        }

        getStudents(classUuid)
            .distinctUntilChanged()
            .onEach { list ->
                _uiState.value = if (list.isEmpty()) StudentListUiState.Empty
                                 else StudentListUiState.Success(list)
            }
            .catch { e -> _uiState.value = StudentListUiState.Error(e.message ?: "Error") }
            .launchIn(screenModelScope)
    }

    private fun toggleAttendance(id: UuidString) {
        val status = if (presentToday.contains(id)) {
            presentToday.remove(id)
            AttendanceStatus.ABSENT
        } else {
            presentToday.add(id)
            AttendanceStatus.PRESENT
        }
        screenModelScope.launch {
            runCatching {
                attendanceRepo.save(
                    Attendance(
                        id        = UuidString.random(),
                        studentId = id,
                        date      = epochMsToIso(currentEpochMs()).take(10),
                        status    = status
                    )
                )
            }
        }
    }

    private fun delete(id: UuidString) {
        val current = _uiState.value
        if (current is StudentListUiState.Success) {
            val updated = current.students.filter { it.id != id }
            _uiState.value = if (updated.isEmpty()) StudentListUiState.Empty
                             else StudentListUiState.Success(updated)
        }
        screenModelScope.launch {
            runCatching { deleteStudent(id) }
                .onFailure { e -> _uiState.value = StudentListUiState.Error(e.message ?: "Error") }
        }
    }

    fun logout() {
        screenModelScope.launch {
            authService.signOut()
        }
    }
}
