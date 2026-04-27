package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.CoursesRepository
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.DataSeeder
import com.tuapp.libreta.domain.model.Course
import com.tuapp.libreta.domain.repository.CourseAssignmentRepository
import com.tuapp.libreta.data.util.UuidString
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherProfile(val name: String, val avatarUrl: String?)

sealed interface TeacherDashboardUiState {
    data object Loading : TeacherDashboardUiState
    data class  Success(val profile: TeacherProfile, val courses: List<Course>) : TeacherDashboardUiState
    data class  Error(val message: String) : TeacherDashboardUiState
}

class TeacherDashboardScreenModel(
    private val authService: SupabaseAuthService,
    private val coursesRepo: CoursesRepository,
    private val assignmentRepo: CourseAssignmentRepository,
    private val dataSeeder: DataSeeder,
    private val supabase: SupabaseClient
) : ScreenModel {

    private val _state = MutableStateFlow<TeacherDashboardUiState>(TeacherDashboardUiState.Loading)
    val state: StateFlow<TeacherDashboardUiState> = _state.asStateFlow()

    private val _generatedCode = MutableStateFlow<String?>(null)
    val generatedCode: StateFlow<String?> = _generatedCode.asStateFlow()

    private val _colleagueCode = MutableStateFlow<String?>(null)
    val colleagueCode: StateFlow<String?> = _colleagueCode.asStateFlow()

    init { load() }

    fun load() {
        screenModelScope.launch {
            _state.value = TeacherDashboardUiState.Loading
            runCatching {
                val user = authService.currentUser() ?: error("No autenticado")
                val coursesResult = coursesRepo.getTeacherCourses()
                val courses = coursesResult.getOrThrow()
                _state.value = TeacherDashboardUiState.Success(user.toTeacherProfile(), courses)
            }.onFailure { e ->
                _state.value = TeacherDashboardUiState.Error(e.message ?: "Error al cargar cursos")
            }
        }
    }

    fun generateInviteCodeForCourse(course: Course) {
        _generatedCode.value = course.inviteCode
    }

    fun generateColleagueInvite(course: Course) {
        screenModelScope.launch {
            val user = authService.currentUser() ?: return@launch
            val code = assignmentRepo.generateColleagueInvite(
                courseId = UuidString(course.id),
                schoolId = UuidString("00000000-0000-0000-0000-000000000000"), // Default
                issuedByTeacherId = UuidString(user.id)
            )
            _colleagueCode.value = code
        }
    }

    fun joinCourse(code: String) {
        screenModelScope.launch {
            _state.value = TeacherDashboardUiState.Loading
            val user = authService.currentUser() ?: error("No autenticado")
            assignmentRepo.assignByCode(code, UuidString(user.id))
                .onSuccess { load() }
                .onFailure { e -> 
                    _state.value = TeacherDashboardUiState.Error("Error al unirse: ${e.message}")
                    load()
                }
        }
    }

    fun clearGeneratedCode() { 
        _generatedCode.value = null
        _colleagueCode.value = null
    }

    fun createCourse(name: String, grade: String, schoolName: String) {
        screenModelScope.launch {
            _state.value = TeacherDashboardUiState.Loading
            val fullName = "${grade.trim()} ${name.trim()}"
            
            coursesRepo.createCourse(
                name        = fullName,
                description = "Curso creado por el profesor",
                subject     = null,
                grade       = grade,
                schoolName  = schoolName
            ).onSuccess { newCourse ->
                _generatedCode.value = newCourse.inviteCode
                load()
            }.onFailure { e ->
                _state.value = TeacherDashboardUiState.Error("Error al crear curso: ${e.message}")
            }
        }
    }

    fun logout() {
        screenModelScope.launch {
            authService.signOut()
        }
    }
}

private fun UserInfo.toTeacherProfile(): TeacherProfile {
    fun key(k: String) = userMetadata?.get(k)
        ?.let { runCatching { (it as kotlinx.serialization.json.JsonPrimitive).content }.getOrNull() }
    return TeacherProfile(
        name      = key("full_name") ?: key("name") ?: email ?: "Profesor",
        avatarUrl = key("avatar_url")
    )
}
