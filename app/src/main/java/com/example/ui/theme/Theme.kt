package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkMode = staticCompositionLocalOf { true }

// Enforces deep #09090B canvas with #121215 primary surfaces/containers
private val DarkColorScheme = darkColorScheme(
    primary = TextWhite,
    onPrimary = CanvasBlack,
    primaryContainer = ContainerDark, // #121215
    onPrimaryContainer = TextWhite,
    secondary = TextZinc300,
    onSecondary = CanvasBlack,
    secondaryContainer = ContainerDark, // #121215
    onSecondaryContainer = TextWhite,
    tertiary = AccentEmerald,
    background = CanvasBlack, // #09090B
    onBackground = TextWhite,
    surface = ContainerDark, // #121215
    onSurface = TextWhite,
    surfaceVariant = SurfaceCard, // #18181B
    onSurfaceVariant = TextZinc400,
    outline = BorderZinc800, // #27272A
    outlineVariant = BorderZinc700,
    error = AccentRose
)

/**
 * Global ThemeProvider wrapper that permanently enforces the industrial #121215 dark theme across all screens.
 */
@Composable
fun ThemeProvider(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalIsDarkMode provides true
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    ThemeProvider(darkTheme = true, content = content)
}

