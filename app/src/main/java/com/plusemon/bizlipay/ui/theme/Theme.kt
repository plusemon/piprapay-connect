package com.plusemon.bizlipay.ui.theme

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
data class BizliColors(
    val primary: Color,
    val primaryVariant: Color,
    val accent: Color,
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
    val statusEmerald: Color = StatusEmerald,
    val statusAmber: Color = StatusAmber,
    val statusRose: Color = StatusRose,
    val isDark: Boolean
)

val DarkBizliColors = BizliColors(
    primary = Indigo500,
    primaryVariant = Indigo600,
    accent = Cyan500,
    canvasBg = Slate900,
    container = Slate800,
    surfaceCard = Slate800,
    surfaceCardMuted = Slate850,
    border = Slate700,
    borderInteractive = Slate600,
    textPrimary = TextWhite,
    textSecondary = Slate300,
    textMuted = Slate400,
    textSubtle = Slate500,
    isDark = true
)

val LightBizliColors = BizliColors(
    primary = Indigo600,
    primaryVariant = Indigo500,
    accent = Cyan500,
    canvasBg = Slate50,
    container = Color.White,
    surfaceCard = Slate100,
    surfaceCardMuted = Color(0xFFE2E8F0),
    border = BorderLight,
    borderInteractive = BorderLightInteractive,
    textPrimary = TextBlack,
    textSecondary = Color(0xFF334155),
    textMuted = Color(0xFF64748B),
    textSubtle = Color(0xFF94A3B8),
    isDark = false
)

typealias PipraColors = BizliColors
val DarkPipraColors = DarkBizliColors
val LightPipraColors = LightBizliColors

val LocalBizliColors = staticCompositionLocalOf { DarkBizliColors }
val LocalIsDarkMode = staticCompositionLocalOf { true }

object BizliTheme {
    val colors: BizliColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBizliColors.current

    val isDarkMode: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsDarkMode.current
}

object PipraTheme {
    val colors: BizliColors
        @Composable
        @ReadOnlyComposable
        get() = LocalBizliColors.current

    val isDarkMode: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsDarkMode.current
}

// Dark Scheme centered on Slate 900 / Slate 800 with Indigo & Cyan accents
private val DarkColorScheme = darkColorScheme(
    primary = Indigo500,
    onPrimary = Color.White,
    primaryContainer = Indigo600,
    onPrimaryContainer = Color.White,
    secondary = Cyan500,
    onSecondary = Slate900,
    secondaryContainer = Slate800,
    onSecondaryContainer = Color.White,
    tertiary = StatusEmerald,
    background = Slate900,
    onBackground = TextWhite,
    surface = Slate800,
    onSurface = TextWhite,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate300,
    outline = Slate700,
    outlineVariant = Slate600,
    error = StatusRose
)

// Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Indigo500,
    onPrimaryContainer = Color.White,
    secondary = Cyan500,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = TextBlack,
    tertiary = StatusEmerald,
    background = Slate50,
    onBackground = TextBlack,
    surface = Color.White,
    onSurface = TextBlack,
    surfaceVariant = Slate100,
    onSurfaceVariant = Color(0xFF64748B),
    outline = BorderLight,
    outlineVariant = BorderLightInteractive,
    error = StatusRose
)

@Composable
fun ThemeProvider(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkBizliColors else LightBizliColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalIsDarkMode provides darkTheme,
        LocalBizliColors provides colors
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
