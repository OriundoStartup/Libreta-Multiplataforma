package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.CoursesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CourseEditUiState {
    data object Idle : CourseEditUiState()
    data object Saving : CourseEditUiState()
    data object Success : CourseEditUiState()
    data class Error(val message: String) : CourseEditUiState()
}

class CourseEditScreenModel(
    private val coursesRepository: CoursesRepository
) : ScreenModel {

    private val _state = MutableStateFlow<CourseEditUiState>(CourseEditUiState.Idle)
    val state: StateFlow<CourseEditUiState> = _state.asStateFlow()

    fun saveCourse(
        courseId: String,
        name: String,
        description: String?,
        subject: String?,
        grade: String?
    ) {
        screenModelScope.launch {
            _state.value = CourseEditUiState.Saving
            coursesRepository.updateCourse(
                courseId = courseId,
                name = name,
                description = description,
                subject = subject,
                grade = grade
            ).onSuccess {
                _state.value = CourseEditUiState.Success
            }.onFailure { e ->
                _state.value = CourseEditUiState.Error(e.message ?: "Error al guardar")
            }
        }
    }

    fun deleteCourse(courseId: String) {
        screenModelScope.launch {
            _state.value = CourseEditUiState.Saving // Reutilizamos el estado de carga
            coursesRepository.deleteCourse(courseId)
                .onSuccess { _state.value = CourseEditUiState.Success }
                .onFailure { e -> _state.value = CourseEditUiState.Error(e.message ?: "Error al borrar") }
        }
    }

    fun resetState() {
        _state.value = CourseEditUiState.Idle
    }
}
