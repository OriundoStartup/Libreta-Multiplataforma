package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.toUuidOrNull
import com.tuapp.libreta.domain.usecase.CourseAnalytics
import com.tuapp.libreta.domain.usecase.GetCourseAnalyticsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

sealed interface StatsUiState {
    data object Loading                            : StatsUiState
    data class  Success(val data: CourseAnalytics) : StatsUiState
    data class  Error(val message: String)         : StatsUiState
}

class StatsScreenModel(
    private val getAnalytics: GetCourseAnalyticsUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private var loaded = false

    fun load(classId: String) {
        if (loaded) return
        loaded = true

        val classUuid = classId.toUuidOrNull() ?: run {
            _uiState.value = StatsUiState.Error("ID de clase inválido")
            return
        }

        getAnalytics(classUuid)
            .onEach  { _uiState.value = StatsUiState.Success(it) }
            .catch   { e -> _uiState.value = StatsUiState.Error(e.message ?: "Error") }
            .launchIn(screenModelScope)
    }
}
