package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Custom semantic color design tokens that dynamically switch between Dark and Light mode.
 */
data class PipraColors(
    val canvasBg: Color,
    val container: Color,
    val surfaceCard: Color,
    val surfaceCardMuted: Color,
    val border: Color,
    val borderInteractive: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textSubtle: Color,
    val accentEmerald: Color = AccentEmerald,
    val accentAmber: Color = AccentAmber,
    val accentRose: Color = AccentRose,
    val isDark: Boolean
)

val DarkPipraColors = PipraColors(
    canvasBg = CanvasBlack,
    container = ContainerDark,
    surfaceCard = SurfaceCard,
    surfaceCardMuted = SurfaceCardMuted,
    border = BorderZinc800,
    borderInteractive = BorderZinc700,
    textPrimary = TextWhite,
    textSecondary = TextZinc300,
    textMuted = TextZinc400,
    textSubtle = TextZinc500,
    isDark = true
)

val LightPipraColors = PipraColors(
    canvasBg = CanvasWhite,
    container = ContainerLight,
    surfaceCard = SurfaceCardLight,
    surfaceCardMuted = SurfaceCardMutedLight,
    border = BorderLight,
    borderInteractive = BorderLightInteractive,
    textPrimary = TextBlack,
    textSecondary = TextSlate700,
    textMuted = TextSlate500,
    textSubtle = TextSlate400,
    isDark = false
)

val LocalPipraColors = staticCompositionLocalOf { DarkPipraColors }
val LocalIsDarkMode = staticCompositionLocalOf { true }

object PipraTheme {
    val colors: PipraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPipraColors.current

    val isDarkMode: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsDarkMode.current
}

// Enforces deep #09090B canvas with #121215 primary surfaces/containers in Dark mode
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

// Crisp Light Color Scheme for light mode
private val LightColorScheme = lightColorScheme(
    primary = TextBlack,
    onPrimary = Color.White,
    primaryContainer = ContainerLight,
    onPrimaryContainer = TextBlack,
    secondary = TextSlate700,
    onSecondary = Color.White,
    secondaryContainer = SurfaceCardLight,
    onSecondaryContainer = TextBlack,
    tertiary = AccentEmerald,
    background = CanvasWhite,
    onBackground = TextBlack,
    surface = ContainerLight,
    onSurface = TextBlack,
    surfaceVariant = SurfaceCardLight,
    onSurfaceVariant = TextSlate500,
    outline = BorderLight,
    outlineVariant = BorderLightInteractive,
    error = AccentRose
)

/**
 * Global ThemeProvider wrapper that adapts dynamically based on darkTheme parameter
 * and provides theme tokens via LocalPipraColors and LocalIsDarkMode.
 */
@Composable
fun ThemeProvider(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val pipraColors = if (darkTheme) DarkPipraColors else LightPipraColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalIsDarkMode provides darkTheme,
        LocalPipraColors provides pipraColors
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
