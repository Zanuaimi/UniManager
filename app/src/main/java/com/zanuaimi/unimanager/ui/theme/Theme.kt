package com.zanuaimi.unimanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

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
    val palette = UniManagerPalette()
    val colors = darkColorScheme(
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
}

@Composable
fun UniManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = UniManagerTheme.colors, content = content)
}
