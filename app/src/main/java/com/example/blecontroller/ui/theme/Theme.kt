package com.example.blecontroller.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val SurfaceDark = Color(0xFF0D1117)
val CardDark = Color(0xFF161B22)
val TerminalGreen = Color(0xFF00E676)
val AccentCyan = Color(0xFF00B0FF)
val PrimaryRed = Color(0xFFF44336)
val OnSurfaceText = Color(0xFFC9D1D9)

private val TacticalColorScheme = darkColorScheme(
    primary = TerminalGreen,
    secondary = AccentCyan,
    background = SurfaceDark,
    surface = CardDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = OnSurfaceText,
    onSurface = OnSurfaceText,
    error = PrimaryRed
)

@Composable
fun BLEControllerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = TacticalColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
