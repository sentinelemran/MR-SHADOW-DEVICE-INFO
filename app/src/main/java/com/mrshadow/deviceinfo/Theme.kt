package com.mrshadow.deviceinfo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Neon = Color(0xFF39FF14)

private val DarkColors = darkColorScheme(
    primary = Neon,
    onPrimary = Color.Black,
    secondary = Neon,
    background = Color(0xFF000000),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF151515),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = Color(0xFF9E9E9E),
    primaryContainer = Color(0xFF0F3A08),
    onPrimaryContainer = Neon
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF138A00),
    onPrimary = Color.White,
    secondary = Color(0xFF138A00),
    background = Color(0xFFF6F6F6),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8E8E8)
)

@Composable
fun AppTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) {
        1 -> true
        2 -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
