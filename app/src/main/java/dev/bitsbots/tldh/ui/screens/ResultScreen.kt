package dev.bitsbots.tldh.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import dev.bitsbots.tldh.analysis.CriticalFinding
import dev.bitsbots.tldh.analysis.ReplySuggestions
import dev.bitsbots.tldh.analysis.ReplyTone
import dev.bitsbots.tldh.audio.SnippetPlayer
import dev.bitsbots.tldh.summarization.AudioSummary
import dev.bitsbots.tldh.transcription.ConfidenceLevel
import dev.bitsbots.tldh.transcription.TranscriptWord
import dev.bitsbots.tldh.transcription.TranscriptionOutcome
import dev.bitsbots.tldh.ui.TldhUiState
import dev.bitsbots.tldh.ui.UiText
import dev.bitsbots.tldh.ui.components.GhostButton
import dev.bitsbots.tldh.ui.components.GlassEdge
import dev.bitsbots.tldh.ui.components.GlassPane
import dev.bitsbots.tldh.ui.components.GlassScaffold
import dev.bitsbots.tldh.ui.components.GlassState
import dev.bitsbots.tldh.ui.components.Group
import dev.bitsbots.tldh.ui.components.GroupDivider
import dev.bitsbots.tldh.ui.components.IconRow
import dev.bitsbots.tldh.ui.components.MarkStyle
import dev.bitsbots.tldh.ui.components.MarkedText
import dev.bitsbots.tldh.ui.components.OutlineButton
import dev.bitsbots.tldh.ui.components.Pill
import dev.bitsbots.tldh.ui.components.RoundIconButton
import dev.bitsbots.tldh.ui.components.SegmentedControl
import dev.bitsbots.tldh.ui.components.TextMark
import dev.bitsbots.tldh.ui.components.TldhIcons
import dev.bitsbots.tldh.ui.components.markBackground
import dev.bitsbots.tldh.ui.components.rememberGlassState
import dev.bitsbots.tldh.ui.components.rememberPressScale
import dev.bitsbots.tldh.ui.copyToClipboard
import dev.bitsbots.tldh.ui.theme.LocalReducedMotion
import dev.bitsbots.tldh.ui.theme.TldhColors
import dev.bitsbots.tldh.ui.theme.TldhGlass
import dev.bitsbots.tldh.ui.theme.TldhMotion
import dev.bitsbots.tldh.ui.theme.TldhShapes
import dev.bitsbots.tldh.ui.theme.TldhType
import dev.bitsbots.tldh.ui.theme.tabular
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class PlayingSnippet(val key: String, val label: String, val startMs: Long)

private const val MAX_CHECKS_SHOWN = 6

@Composable
fun ResultScreen(
    result: TldhUiState.Result,
    markUncertain: Boolean,
    tone: ReplyTone,
    onToneChange: (ReplyTone) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glass = rememberGlassState()
    val player = remember { SnippetPlayer() }
    DisposableEffect(player) { onDispose { player.stop() } }
    var playing by remember { mutableStateOf<PlayingSnippet?>(null) }
    var playJob by remember { mutableStateOf<Job?>(null) }
    var copied by remember { mutableStateOf<String?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied != null) {
            delay(1800)
            copied = null
        }
    }
    val summary = result.summary
    val transcription = result.transcription

    fun stop() {
        playJob?.cancel()
        player.stop()
        playing = null
    }

    fun play(key: String, word: TranscriptWord) {
        if (playing?.key == key) {
            stop()
            return
        }
        val pcm = transcription.pcmFile ?: return
        val start = word.startMs ?: return
        val end = word.endMs ?: return
        playJob?.cancel()
        playing = null
        playJob = scope.launch {
            val started = runCatching { player.play(pcm, transcription.sampleRate, start, end) }.isSuccess
            if (!started) return@launch
            playing = PlayingSnippet(key, cleanWord(word.text), start)
            delay((end - start + 1200L).coerceIn(600L, 8000L))
            playing = null
        }
    }

    fun copy(key: String, text: String) {
        context.copyToClipboard(text)
        copied = key
    }

    GlassScaffold(
        glass = glass,
        topBar = {
            ScreenTitleBar(
                icon = TldhIcons.Close,
                iconDescription = "Schließen und Session löschen",
                title = result.title,
                subtitle = result.subtitle,
                onIcon = onClose
            ) {
                RoundIconButton(
                    icon = if (copied == "all") TldhIcons.Check else TldhIcons.Copy,
                    contentDescription = if (copied == "all") "Alles kopiert" else "Alles kopieren",
                    onClick = { copy("all", formatSummary(summary)) },
                    tint = if (copied == "all") TldhColors.Success else TldhColors.TextMuted,
                    iconSize = 22.dp
                )
            }
        },
        overlay = {
            SnippetPane(
                playing = playing,
                glass = glass,
                onStop = { stop() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            TldrSection(summary, markUncertain)
            if (transcription.words.isNotEmpty()) {
                ChecksSection(
                    checks = summary.checks,
                    playingKey = playing?.key,
                    onPlay = { check -> play("check-${check.wordIndex}", check.word) }
                )
            }
            KeyPointsSection(summary)
            summary.replies?.let { replies ->
                RepliesSection(
                    replies = replies,
                    note = summary.note,
                    tone = tone,
                    onToneChange = onToneChange,
                    copied = copied,
                    onCopy = { key, text -> copy(key, text) }
                )
            }
            summary.transcript?.let { transcript ->
                TranscriptSection(
                    transcript = transcript,
                    transcription = transcription,
                    markUncertain = markUncertain,
                    onWordTap = { index, word -> play("word-$index", word) }
                )
            }
        }
        Column(
            modifier = Modifier.padding(top = 40.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GroupDivider(inset = 0.dp)
            Spacer(Modifier.height(14.dp))
            OutlineButton(
                text = "Session löschen",
                icon = TldhIcons.Trash,
                contentColor = TldhColors.Danger,
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Audio und Transkript werden auch beim Schließen gelöscht.",
                style = TldhType.Caption,
                color = TldhColors.TextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
            val rotation by animateFloatAsState(if (showDetails) 180f else 0f, tween(200, easing = TldhMotion.Enter), label = "details")
            GhostButton(
                text = "Technische Details",
                onClick = { showDetails = !showDetails },
                trailingIcon = TldhIcons.ChevronDown,
                trailingRotation = rotation,
                modifier = Modifier.padding(top = 8.dp)
            )
            AnimatedVisibility(visible = showDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(horizontal = 12.dp)) {
                    (summary.technicalDetails + summary.warnings).forEach { detail ->
                        Text("· $detail", style = TldhType.Caption, color = TldhColors.TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun TldrSection(summary: AudioSummary, markUncertain: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Kurz gesagt", style = TldhType.Small, color = TldhColors.TextMuted, modifier = Modifier.semantics { heading() })
        val spans = if (markUncertain) {
            UiText.marksIn(
                summary.tldr,
                summary.checks.map { Triple(it.sentence, it.word.text, it.word.confidence == ConfidenceLevel.SEHR_UNSICHER) }
            )
        } else {
            emptyList()
        }
        val marks = spans.map { TextMark(it.start, it.end, if (it.strong) MarkStyle.Wavy else MarkStyle.Dotted) }
        val text = buildAnnotatedString {
            append(summary.tldr)
            marks.forEach { addStyle(markBackground(it.style), it.start, it.end) }
        }
        MarkedText(text = text, marks = marks, style = TldhType.Tldr)
        Text("Wörtlich aus dem Transkript, nichts dazuerfunden", style = TldhType.Caption, color = TldhColors.TextMuted)
    }
}

/**
 * Meaning-critical words (negations, times, numbers, conditions, modal verbs) that whisper
 * recognized with low confidence, each with a button to hear that moment.
 */
@Composable
private fun ChecksSection(checks: List<CriticalFinding>, playingKey: String?, onPlay: (CriticalFinding) -> Unit) {
    Group {
        if (checks.isEmpty()) {
            IconRow(
                icon = TldhIcons.Check,
                iconTint = TldhColors.Success,
                title = "Nichts Kritisches unsicher erkannt",
                subtitle = "Verneinungen, Zeiten und Zahlen wurden sicher erkannt.",
                titleStyle = TldhType.BodyStrong
            )
        } else {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(TldhIcons.Warning, contentDescription = null, tint = TldhColors.Unsure, modifier = Modifier.padding(top = 1.dp).size(22.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (checks.size == 1) "1 Stelle prüfen" else "${checks.size} Stellen prüfen",
                        style = TldhType.BodyStrong,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text("Diese Wörter können die Aussage ändern und sind unsicher erkannt.", style = TldhType.Small, color = TldhColors.TextMuted)
                }
            }
            checks.take(MAX_CHECKS_SHOWN).forEach { check ->
                GroupDivider(inset = 20.dp)
                CheckRow(check, playing = playingKey == "check-${check.wordIndex}", onPlay = { onPlay(check) })
            }
            if (checks.size > MAX_CHECKS_SHOWN) {
                GroupDivider(inset = 20.dp)
                Text(
                    "+ ${checks.size - MAX_CHECKS_SHOWN} weitere im Transkript markiert",
                    style = TldhType.Caption,
                    color = TldhColors.TextMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun CheckRow(check: CriticalFinding, playing: Boolean, onPlay: () -> Unit) {
    val veryUnsure = check.word.confidence == ConfidenceLevel.SEHR_UNSICHER
    val word = cleanWord(check.word.text)
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("„$word“", style = TldhType.BodyStrong.copy(fontWeight = FontWeight.Bold))
                Pill(
                    text = "${check.category.label} · ${(check.word.probability * 100).toInt()} %",
                    color = if (veryUnsure) TldhColors.Danger else TldhColors.Unsure
                )
            }
            val range = UiText.findWord(check.sentence, check.word.text)
            val style = if (veryUnsure) MarkStyle.Wavy else MarkStyle.Dotted
            val marks = range?.let { listOf(TextMark(it.first, it.last + 1, style)) }.orEmpty()
            val text = buildAnnotatedString {
                append(check.sentence)
                marks.forEach { addStyle(markBackground(style), it.start, it.end) }
            }
            MarkedText(text = text, marks = marks, style = TldhType.Small, color = TldhColors.TextMuted)
        }
        check.word.startMs?.let { start ->
            PlayButton(time = UiText.timestamp(start), playing = playing, onClick = onPlay)
        }
    }
}

@Composable
private fun PlayButton(time: String, playing: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    val background by animateColorAsState(
        targetValue = if (playing) TldhColors.Accent else TldhColors.Accent.copy(alpha = 0f),
        animationSpec = tween(TldhMotion.PRESS_MS, easing = TldhMotion.Enter),
        label = "playBackground"
    )
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .widthIn(min = 72.dp)
            .height(48.dp)
            .clip(TldhShapes.Pill)
            .background(background)
            .border(1.dp, TldhColors.Muted, TldhShapes.Pill)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (playing) "Wiedergabe stoppen" else "Stelle bei $time abspielen" }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (playing) {
            EqualizerBars(color = TldhColors.Text)
        } else {
            Icon(TldhIcons.Play, contentDescription = null, tint = TldhColors.Text, modifier = Modifier.size(14.dp))
        }
        Text(time, style = TldhType.Label.tabular())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyPointsSection(summary: AudioSummary) {
    val sections = listOf(
        Triple("Fragen an dich", summary.questions, false),
        Triple("Termine & Zahlen", summary.datesAndNumbers, true),
        Triple("Aufgaben & Bitten", summary.tasks, false)
    ).filter { it.second.isNotEmpty() }
    if (sections.isEmpty()) return
    Column {
        sections.forEachIndexed { index, (title, items, chips) ->
            if (index > 0) GroupDivider(inset = 0.dp)
            Column(
                modifier = Modifier.padding(top = if (index > 0) 18.dp else 0.dp, bottom = if (index < sections.lastIndex) 18.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title, style = TldhType.BodyStrong, modifier = Modifier.semantics { heading() })
                if (chips) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items.forEach { InfoChip(it) }
                    }
                } else {
                    items.forEach { Text(it, style = TldhType.Body) }
                }
            }
        }
    }
}

/**
 * Reply suggestions for the detected kind of message, in three tones. Tapping a reply copies it,
 * ready to paste into the chat; the note button copies the content as a note.
 */
@Composable
private fun RepliesSection(
    replies: ReplySuggestions,
    note: String?,
    tone: ReplyTone,
    onToneChange: (ReplyTone) -> Unit,
    copied: String?,
    onCopy: (String, String) -> Unit
) {
    Group {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Antworten", style = TldhType.BodyStrong, modifier = Modifier.semantics { heading() })
                Text("Erkannt: ${replies.intent.label}", style = TldhType.Caption, color = TldhColors.TextMuted)
            }
            SegmentedControl(
                options = ReplyTone.entries,
                selected = tone,
                label = { it.label },
                onSelect = onToneChange
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                replies.byTone.getValue(tone).forEachIndexed { index, reply ->
                    val key = "reply-${tone.name}-$index"
                    ReplyButton(text = reply, copied = copied == key, onClick = { onCopy(key, reply) })
                }
            }
            Text(
                "Antippen kopiert. In WhatsApp ins Eingabefeld tippen und einfügen.",
                style = TldhType.Caption,
                color = TldhColors.TextMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            note?.let { text ->
                OutlineButton(
                    text = if (copied == "note") "Notiz kopiert" else "Als Notiz kopieren",
                    icon = if (copied == "note") TldhIcons.Check else TldhIcons.Note,
                    contentColor = if (copied == "note") TldhColors.Success else TldhColors.Text,
                    onClick = { onCopy("note", text) },
                    height = 48.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ReplyButton(text: String, copied: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction)
    val border by animateColorAsState(
        targetValue = if (copied) TldhColors.Success else TldhColors.Divider,
        animationSpec = tween(TldhMotion.ENTER_MS, easing = TldhMotion.Enter),
        label = "replyBorder"
    )
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(TldhShapes.Inner)
            .background(TldhColors.SurfaceRaised)
            .border(1.dp, border, TldhShapes.Inner)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClickLabel = "Antwort kopieren", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = TldhType.Row, modifier = Modifier.weight(1f))
        if (copied) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(TldhIcons.Check, contentDescription = null, tint = TldhColors.Success, modifier = Modifier.size(16.dp))
                Text("Kopiert", style = TldhType.Caption.copy(fontWeight = FontWeight.Medium), color = TldhColors.Success)
            }
        } else {
            Icon(TldhIcons.Copy, contentDescription = null, tint = TldhColors.TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * The transcript with uncertain words marked twice (tint plus dotted or wavy underline). Tapping
 * a marked word plays the moment around it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TranscriptSection(
    transcript: String,
    transcription: TranscriptionOutcome,
    markUncertain: Boolean,
    onWordTap: (Int, TranscriptWord) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val collapsible = transcript.length > 240
    val reduced = LocalReducedMotion.current
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(200, easing = TldhMotion.Enter), label = "chevron")
    val words = transcription.words
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Transkript", style = TldhType.BodyStrong, modifier = Modifier.semantics { heading() })
            if (collapsible) {
                GhostButton(
                    text = if (expanded) "Einklappen" else "Ganz anzeigen",
                    onClick = { expanded = !expanded },
                    trailingIcon = TldhIcons.ChevronDown,
                    trailingRotation = rotation
                )
            }
        }
        val maxLines = if (expanded || !collapsible) Int.MAX_VALUE else 4
        val sizeModifier = if (reduced) Modifier else Modifier.animateContentSize(tween(TldhMotion.MOVE_MS, easing = TldhMotion.Enter))
        val marks = mutableListOf<TextMark>()
        val text: AnnotatedString = if (words.isEmpty() || !markUncertain) {
            AnnotatedString(transcript)
        } else {
            buildAnnotatedString {
                words.forEachIndexed { index, word ->
                    if (index > 0) append(' ')
                    val style = when (word.confidence) {
                        ConfidenceLevel.SICHER -> null
                        ConfidenceLevel.UNSICHER -> MarkStyle.Dotted
                        ConfidenceLevel.SEHR_UNSICHER -> MarkStyle.Wavy
                    }
                    if (style == null) {
                        append(word.text)
                    } else {
                        val start = length
                        withLink(LinkAnnotation.Clickable(tag = "word-$index", styles = TextLinkStyles(style = markBackground(style))) { onWordTap(index, word) }) {
                            append(word.text)
                        }
                        marks += TextMark(start, length, style)
                    }
                }
            }
        }
        MarkedText(text = text, marks = marks, style = TldhType.Transcript, maxLines = maxLines, modifier = sizeModifier)
        if (marks.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LegendItem(word = "gepunktet", meaning = "unsicher", style = MarkStyle.Dotted)
                LegendItem(word = "wellig", meaning = "sehr unsicher", style = MarkStyle.Wavy)
                Text("Antippen spielt die Stelle ab", style = TldhType.Caption, color = TldhColors.TextMuted)
            }
        } else if (words.isNotEmpty() && markUncertain) {
            Text("Alle Wörter wurden sicher erkannt.", style = TldhType.Caption, color = TldhColors.TextMuted)
        }
    }
}

@Composable
private fun LegendItem(word: String, meaning: String, style: MarkStyle) {
    val text = buildAnnotatedString {
        append(word)
        addStyle(markBackground(style), 0, word.length)
        append(" $meaning")
    }
    MarkedText(text = text, marks = listOf(TextMark(0, word.length, style)), style = TldhType.Caption, color = TldhColors.TextMuted)
}

/** Floating glass pane while a snippet plays, with a stop button. */
@Composable
private fun SnippetPane(playing: PlayingSnippet?, glass: GlassState, onStop: () -> Unit, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf<PlayingSnippet?>(null) }
    LaunchedEffect(playing) { if (playing != null) last = playing }
    val reduced = LocalReducedMotion.current
    AnimatedVisibility(
        visible = playing != null,
        modifier = modifier,
        enter = fadeIn(tween(if (reduced) 0 else TldhMotion.ENTER_MS, easing = TldhMotion.Enter)) +
            slideInVertically(tween(if (reduced) 0 else TldhMotion.ENTER_MS, easing = TldhMotion.Enter)) { it / 2 },
        exit = fadeOut(tween(if (reduced) 0 else 180)) + slideOutVertically(tween(if (reduced) 0 else 180)) { it / 2 }
    ) {
        val shown = playing ?: last
        GlassPane(state = glass, shape = TldhShapes.Pill, tint = TldhGlass.PaneTint, edge = GlassEdge.Outline) {
            Row(
                modifier = Modifier.padding(start = 18.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EqualizerBars(color = TldhColors.Glow)
                Text(
                    shown?.let { "„${it.label}“ · ${UiText.timestamp(it.startMs)}" } ?: "",
                    style = TldhType.Label.tabular()
                )
                RoundIconButton(icon = TldhIcons.Stop, contentDescription = "Wiedergabe stoppen", onClick = onStop, iconSize = 18.dp)
            }
        }
    }
}

private fun cleanWord(text: String): String = text.trim().trimEnd(',', '.', '!', '?', ';', ':').trim('„', '“', '"')

private fun formatSummary(summary: AudioSummary): String = buildString {
    appendLine("tl;dh · Long story, short.")
    appendLine()
    appendLine("Kurz gesagt")
    appendLine(summary.tldr)
    if (summary.checks.isNotEmpty()) {
        appendLine()
        appendLine("Prüfen (unsicher erkannt)")
        summary.checks.forEach { appendLine("- ${it.category.label}: \"${cleanWord(it.word.text)}\" in: ${it.sentence}") }
    }
    listOf(
        "Fragen an dich" to summary.questions,
        "Termine & Zahlen" to summary.datesAndNumbers,
        "Aufgaben & Bitten" to summary.tasks
    ).filter { it.second.isNotEmpty() }.forEach { (title, items) ->
        appendLine()
        appendLine(title)
        items.forEach { appendLine("- $it") }
    }
    summary.transcript?.let {
        appendLine()
        appendLine("Transkript")
        appendLine(it)
    }
}
