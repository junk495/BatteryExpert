package com.batteryexpert.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.data.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AiResearchState {
    data object Idle : AiResearchState()
    data object Loading : AiResearchState()
    data class Success(val cellType: CellTypeEntity) : AiResearchState()
    data class Error(val message: String) : AiResearchState()
}

class AiResearchViewModel(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow<AiResearchState>(AiResearchState.Idle)
    val state: StateFlow<AiResearchState> = _state.asStateFlow()

    fun setQuery(text: String) {
        _query.value = text
    }

    fun research() {
        val currentQuery = _query.value.trim()
        if (currentQuery.isBlank()) return

        _state.value = AiResearchState.Loading

        viewModelScope.launch {
            val result = aiRepository.researchCellType(currentQuery)
            result.fold(
                onSuccess = { cellType ->
                    _state.value = AiResearchState.Success(cellType)
                },
                onFailure = { error ->
                    _state.value = AiResearchState.Error(error.localizedMessage ?: "Unbekannter Fehler")
                }
            )
        }
    }

    fun resetState() {
        _state.value = AiResearchState.Idle
    }
}
