package dev.bitsbots.tldh.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.bitsbots.tldh.ui.ProcessingInfo
import dev.bitsbots.tldh.ui.ProcessingStage
import dev.bitsbots.tldh.ui.UiText
import dev.bitsbots.tldh.ui.components.GlassScaffold
import dev.bitsbots.tldh.ui.components.Group
import dev.bitsbots.tldh.ui.components.GroupDivider
import dev.bitsbots.tldh.ui.components.OutlineButton
import dev.bitsbots.tldh.ui.components.ProgressWaveform
import dev.bitsbots.tldh.ui.components.TldhIcons
import dev.bitsbots.tldh.ui.theme.LocalReducedMotion
import dev.bitsbots.tldh.ui.theme.TldhColors
import dev.bitsbots.tldh.ui.theme.TldhMotion
import dev.bitsbots.tldh.ui.theme.TldhType
import dev.bitsbots.tldh.ui.theme.tabular
import java.util.Locale

private enum class StepState { Done, Active, Pending }

@Composable
fun ProcessingScreen(info: ProcessingInfo, onCancel: () -> Unit) {
    GlassScaffold(
        topBar = { ScreenTitleBar(icon = TldhIcons.Close, iconDescription = "Abbrechen", title = info.title, subtitle = info.subtitle, onIcon = onCancel) },
        bottomBar = {
            OutlineButton(
                text = "Abbrechen",
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Wird kurz gemacht …",
                    style = TldhType.Headline,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    when (info.stage) {
                        ProcessingStage.Prepare -> "Audio wird vorbereitet. Bitte die App geöffnet lassen."
                        ProcessingStage.Transcribe -> UiText.remainingText(info.remainingSeconds) + " Bitte die App geöffnet lassen."
                        ProcessingStage.Summarize -> "Gleich fertig."
                    },
                    style = TldhType.Row.tabular(),
                    color = TldhColors.TextMuted
                )
            }
            ProgressWaveform(
                levels = info.levels,
                progress = when (info.stage) {
                    ProcessingStage.Prepare -> 0f
                    ProcessingStage.Transcribe -> info.progress / 100f
                    ProcessingStage.Summarize -> 1f
                }
            )
            Group(Modifier.padding(vertical = 0.dp)) {
                StepRow(
                    label = "Audio vorbereiten",
                    state = if (info.stage == ProcessingStage.Prepare) StepState.Active else StepState.Done,
                    trailing = info.prepareMs?.let { "%.1f s".format(Locale.GERMANY, it / 1000.0) }
                )
                GroupDivider()
                val transcribeState = when (info.stage) {
                    ProcessingStage.Prepare -> StepState.Pending
                    ProcessingStage.Transcribe -> StepState.Active
                    ProcessingStage.Summarize -> StepState.Done
                }
                StepRow(
                    label = "Transkribieren",
                    state = transcribeState,
                    trailing = if (transcribeState == StepState.Active) "${info.progress} %" else null,
                    progress = if (transcribeState == StepState.Active) info.progress / 100f else null
                )
                GroupDivider()
                StepRow(
                    label = "Zusammenfassen",
                    state = if (info.stage == ProcessingStage.Summarize) StepState.Active else StepState.Pending,
                    trailing = null
                )
            }
            AnimatedVisibility(visible = info.liveText.isNotBlank(), enter = fadeIn(tween(TldhMotion.ENTER_MS, easing = TldhMotion.Enter))) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Schon erkannt", style = TldhType.Label, color = TldhColors.TextMuted, modifier = Modifier.semantics { heading() })
                    Text(UiText.livePreview(info.liveText), style = TldhType.Body.copy(lineHeight = TldhType.Transcript.lineHeight))
                }
            }
        }
    }
}

@Composable
private fun StepRow(label: String, state: StepState, trailing: String?, progress: Float? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StepIcon(state)
            Text(
                label,
                style = if (state == StepState.Active) TldhType.Row.copy(fontWeight = TldhType.Label.fontWeight) else TldhType.Row,
                color = if (state == StepState.Pending) TldhColors.TextMuted else TldhColors.Text,
                modifier = Modifier.weight(1f)
            )
            if (trailing != null) {
                Text(
                    trailing,
                    style = (if (state == StepState.Active) TldhType.Row.copy(fontWeight = TldhType.Label.fontWeight) else TldhType.Caption).tabular(),
                    color = if (state == StepState.Active) TldhColors.Text else TldhColors.TextMuted
                )
            }
        }
        if (progress != null) LinearProgress(progress, Modifier.padding(start = 36.dp))
    }
}

@Composable
private fun StepIcon(state: StepState) {
    when (state) {
        StepState.Done -> Icon(TldhIcons.Check, contentDescription = "erledigt", tint = TldhColors.Success, modifier = Modifier.size(22.dp))
        StepState.Active -> Spinner()
        StepState.Pending -> Canvas(Modifier.size(22.dp)) {
            drawCircle(TldhColors.Muted, radius = size.minDimension / 2f - 2.dp.toPx(), style = Stroke(width = 2.2.dp.toPx()))
        }
    }
}

@Composable
fun Spinner(modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "spinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "rotation"
    )
    Canvas(modifier.size(22.dp)) {
        val stroke = 2.2.dp.toPx()
        val inset = 2.dp.toPx()
        drawCircle(TldhColors.Muted, radius = size.minDimension / 2f - inset, style = Stroke(width = stroke))
        drawArc(
            color = TldhColors.Glow,
            startAngle = (if (reduced) 0f else rotation) - 90f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - 2 * inset, size.height - 2 * inset),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun LinearProgress(progress: Float, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(if (reduced) 0 else 400, easing = TldhMotion.Enter),
        label = "linearProgress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
            .background(TldhColors.Divider)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f) }
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(4.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                .background(TldhColors.Glow)
        )
    }
}
