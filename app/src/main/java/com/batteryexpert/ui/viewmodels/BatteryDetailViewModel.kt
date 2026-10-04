package com.batteryexpert.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batteryexpert.data.db.BatteryEntity
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.data.db.ChargeProfileEntity
import com.batteryexpert.data.db.MeasurementEntity
import com.batteryexpert.data.repository.BatteryRepository
import com.batteryexpert.data.repository.MeasurementRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BatteryDetailViewModel(
    private val batteryRepository: BatteryRepository,
    private val measurementRepository: MeasurementRepository
) : ViewModel() {

    private val _selectedCellTypeId = MutableStateFlow<Long?>(null)
    val selectedCellTypeId: StateFlow<Long?> = _selectedCellTypeId.asStateFlow()

    fun selectCellType(id: Long) {
        _selectedCellTypeId.value = id
    }

    val cellType: StateFlow<CellTypeEntity?> = _selectedCellTypeId.flatMapLatest { id ->
        if (id != null) batteryRepository.getCellTypeById(id) else flowOf(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val batteries: StateFlow<List<BatteryEntity>> = _selectedCellTypeId.flatMapLatest { id ->
        if (id != null) batteryRepository.getBatteriesForType(id) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val profiles: StateFlow<List<ChargeProfileEntity>> = _selectedCellTypeId.flatMapLatest { id ->
        if (id != null) batteryRepository.getProfilesForBattery(id) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val measurements: StateFlow<List<MeasurementEntity>> = _selectedCellTypeId.flatMapLatest { id ->
        if (id != null) measurementRepository.getMeasurementsForBattery(id) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun insertBattery(battery: BatteryEntity) {
        viewModelScope.launch {
            batteryRepository.insertBattery(battery)
        }
    }

    fun deleteBattery(battery: BatteryEntity) {
        viewModelScope.launch {
            batteryRepository.deleteBattery(battery)
        }
    }

    fun insertProfile(profile: ChargeProfileEntity) {
        viewModelScope.launch {
            batteryRepository.insertProfile(profile)
        }
    }

    fun updateProfile(profile: ChargeProfileEntity) {
        viewModelScope.launch {
            batteryRepository.updateProfile(profile)
        }
    }

    fun deleteProfile(profile: ChargeProfileEntity) {
        viewModelScope.launch {
            batteryRepository.deleteProfile(profile)
        }
    }

    fun insertMeasurements(measurements: List<MeasurementEntity>) {
        viewModelScope.launch {
            measurementRepository.insertMeasurements(measurements)
        }
    }

    fun deleteCellType(cellType: CellTypeEntity) {
        viewModelScope.launch {
            batteryRepository.deleteCellType(cellType)
        }
    }
}
