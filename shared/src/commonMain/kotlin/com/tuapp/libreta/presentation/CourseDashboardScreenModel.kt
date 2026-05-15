package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Course
import com.tuapp.libreta.domain.repository.ClassRoomRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import com.tuapp.libreta.domain.usecase.GetCourseAnalyticsUseCase
import com.tuapp.libreta.domain.usecase.GetGlobalStatsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface CourseDashboardUiState {
    data object Loading : CourseDashboardUiState
    data class Success(
        val course: Course?,
        val studentCount: Int = 0,
        val attendanceRate: Float = 0f,
        val pendingJustificationsCount: Int = 0
    ) : CourseDashboardUiState
    data class Error(val message: String) : CourseDashboardUiState
}

class CourseDashboardScreenModel(
    private val classRoomRepo: ClassRoomRepository,
    private val coursesRepo: com.tuapp.libreta.data.remote.CoursesRepository,
    private val studentRepo: StudentRepository,
    private val analyticsUseCase: GetCourseAnalyticsUseCase,
    private val globalStatsUseCase: GetGlobalStatsUseCase
) : ScreenModel {

    private val _state = MutableStateFlow<CourseDashboardUiState>(CourseDashboardUiState.Loading)
    val state: StateFlow<CourseDashboardUiState> = _state.asStateFlow()

    private var currentLoadingId: String? = null

    fun load(courseId: String) {
        // Evitar recargas si ya tenemos éxito para este curso
        val current = _state.value
        if (current is CourseDashboardUiState.Success && current.course?.id == courseId) return
        if (currentLoadingId == courseId) return
        
        currentLoadingId = courseId
        
        screenModelScope.launch {
            println("DEBUG Dashboard: Iniciando carga para $courseId")
            _state.value = CourseDashboardUiState.Loading
            
            try {
                val classUuid = UuidString(courseId)
                
                // 1. Obtener Metadatos del Curso (Preferir local, fallback a remoto)
                val course = withTimeoutOrNull(3000L) {
                    classRoomRepo.getAll().firstOrNull()?.find { it.id == courseId }
                } ?: withTimeoutOrNull(5000L) {
                    coursesRepo.getTeacherCourses().getOrNull()?.find { it.id == courseId }
                }

                // 2. Cargar el resto en paralelo
                val analyticsJob = async { 
                    withTimeoutOrNull(6000L) { 
                        analyticsUseCase(classUuid).firstOrNull() 
                    } 
                }
                
                val studentsJob = async { 
                    withTimeoutOrNull(5000L) { 
                        studentRepo.getStudentsByClass(classUuid).firstOrNull() 
                    } 
                }

                val analytics = analyticsJob.await()
                val students = studentsJob.await()
                
                // 3. Justificaciones (usar ID del profesor del curso o el actual)
                val teacherId = course?.teacherId ?: ""
                val stats = if (teacherId.isNotEmpty()) {
                    withTimeoutOrNull(4000L) {
                        globalStatsUseCase(UuidString(teacherId)).firstOrNull()
                    }
                } else null

                println("DEBUG Dashboard: Carga completada. Alumnos: ${students?.size ?: 0}")
                _state.value = CourseDashboardUiState.Success(
                    course = course,
                    studentCount = students?.size ?: 0,
                    attendanceRate = analytics?.overallAttendancePercent ?: 0f,
                    pendingJustificationsCount = stats?.pendingJustificationsCount ?: 0
                )
            } catch (e: Exception) {
                println("DEBUG Dashboard: ERROR CRÍTICO - ${e.message}")
                _state.value = CourseDashboardUiState.Error("No se pudo cargar la información: ${e.message}")
            } finally {
                currentLoadingId = null
            }
        }
    }
}
