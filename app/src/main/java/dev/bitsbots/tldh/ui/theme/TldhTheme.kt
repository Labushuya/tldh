package dev.bitsbots.tldh.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.bitsbots.tldh.R

/** Brand palette. See DESIGN.md: the accent marks only the primary action, selection and state. */
object TldhColors {
    val Background = Color(0xFF08030A)
    val Surface = Color(0xFF160815)
    val SurfaceRaised = Color(0xFF1F0C1B)
    val Divider = Color(0xFF2A1424)
    val Muted = Color(0xFF4A2140)
    val Accent = Color(0xFFA50B5E)
    val AccentDeep = Color(0xFF6F063F)
    val Glow = Color(0xFFD72B84)
    val Text = Color(0xFFFDF7FB)
    val TextMuted = Color(0xFFD7B8CA)
    val Success = Color(0xFF34D399)
    val Danger = Color(0xFFFB7185)
    val Unsure = Color(0xFFFBBF24)
}

/** Glass nuances: a lit top edge on surfaces, like light catching the rim of a pane. */
object TldhGlass {
    val SurfaceEdge = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.09f),
        0.35f to Color.White.copy(alpha = 0.025f),
        1f to Color.Transparent
    )
    val ButtonEdge = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.22f),
        0.5f to Color.White.copy(alpha = 0.04f),
        1f to Color.Transparent
    )
    val PaneEdge = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.18f),
        1f to Color.White.copy(alpha = 0.04f)
    )
    val Tint = Color(0xFF08030A).copy(alpha = 0.62f)
    val PaneTint = Color(0xFF1F0C1B).copy(alpha = 0.72f)
}

object TldhShapes {
    val Group = RoundedCornerShape(20.dp)
    val Button = RoundedCornerShape(16.dp)
    val Inner = RoundedCornerShape(14.dp)
    val Control = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(percent = 50)
}

val BricolageGrotesque = FontFamily(
    Font(R.font.bricolage_grotesque_medium, FontWeight.Medium),
    Font(R.font.bricolage_grotesque_bold, FontWeight.Bold)
)

/** Bricolage Grotesque has one job (display); everything else is the system sans (Roboto). */
object TldhType {
    val Display = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-0.02).em)
    val Headline = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.015).em)
    val Title = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.01).em)
    val Tldr = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 33.sp, letterSpacing = (-0.01).em)
    val Wordmark = TextStyle(fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.01).em)
    val Body = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
    val BodyStrong = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    val Transcript = TextStyle(fontSize = 16.sp, lineHeight = 27.sp)
    val Row = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
    val Label = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    val Small = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
    val Caption = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    val Micro = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
    val Button = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
}

/** Tabular figures so counters and timestamps don't jitter. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

object TldhMotion {
    /** Entering and state changes: fast start, soft landing. */
    val Enter = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    /** Moving things on screen. */
    val Move = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
    const val PRESS_MS = 160
    const val ENTER_MS = 240
    const val MOVE_MS = 280
}

/** True when the user turned animations off (developer options or accessibility "remove animations"). */
val LocalReducedMotion = staticCompositionLocalOf { false }

private val TldhColorScheme: ColorScheme = darkColorScheme(
    primary = TldhColors.Accent,
    onPrimary = TldhColors.Text,
    secondary = TldhColors.Glow,
    onSecondary = TldhColors.Text,
    background = TldhColors.Background,
    onBackground = TldhColors.Text,
    surface = TldhColors.Surface,
    onSurface = TldhColors.Text,
    surfaceVariant = TldhColors.SurfaceRaised,
    onSurfaceVariant = TldhColors.TextMuted,
    surfaceContainerHigh = TldhColors.SurfaceRaised,
    outline = TldhColors.Muted,
    outlineVariant = TldhColors.Divider,
    error = TldhColors.Danger,
    onError = TldhColors.Background
)

@Composable
fun TldhTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reducedMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    MaterialTheme(
        colorScheme = TldhColorScheme,
        typography = Typography(
            bodyLarge = TldhType.Body,
            bodyMedium = TldhType.Small,
            bodySmall = TldhType.Caption,
            labelLarge = TldhType.Label,
            titleLarge = TldhType.Title,
            headlineSmall = TldhType.Title
        )
    ) {
        CompositionLocalProvider(
            LocalContentColor provides TldhColors.Text,
            LocalTextStyle provides TldhType.Body,
            LocalReducedMotion provides reducedMotion,
            content = content
        )
    }
}
