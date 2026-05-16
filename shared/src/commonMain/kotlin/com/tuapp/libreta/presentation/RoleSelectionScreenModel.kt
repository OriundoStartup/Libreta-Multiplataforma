package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.CoursesRepository
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.UserRole
import com.tuapp.libreta.domain.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.firstOrNull

sealed interface RoleSelectionUiState {
    data object Idle    : RoleSelectionUiState
    data object Loading : RoleSelectionUiState
    data class  Success(val role: UserRole, val userId: UuidString) : RoleSelectionUiState
    data class  Error(val message: String)  : RoleSelectionUiState
    data class  ProfileStatus(
        val hasParentRole: Boolean, 
        val hasTeacherRole: Boolean,
        val hasStudents: Boolean,
        val userEmail: String = ""
    ) : RoleSelectionUiState
}

class RoleSelectionScreenModel(
    private val authService: SupabaseAuthService,
    private val coursesRepository: CoursesRepository,
    private val studentRepository: StudentRepository
) : ScreenModel {

    private val _state = MutableStateFlow<RoleSelectionUiState>(RoleSelectionUiState.Idle)
    val state: StateFlow<RoleSelectionUiState> = _state.asStateFlow()

    fun checkExistingProfile(forceShowSelection: Boolean = false) {
        screenModelScope.launch {
            val user = authService.currentUser() ?: return@launch
            val uid = UuidString(user.id)
            val role = authService.getUserRole(user.id)
            
            if (role != null && !forceShowSelection) {
                // Si ya tiene un rol y no estamos forzando la selección, saltamos al dashboard
                _state.value = RoleSelectionUiState.Success(role, uid)
                return@launch
            }

            // Si llegamos aquí, mostramos la selección de roles
            _state.value = RoleSelectionUiState.Loading
            
            // Verificar si tiene alumnos (apoderado real)
            val students = studentRepository.getStudentsByParent(uid).firstOrNull() ?: emptyList()
            
            // Verificar si tiene cursos asignados (profesor real)
            // Nota: Podríamos usar un repository de asignaciones, pero por ahora 
            // asumimos que si el rol actual es TEACHER, ya es profesor.
            // Para ser más estrictos, podríamos buscar en la tabla de asignaciones.
            
            _state.value = RoleSelectionUiState.ProfileStatus(
                hasParentRole = role == UserRole.PARENT || students.isNotEmpty(),
                hasTeacherRole = role == UserRole.TEACHER,
                hasStudents = students.isNotEmpty(),
                userEmail = user.email ?: ""
            )
        }
    }

    fun confirmRole(role: UserRole, code: String = "") {
        screenModelScope.launch {
            _state.value = RoleSelectionUiState.Loading
            
            val uid = authService.currentUserId() ?: run {
                _state.value = RoleSelectionUiState.Error("Sesión expirada. Por favor, reingresa.")
                return@launch
            }

            try {
                withTimeout(10_000) {
                    when (role) {
                        UserRole.TEACHER -> {
                            // Validar si ya es profesor o si tiene un código de invitación para profesores
                            val currentRole = authService.getUserRole(uid.value)
                            if (currentRole != UserRole.TEACHER && code.isBlank()) {
                                throw Exception("Para registrarte como profesor debes ingresar un código de autorización.")
                            }
                            
                            if (code.isNotBlank()) {
                                // Si proporciona un código, intentamos asignarlo
                                // Asumimos que existe un método en assignmentRepo para esto
                                // Por ahora solo actualizamos el rol si el código es "válido" (lógica a definir)
                                if (code.length < 4) throw Exception("Código de profesor inválido.")
                            }
                            
                            authService.updateRole(role)
                        }
                        UserRole.PARENT -> {
                            if (code.isNotBlank()) {
                                val course = coursesRepository.getCourseByInviteCode(code).getOrNull()
                                    ?: throw Exception("Código de invitación inválido.")
                                authService.updateRole(role, course.id)
                            } else {
                                // ¿Tiene alumnos ya?
                                val students = studentRepository.getStudentsByParent(uid).firstOrNull()
                                if (students.isNullOrEmpty()) {
                                    throw Exception("Para entrar como apoderado por primera vez debes ingresar el código del curso.")
                                }
                                authService.updateRole(role)
                            }
                        }
                    }
                    _state.value = RoleSelectionUiState.Success(role, uid)
                }
            } catch (e: Exception) {
                _state.value = RoleSelectionUiState.Error(e.message ?: "Error de validación")
                // Refrescar estado para que el usuario vea su email y opciones de nuevo
                checkExistingProfile(forceShowSelection = true)
            }
        }
    }

    fun signOut() {
        screenModelScope.launch {
            _state.value = RoleSelectionUiState.Loading
            authService.signOut()
        }
    }
}
