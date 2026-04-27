package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.remote.CoursesRepository
import com.tuapp.libreta.data.remote.SupabaseAuthService
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.data.util.currentEpochMs
import com.tuapp.libreta.data.util.epochMsToIso
import com.tuapp.libreta.data.util.random
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.usecase.DeleteStudentUseCase
import com.tuapp.libreta.domain.usecase.GetStudentsByClassUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface StudentListUiState {
    data object Loading                              : StudentListUiState
    data object Empty                                : StudentListUiState
    data class  Success(
        val students: List<Student>,
        val searchQuery: String = ""
    ) : StudentListUiState {
        val filteredStudents: List<Student> = if (searchQuery.isBlank()) students
                                              else students.filter { it.fullName.contains(searchQuery, ignoreCase = true) }
    }
    data class  Error(val message: String)           : StudentListUiState
}

sealed interface StudentListEvent {
    data class LoadClass(val classId: String)    : StudentListEvent
    data class ToggleAttendance(val id: UuidString)  : StudentListEvent
    data class DeleteStudent(val id: UuidString)     : StudentListEvent
    data class Search(val query: String)         : StudentListEvent
    data class AddStudent(val name: String, val rut: String?) : StudentListEvent
}

class StudentListScreenModel(
    private val getStudents: GetStudentsByClassUseCase,
    private val deleteStudent: DeleteStudentUseCase,
    private val attendanceRepo: AttendanceRepository,
    private val coursesRepo: CoursesRepository,
    private val authService: SupabaseAuthService
) : ScreenModel {

    private val _uiState = MutableStateFlow<StudentListUiState>(StudentListUiState.Loading)
    val uiState: StateFlow<StudentListUiState> = _uiState.asStateFlow()

    private var currentClassId: String? = null
    private val presentToday = mutableSetOf<UuidString>()

    fun onEvent(event: StudentListEvent) {
        when (event) {
            is StudentListEvent.LoadClass        -> load(event.classId)
            is StudentListEvent.DeleteStudent    -> delete(event.id)
            is StudentListEvent.ToggleAttendance -> toggleAttendance(event.id)
            is StudentListEvent.Search          -> search(event.query)
            is StudentListEvent.AddStudent      -> addStudent(event.name, event.rut)
        }
    }

    private fun search(query: String) {
        val current = _uiState.value
        if (current is StudentListUiState.Success) {
            _uiState.value = current.copy(searchQuery = query)
        }
    }

    private fun load(classId: String) {
        if (currentClassId == classId && _uiState.value !is StudentListUiState.Loading) return
        currentClassId = classId

        val classUuid = classId.toUuidOrNull() ?: run {
            _uiState.value = StudentListUiState.Error("ID de clase inválido")
            return
        }

        screenModelScope.launch {
            _uiState.value = StudentListUiState.Loading
            println("DEBUG StudentList: Iniciando carga remota para $classId")
            
            try {
                // Ahora usamos el repositorio remoto directamente para evitar vacíos locales
                getStudents(classUuid)
                    .distinctUntilChanged()
                    .collect { list ->
                        println("DEBUG StudentList: Recibidos ${list.size} alumnos desde Supabase")
                        _uiState.value = if (list.isEmpty()) StudentListUiState.Empty 
                                         else StudentListUiState.Success(list)
                    }
            } catch (e: Exception) {
                println("ERROR StudentList: ${e.message}")
                _uiState.value = StudentListUiState.Error("Error al conectar con el servidor")
            }
        }
    }

    private fun addStudent(name: String, rut: String?) {
        val classId = currentClassId ?: return
        
        val current = _uiState.value
        if (current is StudentListUiState.Success) {
            val isDuplicate = current.students.any { 
                (rut != null && it.studentRut == rut) || 
                (it.fullName.equals(name, ignoreCase = true)) 
            }
            if (isDuplicate) {
                _uiState.value = StudentListUiState.Error("Este alumno ya está registrado")
                return
            }
        }

        screenModelScope.launch {
            coursesRepo.enrollStudent(classId, name, rut)
                .onSuccess {
                    println("DEBUG StudentList: Alumno registrado con éxito")
                    // La UI se actualizará vía el Flow de load()
                }
                .onFailure { e ->
                    println("ERROR StudentList: Fallo al registrar: ${e.message}")
                    _uiState.value = StudentListUiState.Error(e.message ?: "Error al registrar")
                }
        }
    }

    private fun toggleAttendance(id: UuidString) {
        val status = if (presentToday.contains(id)) {
            presentToday.remove(id)
            AttendanceStatus.ABSENT
        } else {
            presentToday.add(id)
            AttendanceStatus.PRESENT
        }
        screenModelScope.launch {
            runCatching {
                attendanceRepo.save(
                    Attendance(
                        id        = UuidString.random(),
                        studentId = id,
                        date      = epochMsToIso(currentEpochMs()).take(10),
                        status    = status
                    )
                )
            }
        }
    }

    private fun delete(id: UuidString) {
        screenModelScope.launch {
            runCatching { deleteStudent(id) }
                .onFailure { e -> _uiState.value = StudentListUiState.Error(e.message ?: "Error") }
        }
    }

    fun logout() {
        screenModelScope.launch { authService.signOut() }
    }
}
