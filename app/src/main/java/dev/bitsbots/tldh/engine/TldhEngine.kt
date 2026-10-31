package dev.bitsbots.tldh.engine

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import dev.bitsbots.tldh.BuildConfig
import dev.bitsbots.tldh.audio.AudioIngestException
import dev.bitsbots.tldh.audio.AudioIngestor
import dev.bitsbots.tldh.audio.WaveformSampler
import dev.bitsbots.tldh.session.SessionManager
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
import dev.bitsbots.tldh.ui.AudioOrigin
import dev.bitsbots.tldh.ui.ModelDownload
import dev.bitsbots.tldh.ui.ModelUiState
import dev.bitsbots.tldh.ui.PendingAudio
import dev.bitsbots.tldh.ui.ProcessingInfo
import dev.bitsbots.tldh.ui.ProcessingStage
import dev.bitsbots.tldh.ui.TldhUiState
import dev.bitsbots.tldh.ui.UiText
import dev.bitsbots.tldh.ui.UpdateController
import dev.bitsbots.tldh.ui.UpdateUiState
import dev.bitsbots.tldh.updates.UpdateDownloadGuard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** What the background service shows while work is running. */
data class WorkStatus(
    val title: String,
    val text: String,
    /** 0..100, or null while the step has no measurable progress. */
    val progress: Int?,
    val transcribing: Boolean
)

/**
 * Owns everything that must outlive a screen: the voice-note pipeline, model downloads and the
 * app update download. One instance per process, so rotating the phone, switching apps or turning
 * the display off never restarts or loses a running voice note. While work runs, [TldhWorkService]
 * keeps the process in the foreground with a progress notification and a partial wake lock.
 */
class TldhEngine private constructor(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessionManager = SessionManager(filesDir = context.filesDir, cacheDir = context.cacheDir)
    private val modelStore = WhisperModelStore(context)
    private val deviceProfile = DeviceProfiler.read(context)
    private val runtime = WhisperRuntimeLocator.locate(context, deviceProfile.cpu)
    private val recommendedModel = WhisperModelCatalog.recommendedFor(deviceProfile)

    private val _flow = MutableStateFlow<TldhUiState>(TldhUiState.Idle)
    val flow: StateFlow<TldhUiState> = _flow.asStateFlow()

    private val _models = MutableStateFlow(readModels(download = null, error = null))
    val models: StateFlow<ModelUiState> = _models.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    /** One-shot confirmation for the start screen, e.g. after a session was deleted. */
    val toast: StateFlow<String?> = _toast.asStateFlow()

    val updates: UpdateController? = if (BuildConfig.UPDATER_ENABLED) {
        UpdateController(
            context = context,
            scope = scope,
            appVersion = BuildConfig.VERSION_NAME,
            repositorySlug = BuildConfig.GITHUB_REPOSITORY,
            onDownloadStarted = { startWorkService() }
        )
    } else {
        null
    }

    /** Set by the activity: completion notifications are only posted while the user is elsewhere. */
    @Volatile
    var uiVisible: Boolean = false

    private var processingJob: Job? = null
    private var downloadJob: Job? = null
    private var runCounter = 0

    init {
        // A previous process may have been killed mid-session; its audio must not survive.
        sessionManager.wipeOrphanedSessions()
    }

    val work: Flow<WorkStatus?> = combine(
        _flow,
        _models,
        updates?.state ?: flowOf<UpdateUiState>(UpdateUiState.Idle)
    ) { flow, models, update -> workStatus(flow, models, update) }.distinctUntilChanged()

    fun currentWork(): WorkStatus? = workStatus(_flow.value, _models.value, updates?.state?.value ?: UpdateUiState.Idle)

    fun process(pending: PendingAudio) {
        _toast.value = null
        if (runtime == null) {
            _flow.value = TldhUiState.Error(
                "Auf diesem Handy nicht möglich",
                "tl;dh enthält die Spracherkennung nur für 64-Bit-ARM-Handys (arm64-v8a)."
            )
            return
        }
        val model = _models.value.active
        if (model == null) {
            _flow.value = TldhUiState.NeedsModel(pending)
            return
        }
        processingJob?.cancel()
        runCounter += 1
        val token = runCounter
        val title = UiText.title(pending.origin)
        _flow.value = TldhUiState.Processing(ProcessingInfo(token = token, title = title, subtitle = UiText.sourceLabel(pending.origin, null)))
        startWorkService()
        processingJob = scope.launch {
            val outcome = try {
                runPipeline(pending, model, token, title)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: AudioIngestException) {
                sessionManager.wipeCurrentSession()
                TldhUiState.Error("Diese Datei geht leider nicht", error.message ?: "Die Datei konnte nicht gelesen werden.")
            } catch (error: Throwable) {
                sessionManager.wipeCurrentSession()
                TldhUiState.Error("Das hat nicht geklappt", error.message ?: "Die Audiodatei konnte nicht verarbeitet werden.")
            }
            val current = _flow.value
            if (current is TldhUiState.Processing && current.info.token == token) {
                _flow.value = outcome
                if (!uiVisible) WorkNotifications.notifyFinished(context, success = outcome is TldhUiState.Result)
            }
        }
    }

    private suspend fun runPipeline(pending: PendingAudio, model: WhisperModelSpec, token: Int, title: String): TldhUiState.Result {
        val whisper = requireNotNull(runtime)
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
        updateProcessing(token) { it.copy(stage = ProcessingStage.Transcribe, prepareMs = prepareMs, levels = levels, progress = 0) }

        val transcribeStarted = SystemClock.elapsedRealtime()
        val run = WhisperCliTranscriber(whisper).transcribe(
            pcmFile = prepared.file,
            sampleRate = prepared.sampleRate,
            model = modelStore.modelFile(model),
            workDir = workDir,
            audioDurationMs = metadata.durationMs,
            onSegment = { segment -> updateProcessing(token) { it.copy(liveText = (it.liveText + " " + segment).trim()) } }
        ) { progress ->
            val elapsed = SystemClock.elapsedRealtime() - transcribeStarted
            updateProcessing(token) { it.copy(progress = progress, remainingSeconds = UiText.estimateRemainingSeconds(progress, elapsed)) }
        }
        updateProcessing(token) { it.copy(stage = ProcessingStage.Summarize, progress = 100) }

        val outcome = TranscriptionOutcome(
            status = if (run.transcript.isBlank()) TranscriptionStatus.FAILED else TranscriptionStatus.COMPLETED,
            transcript = run.transcript.takeIf { it.isNotBlank() },
            message = if (run.transcript.isBlank()) "Whisper hat in dieser Audio keine Sprache erkannt." else "Lokal transkribiert.",
            elapsedMs = run.elapsedMs,
            modelName = "${model.displayName} (${model.qualityLabel}) · ${whisper.variant}",
            words = run.words,
            pcmFile = prepared.file,
            sampleRate = prepared.sampleRate
        )
        val source = when (pending.origin) {
            AudioOrigin.SHARE -> "Teilen (ACTION_SEND)"
            AudioOrigin.FILE -> "Audiodatei öffnen (ACTION_OPEN_DOCUMENT)"
        }
        val summary = withContext(Dispatchers.Default) { RuleBasedSummaryEngine().summarize(metadata, outcome, source) }
        return TldhUiState.Result(
            summary = summary,
            transcription = outcome,
            title = title,
            subtitle = listOfNotNull(subtitle, UiText.transcribedIn(run.elapsedMs)).joinToString(" · ")
        )
    }

    /** Thread-safe: whisper reports progress and segments from its reader threads. */
    private fun updateProcessing(token: Int, change: (ProcessingInfo) -> ProcessingInfo) {
        _flow.update { current ->
            if (current is TldhUiState.Processing && current.info.token == token) TldhUiState.Processing(change(current.info)) else current
        }
    }

    /** Ends the current voice note: stops work, deletes audio and transcript, back to start. */
    fun endSession(message: String?) {
        processingJob?.cancel()
        processingJob = null
        sessionManager.wipeCurrentSession()
        _flow.value = TldhUiState.Idle
        _toast.value = message
    }

    /** The activity is finishing for good: a shown result is private and goes; running work continues. */
    fun onUiFinished() {
        when (_flow.value) {
            is TldhUiState.Result, is TldhUiState.Error -> endSession(null)
            else -> Unit
        }
    }

    /** "Abbrechen" in the notification. */
    fun cancelWork() {
        if (_flow.value is TldhUiState.Processing) endSession(TOAST_CANCELLED)
        downloadJob?.cancel()
        updates?.cancelDownload()
    }

    fun consumeToast(message: String) {
        _toast.compareAndSet(message, null)
    }

    fun activateModel(spec: WhisperModelSpec) {
        modelStore.setActiveModel(spec)
        _models.value = readModels(download = _models.value.download, error = null)
    }

    fun useInstalledModel(spec: WhisperModelSpec) {
        activateModel(spec)
        continueAfterSetup()
    }

    fun deleteModels() {
        if (_models.value.download != null) return
        WhisperModelCatalog.models.forEach { modelStore.delete(it) }
        _models.value = readModels(download = null, error = null)
    }

    fun downloadModel(spec: WhisperModelSpec) {
        if (_models.value.download != null) return
        _models.update { it.copy(download = ModelDownload(spec, 0f), error = null) }
        startWorkService()
        downloadJob = scope.launch {
            try {
                UpdateDownloadGuard(context).runGuardedDownload {
                    modelStore.download(spec) { progress ->
                        _models.update { models -> if (models.download?.spec == spec) models.copy(download = ModelDownload(spec, progress)) else models }
                    }
                }
                modelStore.setActiveModel(spec)
                _models.value = readModels(download = null, error = null)
                continueAfterSetup()
            } catch (cancelled: CancellationException) {
                _models.value = readModels(download = null, error = null)
                throw cancelled
            } catch (error: Throwable) {
                _models.value = readModels(download = null, error = error.message ?: "Modell-Download fehlgeschlagen.")
            }
        }
    }

    fun closeSetup() {
        val current = _flow.value
        if (current is TldhUiState.NeedsModel) endSession(if (current.pending != null) TOAST_CANCELLED else null)
    }

    /** After setup: continue with the waiting voice note, or report readiness on the start screen. */
    private fun continueAfterSetup() {
        val current = _flow.value
        if (current is TldhUiState.NeedsModel) {
            val pending = current.pending
            if (pending != null) {
                process(pending)
            } else {
                _flow.value = TldhUiState.Idle
                _toast.value = "Erkennung ist bereit."
            }
        }
    }

    private fun readModels(download: ModelDownload?, error: String?) = ModelUiState(
        runtimeAvailable = runtime != null,
        recommended = recommendedModel,
        installed = modelStore.installedModels(),
        active = modelStore.activeModel(),
        download = download,
        error = error
    )

    private fun startWorkService() {
        // A plain start (not startForegroundService): the service then promotes itself and, if
        // Android refuses that, keeps running without crashing. startForegroundService would
        // require startForeground() to succeed, or Android kills the app. Called right after a user
        // action, while tl;dh is in the foreground, so starting the service is allowed.
        runCatching { context.startService(Intent(context, TldhWorkService::class.java)) }
    }

    private fun workStatus(flow: TldhUiState, models: ModelUiState, update: UpdateUiState): WorkStatus? {
        if (flow is TldhUiState.Processing) {
            val info = flow.info
            return when (info.stage) {
                ProcessingStage.Prepare -> WorkStatus("Sprachnachricht wird zusammengefasst", "Audio wird vorbereitet", null, true)
                ProcessingStage.Transcribe -> WorkStatus(
                    "Sprachnachricht wird zusammengefasst",
                    "Transkribieren · ${info.progress} %" + (info.remainingSeconds?.let { " · ${UiText.remainingText(it).removeSuffix(".")}" } ?: ""),
                    info.progress,
                    true
                )
                ProcessingStage.Summarize -> WorkStatus("Sprachnachricht wird zusammengefasst", "Gleich fertig", null, true)
            }
        }
        models.download?.let { download ->
            val percent = (download.progress * 100).toInt()
            return WorkStatus("Erkennungsmodell wird geladen", "${download.spec.qualityLabel} · $percent % von ${download.spec.sizeLabel}", percent, false)
        }
        if (update is UpdateUiState.Downloading) {
            val percent = (update.progress * 100).toInt()
            return WorkStatus("Update wird geladen", "Version ${update.update.version} · $percent %", percent, false)
        }
        return null
    }

    companion object {
        const val TOAST_SESSION_DELETED = "Session gelöscht. Audio und Transkript sind weg."
        const val TOAST_CANCELLED = "Abgebrochen. Nichts wurde gespeichert."
        private const val WAVEFORM_BARS = 72

        @Volatile
        private var instance: TldhEngine? = null

        fun get(context: Context): TldhEngine =
            instance ?: synchronized(this) {
                instance ?: TldhEngine(context.applicationContext).also { instance = it }
            }
    }
}
