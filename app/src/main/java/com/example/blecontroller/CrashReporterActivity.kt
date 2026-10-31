package com.example.blecontroller

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.blecontroller.ui.theme.BLEControllerTheme

class CrashReporterActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CRASH_INFO = "EXTRA_CRASH_INFO"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stackTrace = intent.getStringExtra(EXTRA_CRASH_INFO) ?: "No crash info provided"

        setContent {
            BLEControllerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Application Crashed", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Crash Log", stackTrace)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(this@CrashReporterActivity, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Copy Crash Log")
                        }
                        Spacer(Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(Color.Black)
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = stackTrace,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
