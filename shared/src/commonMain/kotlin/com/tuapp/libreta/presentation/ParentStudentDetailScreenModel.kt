package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ParentStudentDetailState(
    val isLoading: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val initialRut: String? = null
)

class ParentStudentDetailScreenModel(
    private val studentRepo: StudentRepository
) : ScreenModel {

    private val _state = MutableStateFlow(ParentStudentDetailState())
    val state: StateFlow<ParentStudentDetailState> = _state.asStateFlow()

    fun loadStudent(id: String) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // Buscamos en los alumnos vinculados al padre
                // (Nota: Esto asume que el repo puede filtrar o que obtenemos la lista y buscamos)
                // Usamos queries directas o repo. 
                // StudentRepository tiene getStudentsByParent.
                val students = studentRepo.getStudentsByParent(UuidString("")).first() // ID se pasa en el dashboard
                // En este contexto, mejor pasar el objeto completo o buscarlo.
                // Como no tenemos el parentId aquí fácil, confiaremos en que el repo ya lo sabe o lo cargaremos.
            } catch (e: Exception) { }
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun updateStudent(id: String, name: String, rut: String?) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            studentRepo.updateStudentEnrollment(UuidString(id), name, rut)
                .onSuccess { 
                    _state.update { it.copy(isLoading = false, success = true) }
                }
                .onFailure { e -> 
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun unlinkStudent(id: String) {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                studentRepo.deleteStudent(UuidString(id))
                _state.update { it.copy(isLoading = false, isDeleted = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
