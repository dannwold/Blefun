package com.example.blecontroller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.blecontroller.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroSequencerScreen(viewModel: MainViewModel) {
    val sequences by viewModel.sequences.collectAsState()
    val isPlaying by viewModel.isPlayingMacro.collectAsState()
    val currentStepIndex by viewModel.currentMacroStepIndex.collectAsState()
    val speedMultiplier by viewModel.speedMultiplier.collectAsState()

    var selectedSeqId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sequences) {
        if (selectedSeqId == null && sequences.isNotEmpty()) {
            selectedSeqId = sequences.first().id
        }
    }

    val selectedSeq = sequences.find { it.id == selectedSeqId }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("MACRO SEQUENCES", color = MaterialTheme.colorScheme.primary)

        // Sequence selector
        Row {
            sequences.forEach { seq ->
                FilterChip(
                    selected = seq.id == selectedSeqId,
                    onClick = { selectedSeqId = seq.id },
                    label = { Text(seq.name) }
                )
                Spacer(Modifier.width(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Speed: ${speedMultiplier}x")
            Slider(
                value = speedMultiplier,
                onValueChange = { viewModel.setSpeedMultiplier(it) },
                valueRange = 0.25f..4.0f,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { selectedSeq?.let { viewModel.playMacro(it) } }, enabled = !isPlaying) { Text("PLAY") }
            Button(onClick = { viewModel.stopMacro() }, enabled = isPlaying, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("STOP") }
        }

        Spacer(Modifier.height(16.dp))

        selectedSeq?.let { seq ->
            LazyColumn {
                itemsIndexed(seq.steps) { index, step ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (index == currentStepIndex) MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            step.rgbPreview?.let { color ->
                                Box(modifier = Modifier.size(24.dp).background(Color(color)))
                                Spacer(Modifier.width(8.dp))
                            }
                            Column {
                                Text(step.label, color = Color.White)
                                Text("${step.delayMs} ms", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
