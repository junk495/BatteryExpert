package com.batteryexpert.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batteryexpert.data.ApiKeyStore
import com.batteryexpert.data.ApiKeyStore.Provider
import com.batteryexpert.data.ble.BleDevice
import com.batteryexpert.data.ble.ConnectionState
import com.batteryexpert.data.repository.BleRepository
import com.batteryexpert.data.repository.ExportImportRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ExportImportState {
    data object Idle : ExportImportState()
    data object Exporting : ExportImportState()
    data class ExportSuccess(val json: String) : ExportImportState()
    data object Importing : ExportImportState()
    data class ImportSuccess(val count: Int) : ExportImportState()
    data class Error(val message: String) : ExportImportState()
}

class SettingsViewModel(
    private val exportImportRepository: ExportImportRepository,
    private val bleRepository: BleRepository,
    private val apiKeyStore: ApiKeyStore
) : ViewModel() {

    private val _provider = MutableStateFlow(apiKeyStore.getProvider())
    val provider: StateFlow<Provider> = _provider.asStateFlow()

    private val _apiKey = MutableStateFlow(apiKeyStore.getApiKey(_provider.value))
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    fun setProvider(provider: Provider) {
        apiKeyStore.setProvider(provider)
        _provider.value = provider
        _apiKey.value = apiKeyStore.getApiKey(provider)
    }

    fun setApiKey(key: String) {
        apiKeyStore.setApiKey(_provider.value, key)
        _apiKey.value = apiKeyStore.getApiKey(_provider.value)
    }

    val connectionState: StateFlow<ConnectionState> = bleRepository.connectionState
    val rawPacketLog: StateFlow<List<String>> = bleRepository.rawPacketLog

    private val _scannedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BleDevice>> = _scannedDevices.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _state = MutableStateFlow<ExportImportState>(ExportImportState.Idle)
    val state: StateFlow<ExportImportState> = _state.asStateFlow()

    private var scanJob: Job? = null

    fun startScan() {
        scanJob?.cancel()
        _errorMessage.value = null
        _scannedDevices.value = emptyList()
        scanJob = viewModelScope.launch {
            try {
                bleRepository.scanDevices().collect { devices ->
                    _scannedDevices.value = devices
                }
            } catch (e: Exception) {
                _errorMessage.value = "Suche fehlgeschlagen: ${e.message}"
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
    }

    fun connect(device: BleDevice) {
        _errorMessage.value = null
        viewModelScope.launch {
            bleRepository.connect(device).onFailure { e ->
                _errorMessage.value = e.message ?: "Verbindung fehlgeschlagen"
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            bleRepository.disconnect()
        }
    }

    fun sendStartStop(action: Int) {
        viewModelScope.launch {
            bleRepository.startStop(action)
        }
    }

    fun exportData() {
        _state.value = ExportImportState.Exporting
        viewModelScope.launch {
            try {
                val json = exportImportRepository.exportDataToJson()
                _state.value = ExportImportState.ExportSuccess(json)
            } catch (e: Exception) {
                _state.value = ExportImportState.Error("Export fehlgeschlagen: ${e.localizedMessage}")
            }
        }
    }

    fun importData(jsonString: String) {
        _state.value = ExportImportState.Importing
        viewModelScope.launch {
            val result = exportImportRepository.importDataFromJson(jsonString)
            result.fold(
                onSuccess = { count ->
                    _state.value = ExportImportState.ImportSuccess(count)
                },
                onFailure = { error ->
                    _state.value = ExportImportState.Error("Import fehlgeschlagen: ${error.localizedMessage}")
                }
            )
        }
    }

    fun resetState() {
        _state.value = ExportImportState.Idle
    }
}
