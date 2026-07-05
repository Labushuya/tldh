package dev.bitsbots.tldhbench.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bitsbots.tldhbench.bench.BenchmarkResult
import dev.bitsbots.tldhbench.bench.BenchmarkRunner
import dev.bitsbots.tldhbench.bench.Signal
import dev.bitsbots.tldhbench.bench.VoskModelCatalog
import dev.bitsbots.tldhbench.bench.VoskModelSpec
import dev.bitsbots.tldhbench.history.BenchmarkHistoryItem
import dev.bitsbots.tldhbench.history.BenchmarkHistoryStore
import dev.bitsbots.tldhbench.models.VoskModelManager
import dev.bitsbots.tldhbench.share.SharedAudio
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF09040A)
private val Surface = Color(0xFF150A12)
private val Surface2 = Color(0xFF24101C)
private val Accent = Color(0xFFA50B5E)
private val Accent2 = Color(0xFFD83F8D)
private val TextMain = Color(0xFFFBEAF4)
private val TextMuted = Color(0xFFC9AFC0)
private val Good = Color(0xFF34D399)
private val Warn = Color(0xFFFBBF24)
private val Bad = Color(0xFFFB7185)

@Composable
fun BenchApp(sharedAudioState: MutableState<SharedAudio?>) {
    val context = LocalContext.current
    val modelManager = remember { VoskModelManager(context) }
    val historyStore = remember { BenchmarkHistoryStore(context) }
    val scope = rememberCoroutineScope()
    var selectedModel by remember { mutableStateOf(VoskModelCatalog.defaultModel) }
    var installedIds by remember { mutableStateOf(modelManager.installedIds()) }
    var progress by remember { mutableIntStateOf(0) }
    var busyLabel by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<BenchmarkResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf(historyStore.load()) }
    var historyExpanded by remember { mutableStateOf(false) }
    var referenceText by remember { mutableStateOf("") }

    fun resetBenchmark() {
        result = null
        error = null
        busyLabel = null
        progress = 0
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Bg, Color(0xFF130711), Bg)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Header()
                CardBlock(title = "Benchmark-Ziel") {
                    Text(
                        "Mehrere Vosk-Modelle live herunterladen, wechseln und gegen dieselbe Audio testen. tl;dh bleibt unberührt.",
                        color = TextMuted,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Zielmarken: 15s→≤15s · 60s→≤30s · 180s→≤120s", color = TextMain, fontWeight = FontWeight.SemiBold)
                }


                HistoryCard(
                    history = history,
                    expanded = historyExpanded,
                    onToggle = { historyExpanded = !historyExpanded },
                    onClear = {
                        historyStore.clear()
                        history = emptyList()
                    }
                )

                ReferenceTextCard(
                    referenceText = referenceText,
                    onReferenceTextChange = { referenceText = it },
                    onPasteFromClipboard = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        val text = clip?.takeIf { it.itemCount > 0 }
                            ?.getItemAt(0)
                            ?.coerceToText(context)
                            ?.toString()
                            .orEmpty()
                        if (text.isNotBlank()) referenceText = text
                    },
                    onClear = { referenceText = "" }
                )

                CardBlock(title = "Vosk Modelle") {
                    Text("Ampel: Geschwindigkeit / Genauigkeit / Handy-Eignung", color = TextMuted, lineHeight = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    VoskModelCatalog.models.forEach { spec ->
                        ModelCard(
                            spec = spec,
                            selected = spec.id == selectedModel.id,
                            installed = installedIds.contains(spec.id),
                            busy = busyLabel != null,
                            progress = progress,
                            busyLabel = busyLabel,
                            onSelect = {
                                selectedModel = spec
                                result = null
                                error = null
                            },
                            onDownload = {
                                scope.launch {
                                    error = null
                                    result = null
                                    progress = 0
                                    busyLabel = "Download ${spec.displayName}…"
                                    runCatching {
                                        modelManager.downloadAndInstall(spec) { progress = it }
                                    }.onSuccess {
                                        installedIds = modelManager.installedIds()
                                        busyLabel = null
                                    }.onFailure {
                                        busyLabel = null
                                        error = "Modell-Download/Installation fehlgeschlagen (${spec.displayName}): ${it.message}"
                                    }
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    error = null
                                    result = null
                                    busyLabel = "Lösche ${spec.displayName}…"
                                    runCatching { modelManager.delete(spec) }
                                        .onSuccess {
                                            installedIds = modelManager.installedIds()
                                            busyLabel = null
                                        }
                                        .onFailure {
                                            busyLabel = null
                                            error = "Modell konnte nicht gelöscht werden (${spec.displayName}): ${it.message}"
                                        }
                                }
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    if (busyLabel?.startsWith("Download") == true) Text("$busyLabel $progress%", color = TextMuted)
                }

                CardBlock(title = "Geteilte Audio") {
                    val shared = sharedAudioState.value
                    Text(
                        if (shared == null) "Teile eine WhatsApp/Telegram-Audio an diese Benchmark-App." else "Audio empfangen: ${shared.mimeType ?: "unbekannter MIME-Type"}",
                        color = if (shared == null) TextMuted else Good
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Aktives Modell: ${selectedModel.displayName}", color = TextMain, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                val audio = sharedAudioState.value ?: return@Button
                                scope.launch {
                                    error = null
                                    result = null
                                    busyLabel = "Benchmark läuft…"
                                    runCatching {
                                        BenchmarkRunner(context).runVosk(audio, selectedModel, referenceText)
                                    }.onSuccess {
                                        result = it
                                        historyStore.add(it)
                                        history = historyStore.load()
                                        historyExpanded = true
                                        busyLabel = null
                                    }.onFailure {
                                        busyLabel = null
                                        error = "Benchmark fehlgeschlagen (${selectedModel.displayName}): ${it.message}"
                                    }
                                }
                            },
                            enabled = sharedAudioState.value != null && installedIds.contains(selectedModel.id) && busyLabel == null,
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) { Text("Benchmark starten") }
                        OutlinedButton(onClick = { resetBenchmark() }, enabled = busyLabel == null) {
                            Text("Reset")
                        }
                    }
                    if (!installedIds.contains(selectedModel.id)) {
                        Spacer(Modifier.height(8.dp))
                        Text("Ausgewähltes Modell ist noch nicht installiert.", color = Warn)
                    }
                    if (busyLabel?.contains("Benchmark") == true) Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Accent2)
                        Text(busyLabel ?: "", color = TextMuted)
                    }
                }

                error?.let { ErrorCard(it) }
                result?.let { ResultCard(it, onReset = { resetBenchmark() }) }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("tl;dh STT Bench", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Vosk-Modellvergleich: Speed vs. Deutsch-Qualität.", color = TextMuted)
    }
}

@Composable
private fun CardBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ReferenceTextCard(
    referenceText: String,
    onReferenceTextChange: (String) -> Unit,
    onPasteFromClipboard: () -> Unit,
    onClear: () -> Unit
) {
    val wordCount = referenceText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    CardBlock(title = "Referenztext / Goldstandard") {
        Text(
            "Optional: korrekten Text zur Audio einfügen. Nach dem Benchmark berechnet die App WER und CER gegen das erkannte Vosk-Transkript.",
            color = TextMuted,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = referenceText,
            onValueChange = onReferenceTextChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 8,
            label = { Text("Korrektes Referenztranskript") },
            placeholder = { Text("z. B. aus Common Voice TSV oder selbst geschriebenem WhatsApp-Goldstandard") }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (referenceText.isBlank()) "Kein Referenzvergleich aktiv." else "Referenzvergleich aktiv · Wörter: $wordCount",
            color = if (referenceText.isBlank()) TextMuted else Good
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onPasteFromClipboard) { Text("Aus Zwischenablage") }
            OutlinedButton(onClick = onClear, enabled = referenceText.isNotBlank()) { Text("Referenz leeren") }
        }
    }
}

@Composable
private fun HistoryCard(
    history: List<BenchmarkHistoryItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onClear: () -> Unit
) {
    CardBlock(title = "Letzte 5 Benchmarks") {
        Text(
            if (history.isEmpty()) "Noch keine gespeicherten Läufe. Nach jedem Benchmark wird der neueste Lauf automatisch hier abgelegt."
            else "Gespeichert: ${history.size}/5. Die Historie bleibt in der Benchmark-App und berührt tl;dh nicht.",
            color = TextMuted,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onToggle, enabled = history.isNotEmpty()) {
                Text(if (expanded) "Historie ausblenden" else "Historie anzeigen")
            }
            OutlinedButton(onClick = onClear, enabled = history.isNotEmpty()) {
                Text("Historie löschen")
            }
        }
        AnimatedVisibility(expanded && history.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                history.forEachIndexed { index, item ->
                    HistoryItemCard(index = index + 1, item = item)
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(index: Int, item: BenchmarkHistoryItem) {
    var transcriptExpanded by remember(item.timestampMs) { mutableStateOf(false) }
    val verdictColor = when (item.passed) {
        true -> Good
        false -> Bad
        null -> TextMuted
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF10070C), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("#$index · ${item.model}", color = TextMain, fontWeight = FontWeight.Bold)
                Text(formatHistoryTime(item.timestampMs), color = TextMuted, fontSize = 12.sp)
            }
            Text(item.passed?.let { if (it) "OK" else "FAIL" } ?: "INFO", color = verdictColor, fontWeight = FontWeight.Bold)
        }
        Text("Audio: ${fmtMs(item.durationMs)} · Gesamt: ${fmtMs(item.totalMs)} · RTF: ${item.rtf?.let { String.format(Locale.US, "%.2f", it) } ?: "n/a"}", color = TextMain)
        Text("Decode: ${fmtMs(item.decodeMs)} · Modell: ${fmtMs(item.modelLoadMs)} · STT: ${fmtMs(item.sttMs)}", color = TextMuted, fontSize = 13.sp)
        Text("Datei: ${item.audioName ?: "unbekannt"}", color = TextMuted, fontSize = 13.sp)
        if (item.warningsCount > 0) Text("Hinweise: ${item.warningsCount}", color = Warn, fontSize = 13.sp)
        if (item.werPercent != null && item.cerPercent != null) {
            Text("Referenz: WER ${fmtPct(item.werPercent)} · CER ${fmtPct(item.cerPercent)} · ${item.comparisonLabel ?: "ohne Label"}", color = comparisonColor(item.werPercent), fontWeight = FontWeight.SemiBold)
            item.comparisonSummary?.let { Text(it, color = TextMuted, fontSize = 13.sp) }
        }
        Text(item.transcriptPreview, color = TextMuted, lineHeight = 19.sp)
        OutlinedButton(onClick = { transcriptExpanded = !transcriptExpanded }) {
            Text(if (transcriptExpanded) "Transkript ausblenden" else "Transkript anzeigen")
        }
        AnimatedVisibility(transcriptExpanded) {
            Text(
                item.transcriptFull.ifBlank { "Kein Transkript gespeichert." },
                color = TextMuted,
                lineHeight = 19.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0509), RoundedCornerShape(14.dp))
                    .padding(10.dp)
            )
        }
    }
}

@Composable
private fun ModelCard(
    spec: VoskModelSpec,
    selected: Boolean,
    installed: Boolean,
    busy: Boolean,
    progress: Int,
    busyLabel: String?,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (selected) Accent2 else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF10070C), RoundedCornerShape(18.dp))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(enabled = !busy) { onSelect() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(spec.displayName, color = TextMain, fontWeight = FontWeight.Bold)
                Text("${spec.sizeLabel} · ${if (installed) "installiert" else "nicht installiert"}", color = if (installed) Good else TextMuted)
            }
            Text(if (selected) "aktiv" else "wählen", color = if (selected) Accent2 else TextMuted, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SignalChip("Speed", spec.speedSignal)
            SignalChip("Genauigkeit", spec.accuracySignal)
            SignalChip("Handy", spec.deviceSignal)
        }
        Text(spec.tradeoff, color = TextMain, lineHeight = 19.sp)
        Text(spec.notes, color = TextMuted, lineHeight = 19.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onDownload,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) { Text(if (installed) "Neu laden" else "Download") }
            if (installed) {
                OutlinedButton(onClick = onDelete, enabled = !busy) { Text("Löschen") }
            }
        }
        if (busyLabel?.contains(spec.displayName) == true) Text("$busyLabel $progress%", color = TextMuted)
    }
}

@Composable
private fun SignalChip(label: String, signal: Signal) {
    val color = when (signal) {
        Signal.GREEN -> Good
        Signal.YELLOW -> Warn
        Signal.RED -> Bad
    }
    Row(
        modifier = Modifier
            .background(Color(0xFF1A0A12), RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Text(label, color = TextMuted, fontSize = 12.sp)
    }
}

@Composable
private fun ReferenceComparisonBlock(comparison: dev.bitsbots.tldhbench.bench.ReferenceComparison) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF10070C), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Referenzvergleich", color = TextMain, fontWeight = FontWeight.Bold)
        Text(comparison.summary, color = comparisonColor(comparison.werPercent), fontWeight = FontWeight.SemiBold)
        Text(
            "Wörter Ref/Hyp: ${comparison.referenceWordCount}/${comparison.hypothesisWordCount} · Wortfehler: ${comparison.wordDistance} · Sub/Ins/Del: ${comparison.wordSubstitutions}/${comparison.wordInsertions}/${comparison.wordDeletions}",
            color = TextMuted,
            lineHeight = 19.sp
        )
        Text(
            "Zeichenfehler: ${comparison.charDistance}/${comparison.referenceCharCount}",
            color = TextMuted,
            lineHeight = 19.sp
        )
        OutlinedButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Referenz einklappen" else "Referenz und Erkennung anzeigen")
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Referenz", color = TextMain, fontWeight = FontWeight.SemiBold)
                Text(comparison.referenceRaw, color = TextMuted, lineHeight = 19.sp)
                Text("Erkannt", color = TextMain, fontWeight = FontWeight.SemiBold)
                Text(comparison.hypothesisRaw.ifBlank { "Kein Transkript erkannt." }, color = TextMuted, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF351018)), shape = RoundedCornerShape(18.dp)) {
        Text(message, color = Bad, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun ResultCard(result: BenchmarkResult, onReset: () -> Unit) {
    var detailsExpanded by remember { mutableStateOf(false) }
    var transcriptExpanded by remember { mutableStateOf(true) }
    val passColor = when (result.verdict.passed) {
        true -> Good
        false -> Bad
        null -> TextMuted
    }

    Card(colors = CardDefaults.cardColors(containerColor = Surface2), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ergebnis", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(result.model, color = TextMuted)
                }
                OutlinedButton(onClick = onReset) { Text("Reset") }
            }
            Text(result.verdict.message, color = passColor, fontWeight = FontWeight.SemiBold)
            Text("Gesamt: ${fmtMs(result.timing.totalMs)} · Audio: ${fmtMs(result.timing.audioDurationMs)} · RTF: ${result.timing.rtf?.let { String.format(Locale.US, "%.2f", it) } ?: "n/a"}", color = TextMain)
            Text("Decode: ${fmtMs(result.timing.decodeMs)} · Modell: ${fmtMs(result.timing.modelLoadMs)} · STT: ${fmtMs(result.timing.sttMs)}", color = TextMuted)

            result.referenceComparison?.let { ReferenceComparisonBlock(it) }

            if (result.warnings.isNotEmpty()) {
                Column(Modifier.background(Color(0xFF2A111F), RoundedCornerShape(16.dp)).padding(12.dp)) {
                    Text("Hinweise", color = TextMain, fontWeight = FontWeight.Bold)
                    result.warnings.forEach { Text("- $it", color = TextMuted) }
                }
            }

            OutlinedButton(onClick = { transcriptExpanded = !transcriptExpanded }) {
                Text(if (transcriptExpanded) "Transkript einklappen" else "Transkript mit Zeitstempeln anzeigen")
            }
            AnimatedVisibility(transcriptExpanded) {
                Column(Modifier.background(Color(0xFF11070D), RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Komplettes erkanntes Exzerpt", color = TextMain, fontWeight = FontWeight.Bold)
                    if (result.segments.isEmpty()) {
                        Text(result.transcript.ifBlank { "Kein Transkript erkannt." }, color = TextMuted)
                    } else {
                        result.segments.forEach { segment ->
                            Text("${ts(segment.startSec)}–${ts(segment.endSec)}  ${segment.text}", color = TextMuted, lineHeight = 20.sp)
                        }
                    }
                }
            }

            OutlinedButton(onClick = { detailsExpanded = !detailsExpanded }) {
                Text(if (detailsExpanded) "Technik einklappen" else "Technische Details")
            }
            AnimatedVisibility(detailsExpanded) {
                Column(Modifier.background(Color(0xFF10070C), RoundedCornerShape(16.dp)).padding(12.dp)) {
                    Text("Engine: ${result.engine} · Modell: ${result.model} · Sprache: ${result.language}", color = TextMuted)
                    Text("Datei: ${result.metadata.displayName ?: "unbekannt"}", color = TextMuted)
                    Text("MIME: ${result.metadata.mimeType ?: "unbekannt"} · Format: ${result.metadata.format}", color = TextMuted)
                    Text("Größe: ${result.metadata.sizeBytes ?: -1} Bytes · Header: ${result.metadata.headerProbeBytes} Bytes", color = TextMuted)
                }
            }
        }
    }
}

private fun formatHistoryTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return "Zeitpunkt unbekannt"
    return SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY).format(Date(timestampMs))
}

private fun fmtMs(ms: Long?): String {
    if (ms == null) return "n/a"
    return if (ms < 1000L) "${ms} ms" else String.format(Locale.GERMANY, "%.2f s", ms / 1000.0)
}

private fun fmtPct(value: Double): String = String.format(Locale.GERMANY, "%.1f%%", value)

private fun comparisonColor(werPercent: Double): Color = when {
    werPercent <= 15.0 -> Good
    werPercent <= 25.0 -> Good
    werPercent <= 40.0 -> Warn
    else -> Bad
}

private fun ts(sec: Double?): String {
    if (sec == null) return "?:??"
    val total = sec.toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
