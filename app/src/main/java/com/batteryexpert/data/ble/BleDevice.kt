package com.batteryexpert.data.ble

import android.bluetooth.BluetoothDevice

data class BleDevice(
    val name: String,
    val address: String,
    val device: BluetoothDevice,
    val isMc5000: Boolean = false
)

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}
