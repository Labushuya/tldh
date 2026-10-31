package dev.bitsbots.tldh.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.bitsbots.tldh.ui.components.RoundIconButton
import dev.bitsbots.tldh.ui.theme.LocalReducedMotion
import dev.bitsbots.tldh.ui.theme.TldhColors
import dev.bitsbots.tldh.ui.theme.TldhShapes
import dev.bitsbots.tldh.ui.theme.TldhType
import dev.bitsbots.tldh.ui.theme.tabular

/** Top bar content: leading close/back icon, title (and subtitle), trailing actions. */
@Composable
fun ScreenTitleBar(
    icon: ImageVector,
    iconDescription: String,
    title: String,
    subtitle: String?,
    onIcon: () -> Unit,
    large: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RoundIconButton(icon = icon, contentDescription = iconDescription, onClick = onIcon)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = if (large) TldhType.Title else TldhType.BodyStrong.copy(lineHeight = TldhType.Row.lineHeight),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = TldhType.Caption.tabular(),
                    color = TldhColors.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        actions()
    }
}

/** Three bouncing bars while a snippet plays; static when animations are off. */
@Composable
fun EqualizerBars(color: Color, modifier: Modifier = Modifier, height: Dp = 14.dp) {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "eq")
    val levels = (0 until 3).map { index ->
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(420 + index * 90), RepeatMode.Reverse, initialStartOffset = androidx.compose.animation.core.StartOffset(index * 150)),
            label = "eq$index"
        )
    }
    Canvas(modifier.size(width = 13.dp, height = height)) {
        val bar = 3.dp.toPx()
        val gap = 2.dp.toPx()
        levels.forEachIndexed { index, level ->
            val value = if (reduced) 0.7f else level.value
            val barHeight = size.height * value
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (bar + gap), size.height - barHeight),
                size = Size(bar, barHeight),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
        }
    }
}

/** Outlined chip for dates, times and amounts. */
@Composable
fun InfoChip(text: String) {
    Text(
        text = text,
        style = TldhType.Small.tabular(),
        modifier = Modifier
            .clip(TldhShapes.Group)
            .background(TldhColors.Surface)
            .border(1.dp, TldhColors.Divider, TldhShapes.Group)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
