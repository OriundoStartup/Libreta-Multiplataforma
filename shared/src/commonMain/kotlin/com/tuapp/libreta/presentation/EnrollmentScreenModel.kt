package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.CoursesRepository
import com.tuapp.libreta.domain.model.Course
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EnrollmentUiState {
    data object Loading : EnrollmentUiState
    data object Searching : EnrollmentUiState
    data object Enrolling : EnrollmentUiState
    data object Success : EnrollmentUiState
    data class CoursesFound(val courses: List<Course>) : EnrollmentUiState
    data class Error(val message: String) : EnrollmentUiState
}

class EnrollmentScreenModel(
    private val coursesRepo: CoursesRepository
) : ScreenModel {

    private val _state = MutableStateFlow<EnrollmentUiState>(EnrollmentUiState.Loading)
    val state: StateFlow<EnrollmentUiState> = _state.asStateFlow()

    fun searchCourse(inviteCode: String) {
        if (inviteCode.isBlank()) {
            _state.value = EnrollmentUiState.Error("Ingresa un código")
            return
        }

        screenModelScope.launch {
            _state.value = EnrollmentUiState.Searching
            try {
                val course = coursesRepo.getCourseByInviteCode(inviteCode.trim().uppercase())
                    .getOrNull()
                val courses = if (course != null) listOf(course) else emptyList()
                
                if (courses.isEmpty()) {
                    _state.value = EnrollmentUiState.Error("No se encontró ningún curso con ese código")
                } else {
                    _state.value = EnrollmentUiState.CoursesFound(courses)
                }
            } catch (e: Exception) {
                _state.value = EnrollmentUiState.Error(e.message ?: "Error al buscar")
            }
        }
    }

    fun enrollStudent(courseId: String, studentName: String, studentRut: String?) {
        screenModelScope.launch {
            _state.value = EnrollmentUiState.Enrolling
            try {
                coursesRepo.enrollStudent(
                    courseId = courseId,
                    studentName = studentName,
                    studentRut = studentRut
                ).getOrThrow()
                _state.value = EnrollmentUiState.Success
            } catch (e: Exception) {
                _state.value = EnrollmentUiState.Error(e.message ?: "Error al inscribir")
            }
        }
    }

    fun reset() {
        _state.value = EnrollmentUiState.Loading
    }
}