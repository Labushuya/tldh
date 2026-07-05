package dev.bitsbots.tldhbench.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.io.File
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bitsbots.tldhbench.BuildConfig
import dev.bitsbots.tldhbench.bench.BenchmarkResult
import dev.bitsbots.tldhbench.bench.BenchmarkRunner
import dev.bitsbots.tldhbench.bench.Signal
import dev.bitsbots.tldhbench.bench.VoskModelCatalog
import dev.bitsbots.tldhbench.bench.VoskModelSpec
import dev.bitsbots.tldhbench.corpus.BuiltInReferenceCorpus
import dev.bitsbots.tldhbench.corpus.ReferenceCorpusManager
import dev.bitsbots.tldhbench.corpus.ReferenceSample
import dev.bitsbots.tldhbench.history.BenchmarkHistoryItem
import dev.bitsbots.tldhbench.history.BenchmarkHistoryStore
import dev.bitsbots.tldhbench.models.VoskModelManager
import dev.bitsbots.tldhbench.share.SharedAudio
import dev.bitsbots.tldhbench.updates.ApkInstaller
import dev.bitsbots.tldhbench.updates.BenchmarkReleaseSelector
import dev.bitsbots.tldhbench.updates.BenchmarkUpdate
import dev.bitsbots.tldhbench.updates.GitHubReleaseClient
import dev.bitsbots.tldhbench.updates.UpdateDownloadGuard
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
private val FieldBg = Color(0xFF0B0509)
private val FieldBorder = Color(0xFF7A365A)
private val DisabledBg = Color(0xFF2B1723)
private val DisabledText = Color(0xFF8D7484)

private val BenchColorScheme = darkColorScheme(
    primary = Accent2,
    onPrimary = TextMain,
    secondary = Accent,
    onSecondary = TextMain,
    background = Bg,
    onBackground = TextMain,
    surface = Surface,
    onSurface = TextMain,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMuted,
    error = Bad,
    onError = TextMain,
    outline = FieldBorder
)


@Composable
fun BenchApp(sharedAudioState: MutableState<SharedAudio?>) {
    val context = LocalContext.current
    val modelManager = remember { VoskModelManager(context) }
    val corpusManager = remember { ReferenceCorpusManager(context) }
    val historyStore = remember { BenchmarkHistoryStore(context) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedSection by remember { mutableStateOf(BenchSection.Start) }
    var selectedModel by remember { mutableStateOf(VoskModelCatalog.defaultModel) }
    var installedIds by remember { mutableStateOf(modelManager.installedIds()) }
    var installedSampleIds by remember { mutableStateOf(corpusManager.installedIds()) }
    var selectedSample by remember { mutableStateOf<ReferenceSample?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var busyLabel by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<BenchmarkResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf(historyStore.load()) }
    var historyExpanded by remember { mutableStateOf(false) }
    var referenceText by remember { mutableStateOf("") }
    var batchReport by remember { mutableStateOf<BatchRunReport?>(null) }
    var batchRepeatCount by remember { mutableIntStateOf(1) }

    fun resetBenchmark(clearBatch: Boolean = false) {
        result = null
        error = null
        busyLabel = null
        progress = 0
        if (clearBatch) batchReport = null
    }

    fun selectReferenceSample(sample: ReferenceSample) {
        runCatching {
            sharedAudioState.value = corpusManager.sharedAudio(sample)
            selectedSample = sample
            referenceText = sample.referenceText
            resetBenchmark()
            selectedSection = BenchSection.Run
        }.onFailure {
            error = "Testaudio konnte nicht ausgewählt werden (${sample.id}): ${it.message}"
            selectedSection = BenchSection.Results
        }
    }

    fun runSingleBenchmark() {
        val audio = sharedAudioState.value ?: return
        scope.launch {
            error = null
            result = null
            busyLabel = "Benchmark läuft…"
            selectedSection = BenchSection.Run
            runCatching {
                BenchmarkRunner(context).runVosk(audio, selectedModel, referenceText)
            }.onSuccess {
                result = it
                historyStore.add(it)
                history = historyStore.load()
                historyExpanded = true
                busyLabel = null
                selectedSection = BenchSection.Results
            }.onFailure {
                busyLabel = null
                error = "Benchmark fehlgeschlagen (${selectedModel.displayName}): ${it.message}"
                selectedSection = BenchSection.Results
            }
        }
    }

    fun runBatchBenchmark() {
        val baseSamples = BuiltInReferenceCorpus.samples.filter { installedSampleIds.contains(it.id) }
        if (baseSamples.isEmpty()) {
            error = "Batch nicht möglich: Lade zuerst mindestens ein Goldstandard-Testaudio."
            selectedSection = BenchSection.Results
            return
        }
        if (!installedIds.contains(selectedModel.id)) {
            error = "Batch nicht möglich: Installiere zuerst das aktive Modell ${selectedModel.displayName}."
            selectedSection = BenchSection.Results
            return
        }
        val batchSamples = buildList {
            repeat(batchRepeatCount.coerceAtLeast(1)) { addAll(baseSamples) }
        }
        scope.launch {
            error = null
            result = null
            batchReport = null
            progress = 0
            busyLabel = "Batch 0/${batchSamples.size} vorbereitet…"
            selectedSection = BenchSection.Run
            val results = mutableListOf<BenchmarkResult>()
            for ((index, sample) in batchSamples.withIndex()) {
                runCatching {
                    busyLabel = "Batch ${index + 1}/${batchSamples.size}: ${sample.id}"
                    progress = (((index + 1) * 100) / batchSamples.size).coerceIn(0, 100)
                    BenchmarkRunner(context).runVosk(corpusManager.sharedAudio(sample), selectedModel, sample.referenceText)
                }.onSuccess {
                    results += it
                    historyStore.add(it)
                    history = historyStore.load()
                }.onFailure { throwable ->
                    busyLabel = null
                    error = "Batch-Benchmark fehlgeschlagen (${selectedModel.displayName}): ${throwable.message}"
                    if (results.isNotEmpty()) batchReport = BatchRunReport.from(selectedModel, results, batchRepeatCount)
                    selectedSection = BenchSection.Results
                    return@launch
                }
            }
            batchReport = BatchRunReport.from(selectedModel, results, batchRepeatCount)
            busyLabel = null
            historyExpanded = true
            selectedSection = BenchSection.Results
        }
    }

    LaunchedEffect(selectedSection) {
        scrollState.animateScrollTo(0)
    }

    MaterialTheme(colorScheme = BenchColorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Bg, Color(0xFF130711), Bg)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Header()
                ActiveSetupCard(
                    selectedModel = selectedModel,
                    modelInstalled = installedIds.contains(selectedModel.id),
                    selectedSample = selectedSample,
                    sharedAudio = sharedAudioState.value,
                    referenceText = referenceText,
                    installedSampleCount = installedSampleIds.size,
                    totalSampleCount = BuiltInReferenceCorpus.samples.size,
                    busyLabel = busyLabel,
                    progress = progress
                )
                SectionNav(selected = selectedSection, onSelect = { selectedSection = it })

                when (selectedSection) {
                    BenchSection.Start -> StartSection(
                        appVersion = BuildConfig.VERSION_NAME,
                        onGoModels = { selectedSection = BenchSection.Models },
                        onGoCorpus = { selectedSection = BenchSection.Corpus },
                        onGoRun = { selectedSection = BenchSection.Run },
                        onGoUpdates = { selectedSection = BenchSection.Updates }
                    )

                    BenchSection.Models -> ModelsSection(
                        selectedModel = selectedModel,
                        installedIds = installedIds,
                        busyLabel = busyLabel,
                        progress = progress,
                        onSelect = { spec ->
                            selectedModel = spec
                            result = null
                            error = null
                        },
                        onDownload = { spec ->
                            scope.launch {
                                error = null
                                result = null
                                progress = 0
                                busyLabel = "Download ${spec.displayName}…"
                                runCatching { modelManager.downloadAndInstall(spec) { progress = it } }
                                    .onSuccess {
                                        installedIds = modelManager.installedIds()
                                        busyLabel = null
                                    }
                                    .onFailure {
                                        busyLabel = null
                                        error = "Modell-Download/Installation fehlgeschlagen (${spec.displayName}): ${it.message}"
                                        selectedSection = BenchSection.Results
                                    }
                            }
                        },
                        onDelete = { spec ->
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
                                        selectedSection = BenchSection.Results
                                    }
                            }
                        }
                    )

                    BenchSection.Corpus -> CorpusSection(
                        installedSampleIds = installedSampleIds,
                        selectedSample = selectedSample,
                        busy = busyLabel != null,
                        progress = progress,
                        busyLabel = busyLabel,
                        onDownloadAll = {
                            scope.launch {
                                error = null
                                result = null
                                progress = 0
                                busyLabel = "Lade Goldstandard-Testaudios…"
                                runCatching {
                                    corpusManager.downloadAll(BuiltInReferenceCorpus.samples) { label, pct ->
                                        busyLabel = label
                                        progress = pct
                                    }
                                }.onSuccess {
                                    installedSampleIds = corpusManager.installedIds()
                                    busyLabel = null
                                    progress = 100
                                }.onFailure {
                                    busyLabel = null
                                    error = "Goldstandard-Download fehlgeschlagen: ${it.message}"
                                    selectedSection = BenchSection.Results
                                }
                            }
                        },
                        onDownload = { sample ->
                            scope.launch {
                                error = null
                                result = null
                                progress = 0
                                busyLabel = "Lade ${sample.id}…"
                                runCatching { corpusManager.download(sample) { progress = it } }
                                    .onSuccess {
                                        installedSampleIds = corpusManager.installedIds()
                                        busyLabel = null
                                    }
                                    .onFailure {
                                        busyLabel = null
                                        error = "Testaudio-Download fehlgeschlagen (${sample.id}): ${it.message}"
                                        selectedSection = BenchSection.Results
                                    }
                            }
                        },
                        onSelect = { sample -> selectReferenceSample(sample) },
                        onDelete = { sample ->
                            scope.launch {
                                error = null
                                result = null
                                busyLabel = "Lösche ${sample.id}…"
                                runCatching { corpusManager.delete(sample) }
                                    .onSuccess {
                                        installedSampleIds = corpusManager.installedIds()
                                        if (selectedSample?.id == sample.id) {
                                            selectedSample = null
                                            sharedAudioState.value = null
                                        }
                                        busyLabel = null
                                    }
                                    .onFailure {
                                        busyLabel = null
                                        error = "Testaudio konnte nicht gelöscht werden (${sample.id}): ${it.message}"
                                        selectedSection = BenchSection.Results
                                    }
                            }
                        },
                        onClear = {
                            scope.launch {
                                error = null
                                result = null
                                busyLabel = "Lösche Goldstandard-Testaudios…"
                                runCatching { corpusManager.clear() }
                                    .onSuccess {
                                        installedSampleIds = corpusManager.installedIds()
                                        selectedSample = null
                                        sharedAudioState.value = null
                                        busyLabel = null
                                    }
                                    .onFailure {
                                        busyLabel = null
                                        error = "Goldstandard-Testaudios konnten nicht gelöscht werden: ${it.message}"
                                        selectedSection = BenchSection.Results
                                    }
                            }
                        }
                    )

                    BenchSection.Run -> RunSection(
                        sharedAudio = sharedAudioState.value,
                        selectedModel = selectedModel,
                        selectedModelInstalled = installedIds.contains(selectedModel.id),
                        selectedSample = selectedSample,
                        referenceText = referenceText,
                        installedSampleCount = installedSampleIds.size,
                        totalSampleCount = BuiltInReferenceCorpus.samples.size,
                        busy = busyLabel != null,
                        progress = progress,
                        busyLabel = busyLabel,
                        batchRepeatCount = batchRepeatCount,
                        batchReport = batchReport,
                        onReferenceTextChange = { referenceText = it },
                        onPasteReference = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = clipboard.primaryClip?.takeIf { it.itemCount > 0 }
                                ?.getItemAt(0)
                                ?.coerceToText(context)
                                ?.toString()
                                .orEmpty()
                            if (text.isNotBlank()) referenceText = text
                        },
                        onClearReference = { referenceText = "" },
                        onRunSingle = { runSingleBenchmark() },
                        onReset = { resetBenchmark(clearBatch = false) },
                        onBatchRepeatChange = { batchRepeatCount = it },
                        onRunBatch = { runBatchBenchmark() },
                        onCopyBatchReport = { report -> copyToClipboard(context, "tl;dh STT Bench Batch Report", report.toMarkdown()) },
                        onClearBatchReport = { batchReport = null }
                    )

                    BenchSection.Results -> ResultsSection(
                        error = error,
                        result = result,
                        batchReport = batchReport,
                        history = history,
                        historyExpanded = historyExpanded,
                        onReset = { resetBenchmark(clearBatch = false) },
                        onCopyResult = { current -> copyToClipboard(context, "tl;dh STT Bench Einzelreport", current.toMarkdown()) },
                        onCopyBatch = { report -> copyToClipboard(context, "tl;dh STT Bench Batch Report", report.toMarkdown()) },
                        onClearBatch = { batchReport = null },
                        onHistoryToggle = { historyExpanded = !historyExpanded },
                        onHistoryClear = {
                            historyStore.clear()
                            history = emptyList()
                        },
                        onGoRun = { selectedSection = BenchSection.Run }
                    )

                    BenchSection.Updates -> ManualBenchmarkUpdaterCard(
                        appVersion = BuildConfig.VERSION_NAME,
                        repositorySlug = BuildConfig.GITHUB_REPOSITORY
                    )
                }
            }
        }
    }
}

private enum class BenchSection(val label: String) {
    Start("Start"),
    Models("Modelle"),
    Corpus("Goldstandard"),
    Run("Benchmark"),
    Results("Ergebnisse"),
    Updates("Updates")
}

private sealed interface BenchUpdateUiState {
    data object Idle : BenchUpdateUiState
    data object Checking : BenchUpdateUiState
    data object UpToDate : BenchUpdateUiState
    data class Available(val update: BenchmarkUpdate) : BenchUpdateUiState
    data class Downloading(val update: BenchmarkUpdate, val progress: Float) : BenchUpdateUiState
    data class ReadyToInstall(val update: BenchmarkUpdate, val apkFile: File) : BenchUpdateUiState
    data class Error(val message: String) : BenchUpdateUiState
}

@Composable
private fun SectionNav(selected: BenchSection, onSelect: (BenchSection) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BenchSection.entries.chunked(3).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { section ->
                        val active = section == selected
                        if (active) {
                            Button(
                                onClick = { onSelect(section) },
                                modifier = Modifier.weight(1f),
                                colors = benchButtonColors()
                            ) { Text(section.label, fontSize = 12.sp) }
                        } else {
                            OutlinedButton(
                                onClick = { onSelect(section) },
                                modifier = Modifier.weight(1f)
                            ) { Text(section.label, fontSize = 12.sp) }
                        }
                    }
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ActiveSetupCard(
    selectedModel: VoskModelSpec,
    modelInstalled: Boolean,
    selectedSample: ReferenceSample?,
    sharedAudio: SharedAudio?,
    referenceText: String,
    installedSampleCount: Int,
    totalSampleCount: Int,
    busyLabel: String?,
    progress: Int
) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface2), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Aktueller Prüfstand", color = TextMain, fontWeight = FontWeight.Bold)
                    Text("Modell: ${selectedModel.displayName}", color = if (modelInstalled) Good else Warn, lineHeight = 18.sp)
                    Text("Audio: ${selectedSample?.id ?: sharedAudio?.mimeType ?: "noch keine Quelle"}", color = if (sharedAudio != null) Good else TextMuted, lineHeight = 18.sp)
                }
                Text("$installedSampleCount/$totalSampleCount", color = if (installedSampleCount > 0) Good else TextMuted, fontWeight = FontWeight.Bold)
            }
            Text(
                if (referenceText.isBlank()) "Referenzvergleich inaktiv" else "Referenzvergleich aktiv · ${referenceText.wordCount()} Wörter",
                color = if (referenceText.isBlank()) TextMuted else Good,
                fontSize = 13.sp
            )
            busyLabel?.let {
                LinearProgressIndicator(progress = { (progress / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text("$it $progress%", color = TextMuted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StartSection(
    appVersion: String,
    onGoModels: () -> Unit,
    onGoCorpus: () -> Unit,
    onGoRun: () -> Unit,
    onGoUpdates: () -> Unit
) {
    CardBlock(title = "Geführter Ablauf") {
        Text("Version v$appVersion · separater STT-Benchmark. Die Haupt-App tl;dh bleibt unberührt.", color = TextMuted, lineHeight = 20.sp)
        Spacer(Modifier.height(8.dp))
        StepText("1", "Modell installieren oder wechseln", "Small DE fürs Handy, Big DE als Qualitäts-/Stressprobe.")
        StepText("2", "Goldstandard laden", "Starter-Korpus plus Langlauf-Batch für längere Messstrecken.")
        StepText("3", "Benchmark starten", "Einzeltest oder Batch laufen lassen; Ergebnis öffnet danach im Ergebnisbereich.")
        StepText("4", "Report kopieren", "Markdown-Report für Vergleich und nächste Modellentscheidung.")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onGoModels, modifier = Modifier.weight(1f), colors = benchButtonColors()) { Text("Modelle") }
            Button(onClick = onGoCorpus, modifier = Modifier.weight(1f), colors = benchButtonColors()) { Text("Goldstandard") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onGoRun, modifier = Modifier.weight(1f)) { Text("Benchmark") }
            OutlinedButton(onClick = onGoUpdates, modifier = Modifier.weight(1f)) { Text("Updates") }
        }
    }
    CardBlock(title = "Was wurde am UI geändert?") {
        Text("Die App nutzt jetzt getrennte Bereiche statt einem langen Endlos-Scroll. Nach Einzel- oder Batchlauf springt sie logisch in den Ergebnisbereich; der Scroll startet dort oben.", color = TextMuted, lineHeight = 20.sp)
    }
}

@Composable
private fun StepText(number: String, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Accent, CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(number, color = TextMain, fontWeight = FontWeight.Bold) }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextMain, fontWeight = FontWeight.SemiBold)
            Text(body, color = TextMuted, lineHeight = 18.sp, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ModelsSection(
    selectedModel: VoskModelSpec,
    installedIds: Set<String>,
    busyLabel: String?,
    progress: Int,
    onSelect: (VoskModelSpec) -> Unit,
    onDownload: (VoskModelSpec) -> Unit,
    onDelete: (VoskModelSpec) -> Unit
) {
    CardBlock(title = "Vosk Modelle") {
        Text("Installieren, auswählen, danach ohne App-Neustart benchmarken. Ampel: Geschwindigkeit / Genauigkeit / Handy-Eignung.", color = TextMuted, lineHeight = 20.sp)
        Spacer(Modifier.height(8.dp))
        VoskModelCatalog.models.forEach { spec ->
            ModelCard(
                spec = spec,
                selected = spec.id == selectedModel.id,
                installed = installedIds.contains(spec.id),
                busy = busyLabel != null,
                progress = progress,
                busyLabel = busyLabel,
                onSelect = { onSelect(spec) },
                onDownload = { onDownload(spec) },
                onDelete = { onDelete(spec) }
            )
            Spacer(Modifier.height(10.dp))
        }
        if (busyLabel?.startsWith("Download") == true) Text("$busyLabel $progress%", color = TextMuted)
    }
}

@Composable
private fun CorpusSection(
    installedSampleIds: Set<String>,
    selectedSample: ReferenceSample?,
    busy: Boolean,
    progress: Int,
    busyLabel: String?,
    onDownloadAll: () -> Unit,
    onDownload: (ReferenceSample) -> Unit,
    onSelect: (ReferenceSample) -> Unit,
    onDelete: (ReferenceSample) -> Unit,
    onClear: () -> Unit
) {
    GoldstandardCorpusCard(
        samples = BuiltInReferenceCorpus.samples,
        installedIds = installedSampleIds,
        selectedSample = selectedSample,
        busy = busy,
        progress = progress,
        busyLabel = busyLabel,
        onDownloadAll = onDownloadAll,
        onDownload = onDownload,
        onSelect = onSelect,
        onDelete = onDelete,
        onClear = onClear
    )
    CardBlock(title = "Längere Goldstandard-Läufe") {
        Text("Der Starter-Korpus besteht bewusst aus kurzen CC0-Referenzsätzen. Für längere Modellvergleiche nutzt der Batch-Benchmark jetzt Wiederholprofile: 1×, 3×, 8× oder 20× alle installierten Samples. Das ist kein Ersatz für ein echtes langes WhatsApp-Audio, aber deutlich besser für reproduzierbare RTF-/WER-Modellvergleiche.", color = TextMuted, lineHeight = 20.sp)
    }
}

@Composable
private fun RunSection(
    sharedAudio: SharedAudio?,
    selectedModel: VoskModelSpec,
    selectedModelInstalled: Boolean,
    selectedSample: ReferenceSample?,
    referenceText: String,
    installedSampleCount: Int,
    totalSampleCount: Int,
    busy: Boolean,
    progress: Int,
    busyLabel: String?,
    batchRepeatCount: Int,
    batchReport: BatchRunReport?,
    onReferenceTextChange: (String) -> Unit,
    onPasteReference: () -> Unit,
    onClearReference: () -> Unit,
    onRunSingle: () -> Unit,
    onReset: () -> Unit,
    onBatchRepeatChange: (Int) -> Unit,
    onRunBatch: () -> Unit,
    onCopyBatchReport: (BatchRunReport) -> Unit,
    onClearBatchReport: () -> Unit
) {
    BenchmarkActionCard(
        sharedAudio = sharedAudio,
        selectedModel = selectedModel,
        selectedModelInstalled = selectedModelInstalled,
        selectedSample = selectedSample,
        busy = busy,
        busyLabel = busyLabel,
        onRunSingle = onRunSingle,
        onReset = onReset
    )
    ReferenceTextCard(
        referenceText = referenceText,
        onReferenceTextChange = onReferenceTextChange,
        onPasteFromClipboard = onPasteReference,
        onClear = onClearReference
    )
    BatchBenchmarkCard(
        installedCount = installedSampleCount,
        totalCount = totalSampleCount,
        selectedModel = selectedModel,
        selectedModelInstalled = selectedModelInstalled,
        busy = busy,
        progress = progress,
        busyLabel = busyLabel,
        batchRepeatCount = batchRepeatCount,
        batchReport = batchReport,
        onRepeatChange = onBatchRepeatChange,
        onRunBatch = onRunBatch,
        onCopyReport = onCopyBatchReport,
        onClearReport = onClearBatchReport
    )
}

@Composable
private fun BenchmarkActionCard(
    sharedAudio: SharedAudio?,
    selectedModel: VoskModelSpec,
    selectedModelInstalled: Boolean,
    selectedSample: ReferenceSample?,
    busy: Boolean,
    busyLabel: String?,
    onRunSingle: () -> Unit,
    onReset: () -> Unit
) {
    CardBlock(title = "Einzelbenchmark") {
        Text(
            if (sharedAudio == null) "Teile eine WhatsApp/Telegram-Audio an diese Benchmark-App oder wähle ein Goldstandard-Sample." else "Audio bereit: ${sharedAudio.mimeType ?: "unbekannter MIME-Type"}",
            color = if (sharedAudio == null) TextMuted else Good
        )
        Spacer(Modifier.height(8.dp))
        Text("Aktives Modell: ${selectedModel.displayName}", color = TextMain, fontWeight = FontWeight.SemiBold)
        selectedSample?.let { Text("Goldstandard: ${it.id} · Referenz automatisch gesetzt", color = Good, fontWeight = FontWeight.SemiBold) }
        if (!selectedModelInstalled) Text("Ausgewähltes Modell ist noch nicht installiert.", color = Warn)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onRunSingle,
                enabled = sharedAudio != null && selectedModelInstalled && !busy,
                colors = benchButtonColors()
            ) { Text("Benchmark starten") }
            OutlinedButton(onClick = onReset, enabled = !busy) { Text("Reset") }
        }
        if (busyLabel?.contains("Benchmark") == true && !busyLabel.startsWith("Batch")) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Accent2)
                Text(busyLabel, color = TextMuted)
            }
        }
    }
}

@Composable
private fun ResultsSection(
    error: String?,
    result: BenchmarkResult?,
    batchReport: BatchRunReport?,
    history: List<BenchmarkHistoryItem>,
    historyExpanded: Boolean,
    onReset: () -> Unit,
    onCopyResult: (BenchmarkResult) -> Unit,
    onCopyBatch: (BatchRunReport) -> Unit,
    onClearBatch: () -> Unit,
    onHistoryToggle: () -> Unit,
    onHistoryClear: () -> Unit,
    onGoRun: () -> Unit
) {
    if (error == null && result == null && batchReport == null && history.isEmpty()) {
        CardBlock(title = "Noch kein Ergebnis") {
            Text("Starte einen Einzel- oder Batchbenchmark. Danach landet die App automatisch hier oben im Ergebnisbereich.", color = TextMuted, lineHeight = 20.sp)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onGoRun, colors = benchButtonColors()) { Text("Zum Benchmark") }
        }
    }
    error?.let { ErrorCard(it) }
    result?.let { current ->
        ResultCard(result = current, onReset = onReset, onCopyReport = { onCopyResult(current) })
    }
    batchReport?.let { report ->
        CardBlock(title = "Batch-Report") {
            BatchReportBlock(report)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { onCopyBatch(report) }) { Text("Report kopieren") }
                OutlinedButton(onClick = onClearBatch) { Text("Report leeren") }
            }
        }
    }
    HistoryCard(history = history, expanded = historyExpanded, onToggle = onHistoryToggle, onClear = onHistoryClear)
}

@Composable
private fun ManualBenchmarkUpdaterCard(appVersion: String, repositorySlug: String) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var updateState by remember { mutableStateOf<BenchUpdateUiState>(BenchUpdateUiState.Idle) }
    val repositoryConfigured = repositorySlug.contains('/')
    val downloadActive = updateState is BenchUpdateUiState.Downloading

    DisposableEffect(downloadActive, view) {
        val previous = view.keepScreenOn
        view.keepScreenOn = downloadActive || previous
        onDispose { view.keepScreenOn = previous }
    }

    CardBlock(title = "In-App Update") {
        Text("Manueller Update-Check nur für die Benchmark-App. Es werden ausschließlich Releases mit Tag `stt-bench-vX.Y.Z` und Asset `tldh-stt-bench-X.Y.Z.apk` berücksichtigt.", color = TextMuted, lineHeight = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text("Installiert: v$appVersion", color = TextMain, fontWeight = FontWeight.SemiBold)
        Text("Repo: ${repositorySlug.ifBlank { "nicht konfiguriert" }}", color = TextMuted)
        Spacer(Modifier.height(10.dp))
        when (val current = updateState) {
            BenchUpdateUiState.Idle -> {
                Button(
                    enabled = repositoryConfigured,
                    onClick = {
                        scope.launch {
                            updateState = BenchUpdateUiState.Checking
                            updateState = runCatching {
                                val releases = GitHubReleaseClient(repositorySlug).fetchStableReleases()
                                val update = BenchmarkReleaseSelector().select(appVersion, releases)
                                if (update == null) BenchUpdateUiState.UpToDate else BenchUpdateUiState.Available(update)
                            }.getOrElse { BenchUpdateUiState.Error(it.message ?: "Update-Prüfung fehlgeschlagen.") }
                        }
                    },
                    colors = benchButtonColors()
                ) { Text("Nach Bench-Update suchen") }
                if (!repositoryConfigured) Text("GitHub Repository wurde im Build nicht gesetzt. Release-Builds aus GitHub Actions konfigurieren das automatisch.", color = Bad, lineHeight = 20.sp)
            }
            BenchUpdateUiState.Checking -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Prüfe Benchmark-Releases …", color = TextMuted)
            }
            BenchUpdateUiState.UpToDate -> {
                Text("Du nutzt bereits den aktuellsten Benchmark-Release.", color = Good)
                OutlinedButton(onClick = { updateState = BenchUpdateUiState.Idle }) { Text("Erneut prüfen") }
            }
            is BenchUpdateUiState.Available -> {
                Text("Benchmark-Update verfügbar: v${current.update.version}", color = Good, fontWeight = FontWeight.Bold)
                Text(current.update.apk.name, color = TextMuted)
                current.update.apk.sizeBytes?.let { Text("Größe: ${formatBytes(it)}", color = TextMuted) }
                Text("Während des Downloads bleibt die App wach. Nach SHA256-Prüfung wird der Android-Installer geöffnet.", color = TextMuted, lineHeight = 20.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        scope.launch {
                            updateState = BenchUpdateUiState.Downloading(current.update, 0f)
                            updateState = runCatching {
                                val file = UpdateDownloadGuard(context).runGuardedDownload {
                                    GitHubReleaseClient(repositorySlug).downloadAsset(
                                        asset = current.update.apk,
                                        destinationDir = File(context.cacheDir, "updates"),
                                        progress = { pct -> scope.launch { updateState = BenchUpdateUiState.Downloading(current.update, pct) } }
                                    )
                                }
                                BenchUpdateUiState.ReadyToInstall(current.update, file)
                            }.getOrElse { BenchUpdateUiState.Error(it.message ?: "Download fehlgeschlagen.") }
                        }
                    }, colors = benchButtonColors()) { Text("Download") }
                    OutlinedButton(onClick = { updateState = BenchUpdateUiState.Idle }) { Text("Abbrechen") }
                }
            }
            is BenchUpdateUiState.Downloading -> {
                LinearProgressIndicator(progress = { current.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text("Download läuft … ${(current.progress * 100).toInt()} %", color = TextMuted)
                Text("Wake-Lock aktiv, damit Android den Download beim ausgeschalteten Display nicht abwürgt.", color = Good, lineHeight = 20.sp)
            }
            is BenchUpdateUiState.ReadyToInstall -> {
                Text("Download verifiziert. SHA256 korrekt.", color = Good, fontWeight = FontWeight.Bold)
                Text(current.update.apk.name, color = TextMuted)
                Button(onClick = { context.startActivity(ApkInstaller(context).createInstallIntent(current.apkFile)) }, colors = benchButtonColors()) { Text("Installieren") }
            }
            is BenchUpdateUiState.Error -> {
                Text("Update-Prüfung fehlgeschlagen", color = Bad, fontWeight = FontWeight.Bold)
                Text(current.message, color = TextMuted, lineHeight = 20.sp)
                OutlinedButton(onClick = { updateState = BenchUpdateUiState.Idle }) { Text("Zurück") }
            }
        }
    }
}

private fun String.wordCount(): Int = trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size

@Composable
private fun benchButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Accent,
    contentColor = TextMain,
    disabledContainerColor = DisabledBg,
    disabledContentColor = DisabledText
)

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
private fun GoldstandardCorpusCard(
    samples: List<ReferenceSample>,
    installedIds: Set<String>,
    selectedSample: ReferenceSample?,
    busy: Boolean,
    progress: Int,
    busyLabel: String?,
    onDownloadAll: () -> Unit,
    onDownload: (ReferenceSample) -> Unit,
    onSelect: (ReferenceSample) -> Unit,
    onDelete: (ReferenceSample) -> Unit,
    onClear: () -> Unit
) {
    val installedCount = samples.count { installedIds.contains(it.id) }
    var expanded by remember { mutableStateOf(installedCount < samples.size) }
    CardBlock(title = "Goldstandard-Testaudios") {
        Text(
            "Optionaler Starter-Korpus: saubere deutsche Referenz-Audios inklusive korrektem Text. Damit musst Du für Baseline-WER/CER nicht selbst erst Audios und Transkripte zusammensuchen.",
            color = TextMuted,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Quelle: rhasspy/dataset-voice-kerstin · CC0-1.0 · ${samples.size} kuratierte Kurz-/Mittelsätze",
            color = TextMain,
            fontWeight = FontWeight.SemiBold
        )
        selectedSample?.let {
            Text("Aktiv: ${it.id} · ${it.title}", color = Good, fontWeight = FontWeight.SemiBold)
        }
        Text("Installiert: $installedCount/${samples.size}", color = if (installedCount > 0) Good else TextMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onDownloadAll,
                enabled = !busy && installedCount < samples.size,
                colors = benchButtonColors()
            ) { Text("Starter-Set laden") }
            OutlinedButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Liste einklappen" else "Liste anzeigen")
            }
            OutlinedButton(onClick = onClear, enabled = !busy && installedCount > 0) {
                Text("Corpus löschen")
            }
        }
        if (busyLabel?.contains("rhasspy") == true || busyLabel?.contains("Goldstandard") == true || busyLabel?.startsWith("Lade de_rhasspy") == true) {
            Text("$busyLabel $progress%", color = TextMuted)
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                samples.forEach { sample ->
                    ReferenceSampleCard(
                        sample = sample,
                        installed = installedIds.contains(sample.id),
                        selected = selectedSample?.id == sample.id,
                        busy = busy,
                        onDownload = { onDownload(sample) },
                        onSelect = { onSelect(sample) },
                        onDelete = { onDelete(sample) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceSampleCard(
    sample: ReferenceSample,
    installed: Boolean,
    selected: Boolean,
    busy: Boolean,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF10070C), RoundedCornerShape(18.dp))
            .border(1.dp, if (selected) Accent2 else Color.Transparent, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${sample.id} · ${sample.title}", color = TextMain, fontWeight = FontWeight.Bold)
                Text("${sample.difficultyLabel} · ${sample.speaker} · ${sample.licenseLabel}", color = TextMuted, fontSize = 13.sp)
            }
            Text(
                if (selected) "aktiv" else if (installed) "bereit" else "nicht geladen",
                color = if (selected || installed) Good else TextMuted,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(sample.referenceText, color = TextMain, lineHeight = 19.sp)
        Text(sample.notes, color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onDownload,
                enabled = !busy,
                colors = benchButtonColors()
            ) { Text(if (installed) "Neu laden" else "Download") }
            OutlinedButton(onClick = onSelect, enabled = installed && !busy) {
                Text("Als Testaudio nutzen")
            }
            if (installed) {
                OutlinedButton(onClick = onDelete, enabled = !busy) { Text("Löschen") }
            }
        }
    }
}

@Composable
private fun BatchBenchmarkCard(
    installedCount: Int,
    totalCount: Int,
    selectedModel: VoskModelSpec,
    selectedModelInstalled: Boolean,
    busy: Boolean,
    progress: Int,
    busyLabel: String?,
    batchRepeatCount: Int,
    batchReport: BatchRunReport?,
    onRepeatChange: (Int) -> Unit,
    onRunBatch: () -> Unit,
    onCopyReport: (BatchRunReport) -> Unit,
    onClearReport: () -> Unit
) {
    CardBlock(title = "Batch-Benchmark / Langlauf") {
        Text(
            "Startet alle geladenen Goldstandard-Audios mit dem aktiven Modell und erzeugt einen aggregierten Markdown-Report. Wiederholprofile erzeugen längere, reproduzierbare Messstrecken für RTF/WER/CER.",
            color = TextMuted,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(8.dp))
        Text("Aktives Modell: ${selectedModel.displayName}", color = TextMain, fontWeight = FontWeight.SemiBold)
        Text("Goldstandard-Audios bereit: $installedCount/$totalCount", color = if (installedCount > 0) Good else TextMuted)
        Text(if (selectedModelInstalled) "Modell installiert: ja" else "Modell installiert: nein", color = if (selectedModelInstalled) Good else Warn)
        Text("Geplante Läufe: ${installedCount * batchRepeatCount} · Profil: ${batchRepeatCount}× Corpus", color = TextMain, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(1, 3, 8, 20).forEach { repeatCount ->
                if (repeatCount == batchRepeatCount) {
                    Button(onClick = { onRepeatChange(repeatCount) }, modifier = Modifier.weight(1f), enabled = !busy, colors = benchButtonColors()) { Text("${repeatCount}×") }
                } else {
                    OutlinedButton(onClick = { onRepeatChange(repeatCount) }, modifier = Modifier.weight(1f), enabled = !busy) { Text("${repeatCount}×") }
                }
            }
        }
        Text("Hinweis: Das ist ein reproduzierbarer Langlauf über mehrere Referenzdateien, kein künstlich zusammengeklebtes WhatsApp-Audio. Für echte Long-Form-Audios weiter eigene Dateien teilen.", color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onRunBatch,
                enabled = !busy && installedCount > 0 && selectedModelInstalled,
                colors = benchButtonColors()
            ) { Text("Batch starten") }
            OutlinedButton(onClick = { batchReport?.let(onCopyReport) }, enabled = !busy && batchReport != null) { Text("Report kopieren") }
            OutlinedButton(onClick = onClearReport, enabled = !busy && batchReport != null) { Text("Leeren") }
        }
        if (busyLabel?.startsWith("Batch") == true) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = Accent2)
                Text("$busyLabel $progress%", color = TextMuted)
            }
        }
        batchReport?.let { BatchReportBlock(it) }
    }
}

@Composable
private fun BatchReportBlock(report: BatchRunReport) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF10070C), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Batch-Ergebnis", color = TextMain, fontWeight = FontWeight.Bold)
        Text("${report.modelName} · ${report.sampleCount} Läufe · ${report.repeatCount}× Corpus · ${formatHistoryTime(report.createdAtMs)}", color = TextMuted)
        Text(
            "Ø RTF: ${report.avgRtf?.let { fmtNumber(it) } ?: "n/a"} · Ø WER: ${report.avgWerPercent?.let { fmtPct(it) } ?: "n/a"} · Ø CER: ${report.avgCerPercent?.let { fmtPct(it) } ?: "n/a"}",
            color = TextMain,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Speed-Pass: ${report.speedPassCount}/${report.speedEvaluatedCount} · schwächste WER: ${report.worstWerLabel}",
            color = if ((report.avgWerPercent ?: 100.0) <= 25.0) Good else if ((report.avgWerPercent ?: 100.0) <= 40.0) Warn else Bad
        )
        report.results.forEach { item ->
            val comparison = item.referenceComparison
            Text(
                "• ${item.metadata.displayName ?: item.metadata.uriString.substringAfterLast('/')} · Gesamt ${fmtMs(item.timing.totalMs)} · RTF ${item.timing.rtf?.let { fmtNumber(it) } ?: "n/a"} · WER ${comparison?.werPercent?.let { fmtPct(it) } ?: "n/a"}",
                color = TextMuted,
                lineHeight = 19.sp
            )
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
            placeholder = { Text("z. B. aus Common Voice TSV oder selbst geschriebenem WhatsApp-Goldstandard") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextMain,
                unfocusedTextColor = TextMain,
                disabledTextColor = DisabledText,
                cursorColor = Accent2,
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg,
                disabledContainerColor = DisabledBg,
                focusedBorderColor = Accent2,
                unfocusedBorderColor = FieldBorder,
                disabledBorderColor = DisabledText,
                focusedLabelColor = Accent2,
                unfocusedLabelColor = TextMuted,
                disabledLabelColor = DisabledText,
                focusedPlaceholderColor = TextMuted,
                unfocusedPlaceholderColor = TextMuted,
                disabledPlaceholderColor = DisabledText
            )
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
                colors = benchButtonColors()
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
private fun ResultCard(result: BenchmarkResult, onReset: () -> Unit, onCopyReport: () -> Unit) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onCopyReport) { Text("Report kopieren") }
                    OutlinedButton(onClick = onReset) { Text("Reset") }
                }
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

private data class BatchRunReport(
    val modelName: String,
    val modelId: String,
    val createdAtMs: Long,
    val results: List<BenchmarkResult>,
    val repeatCount: Int = 1
) {
    val sampleCount: Int = results.size
    val avgRtf: Double? = results.mapNotNull { it.timing.rtf }.averageOrNull()
    val avgWerPercent: Double? = results.mapNotNull { it.referenceComparison?.werPercent }.averageOrNull()
    val avgCerPercent: Double? = results.mapNotNull { it.referenceComparison?.cerPercent }.averageOrNull()
    val speedEvaluatedCount: Int = results.count { it.verdict.passed != null }
    val speedPassCount: Int = results.count { it.verdict.passed == true }
    val worstWerLabel: String = results
        .mapNotNull { result -> result.referenceComparison?.werPercent?.let { result to it } }
        .maxByOrNull { it.second }
        ?.let { (result, wer) -> "${result.metadata.displayName ?: result.metadata.uriString.substringAfterLast('/')} (${fmtPct(wer)})" }
        ?: "n/a"

    companion object {
        fun from(model: VoskModelSpec, results: List<BenchmarkResult>, repeatCount: Int = 1): BatchRunReport = BatchRunReport(
            modelName = model.displayName,
            modelId = model.id,
            createdAtMs = System.currentTimeMillis(),
            results = results.toList(),
            repeatCount = repeatCount
        )
    }
}

private fun BatchRunReport.toMarkdown(): String {
    val lines = mutableListOf<String>()
    lines += "# tl;dh STT Bench Batch Report"
    lines += ""
    lines += "- Modell: $modelName (`$modelId`)"
    lines += "- Zeitpunkt: ${formatHistoryTime(createdAtMs)}"
    lines += "- Läufe: $sampleCount"
    lines += "- Profil: ${repeatCount}× Corpus"
    lines += "- Speed-Pass: $speedPassCount/$speedEvaluatedCount"
    lines += "- Ø RTF: ${avgRtf?.let { fmtNumber(it) } ?: "n/a"}"
    lines += "- Ø WER: ${avgWerPercent?.let { fmtPct(it) } ?: "n/a"}"
    lines += "- Ø CER: ${avgCerPercent?.let { fmtPct(it) } ?: "n/a"}"
    lines += "- Schwächste WER: $worstWerLabel"
    lines += ""
    lines += "| Sample | Dauer | Gesamt | Decode | Modell | STT | RTF | WER | CER | Verdict |"
    lines += "|---|---:|---:|---:|---:|---:|---:|---:|---:|---|"
    results.forEach { result ->
        val comparison = result.referenceComparison
        lines += listOf(
            result.metadata.displayName ?: result.metadata.uriString.substringAfterLast('/'),
            fmtMs(result.timing.audioDurationMs),
            fmtMs(result.timing.totalMs),
            fmtMs(result.timing.decodeMs),
            fmtMs(result.timing.modelLoadMs),
            fmtMs(result.timing.sttMs),
            result.timing.rtf?.let { fmtNumber(it) } ?: "n/a",
            comparison?.werPercent?.let { fmtPct(it) } ?: "n/a",
            comparison?.cerPercent?.let { fmtPct(it) } ?: "n/a",
            result.verdict.message.replace("|", "/")
        ).joinToString(prefix = "| ", separator = " | ", postfix = " |")
    }
    lines += ""
    lines += "## Vollständige Einzeltranskripte"
    results.forEachIndexed { index, result ->
        lines += ""
        lines += "### ${index + 1}. ${result.metadata.displayName ?: result.metadata.uriString.substringAfterLast('/')}"
        lines += ""
        result.referenceComparison?.let { comparison ->
            lines += "- ${comparison.summary}"
            lines += "- Referenz: ${comparison.referenceRaw}"
        }
        lines += "- Erkannt: ${result.transcript.ifBlank { "Kein Transkript erkannt." }}"
    }
    return lines.joinToString("\n")
}

private fun BenchmarkResult.toMarkdown(): String {
    val lines = mutableListOf<String>()
    lines += "# tl;dh STT Bench Einzelreport"
    lines += ""
    lines += "- Engine: $engine"
    lines += "- Modell: $model (`$modelId`)"
    lines += "- Sprache: $language"
    lines += "- Datei: ${metadata.displayName ?: "unbekannt"}"
    lines += "- MIME/Format: ${metadata.mimeType ?: "unbekannt"} / ${metadata.format}"
    lines += "- Dauer: ${fmtMs(timing.audioDurationMs)}"
    lines += "- Gesamt: ${fmtMs(timing.totalMs)}"
    lines += "- Decode: ${fmtMs(timing.decodeMs)}"
    lines += "- Modell-Load: ${fmtMs(timing.modelLoadMs)}"
    lines += "- STT: ${fmtMs(timing.sttMs)}"
    lines += "- RTF: ${timing.rtf?.let { fmtNumber(it) } ?: "n/a"}"
    lines += "- Verdict: ${verdict.message}"
    referenceComparison?.let { comparison ->
        lines += "- Referenzvergleich: ${comparison.summary}"
    }
    if (warnings.isNotEmpty()) {
        lines += ""
        lines += "## Hinweise"
        warnings.forEach { lines += "- $it" }
    }
    lines += ""
    lines += "## Transkript"
    if (segments.isEmpty()) {
        lines += transcript.ifBlank { "Kein Transkript erkannt." }
    } else {
        segments.forEach { segment ->
            lines += "${ts(segment.startSec)}–${ts(segment.endSec)}  ${segment.text}"
        }
    }
    referenceComparison?.let { comparison ->
        lines += ""
        lines += "## Referenz"
        lines += comparison.referenceRaw
        lines += ""
        lines += "## Erkannt"
        lines += comparison.hypothesisRaw.ifBlank { "Kein Transkript erkannt." }
    }
    return lines.joinToString("\n")
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

private fun fmtNumber(value: Double): String = String.format(Locale.GERMANY, "%.2f", value)

private fun formatHistoryTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return "Zeitpunkt unbekannt"
    return SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY).format(Date(timestampMs))
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> String.format(Locale.GERMANY, "%.2f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000L -> String.format(Locale.GERMANY, "%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000L -> String.format(Locale.GERMANY, "%.1f KB", bytes / 1_000.0)
    else -> "$bytes B"
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
