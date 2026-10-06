package com.batteryexpert.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batteryexpert.data.ble.ConnectionState
import com.batteryexpert.data.ble.ProtocolCodec
import com.batteryexpert.data.ble.SlotHistory
import com.batteryexpert.data.ble.SlotStatus
import com.batteryexpert.data.db.CellTypeEntity
import com.batteryexpert.data.db.ChargeProfileEntity
import com.batteryexpert.data.repository.BatteryRepository
import com.batteryexpert.data.repository.BleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MonitorViewModel(
    private val bleRepository: BleRepository,
    private val batteryRepository: BatteryRepository,
    private val protocolCodec: ProtocolCodec = ProtocolCodec()
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = bleRepository.connectionState
    val slotStatuses: StateFlow<List<SlotStatus>> = bleRepository.slotStatuses
    val slotHistories: StateFlow<Map<Int, SlotHistory>> = bleRepository.slotHistories

    val cellTypes: StateFlow<List<CellTypeEntity>> = batteryRepository.getCellTypes()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun sendStartStop(action: Int) {
        viewModelScope.launch { bleRepository.startStop(action) }
    }

    fun sendConfig(
        profile: ChargeProfileEntity,
        chemistryCode: Int,
        capacityCutoffMah: Int,
        targetSlots: Set<Int>
    ) {
        viewModelScope.launch {
            try {
                bleRepository.startStop(0)
                delay(200)

                if (targetSlots.size == 4) {
                    bleRepository.writePacket(
                        protocolCodec.buildChargeConfig(profile, chemistryCode, 0x00, capacityCutoffMah)
                    )
                    delay(200)
                    bleRepository.startStop(3)
                } else {
                    for (slot in targetSlots.sorted()) {
                        val bm = 1 shl (slot - 1)
                        bleRepository.writePacket(
                            protocolCodec.buildChargeConfig(profile, chemistryCode, bm, capacityCutoffMah)
                        )
                        delay(150)
                    }
                    delay(200)
                    for (slot in targetSlots.sorted()) {
                        bleRepository.startStop(1 shl (slot - 1))
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
