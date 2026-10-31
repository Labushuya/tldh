package dev.bitsbots.tldh.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import dev.bitsbots.tldh.ui.theme.LocalReducedMotion
import dev.bitsbots.tldh.ui.theme.TldhColors
import dev.bitsbots.tldh.ui.theme.TldhMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor

private val idlePattern = intArrayOf(
    22, 38, 30, 54, 70, 44, 62, 80, 52, 34, 58, 74, 48, 28, 40, 66, 56, 36, 24, 46,
    60, 42, 30, 50, 38, 26, 34, 58, 72, 44, 26, 38, 52, 30, 20, 40, 58, 36, 24, 32
)

/**
 * The start screen's long voice note: muted bars breathe slowly, then the line compresses into
 * the accent mark (three bars and a dot) - "long story, short".
 */
@Composable
fun BreathingWaveform(modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "breathe")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "phase"
    )
    Canvas(
        modifier
            .fillMaxWidth()
            .height(88.dp)
            .clearAndSetSemantics { }
    ) {
        val t = if (reduced) 0f else phase
        val bar = 3.dp.toPx()
        val pitch = 6.dp.toPx()
        val markWidth = 3 * 6.dp.toPx() + 2 * 3.dp.toPx() + 4.dp.toPx() + 7.dp.toPx()
        val gapToMark = 14.dp.toPx()
        val count = floor((size.width - markWidth - gapToMark) / pitch).toInt().coerceAtLeast(0)
        val centerY = size.height / 2f
        for (i in 0 until count) {
            val offset = ((i * 97) % 1400) / 2400f
            val wave = 0.5f + 0.5f * cos(2f * PI.toFloat() * (t + offset))
            val taper = if (i >= count - 6) 0.45f + 0.55f * (count - i) / 7f else 1f
            val height = idlePattern[i % idlePattern.size].dp.toPx() * (0.5f + 0.5f * wave) * taper
            drawRoundRect(
                color = TldhColors.Muted,
                topLeft = Offset(i * pitch, centerY - height / 2f),
                size = Size(bar, height),
                cornerRadius = CornerRadius(bar / 2f)
            )
        }
        var x = count * pitch - (pitch - bar) + gapToMark
        listOf(44, 28, 14).forEach { h ->
            val height = h.dp.toPx()
            val width = 6.dp.toPx()
            drawRoundRect(
                color = TldhColors.Glow,
                topLeft = Offset(x, centerY - height / 2f),
                size = Size(width, height),
                cornerRadius = CornerRadius(width / 2f)
            )
            x += width + 3.dp.toPx()
        }
        drawCircle(TldhColors.Glow, radius = 3.5.dp.toPx(), center = Offset(x + 4.dp.toPx(), centerY))
    }
}

/**
 * Processing waveform from the real loudness of the voice note. Bars fill with the glow tone up
 * to the transcription progress; a blinking frontier marks where whisper currently is.
 */
@Composable
fun ProgressWaveform(levels: List<Float>, progress: Float, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(if (reduced) 0 else 420, easing = TldhMotion.Enter),
        label = "progress"
    )
    val transition = rememberInfiniteTransition(label = "frontier")
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "blink"
    )
    Canvas(
        modifier
            .fillMaxWidth()
            .height(96.dp)
            .clearAndSetSemantics { }
    ) {
        val bar = 3.dp.toPx()
        val pitch = 6.dp.toPx()
        val count = floor((size.width + pitch - bar) / pitch).toInt().coerceAtLeast(1)
        val done = (count * animatedProgress).toInt()
        val minHeight = 10.dp.toPx()
        val range = size.height - 20.dp.toPx() - minHeight
        val centerY = size.height / 2f
        for (i in 0 until count) {
            val level = if (levels.isNotEmpty()) {
                levels[(i * levels.size / count).coerceIn(0, levels.size - 1)]
            } else {
                idlePattern[i % idlePattern.size] / 80f * 0.6f
            }
            val height = minHeight + range * level
            drawRoundRect(
                color = if (i < done) TldhColors.Glow else TldhColors.Muted,
                topLeft = Offset(i * pitch, centerY - height / 2f),
                size = Size(bar, height),
                cornerRadius = CornerRadius(bar / 2f)
            )
        }
        if (animatedProgress > 0f && animatedProgress < 1f) {
            val x = (done * pitch - (pitch - bar) / 2f - 1.dp.toPx()).coerceAtLeast(0f)
            drawRoundRect(
                color = TldhColors.Text.copy(alpha = if (reduced) 1f else blink),
                topLeft = Offset(x, 4.dp.toPx()),
                size = Size(2.dp.toPx(), size.height - 8.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
        }
    }
}
