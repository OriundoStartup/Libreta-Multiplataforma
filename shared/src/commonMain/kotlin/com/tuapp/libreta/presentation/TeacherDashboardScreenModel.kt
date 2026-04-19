package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.remote.SupabaseCoursesRepository
import com.tuapp.libreta.data.remote.dto.CourseDto
import com.tuapp.libreta.data.util.DataSeeder
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.model.Course
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
    private val coursesRepo: SupabaseCoursesRepository,
    private val dataSeeder: DataSeeder,
    private val supabase: SupabaseClient
) : ScreenModel {

    private val _state = MutableStateFlow<TeacherDashboardUiState>(TeacherDashboardUiState.Loading)
    val state: StateFlow<TeacherDashboardUiState> = _state.asStateFlow()

    private val _generatedCode = MutableStateFlow<String?>(null)
    val generatedCode: StateFlow<String?> = _generatedCode.asStateFlow()

    init { load() }

    fun load() {
        screenModelScope.launch {
            _state.value = TeacherDashboardUiState.Loading
            runCatching {
                val user = authService.currentUser() ?: error("No autenticado")
                // Intentamos cargar los cursos reales desde el nuevo repositorio
                val coursesResult = coursesRepo.getTeacherCourses()
                val courses = coursesResult.getOrThrow()
                
                _state.value = TeacherDashboardUiState.Success(user.toTeacherProfile(), courses)
            }.onFailure { e ->
                _state.value = TeacherDashboardUiState.Error(e.message ?: "Error al cargar cursos")
            }
        }
    }

    fun generateInviteCodeForCourse(course: Course) {
        // En el nuevo sistema, el código ya viene en el curso
        _generatedCode.value = course.inviteCode
    }

    fun clearGeneratedCode() { _generatedCode.value = null }

    fun createCourse(name: String, grade: String, schoolName: String) {
        println("DEBUG: createCourse() llamado con name=$name grade=$grade school=$schoolName")
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
}

private fun UserInfo.toTeacherProfile(): TeacherProfile {
    fun key(k: String) = userMetadata?.get(k)
        ?.let { runCatching { (it as kotlinx.serialization.json.JsonPrimitive).content }.getOrNull() }
    return TeacherProfile(
        name      = key("full_name") ?: key("name") ?: email ?: "Profesor",
        avatarUrl = key("avatar_url")
    )
}
