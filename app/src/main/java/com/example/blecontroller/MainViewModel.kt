package com.example.blecontroller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val bleEngine = BleEngine(application.applicationContext)

    // BLE State flows directly from BleEngine
    val scannedDevices = bleEngine.scannedDevices
    val connectionState = bleEngine.connectionState
    val connectedDevice = bleEngine.connectedDevice
    val gattServices = bleEngine.gattServices
    val logs = bleEngine.logs

    val powerState = bleEngine.powerState
    val brightness = bleEngine.brightness
    val rgbState = bleEngine.rgbState

    // Macro Engine State
    private val _sequences = MutableStateFlow<List<MacroSequence>>(emptyList())
    val sequences = _sequences.asStateFlow()

    private val _isPlayingMacro = MutableStateFlow(false)
    val isPlayingMacro = _isPlayingMacro.asStateFlow()

    private val _currentMacroStepIndex = MutableStateFlow(-1)
    val currentMacroStepIndex = _currentMacroStepIndex.asStateFlow()

    private val _speedMultiplier = MutableStateFlow(1.0f)
    val speedMultiplier = _speedMultiplier.asStateFlow()

    private var macroJob: Job? = null

    init {
        loadDefaultMacros()
    }

    private fun loadDefaultMacros() {
        val emergencyStrobe = MacroSequence(
            id = "1",
            name = "Emergency Red/Blue Strobe",
            steps = listOf(
                MacroStep("1a", "Red", byteArrayOf(0x02, 0xFF.toByte(), 0x00, 0x00), 150, 0xFFFF0000.toInt(), false),
                MacroStep("1b", "Blue", byteArrayOf(0x02, 0x00, 0x00, 0xFF.toByte()), 150, 0xFF0000FF.toInt(), false)
            ),
            loopIndefinitely = true,
            repeatCount = 0
        )

        val cyberpunkPulse = MacroSequence(
            id = "2",
            name = "Cyberpunk Pulse",
            steps = listOf(
                MacroStep("2a", "Pink", byteArrayOf(0x02, 0xFF.toByte(), 0x00, 0x7F), 400, 0xFFFF007F.toInt(), false),
                MacroStep("2b", "Cyan", byteArrayOf(0x02, 0x00, 0xF0.toByte(), 0xFF.toByte()), 400, 0xFF00F0FF.toInt(), false),
                MacroStep("2c", "Purple", byteArrayOf(0x02, 0x70, 0x00, 0xFF.toByte()), 400, 0xFF7000FF.toInt(), false)
            ),
            loopIndefinitely = true,
            repeatCount = 0
        )

        val lightningStrike = MacroSequence(
            id = "3",
            name = "Lightning Strike",
            steps = listOf(
                MacroStep("3a", "Off", byteArrayOf(0x00), 0, null, true), // Replace with raw if needed
                MacroStep("3b", "100% White", byteArrayOf(0x02, 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()), 80, 0xFFFFFFFF.toInt(), false),
                MacroStep("3c", "Off", byteArrayOf(0x00), 60, null, true), // replace with actual
                MacroStep("3d", "80% White", byteArrayOf(0x02, 0xCC.toByte(), 0xCC.toByte(), 0xCC.toByte()), 120, 0xFFCCCCCC.toInt(), false),
                MacroStep("3e", "Off", byteArrayOf(0x00), 2000, null, true)
            ),
            loopIndefinitely = true,
            repeatCount = 0
        )

        _sequences.value = listOf(emergencyStrobe, cyberpunkPulse, lightningStrike)
    }

    fun startScan() = bleEngine.startScan()
    fun stopScan() = bleEngine.stopScan()
    fun connect(address: String) = bleEngine.connect(address)
    fun disconnect() = bleEngine.disconnect()

    fun toggleKeepAlive(enabled: Boolean) {
        bleEngine.keepAliveEnabled = enabled
    }

    fun setPower(on: Boolean) {
        viewModelScope.launch {
            val char = bleEngine.findCharacteristic(bleEngine.writeCharacteristicUuid) ?: return@launch
            val payload = byteArrayOf(if (on) 0x01 else 0x00)
            val pkt = XorPacketEngine.build20BytePaddedXorPacket(0x33, 0x01, payload)
            bleEngine.writeCharacteristic(char, pkt)
        }
    }

    fun setRgb(r: Int, g: Int, b: Int, mode: Int = 0x02) {
        viewModelScope.launch {
            val char = bleEngine.findCharacteristic(bleEngine.writeCharacteristicUuid) ?: return@launch
            val payload = byteArrayOf(mode.toByte(), r.toByte(), g.toByte(), b.toByte())
            val pkt = XorPacketEngine.build20BytePaddedXorPacket(0x33, 0x05, payload)
            bleEngine.writeCharacteristic(char, pkt)
        }
    }

    fun setBrightness(level: Int) {
        viewModelScope.launch {
            val char = bleEngine.findCharacteristic(bleEngine.writeCharacteristicUuid) ?: return@launch
            val payload = byteArrayOf(level.toByte())
            val pkt = XorPacketEngine.build20BytePaddedXorPacket(0x33, 0x04, payload)
            bleEngine.writeCharacteristic(char, pkt)
        }
    }

    fun sendRawHex(bytes: ByteArray, appendXor: Boolean) {
        viewModelScope.launch {
            val char = bleEngine.findCharacteristic(bleEngine.writeCharacteristicUuid) ?: return@launch
            val pkt = XorPacketEngine.buildVariableXorPacket(bytes, appendXor)
            bleEngine.writeCharacteristic(char, pkt)
        }
    }

    // Macro Engine
    fun setSpeedMultiplier(multiplier: Float) {
        _speedMultiplier.value = multiplier
    }

    fun playMacro(sequence: MacroSequence) {
        stopMacro()
        _isPlayingMacro.value = true

        macroJob = viewModelScope.launch {
            val char = bleEngine.findCharacteristic(bleEngine.writeCharacteristicUuid)
            if (char == null) {
                _isPlayingMacro.value = false
                return@launch
            }

            bleEngine.keepAliveEnabled = false // pause keepalive to avoid interleaving

            var loopCount = 0
            while (isActive && (sequence.loopIndefinitely || loopCount < sequence.repeatCount)) {
                for ((index, step) in sequence.steps.withIndex()) {
                    if (!isActive) break
                    _currentMacroStepIndex.value = index

                    val pkt = if (step.isRaw) {
                        step.rawHexOrPayload
                    } else {
                        XorPacketEngine.build20BytePaddedXorPacket(0x33, 0x05, step.rawHexOrPayload)
                    }

                    bleEngine.writeCharacteristic(char, pkt)

                    val delayTime = (step.delayMs / _speedMultiplier.value).toLong()
                    if (delayTime > 0) {
                        delay(delayTime)
                    }
                }
                loopCount++
            }
            _isPlayingMacro.value = false
            _currentMacroStepIndex.value = -1
            bleEngine.keepAliveEnabled = true // resume keepalive
        }
    }

    fun stopMacro() {
        macroJob?.cancel()
        _isPlayingMacro.value = false
        _currentMacroStepIndex.value = -1
        bleEngine.keepAliveEnabled = true
    }
}
