package dev.bitsbots.tldh.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TldhBackground = Color(0xFF09060F)
val TldhSurface = Color(0xFF120B1F)
val TldhPurple = Color(0xFF8B5CF6)
val TldhHotPurple = Color(0xFFC026D3)
val TldhText = Color(0xFFF8F7FF)
val TldhTextMuted = Color(0xFFBEB7D8)
val TldhSuccess = Color(0xFF34D399)
val TldhDanger = Color(0xFFFB7185)

private val TldhDarkColorScheme: ColorScheme = darkColorScheme(
    primary = TldhPurple,
    secondary = TldhHotPurple,
    background = TldhBackground,
    surface = TldhSurface,
    onPrimary = TldhText,
    onSecondary = TldhText,
    onBackground = TldhText,
    onSurface = TldhText
)

@Composable
fun TldhTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TldhDarkColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
