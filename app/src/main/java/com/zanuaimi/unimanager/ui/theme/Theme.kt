package com.zanuaimi.unimanager.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.dynamicDarkColorScheme
import com.zanuaimi.unimanager.data.model.AppearanceSettings

@Immutable
data class UniManagerPalette(
    val background: Color = Color(0xFF210000),
    val surface: Color = Color(0xFF300000),
    val elevated: Color = Color(0xFF500000),
    val accent: Color = Color(0xFFFF5656),
    val accentDark: Color = Color(0xFFAA0000),
    val outline: Color = Color(0xFF7D3636),
)

object UniManagerTheme {
    var activeAppearance by mutableStateOf(AppearanceSettings())
        private set

    fun setAppearance(settings: AppearanceSettings) {
        activeAppearance = settings
    }

    val palette = UniManagerPalette()
    private val uniPatchesColors = darkColorScheme(
        primary = palette.accent,
        onPrimary = Color.White,
        background = palette.background,
        onBackground = Color.White,
        surface = palette.surface,
        onSurface = Color.White,
        surfaceVariant = palette.elevated,
        onSurfaceVariant = Color(0xFFE8BABA),
        outline = palette.outline,
    )

    @Composable
    fun colors(settings: AppearanceSettings) = when (settings.colorSet) {
        "dynamic" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(LocalContext.current) else uniPatchesColors
        "custom" -> darkColorScheme(
            primary = parseColor(settings.customAccent, palette.accent),
            onPrimary = Color.White,
            background = palette.background,
            onBackground = Color.White,
            surface = palette.surface,
            onSurface = Color.White,
            surfaceVariant = palette.elevated,
            onSurfaceVariant = Color(0xFFE8BABA),
            outline = palette.outline,
        )
        else -> uniPatchesColors
    }

    private fun parseColor(value: String, fallback: Color): Color = runCatching {
        Color(android.graphics.Color.parseColor(value))
    }.getOrDefault(fallback)
}

@Composable
fun UniManagerTheme(settings: AppearanceSettings = UniManagerTheme.activeAppearance, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = UniManagerTheme.colors(settings), content = content)
}
