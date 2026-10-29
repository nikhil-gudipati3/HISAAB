package app.hisaab.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object HColors {
    val Void = Color(0xFF040706)
    val Surface = Color(0xFF0A100D)
    val SurfaceHigh = Color(0xFF0F1813)
    val Hairline = Color(0xFF16251D)
    val OutlineDim = Color(0xFF1F3A2D)
    val TextPrimary = Color(0xFFE9F5EE)
    val TextSecondary = Color(0xFF8FA399)
    val TextTertiary = Color(0xFF55675D)
    val Accent = Color(0xFF35F0A1)
    val AccentDim = Color(0xFF1C8A5F)
    val OnAccent = Color(0xFF03110B)
}

/** Dark only. System dark theme is ignored. No dynamic color, no purple/blue anywhere. */
@Composable
fun HisaabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = HColors.Void, surface = HColors.Surface,
            surfaceContainer = HColors.SurfaceHigh, primary = HColors.Accent,
            onPrimary = HColors.OnAccent, onBackground = HColors.TextPrimary,
            onSurface = HColors.TextPrimary, outline = HColors.OutlineDim,
            secondary = HColors.AccentDim
        ),
        content = content
    )
}
