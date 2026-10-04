package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    accentPrimary: Color = SamPurplePrimary,
    accentSecondary: Color = SamPurpleSecondary,
    accentTertiary: Color = SamMagenta,
    accentGlow: Color = SamCyanGlow,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = accentPrimary,
        onPrimary = SamDarkBg,
        primaryContainer = SamSurfaceElevated,
        onPrimaryContainer = accentSecondary,
        secondary = accentGlow,
        onSecondary = SamDarkBg,
        secondaryContainer = SamSurfaceElevated,
        onSecondaryContainer = accentGlow,
        tertiary = accentTertiary,
        onTertiary = SamDarkBg,
        background = SamDarkBg,
        onBackground = SamTextPrimary,
        surface = SamSurface,
        onSurface = SamTextPrimary,
        surfaceVariant = SamSurfaceElevated,
        onSurfaceVariant = SamTextSecondary,
        outline = SamSurfaceBorder,
        error = SamErrorRed,
        onError = SamTextPrimary
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SamDarkBg.toArgb()
                window.navigationBarColor = SamDarkBg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
