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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.TextFieldColors
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bitsbots.tldhbench.BuildConfig
import dev.bitsbots.tldhbench.bench.BenchmarkResult
import dev.bitsbots.tldhbench.bench.BenchmarkRunner
import dev.bitsbots.tldhbench.bench.GroqSttModelCatalog
import dev.bitsbots.tldhbench.bench.GroqSttModelSpec
import dev.bitsbots.tldhbench.bench.IntegrationDecisionLevel
import dev.bitsbots.tldhbench.bench.IntegrationReadiness
import dev.bitsbots.tldhbench.bench.SttEngineCatalog
import dev.bitsbots.tldhbench.bench.SttEngineReadiness
import dev.bitsbots.tldhbench.bench.SttEngineSpec
import dev.bitsbots.tldhbench.bench.Signal
import dev.bitsbots.tldhbench.bench.VoskModelCatalog
import dev.bitsbots.tldhbench.bench.VoskModelSpec
import dev.bitsbots.tldhbench.bench.WordDiffType
import dev.bitsbots.tldhbench.bench.WhisperModelCatalog
import dev.bitsbots.tldhbench.bench.WhisperModelSpec
import dev.bitsbots.tldhbench.audio.AudioPreparationProfile
import dev.bitsbots.tldhbench.audio.AudioPreparationProfiles
import dev.bitsbots.tldhbench.audio.LongFormAudioComposer
import dev.bitsbots.tldhbench.audio.LongFormProfile
import dev.bitsbots.tldhbench.audio.LongFormProfiles
import dev.bitsbots.tldhbench.corpus.BuiltInReferenceCorpus
import dev.bitsbots.tldhbench.corpus.ReferenceCorpusManager
import dev.bitsbots.tldhbench.corpus.ReferenceSample
import dev.bitsbots.tldhbench.history.BenchmarkHistoryItem
import dev.bitsbots.tldhbench.history.BenchmarkHistoryStore
import dev.bitsbots.tldhbench.models.VoskModelManager
import dev.bitsbots.tldhbench.models.WhisperModelManager
import dev.bitsbots.tldhbench.remote.GroqSettingsStore
import dev.bitsbots.tldhbench.share.AudioSourceKind
import dev.bitsbots.tldhbench.share.SharedAudio
import dev.bitsbots.tldhbench.updates.ApkInstaller
import dev.bitsbots.tldhbench.updates.BenchmarkReleaseSelector
import dev.bitsbots.tldhbench.updates.BenchmarkUpdate
import dev.bitsbots.tldhbench.updates.GitHubReleaseClient
import dev.bitsbots.tldhbench.updates.UpdateDownloadGuard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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


@Composable
private fun benchTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
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
    unfocusedPlaceholderColor = TextMuted
)

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



private fun activeEngineDisplayName(engineId: String): String = when (engineId) {
    "whisper-cpp" -> "whisper.cpp"
    "groq-stt" -> "Groq Speech-to-Text"
    else -> "Vosk Android"
}

private fun activeModelDisplayName(
    engineId: String,
    selectedModel: VoskModelSpec,
    selectedWhisperModel: WhisperModelSpec,
    selectedGroqModel: GroqSttModelSpec
): String = when (engineId) {
    "whisper-cpp" -> selectedWhisperModel.displayName
    "groq-stt" -> selectedGroqModel.displayName
    else -> selectedModel.displayName
}

private fun whisperWatchdogProgress(elapsedSec: Long): Int = when {
    elapsedSec < 3L -> 8
    elapsedSec < 8L -> 14
    elapsedSec < 20L -> 24
    elapsedSec < 45L -> 38
    elapsedSec < 90L -> 52
    elapsedSec < 180L -> 68
    elapsedSec < 360L -> 82
    else -> 92
}

@Composable
fun BenchApp(sharedAudioState: MutableState<SharedAudio?>) {
    val context = LocalContext.current
    val modelManager = remember { VoskModelManager(context) }
    val whisperModelManager = remember { WhisperModelManager(context) }
    val groqSettingsStore = remember { GroqSettingsStore(context) }
    val corpusManager = remember { ReferenceCorpusManager(context) }
    val historyStore = remember { BenchmarkHistoryStore(context) }
    val uiStatePrefs = remember { context.getSharedPreferences("bench_ui_state", Context.MODE_PRIVATE) }
    val initiallyInstalledVoskIds = remember { modelManager.installedIds() }
    val initiallyInstalledWhisperIds = remember { whisperModelManager.installedIds() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedSection by remember { mutableStateOf(BenchSection.Start) }
    var selectedModel by remember {
        mutableStateOf(
            VoskModelCatalog.byId(
                uiStatePrefs.getString("selected_vosk_model_id", VoskModelCatalog.defaultModel.id)
                    ?: VoskModelCatalog.defaultModel.id
            )
        )
    }
    var installedIds by remember { mutableStateOf(initiallyInstalledVoskIds) }
    var selectedWhisperModel by remember {
        mutableStateOf(
            WhisperModelCatalog.byId(
                uiStatePrefs.getString("selected_whisper_model_id", WhisperModelCatalog.defaultModel.id)
                    ?: WhisperModelCatalog.defaultModel.id
            )
        )
    }
    val persistedEngineId = remember { uiStatePrefs.getString("active_engine_id", "vosk") ?: "vosk" }
    var groqApiKeySaved by remember { mutableStateOf(groqSettingsStore.hasApiKey()) }
    var groqApiKeyDraft by remember { mutableStateOf("") }
    var groqPrompt by remember { mutableStateOf(groqSettingsStore.prompt()) }
    var selectedGroqModel by remember {
        mutableStateOf(
            GroqSttModelCatalog.byId(
                uiStatePrefs.getString("selected_groq_model_id", GroqSttModelCatalog.defaultModel.id)
                    ?: GroqSttModelCatalog.defaultModel.id
            )
        )
    }
    var activeEngineId by remember {
        mutableStateOf(
            when {
                persistedEngineId == "whisper-cpp" && initiallyInstalledWhisperIds.isNotEmpty() -> "whisper-cpp"
                persistedEngineId == "groq-stt" && groqApiKeySaved -> "groq-stt"
                else -> "vosk"
            }
        )
    }
    var whisperBusyModelId by remember { mutableStateOf<String?>(null) }
    var installedWhisperIds by remember { mutableStateOf(initiallyInstalledWhisperIds) }
    var installedSampleIds by remember { mutableStateOf(corpusManager.installedIds()) }
    var selectedSample by remember { mutableStateOf<ReferenceSample?>(null) }
    var selectedLongFormLabel by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var busyLabel by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<BenchmarkResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf(historyStore.load()) }
    var historyExpanded by remember { mutableStateOf(false) }
    var referenceText by remember { mutableStateOf("") }
    var batchReport by remember { mutableStateOf<BatchRunReport?>(null) }
    var batchRepeatCount by remember { mutableIntStateOf(1) }
    var selectedAudioPrepProfile by remember {
        mutableStateOf(AudioPreparationProfiles.byId(uiStatePrefs.getString("audio_prep_profile_id", AudioPreparationProfiles.defaultProfile.id)))
    }
    var lastHandledExternalShareKey by remember { mutableStateOf<String?>(null) }
    var currentBenchmarkJob by remember { mutableStateOf<Job?>(null) }

    fun resetBenchmark(clearBatch: Boolean = false) {
        result = null
        error = null
        busyLabel = null
        progress = 0
        if (clearBatch) batchReport = null
    }


    fun activeModelReady(): Boolean = when (activeEngineId) {
        "whisper-cpp" -> installedWhisperIds.contains(selectedWhisperModel.id)
        "groq-stt" -> groqApiKeySaved
        else -> installedIds.contains(selectedModel.id)
    }

    fun activeModelName(): String = activeModelDisplayName(activeEngineId, selectedModel, selectedWhisperModel, selectedGroqModel)

    fun activeModelId(): String = when (activeEngineId) {
        "whisper-cpp" -> selectedWhisperModel.id
        "groq-stt" -> selectedGroqModel.id
        else -> selectedModel.id
    }

    suspend fun runActiveBenchmark(audio: SharedAudio, reference: String?, profile: AudioPreparationProfile): BenchmarkResult = when (activeEngineId) {
        "whisper-cpp" -> BenchmarkRunner(context).runWhisper(audio, selectedWhisperModel, reference, profile)
        "groq-stt" -> BenchmarkRunner(context).runGroq(audio, selectedGroqModel, groqSettingsStore.apiKey(), groqPrompt, reference, profile)
        else -> BenchmarkRunner(context).runVosk(audio, selectedModel, reference, profile)
    }

    fun selectReferenceSample(sample: ReferenceSample) {
        runCatching {
            sharedAudioState.value = corpusManager.sharedAudio(sample)
            selectedSample = sample
            selectedLongFormLabel = null
            referenceText = sample.referenceText
            resetBenchmark()
            selectedSection = BenchSection.Run
        }.onFailure {
            error = "Testaudio konnte nicht ausgewählt werden (${sample.id}): ${it.message}"
            selectedSection = BenchSection.Results
        }
    }

    fun composeLongForm(profile: LongFormProfile) {
        scope.launch {
            error = null
            result = null
            progress = 0
            busyLabel = "Erzeuge ${profile.title}-Longform-Audio…"
            selectedSection = BenchSection.Run
            runCatching {
                LongFormAudioComposer(context, corpusManager).compose(
                    profile = profile,
                    samples = BuiltInReferenceCorpus.longFormSamples
                )
            }.onSuccess { composed ->
                sharedAudioState.value = composed.sharedAudio
                selectedSample = null
                selectedLongFormLabel = composed.displayName
                referenceText = composed.referenceText
                busyLabel = null
                progress = 100
            }.onFailure {
                busyLabel = null
                error = "Longform-Testaudio konnte nicht erzeugt werden (${profile.title}): ${it.message}"
                selectedSection = BenchSection.Results
            }
        }
    }

    fun cancelBenchmark() {
        currentBenchmarkJob?.cancel(CancellationException("Nutzer hat den Benchmark abgebrochen."))
        currentBenchmarkJob = null
        busyLabel = null
        progress = 0
        error = "Benchmark abgebrochen. Hinweis: Native STT-Engines können einen laufenden nativen Aufruf nicht immer sofort hart stoppen; falls whisper.cpp danach blockiert wirkt, im Engines-Tab 'Whisper-Runtime zurücksetzen' nutzen."
        selectedSection = BenchSection.Results
    }

    fun runSingleBenchmark() {
        val audio = sharedAudioState.value ?: return
        if (currentBenchmarkJob?.isActive == true) return
        currentBenchmarkJob = scope.launch {
            error = null
            result = null
            progress = 0
            val startedUiAt = System.currentTimeMillis()
            val activeEngineLabel = activeEngineDisplayName(activeEngineId)
            busyLabel = "$activeEngineLabel Benchmark läuft · 0s"
            selectedSection = BenchSection.Run
            val activeModelAtStart = activeModelDisplayName(activeEngineId, selectedModel, selectedWhisperModel, selectedGroqModel)
            val heartbeat = scope.launch {
                while (busyLabel != null && result == null && error == null) {
                    val elapsedSec = ((System.currentTimeMillis() - startedUiAt) / 1000L).coerceAtLeast(0L)
                    progress = if (activeEngineId == "whisper-cpp") whisperWatchdogProgress(elapsedSec) else 0
                    busyLabel = "$activeEngineLabel Benchmark läuft · $activeModelAtStart · ${elapsedSec}s"
                    delay(1_000L)
                }
            }
            runCatching {
                when (activeEngineId) {
                    "whisper-cpp" -> BenchmarkRunner(context).runWhisper(audio, selectedWhisperModel, referenceText, selectedAudioPrepProfile)
                    "groq-stt" -> BenchmarkRunner(context).runGroq(audio, selectedGroqModel, groqSettingsStore.apiKey(), groqPrompt, referenceText, selectedAudioPrepProfile)
                    else -> BenchmarkRunner(context).runVosk(audio, selectedModel, referenceText, selectedAudioPrepProfile)
                }
            }.onSuccess {
                heartbeat?.cancel()
                progress = 100
                result = it
                historyStore.add(it)
                history = historyStore.load()
                historyExpanded = true
                busyLabel = null
                selectedSection = BenchSection.Results
            }.onFailure {
                heartbeat.cancel()
                busyLabel = null
                progress = 0
                val activeModelName = activeModelDisplayName(activeEngineId, selectedModel, selectedWhisperModel, selectedGroqModel)
                error = if (it is CancellationException) {
                    "Benchmark abgebrochen ($activeModelName)."
                } else {
                    "Benchmark fehlgeschlagen ($activeModelName): ${it.message}"
                }
                selectedSection = BenchSection.Results
            }
            currentBenchmarkJob = null
        }
    }

    fun resetWhisperRuntime() {
        currentBenchmarkJob?.cancel()
        currentBenchmarkJob = null
        runCatching { File(context.cacheDir, "bench-work-whisper").deleteRecursively() }
        runCatching { File(context.cacheDir, "bench-work").deleteRecursively() }
        System.gc()
        busyLabel = null
        progress = 0
        result = null
        batchReport = null
        error = "Whisper-Runtime zurückgesetzt: temporäre WAV-/PCM-Arbeitsdateien wurden gelöscht und ein GC wurde angestoßen. Wenn der native Wrapper trotzdem blockiert, App einmal vollständig schließen und danach mit tiny/base erneut testen."
        selectedSection = BenchSection.Results
    }

    fun runBatchBenchmark() {
        val baseSamples = BuiltInReferenceCorpus.samples.filter { installedSampleIds.contains(it.id) }
        if (baseSamples.isEmpty()) {
            error = "Batch nicht möglich: Lade zuerst mindestens ein Goldstandard-Testaudio."
            selectedSection = BenchSection.Results
            return
        }
        val activeModelInstalled = activeModelReady()
        val activeModelName = activeModelName()
        if (!activeModelInstalled) {
            error = "Batch nicht möglich: Installiere zuerst das aktive Modell $activeModelName."
            selectedSection = BenchSection.Results
            return
        }
        if (activeEngineId == "vosk" && selectedModel.deviceSignal == Signal.RED) {
            error = "Batch nicht möglich: ${selectedModel.displayName} ist auf Android per Crash-Guard blockiert."
            selectedSection = BenchSection.Results
            return
        }
        val batchSamples = buildList {
            repeat(batchRepeatCount.coerceAtLeast(1)) { addAll(baseSamples) }
        }
        if (currentBenchmarkJob?.isActive == true) return
        currentBenchmarkJob = scope.launch {
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
                    runActiveBenchmark(corpusManager.sharedAudio(sample), sample.referenceText, selectedAudioPrepProfile)
                }.onSuccess {
                    results += it
                    historyStore.add(it)
                    history = historyStore.load()
                }.onFailure { throwable ->
                    busyLabel = null
                    error = "Batch-Benchmark fehlgeschlagen ($activeModelName): ${throwable.message}"
                    if (results.isNotEmpty()) batchReport = BatchRunReport.from(activeModelName, activeModelId(), results, batchRepeatCount)
                    selectedSection = BenchSection.Results
                    currentBenchmarkJob = null
                    return@launch
                }
            }
            batchReport = BatchRunReport.from(activeModelName, activeModelId(), results, batchRepeatCount)
            busyLabel = null
            historyExpanded = true
            selectedSection = BenchSection.Results
            currentBenchmarkJob = null
        }
    }

    fun runAudioPrepMatrix() {
        val audio = sharedAudioState.value
        if (audio == null) {
            error = "Audio-Prep-Matrix nicht möglich: keine Audioquelle aktiv."
            selectedSection = BenchSection.Results
            return
        }
        if (referenceText.isBlank()) {
            error = "Audio-Prep-Matrix nicht möglich: Referenztext fehlt. Ohne Referenz kann WER/CER nicht verglichen werden."
            selectedSection = BenchSection.Results
            return
        }
        val activeModelInstalled = activeModelReady()
        val activeModelName = activeModelName()
        if (!activeModelInstalled) {
            error = "Audio-Prep-Matrix nicht möglich: Installiere zuerst das aktive Modell $activeModelName."
            selectedSection = BenchSection.Results
            return
        }
        if (activeEngineId == "vosk" && selectedModel.deviceSignal == Signal.RED) {
            error = "Audio-Prep-Matrix nicht möglich: ${selectedModel.displayName} ist auf Android per Crash-Guard blockiert."
            selectedSection = BenchSection.Results
            return
        }
        if (currentBenchmarkJob?.isActive == true) return
        currentBenchmarkJob = scope.launch {
            error = null
            result = null
            batchReport = null
            progress = 0
            val profiles = AudioPreparationProfiles.matrixProfiles
            busyLabel = "Audio-Prep-Matrix 0/${profiles.size} vorbereitet…"
            selectedSection = BenchSection.Run
            val previousProfile = selectedAudioPrepProfile
            val results = mutableListOf<BenchmarkResult>()
            for ((index, profile) in profiles.withIndex()) {
                runCatching {
                    selectedAudioPrepProfile = profile
                    busyLabel = "Audio-Prep-Matrix ${index + 1}/${profiles.size}: ${profile.shortLabel}"
                    progress = (((index + 1) * 100) / profiles.size).coerceIn(0, 100)
                    runActiveBenchmark(audio, referenceText, profile)
                }.onSuccess {
                    results += it
                    historyStore.add(it)
                    history = historyStore.load()
                }.onFailure { throwable ->
                    selectedAudioPrepProfile = previousProfile
                    busyLabel = null
                    error = "Audio-Prep-Matrix fehlgeschlagen ($activeModelName): ${throwable.message}"
                    if (results.isNotEmpty()) {
                        batchReport = BatchRunReport.from(
                            modelName = activeModelName,
                            modelId = activeModelId(),
                            results = results,
                            repeatCount = 1,
                            reportLabel = "Audio-Prep-Matrix (${results.size}/${profiles.size} Profile)"
                        )
                    }
                    selectedSection = BenchSection.Results
                    currentBenchmarkJob = null
                    return@launch
                }
            }
            selectedAudioPrepProfile = previousProfile
            batchReport = BatchRunReport.from(
                modelName = activeModelName,
                modelId = activeModelId(),
                results = results,
                repeatCount = 1,
                reportLabel = "Audio-Prep-Matrix (${profiles.size} Profile)"
            )
            busyLabel = null
            historyExpanded = true
            selectedSection = BenchSection.Results
            currentBenchmarkJob = null
        }
    }

    LaunchedEffect(sharedAudioState.value?.uri?.toString(), sharedAudioState.value?.createdAtMs, sharedAudioState.value?.sourceKind) {
        val audio = sharedAudioState.value
        if (audio?.sourceKind == AudioSourceKind.EXTERNAL_SHARE) {
            val key = "${audio.uri}|${audio.createdAtMs}"
            if (key != lastHandledExternalShareKey) {
                lastHandledExternalShareKey = key
                selectedSample = null
                selectedLongFormLabel = null
                referenceText = ""
                result = null
                batchReport = null
                error = null
                busyLabel = null
                progress = 0
                selectedSection = BenchSection.Run
            }
        }
    }

    LaunchedEffect(activeEngineId, selectedModel.id, selectedWhisperModel.id, selectedGroqModel.id, selectedAudioPrepProfile.id) {
        uiStatePrefs.edit()
            .putString("active_engine_id", activeEngineId)
            .putString("selected_vosk_model_id", selectedModel.id)
            .putString("selected_whisper_model_id", selectedWhisperModel.id)
            .putString("selected_groq_model_id", selectedGroqModel.id)
            .putString("audio_prep_profile_id", selectedAudioPrepProfile.id)
            .apply()
    }

    LaunchedEffect(selectedSection) {
        scrollState.scrollTo(0)
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
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Header()
                ActiveSetupCard(
                    activeEngineId = activeEngineId,
                    selectedModel = selectedModel,
                    modelInstalled = installedIds.contains(selectedModel.id),
                    selectedWhisperModel = selectedWhisperModel,
                    whisperModelInstalled = installedWhisperIds.contains(selectedWhisperModel.id),
                    selectedGroqModel = selectedGroqModel,
                    groqApiKeySaved = groqApiKeySaved,
                    selectedSample = selectedSample,
                    selectedLongFormLabel = selectedLongFormLabel,
                    sharedAudio = sharedAudioState.value,
                    referenceText = referenceText,
                    selectedAudioPrepProfile = selectedAudioPrepProfile,
                    installedSampleCount = installedSampleIds.size,
                    totalSampleCount = BuiltInReferenceCorpus.samples.size,
                    busyLabel = busyLabel,
                    progress = progress
                )
                SectionNav(selected = selectedSection, onSelect = { selectedSection = it })

                when (selectedSection) {
                    BenchSection.Start -> StartSection(
                        appVersion = BuildConfig.VERSION_NAME,
                        onGoEngines = { selectedSection = BenchSection.Engines },
                        onGoModels = { selectedSection = BenchSection.Models },
                        onGoCorpus = { selectedSection = BenchSection.Corpus },
                        onGoRun = { selectedSection = BenchSection.Run },
                        onGoUpdates = { selectedSection = BenchSection.Updates }
                    )

                    BenchSection.Engines -> EnginesSection(
                        activeEngineId = activeEngineId,
                        selectedWhisperModel = selectedWhisperModel,
                        installedWhisperIds = installedWhisperIds,
                        selectedGroqModel = selectedGroqModel,
                        groqApiKeySaved = groqApiKeySaved,
                        groqApiKeyDraft = groqApiKeyDraft,
                        groqPrompt = groqPrompt,
                        whisperBusyModelId = whisperBusyModelId,
                        busyLabel = busyLabel,
                        progress = progress,
                        onActivateEngine = { engineId ->
                            if (engineId == "whisper-cpp" && installedWhisperIds.isEmpty()) {
                                error = "whisper.cpp kann erst aktiv gesetzt werden, wenn mindestens ein Whisper-Modell geladen ist."
                                selectedSection = BenchSection.Results
                            } else if (engineId == "groq-stt" && !groqApiKeySaved) {
                                error = "Groq Remote-STT kann erst aktiv gesetzt werden, wenn ein Groq API-Key gespeichert ist."
                                selectedSection = BenchSection.Results
                            } else {
                                activeEngineId = engineId
                                result = null
                                error = null
                            }
                        },
                        onSelectWhisper = {
                            selectedWhisperModel = it
                            if (installedWhisperIds.contains(it.id)) activeEngineId = "whisper-cpp"
                        },
                        onDownloadWhisper = { spec ->
                            scope.launch {
                                error = null
                                result = null
                                whisperBusyModelId = spec.id
                                busyLabel = "Download ${spec.displayName}…"
                                progress = 0
                                runCatching { whisperModelManager.download(spec) { progress = it } }
                                    .onSuccess {
                                        installedWhisperIds = whisperModelManager.installedIds()
                                        selectedWhisperModel = spec
                                        activeEngineId = "whisper-cpp"
                                        busyLabel = null
                                        whisperBusyModelId = null
                                        progress = 100
                                    }
                                    .onFailure {
                                        busyLabel = null
                                        whisperBusyModelId = null
                                        error = "Whisper-Modell-Download fehlgeschlagen (${spec.displayName}): ${it.message}"
                                        selectedSection = BenchSection.Results
                                    }
                            }
                        },
                        onDeleteWhisper = { spec ->
                            scope.launch {
                                error = null
                                whisperBusyModelId = spec.id
                                busyLabel = "Lösche ${spec.displayName}…"
                                runCatching { whisperModelManager.delete(spec) }
                                    .onSuccess {
                                        val newInstalledIds = whisperModelManager.installedIds()
                                        installedWhisperIds = newInstalledIds
                                        if (spec.id == selectedWhisperModel.id && newInstalledIds.isNotEmpty()) {
                                            selectedWhisperModel = WhisperModelCatalog.byId(newInstalledIds.first())
                                        }
                                        if (newInstalledIds.isEmpty() && activeEngineId == "whisper-cpp") activeEngineId = "vosk"
                                        busyLabel = null
                                        whisperBusyModelId = null
                                    }
                                    .onFailure {
                                        busyLabel = null
                                        whisperBusyModelId = null
                                        error = "Whisper-Modell konnte nicht gelöscht werden (${spec.displayName}): ${it.message}"
                                        selectedSection = BenchSection.Results
                                    }
                            }
                        },
                        onSelectGroq = { spec ->
                            selectedGroqModel = spec
                            if (groqApiKeySaved) activeEngineId = "groq-stt"
                            result = null
                            error = null
                        },
                        onGroqKeyDraftChange = { groqApiKeyDraft = it },
                        onSaveGroqKey = {
                            if (groqApiKeyDraft.isBlank()) {
                                error = "Groq API-Key Feld ist leer."
                                selectedSection = BenchSection.Results
                            } else {
                                groqSettingsStore.saveApiKey(groqApiKeyDraft)
                                groqApiKeyDraft = ""
                                groqApiKeySaved = true
                                activeEngineId = "groq-stt"
                                error = null
                            }
                        },
                        onClearGroqKey = {
                            groqSettingsStore.clearApiKey()
                            groqApiKeyDraft = ""
                            groqApiKeySaved = false
                            if (activeEngineId == "groq-stt") activeEngineId = "vosk"
                        },
                        onGroqPromptChange = { groqPrompt = it },
                        onSaveGroqPrompt = { groqSettingsStore.savePrompt(groqPrompt) },
                        onResetWhisperRuntime = { resetWhisperRuntime() },
                        onGoModels = { selectedSection = BenchSection.Models },
                        onGoRun = { selectedSection = BenchSection.Run }
                    )

                    BenchSection.Models -> ModelsSection(
                        activeEngineId = activeEngineId,
                        selectedModel = selectedModel,
                        installedIds = installedIds,
                        selectedWhisperModel = selectedWhisperModel,
                        installedWhisperIds = installedWhisperIds,
                        selectedGroqModel = selectedGroqModel,
                        groqApiKeySaved = groqApiKeySaved,
                        groqApiKeyDraft = groqApiKeyDraft,
                        groqPrompt = groqPrompt,
                        whisperBusyModelId = whisperBusyModelId,
                        busyLabel = busyLabel,
                        progress = progress,
                        onSelectVosk = { spec ->
                            selectedModel = spec
                            activeEngineId = "vosk"
                            result = null
                            error = null
                        },
                        onDownloadVosk = { spec ->
                            scope.launch {
                                error = null
                                result = null
                                progress = 0
                                busyLabel = "Download ${spec.displayName}…"
                                runCatching { modelManager.downloadAndInstall(spec) { progress = it } }
                                    .onSuccess {
                                        installedIds = modelManager.installedIds()
                                        selectedModel = spec
                                        activeEngineId = "vosk"
                                        busyLabel = null
                                    }
                                    .onFailure {
                                        busyLabel = null
                                        error = "Modell-Download/Installation fehlgeschlagen (${spec.displayName}): ${it.message}"
                                        installedIds = modelManager.installedIds()
                                        selectedSection = BenchSection.Results
                                    }
                            }
                        },
                        onDeleteVosk = { spec ->
                            scope.launch {
                                error = null
                                result = null
                                busyLabel = if (installedIds.contains(spec.id)) "Lösche ${spec.displayName}…" else "Bereinige ${spec.displayName}…"
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
                        },
                        onSelectWhisper = { spec ->
                            selectedWhisperModel = spec
                            if (installedWhisperIds.contains(spec.id)) activeEngineId = "whisper-cpp"
                            result = null
                            error = null
                        },
                        onDownloadWhisper = { spec ->
                            scope.launch {
                                error = null
                                result = null
                                whisperBusyModelId = spec.id
                                busyLabel = "Download ${spec.displayName}…"
                                progress = 0
                                runCatching { whisperModelManager.download(spec) { progress = it } }
                                    .onSuccess {
                                        installedWhisperIds = whisperModelManager.installedIds()
                                        selectedWhisperModel = spec
                                        activeEngineId = "whisper-cpp"
                                        busyLabel = null
                                        whisperBusyModelId = null
                                        progress = 100
                                    }
                                    .onFailure {
                                        busyLabel = null
                                        whisperBusyModelId = null
                                        error = "Whisper-Modell-Download fehlgeschlagen (${spec.displayName}): ${it.message}"
                                        selectedSection = BenchSection.Results
                                    }
                            }
                        },
                        onDeleteWhisper = { spec ->
                            scope.launch {
                                error = null
                                whisperBusyModelId = spec.id
                                busyLabel = "Lösche ${spec.displayName}…"
                                runCatching { whisperModelManager.delete(spec) }
                                    .onSuccess {
                                        val newInstalledIds = whisperModelManager.installedIds()
                                        installedWhisperIds = newInstalledIds
                                        if (spec.id == selectedWhisperModel.id && newInstalledIds.isNotEmpty()) {
                                            selectedWhisperModel = WhisperModelCatalog.byId(newInstalledIds.first())
                                        }
                                        if (newInstalledIds.isEmpty() && activeEngineId == "whisper-cpp") activeEngineId = "vosk"
                                        busyLabel = null
                                        whisperBusyModelId = null
                                    }
                                    .onFailure {
                                        busyLabel = null
                                        whisperBusyModelId = null
                                        error = "Whisper-Modell konnte nicht gelöscht werden (${spec.displayName}): ${it.message}"
                                        selectedSection = BenchSection.Results
                                    }
                            }
                        },
                        onSelectGroq = { spec ->
                            selectedGroqModel = spec
                            if (groqApiKeySaved) activeEngineId = "groq-stt"
                            result = null
                            error = null
                        },
                        onGroqKeyDraftChange = { groqApiKeyDraft = it },
                        onSaveGroqKey = {
                            if (groqApiKeyDraft.isBlank()) {
                                error = "Groq API-Key Feld ist leer."
                                selectedSection = BenchSection.Results
                            } else {
                                groqSettingsStore.saveApiKey(groqApiKeyDraft)
                                groqApiKeyDraft = ""
                                groqApiKeySaved = true
                                activeEngineId = "groq-stt"
                                error = null
                            }
                        },
                        onClearGroqKey = {
                            groqSettingsStore.clearApiKey()
                            groqApiKeyDraft = ""
                            groqApiKeySaved = false
                            if (activeEngineId == "groq-stt") activeEngineId = "vosk"
                        },
                        onGroqPromptChange = { groqPrompt = it },
                        onSaveGroqPrompt = { groqSettingsStore.savePrompt(groqPrompt) },
                        onSwitchToVosk = { activeEngineId = "vosk" },
                        onSwitchToWhisper = {
                            if (installedWhisperIds.isNotEmpty()) activeEngineId = "whisper-cpp"
                            else {
                                error = "whisper.cpp kann erst aktiv gesetzt werden, wenn mindestens ein Whisper-Modell geladen ist."
                                selectedSection = BenchSection.Results
                            }
                        },
                        onSwitchToGroq = {
                            if (groqApiKeySaved) activeEngineId = "groq-stt"
                            else {
                                error = "Groq Remote-STT kann erst aktiv gesetzt werden, wenn ein API-Key gespeichert ist."
                                selectedSection = BenchSection.Results
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
                                            selectedLongFormLabel = null
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
                                        selectedLongFormLabel = null
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
                        activeEngineId = activeEngineId,
                        selectedModel = selectedModel,
                        selectedModelInstalled = installedIds.contains(selectedModel.id),
                        selectedWhisperModel = selectedWhisperModel,
                        selectedWhisperModelInstalled = installedWhisperIds.contains(selectedWhisperModel.id),
                        selectedGroqModel = selectedGroqModel,
                        groqApiKeySaved = groqApiKeySaved,
                        selectedSample = selectedSample,
                        selectedLongFormLabel = selectedLongFormLabel,
                        referenceText = referenceText,
                        installedSampleCount = installedSampleIds.size,
                        totalSampleCount = BuiltInReferenceCorpus.samples.size,
                        busy = busyLabel != null,
                        progress = progress,
                        busyLabel = busyLabel,
                        batchRepeatCount = batchRepeatCount,
                        batchReport = batchReport,
                        selectedAudioPrepProfile = selectedAudioPrepProfile,
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
                        onComposeLongForm = { profile -> composeLongForm(profile) },
                        onAudioPrepProfileChange = { selectedAudioPrepProfile = it },
                        onRunAudioPrepMatrix = { runAudioPrepMatrix() },
                        onRunSingle = { runSingleBenchmark() },
                        onCancel = { cancelBenchmark() },
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
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

private enum class BenchSection(val label: String) {
    Start("Start"),
    Engines("Engines"),
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
            BenchSection.entries.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { section ->
                        val active = section == selected
                        if (active) {
                            PrimaryActionButton(
                                label = section.label,
                                onClick = { onSelect(section) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            SecondaryActionButton(
                                label = section.label,
                                onClick = { onSelect(section) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    repeat(2 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ActiveSetupCard(
    activeEngineId: String,
    selectedModel: VoskModelSpec,
    modelInstalled: Boolean,
    selectedWhisperModel: WhisperModelSpec,
    whisperModelInstalled: Boolean,
    selectedGroqModel: GroqSttModelSpec,
    groqApiKeySaved: Boolean,
    selectedSample: ReferenceSample?,
    selectedLongFormLabel: String?,
    sharedAudio: SharedAudio?,
    referenceText: String,
    selectedAudioPrepProfile: AudioPreparationProfile,
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
                    val engineLabel = activeEngineDisplayName(activeEngineId)
                    val modelLabel = activeModelDisplayName(activeEngineId, selectedModel, selectedWhisperModel, selectedGroqModel)
                    val modelReady = when (activeEngineId) {
                        "whisper-cpp" -> whisperModelInstalled
                        "groq-stt" -> groqApiKeySaved
                        else -> modelInstalled
                    }
                    Text("Scope: ${if (activeEngineId == "groq-stt") "Remote" else "Local"}", color = if (activeEngineId == "groq-stt") Warn else Good, lineHeight = 18.sp)
                    Text("Engine: $engineLabel", color = if (activeEngineId == "whisper-cpp") Accent2 else if (activeEngineId == "groq-stt") Warn else Good, lineHeight = 18.sp)
                    Text("Modell: $modelLabel", color = if (modelReady) Good else Warn, lineHeight = 18.sp)
                    Text("Audio-Prep: ${selectedAudioPrepProfile.displayName}", color = Accent2, lineHeight = 18.sp)
                    Text("Quelle: ${activeAudioSourceLabel(sharedAudio)}", color = if (sharedAudio != null) Good else TextMuted, lineHeight = 18.sp)
                    Text("Audio: ${activeAudioTitle(sharedAudio, selectedSample, selectedLongFormLabel)}", color = if (sharedAudio != null) TextMain else TextMuted, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text("$installedSampleCount/$totalSampleCount", color = if (installedSampleCount > 0) Good else TextMuted, fontWeight = FontWeight.Bold)
            }
            Text(
                if (referenceText.isBlank()) "Referenzvergleich inaktiv" else "Referenzvergleich aktiv · ${referenceText.wordCount()} Wörter",
                color = if (referenceText.isBlank()) TextMuted else Good,
                fontSize = 13.sp
            )
            busyLabel?.let {
                val benchmarkBusy = it.contains("Benchmark", ignoreCase = true)
                if (benchmarkBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(it, color = TextMuted, fontSize = 13.sp)
                } else {
                    LinearProgressIndicator(progress = { (progress / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    Text("$it $progress%", color = TextMuted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun StartSection(
    appVersion: String,
    onGoEngines: () -> Unit,
    onGoModels: () -> Unit,
    onGoCorpus: () -> Unit,
    onGoRun: () -> Unit,
    onGoUpdates: () -> Unit
) {
    CardBlock(title = "Geführter Ablauf") {
        Text("Version v$appVersion · separater STT-Benchmark. Die Haupt-App tl;dh bleibt unberührt.", color = TextMuted, lineHeight = 20.sp)
        Spacer(Modifier.height(8.dp))
        StepText("1", "Engine-Strategie prüfen", "Vosk ist die Speed-Baseline; whisper.cpp ist nach Modell-Download als zweite ausführbare Offline-Engine testbar.")
        StepText("2", "Vosk-Modell installieren oder wechseln", "Small DE fürs Handy, Big DE als Qualitäts-/Stressprobe.")
        StepText("3", "Goldstandard oder eigene Shared-Audio nutzen", "Für Wortabweichungen braucht die App immer ein korrektes Referenztranskript.")
        StepText("4", "Benchmark starten und Produktentscheidung lesen", "Ergebnis zeigt jetzt zusätzlich, ob diese Engine/Modell-Kombination tl;dh-tauglich wäre.")
        Spacer(Modifier.height(10.dp))
        ActionStack {
            PrimaryActionButton("Engine-Strategie", onGoEngines)
            PrimaryActionButton("Modelle", onGoModels)
            PrimaryActionButton("Goldstandard", onGoCorpus)
            SecondaryActionButton("Benchmark", onGoRun)
            SecondaryActionButton("Updates", onGoUpdates)
        }
    }
    CardBlock(title = "v0.5.0 Fokus") {
        Text("Diese Version ergänzt den Remote-Scope: Groq Speech-to-Text kann mit whisper-large-v3-turbo oder whisper-large-v3 gegen dieselbe Audio-/Referenz-/WER-Pipeline wie Local getestet werden.", color = TextMuted, lineHeight = 20.sp)
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
private fun EnginesSection(
    activeEngineId: String,
    selectedWhisperModel: WhisperModelSpec,
    installedWhisperIds: Set<String>,
    selectedGroqModel: GroqSttModelSpec,
    groqApiKeySaved: Boolean,
    groqApiKeyDraft: String,
    groqPrompt: String,
    whisperBusyModelId: String?,
    busyLabel: String?,
    progress: Int,
    onActivateEngine: (String) -> Unit,
    onSelectWhisper: (WhisperModelSpec) -> Unit,
    onDownloadWhisper: (WhisperModelSpec) -> Unit,
    onDeleteWhisper: (WhisperModelSpec) -> Unit,
    onSelectGroq: (GroqSttModelSpec) -> Unit,
    onGroqKeyDraftChange: (String) -> Unit,
    onSaveGroqKey: () -> Unit,
    onClearGroqKey: () -> Unit,
    onGroqPromptChange: (String) -> Unit,
    onSaveGroqPrompt: () -> Unit,
    onResetWhisperRuntime: () -> Unit,
    onGoModels: () -> Unit,
    onGoRun: () -> Unit
) {
    CardBlock(title = "Engine-Schicht / Kandidaten") {
        Text(
            "Neue Hierarchie: Local → Engine → Model für Vosk/whisper.cpp und Remote → Provider → Model für Groq Speech-to-Text. Alle ausführbaren Pfade laufen gegen dieselbe Referenz-/WER-/CER-Pipeline.",
            color = TextMuted,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(8.dp))
        SttEngineCatalog.engines.forEach { engine ->
            val canActivate = engine.id == "vosk" || (engine.id == "whisper-cpp" && installedWhisperIds.isNotEmpty()) || (engine.id == "groq-stt" && groqApiKeySaved)
            EngineCandidateCard(
                engine = engine,
                active = activeEngineId == engine.id,
                canActivate = canActivate,
                installedWhisperCount = installedWhisperIds.size,
                onActivate = { onActivateEngine(engine.id) }
            )
            Spacer(Modifier.height(10.dp))
        }
        ActionStack {
            PrimaryActionButton("Vosk-Modelle öffnen", onGoModels)
            SecondaryActionButton("Zum Benchmark", onGoRun)
        }
    }
    WhisperModelPrepSection(
        selectedModel = selectedWhisperModel,
        installedIds = installedWhisperIds,
        busyModelId = whisperBusyModelId,
        busyLabel = busyLabel,
        progress = progress,
        onSelect = onSelectWhisper,
        onDownload = onDownloadWhisper,
        onDelete = onDeleteWhisper
    )
    GroqRemoteSettingsCard(
        selectedModel = selectedGroqModel,
        apiKeySaved = groqApiKeySaved,
        apiKeyDraft = groqApiKeyDraft,
        prompt = groqPrompt,
        busy = busyLabel != null,
        onSelectModel = onSelectGroq,
        onApiKeyDraftChange = onGroqKeyDraftChange,
        onSaveApiKey = onSaveGroqKey,
        onClearApiKey = onClearGroqKey,
        onPromptChange = onGroqPromptChange,
        onSavePrompt = onSaveGroqPrompt
    )
    WhisperRuntimeRecoveryCard(
        busy = busyLabel != null,
        onReset = onResetWhisperRuntime
    )
    CardBlock(title = "Messlatte für tl;dh") {
        Text("Produktintegration erst bei echten Audios mit Referenztext. Ziel: deutlich unter 25 % WER; ideal eher <= 15 % WER, geringe Deletions und RTF <= 1,0.", color = TextMuted, lineHeight = 20.sp)
        Text("Vosk Small/Big lagen in Deinem Realtest bei ca. 40–47 % WER. Damit: gute Speed-Baseline, aber noch kein sicherer Summary-Unterbau.", color = Warn, lineHeight = 20.sp)
    }
}

@Composable
private fun WhisperRuntimeRecoveryCard(
    busy: Boolean,
    onReset: () -> Unit
) {
    CardBlock(title = "Whisper Runtime / Hänger") {
        Text(
            "Wenn whisper.cpp nach einem fehlgeschlagenen oder abgebrochenen Lauf keine weiteren Benchmarks mehr startet, kannst Du hier den temporären Whisper-Arbeitsbereich leeren. Das ersetzt keinen echten Prozessneustart, verhindert aber häufig kaputte WAV-/PCM-Reste nach einem Hänger.",
            color = TextMuted,
            lineHeight = 20.sp
        )
        Text(
            "Bei small gilt: Ein langer Lauf kann wirklich minutenlang dauern. v0.5.0 zeigt deshalb bewusst nur Laufstatus/Elapsed-Time statt irreführender Prozentwerte.",
            color = Warn,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        ActionStack {
            SecondaryActionButton("Whisper-Runtime zurücksetzen", onReset, enabled = !busy)
        }
    }
}

@Composable
private fun WhisperModelPrepSection(
    selectedModel: WhisperModelSpec,
    installedIds: Set<String>,
    busyModelId: String?,
    busyLabel: String?,
    progress: Int,
    onSelect: (WhisperModelSpec) -> Unit,
    onDownload: (WhisperModelSpec) -> Unit,
    onDelete: (WhisperModelSpec) -> Unit
) {
    CardBlock(title = "whisper.cpp Modell-Preflight") {
        Text(
            "Vosk- und Whisper-Modellpools sind getrennt. tiny/base/small werden ausschließlich von whisper.cpp genutzt; Small DE/Big DE/TUDA ausschließlich von Vosk Android.",
            color = TextMuted,
            lineHeight = 20.sp
        )
        Text(
            "Nutze zuerst tiny oder base. small ist der erste echte Qualitätskandidat, kann auf dem Handy aber Laufzeit und Akku deutlich stärker belasten.",
            color = Warn,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        WhisperModelCatalog.models.forEach { spec ->
            WhisperModelCard(
                spec = spec,
                selected = selectedModel.id == spec.id,
                installed = installedIds.contains(spec.id),
                busy = busyLabel != null,
                downloading = busyModelId == spec.id,
                progress = progress,
                busyLabel = busyLabel,
                onSelect = { onSelect(spec) },
                onDownload = { onDownload(spec) },
                onDelete = { onDelete(spec) }
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun WhisperModelCard(
    spec: WhisperModelSpec,
    selected: Boolean,
    installed: Boolean,
    busy: Boolean,
    downloading: Boolean,
    progress: Int,
    busyLabel: String?,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val border = when {
        selected -> Accent2
        installed -> Good
        else -> FieldBorder
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF10070C), RoundedCornerShape(18.dp))
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(spec.displayName, color = TextMain, fontWeight = FontWeight.Bold)
                Text("${spec.sizeLabel} · RAM ${spec.memoryLabel}", color = TextMuted, fontSize = 13.sp)
            }
            Text(
                when {
                    selected && installed -> "aktiv"
                    installed -> "bereit"
                    else -> "nicht geladen"
                },
                color = when {
                    selected && installed -> Accent2
                    installed -> Good
                    else -> TextMuted
                },
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SignalChip("Speed", spec.speedSignal)
            SignalChip("Qualität", spec.expectedAccuracySignal)
            SignalChip("Phone", spec.phoneSignal)
        }
        Text(spec.notes, color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        if (downloading) {
            LinearProgressIndicator(progress = { (progress / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text("${busyLabel.orEmpty()} $progress%", color = TextMuted, fontSize = 13.sp)
        }
        ActionStack {
            if (installed) {
                PrimaryActionButton(if (selected) "Whisper aktiv" else "Whisper aktiv setzen", onSelect, enabled = !busy)
                SecondaryActionButton("Whisper-Modell löschen", onDelete, enabled = !busy)
            } else {
                SecondaryActionButton("Whisper-Modell laden", onDownload, enabled = !busy)
            }
        }
    }
}

@Composable
private fun EngineCandidateCard(
    engine: SttEngineSpec,
    active: Boolean,
    canActivate: Boolean,
    installedWhisperCount: Int,
    onActivate: () -> Unit
) {
    val color = when {
        active -> Good
        engine.readiness == SttEngineReadiness.NEXT_CANDIDATE -> Accent2
        engine.readiness == SttEngineReadiness.PLANNED -> Warn
        engine.readiness == SttEngineReadiness.EXTERNAL -> TextMain
        else -> Good
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF10070C), RoundedCornerShape(18.dp))
            .border(1.dp, color.copy(alpha = if (active) 0.95f else 0.55f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(engine.displayName, color = TextMain, fontWeight = FontWeight.Bold)
                Text(engine.localMode, color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Text(if (active) "aktiv" else engine.shortLabel, color = color, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        }
        Text("Stärke: ${engine.expectedStrength}", color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        Text("Risiko: ${engine.expectedRisk}", color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        Text("Nächster Schritt: ${engine.nextStep}", color = color, fontSize = 13.sp, lineHeight = 18.sp)
        if (engine.id == "whisper-cpp") {
            Text(
                if (installedWhisperCount > 0) "Whisper-Modelle bereit: $installedWhisperCount. Engine ist ausführbar; starte im Benchmark-Tab mit derselben Audio-/Referenz-Pipeline." else "Noch kein Whisper-Modell geladen. Nach dem ersten Download kann whisper.cpp aktiv gesetzt und benchmarked werden.",
                color = if (installedWhisperCount > 0) Good else Warn,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        ActionStack {
            when {
                active -> SecondaryActionButton("Aktiv", onClick = {}, enabled = false)
                canActivate && engine.id in setOf("vosk", "whisper-cpp", "groq-stt") -> PrimaryActionButton("Als aktive Engine setzen", onActivate)
                engine.id == "whisper-cpp" -> SecondaryActionButton("Erst Whisper-Modell laden", onClick = {}, enabled = false)
                engine.id == "groq-stt" -> SecondaryActionButton("Erst Groq-Key speichern", onClick = {}, enabled = false)
                else -> SecondaryActionButton("Noch nicht auswählbar", onClick = {}, enabled = false)
            }
        }
    }
}

@Composable
private fun ModelsSection(
    activeEngineId: String,
    selectedModel: VoskModelSpec,
    installedIds: Set<String>,
    selectedWhisperModel: WhisperModelSpec,
    installedWhisperIds: Set<String>,
    selectedGroqModel: GroqSttModelSpec,
    groqApiKeySaved: Boolean,
    groqApiKeyDraft: String,
    groqPrompt: String,
    whisperBusyModelId: String?,
    busyLabel: String?,
    progress: Int,
    onSelectVosk: (VoskModelSpec) -> Unit,
    onDownloadVosk: (VoskModelSpec) -> Unit,
    onDeleteVosk: (VoskModelSpec) -> Unit,
    onSelectWhisper: (WhisperModelSpec) -> Unit,
    onDownloadWhisper: (WhisperModelSpec) -> Unit,
    onDeleteWhisper: (WhisperModelSpec) -> Unit,
    onSelectGroq: (GroqSttModelSpec) -> Unit,
    onGroqKeyDraftChange: (String) -> Unit,
    onSaveGroqKey: () -> Unit,
    onClearGroqKey: () -> Unit,
    onGroqPromptChange: (String) -> Unit,
    onSaveGroqPrompt: () -> Unit,
    onSwitchToVosk: () -> Unit,
    onSwitchToWhisper: () -> Unit,
    onSwitchToGroq: () -> Unit
) {
    val whisperActive = activeEngineId == "whisper-cpp"
    val groqActive = activeEngineId == "groq-stt"
    val activePoolLabel = when {
        groqActive -> "Remote · Groq Speech-to-Text API-Modelle"
        whisperActive -> "Local · whisper.cpp · Whisper ggml-Modelle"
        else -> "Local · Vosk Android · Vosk-Modellordner"
    }

    CardBlock(title = "Aktiver Scope / Modellpool") {
        Text("Scope: ${if (groqActive) "Remote" else "Local"}", color = if (groqActive) Warn else Good, fontWeight = FontWeight.Bold)
        Text("Aktive Engine: ${activeEngineDisplayName(activeEngineId)}", color = TextMain, fontWeight = FontWeight.Bold)
        Text("Dieser Tab zeigt nur den Modellpool der aktiven Engine: $activePoolLabel.", color = TextMuted, lineHeight = 20.sp)
        ActionStack {
            if (!groqActive) SecondaryActionButton("Zu Groq Remote wechseln", onSwitchToGroq, enabled = busyLabel == null)
            if (!whisperActive) SecondaryActionButton("Zu whisper.cpp wechseln", onSwitchToWhisper, enabled = busyLabel == null)
            if (activeEngineId != "vosk") SecondaryActionButton("Zu Vosk Android wechseln", onSwitchToVosk, enabled = busyLabel == null)
        }
    }

    when {
        groqActive -> GroqModelPoolSection(
            selectedModel = selectedGroqModel,
            apiKeySaved = groqApiKeySaved,
            apiKeyDraft = groqApiKeyDraft,
            prompt = groqPrompt,
            busy = busyLabel != null,
            onSelect = onSelectGroq,
            onApiKeyDraftChange = onGroqKeyDraftChange,
            onSaveApiKey = onSaveGroqKey,
            onClearApiKey = onClearGroqKey,
            onPromptChange = onGroqPromptChange,
            onSavePrompt = onSaveGroqPrompt
        )
        whisperActive -> WhisperModelPoolSection(
            selectedModel = selectedWhisperModel,
            installedIds = installedWhisperIds,
            busyModelId = whisperBusyModelId,
            busyLabel = busyLabel,
            progress = progress,
            onSelect = onSelectWhisper,
            onDownload = onDownloadWhisper,
            onDelete = onDeleteWhisper
        )
        else -> VoskModelPoolSection(
            selectedModel = selectedModel,
            installedIds = installedIds,
            busyLabel = busyLabel,
            progress = progress,
            onSelect = onSelectVosk,
            onDownload = onDownloadVosk,
            onDelete = onDeleteVosk
        )
    }
}


@Composable
private fun GroqRemoteSettingsCard(
    selectedModel: GroqSttModelSpec,
    apiKeySaved: Boolean,
    apiKeyDraft: String,
    prompt: String,
    busy: Boolean,
    onSelectModel: (GroqSttModelSpec) -> Unit,
    onApiKeyDraftChange: (String) -> Unit,
    onSaveApiKey: () -> Unit,
    onClearApiKey: () -> Unit,
    onPromptChange: (String) -> Unit,
    onSavePrompt: () -> Unit
) {
    CardBlock(title = "Remote / Groq Speech-to-Text") {
        Text(
            "Remote-Scope für High-End-Vergleich: Groq transkribiert über die OpenAI-kompatible Audio-Transcriptions-API. Die Bench-App nutzt dieselbe Referenztext-/WER-/CER-/S/I/D-Auswertung wie Local.",
            color = TextMuted,
            lineHeight = 20.sp
        )
        Text(
            if (apiKeySaved) "API-Key gespeichert · Remote-Benchmarks können gestartet werden." else "API-Key fehlt · Groq bleibt blockiert, bis Du einen Key speicherst.",
            color = if (apiKeySaved) Good else Warn,
            fontWeight = FontWeight.SemiBold
        )
        Text("Aktives Groq-Modell: ${selectedModel.displayName}", color = TextMain, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = apiKeyDraft,
            onValueChange = onApiKeyDraftChange,
            label = { Text("Groq API-Key", color = TextMuted) },
            placeholder = { Text(if (apiKeySaved) "Neuen Key einfügen, um zu ersetzen" else "gsk_…", color = TextMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = benchTextFieldColors()
        )
        ActionStack {
            PrimaryActionButton(if (apiKeySaved) "Groq-Key ersetzen" else "Groq-Key speichern", onSaveApiKey, enabled = !busy)
            SecondaryActionButton("Groq-Key löschen", onClearApiKey, enabled = !busy && apiKeySaved)
        }
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            label = { Text("Groq Prompt / Kontext", color = TextMuted) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            colors = benchTextFieldColors()
        )
        ActionStack {
            SecondaryActionButton("Prompt speichern", onSavePrompt, enabled = !busy)
        }
        Text(
            "Datenschutz-Hinweis: Diese Bench-Version speichert den API-Key app-intern. Für Produktbetrieb: Android Keystore/Encrypted Storage, explizites Opt-in pro Audio und sichtbarer Provider-/Retention-Hinweis.",
            color = Warn,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun GroqModelPoolSection(
    selectedModel: GroqSttModelSpec,
    apiKeySaved: Boolean,
    apiKeyDraft: String,
    prompt: String,
    busy: Boolean,
    onSelect: (GroqSttModelSpec) -> Unit,
    onApiKeyDraftChange: (String) -> Unit,
    onSaveApiKey: () -> Unit,
    onClearApiKey: () -> Unit,
    onPromptChange: (String) -> Unit,
    onSavePrompt: () -> Unit
) {
    GroqRemoteSettingsCard(
        selectedModel = selectedModel,
        apiKeySaved = apiKeySaved,
        apiKeyDraft = apiKeyDraft,
        prompt = prompt,
        busy = busy,
        onSelectModel = onSelect,
        onApiKeyDraftChange = onApiKeyDraftChange,
        onSaveApiKey = onSaveApiKey,
        onClearApiKey = onClearApiKey,
        onPromptChange = onPromptChange,
        onSavePrompt = onSavePrompt
    )
    CardBlock(title = "Groq Modelle · nur Remote") {
        Text("Diese Modelle werden nicht lokal ausgeführt. Die Audio wird mit dem gewählten Audio-Prep-Profil als 16 kHz Mono WAV vorbereitet und an Groq hochgeladen.", color = TextMuted, lineHeight = 20.sp)
        Spacer(Modifier.height(8.dp))
        GroqSttModelCatalog.models.forEach { spec ->
            GroqModelCard(
                spec = spec,
                selected = selectedModel.id == spec.id,
                apiKeySaved = apiKeySaved,
                busy = busy,
                onSelect = { onSelect(spec) }
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun GroqModelCard(
    spec: GroqSttModelSpec,
    selected: Boolean,
    apiKeySaved: Boolean,
    busy: Boolean,
    onSelect: () -> Unit
) {
    val border = when {
        selected && apiKeySaved -> Warn
        selected -> Accent2
        else -> FieldBorder
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF10070C), RoundedCornerShape(18.dp))
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(spec.displayName, color = TextMain, fontWeight = FontWeight.Bold)
                Text(spec.sizeLabel, color = TextMuted, fontSize = 13.sp)
            }
            Text(
                if (selected) "aktiv" else "remote",
                color = if (selected) Warn else TextMuted,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SignalChip("Speed", spec.speedSignal)
            SignalChip("Qualität", spec.accuracySignal)
            SignalChip("Privacy", spec.privacySignal)
        }
        Text(spec.tradeoff, color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        Text(spec.notes, color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        ActionStack {
            PrimaryActionButton(if (selected) "Groq-Modell aktiv" else "Groq-Modell aktiv setzen", onSelect, enabled = !busy)
        }
    }
}

@Composable
private fun VoskModelPoolSection(
    selectedModel: VoskModelSpec,
    installedIds: Set<String>,
    busyLabel: String?,
    progress: Int,
    onSelect: (VoskModelSpec) -> Unit,
    onDownload: (VoskModelSpec) -> Unit,
    onDelete: (VoskModelSpec) -> Unit
) {
    CardBlock(title = "Vosk Modelle · nur Vosk Android") {
        Text("Installieren, auswählen, danach ohne App-Neustart mit Vosk benchmarken. Diese Modelle werden von whisper.cpp nicht verwendet.", color = TextMuted, lineHeight = 20.sp)
        Text("Wenn ein Download beim Entpacken abbricht, nutze beim betroffenen Modell zuerst 'Lokale Reste bereinigen' und starte danach den Download neu.", color = TextMuted, lineHeight = 20.sp)
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
private fun WhisperModelPoolSection(
    selectedModel: WhisperModelSpec,
    installedIds: Set<String>,
    busyModelId: String?,
    busyLabel: String?,
    progress: Int,
    onSelect: (WhisperModelSpec) -> Unit,
    onDownload: (WhisperModelSpec) -> Unit,
    onDelete: (WhisperModelSpec) -> Unit
) {
    CardBlock(title = "Whisper Modelle · nur whisper.cpp") {
        Text("Diese Modelle gehören ausschließlich zur whisper.cpp Engine und werden nicht von Vosk Android verwendet.", color = TextMuted, lineHeight = 20.sp)
        Text("Nutze zuerst tiny/base für Stabilität; small ist der erste stärkere Qualitätskandidat, aber deutlich schwerer für das Handy.", color = Warn, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(8.dp))
        WhisperModelCatalog.models.forEach { spec ->
            WhisperModelCard(
                spec = spec,
                selected = selectedModel.id == spec.id,
                installed = installedIds.contains(spec.id),
                busy = busyLabel != null,
                downloading = busyModelId == spec.id,
                progress = progress,
                busyLabel = busyLabel,
                onSelect = { onSelect(spec) },
                onDownload = { onDownload(spec) },
                onDelete = { onDelete(spec) }
            )
            Spacer(Modifier.height(10.dp))
        }
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
    CardBlock(title = "Longform-Nutzung") {
        Text("Der Korpus enthält jetzt deutlich mehr saubere Referenzsätze. Im Benchmark-Bereich kannst Du daraus echte einzelne WAV-Testaudios mit ca. 30 Sekunden, 90 Sekunden oder 4 Minuten erzeugen. Der Referenztext wird automatisch aus denselben Sätzen zusammengesetzt.", color = TextMuted, lineHeight = 20.sp)
    }
}

@Composable
private fun RunSection(
    sharedAudio: SharedAudio?,
    activeEngineId: String,
    selectedModel: VoskModelSpec,
    selectedModelInstalled: Boolean,
    selectedWhisperModel: WhisperModelSpec,
    selectedWhisperModelInstalled: Boolean,
    selectedGroqModel: GroqSttModelSpec,
    groqApiKeySaved: Boolean,
    selectedSample: ReferenceSample?,
    selectedLongFormLabel: String?,
    referenceText: String,
    installedSampleCount: Int,
    totalSampleCount: Int,
    busy: Boolean,
    progress: Int,
    busyLabel: String?,
    batchRepeatCount: Int,
    batchReport: BatchRunReport?,
    selectedAudioPrepProfile: AudioPreparationProfile,
    onReferenceTextChange: (String) -> Unit,
    onPasteReference: () -> Unit,
    onClearReference: () -> Unit,
    onComposeLongForm: (LongFormProfile) -> Unit,
    onAudioPrepProfileChange: (AudioPreparationProfile) -> Unit,
    onRunAudioPrepMatrix: () -> Unit,
    onRunSingle: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onBatchRepeatChange: (Int) -> Unit,
    onRunBatch: () -> Unit,
    onCopyBatchReport: (BatchRunReport) -> Unit,
    onClearBatchReport: () -> Unit
) {
    val activeModelInstalled = when (activeEngineId) {
        "whisper-cpp" -> selectedWhisperModelInstalled
        "groq-stt" -> groqApiKeySaved
        else -> selectedModelInstalled
    }

    BenchmarkActionCard(
        sharedAudio = sharedAudio,
        activeEngineId = activeEngineId,
        selectedModel = selectedModel,
        selectedModelInstalled = selectedModelInstalled,
        selectedWhisperModel = selectedWhisperModel,
        selectedWhisperModelInstalled = selectedWhisperModelInstalled,
        selectedGroqModel = selectedGroqModel,
        groqApiKeySaved = groqApiKeySaved,
        selectedSample = selectedSample,
        selectedLongFormLabel = selectedLongFormLabel,
        referenceText = referenceText,
        selectedAudioPrepProfile = selectedAudioPrepProfile,
        busy = busy,
        progress = progress,
        busyLabel = busyLabel,
        onRunSingle = onRunSingle,
        onCancel = onCancel,
        onReset = onReset
    )
    MetricHelpCard()
    AudioPrepExperimentCard(
        sharedAudio = sharedAudio,
        referenceText = referenceText,
        activeModelInstalled = activeModelInstalled,
        busy = busy,
        busyLabel = busyLabel,
        progress = progress,
        selectedProfile = selectedAudioPrepProfile,
        onProfileChange = onAudioPrepProfileChange,
        onRunMatrix = onRunAudioPrepMatrix,
        onCancel = onCancel
    )
    LongFormScenarioCard(
        installedCount = installedSampleCount,
        totalCount = totalSampleCount,
        selectedModelInstalled = activeModelInstalled,
        busy = busy,
        onCompose = onComposeLongForm
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
        activeEngineId = activeEngineId,
        selectedModel = selectedModel,
        selectedModelInstalled = selectedModelInstalled,
        selectedWhisperModel = selectedWhisperModel,
        selectedWhisperModelInstalled = selectedWhisperModelInstalled,
        selectedGroqModel = selectedGroqModel,
        groqApiKeySaved = groqApiKeySaved,
        busy = busy,
        progress = progress,
        busyLabel = busyLabel,
        batchRepeatCount = batchRepeatCount,
        batchReport = batchReport,
        onRepeatChange = onBatchRepeatChange,
        onRunBatch = onRunBatch,
        onCancel = onCancel,
        onCopyReport = onCopyBatchReport,
        onClearReport = onClearBatchReport
    )
}

@Composable
private fun BenchmarkActionCard(
    sharedAudio: SharedAudio?,
    activeEngineId: String,
    selectedModel: VoskModelSpec,
    selectedModelInstalled: Boolean,
    selectedWhisperModel: WhisperModelSpec,
    selectedWhisperModelInstalled: Boolean,
    selectedGroqModel: GroqSttModelSpec,
    groqApiKeySaved: Boolean,
    selectedSample: ReferenceSample?,
    selectedLongFormLabel: String?,
    referenceText: String,
    selectedAudioPrepProfile: AudioPreparationProfile,
    busy: Boolean,
    progress: Int,
    busyLabel: String?,
    onRunSingle: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit
) {
    CardBlock(title = "Einzelbenchmark") {
        ActiveAudioBox(
            sharedAudio = sharedAudio,
            selectedSample = selectedSample,
            selectedLongFormLabel = selectedLongFormLabel,
            referenceText = referenceText
        )
        Spacer(Modifier.height(8.dp))
        val activeEngineLabel = activeEngineDisplayName(activeEngineId)
        val activeModelLabel = activeModelDisplayName(activeEngineId, selectedModel, selectedWhisperModel, selectedGroqModel)
        val activeModelInstalled = when (activeEngineId) {
            "whisper-cpp" -> selectedWhisperModelInstalled
            "groq-stt" -> groqApiKeySaved
            else -> selectedModelInstalled
        }
        Text("Scope: ${if (activeEngineId == "groq-stt") "Remote" else "Local"}", color = if (activeEngineId == "groq-stt") Warn else Good, fontWeight = FontWeight.SemiBold)
        Text("Aktive Engine: $activeEngineLabel", color = TextMain, fontWeight = FontWeight.SemiBold)
        Text("Aktives Modell: $activeModelLabel", color = TextMain, fontWeight = FontWeight.SemiBold)
        Text("Audio-Prep: ${selectedAudioPrepProfile.displayName}", color = Accent2, fontWeight = FontWeight.SemiBold)
        Text(selectedAudioPrepProfile.description, color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        if (!activeModelInstalled) Text(if (activeEngineId == "groq-stt") "Groq API-Key fehlt. Im Engines-Tab speichern." else "Ausgewähltes Modell ist noch nicht installiert.", color = Warn)
        if (activeEngineId == "whisper-cpp") {
            Text(
                "whisper.cpp ist aktiv. Erste Integration: Gesamttranskript mit Segment-Zeitstempeln; WER/CER/S/I/D funktionieren mit Referenztext.",
                color = Warn,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        if (activeEngineId == "groq-stt") {
            Text(
                "Groq Remote ist aktiv. Die App bereitet Audio lokal als 16 kHz Mono WAV vor und lädt sie dann an Groq hoch. Referenzvergleich läuft danach identisch lokal.",
                color = Warn,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        if (sharedAudio?.sourceKind == AudioSourceKind.EXTERNAL_SHARE && referenceText.isBlank()) {
            Text(
                "Eigene Shared-Audio ist bereit. Für WER/CER/S/I/D musst Du darunter noch den korrekten Referenztext einfügen; sonst misst die App nur Speed und erzeugt das erkannte Transkript.",
                color = Warn,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        if (activeEngineId == "vosk" && selectedModel.deviceSignal == Signal.RED) {
            Text(
                "Android-Crash-Guard aktiv: Dieses Modell ist als Handy-ungeeignet markiert und wird auf dem Gerät nicht gestartet. Nutze Big DE 0.21 oder später den Tower/LAN-Modus.",
                color = Bad,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        ActionStack {
            val canRun = sharedAudio != null && activeModelInstalled && !busy && (activeEngineId != "vosk" || selectedModel.deviceSignal != Signal.RED)
            PrimaryActionButton("Diese Audio benchmarken", onRunSingle, enabled = canRun)
            if (busy) {
                SecondaryActionButton("Benchmark abbrechen", onCancel, enabled = true)
            } else {
                SecondaryActionButton("Lauf zurücksetzen", onReset, enabled = true)
            }
        }
        if (busyLabel?.contains("Benchmark") == true && !busyLabel.startsWith("Batch")) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(busyLabel, color = TextMuted)
                Text(
                    if (activeEngineId == "whisper-cpp")
                        "Hinweis: Die aktuelle Whisper-Integration liefert keinen echten Token-Progress. Die App zeigt deshalb bewusst keinen Prozentwert, sondern nur Laufstatus/Elapsed-Time."
                    else
                        "Hinweis: Vosk liefert hier ebenfalls keinen stabilen Zwischenfortschritt. Der Laufstatus ist absichtlich indeterminate.",
                    color = Warn,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}


@Composable
private fun AudioPrepExperimentCard(
    sharedAudio: SharedAudio?,
    referenceText: String,
    activeModelInstalled: Boolean,
    busy: Boolean,
    busyLabel: String?,
    progress: Int,
    selectedProfile: AudioPreparationProfile,
    onProfileChange: (AudioPreparationProfile) -> Unit,
    onRunMatrix: () -> Unit,
    onCancel: () -> Unit
) {
    CardBlock(title = "Audio-Prep-Matrix / Real-Audio-Optimierung") {
        Text(
            "Vergleicht dieselbe Audio gegen dieselbe Referenz mit mehreren deterministischen Audio-Prep-Profilen. Ziel: herausfinden, ob Normalisierung, Sprachband-Filterung oder aggressiveres Gate WER/CER wirklich verbessern — statt nur gefühlt besser zu sein.",
            color = TextMuted,
            lineHeight = 20.sp
        )
        Text("Aktives Profil für Einzelbenchmark: ${selectedProfile.displayName}", color = TextMain, fontWeight = FontWeight.SemiBold)
        Text(selectedProfile.description, color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        ActionStack {
            AudioPreparationProfiles.all.forEach { profile ->
                if (profile.id == selectedProfile.id) {
                    PrimaryActionButton(profile.shortLabel, { onProfileChange(profile) }, enabled = !busy)
                } else {
                    SecondaryActionButton(profile.shortLabel, { onProfileChange(profile) }, enabled = !busy)
                }
            }
        }
        Text(
            "Matrix-Profile: Original, Basic Gate, Normalisiert, Voice-Band + Basic, Aggressives Gate. Der Report zeigt danach pro Profil RTF/WER/CER/S/I/D.",
            color = Warn,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        val canRunMatrix = sharedAudio != null && referenceText.isNotBlank() && activeModelInstalled && !busy
        ActionStack {
            PrimaryActionButton("Audio-Prep-Matrix starten", onRunMatrix, enabled = canRunMatrix)
            if (busy && busyLabel?.startsWith("Audio-Prep-Matrix") == true) {
                SecondaryActionButton("Matrix abbrechen", onCancel, enabled = true)
            }
        }
        if (referenceText.isBlank()) {
            Text("Matrix benötigt Referenztext, sonst kann sie keine WER/CER-Verbesserung messen.", color = Warn, fontSize = 13.sp, lineHeight = 18.sp)
        }
        if (busyLabel?.startsWith("Audio-Prep-Matrix") == true) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(progress = { (progress / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text("$busyLabel · $progress%", color = TextMuted)
            }
        }
    }
}
@Composable
private fun ActiveAudioBox(
    sharedAudio: SharedAudio?,
    selectedSample: ReferenceSample?,
    selectedLongFormLabel: String?,
    referenceText: String
) {
    val ready = sharedAudio != null
    val title = activeAudioTitle(sharedAudio, selectedSample, selectedLongFormLabel)
    val source = activeAudioSourceLabel(sharedAudio)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (ready) Color(0xFF0F1D17) else Color(0xFF1A0A12), RoundedCornerShape(18.dp))
            .border(1.dp, if (ready) Good else FieldBorder, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(if (ready) "Audioquelle aktiv / bereit" else "Keine Audioquelle aktiv", color = if (ready) Good else TextMuted, fontWeight = FontWeight.Bold)
        Text("Quelle: $source", color = TextMain, fontWeight = FontWeight.SemiBold)
        Text(title, color = TextMuted, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text(
            if (referenceText.isBlank()) "Referenz: fehlt → keine Wortabweichungen möglich" else "Referenz: aktiv · ${referenceText.wordCount()} Wörter → WER/CER/S/I/D wird berechnet",
            color = if (referenceText.isBlank()) Warn else Good,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
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
            PrimaryActionButton("Zum Benchmark", onGoRun)
        }
    }
    error?.let { ErrorCard(it) }
    result?.let { current ->
        ResultCard(result = current, onReset = onReset, onCopyReport = { onCopyResult(current) })
    }
    batchReport?.let { report ->
        CardBlock(title = "Batch-Report") {
            BatchReportBlock(report)
            ActionStack {
                SecondaryActionButton("Report kopieren", { onCopyBatch(report) })
                SecondaryActionButton("Report leeren", onClearBatch)
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
                PrimaryActionButton(
                    label = "Nach Bench-Update suchen",
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
                    }
                )
                if (!repositoryConfigured) Text("GitHub Repository wurde im Build nicht gesetzt. Release-Builds aus GitHub Actions konfigurieren das automatisch.", color = Bad, lineHeight = 20.sp)
            }
            BenchUpdateUiState.Checking -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Prüfe Benchmark-Releases …", color = TextMuted)
            }
            BenchUpdateUiState.UpToDate -> {
                Text("Du nutzt bereits den aktuellsten Benchmark-Release.", color = Good)
                SecondaryActionButton("Erneut prüfen", { updateState = BenchUpdateUiState.Idle })
            }
            is BenchUpdateUiState.Available -> {
                Text("Benchmark-Update verfügbar: v${current.update.version}", color = Good, fontWeight = FontWeight.Bold)
                Text(current.update.apk.name, color = TextMuted)
                current.update.apk.sizeBytes?.let { Text("Größe: ${formatBytes(it)}", color = TextMuted) }
                Text("Während des Downloads bleibt die App wach. Nach SHA256-Prüfung wird der Android-Installer geöffnet.", color = TextMuted, lineHeight = 20.sp)
                ActionStack {
                    PrimaryActionButton("Download", {
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
                    })
                    SecondaryActionButton("Abbrechen", { updateState = BenchUpdateUiState.Idle })
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
                PrimaryActionButton("Installieren", { context.startActivity(ApkInstaller(context).createInstallIntent(current.apkFile)) })
            }
            is BenchUpdateUiState.Error -> {
                Text("Update-Prüfung fehlgeschlagen", color = Bad, fontWeight = FontWeight.Bold)
                Text(current.message, color = TextMuted, lineHeight = 20.sp)
                SecondaryActionButton("Zurück", { updateState = BenchUpdateUiState.Idle })
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
private fun BenchButtonText(label: String) {
    Text(
        text = label,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontSize = 14.sp,
        lineHeight = 17.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true
) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled, colors = benchButtonColors()) {
        BenchButtonText(label)
    }
}

@Composable
private fun SecondaryActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true
) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled) {
        BenchButtonText(label)
    }
}

@Composable
private fun ActionStack(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("tl;dh STT Bench", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Engine-Vergleich: Speed vs. Deutsch-Qualität.", color = TextMuted)
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
    var expanded by remember { mutableStateOf(false) }
    CardBlock(title = "Goldstandard-Testaudios") {
        Text(
            "Saubere deutsche Referenz-Audios inklusive korrektem Text. Die App vergleicht nach dem Benchmark den erkannten Vosk-Text gegen diese Referenz und berechnet WER/CER.",
            color = TextMuted,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Quelle: rhasspy/dataset-voice-kerstin · CC0-1.0 · ${samples.size} kuratierte Sätze für Starter- und Longform-Profile",
            color = TextMain,
            fontWeight = FontWeight.SemiBold
        )
        selectedSample?.let {
            Text("Aktiv: ${it.id} · ${it.title}", color = Good, fontWeight = FontWeight.SemiBold)
        }
        Text("Installiert: $installedCount/${samples.size}", color = if (installedCount > 0) Good else TextMuted)
        ActionStack {
            PrimaryActionButton("Goldstandard laden", onDownloadAll, enabled = !busy && installedCount < samples.size)
            SecondaryActionButton(if (expanded) "Liste einklappen" else "Liste anzeigen", { expanded = !expanded })
            SecondaryActionButton("Corpus löschen", onClear, enabled = !busy && installedCount > 0)
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
        ActionStack {
            PrimaryActionButton(if (installed) "Neu laden" else "Download", onDownload, enabled = !busy)
            SecondaryActionButton("Als Testaudio nutzen", onSelect, enabled = installed && !busy)
            if (installed) {
                SecondaryActionButton("Löschen", onDelete, enabled = !busy)
            }
        }
    }
}


@Composable
private fun MetricHelpCard() {
    CardBlock(title = "Metriken kurz erklärt") {
        Text("WER = Wortfehlerrate: alle Wortabweichungen geteilt durch die Anzahl der Referenzwörter.", color = TextMuted, lineHeight = 19.sp)
        Text("CER = Zeichenfehlerrate: dasselbe Prinzip auf Buchstaben-/Ziffernebene.", color = TextMuted, lineHeight = 19.sp)
        Text("S/I/D = Substitutionen / Insertions / Deletions: ersetzte, zusätzlich erkannte oder fehlende Wörter.", color = TextMuted, lineHeight = 19.sp)
        Text("Die Abweichungsübersicht erscheint nach dem Benchmark im Ergebnis. Bei eigenen Shared-Audios nur dann, wenn Du einen korrekten Referenztext einfügst.", color = Good, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun LongFormScenarioCard(
    installedCount: Int,
    totalCount: Int,
    selectedModelInstalled: Boolean,
    busy: Boolean,
    onCompose: (LongFormProfile) -> Unit
) {
    CardBlock(title = "Realistische Longform-Testaudios") {
        Text(
            "Erzeugt aus mehreren echten CC0-Referenzaufnahmen ein einzelnes WAV-Testaudio mit automatisch zusammengesetztem Referenztranskript. Danach läuft der normale Einzelbenchmark gegen genau diese Audio. Der Referenzvergleich erfolgt gegen das erkannte STT-Transkript der aktiven Engine.",
            color = TextMuted,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(8.dp))
        Text("Goldstandard-Audios bereit: $installedCount/$totalCount", color = if (installedCount > 0) Good else TextMuted)
        Text(
            if (selectedModelInstalled) "Aktives Modell installiert: ja" else "Aktives Modell installiert: nein",
            color = if (selectedModelInstalled) Good else Warn
        )
        Text(
            "Profile: 30 Sekunden, 90 Sekunden und 4 Minuten. Für 4 Minuten werden bei Bedarf unterschiedliche Sätze zyklisch genutzt; der Report bleibt trotzdem WER/CER-fähig.",
            color = TextMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        ActionStack {
            LongFormProfiles.profiles.forEach { profile ->
                PrimaryActionButton(
                    label = "${profile.title} erzeugen",
                    onClick = { onCompose(profile) },
                    enabled = !busy && installedCount > 0 && selectedModelInstalled
                )
                Text(profile.description, color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun BatchBenchmarkCard(
    installedCount: Int,
    totalCount: Int,
    activeEngineId: String,
    selectedModel: VoskModelSpec,
    selectedModelInstalled: Boolean,
    selectedWhisperModel: WhisperModelSpec,
    selectedWhisperModelInstalled: Boolean,
    selectedGroqModel: GroqSttModelSpec,
    groqApiKeySaved: Boolean,
    busy: Boolean,
    progress: Int,
    busyLabel: String?,
    batchRepeatCount: Int,
    batchReport: BatchRunReport?,
    onRepeatChange: (Int) -> Unit,
    onRunBatch: () -> Unit,
    onCancel: () -> Unit,
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
        val activeEngineLabel = activeEngineDisplayName(activeEngineId)
        val activeModelLabel = activeModelDisplayName(activeEngineId, selectedModel, selectedWhisperModel, selectedGroqModel)
        val activeModelInstalled = when (activeEngineId) {
            "whisper-cpp" -> selectedWhisperModelInstalled
            "groq-stt" -> groqApiKeySaved
            else -> selectedModelInstalled
        }
        Text("Scope: ${if (activeEngineId == "groq-stt") "Remote" else "Local"}", color = if (activeEngineId == "groq-stt") Warn else Good, fontWeight = FontWeight.SemiBold)
        Text("Aktive Engine: $activeEngineLabel", color = TextMain, fontWeight = FontWeight.SemiBold)
        Text("Aktives Modell: $activeModelLabel", color = TextMain, fontWeight = FontWeight.SemiBold)
        Text("Goldstandard-Audios bereit: $installedCount/$totalCount", color = if (installedCount > 0) Good else TextMuted)
        Text(if (activeModelInstalled) "Modell installiert: ja" else "Modell installiert: nein", color = if (activeModelInstalled) Good else Warn)
        if (activeEngineId == "vosk" && selectedModel.deviceSignal == Signal.RED) Text("Android-Crash-Guard: Dieses Modell wird auf dem Handy nicht gestartet.", color = Bad, fontSize = 13.sp, lineHeight = 18.sp)
        Text("Geplante Läufe: ${installedCount * batchRepeatCount} · Profil: ${batchRepeatCount}× Corpus", color = TextMain, fontWeight = FontWeight.SemiBold)
        Text("1×/3×/8×/20× bedeutet: kompletter geladener Goldstandard-Korpus wird so oft wiederholt. Das ist ein Batch-Langlauf, kein einzelnes zusammengeklebtes Audio.", color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        ActionStack {
            listOf(1, 3, 8, 20).forEach { repeatCount ->
                val label = "${repeatCount}× Corpus"
                if (repeatCount == batchRepeatCount) {
                    PrimaryActionButton(label, { onRepeatChange(repeatCount) }, enabled = !busy)
                } else {
                    SecondaryActionButton(label, { onRepeatChange(repeatCount) }, enabled = !busy)
                }
            }
        }
        Text("Für realistische Einzel-Audios mit ca. 30 Sekunden, 90 Sekunden oder 4 Minuten nutze die Longform-Testaudios oben. Für echte WhatsApp-/Telegram-Dateien teile die Audio direkt an diese App.", color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)
        ActionStack {
            PrimaryActionButton("Batch starten", onRunBatch, enabled = !busy && installedCount > 0 && activeModelInstalled && (activeEngineId != "vosk" || selectedModel.deviceSignal != Signal.RED))
            if (busy && busyLabel?.startsWith("Batch") == true) {
                SecondaryActionButton("Batch abbrechen", onCancel, enabled = true)
            }
            SecondaryActionButton("Report kopieren", { batchReport?.let(onCopyReport) }, enabled = !busy && batchReport != null)
            SecondaryActionButton("Leeren", onClearReport, enabled = !busy && batchReport != null)
        }
        if (busyLabel?.startsWith("Batch") == true) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(progress = { (progress / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text("$busyLabel · $progress%", color = TextMuted)
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
        Text("${report.modelName} · ${report.sampleCount} Läufe · ${report.reportLabel} · ${formatHistoryTime(report.createdAtMs)}", color = TextMuted)
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
                "• ${extractAudioPrepLabel(item)} · ${item.metadata.displayName ?: item.metadata.uriString.substringAfterLast('/')} · Gesamt ${fmtMs(item.timing.totalMs)} · RTF ${item.timing.rtf?.let { fmtNumber(it) } ?: "n/a"} · Wortfehler ${comparison?.wordDistance?.toString() ?: "n/a"} · WER ${comparison?.werPercent?.let { fmtPct(it) } ?: "n/a"} · S/I/D ${comparison?.let { "${it.wordSubstitutions}/${it.wordInsertions}/${it.wordDeletions}" } ?: "n/a"}",
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
            "Optional: korrekten Text zur Audio einfügen. Nach dem Benchmark berechnet die App WER und CER gegen das erkannte STT-Transkript.",
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
        ActionStack {
            SecondaryActionButton("Aus Zwischenablage", onPasteFromClipboard)
            SecondaryActionButton("Referenz leeren", onClear, enabled = referenceText.isNotBlank())
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
        ActionStack {
            SecondaryActionButton(if (expanded) "Historie ausblenden" else "Historie anzeigen", onToggle, enabled = history.isNotEmpty())
            SecondaryActionButton("Historie löschen", onClear, enabled = history.isNotEmpty())
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
        if (spec.deviceSignal == Signal.RED) Text("Crash-Guard: Download erlaubt, Benchmark auf Android blockiert.", color = Bad, fontSize = 13.sp, lineHeight = 18.sp)
        ActionStack {
            PrimaryActionButton(if (installed) "Neu laden" else "Download", onDownload, enabled = !busy)
            SecondaryActionButton(if (installed) "Löschen" else "Lokale Reste bereinigen", onDelete, enabled = !busy)
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
    var diffExpanded by remember { mutableStateOf(true) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF10070C), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Abweichungsübersicht", color = TextMain, fontWeight = FontWeight.Bold)
        Text(comparison.summary, color = comparisonColor(comparison.werPercent), fontWeight = FontWeight.SemiBold)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF170A12), RoundedCornerShape(14.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            MetricLine("Wortabweichungen gesamt", "${comparison.wordDistance} von ${comparison.referenceWordCount} Referenzwörtern")
            MetricLine("S — ersetzt", comparison.wordSubstitutions.toString())
            MetricLine("I — zusätzlich erkannt", comparison.wordInsertions.toString())
            MetricLine("D — fehlt in Erkennung", comparison.wordDeletions.toString())
            MetricLine("Zeichenfehler", "${comparison.charDistance} von ${comparison.referenceCharCount} Zeichen")
        }
        Text(
            "WER ist der Blick auf Wörter; CER ist der Blick auf Zeichen. S/I/D sind die konkreten Wortfehler hinter der WER.",
            color = TextMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
        if (comparison.wordDiffs.isNotEmpty()) {
            OutlinedButton(onClick = { diffExpanded = !diffExpanded }) {
                Text(if (diffExpanded) "Wortfehlerliste einklappen" else "Wortfehlerliste anzeigen")
            }
            AnimatedVisibility(diffExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    comparison.wordDiffs.take(40).forEachIndexed { index, diff ->
                        Text("${index + 1}. ${formatWordDiff(diff)}", color = TextMuted, lineHeight = 18.sp)
                    }
                    if (comparison.wordDiffs.size > 40) {
                        Text("… ${comparison.wordDiffs.size - 40} weitere Wortabweichungen im kopierten Report/Transkript prüfen.", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
        } else {
            Text("Keine Wortabweichungen gefunden.", color = Good, fontSize = 13.sp)
        }
        OutlinedButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Referenz/Erkennung einklappen" else "Referenz und Erkennung anzeigen")
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
private fun MetricLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = TextMain, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

private fun formatWordDiff(diff: dev.bitsbots.tldhbench.bench.WordDiff): String = when (diff.type) {
    WordDiffType.SUBSTITUTE -> "Wort ${diff.referenceIndex}: '${diff.referenceWord}' → '${diff.hypothesisWord}'"
    WordDiffType.INSERT -> "zusätzlich erkannt bei Wort ${diff.hypothesisIndex}: '${diff.hypothesisWord}'"
    WordDiffType.DELETE -> "fehlt ab Referenz-Wort ${diff.referenceIndex}: '${diff.referenceWord}'"
}


@Composable
private fun IntegrationDecisionBlock(result: BenchmarkResult) {
    val decision = IntegrationReadiness.evaluate(result)
    val color = when (decision.level) {
        IntegrationDecisionLevel.PASS -> Good
        IntegrationDecisionLevel.GUARDED -> Warn
        IntegrationDecisionLevel.BLOCKED -> Bad
        IntegrationDecisionLevel.UNKNOWN -> TextMuted
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF10070C), RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("tl;dh-Produktentscheidung", color = TextMain, fontWeight = FontWeight.Bold)
        Text(decision.title, color = color, fontWeight = FontWeight.SemiBold)
        Text(decision.message, color = TextMuted, lineHeight = 19.sp)
        Text(decision.details, color = color, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun NoReferenceComparisonBlock() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A0A12), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Keine Wortabweichungen berechnet", color = Warn, fontWeight = FontWeight.Bold)
        Text("Für diese Audio war kein korrekter Referenztext aktiv. Die App kann deshalb Speed, Formatdaten und erkanntes Transkript zeigen, aber keine WER/CER/S/I/D-Wortabweichungen.", color = TextMuted, lineHeight = 19.sp)
        Text("Bei eigenen Shared-Audios: Referenztext im Benchmark-Tab einfügen und denselben Lauf erneut starten.", color = Good, fontSize = 13.sp, lineHeight = 18.sp)
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
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ergebnis", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(result.model, color = TextMuted)
                ActionStack {
                    SecondaryActionButton("Report kopieren", onCopyReport)
                    SecondaryActionButton("Reset", onReset)
                }
            }
            Text(result.verdict.message, color = passColor, fontWeight = FontWeight.SemiBold)
            Text("Gesamt: ${fmtMs(result.timing.totalMs)} · Audio: ${fmtMs(result.timing.audioDurationMs)} · RTF: ${result.timing.rtf?.let { String.format(Locale.US, "%.2f", it) } ?: "n/a"}", color = TextMain)
            Text("Decode: ${fmtMs(result.timing.decodeMs)} · Modell: ${fmtMs(result.timing.modelLoadMs)} · STT: ${fmtMs(result.timing.sttMs)}", color = TextMuted)

            IntegrationDecisionBlock(result)
            result.referenceComparison?.let { ReferenceComparisonBlock(it) } ?: NoReferenceComparisonBlock()

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
                    Text("Komplettes erkanntes Transkript", color = TextMain, fontWeight = FontWeight.Bold)
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
    val repeatCount: Int = 1,
    val reportLabel: String = "${repeatCount}× Corpus"
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
        fun from(
            modelName: String,
            modelId: String,
            results: List<BenchmarkResult>,
            repeatCount: Int = 1,
            reportLabel: String = "${repeatCount}× Corpus"
        ): BatchRunReport = BatchRunReport(
            modelName = modelName,
            modelId = modelId,
            createdAtMs = System.currentTimeMillis(),
            results = results.toList(),
            repeatCount = repeatCount,
            reportLabel = reportLabel
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
    lines += "- Profil: $reportLabel"
    lines += "- Speed-Pass: $speedPassCount/$speedEvaluatedCount"
    lines += "- Ø RTF: ${avgRtf?.let { fmtNumber(it) } ?: "n/a"}"
    lines += "- Ø WER: ${avgWerPercent?.let { fmtPct(it) } ?: "n/a"}"
    lines += "- Ø CER: ${avgCerPercent?.let { fmtPct(it) } ?: "n/a"}"
    lines += "- Schwächste WER: $worstWerLabel"
    lines += ""
    lines += "| Prep | Sample | Dauer | Gesamt | Decode | Modell | STT | RTF | Wortfehler | S/I/D | WER | CER | Verdict |"
    lines += "|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|"
    results.forEach { result ->
        val comparison = result.referenceComparison
        lines += listOf(
            extractAudioPrepLabel(result),
            result.metadata.displayName ?: result.metadata.uriString.substringAfterLast('/'),
            fmtMs(result.timing.audioDurationMs),
            fmtMs(result.timing.totalMs),
            fmtMs(result.timing.decodeMs),
            fmtMs(result.timing.modelLoadMs),
            fmtMs(result.timing.sttMs),
            result.timing.rtf?.let { fmtNumber(it) } ?: "n/a",
            comparison?.wordDistance?.toString() ?: "n/a",
            comparison?.let { "${it.wordSubstitutions}/${it.wordInsertions}/${it.wordDeletions}" } ?: "n/a",
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
        lines += "- Audio-Prep: ${extractAudioPrepLabel(result)}"
        result.referenceComparison?.let { comparison ->
            lines += "- ${comparison.summary}"
            lines += "- Wortfehler: ${comparison.wordDistance} von ${comparison.referenceWordCount}; S/I/D ${comparison.wordSubstitutions}/${comparison.wordInsertions}/${comparison.wordDeletions}"
            if (comparison.wordDiffs.isNotEmpty()) {
                lines += "- Erste Wortabweichungen: " + comparison.wordDiffs.take(25).joinToString("; ") { formatWordDiff(it) }
            }
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
    lines += "- Audio-Prep: ${extractAudioPrepLabel(this)}"
    lines += "- Datei: ${metadata.displayName ?: "unbekannt"}"
    lines += "- MIME/Format: ${metadata.mimeType ?: "unbekannt"} / ${metadata.format}"
    lines += "- Dauer: ${fmtMs(timing.audioDurationMs)}"
    lines += "- Gesamt: ${fmtMs(timing.totalMs)}"
    lines += "- Decode: ${fmtMs(timing.decodeMs)}"
    lines += "- Modell-Load: ${fmtMs(timing.modelLoadMs)}"
    lines += "- STT: ${fmtMs(timing.sttMs)}"
    lines += "- RTF: ${timing.rtf?.let { fmtNumber(it) } ?: "n/a"}"
    lines += "- Verdict: ${verdict.message}"
    IntegrationReadiness.evaluate(this).let { decision ->
        lines += "- Produktentscheidung: ${decision.title} — ${decision.message}"
    }
    referenceComparison?.let { comparison ->
        lines += "- Referenzvergleich: ${comparison.summary}"
        lines += "- Wortfehler: ${comparison.wordDistance} von ${comparison.referenceWordCount}; S/I/D ${comparison.wordSubstitutions}/${comparison.wordInsertions}/${comparison.wordDeletions}"
        if (comparison.wordDiffs.isNotEmpty()) {
            lines += "- Erste Wortabweichungen: " + comparison.wordDiffs.take(40).joinToString("; ") { formatWordDiff(it) }
        }
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

private fun extractAudioPrepLabel(result: BenchmarkResult): String =
    result.warnings.firstOrNull { it.startsWith("Audio-Prep-Profil:") }
        ?.substringAfter("Audio-Prep-Profil:")
        ?.substringBefore("—")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: "n/a"

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()


private fun activeAudioSourceLabel(sharedAudio: SharedAudio?): String = when (sharedAudio?.sourceKind) {
    AudioSourceKind.EXTERNAL_SHARE -> "Geteilte Audio aus Android Share"
    AudioSourceKind.GOLDSTANDARD_SAMPLE -> "Goldstandard-Testaudio"
    AudioSourceKind.GENERATED_LONGFORM -> "Generiertes Longform-Testaudio"
    null -> "keine Quelle"
}

private fun activeAudioTitle(
    sharedAudio: SharedAudio?,
    selectedSample: ReferenceSample?,
    selectedLongFormLabel: String?
): String {
    if (sharedAudio == null) return "Teile eine WhatsApp/Telegram-Audio an diese App oder wähle ein Goldstandard-/Longform-Testaudio."
    return when (sharedAudio.sourceKind) {
        AudioSourceKind.EXTERNAL_SHARE -> "${sharedAudio.displayName ?: sharedAudio.uri.toString().substringAfterLast('/')} · ${sharedAudio.mimeType ?: "MIME unbekannt"}"
        AudioSourceKind.GOLDSTANDARD_SAMPLE -> selectedSample?.let { "${it.id} · ${it.title} · Referenz automatisch gesetzt" }
            ?: (sharedAudio.displayName ?: sharedAudio.uri.toString().substringAfterLast('/'))
        AudioSourceKind.GENERATED_LONGFORM -> selectedLongFormLabel ?: (sharedAudio.displayName ?: "Longform-Testaudio")
    }
}

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
