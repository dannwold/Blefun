package com.example.blecontroller.ui.screens

import androidx.compose.foundation.background
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

@Composable
fun TerminalScreen(viewModel: MainViewModel) {
    val logs by viewModel.logs.collectAsState()
    var hexInput by remember { mutableStateOf("") }
    var appendXor by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black).padding(8.dp)) {
            LazyColumn {
                items(logs) { log ->
                    Text(log, color = Color(0xFF00E676), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = hexInput,
            onValueChange = { hexInput = it },
            label = { Text("Raw Hex (e.g. 330502FF0000)") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
        )

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = appendXor, onCheckedChange = { appendXor = it })
            Text("Append XOR Checksum")
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                try {
                    val cleaned = hexInput.replace(" ", "")
                    val bytes = cleaned.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                    viewModel.sendRawHex(bytes, appendXor)
                } catch (e: Exception) {
                    viewModel.bleEngine.log("Invalid Hex")
                }
            }) {
                Text("SEND")
            }
        }
    }
}
