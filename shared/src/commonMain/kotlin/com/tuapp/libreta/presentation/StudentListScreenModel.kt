package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
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
    data class ToggleAttendance(val id: String)  : StudentListEvent
    data class DeleteStudent(val id: String)     : StudentListEvent
}

class StudentListScreenModel(
    private val getStudents: GetStudentsByClassUseCase,
    private val deleteStudent: DeleteStudentUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow<StudentListUiState>(StudentListUiState.Loading)

    // stateIn evita que el Flow se reinicie en rotaciones o recomposiciones
    val uiState: StateFlow<StudentListUiState> = _uiState
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5_000), StudentListUiState.Loading)

    fun onEvent(event: StudentListEvent) {
        when (event) {
            is StudentListEvent.LoadClass        -> load(event.classId)
            is StudentListEvent.DeleteStudent    -> delete(event.id)
            is StudentListEvent.ToggleAttendance -> Unit
        }
    }

    private var loaded = false  // evita re-suscribir si ya está activo

    private fun load(classId: String) {
        if (loaded) return
        loaded = true
        getStudents(classId)
            .onEach { list ->
                _uiState.value = if (list.isEmpty()) StudentListUiState.Empty
                                 else StudentListUiState.Success(list)
            }
            .catch { e -> _uiState.value = StudentListUiState.Error(e.message ?: "Error") }
            .launchIn(screenModelScope)
    }

    private fun delete(id: String) {
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
}
