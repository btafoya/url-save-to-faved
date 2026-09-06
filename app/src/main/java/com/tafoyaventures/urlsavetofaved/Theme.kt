package com.tafoyaventures.urlsavetofaved

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand blue sampled from logo.png; tuned per-mode for AA contrast against onPrimary/onSecondary.
private val LightColors = lightColorScheme(
    primary = Color(0xFF0058CC),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF3D6FB4),
    onSecondary = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF0080FE),
    onPrimary = Color(0xFF00203D),
    secondary = Color(0xFFA2D2FD),
    onSecondary = Color(0xFF00203D)
)

@Composable
fun AppTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
        }
    }

    MaterialTheme(colorScheme = if (useDark) DarkColors else LightColors, content = content)
}
