package com.batteryexpert.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batteryexpert.data.db.BatteryEntity
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.data.repository.BatteryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortBy {
    BRAND,
    MODEL,
    CHEMISTRY
}

class BatteryListViewModel(
    private val batteryRepository: BatteryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortBy = MutableStateFlow(SortBy.BRAND)
    val sortBy: StateFlow<SortBy> = _sortBy

    val cellTypes: StateFlow<List<CellTypeEntity>> = combine(
        batteryRepository.getCellTypes(),
        _searchQuery,
        _sortBy
    ) { list, query, sort ->
        val filtered = if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.manufacturer.contains(query, ignoreCase = true) ||
                it.model.contains(query, ignoreCase = true) ||
                it.chemistry.contains(query, ignoreCase = true)
            }
        }

        when (sort) {
            SortBy.BRAND -> filtered.sortedBy { it.manufacturer.lowercase() }
            SortBy.MODEL -> filtered.sortedBy { it.model.lowercase() }
            SortBy.CHEMISTRY -> filtered.sortedBy { it.chemistry.lowercase() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortBy(sort: SortBy) {
        _sortBy.value = sort
    }

    fun insertCellType(cellType: CellTypeEntity) {
        viewModelScope.launch {
            batteryRepository.insertCellType(cellType)
        }
    }

    fun updateCellType(cellType: CellTypeEntity) {
        viewModelScope.launch {
            batteryRepository.updateCellType(cellType)
        }
    }

    fun deleteCellType(cellType: CellTypeEntity) {
        viewModelScope.launch {
            batteryRepository.deleteCellType(cellType)
        }
    }

    fun insertBattery(battery: BatteryEntity) {
        viewModelScope.launch {
            batteryRepository.insertBattery(battery)
        }
    }

    fun updateBattery(battery: BatteryEntity) {
        viewModelScope.launch {
            batteryRepository.updateBattery(battery)
        }
    }

    fun deleteBattery(battery: BatteryEntity) {
        viewModelScope.launch {
            batteryRepository.deleteBattery(battery)
        }
    }
}
