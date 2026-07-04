package dev.bitsbots.tldh.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TldhBackground = Color(0xFF08030A)
val TldhSurface = Color(0xFF160815)
val TldhPurple = Color(0xFF6F063F)
val TldhHotPurple = Color(0xFFA50B5E)
val TldhGlow = Color(0xFFD72B84)
val TldhText = Color(0xFFFDF7FB)
val TldhTextMuted = Color(0xFFD7B8CA)
val TldhSuccess = Color(0xFF34D399)
val TldhDanger = Color(0xFFFB7185)

private val TldhDarkColorScheme: ColorScheme = darkColorScheme(
    primary = TldhPurple,
    secondary = TldhGlow,
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
