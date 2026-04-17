package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.domain.usecase.CourseAnalytics
import com.tuapp.libreta.domain.usecase.GetCourseAnalyticsUseCase
import kotlinx.coroutines.flow.*

sealed interface StatsUiState {
    data object Loading                            : StatsUiState
    data class  Success(val data: CourseAnalytics) : StatsUiState
    data class  Error(val message: String)         : StatsUiState
}

class StatsScreenModel(
    private val getAnalytics: GetCourseAnalyticsUseCase
) : ScreenModel {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)

    val uiState: StateFlow<StatsUiState> = _uiState
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState.Loading)

    private var loaded = false

    fun load(classId: String) {
        if (loaded) return
        loaded = true
        getAnalytics(classId)
            .onEach  { _uiState.value = StatsUiState.Success(it) }
            .catch   { e -> _uiState.value = StatsUiState.Error(e.message ?: "Error") }
            .launchIn(screenModelScope)
    }
}
