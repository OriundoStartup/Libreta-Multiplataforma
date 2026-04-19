package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.epochMsToIso
import com.tuapp.libreta.domain.model.UserRole
import com.tuapp.libreta.domain.repository.CourseAssignmentRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val role: String
)

data class ProfileUiData(
    val id: UuidString,
    val fullName: String,
    val role: UserRole
)

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class  Success(
        val profile: ProfileUiData,
        val teacherCourses: List<TeacherCourseInfo> = emptyList(),
        val linkedStudent: LinkedStudentInfo? = null
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
    private val courseRepo: CourseAssignmentRepository,
    private val studentRepo: StudentRepository
) : ScreenModel {

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        screenModelScope.launch {
            _state.value = ProfileUiState.Loading
            runCatching {
                val uid = authService.currentUserId() ?: error("No autenticado")
                val profile = supabase.from("profiles")
                    .select { filter { eq("id", uid.value) } }
                    .decodeSingle<ProfileDto>()
                val role = if (profile.role == "PARENT") UserRole.PARENT else UserRole.TEACHER
                val uiData = ProfileUiData(
                    id       = uid,
                    fullName = profile.fullName,
                    role     = role
                )
                if (role == UserRole.TEACHER) {
                    val assignments = courseRepo.getByTeacher(uid).first()
                    val courses = assignments.map { a ->
                        val count = runCatching {
                            studentRepo.getStudentsByClass(a.courseId).first().size
                        }.getOrElse { 0 }
                        TeacherCourseInfo(courseId = a.courseId, studentCount = count)
                    }
                    _state.value = ProfileUiState.Success(uiData, teacherCourses = courses)
                } else {
                    val students = runCatching {
                        studentRepo.getStudentsByClass(uid).first()
                    }.getOrElse { emptyList() }
                    val linked = students.firstOrNull()?.let { s ->
                        LinkedStudentInfo(
                            studentName = s.fullName,
                            courseName  = s.courseId.value
                        )
                    }
                    _state.value = ProfileUiState.Success(uiData, linkedStudent = linked)
                }
            }.onFailure { e ->
                _state.value = ProfileUiState.Error(e.message ?: "Error al cargar perfil")
            }
        }
    }

    fun saveName(newFullName: String) {
        val current = (_state.value as? ProfileUiState.Success) ?: return
        screenModelScope.launch {
            runCatching {
                val uid = authService.currentUserId() ?: error("No autenticado")
                supabase.from("profiles").update({
                    set("full_name", newFullName.trim())
                }) { filter { eq("id", uid.value) } }
                _state.value = ProfileUiState.Success(
                    current.profile.copy(fullName = newFullName.trim()),
                    teacherCourses = current.teacherCourses,
                    linkedStudent  = current.linkedStudent
                )
                _state.value = ProfileUiState.Saved
            }.onFailure { e ->
                _state.value = ProfileUiState.Error(e.message ?: "Error al guardar")
            }
        }
    }

    fun generateCodeForCourse(courseId: String) {
        val uuid = courseId.toUuidOrNull() ?: return
        val current = (_state.value as? ProfileUiState.Success) ?: return
        val uid = authService.currentUserId() ?: return
        screenModelScope.launch {
            runCatching {
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
            }.onFailure { e ->
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
}
