package com.batteryexpert.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batteryexpert.data.ble.ConnectionState
import com.batteryexpert.data.ble.ProtocolCodec
import com.batteryexpert.data.ble.SlotStatus
import com.batteryexpert.data.repository.BleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MonitorViewModel(
    private val bleRepository: BleRepository,
    private val protocolCodec: ProtocolCodec = ProtocolCodec()
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = bleRepository.connectionState

    private val _slotStatuses = MutableStateFlow<List<SlotStatus>>(emptyList())
    val slotStatuses: StateFlow<List<SlotStatus>> = _slotStatuses.asStateFlow()

    private var pollingJob: Job? = null

    init {
        observeConnectionAndPoll()
        observeSlotStatuses()
    }

    fun sendStartStop(action: Int) {
        viewModelScope.launch {
            bleRepository.startStop(action)
        }
    }

    private fun observeConnectionAndPoll() {
        viewModelScope.launch {
            connectionState.collectLatest { state ->
                if (state == ConnectionState.CONNECTED) {
                    startPollingSlots()
                } else {
                    stopPollingSlots()
                }
            }
        }
    }

    private fun observeSlotStatuses() {
        viewModelScope.launch {
            bleRepository.observeSlotStatuses().collect { status ->
                val current = _slotStatuses.value.toMutableList()
                current.removeAll { it.slot == status.slot }
                current.add(status)
                _slotStatuses.value = current
            }
        }
    }

    private fun startPollingSlots() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val slotBitmasks = listOf(1, 2, 4, 8)

            while (true) {
                if (connectionState.value == ConnectionState.CONNECTED) {
                    for (mask in slotBitmasks) {
                        try {
                            bleRepository.writePacket(protocolCodec.buildStatusRequest(mask))
                            delay(100L)
                        } catch (_: Exception) {
                        }
                    }
                }
                delay(10_000L)
            }
        }
    }

    private fun stopPollingSlots() {
        pollingJob?.cancel()
        _slotStatuses.value = emptyList()
    }
}
