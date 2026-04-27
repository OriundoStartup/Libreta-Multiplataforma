package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.model.NoticeCategory
import com.tuapp.libreta.domain.repository.CommunicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

sealed interface NoticeListUiState {
    data object Loading : NoticeListUiState
    data class Success(val notices: List<Message>) : NoticeListUiState
    data class Error(val message: String) : NoticeListUiState
}

class NoticeListScreenModel(
    private val communicationRepo: CommunicationRepository
) : ScreenModel {

    private val _state = MutableStateFlow<NoticeListUiState>(NoticeListUiState.Loading)
    val state: StateFlow<NoticeListUiState> = _state.asStateFlow()

    private val _selectedCategory = MutableStateFlow<NoticeCategory?>(null)
    val selectedCategory: StateFlow<NoticeCategory?> = _selectedCategory.asStateFlow()

    private var allNotices: List<Message> = emptyList()

    fun loadNotices(classId: UuidString) {
        screenModelScope.launch {
            _state.value = NoticeListUiState.Loading
            communicationRepo.getByClass(classId)
                .onEach { notices ->
                    allNotices = notices
                    applyFilter()
                }
                .catch { e ->
                    _state.value = NoticeListUiState.Error(e.message ?: "Error al cargar avisos")
                }
                .collect {}
        }
    }

    fun setCategoryFilter(category: NoticeCategory?) {
        _selectedCategory.value = category
        applyFilter()
    }

    private fun applyFilter() {
        val filter = _selectedCategory.value
        val filtered = if (filter == null) allNotices
        else allNotices.filter { it.content.contains(filter.label) || it.content.contains(filter.emoji) }
        
        _state.value = NoticeListUiState.Success(filtered)
    }
}
