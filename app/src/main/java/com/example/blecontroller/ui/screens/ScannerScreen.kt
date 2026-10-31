package com.example.blecontroller.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothProfile
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.blecontroller.MainViewModel

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(viewModel: MainViewModel) {
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = { viewModel.startScan() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("START SCAN")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("QUICK CONNECT (Govee H6167)", color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.connect("E5:90:03:C6:3F:38") }) {
                    Text("CONNECT E5:90:03:C6:3F:38")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("DISCOVERED DEVICES", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        LazyColumn {
            items(scannedDevices) { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewModel.connect(result.device.address) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = result.device.name ?: "Unknown Device", color = Color.White)
                            Text(text = result.device.address, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        }
                        Badge(containerColor = if (result.rssi > -70) Color(0xFF00E676) else Color.Red) {
                            Text("${result.rssi} dBm", color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}
