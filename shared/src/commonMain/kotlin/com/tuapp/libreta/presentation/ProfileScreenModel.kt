package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.CoursesRepository
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.remote.dto.CourseSupabaseDto
import com.tuapp.libreta.data.remote.dto.ProfileSupabaseDto
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.epochMsToIso
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.model.UserRole
import com.tuapp.libreta.domain.repository.StudentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class ProfileUiData(
    val id: UuidString,
    val fullName: String,
    val email: String,
    val role: UserRole
)

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class  Success(
        val profile: ProfileUiData,
        val teacherCourses: List<TeacherCourseInfo> = emptyList(),
        val linkedStudents: List<LinkedStudentInfo> = emptyList()
    ) : ProfileUiState
    data class  Error(val message: String) : ProfileUiState
    data object Saved : ProfileUiState
}

data class TeacherCourseInfo(
    val courseId: UuidString,
    val studentCount: Int,
    val generatedCode: String? = null
)

data class LinkedStudentInfo(
    val studentName: String,
    val courseName: String
)

class ProfileScreenModel(
    private val authService: SupabaseAuthService,
    private val supabase: SupabaseClient,
    private val coursesRepo: CoursesRepository,
    private val studentRepo: StudentRepository
) : ScreenModel {

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        screenModelScope.launch {
            _state.value = ProfileUiState.Loading
            println("DEBUG Profile: Iniciando carga de perfil")
            try {
                val uid = authService.currentUserId() ?: error("No autenticado")
                
                // 1. Obtener perfil y email de la sesión
                val profileDto = supabase.from("profiles")
                    .select { filter { eq("id", uid.value) } }
                    .decodeSingle<ProfileSupabaseDto>()
                
                val userEmail = authService.currentUser()?.email ?: profileDto.email ?: "Sin email"
                val role = if (profileDto.role == "PARENT") UserRole.PARENT else UserRole.TEACHER
                
                val uiData = ProfileUiData(
                    id       = uid,
                    fullName = profileDto.fullName ?: "Sin nombre",
                    email    = userEmail,
                    role     = role
                )

                if (role == UserRole.TEACHER) {
                    val teacherCoursesResult = coursesRepo.getTeacherCourses()
                    val teacherCourses = teacherCoursesResult.getOrDefault(emptyList())
                    
                    val courses = teacherCourses.map { c ->
                        val count = try {
                            studentRepo.getStudentsByClass(UuidString(c.id)).first().size
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) { 0 }
                        TeacherCourseInfo(courseId = UuidString(c.id), studentCount = count)
                    }
                    _state.value = ProfileUiState.Success(uiData, teacherCourses = courses)
                } else {
                    // PRIORIDAD 1: Usar getStudentsByParent (enrollments table)
                    val students = studentRepo.getStudentsByParent(uid).first()
                    println("DEBUG profile: alumnos a mapear = ${students.size}")

                    // PRIORIDAD 2 y 3: Mapeo multi-hijo con nombres de curso reales
                    val linked = students.map { s ->
                        // Obtener nombre del curso desde la tabla courses
                        val courseName = try {
                            supabase.from("courses")
                                .select { filter { eq("id", s.courseId.value) } }
                                .decodeSingle<CourseSupabaseDto>()
                                .name
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            println("WARN Profile: No se pudo obtener nombre del curso ${s.courseId.value}: ${e.message}")
                            "Curso Desconocido"
                        }

                        LinkedStudentInfo(
                            studentName = s.fullName,
                            courseName  = courseName
                        )
                    }
                    _state.value = ProfileUiState.Success(uiData, linkedStudents = linked)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("ERROR Profile: ${e.message}")
                _state.value = ProfileUiState.Error(e.message ?: "Error al cargar perfil")
            }
        }
    }

    fun saveName(newFullName: String) {
        val current = (_state.value as? ProfileUiState.Success) ?: return
        screenModelScope.launch {
            try {
                val uid = authService.currentUserId() ?: error("No autenticado")
                supabase.from("profiles").update({
                    set("full_name", newFullName.trim())
                }) { filter { eq("id", uid.value) } }
                
                _state.value = ProfileUiState.Saved
                // Recargar para actualizar UI
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = ProfileUiState.Error(e.message ?: "Error al guardar")
            }
        }
    }

    fun generateCodeForCourse(courseId: String) {
        val uuid = courseId.toUuidOrNull() ?: return
        val current = (_state.value as? ProfileUiState.Success) ?: return
        val uid = authService.currentUserId() ?: return
        screenModelScope.launch {
            try {
                val code = (1..8).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString("")
                supabase.from("invitation_codes").insert(mapOf(
                    "code"       to code,
                    "student_id" to uuid.value,
                    "teacher_id" to uid.value,
                    "expires_at" to epochMsToIso(currentEpochMs() + 7 * 24 * 3600 * 1000L)
                ))
                val updatedCourses = current.teacherCourses.map {
                    if (it.courseId == uuid) it.copy(generatedCode = code) else it
                }
                _state.value = current.copy(teacherCourses = updatedCourses)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = ProfileUiState.Error(e.message ?: "Error al generar código")
            }
        }
    }

    fun clearCourseCode(courseId: String) {
        val uuid = courseId.toUuidOrNull() ?: return
        val current = (_state.value as? ProfileUiState.Success) ?: return
        _state.value = current.copy(
            teacherCourses = current.teacherCourses.map {
                if (it.courseId == uuid) it.copy(generatedCode = null) else it
            }
        )
    }

    fun signOut() {
        screenModelScope.launch { runCatching { authService.signOut() } }
    }

    fun deleteAccount() {
        screenModelScope.launch {
            try {
                val uid = authService.currentUserId() ?: return@launch
                // En un sistema real borraríamos de Supabase y luego Auth
                supabase.from("profiles").delete { filter { eq("id", uid.value) } }
                authService.signOut()
            } catch (e: Exception) {
                _state.value = ProfileUiState.Error("No se pudo eliminar la cuenta: ${e.message}")
            }
        }
    }
}
