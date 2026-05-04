package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Course
import com.tuapp.libreta.domain.repository.ClassRoomRepository
import com.tuapp.libreta.domain.repository.JustificationRepository
import com.tuapp.libreta.domain.repository.StudentRepository
import com.tuapp.libreta.domain.usecase.GetCourseAnalyticsUseCase
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
    private val studentRepo: StudentRepository,
    private val analyticsUseCase: GetCourseAnalyticsUseCase,
    private val justificationRepo: JustificationRepository
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
            println("DEBUG Dashboard: Iniciando carga PARALELA para $courseId")
            _state.value = CourseDashboardUiState.Loading
            
            try {
                val classUuid = UuidString(courseId)
                
                // Lanzar todas las peticiones en paralelo
                val courseJob = async { 
                    withTimeoutOrNull(1500L) {
                        classRoomRepo.getAll().firstOrNull()?.find { it.id == courseId }
                    }
                }
                
                val analyticsJob = async { 
                    withTimeoutOrNull(4000L) { 
                        analyticsUseCase(classUuid).firstOrNull() 
                    } 
                }
                
                val studentsJob = async { 
                    withTimeoutOrNull(2000L) { 
                        studentRepo.getStudentsByClass(classUuid).firstOrNull() 
                    } 
                }

                // Esperar resultados
                val course = courseJob.await()
                val analytics = analyticsJob.await()
                val students = studentsJob.await()
                
                // Solo cargar justificaciones si tenemos el profesor
                val pendingJusts = if (course != null) {
                    withTimeoutOrNull(2000L) {
                        justificationRepo.getPendingByTeacher(UuidString(course.teacherId)).firstOrNull()
                    }
                } else null

                println("DEBUG Dashboard: Carga terminada. Curso encontrado: ${course != null}")
                _state.value = CourseDashboardUiState.Success(
                    course = course,
                    studentCount = students?.size ?: 0,
                    attendanceRate = analytics?.overallAttendancePercent ?: 0f,
                    pendingJustificationsCount = pendingJusts?.size ?: 0
                )
            } catch (e: Exception) {
                println("DEBUG Dashboard: ERROR - ${e.message}")
                _state.value = CourseDashboardUiState.Error("Error: ${e.message}")
            } finally {
                currentLoadingId = null
            }
        }
    }
}
