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
    outline = BorderZinc800,
    outlineVariant = BorderZinc700,
    error = AccentRose
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF09090B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF4F4F5),
    onPrimaryContainer = Color(0xFF09090B),
    secondary = Color(0xFF52525B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4F4F5),
    onSecondaryContainer = Color(0xFF18181B),
    tertiary = AccentEmerald,
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF09090B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF09090B),
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF71717A),
    outline = Color(0xFFE4E4E7),
    outlineVariant = Color(0xFFD4D4D8),
    error = AccentRose
)

/**
 * Global ThemeProvider wrapper that enforces the #121215 dark theme across all screens.
 */
@Composable
fun ThemeProvider(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalIsDarkMode provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
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
    ThemeProvider(darkTheme = darkTheme, content = content)
}

