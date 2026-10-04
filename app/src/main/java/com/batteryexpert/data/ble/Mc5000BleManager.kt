package com.batteryexpert.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume

class Mc5000BleManager(
    private val context: Context,
    private val protocolCodec: ProtocolCodec = ProtocolCodec()
) {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000L
        val SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val CLIENT_CONFIG_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _notificationFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)

    private val _rawPacketLog = MutableStateFlow<List<String>>(emptyList())
    val rawPacketLog: StateFlow<List<String>> = _rawPacketLog.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null

    private fun logRawPacket(prefix: String, bytes: ByteArray) {
        val hex = bytes.joinToString(" ") { "%02X".format(it) }
        val entry = "$prefix: $hex"
        _rawPacketLog.value = (_rawPacketLog.value + entry).takeLast(50)
    }

    fun notifications(): Flow<ByteArray> = _notificationFlow

    fun scanDevices(): Flow<List<BleDevice>> = callbackFlow {
        val adapter: BluetoothAdapter? = bluetoothManager?.adapter
        val scanner = adapter?.bluetoothLeScanner
        if (adapter == null || !adapter.isEnabled || scanner == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val devicesMap = mutableMapOf<String, BleDevice>()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { device ->
                    @SuppressLint("MissingPermission")
                    val name = device.name ?: result.scanRecord?.deviceName ?: "Unbekannt"
                    devicesMap[device.address] = BleDevice(
                        name = name,
                        address = device.address,
                        device = device,
                        isMc5000 = name.contains("MC5000", ignoreCase = true) ||
                            name.contains("SkyRC", ignoreCase = true)
                    )
                    trySend(devicesMap.values.toList())
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { result ->
                    result.device?.let { device ->
                        @SuppressLint("MissingPermission")
                        val name = device.name ?: result.scanRecord?.deviceName ?: "Unbekannt"
                        devicesMap[device.address] = BleDevice(
                            name = name,
                            address = device.address,
                            device = device,
                            isMc5000 = name.contains("MC5000", ignoreCase = true) ||
                                name.contains("SkyRC", ignoreCase = true)
                        )
                    }
                }
                trySend(devicesMap.values.toList())
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("BLE Scan failed with error code $errorCode"))
            }
        }

        @SuppressLint("MissingPermission")
        scanner.startScan(scanCallback)

        awaitClose {
            @SuppressLint("MissingPermission")
            scanner.stopScan(scanCallback)
        }
    }

    suspend fun connect(device: BleDevice): Result<Unit> = withContext(Dispatchers.IO) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            return@withContext Result.success(Unit)
        }

        _connectionState.value = ConnectionState.CONNECTING

        suspendCancellableCoroutine { continuation ->
            var resumed = false
            var timeoutHandler: android.os.Handler? = null

            fun safeResume(result: Result<Unit>) {
                timeoutHandler?.removeCallbacksAndMessages(null)
                if (!resumed) {
                    resumed = true
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }
            }

            timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
            timeoutHandler.postDelayed({
                @SuppressLint("MissingPermission")
                bluetoothGatt?.disconnect()
                @SuppressLint("MissingPermission")
                bluetoothGatt?.close()
                bluetoothGatt = null
                _connectionState.value = ConnectionState.DISCONNECTED
                safeResume(Result.failure(IllegalStateException("Verbindung Timeout (${CONNECT_TIMEOUT_MS / 1000}s)")))
            }, CONNECT_TIMEOUT_MS)

            val gattCallback = object : BluetoothGattCallback() {
                @SuppressLint("MissingPermission")
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            if (!gatt.requestMtu(247)) {
                                gatt.discoverServices()
                            }
                        } else {
                            _connectionState.value = ConnectionState.DISCONNECTED
                            safeResume(Result.failure(IllegalStateException("Gatt connect failed with status $status")))
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        safeResume(Result.failure(IllegalStateException("Disconnected")))
                    }
                }

                @SuppressLint("MissingPermission")
                override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                    gatt.discoverServices()
                }

                @SuppressLint("MissingPermission")
                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val service = gatt.getService(SERVICE_UUID)
                        val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                        if (service != null && characteristic != null) {
                            enableNotifications(gatt, characteristic)
                        } else {
                            _connectionState.value = ConnectionState.DISCONNECTED
                            safeResume(Result.failure(IllegalStateException("MC5000 service or characteristic not found")))
                        }
                    } else {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        safeResume(Result.failure(IllegalStateException("Services discovery failed with status $status")))
                    }
                }

                override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                    if (descriptor.uuid == CLIENT_CONFIG_DESCRIPTOR_UUID) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            _connectionState.value = ConnectionState.CONNECTED
                            safeResume(Result.success(Unit))
                        } else {
                            _connectionState.value = ConnectionState.DISCONNECTED
                            safeResume(Result.failure(IllegalStateException("Failed to enable notifications, status $status")))
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                    if (characteristic.uuid == CHARACTERISTIC_UUID) {
                        @Suppress("DEPRECATION")
                        val value = characteristic.value ?: return
                        logRawPacket("RX", value)
                        _notificationFlow.tryEmit(value)
                    }
                }

                override fun onCharacteristicChanged(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray
                ) {
                    if (characteristic.uuid == CHARACTERISTIC_UUID) {
                        logRawPacket("RX", value)
                        _notificationFlow.tryEmit(value)
                    }
                }
            }

            @SuppressLint("MissingPermission")
            val gatt = device.device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            bluetoothGatt = gatt

            continuation.invokeOnCancellation {
                timeoutHandler?.removeCallbacksAndMessages(null)
                @SuppressLint("MissingPermission")
                gatt?.disconnect()
                @SuppressLint("MissingPermission")
                gatt?.close()
                bluetoothGatt = null
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CLIENT_CONFIG_DESCRIPTOR_UUID) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            @Suppress("DEPRECATION")
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            @Suppress("DEPRECATION")
            gatt.writeDescriptor(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun writePacket(packet: ByteArray) {
        val gatt = bluetoothGatt ?: throw IllegalStateException("Not connected")
        val service = gatt.getService(SERVICE_UUID) ?: throw IllegalStateException("Service not found")
        val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID) ?: throw IllegalStateException("Characteristic not found")

        logRawPacket("TX", packet)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, packet, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = packet
            @Suppress("DEPRECATION")
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    fun slotStatus(slotBitmask: Int): Flow<SlotStatus> = flow {
        val requestPacket = protocolCodec.buildStatusRequest(slotBitmask)
        writePacket(requestPacket)
        _notificationFlow.collect { notificationBytes ->
            val status = protocolCodec.parseStatus(notificationBytes)
            emit(status)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTING
        bluetoothGatt?.let { gatt ->
            gatt.disconnect()
            gatt.close()
        }
        bluetoothGatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
}
