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
import androidx.compose.foundation.Image
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bitsbots.tldh.R
import dev.bitsbots.tldh.audio.AudioIngestor
import dev.bitsbots.tldh.session.SessionManager
import dev.bitsbots.tldh.share.ShareIntentReader
import dev.bitsbots.tldh.summarization.AudioSummary
import dev.bitsbots.tldh.summarization.FakeSummaryEngine
import dev.bitsbots.tldh.transcription.LocalTranscriptionSpike
import dev.bitsbots.tldh.ui.theme.TldhBackground
import dev.bitsbots.tldh.ui.theme.TldhDanger
import dev.bitsbots.tldh.ui.theme.TldhGlow
import dev.bitsbots.tldh.ui.theme.TldhHotPurple
import dev.bitsbots.tldh.ui.theme.TldhPurple
import dev.bitsbots.tldh.ui.theme.TldhSuccess
import dev.bitsbots.tldh.ui.theme.TldhSurface
import dev.bitsbots.tldh.ui.theme.TldhTextMuted
import dev.bitsbots.tldh.updates.ApkInstaller
import dev.bitsbots.tldh.updates.GitHubReleaseClient
import dev.bitsbots.tldh.updates.StableReleaseSelector
import dev.bitsbots.tldh.updates.StableUpdate
import dev.bitsbots.tldh.updates.UpdateDownloadGuard
import kotlinx.coroutines.launch
import java.io.File

sealed interface TldhUiState {
    data object Idle : TldhUiState
    data object Processing : TldhUiState
    data class Result(val summary: AudioSummary) : TldhUiState
    data class Error(val message: String) : TldhUiState
}

private sealed interface ManualUpdateUiState {
    data object Idle : ManualUpdateUiState
    data object Checking : ManualUpdateUiState
    data object UpToDate : ManualUpdateUiState
    data class Available(val update: StableUpdate) : ManualUpdateUiState
    data class Downloading(val update: StableUpdate, val progress: Float) : ManualUpdateUiState
    data class ReadyToInstall(val update: StableUpdate, val apkFile: File) : ManualUpdateUiState
    data class Error(val message: String) : ManualUpdateUiState
}

@Composable
fun TldhApp(
    currentIntent: Intent?,
    sessionManager: SessionManager,
    updaterEnabled: Boolean,
    appVersion: String,
    channel: String,
    repositorySlug: String
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<TldhUiState>(TldhUiState.Idle) }

    LaunchedEffect(currentIntent) {
        val sharedAudio = ShareIntentReader.read(currentIntent)
        if (sharedAudio != null) {
            state = TldhUiState.Processing
            state = runCatching {
                val sessionId = sessionManager.newSessionId()
                val metadata = AudioIngestor(context).inspect(sharedAudio)
                val transcription = LocalTranscriptionSpike(
                    context = context,
                    workDir = File(context.cacheDir, sessionId)
                ).transcribe(sharedAudio, metadata)
                TldhUiState.Result(FakeSummaryEngine().summarize(metadata, transcription))
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
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(appVersion = appVersion, channel = channel)
            Spacer(Modifier.height(28.dp))
            Column(Modifier.widthIn(max = 920.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                when (val current = state) {
                    TldhUiState.Idle -> IdleCard()
                    TldhUiState.Processing -> ProcessingCard()
                    is TldhUiState.Result -> ResultCard(current.summary, sessionManager)
                    is TldhUiState.Error -> ErrorCard(current.message)
                }
                if (updaterEnabled) {
                    ManualUpdaterCard(appVersion = appVersion, repositorySlug = repositorySlug)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Header(appVersion: String, channel: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.tldh_logo),
            contentDescription = "tl;dh logo",
            modifier = Modifier.size(112.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("tl;dh", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
        Text("Long story, short.", color = TldhTextMuted, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "v$appVersion · $channel · manual stable updater",
            color = TldhHotPurple,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun IdleCard() {
    TldhCard {
        Text("Long story, short.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Teile eine WhatsApp-Sprachnotiz an tl;dh. Die App prüft Format, Dauer und versucht im Spike erstmals lokale Transkription.",
            color = TldhTextMuted
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "Eine App, eine APK. Core offline, Updates nur manuell.",
            color = TldhSuccess
        )
    }
}

@Composable
private fun ManualUpdaterCard(appVersion: String, repositorySlug: String) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<ManualUpdateUiState>(ManualUpdateUiState.Idle) }
    val repositoryConfigured = repositorySlug.contains('/')
    val downloadActive = updateState is ManualUpdateUiState.Downloading

    DisposableEffect(downloadActive, view) {
        val previous = view.keepScreenOn
        view.keepScreenOn = downloadActive || previous
        onDispose { view.keepScreenOn = previous }
    }

    TldhCard {
        Text("Updates", color = TldhHotPurple, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "tl;dh bleibt vollständig offline nutzbar. Diese Prüfung läuft nur, wenn du sie manuell startest und Internet vorhanden ist. Downloads laufen bewusst im Vordergrund; währenddessen hält tl;dh das Gerät wach.",
            color = TldhTextMuted
        )
        Spacer(Modifier.height(10.dp))
        Text("Installiert: v$appVersion", color = TldhTextMuted)
        Text("Repo: ${repositorySlug.ifBlank { "nicht konfiguriert" }}", color = TldhTextMuted)
        Spacer(Modifier.height(14.dp))

        when (val current = updateState) {
            ManualUpdateUiState.Idle -> {
                Button(
                    enabled = repositoryConfigured,
                    onClick = {
                        scope.launch {
                            updateState = ManualUpdateUiState.Checking
                            updateState = runCatching {
                                val client = GitHubReleaseClient(repositorySlug)
                                val releases = client.fetchStableReleases()
                                val update = StableReleaseSelector().select(appVersion, releases)
                                if (update == null) ManualUpdateUiState.UpToDate else ManualUpdateUiState.Available(update)
                            }.getOrElse { error -> ManualUpdateUiState.Error(error.message ?: "Update-Prüfung fehlgeschlagen.") }
                        }
                    }
                ) { Text("Nach stabilem Update suchen") }
                if (!repositoryConfigured) {
                    Spacer(Modifier.height(8.dp))
                    Text("GitHub Repository wurde im Build nicht gesetzt. Release-Builds aus GitHub Actions konfigurieren das automatisch.", color = TldhDanger)
                }
            }

            ManualUpdateUiState.Checking -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Prüfe stabile GitHub Releases …", color = TldhTextMuted)
            }

            ManualUpdateUiState.UpToDate -> {
                Text("Du nutzt bereits den aktuellsten stabilen Release.", color = TldhSuccess)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { updateState = ManualUpdateUiState.Idle }) { Text("Erneut prüfen") }
            }

            is ManualUpdateUiState.Available -> {
                Text("Stabiles Update verfügbar: v${current.update.version}", color = TldhSuccess, fontWeight = FontWeight.Bold)
                Text(current.update.apk.name, color = TldhTextMuted)
                current.update.apk.sizeBytes?.let { Text("Größe: ${formatBytes(it)}", color = TldhTextMuted) }
                Spacer(Modifier.height(8.dp))
                Text("Beim Download bleibt die App wach. Bitte nicht wegwischen; nach erfolgreicher SHA256-Prüfung öffnet tl;dh den Android-Installer.", color = TldhTextMuted)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        scope.launch {
                            updateState = ManualUpdateUiState.Downloading(current.update, 0f)
                            updateState = runCatching {
                                val file = UpdateDownloadGuard(context).runGuardedDownload {
                                    GitHubReleaseClient(repositorySlug).downloadAsset(
                                        asset = current.update.apk,
                                        destinationDir = File(context.cacheDir, "updates"),
                                        progress = { progress -> scope.launch { updateState = ManualUpdateUiState.Downloading(current.update, progress) } }
                                    )
                                }
                                ManualUpdateUiState.ReadyToInstall(current.update, file)
                            }.getOrElse { error -> ManualUpdateUiState.Error(error.message ?: "Download fehlgeschlagen.") }
                        }
                    }) { Text("Download") }
                    OutlinedButton(onClick = { updateState = ManualUpdateUiState.Idle }) { Text("Abbrechen") }
                }
            }

            is ManualUpdateUiState.Downloading -> {
                LinearProgressIndicator(progress = { current.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Download läuft … ${(current.progress * 100).toInt()} %", color = TldhTextMuted)
                Spacer(Modifier.height(6.dp))
                Text("tl;dh hält das Gerät währenddessen wach, damit Android den Download beim ausgeschalteten Display nicht abwürgt.", color = TldhSuccess)
            }

            is ManualUpdateUiState.ReadyToInstall -> {
                Text("Download verifiziert. SHA256 korrekt.", color = TldhSuccess, fontWeight = FontWeight.Bold)
                Text(current.update.apk.name, color = TldhTextMuted)
                Spacer(Modifier.height(10.dp))
                Button(onClick = {
                    context.startActivity(ApkInstaller(context).createInstallIntent(current.apkFile))
                }) { Text("Installieren") }
            }

            is ManualUpdateUiState.Error -> {
                Text("Update-Prüfung fehlgeschlagen", color = TldhDanger, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(current.message, color = TldhTextMuted)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { updateState = ManualUpdateUiState.Idle }) { Text("Zurück") }
            }
        }
    }
}

@Composable
private fun ProcessingCard() {
    TldhCard {
        Text("Audio wird lokal verarbeitet …", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text("Ingest → Dauer-Gate → lokaler Transkriptions-Spike. Keine Cloud für die Core-Verarbeitung.", color = TldhTextMuted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultCard(summary: AudioSummary, sessionManager: SessionManager) {
    val context = LocalContext.current
    var showDetails by remember(summary) { mutableStateOf(false) }
    TldhCard {
        Text("Long story, short.", color = TldhHotPurple, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        TldrBlock(summary.tldr)

        if (summary.transcript != null) {
            Spacer(Modifier.height(14.dp))
            TranscriptBlock(summary.transcript)
        }

        if (summary.warnings.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            GuardrailWarningBlock(summary.warnings.take(3))
        }

        Spacer(Modifier.height(16.dp))
        Text("Kurzpunkte", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        summary.keyPoints.take(3).forEach { point ->
            Text("• $point", color = TldhTextMuted)
        }

        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            summary.tags.take(5).forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { context.copyToClipboard(formatSummary(summary)) }) { Text("Copy All") }
            summary.transcript?.let { transcript ->
                OutlinedButton(onClick = { context.copyToClipboard(transcript) }) { Text("Copy Transcript") }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = { showDetails = !showDetails }) {
                Text(if (showDetails) "Details ausblenden" else "Technische Details")
            }
            TextButton(onClick = { sessionManager.wipeCurrentSession() }) { Text("Session löschen") }
        }

        if (showDetails) {
            Spacer(Modifier.height(12.dp))
            DetailsBlock(summary)
        }
    }
}

@Composable
private fun TldrBlock(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B0B22)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = Color(0xFFFFEAF5),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(18.dp)
        )
    }
}

@Composable
private fun TranscriptBlock(transcript: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0818)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Transkript", color = TldhHotPurple, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                transcript,
                color = Color(0xFFFBE8F3),
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailsBlock(summary: AudioSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12050F)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            summary.transcriptionStatus?.let {
                Text(it, color = TldhSuccess, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
            }
            summary.technicalDetails.forEach { detail -> Text("• $detail", color = TldhTextMuted) }
            summary.category?.let {
                Spacer(Modifier.height(8.dp))
                Text("Kategorie: $it", color = TldhTextMuted)
            }
            if (summary.replySuggestions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Antwortvorschläge", color = TldhHotPurple, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                summary.replySuggestions.forEach { reply ->
                    Text("[${reply.tone}] ${reply.text}", color = TldhTextMuted)
                }
            }
        }
    }
}

@Composable
private fun GuardrailWarningBlock(warnings: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TldhDanger.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Hinweise / Guardrails", color = TldhDanger, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            warnings.forEach { Text("• $it", color = TldhTextMuted) }
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
        colors = CardDefaults.cardColors(containerColor = TldhSurface.copy(alpha = 0.94f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 72.dp), content = { content() })
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
            colors = listOf(TldhPurple.copy(alpha = pulse), TldhHotPurple.copy(alpha = pulse * 0.9f), TldhGlow.copy(alpha = pulse * 0.52f), Color.Transparent),
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
    appendLine("tl;dh — Long story, short.")
    appendLine()
    appendLine("TL;DR")
    appendLine(summary.tldr)
    appendLine()
    summary.transcript?.let {
        appendLine("Transkript")
        appendLine(it)
        appendLine()
    }
    if (summary.warnings.isNotEmpty()) {
        appendLine("Hinweise / Guardrails")
        summary.warnings.take(4).forEach { appendLine("- $it") }
        appendLine()
    }
    appendLine("Kurzpunkte")
    summary.keyPoints.take(4).forEach { appendLine("- $it") }
    appendLine()
    appendLine("Tags: ${summary.tags.joinToString(", ")}")
    summary.category?.let { appendLine("Kategorie: $it") }
}


private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
