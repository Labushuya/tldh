package dev.bitsbots.tldh.ui

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import dev.bitsbots.tldh.analysis.ReplyTone
import dev.bitsbots.tldh.audio.AudioIngestException
import dev.bitsbots.tldh.audio.AudioIngestor
import dev.bitsbots.tldh.audio.WaveformSampler
import dev.bitsbots.tldh.session.SessionManager
import dev.bitsbots.tldh.share.SharedAudio
import dev.bitsbots.tldh.share.ShareIntentReader
import dev.bitsbots.tldh.summarization.RuleBasedSummaryEngine
import dev.bitsbots.tldh.transcription.DeviceProfiler
import dev.bitsbots.tldh.transcription.PcmAudioPreparer
import dev.bitsbots.tldh.transcription.TranscriptionOutcome
import dev.bitsbots.tldh.transcription.TranscriptionStatus
import dev.bitsbots.tldh.transcription.WhisperCliTranscriber
import dev.bitsbots.tldh.transcription.WhisperModelCatalog
import dev.bitsbots.tldh.transcription.WhisperModelSpec
import dev.bitsbots.tldh.transcription.WhisperModelStore
import dev.bitsbots.tldh.transcription.WhisperRuntimeLocator
import dev.bitsbots.tldh.ui.screens.ErrorScreen
import dev.bitsbots.tldh.ui.screens.ProcessingScreen
import dev.bitsbots.tldh.ui.screens.RecognitionStatus
import dev.bitsbots.tldh.ui.screens.ResultScreen
import dev.bitsbots.tldh.ui.screens.SettingsScreen
import dev.bitsbots.tldh.ui.screens.SetupScreen
import dev.bitsbots.tldh.ui.screens.StartScreen
import dev.bitsbots.tldh.ui.screens.StatusTone
import dev.bitsbots.tldh.ui.theme.LocalReducedMotion
import dev.bitsbots.tldh.ui.theme.TldhColors
import dev.bitsbots.tldh.ui.theme.TldhMotion
import dev.bitsbots.tldh.updates.UpdateDownloadGuard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val PREFS = "tldh_settings"
private const val KEY_TONE = "reply_tone"
private const val KEY_MARK_UNCERTAIN = "mark_uncertain"
private const val WAVEFORM_BARS = 72

private const val TOAST_SESSION_DELETED = "Session gelöscht. Audio und Transkript sind weg."
private const val TOAST_CANCELLED = "Abgebrochen. Nichts wurde gespeichert."

/** What is on screen: the settings page or the current step of the voice-note flow. */
private sealed interface Screen {
    val key: String

    data object Settings : Screen {
        override val key: String = "settings"
    }

    data class Flow(val state: TldhUiState) : Screen {
        override val key: String
            get() = when (state) {
                TldhUiState.Idle -> "start"
                is TldhUiState.NeedsModel -> "setup"
                is TldhUiState.Processing -> "processing"
                is TldhUiState.Result -> "result"
                is TldhUiState.Error -> "error"
            }
    }
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
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val reducedMotion = LocalReducedMotion.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    var state by remember { mutableStateOf<TldhUiState>(TldhUiState.Idle) }
    var showSettings by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var processingJob by remember { mutableStateOf<Job?>(null) }
    var runCounter by remember { mutableIntStateOf(0) }

    val modelStore = remember { WhisperModelStore(context) }
    val deviceProfile = remember { DeviceProfiler.read(context) }
    val runtime = remember { WhisperRuntimeLocator.locate(context, deviceProfile.cpu) }
    val recommendedModel = remember { WhisperModelCatalog.recommendedFor(deviceProfile) }
    var installedModels by remember { mutableStateOf(modelStore.installedModels()) }
    var activeModel by remember { mutableStateOf(modelStore.activeModel()) }
    var modelDownload by remember { mutableStateOf<ModelDownload?>(null) }
    var modelError by remember { mutableStateOf<String?>(null) }

    var tone by remember {
        mutableStateOf(ReplyTone.entries.firstOrNull { it.name == prefs.getString(KEY_TONE, null) } ?: ReplyTone.FREUNDLICH)
    }
    var markUncertain by remember { mutableStateOf(prefs.getBoolean(KEY_MARK_UNCERTAIN, true)) }
    val updates = remember { if (updaterEnabled) UpdateController(context, scope, appVersion, repositorySlug) else null }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(3200)
            toast = null
        }
    }

    val keepAwake = state is TldhUiState.Processing || modelDownload != null || updates?.state is UpdateUiState.Downloading
    DisposableEffect(keepAwake, view) {
        view.keepScreenOn = keepAwake
        onDispose { view.keepScreenOn = false }
    }

    fun updateProcessing(token: Int, change: (ProcessingInfo) -> ProcessingInfo) {
        val current = state
        if (current is TldhUiState.Processing && current.info.token == token) {
            state = TldhUiState.Processing(change(current.info))
        }
    }

    /** Ends the current voice note: stops work, deletes audio and transcript, back to start. */
    fun endSession(message: String?) {
        processingJob?.cancel()
        processingJob = null
        sessionManager.wipeCurrentSession()
        state = TldhUiState.Idle
        toast = message
    }

    fun process(pending: PendingAudio) {
        showSettings = false
        toast = null
        if (runtime == null) {
            state = TldhUiState.Error(
                "Auf diesem Handy nicht möglich",
                "tl;dh enthält die Spracherkennung nur für 64-Bit-ARM-Handys (arm64-v8a)."
            )
            return
        }
        val model = activeModel
        if (model == null) {
            state = TldhUiState.NeedsModel(pending)
            return
        }
        processingJob?.cancel()
        runCounter += 1
        val token = runCounter
        val title = UiText.title(pending.origin)
        state = TldhUiState.Processing(ProcessingInfo(token = token, title = title, subtitle = UiText.sourceLabel(pending.origin, null)))
        processingJob = scope.launch {
            state = try {
                // One voice note per session: the previous one's audio and transcript go first.
                sessionManager.wipeCurrentSession()
                val workDir = File(context.cacheDir, sessionManager.newSessionId())
                val prepareStarted = SystemClock.elapsedRealtime()
                val metadata = withContext(Dispatchers.IO) { AudioIngestor(context).inspect(pending.audio) }
                val subtitle = UiText.subtitle(pending.origin, metadata.displayName, metadata.durationMs)
                updateProcessing(token) { it.copy(subtitle = subtitle) }

                val prepared = PcmAudioPreparer(context, workDir).prepare(pending.audio.uri, metadata.durationMs)
                val levels = withContext(Dispatchers.IO) {
                    runCatching { WaveformSampler.levels(prepared.file, WAVEFORM_BARS) }.getOrDefault(emptyList())
                }
                val prepareMs = SystemClock.elapsedRealtime() - prepareStarted
                updateProcessing(token) {
                    it.copy(stage = ProcessingStage.Transcribe, prepareMs = prepareMs, levels = levels, progress = 0)
                }

                val transcribeStarted = SystemClock.elapsedRealtime()
                val run = WhisperCliTranscriber(runtime).transcribe(
                    pcmFile = prepared.file,
                    sampleRate = prepared.sampleRate,
                    model = modelStore.modelFile(model),
                    workDir = workDir,
                    audioDurationMs = metadata.durationMs,
                    onSegment = { segment ->
                        scope.launch { updateProcessing(token) { it.copy(liveText = (it.liveText + " " + segment).trim()) } }
                    }
                ) { progress ->
                    val elapsed = SystemClock.elapsedRealtime() - transcribeStarted
                    scope.launch {
                        updateProcessing(token) {
                            it.copy(progress = progress, remainingSeconds = UiText.estimateRemainingSeconds(progress, elapsed))
                        }
                    }
                }
                updateProcessing(token) { it.copy(stage = ProcessingStage.Summarize, progress = 100) }

                val outcome = TranscriptionOutcome(
                    status = if (run.transcript.isBlank()) TranscriptionStatus.FAILED else TranscriptionStatus.COMPLETED,
                    transcript = run.transcript.takeIf { it.isNotBlank() },
                    message = if (run.transcript.isBlank()) "Whisper hat in dieser Audio keine Sprache erkannt." else "Lokal transkribiert.",
                    elapsedMs = run.elapsedMs,
                    modelName = "${model.displayName} (${model.qualityLabel}) · ${runtime.variant}",
                    words = run.words,
                    pcmFile = prepared.file,
                    sampleRate = prepared.sampleRate
                )
                val source = when (pending.origin) {
                    AudioOrigin.SHARE -> "Teilen (ACTION_SEND)"
                    AudioOrigin.FILE -> "Audiodatei öffnen (ACTION_OPEN_DOCUMENT)"
                }
                val summary = withContext(Dispatchers.Default) { RuleBasedSummaryEngine().summarize(metadata, outcome, source) }
                TldhUiState.Result(
                    summary = summary,
                    transcription = outcome,
                    title = title,
                    subtitle = listOfNotNull(subtitle, UiText.transcribedIn(run.elapsedMs)).joinToString(" · ")
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: AudioIngestException) {
                sessionManager.wipeCurrentSession()
                TldhUiState.Error("Diese Datei geht leider nicht", error.message ?: "Die Datei konnte nicht gelesen werden.")
            } catch (error: Throwable) {
                sessionManager.wipeCurrentSession()
                TldhUiState.Error("Das hat nicht geklappt", error.message ?: "Die Audiodatei konnte nicht verarbeitet werden.")
            }
        }
    }

    fun activateModel(spec: WhisperModelSpec) {
        modelStore.setActiveModel(spec)
        activeModel = spec
        modelError = null
    }

    /** After setup: continue with the waiting voice note, or report readiness on the start screen. */
    fun continueAfterSetup() {
        val current = state
        if (current is TldhUiState.NeedsModel) {
            val pending = current.pending
            if (pending != null) {
                process(pending)
            } else {
                state = TldhUiState.Idle
                toast = "Erkennung ist bereit."
            }
        }
    }

    fun downloadModel(spec: WhisperModelSpec) {
        if (modelDownload != null) return
        scope.launch {
            modelError = null
            modelDownload = ModelDownload(spec, 0f)
            try {
                UpdateDownloadGuard(context).runGuardedDownload {
                    modelStore.download(spec) { progress -> scope.launch { modelDownload = ModelDownload(spec, progress) } }
                }
                installedModels = modelStore.installedModels()
                activateModel(spec)
                modelDownload = null
                continueAfterSetup()
            } catch (cancelled: CancellationException) {
                modelDownload = null
                throw cancelled
            } catch (error: Throwable) {
                modelDownload = null
                modelError = error.message ?: "Modell-Download fehlgeschlagen."
            }
        }
    }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            process(PendingAudio(SharedAudio(uri = uri, mimeType = context.contentResolver.getType(uri)), AudioOrigin.FILE))
        }
    }

    fun openFile() {
        runCatching { openDocument.launch(arrayOf("audio/*", "application/ogg")) }
            .onFailure { toast = "Auf diesem Handy gibt es keine Dateiauswahl." }
    }

    LaunchedEffect(currentIntent) {
        ShareIntentReader.read(currentIntent)?.let { process(PendingAudio(it, AudioOrigin.SHARE)) }
    }

    BackHandler(enabled = showSettings || state !is TldhUiState.Idle) {
        when {
            showSettings -> showSettings = false
            state is TldhUiState.Result -> endSession(TOAST_SESSION_DELETED)
            state is TldhUiState.Processing -> endSession(TOAST_CANCELLED)
            else -> endSession(null)
        }
    }

    val models = ModelUiState(
        runtimeAvailable = runtime != null,
        recommended = recommendedModel,
        installed = installedModels,
        active = activeModel,
        download = modelDownload,
        error = modelError
    )
    val recognition = when {
        runtime == null -> RecognitionStatus("Nicht verfügbar", StatusTone.Unavailable)
        modelDownload != null -> RecognitionStatus("Wird geladen · ${((modelDownload?.progress ?: 0f) * 100).toInt()} %", StatusTone.Busy)
        else -> activeModel?.let { RecognitionStatus("${it.displayName} · bereit", StatusTone.Ready) }
            ?: RecognitionStatus("Nicht eingerichtet", StatusTone.Attention)
    }

    AnimatedContent(
        targetState = if (showSettings) Screen.Settings else Screen.Flow(state),
        contentKey = { it.key },
        transitionSpec = {
            if (reducedMotion) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                (fadeIn(tween(TldhMotion.ENTER_MS, easing = TldhMotion.Enter)) +
                    slideInVertically(tween(TldhMotion.ENTER_MS, easing = TldhMotion.Enter)) { it / 24 }) togetherWith
                    fadeOut(tween(140))
            }
        },
        label = "screen",
        modifier = Modifier
            .fillMaxSize()
            .background(TldhColors.Background)
    ) { screen ->
        when (screen) {
            Screen.Settings -> SettingsScreen(
                models = models,
                onSelectModel = { spec -> if (spec in installedModels) activateModel(spec) else downloadModel(spec) },
                onDeleteModels = {
                    WhisperModelCatalog.models.forEach { modelStore.delete(it) }
                    installedModels = modelStore.installedModels()
                    activeModel = modelStore.activeModel()
                },
                tone = tone,
                onToneChange = { selected ->
                    tone = selected
                    prefs.edit().putString(KEY_TONE, selected.name).apply()
                },
                markUncertain = markUncertain,
                onMarkUncertainChange = { enabled ->
                    markUncertain = enabled
                    prefs.edit().putBoolean(KEY_MARK_UNCERTAIN, enabled).apply()
                },
                updates = updates,
                appVersion = appVersion,
                channel = channel,
                repositorySlug = repositorySlug,
                onBack = { showSettings = false }
            )
            is Screen.Flow -> when (val flow = screen.state) {
                TldhUiState.Idle -> StartScreen(
                    recognition = recognition,
                    toast = toast,
                    onOpenFile = { openFile() },
                    onOpenSettings = { showSettings = true }
                )
                is TldhUiState.NeedsModel -> SetupScreen(
                    models = models,
                    hasPendingAudio = flow.pending != null,
                    onDownload = { spec -> downloadModel(spec) },
                    onUseInstalled = { spec ->
                        activateModel(spec)
                        continueAfterSetup()
                    },
                    onClose = { endSession(if (flow.pending != null) TOAST_CANCELLED else null) }
                )
                is TldhUiState.Processing -> ProcessingScreen(info = flow.info, onCancel = { endSession(TOAST_CANCELLED) })
                is TldhUiState.Result -> ResultScreen(
                    result = flow,
                    markUncertain = markUncertain,
                    tone = tone,
                    onToneChange = { selected ->
                        tone = selected
                        prefs.edit().putString(KEY_TONE, selected.name).apply()
                    },
                    onClose = { endSession(TOAST_SESSION_DELETED) }
                )
                is TldhUiState.Error -> ErrorScreen(
                    title = flow.title,
                    message = flow.message,
                    onOpenFile = { openFile() },
                    onClose = { endSession(null) }
                )
            }
        }
    }
}
