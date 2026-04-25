package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.CoursesRepository
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface RoleSelectionUiState {
    data object Idle    : RoleSelectionUiState
    data object Loading : RoleSelectionUiState
    data class  Success(val role: UserRole, val userId: UuidString) : RoleSelectionUiState
    data class  Error(val message: String)  : RoleSelectionUiState
}

class RoleSelectionScreenModel(
    private val authService: SupabaseAuthService,
    private val coursesRepository: CoursesRepository
) : ScreenModel {

    private val _state = MutableStateFlow<RoleSelectionUiState>(RoleSelectionUiState.Idle)
    val state: StateFlow<RoleSelectionUiState> = _state.asStateFlow()

    fun confirmRole(role: UserRole, code: String = "") {
        screenModelScope.launch {
            _state.value = RoleSelectionUiState.Loading
            runCatching {
                withTimeout(10_000) {
                    when (role) {
                        UserRole.PARENT -> {
                            // 1. Verificar que el curso existe y está activo
                            val course = coursesRepository.getCourseByInviteCode(code).getOrNull()
                            if (course == null) {
                                error("Código inválido. Verifica con el profesor del curso.")
                            }
                            
                            // 2. Guardar rol y curso en el perfil del apoderado
                            authService.updateRole(role, course.id)
                            
                            // 3. El registro de alumnos específicos (Enrollment) 
                            // se hará en la siguiente pantalla del flujo.
                            course
                        }
                        UserRole.TEACHER -> {
                            authService.updateRole(role)
                            null
                        }
                    }
                }
            }.onSuccess { courseOrNull ->
                val uid = authService.currentUserId()
                if (uid == null) {
                    _state.value = RoleSelectionUiState.Error("Error de identidad: Sesión no encontrada.")
                } else {
                    _state.value = RoleSelectionUiState.Success(role, uid)
                }
            }.onFailure { e ->
                _state.value = RoleSelectionUiState.Error(
                    when {
                        e.message?.contains("timeout", ignoreCase = true) == true ->
                            "Sin respuesta del servidor. Verifica tu conexión."
                        else -> e.message ?: "Error al procesar el registro"
                    }
                )
            }
        }
    }
}
