package com.zanuaimi.unimanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.zanuaimi.unimanager.data.model.AppearanceSettings

object UniManagerTheme {
    var activeAppearance by mutableStateOf(AppearanceSettings())
        private set

    fun setAppearance(settings: AppearanceSettings) {
        activeAppearance = settings
    }

    private val uniPatchesScheme = darkColorScheme(
        primary = Color(0xFFFF5656),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFAA0000),
        onPrimaryContainer = Color.White,
        background = Color(0xFF210000),
        onBackground = Color.White,
        surface = Color(0xFF300000),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF500000),
        onSurfaceVariant = Color(0xFFE8BABA),
        outline = Color(0xFF7D3636),
    )

    @Composable
    fun colors(settings: AppearanceSettings = activeAppearance): ColorScheme {
        return when (settings.colorSet) {
            "dynamic" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching {
                    if (isSystemInDarkTheme()) dynamicDarkColorScheme(LocalContext.current)
                    else dynamicLightColorScheme(LocalContext.current)
                }.getOrDefault(uniPatchesScheme)
            } else uniPatchesScheme
            "custom" -> customScheme(parseColor(settings.customAccent, Color(0xFFFF5656)))
            else -> uniPatchesScheme
        }
    }

    private fun customScheme(accent: Color): ColorScheme = darkColorScheme(
        primary = accent,
        onPrimary = readableForeground(accent),
        primaryContainer = shade(accent, 0.72f),
        onPrimaryContainer = Color.White,
        background = shade(accent, 0.08f),
        onBackground = Color.White,
        surface = shade(accent, 0.14f),
        onSurface = Color.White,
        surfaceVariant = shade(accent, 0.24f),
        onSurfaceVariant = Color.White.copy(alpha = 0.78f),
        outline = accent.copy(alpha = 0.65f),
    )

    private fun readableForeground(color: Color): Color {
        val luminance = (0.299f * color.red) + (0.587f * color.green) + (0.114f * color.blue)
        return if (luminance > 0.62f) Color.Black else Color.White
    }

    private fun shade(color: Color, factor: Float): Color = Color(
        red = (color.red * factor).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor).coerceIn(0f, 1f),
        alpha = 1f,
    )

    private fun parseColor(value: String, fallback: Color): Color = runCatching {
        Color(android.graphics.Color.parseColor(value))
    }.getOrDefault(fallback)
}

@Suppress("DEPRECATION")
@Composable
fun UniManagerTheme(settings: AppearanceSettings = UniManagerTheme.activeAppearance, content: @Composable () -> Unit) {
    val scheme = UniManagerTheme.colors(settings)
    val systemIsDark = isSystemInDarkTheme()
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            window.statusBarColor = scheme.background.toArgb()
            window.navigationBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                val lightSystemBars = settings.colorSet == "dynamic" &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !systemIsDark
                isAppearanceLightStatusBars = lightSystemBars
                isAppearanceLightNavigationBars = lightSystemBars
            }
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
