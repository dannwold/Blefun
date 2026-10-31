package com.example.blecontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.blecontroller.ui.screens.*
import com.example.blecontroller.ui.theme.BLEControllerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import android.Manifest
import android.os.Build

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BLEControllerTheme {
                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                } else {
                    listOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN
                    )
                }

                val permissionState = rememberMultiplePermissionsState(permissions = permissions)

                if (permissionState.allPermissionsGranted) {
                    MainApp(viewModel)
                } else {
                    Column {
                        Text("Permissions required for BLE")
                        Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
                            Text("Grant Permissions")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Scan", "Control", "Macro", "GATT", "Term")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (selectedTab) {
                0 -> ScannerScreen(viewModel)
                1 -> ControlScreen(viewModel)
                2 -> MacroSequencerScreen(viewModel)
                3 -> GattHandshakeScreen(viewModel)
                4 -> TerminalScreen(viewModel)
            }
        }
    }
}
