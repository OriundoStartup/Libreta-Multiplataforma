package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.usecase.AttendanceReport
import com.tuapp.libreta.domain.usecase.GetConsolidatedReportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReportUiState {
    data object Loading : ReportUiState
    data class Success(val data: AttendanceReport) : ReportUiState
    data class Error(val message: String) : ReportUiState
}

class ReportScreenModel(
    private val getReportUseCase: GetConsolidatedReportUseCase
) : ScreenModel {

    private val _state = MutableStateFlow<ReportUiState>(ReportUiState.Loading)
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    fun load(courseId: String) {
        screenModelScope.launch {
            _state.value = ReportUiState.Loading
            try {
                val report = getReportUseCase.execute(UuidString(courseId))
                _state.value = ReportUiState.Success(report)
            } catch (e: Exception) {
                _state.value = ReportUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}
