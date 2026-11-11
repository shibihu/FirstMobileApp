package com.example.nativestarter.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6AA7FF),
    onPrimary = Color(0xFF0B1220),
    primaryContainer = Color(0xFFEAF2FF),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF5EEAD4),
    background = Color(0xFFF5F7FB),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CC5FF),
    onPrimary = Color(0xFF07111A),
    primaryContainer = Color(0xFF1D2F4D),
    onPrimaryContainer = Color(0xFFE2E8F0),
    secondary = Color(0xFF5EEAD4),
    background = Color(0xFF090E17),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFFCBD5E1),
)

/**
 * Follows the OS setting by default via [darkTheme]; Settings lets the user override it.
 */
@Composable
fun NativeStarterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
