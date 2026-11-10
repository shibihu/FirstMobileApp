package com.example.nativestarter.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = IndigoPrimary,
    primaryContainer = IndigoContainer,
)

private val DarkColors = darkColorScheme(
    primary = IndigoPrimaryDark,
    primaryContainer = IndigoContainerDark,
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
