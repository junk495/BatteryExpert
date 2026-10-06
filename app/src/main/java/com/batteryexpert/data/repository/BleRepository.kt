package com.batteryexpert.data.repository

import com.batteryexpert.data.ble.BleDevice
import com.batteryexpert.data.ble.ConnectionState
import com.batteryexpert.data.ble.Mc5000BleManager
import com.batteryexpert.data.ble.ProtocolCodec
import com.batteryexpert.data.ble.SlotHistory
import com.batteryexpert.data.ble.SlotStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class BleRepository(
    private val bleManager: Mc5000BleManager,
    private val protocolCodec: ProtocolCodec = ProtocolCodec()
) {
    val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
    val rawPacketLog: StateFlow<List<String>> = bleManager.rawPacketLog
    val slotStatuses: StateFlow<List<SlotStatus>> = bleManager.slotStatuses
    val slotHistories: StateFlow<Map<Int, SlotHistory>> = bleManager.slotHistories

    fun scanDevices(): Flow<List<BleDevice>> = bleManager.scanDevices()

    fun observeSlotStatuses(): Flow<SlotStatus> = bleManager.notifications()
        .filter { it.size >= 4 && (it[2].toInt() and 0xFF) == 0x91 }
        .map { protocolCodec.parseStatus(it) }

    suspend fun connect(device: BleDevice): Result<Unit> = bleManager.connect(device)

    suspend fun disconnect() = bleManager.disconnect()

    suspend fun writePacket(packet: ByteArray) = bleManager.writePacket(packet)

    suspend fun startStop(action: Int) {
        val packet = protocolCodec.buildStartStop(action)
        bleManager.writePacket(packet)
    }
}
