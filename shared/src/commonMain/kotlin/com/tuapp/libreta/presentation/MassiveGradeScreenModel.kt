package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Grade
import com.tuapp.libreta.domain.usecase.GetStudentsByClassUseCase
import com.tuapp.libreta.domain.usecase.SaveGradeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed interface MassiveGradeUiState {
    data object Loading : MassiveGradeUiState
    data class Success(
        val students: List<StudentGradeEntry>,
        val subject: String = "",
        val title: String = "",
        val weight: String = "1.0",
        val isSaving: Boolean = false
    ) : MassiveGradeUiState
    data class Error(val message: String) : MassiveGradeUiState
}

data class StudentGradeEntry(
    val studentId: UuidString,
    val studentName: String,
    val score: String = ""
)

class MassiveGradeScreenModel(
    private val getStudentsByClass: GetStudentsByClassUseCase,
    private val saveGradeUseCase: SaveGradeUseCase,
    private val courseId: UuidString
) : ScreenModel {

    private val _state = MutableStateFlow<MassiveGradeUiState>(MassiveGradeUiState.Loading)
    val state: StateFlow<MassiveGradeUiState> = _state.asStateFlow()

    init {
        loadStudents()
    }

    private fun loadStudents() {
        screenModelScope.launch {
            try {
                val students = getStudentsByClass(courseId).firstOrNull() ?: emptyList()
                _state.value = MassiveGradeUiState.Success(
                    students = students.map { StudentGradeEntry(it.id, it.fullName) }
                )
            } catch (e: Exception) {
                _state.value = MassiveGradeUiState.Error(e.message ?: "Error al cargar alumnos")
            }
        }
    }

    fun updateHeader(subject: String? = null, title: String? = null, weight: String? = null) {
        val current = _state.value as? MassiveGradeUiState.Success ?: return
        _state.value = current.copy(
            subject = subject ?: current.subject,
            title = title ?: current.title,
            weight = weight ?: current.weight
        )
    }

    fun updateStudentScore(studentId: UuidString, score: String) {
        val current = _state.value as? MassiveGradeUiState.Success ?: return
        val updated = current.students.map {
            if (it.studentId == studentId) it.copy(score = score) else it
        }
        _state.value = current.copy(students = updated)
    }

    fun saveAll() {
        val current = _state.value as? MassiveGradeUiState.Success ?: return
        if (current.subject.isBlank() || current.title.isBlank()) {
            _state.value = MassiveGradeUiState.Error("Falta asignatura o título de evaluación")
            return
        }

        screenModelScope.launch {
            _state.value = current.copy(isSaving = true)
            try {
                val weight = current.weight.toDoubleOrNull() ?: 1.0
                
                current.students.forEach { entry ->
                    val score = entry.score.replace(",", ".").toDoubleOrNull()
                    if (score != null) {
                        val grade = Grade(
                            studentId = entry.studentId,
                            courseId = courseId,
                            title = current.title,
                            score = score,
                            weight = weight,
                            subject = current.subject,
                            date = com.tuapp.libreta.data.util.currentEpochMs()
                        )
                        saveGradeUseCase(grade)
                    }
                }
                _state.value = MassiveGradeUiState.Success(students = current.students.map { it.copy(score = "") })
            } catch (e: Exception) {
                _state.value = MassiveGradeUiState.Error(e.message ?: "Error al guardar notas")
            }
        }
    }
}
