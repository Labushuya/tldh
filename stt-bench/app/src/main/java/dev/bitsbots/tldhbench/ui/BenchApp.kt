package dev.bitsbots.tldhbench.ui

import android.app.Activity
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import dev.bitsbots.tldhbench.models.VoskModelManager
import dev.bitsbots.tldhbench.share.SharedAudio
import kotlinx.coroutines.launch
import java.util.Locale

private val Bg = Color(0xFF09040A)
private val Surface = Color(0xFF150A12)
private val Surface2 = Color(0xFF24101C)
private val Accent = Color(0xFFA50B5E)
private val Accent2 = Color(0xFFD83F8D)
private val TextMain = Color(0xFFFBEAF4)
private val TextMuted = Color(0xFFC9AFC0)
private val Good = Color(0xFF34D399)
private val Bad = Color(0xFFFB7185)

@Composable
fun BenchApp(sharedAudioState: MutableState<SharedAudio?>) {
    val context = LocalContext.current
    val modelManager = remember { VoskModelManager(context) }
    val scope = rememberCoroutineScope()
    var modelInstalled by remember { mutableStateOf(modelManager.isInstalled()) }
    var progress by remember { mutableIntStateOf(0) }
    var busyLabel by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<BenchmarkResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

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
                        "Separate App. tl;dh bleibt unberührt. Diese App misst Deutsch-STT-Speed und Qualität mit echten WhatsApp-Audios.",
                        color = TextMuted,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Zielmarken: 15s→≤15s · 60s→≤30s · 180s→≤120s", color = TextMain, fontWeight = FontWeight.SemiBold)
                }

                CardBlock(title = "Engine: Vosk small German") {
                    Text(
                        if (modelInstalled) "Modell installiert: vosk-model-small-de-0.15" else "Modell fehlt: vosk-model-small-de-0.15 (~45 MB).",
                        color = if (modelInstalled) Good else TextMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                error = null
                                busyLabel = "Modell wird geladen…"
                                progress = 0
                                runCatching {
                                    modelManager.downloadAndInstallSmallGerman { progress = it }
                                }.onSuccess {
                                    modelInstalled = true
                                    busyLabel = null
                                }.onFailure {
                                    busyLabel = null
                                    error = "Modell-Download/Installation fehlgeschlagen: ${it.message}"
                                }
                            }
                        },
                        enabled = busyLabel == null,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) { Text(if (modelInstalled) "Modell neu installieren" else "Deutsch-Modell herunterladen") }
                    if (busyLabel?.contains("Modell") == true) Text("$busyLabel $progress%", color = TextMuted)
                }

                CardBlock(title = "Geteilte Audio") {
                    val shared = sharedAudioState.value
                    Text(
                        if (shared == null) "Teile eine WhatsApp/Telegram-Audio an diese Benchmark-App." else "Audio empfangen: ${shared.mimeType ?: "unbekannter MIME-Type"}",
                        color = if (shared == null) TextMuted else Good
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val audio = sharedAudioState.value ?: return@Button
                            scope.launch {
                                error = null
                                result = null
                                busyLabel = "Benchmark läuft…"
                                runCatching {
                                    BenchmarkRunner(context).runVoskSmallDe(audio)
                                }.onSuccess {
                                    result = it
                                    busyLabel = null
                                }.onFailure {
                                    busyLabel = null
                                    error = "Benchmark fehlgeschlagen: ${it.message}"
                                }
                            }
                        },
                        enabled = sharedAudioState.value != null && modelInstalled && busyLabel == null,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) { Text("Vosk Deutsch benchmarken") }
                    if (busyLabel?.contains("Benchmark") == true) Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Accent2)
                        Text(busyLabel ?: "", color = TextMuted)
                    }
                }

                error?.let { ErrorCard(it) }
                result?.let { ResultCard(it) }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("tl;dh STT Bench", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Deutsch. Geschwindigkeit. Qualität. Keine Änderungen an tl;dh.", color = TextMuted)
    }
}

@Composable
private fun CardBlock(title: String, content: @Composable Column.() -> Unit) {
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
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF351018)), shape = RoundedCornerShape(18.dp)) {
        Text(message, color = Bad, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun ResultCard(result: BenchmarkResult) {
    var detailsExpanded by remember { mutableStateOf(false) }
    var transcriptExpanded by remember { mutableStateOf(true) }
    val passColor = when (result.verdict.passed) {
        true -> Good
        false -> Bad
        null -> TextMuted
    }

    Card(colors = CardDefaults.cardColors(containerColor = Surface2), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ergebnis", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(result.verdict.message, color = passColor, fontWeight = FontWeight.SemiBold)
            Text("Gesamt: ${fmtMs(result.timing.totalMs)} · Audio: ${fmtMs(result.timing.audioDurationMs)} · RTF: ${result.timing.rtf?.let { String.format(Locale.US, "%.2f", it) } ?: "n/a"}", color = TextMain)
            Text("Decode: ${fmtMs(result.timing.decodeMs)} · Modell: ${fmtMs(result.timing.modelLoadMs)} · STT: ${fmtMs(result.timing.sttMs)}", color = TextMuted)

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

private fun fmtMs(ms: Long?): String {
    if (ms == null) return "n/a"
    return if (ms < 1000L) "${ms} ms" else String.format(Locale.GERMANY, "%.2f s", ms / 1000.0)
}

private fun ts(sec: Double?): String {
    if (sec == null) return "?:??"
    val total = sec.toInt().coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
