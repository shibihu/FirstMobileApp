package com.example.nativestarter.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Every Material 3 role is defined here on purpose. An undefined role falls back
 * to the baseline palette, which shows Material lavender beside our blue.
 */
private val LightColors = lightColorScheme(
    primary = SignalBlue,
    onPrimary = BoothInk,
    primaryContainer = BlueMist,
    onPrimaryContainer = BenchInk,
    inversePrimary = MoonlitBlue,
    secondary = MixerTeal,
    onSecondary = MixerTealInk,
    secondaryContainer = MixerTealContainer,
    onSecondaryContainer = MixerTealContainerDark,
    tertiary = MixerTealDeep,
    onTertiary = SetWhite,
    tertiaryContainer = MixerTealContainer,
    onTertiaryContainer = MixerTealContainerDark,
    background = BenchWhite,
    onBackground = BenchInk,
    surface = SetWhite,
    onSurface = BenchInk,
    surfaceVariant = BenchSurfaceVariant,
    onSurfaceVariant = SlateGrey,
    surfaceTint = SignalBlue,
    inverseSurface = BoothSurfaceVariant,
    inverseOnSurface = BenchWhiteLow,
    surfaceBright = SetWhite,
    surfaceDim = BenchContainerHighest,
    surfaceContainerLowest = SetWhite,
    surfaceContainerLow = BenchWhiteLow,
    surfaceContainer = BenchContainer,
    surfaceContainerHigh = BenchContainerHigh,
    surfaceContainerHighest = BenchContainerHighest,
    outline = BenchOutline,
    outlineVariant = BenchOutlineVariant,
    error = RedInk,
    onError = SetWhite,
    errorContainer = RedContainer,
    onErrorContainer = RedContainerInk,
)

private val DarkColors = darkColorScheme(
    primary = MoonlitBlue,
    onPrimary = NightInk,
    primaryContainer = DeepBoothNavy,
    onPrimaryContainer = DarkPaper,
    inversePrimary = SignalBlue,
    secondary = MixerTeal,
    onSecondary = MixerTealInk,
    secondaryContainer = MixerTealContainerDark,
    onSecondaryContainer = MixerTealContainer,
    tertiary = MixerTeal,
    onTertiary = MixerTealInk,
    tertiaryContainer = MixerTealContainerDark,
    onTertiaryContainer = MixerTealContainer,
    background = BoothNavy,
    onBackground = DarkPaper,
    surface = PanelNavy,
    onSurface = DarkPaper,
    surfaceVariant = BoothSurfaceVariant,
    onSurfaceVariant = DarkSlate,
    surfaceTint = MoonlitBlue,
    inverseSurface = DarkPaper,
    inverseOnSurface = BenchInk,
    surfaceBright = BoothNavyHighest,
    surfaceDim = BoothNavy,
    surfaceContainerLowest = BoothNavyLowest,
    surfaceContainerLow = BoothNavyLow,
    surfaceContainer = BoothNavyContainer,
    surfaceContainerHigh = BoothNavyHigh,
    surfaceContainerHighest = BoothNavyHighest,
    outline = BoothOutline,
    outlineVariant = BoothOutlineVariant,
    error = RedDark,
    onError = RedDarkInk,
    errorContainer = RedDarkContainer,
    onErrorContainer = RedDarkContainerInk,
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
