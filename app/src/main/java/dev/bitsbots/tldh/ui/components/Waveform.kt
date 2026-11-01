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
import androidx.compose.ui.graphics.lerp
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
 * Processing waveform from the real loudness of the voice note, as "long story, short" in motion:
 * transcribed audio is squeezed into a dense glowing strip, so the line gets shorter as whisper
 * advances; the 30 s window whisper is working on shimmers; a blinking frontier marks the position.
 * When [finishing], the strip contracts into the tl;dh mark (three bars and a dot).
 */
@Composable
fun ProgressWaveform(
    levels: List<Float>,
    progress: Float,
    modifier: Modifier = Modifier,
    durationMs: Long? = null,
    finishing: Boolean = false
) {
    val reduced = LocalReducedMotion.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(if (reduced) 0 else 600, easing = TldhMotion.Enter),
        label = "progress"
    )
    val morph by animateFloatAsState(
        targetValue = if (finishing) 1f else 0f,
        animationSpec = tween(if (reduced) 0 else 420, easing = TldhMotion.Move),
        label = "morph"
    )
    val transition = rememberInfiniteTransition(label = "frontier")
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "blink"
    )
    val shimmer by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(900, easing = TldhMotion.Move), RepeatMode.Reverse),
        label = "shimmer"
    )
    Canvas(
        modifier
            .fillMaxWidth()
            .height(96.dp)
            .clearAndSetSemantics { }
    ) {
        val sizes = CompressionSizes(
            pitch = 6.dp.toPx(),
            barWidth = 3.dp.toPx(),
            compressedPitch = 2.4.dp.toPx(),
            compressedWidth = 1.6.dp.toPx(),
            compressedHeightScale = 0.62f,
            markBarWidth = 6.dp.toPx(),
            markGap = 3.dp.toPx(),
            markHeights = listOf(44.dp.toPx(), 28.dp.toPx(), 14.dp.toPx()),
            dotGap = 4.dp.toPx(),
            dotRadius = 3.5.dp.toPx()
        )
        val count = floor((size.width + sizes.pitch - sizes.barWidth) / sizes.pitch).toInt().coerceAtLeast(1)
        val minHeight = 10.dp.toPx()
        val range = size.height - 20.dp.toPx() - minHeight
        val heights = List(count) { i ->
            val level = if (levels.isNotEmpty()) {
                levels[(i * levels.size / count).coerceIn(0, levels.size - 1)]
            } else {
                idlePattern[i % idlePattern.size] / 80f * 0.6f
            }
            minHeight + range * level
        }
        val frame = CompressionLayout.frame(heights, animatedProgress, morph, size.width, sizes)
        // whisper decodes in 30 s windows: the bars of the window in progress shimmer.
        val windowBars = durationMs?.takeIf { it > 0 }?.let { (count * 30_000f / it).coerceAtLeast(1f) } ?: 0f
        val windowStart = animatedProgress * count
        val centerY = size.height / 2f
        frame.bars.forEachIndexed { i, bar ->
            val inWindow = !reduced && morph == 0f && animatedProgress in 0.0001f..0.9999f &&
                i >= windowStart && i < windowStart + windowBars
            val color = when {
                bar.processed > 0f -> lerp(TldhColors.Muted, TldhColors.Glow, bar.processed)
                inWindow -> lerp(TldhColors.Muted, TldhColors.Glow, shimmer)
                else -> TldhColors.Muted
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(bar.x, centerY - bar.height / 2f),
                size = Size(bar.width, bar.height),
                cornerRadius = CornerRadius(bar.width / 2f)
            )
        }
        if (morph > 0f) {
            drawCircle(
                color = TldhColors.Glow.copy(alpha = morph),
                radius = sizes.dotRadius * morph,
                center = Offset(frame.dotX, centerY)
            )
        } else if (animatedProgress > 0f && animatedProgress < 1f) {
            drawRoundRect(
                color = TldhColors.Text.copy(alpha = if (reduced) 1f else blink),
                topLeft = Offset(frame.frontierX - 1.dp.toPx(), 4.dp.toPx()),
                size = Size(2.dp.toPx(), size.height - 8.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
        }
    }
}
