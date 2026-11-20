package com.deepseek.dailytasks.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E6BE6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E3FF),
    onPrimaryContainer = Color(0xFF00174B),
    secondary = Color(0xFF575E71),
    background = Color(0xFFF6F7FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF1F7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF002E6A),
    primaryContainer = Color(0xFF1B4389),
    secondary = Color(0xFFBFC6DC),
    background = Color(0xFF121318),
    surface = Color(0xFF1A1C22),
    surfaceVariant = Color(0xFF272A32),
)

@Composable
fun DailyTasksTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
