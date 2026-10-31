package com.example.blecontroller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.blecontroller.MacroSequence
import com.example.blecontroller.MacroStep
import com.example.blecontroller.MainViewModel
import java.util.UUID

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
    var showAddStepDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("MACRO SEQUENCES", color = MaterialTheme.colorScheme.primary)

        // Sequence selector
        Row(verticalAlignment = Alignment.CenterVertically) {
            sequences.forEach { seq ->
                FilterChip(
                    selected = seq.id == selectedSeqId,
                    onClick = { selectedSeqId = seq.id },
                    label = { Text(seq.name) }
                )
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = {
                val newSeq = MacroSequence(UUID.randomUUID().toString(), "New Seq", emptyList(), true, 0)
                viewModel.updateSequence(newSeq)
                selectedSeqId = newSeq.id
            }) {
                Icon(Icons.Default.Add, "New Sequence")
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

        Spacer(Modifier.height(16.dp))
        Button(onClick = { showAddStepDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("ADD STEP")
        }
        Spacer(Modifier.height(8.dp))

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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(step.label, color = Color.White)
                                Text("${step.delayMs} ms", color = Color.Gray)
                            }
                            IconButton(onClick = {
                                if (index > 0) {
                                    val newSteps = seq.steps.toMutableList()
                                    val temp = newSteps[index]
                                    newSteps[index] = newSteps[index - 1]
                                    newSteps[index - 1] = temp
                                    viewModel.updateSequence(seq.copy(steps = newSteps))
                                }
                            }) { Icon(Icons.Default.KeyboardArrowUp, "Up") }
                            IconButton(onClick = {
                                if (index < seq.steps.size - 1) {
                                    val newSteps = seq.steps.toMutableList()
                                    val temp = newSteps[index]
                                    newSteps[index] = newSteps[index + 1]
                                    newSteps[index + 1] = temp
                                    viewModel.updateSequence(seq.copy(steps = newSteps))
                                }
                            }) { Icon(Icons.Default.KeyboardArrowDown, "Down") }
                            IconButton(onClick = {
                                val newSteps = seq.steps.toMutableList()
                                newSteps.removeAt(index)
                                viewModel.updateSequence(seq.copy(steps = newSteps))
                            }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }

    if (showAddStepDialog && selectedSeq != null) {
        AddStepDialog(
            onDismiss = { showAddStepDialog = false },
            onAdd = { step ->
                val newSteps = selectedSeq.steps.toMutableList()
                newSteps.add(step)
                viewModel.updateSequence(selectedSeq.copy(steps = newSteps))
                showAddStepDialog = false
            }
        )
    }
}

@Composable
fun AddStepDialog(onDismiss: () -> Unit, onAdd: (MacroStep) -> Unit) {
    var label by remember { mutableStateOf("") }
    var delayMs by remember { mutableStateOf("100") }
    var r by remember { mutableStateOf("0") }
    var g by remember { mutableStateOf("0") }
    var b by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add RGB Step") },
        text = {
            Column {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") })
                OutlinedTextField(value = delayMs, onValueChange = { delayMs = it }, label = { Text("Delay (ms)") })
                OutlinedTextField(value = r, onValueChange = { r = it }, label = { Text("R (0-255)") })
                OutlinedTextField(value = g, onValueChange = { g = it }, label = { Text("G (0-255)") })
                OutlinedTextField(value = b, onValueChange = { b = it }, label = { Text("B (0-255)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val delay = delayMs.toLongOrNull() ?: 100L
                val red = r.toIntOrNull() ?: 0
                val green = g.toIntOrNull() ?: 0
                val blue = b.toIntOrNull() ?: 0

                val rgbInt = android.graphics.Color.argb(255, red, green, blue)
                val payload = byteArrayOf(0x02, red.toByte(), green.toByte(), blue.toByte())

                onAdd(MacroStep(UUID.randomUUID().toString(), if(label.isEmpty()) "RGB" else label, payload, delay, rgbInt, false))
            }) { Text("ADD") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
