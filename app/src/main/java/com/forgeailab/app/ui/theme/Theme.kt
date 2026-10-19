package com.forgeailab.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val ForgeBlue = Color(0xFF0EA5E9)
val ForgeCyan = Color(0xFF22D3EE)
val ForgeBg = Color(0xFF0B1220)
val ForgeSurface = Color(0xFF151E2E)
val ForgeCard = Color(0xFF1C2740)
val ForgeText = Color(0xFFE8EEF8)
val ForgeMuted = Color(0xFF94A3B8)
val ForgeDanger = Color(0xFFF43F5E)

private val DarkColors = darkColorScheme(
    primary = ForgeBlue,
    onPrimary = Color.White,
    secondary = ForgeCyan,
    onSecondary = Color.Black,
    background = ForgeBg,
    onBackground = ForgeText,
    surface = ForgeSurface,
    onSurface = ForgeText,
    surfaceVariant = ForgeCard,
    onSurfaceVariant = ForgeMuted,
    error = ForgeDanger,
    onError = Color.White,
    outline = Color(0xFF334155)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    error = ForgeDanger,
    onError = Color.White
)

@Composable
fun ForgeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
