package com.garmincheck.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object GarminColors {
    val Black = Color(0xFF121212)
    val DarkGray = Color(0xFF1E1E1E)
    val Gray = Color(0xFF2C2C2C)
    val LightGray = Color(0xFF3C3C3C)
    val Cyan = Color(0xFF00D4AA)
    val CyanDark = Color(0xFF00A080)
    val Blue = Color(0xFF2196F3)
    val Orange = Color(0xFFFF9800)
    val Red = Color(0xFFFF5252)
    val Purple = Color(0xFFB388FF)
    val White = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB0B0B0)
    val CardBackground = Color(0xFF1A1A1A)
    val SurfaceVariant = Color(0xFF252525)
}

private val GarminDarkColorScheme = darkColorScheme(
    primary = GarminColors.Cyan,
    onPrimary = GarminColors.Black,
    primaryContainer = GarminColors.CyanDark,
    onPrimaryContainer = GarminColors.White,
    secondary = GarminColors.Blue,
    onSecondary = GarminColors.Black,
    tertiary = GarminColors.Orange,
    background = GarminColors.Black,
    onBackground = GarminColors.White,
    surface = GarminColors.DarkGray,
    onSurface = GarminColors.White,
    surfaceVariant = GarminColors.SurfaceVariant,
    onSurfaceVariant = GarminColors.TextSecondary,
    error = GarminColors.Red,
    onError = GarminColors.White,
    outline = GarminColors.LightGray
)

@Composable
fun GarminCheckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GarminDarkColorScheme,
        typography = GarminTypography,
        content = content
    )
}
