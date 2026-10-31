package com.example.blecontroller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.blecontroller.MainViewModel

@Composable
fun ControlScreen(viewModel: MainViewModel) {
    val powerState by viewModel.powerState.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val rgbState by viewModel.rgbState.collectAsState()

    var sliderR by remember { mutableStateOf(0f) }
    var sliderG by remember { mutableStateOf(0f) }
    var sliderB by remember { mutableStateOf(0f) }
    var sliderBrightness by remember { mutableStateOf(0f) }
    var keepAlive by remember { mutableStateOf(true) }

    LaunchedEffect(rgbState) {
        sliderR = rgbState[0].toFloat()
        sliderG = rgbState[1].toFloat()
        sliderB = rgbState[2].toFloat()
    }
    LaunchedEffect(brightness) {
        sliderBrightness = brightness.toFloat()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("DEVICE STATE", color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Power: ${if (powerState) "ON" else "OFF"}")
                    Spacer(Modifier.width(16.dp))
                    Text("Keep-Alive")
                    Switch(checked = keepAlive, onCheckedChange = {
                        keepAlive = it
                        viewModel.toggleKeepAlive(it)
                    })
                }
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Color(rgbState[0], rgbState[1], rgbState[2]))
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { viewModel.setPower(true) }) { Text("POWER ON") }
            Button(onClick = { viewModel.setPower(false) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("POWER OFF") }
        }

        Spacer(Modifier.height(16.dp))

        Text("RGB Control")
        Slider(value = sliderR, onValueChange = { sliderR = it }, valueRange = 0f..255f, onValueChangeFinished = { viewModel.setRgb(sliderR.toInt(), sliderG.toInt(), sliderB.toInt()) })
        Slider(value = sliderG, onValueChange = { sliderG = it }, valueRange = 0f..255f, onValueChangeFinished = { viewModel.setRgb(sliderR.toInt(), sliderG.toInt(), sliderB.toInt()) })
        Slider(value = sliderB, onValueChange = { sliderB = it }, valueRange = 0f..255f, onValueChangeFinished = { viewModel.setRgb(sliderR.toInt(), sliderG.toInt(), sliderB.toInt()) })

        Spacer(Modifier.height(16.dp))

        Text("Brightness")
        Slider(value = sliderBrightness, onValueChange = { sliderBrightness = it }, valueRange = 0f..100f, onValueChangeFinished = { viewModel.setBrightness(sliderBrightness.toInt()) })
    }
}
