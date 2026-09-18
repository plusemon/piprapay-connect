package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TextWhite,
    onPrimary = CanvasBlack,
    primaryContainer = SurfaceCard,
    onPrimaryContainer = TextWhite,
    secondary = TextZinc300,
    onSecondary = CanvasBlack,
    secondaryContainer = ContainerDark,
    onSecondaryContainer = TextWhite,
    tertiary = AccentEmerald,
    background = CanvasBlack,
    onBackground = TextWhite,
    surface = ContainerDark,
    onSurface = TextWhite,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextZinc400,
    outline = BorderZinc800,
    outlineVariant = BorderZinc700,
    error = AccentRose
)

private val LightColorScheme = DarkColorScheme // Default to developer-first dark mode aesthetic

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Developer-first pitch dark mode by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
