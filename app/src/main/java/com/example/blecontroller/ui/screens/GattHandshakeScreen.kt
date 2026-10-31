package com.example.blecontroller.ui.screens

import android.bluetooth.BluetoothGattCharacteristic
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.blecontroller.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GattHandshakeScreen(viewModel: MainViewModel) {
    val services by viewModel.gattServices.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("GATT EXPLORER", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        LazyColumn {
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
    }
}
