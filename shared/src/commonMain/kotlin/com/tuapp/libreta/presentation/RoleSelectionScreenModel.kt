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

    init {
        checkExistingProfile()
    }

    private fun checkExistingProfile() {
        screenModelScope.launch {
            val user = authService.currentUser() ?: return@launch
            val uid = UuidString(user.id)
            val role = authService.getUserRole(user.id)
            val students = studentRepository.getStudentsByParent(uid).firstOrNull() ?: emptyList()
            
            _state.value = RoleSelectionUiState.ProfileStatus(
                hasParentRole = role == UserRole.PARENT,
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
                            // VALIDACIÓN: ¿Tiene cursos creados?
                            val teacherCourses = coursesRepository.getTeacherCourses().getOrNull() ?: emptyList()
                            if (teacherCourses.isEmpty()) {
                                // Si no tiene cursos, no lo dejamos entrar como profesor por error
                                throw Exception("No tienes un perfil de profesor configurado. Por favor, entra como Apoderado o contacta a soporte.")
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
                checkExistingProfile()
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
