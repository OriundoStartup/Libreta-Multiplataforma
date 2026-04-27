package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AttendanceHistoryUiState {
    data object Loading : AttendanceHistoryUiState
    data class Success(
        val studentName: String,
        val records: List<AttendanceRecord>
    ) : AttendanceHistoryUiState
    data class Error(val message: String) : AttendanceHistoryUiState
}

data class AttendanceRecord(
    val id: String?,
    val date: String,
    val status: AttendanceStatus,
    val justification: String? = null
)

class AttendanceHistoryScreenModel(
    private val attendanceRepo: AttendanceRepository,
    private val studentId: String,
    private val studentName: String
) : ScreenModel {

    private val _state = MutableStateFlow<AttendanceHistoryUiState>(AttendanceHistoryUiState.Loading)
    val state: StateFlow<AttendanceHistoryUiState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        screenModelScope.launch {
            _state.value = AttendanceHistoryUiState.Loading
            try {
                attendanceRepo.getByStudent(UuidString(studentId))
                    .collect { attendances ->
                        val sortedRecords = attendances
                            .sortedByDescending { it.date }
                            .map { att ->
                                AttendanceRecord(
                                    id = att.id?.value,
                                    date = att.date,
                                    status = att.status,
                                    justification = null
                                )
                            }
                        
                        _state.value = AttendanceHistoryUiState.Success(
                            studentName = studentName,
                            records = sortedRecords
                        )
                    }
            } catch (e: Exception) {
                _state.value = AttendanceHistoryUiState.Error(e.message ?: "Error al cargar historial")
            }
        }
    }

    fun updateStatus(record: AttendanceRecord, newStatus: AttendanceStatus) {
        screenModelScope.launch {
            try {
                val updated = Attendance(
                    id = record.id?.let { UuidString(it) },
                    studentId = UuidString(studentId),
                    date = record.date,
                    status = newStatus
                )
                attendanceRepo.save(updated)
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    fun refresh() {
        loadHistory()
    }
}