package com.dpibypass.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = YouTubeRed,
    onPrimary = TextPrimary,
    primaryContainer = YouTubeRedDark,
    onPrimaryContainer = TextPrimary,
    secondary = AccentGreen,
    onSecondary = DarkBackground,
    secondaryContainer = AccentGreenDark,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentBlue,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = TextTertiary,
    error = StatusInactive,
    onError = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = YouTubeRed,
    onPrimary = TextPrimary,
    primaryContainer = YouTubeRedLight,
    onPrimaryContainer = DarkBackground,
    secondary = AccentGreen,
    onSecondary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary
)

@Composable
fun DPIBypassTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val view = LocalView.current
            if (!view.isInEditMode) {
                dynamicDarkColorScheme(view.context)
            } else DarkColorScheme
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Always use dark theme for this app
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
