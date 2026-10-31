package com.example.blecontroller

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

@SuppressLint("MissingPermission")
class BleEngine(private val context: Context) {

    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Device state
    private val _scannedDevices = MutableStateFlow<List<ScanResult>>(emptyList())
    val scannedDevices: StateFlow<List<ScanResult>> = _scannedDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(BluetoothProfile.STATE_DISCONNECTED)
    val connectionState: StateFlow<Int> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    private val _gattServices = MutableStateFlow<List<BluetoothGattService>>(emptyList())
    val gattServices: StateFlow<List<BluetoothGattService>> = _gattServices.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    // Parsed Device State
    private val _powerState = MutableStateFlow(false)
    val powerState = _powerState.asStateFlow()
    private val _brightness = MutableStateFlow(0)
    val brightness = _brightness.asStateFlow()
    private val _rgbState = MutableStateFlow(IntArray(3) { 0 })
    val rgbState = _rgbState.asStateFlow()

    // Strict Sequential GATT Queue
    private val gattMutex = Mutex()
    private val gattChannel = Channel<Boolean>(Channel.CONFLATED)

    // Default Profile UUIDs (Govee H6167)
    var writeCharacteristicUuid: UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d2b11")
    var notifyCharacteristicUuid: UUID = UUID.fromString("00010203-0405-0607-0809-0a0b0c0d2b10")

    private var heartbeatJob: Job? = null
    var keepAliveEnabled = true
        set(value) {
            field = value
            if (value && connectionState.value == BluetoothProfile.STATE_CONNECTED) {
                startHeartbeat()
            } else {
                stopHeartbeat()
            }
        }

    fun log(msg: String) {
        Log.d("BleEngine", msg)
        _logs.value = _logs.value.takeLast(100) + "${System.currentTimeMillis()}: $msg"
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val currentList = _scannedDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.device.address == result.device.address }
            if (existingIndex >= 0) {
                currentList[existingIndex] = result
            } else {
                currentList.add(result)
            }
            _scannedDevices.value = currentList.sortedByDescending { it.rssi }
        }

        override fun onScanFailed(errorCode: Int) {
            log("Scan failed: $errorCode")
        }
    }

    fun startScan() {
        _scannedDevices.value = emptyList()
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            log("Scanner not available")
            return
        }
        log("Starting scan...")
        scanner.startScan(scanCallback)
    }

    fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        log("Stopped scan.")
    }

    fun connect(address: String) {
        stopScan()
        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            log("Device not found")
            return
        }
        log("Connecting to $address...")
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = newState
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    _connectedDevice.value = gatt.device
                    log("Connected to ${gatt.device.address}")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _connectedDevice.value = null
                    _gattServices.value = emptyList()
                    stopHeartbeat()
                    log("Disconnected")
                    gatt.close()
                    bluetoothGatt = null
                }
            } else {
                log("Connection error: $status")
                _connectionState.value = BluetoothProfile.STATE_DISCONNECTED
                _connectedDevice.value = null
                stopHeartbeat()
                gatt.close()
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _gattServices.value = gatt.services
                log("Services discovered")

                // Auto-Handshake
                scope.launch {
                    val notifyChar = findCharacteristic(notifyCharacteristicUuid)
                    if (notifyChar != null) {
                        enableNotifications(notifyChar)
                    }

                    val writeChar = findCharacteristic(writeCharacteristicUuid)
                    if (writeChar != null) {
                        val initPacket = XorPacketEngine.build20BytePaddedXorPacket(0xAA, 0x01, byteArrayOf())
                        writeCharacteristic(writeChar, initPacket)

                        if (keepAliveEnabled) {
                            startHeartbeat()
                        }
                    }
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            gattChannel.trySend(true)
            log("TX -> ${characteristic.uuid}: Status $status")
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            gattChannel.trySend(true)
            log("RX <- ${characteristic.uuid}: ${value.joinToString("") { "%02X".format(it) }}")
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onCharacteristicRead(gatt, characteristic, characteristic.value ?: byteArrayOf(), status)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            val hex = value.joinToString("") { "%02x".format(it) }
            log("NOTIFY <- ${characteristic.uuid}: $hex")

            // Decode state (Profile 1: Govee)
            if (value.size >= 20 && value[0] == 0xAA.toByte()) {
                when (value[1].toInt()) {
                    0x01 -> _powerState.value = value[2] == 0x01.toByte()
                    0x04 -> _brightness.value = value[2].toInt() and 0xFF
                    0x05 -> {
                        _rgbState.value = intArrayOf(
                            value[3].toInt() and 0xFF,
                            value[4].toInt() and 0xFF,
                            value[5].toInt() and 0xFF
                        )
                    }
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onCharacteristicChanged(gatt, characteristic, characteristic.value ?: byteArrayOf())
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            gattChannel.trySend(true)
            log("Descriptor write status: $status")
        }
    }

    fun findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        val gatt = bluetoothGatt ?: return null
        for (service in gatt.services) {
            for (char in service.characteristics) {
                if (char.uuid == uuid) return char
            }
        }
        return null
    }

    suspend fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, bytes: ByteArray) = gattMutex.withLock {
        val gatt = bluetoothGatt ?: return@withLock

        log("Writing to ${characteristic.uuid}: ${bytes.joinToString("") { "%02X".format(it) }}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val writeType = if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
                            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                            else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            gatt.writeCharacteristic(characteristic, bytes, writeType)
        } else {
            characteristic.value = bytes
            characteristic.writeType = if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
                            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                            else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            gatt.writeCharacteristic(characteristic)
        }

        // Wait for callback or timeout
        withTimeoutOrNull(3000) {
            gattChannel.receive()
        } ?: log("Write timeout")
    }

    suspend fun readCharacteristic(characteristic: BluetoothGattCharacteristic) = gattMutex.withLock {
        val gatt = bluetoothGatt ?: return@withLock
        gatt.readCharacteristic(characteristic)
        withTimeoutOrNull(3000) {
            gattChannel.receive()
        } ?: log("Read timeout")
    }

    suspend fun enableNotifications(characteristic: BluetoothGattCharacteristic) = gattMutex.withLock {
        val gatt = bluetoothGatt ?: return@withLock
        gatt.setCharacteristicNotification(characteristic, true)

        val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val descriptor = characteristic.getDescriptor(cccdUuid)
        if (descriptor != null) {
            val payload = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, payload)
            } else {
                descriptor.value = payload
                gatt.writeDescriptor(descriptor)
            }

            withTimeoutOrNull(3000) {
                gattChannel.receive()
            } ?: log("Descriptor write timeout")
        }
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(2000)
                if (connectionState.value == BluetoothProfile.STATE_CONNECTED) {
                    val char = findCharacteristic(writeCharacteristicUuid)
                    if (char != null) {
                        val pkt = XorPacketEngine.build20BytePaddedXorPacket(0xAA, 0x01, byteArrayOf())
                        writeCharacteristic(char, pkt)
                    }
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
}
