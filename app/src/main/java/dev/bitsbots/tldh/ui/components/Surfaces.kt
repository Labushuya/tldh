package dev.bitsbots.tldh.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import dev.bitsbots.tldh.ui.theme.TldhColors
import dev.bitsbots.tldh.ui.theme.TldhGlass
import dev.bitsbots.tldh.ui.theme.TldhShapes
import dev.bitsbots.tldh.ui.theme.TldhType

/** A grouped surface (one step above the background) with a lit glass rim. */
@Composable
fun Group(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(TldhShapes.Group)
            .background(TldhColors.Surface)
            .border(1.dp, TldhGlass.SurfaceEdge, TldhShapes.Group),
        content = content
    )
}

@Composable
fun GroupDivider(inset: Dp = 56.dp) {
    Box(
        Modifier
            .padding(start = inset)
            .fillMaxWidth()
            .height(1.dp)
            .background(TldhColors.Divider)
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = TldhType.Label,
        color = TldhColors.TextMuted,
        modifier = modifier
            .padding(start = 4.dp)
            .semantics { heading() }
    )
}

/** Icon, title, optional subtitle and trailing content; clickable when [onClick] is set. */
@Composable
fun IconRow(
    icon: ImageVector?,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color = TldhColors.TextMuted,
    titleStyle: TextStyle = TldhType.Row,
    titleColor: Color = TldhColors.Text,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val clickModifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = if (subtitle != null) Alignment.Top else Alignment.CenterVertically
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = titleStyle, color = titleColor)
            if (subtitle != null) Text(subtitle, style = TldhType.Caption, color = TldhColors.TextMuted)
        }
        if (trailing != null) trailing()
    }
}

/** "tl;dh" with the semicolon in the glow tone. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, style: TextStyle = TldhType.Wordmark, semicolon: Color = TldhColors.Glow) {
    Text(
        text = buildAnnotatedString {
            append("tl")
            withStyle(SpanStyle(color = semicolon)) { append(";") }
            append("dh")
        },
        style = style,
        modifier = modifier.semantics { contentDescription = "tl;dh" }
    )
}

/**
 * The app mark: three bars getting shorter and ending in a dot. Same geometry as the launcher
 * icon (108 dp grid, 72 dp visible).
 */
@Composable
fun TldhMark(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val unit = this.size.width / 72f
        fun p(value: Float) = (value - 18f) * unit
        val squircle = Path().apply {
            moveTo(p(54f), p(18f))
            cubicTo(p(80f), p(18f), p(90f), p(28f), p(90f), p(54f))
            cubicTo(p(90f), p(80f), p(80f), p(90f), p(54f), p(90f))
            cubicTo(p(28f), p(90f), p(18f), p(80f), p(18f), p(54f))
            cubicTo(p(18f), p(28f), p(28f), p(18f), p(54f), p(18f))
            close()
        }
        drawPath(squircle, TldhColors.Accent)
        drawMarkGlyph(unit, originOffset = 18f, color = TldhColors.Text)
    }
}

/** Bars and dot of the mark in 108-grid units, shifted by [originOffset] grid units. */
fun DrawScope.drawMarkGlyph(unit: Float, originOffset: Float, color: Color) {
    val bars = listOf(
        floatArrayOf(32f, 38f, 32f),
        floatArrayOf(44.2f, 43.2f, 21.6f),
        floatArrayOf(56.4f, 48.6f, 10.8f)
    )
    bars.forEach { (x, y, height) ->
        drawRoundRect(
            color = color,
            topLeft = Offset((x - originOffset) * unit, (y - originOffset) * unit),
            size = Size(7f * unit, height * unit),
            cornerRadius = CornerRadius(3.5f * unit)
        )
    }
    drawCircle(color, radius = 4f * unit, center = Offset((72f - originOffset) * unit, (54f - originOffset) * unit))
}

/** Second cue besides color for uncertain words: dotted = unsure, wavy = very unsure. */
enum class MarkStyle { Dotted, Wavy }

/** A marked range; [tag] lets a tap handler find what was marked (e.g. the word index). */
data class TextMark(val start: Int, val end: Int, val style: MarkStyle, val tag: Int = -1)

fun markColor(style: MarkStyle): Color = when (style) {
    MarkStyle.Dotted -> TldhColors.Unsure
    MarkStyle.Wavy -> TldhColors.Danger
}

/** Marked words read in full text color inside muted text; tint and underline are drawn by MarkedText. */
val MarkEmphasis = SpanStyle(color = TldhColors.Text)

private fun markTint(style: MarkStyle): Color = markColor(style).copy(alpha = if (style == MarkStyle.Wavy) 0.18f else 0.15f)

/**
 * Text whose [marks] get a dotted or wavy underline drawn under the baseline.
 *
 * Deliberately no LinkAnnotation per mark: with hundreds of marked words (a long voice note) the
 * link machinery re-laid out the whole text per link and froze the screen for over a minute. Taps
 * go through one gesture handler that maps the touch position to a character offset instead, and
 * the tint and underline geometry is computed once per text layout, not on every frame. Tints are
 * drawn rather than set as span backgrounds, so the text itself stays cheap to lay out.
 */
@Composable
fun MarkedText(
    text: AnnotatedString,
    marks: List<TextMark>,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = TldhColors.Text,
    maxLines: Int = Int.MAX_VALUE,
    onMarkTap: ((TextMark) -> Unit)? = null
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val geometry = remember { MarkGeometryCache() }
    val currentOnTap by rememberUpdatedState(onMarkTap)
    val tapModifier = if (onMarkTap != null && marks.isNotEmpty()) {
        Modifier.pointerInput(marks) {
            detectTapGestures { position ->
                val result = layout ?: return@detectTapGestures
                val offset = result.getOffsetForPosition(position)
                marks.firstOrNull { offset >= it.start && offset <= it.end }?.let { currentOnTap?.invoke(it) }
            }
        }
    } else {
        Modifier
    }
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout = it },
        modifier = modifier
            .then(tapModifier)
            .drawBehind {
                val result = layout ?: return@drawBehind
                val fontPx = if (style.fontSize.isSpecified) style.fontSize.toPx() else 16.dp.toPx()
                geometry.get(result, marks).forEach { drawMark(it, fontPx) }
            }
    )
}

private class MarkSegment(val left: Float, val right: Float, val baseline: Float, val style: MarkStyle)

/** Recomputes underline positions only when the layout or the marks change. */
private class MarkGeometryCache {
    private var layout: TextLayoutResult? = null
    private var marks: List<TextMark>? = null
    private var segments: List<MarkSegment> = emptyList()

    fun get(layout: TextLayoutResult, marks: List<TextMark>): List<MarkSegment> {
        if (layout !== this.layout || marks !== this.marks) {
            this.layout = layout
            this.marks = marks
            segments = marks.mapNotNull { segmentOf(layout, it) }
        }
        return segments
    }

    private fun segmentOf(layout: TextLayoutResult, mark: TextMark): MarkSegment? {
        if (layout.lineCount == 0 || mark.end <= mark.start) return null
        if (mark.end > layout.layoutInput.text.length) return null
        val lastVisible = layout.getLineEnd(layout.lineCount - 1, visibleEnd = true)
        if (mark.start >= lastVisible) return null
        val line = layout.getLineForOffset(mark.start)
        val endLine = layout.getLineForOffset(mark.end - 1)
        val left = layout.getHorizontalPosition(mark.start, usePrimaryDirection = true)
        val right = if (endLine == line) layout.getHorizontalPosition(mark.end, usePrimaryDirection = true) else layout.getLineRight(line)
        if (!(right > left) || right - left > layout.size.width + 1f) return null
        // Baseline offset is the same on every line because the text uses a single style.
        val baseline = layout.getLineTop(line) + (layout.firstBaseline - layout.getLineTop(0))
        return MarkSegment(left, right, baseline, mark.style)
    }
}

private fun DrawScope.drawMark(segment: MarkSegment, fontPx: Float) {
    val left = segment.left
    val right = segment.right
    val pad = 1.5.dp.toPx()
    drawRoundRect(
        color = markTint(segment.style),
        topLeft = Offset(left - pad, segment.baseline - fontPx * 0.92f),
        size = Size(right - left + 2 * pad, fontPx * 1.22f),
        cornerRadius = CornerRadius(3.dp.toPx())
    )
    val y = segment.baseline + 3.dp.toPx()
    val color = markColor(segment.style)
    when (segment.style) {
        MarkStyle.Dotted -> {
            val radius = 1.1.dp.toPx()
            val step = 4.dp.toPx()
            var x = left + radius
            while (x <= right - radius) {
                drawCircle(color, radius, Offset(x, y))
                x += step
            }
        }
        MarkStyle.Wavy -> {
            val amplitude = 1.6.dp.toPx()
            val half = 2.5.dp.toPx()
            val path = Path().apply {
                moveTo(left, y)
                var x = left
                var up = true
                while (x < right) {
                    val next = minOf(x + half, right)
                    val peak = if (up) y - amplitude else y + amplitude
                    cubicTo(x + (next - x) / 3f, peak, x + 2f * (next - x) / 3f, peak, next, y)
                    x = next
                    up = !up
                }
            }
            drawPath(path, color, style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}
