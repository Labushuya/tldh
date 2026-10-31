package dev.bitsbots.tldh.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

data class TextMark(val start: Int, val end: Int, val style: MarkStyle)

fun markColor(style: MarkStyle): Color = when (style) {
    MarkStyle.Dotted -> TldhColors.Unsure
    MarkStyle.Wavy -> TldhColors.Danger
}

fun markBackground(style: MarkStyle): SpanStyle = SpanStyle(
    color = TldhColors.Text,
    background = markColor(style).copy(alpha = if (style == MarkStyle.Wavy) 0.18f else 0.15f)
)

/** Text whose [marks] get a dotted or wavy underline drawn under the baseline. */
@Composable
fun MarkedText(
    text: AnnotatedString,
    marks: List<TextMark>,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = TldhColors.Text,
    maxLines: Int = Int.MAX_VALUE
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout = it },
        modifier = modifier.drawBehind {
            val result = layout ?: return@drawBehind
            marks.forEach { mark -> drawMark(result, mark) }
        }
    )
}

private fun DrawScope.drawMark(layout: TextLayoutResult, mark: TextMark) {
    if (layout.lineCount == 0 || mark.end <= mark.start) return
    val textLength = layout.layoutInput.text.length
    if (mark.end > textLength) return
    val lastVisible = layout.getLineEnd(layout.lineCount - 1, visibleEnd = true)
    if (mark.start >= lastVisible) return
    val line = layout.getLineForOffset(mark.start)
    val endLine = layout.getLineForOffset(mark.end - 1)
    val left = layout.getHorizontalPosition(mark.start, usePrimaryDirection = true)
    val right = if (endLine == line) layout.getHorizontalPosition(mark.end, usePrimaryDirection = true) else layout.getLineRight(line)
    if (right <= left) return
    // Baseline offset is the same on every line because the text uses a single style.
    val baseline = layout.getLineTop(line) + (layout.firstBaseline - layout.getLineTop(0))
    val y = baseline + 3.dp.toPx()
    val color = markColor(mark.style)
    when (mark.style) {
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
