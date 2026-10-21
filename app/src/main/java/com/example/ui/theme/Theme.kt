package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ResonanceColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.Black,
    primaryContainer = ElectricViolet,
    onPrimaryContainer = Color.White,
    secondary = CyberCyan,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = CyberCyan,
    tertiary = NeonPink,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = PrimaryText,
    surface = DarkSurface,
    onSurface = PrimaryText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SecondaryText,
    outline = DarkSurfaceHover,
    surfaceTint = NeonPurple
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ResonanceColorScheme,
        typography = Typography,
        content = content
    )
}
