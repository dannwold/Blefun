package com.example.blecontroller.ui.screens

import android.bluetooth.BluetoothGattCharacteristic
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.blecontroller.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GattHandshakeScreen(viewModel: MainViewModel) {
    val services by viewModel.gattServices.collectAsState()

    var staticKeyHex by remember { mutableStateOf("00000000") }
    var triggerPayloadHex by remember { mutableStateOf("AA01") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("GATT EXPLORER", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(services) { service ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Service: ${service.uuid}", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.secondary)
                        service.characteristics.forEach { char ->
                            val props = mutableListOf<String>()
                            if (char.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) props.add("READ")
                            if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) props.add("WRITE")
                            if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) props.add("WRITE_NO_RES")
                            if (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) props.add("NOTIFY")

                            Row(modifier = Modifier.padding(start = 16.dp, top = 4.dp).clickable {
                                // Make it active UUID
                                viewModel.bleEngine.writeCharacteristicUuid = char.uuid
                            }) {
                                Column {
                                    Text("Char: ${char.uuid}", fontFamily = FontFamily.Monospace, color = Color.LightGray)
                                    Row {
                                        props.forEach {
                                            Badge { Text(it) }
                                            Spacer(Modifier.width(4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Divider(color = Color.DarkGray)
        Spacer(Modifier.height(16.dp))

        Text("XOR HANDSHAKE LAB", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = staticKeyHex,
            onValueChange = { staticKeyHex = it },
            label = { Text("Static Key (Hex)") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
        )
        OutlinedTextField(
            value = triggerPayloadHex,
            onValueChange = { triggerPayloadHex = it },
            label = { Text("Auth Trigger Payload (Hex)") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
        )

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                try {
                    val triggerBytes = triggerPayloadHex.replace(" ", "").chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                    viewModel.sendRawHex(triggerBytes, false)
                    // The actual XOR response requires full 2-way GATT tracking which would be built into BleEngine.
                    // For the Lab UI request, we trigger the auth packet.
                    viewModel.bleEngine.log("Transmitted Auth Trigger")
                } catch (e: Exception) {
                    viewModel.bleEngine.log("Invalid Hex")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("RUN CHALLENGE-RESPONSE FLOW")
        }
    }
}
