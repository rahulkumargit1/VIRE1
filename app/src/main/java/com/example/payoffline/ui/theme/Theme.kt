package com.example.payoffline.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary            = Indigo600,
    onPrimary          = Color.White,
    primaryContainer   = Indigo100,
    onPrimaryContainer = Indigo900,
    secondary          = Emerald500,
    onSecondary        = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary           = Amber500,
    error              = Rose600,
    background         = Slate50,
    onBackground       = Slate900,
    surface            = Color.White,
    onSurface          = Slate900,
    surfaceVariant     = Slate100,
    onSurfaceVariant   = Slate600,
    outline            = Slate300,
)

private val DarkColorScheme = darkColorScheme(
    primary            = Indigo400,
    onPrimary          = Slate900,
    primaryContainer   = Indigo700,
    onPrimaryContainer = Indigo100,
    secondary          = Emerald400,
    onSecondary        = Slate900,
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Emerald400,
    tertiary           = Amber400,
    error              = Rose500,
    background         = Slate900,
    onBackground       = Slate100,
    surface            = Slate800,
    onSurface          = Slate100,
    surfaceVariant     = Slate700,
    onSurfaceVariant   = Slate400,
    outline            = Slate600,
)

@Composable
fun PayOfflineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
