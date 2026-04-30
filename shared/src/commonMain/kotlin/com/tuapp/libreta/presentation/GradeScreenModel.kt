package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Grade
import com.tuapp.libreta.domain.model.SubjectAverage
import com.tuapp.libreta.domain.usecase.DeleteGradeUseCase
import com.tuapp.libreta.domain.usecase.GetStudentGradesUseCase
import com.tuapp.libreta.domain.usecase.SaveGradeUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface GradeUiState {
    data object Idle    : GradeUiState
    data object Loading : GradeUiState
    data class Success(val averages: List<SubjectAverage>) : GradeUiState
    data class Error(val message: String) : GradeUiState
}

class GradeScreenModel(
    private val getGradesUseCase: GetStudentGradesUseCase,
    private val saveGradeUseCase: SaveGradeUseCase,
    private val deleteGradeUseCase: DeleteGradeUseCase,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope? = null
) : ScreenModel {

    private val scope = coroutineScope ?: screenModelScope

    private val _state = MutableStateFlow<GradeUiState>(GradeUiState.Idle)
    val state: StateFlow<GradeUiState> = _state.asStateFlow()

    fun load(studentId: String) {
        val uuid = UuidString(studentId)
        _state.value = GradeUiState.Loading
        getGradesUseCase(uuid)
            .onEach { _state.value = GradeUiState.Success(it) }
            .catch { _state.value = GradeUiState.Error(it.message ?: "Error al cargar notas") }
            .launchIn(scope)
    }

    fun addGrade(
        studentId: String,
        courseId: String,
        title: String,
        score: Double,
        subject: String,
        weight: Double = 1.0
    ) {
        scope.launch {
            val grade = Grade(
                studentId = UuidString(studentId),
                courseId = UuidString(courseId),
                title = title,
                score = score,
                subject = subject,
                weight = weight,
                date = com.tuapp.libreta.data.util.currentEpochMs()
            )
            val result = saveGradeUseCase(grade)
            if (result.isFailure) {
                _state.value = GradeUiState.Error(result.exceptionOrNull()?.message ?: "Error al guardar nota")
            }
        }
    }

    fun updateGrade(grade: Grade) {
        scope.launch {
            val result = saveGradeUseCase(grade)
            if (result.isFailure) {
                _state.value = GradeUiState.Error(result.exceptionOrNull()?.message ?: "Error al actualizar nota")
            }
        }
    }

    fun deleteGrade(id: UuidString) {
        scope.launch {
            deleteGradeUseCase(id)
        }
    }
}
