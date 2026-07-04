package dev.bitsbots.tldh.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bitsbots.tldh.audio.AudioIngestor
import dev.bitsbots.tldh.session.SessionManager
import dev.bitsbots.tldh.share.ShareIntentReader
import dev.bitsbots.tldh.summarization.AudioSummary
import dev.bitsbots.tldh.summarization.FakeSummaryEngine
import dev.bitsbots.tldh.ui.theme.TldhBackground
import dev.bitsbots.tldh.ui.theme.TldhDanger
import dev.bitsbots.tldh.ui.theme.TldhHotPurple
import dev.bitsbots.tldh.ui.theme.TldhPurple
import dev.bitsbots.tldh.ui.theme.TldhSuccess
import dev.bitsbots.tldh.ui.theme.TldhSurface
import dev.bitsbots.tldh.ui.theme.TldhTextMuted

sealed interface TldhUiState {
    data object Idle : TldhUiState
    data object Processing : TldhUiState
    data class Result(val summary: AudioSummary) : TldhUiState
    data class Error(val message: String) : TldhUiState
}

@Composable
fun TldhApp(
    currentIntent: Intent?,
    sessionManager: SessionManager,
    updaterEnabled: Boolean,
    appVersion: String,
    channel: String
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<TldhUiState>(TldhUiState.Idle) }

    LaunchedEffect(currentIntent) {
        val sharedAudio = ShareIntentReader.read(currentIntent)
        if (sharedAudio != null) {
            state = TldhUiState.Processing
            sessionManager.newSessionId()
            state = runCatching {
                val metadata = AudioIngestor(context).inspect(sharedAudio)
                TldhUiState.Result(FakeSummaryEngine().summarize(metadata))
            }.getOrElse { error ->
                TldhUiState.Error(error.message ?: "Audio konnte nicht gelesen werden.")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TldhBackground)
    ) {
        PulsatingPurpleLines()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(appVersion = appVersion, channel = channel, updaterEnabled = updaterEnabled)
            Spacer(Modifier.height(28.dp))
            Box(Modifier.widthIn(max = 920.dp)) {
                when (val current = state) {
                    TldhUiState.Idle -> IdleCard(updaterEnabled)
                    TldhUiState.Processing -> ProcessingCard()
                    is TldhUiState.Result -> ResultCard(current.summary, sessionManager)
                    is TldhUiState.Error -> ErrorCard(current.message)
                }
            }
        }
    }
}

@Composable
private fun Header(appVersion: String, channel: String, updaterEnabled: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("tl;dh", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
        Text("too long; didn't hear", color = TldhTextMuted, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "v$appVersion · $channel${if (updaterEnabled) " · manual stable updater" else " · fully offline"}",
            color = if (updaterEnabled) TldhHotPurple else TldhSuccess,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun IdleCard(updaterEnabled: Boolean) {
    TldhCard {
        Text("Audio rein. Punkt raus.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Teile eine WhatsApp-Sprachnotiz an tl;dh. Diese Bootstrap-Version beweist zuerst den Share-Target-Flow und ersetzt die echte Transkription noch durch eine Fake-Auswertung.",
            color = TldhTextMuted
        )
        Spacer(Modifier.height(18.dp))
        Text(
            if (updaterEnabled) "Updater-Flavor: Update-Checks sind manuell vorgesehen. Keine Hintergrunddienste." else "Offline-Flavor: keine Internet-Permission, keine Update-Prüfung in der App.",
            color = if (updaterEnabled) TldhHotPurple else TldhSuccess
        )
    }
}

@Composable
private fun ProcessingCard() {
    TldhCard {
        Text("Audio wird lokal geprüft …", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text("Keine Cloud. Keine Telemetrie. Keine persistente Session.", color = TldhTextMuted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultCard(summary: AudioSummary, sessionManager: SessionManager) {
    val context = LocalContext.current
    TldhCard {
        Text("TL;DR", color = TldhHotPurple, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(summary.tldr, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(18.dp))

        Text("Kernaussagen", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        summary.keyPoints.forEach { point ->
            Text("• $point", color = TldhTextMuted)
        }

        Spacer(Modifier.height(18.dp))
        Text("Tags", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            summary.tags.forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
        }

        Spacer(Modifier.height(18.dp))
        Text("Antwortvorschläge", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        summary.replySuggestions.forEach { reply ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1D1230)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(reply.tone, color = TldhHotPurple, fontWeight = FontWeight.Bold)
                    Text(reply.text, color = TldhTextMuted)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { context.copyToClipboard(reply.text) }) { Text("Copy") }
                }
            }
        }

        if (summary.warnings.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text("Hinweise", color = TldhDanger, fontWeight = FontWeight.Bold)
            summary.warnings.forEach { Text("• $it", color = TldhTextMuted) }
        }

        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { context.copyToClipboard(formatSummary(summary)) }) { Text("Copy All") }
            OutlinedButton(onClick = { sessionManager.wipeCurrentSession() }) { Text("Delete Session") }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    TldhCard {
        Text("Konnte Audio nicht lesen", color = TldhDanger, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = TldhTextMuted)
    }
}

@Composable
private fun TldhCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = TldhSurface.copy(alpha = 0.92f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp), content = { content() })
    }
}

@Composable
private fun PulsatingPurpleLines() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(animation = tween(2600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "lineAlpha"
    )
    Canvas(Modifier.fillMaxSize()) {
        val brush = Brush.linearGradient(
            colors = listOf(TldhPurple.copy(alpha = pulse), TldhHotPurple.copy(alpha = pulse * 0.8f), Color.Transparent),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
        for (i in 0..5) {
            val y = size.height * (0.12f + i * 0.16f)
            drawLine(
                brush = brush,
                start = Offset(-80f, y),
                end = Offset(size.width + 80f, y + (if (i % 2 == 0) 120f else -90f)),
                strokeWidth = 2.2f
            )
        }
    }
}

private fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("tl;dh", text))
}

private fun formatSummary(summary: AudioSummary): String = buildString {
    appendLine("tl;dh — too long; didn't hear")
    appendLine()
    appendLine("TL;DR")
    appendLine(summary.tldr)
    appendLine()
    appendLine("Kernaussagen")
    summary.keyPoints.forEach { appendLine("- $it") }
    appendLine()
    appendLine("Tags: ${summary.tags.joinToString(", ")}")
    summary.category?.let { appendLine("Kategorie: $it") }
    appendLine()
    appendLine("Antwortvorschläge")
    summary.replySuggestions.forEach { appendLine("[${it.tone}] ${it.text}") }
}
