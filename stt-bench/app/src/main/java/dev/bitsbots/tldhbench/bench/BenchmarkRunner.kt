package dev.bitsbots.tldhbench.bench

import android.content.Context
import dev.bitsbots.tldhbench.audio.AudioIngestor
import dev.bitsbots.tldhbench.audio.PcmAudioPreparer
import dev.bitsbots.tldhbench.audio.PreparedPcmAudio
import dev.bitsbots.tldhbench.models.VoskModelManager
import dev.bitsbots.tldhbench.models.WhisperModelManager
import dev.bitsbots.tldhbench.share.SharedAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

class BenchmarkRunner(private val context: Context) {
    private val voskModelManager = VoskModelManager(context)
    private val whisperModelManager = WhisperModelManager(context)

    suspend fun runVosk(
        sharedAudio: SharedAudio,
        modelSpec: VoskModelSpec,
        referenceText: String? = null
    ): BenchmarkResult = withContext(Dispatchers.Default) {
        if (!voskModelManager.isInstalled(modelSpec)) {
            throw IllegalStateException("${modelSpec.displayName} ist nicht installiert. Erst Modell herunterladen/installieren.")
        }
        if (modelSpec.deviceSignal == Signal.RED) {
            throw IllegalStateException("${modelSpec.displayName} ist auf Android absichtlich blockiert: dieses Modell kann beim nativen Vosk-Laden die App hart beenden. Für diesen Extremtest bitte später Tower/LAN-Quality-Mode nutzen.")
        }
        val started = System.currentTimeMillis()
        val metadata = AudioIngestor(context).inspect(sharedAudio)
        val workDir = File(context.cacheDir, "bench-work").apply { deleteRecursively(); mkdirs() }
        lateinit var prepared: PreparedPcmAudio
        val decodeMs = measureTimeMillis {
            prepared = PcmAudioPreparer(context, workDir).prepare(sharedAudio.uri, metadata.durationMs)
        }
        val engineOutput = VoskBenchmarkEngine().transcribe(
            modelDir = voskModelManager.modelDir(modelSpec),
            pcm = prepared,
            decodeMs = decodeMs,
            totalStartedAtMs = started
        )
        val timing = BenchmarkTiming(
            decodeMs = decodeMs,
            modelLoadMs = engineOutput.modelLoadMs,
            sttMs = engineOutput.sttMs,
            totalMs = engineOutput.totalMs,
            audioDurationMs = metadata.durationMs
        )
        val modelWarnings = listOfNotNull(
            if (modelSpec.deviceSignal == Signal.RED) "${modelSpec.displayName} ist laut Ampel eher kein Handy-Modell. Benchmark kann RAM/Download/Zeit stark belasten." else null,
            if (modelSpec.speedSignal == Signal.RED) "${modelSpec.displayName} priorisiert Qualität gegenüber Geschwindigkeit. RTF kritisch prüfen." else null
        )
        val referenceComparison = ReferenceTextComparator.compare(referenceText, engineOutput.transcript)
        val comparisonWarnings = comparisonWarnings(referenceComparison)
        val preprocessingWarnings = preprocessingWarnings(prepared, "Vosk")

        BenchmarkResult(
            engine = "Vosk",
            model = modelSpec.displayName,
            modelId = modelSpec.id,
            language = "de-DE",
            metadata = metadata,
            timing = timing,
            transcript = engineOutput.transcript,
            segments = engineOutput.segments,
            verdict = BenchmarkTargets.verdict(metadata.durationMs, timing.totalMs),
            warnings = metadata.validation.warnings + modelWarnings + engineOutput.warnings + preprocessingWarnings + comparisonWarnings,
            referenceComparison = referenceComparison
        )
    }

    suspend fun runWhisper(
        sharedAudio: SharedAudio,
        modelSpec: WhisperModelSpec,
        referenceText: String? = null
    ): BenchmarkResult = withContext(Dispatchers.Default) {
        if (!whisperModelManager.isInstalled(modelSpec)) {
            throw IllegalStateException("${modelSpec.displayName} ist nicht installiert. Erst Whisper-Modell herunterladen.")
        }
        val started = System.currentTimeMillis()
        val metadata = AudioIngestor(context).inspect(sharedAudio)
        val workDir = File(context.cacheDir, "bench-work-whisper").apply { deleteRecursively(); mkdirs() }
        lateinit var prepared: PreparedPcmAudio
        val decodeMs = measureTimeMillis {
            prepared = PcmAudioPreparer(context, workDir).prepare(sharedAudio.uri, metadata.durationMs)
        }
        val engineOutput = WhisperBenchmarkEngine(context).transcribe(
            modelFile = whisperModelManager.modelFile(modelSpec),
            pcm = prepared,
            decodeMs = decodeMs,
            totalStartedAtMs = started,
            workDir = workDir
        )
        val timing = BenchmarkTiming(
            decodeMs = decodeMs,
            modelLoadMs = engineOutput.modelLoadMs,
            sttMs = engineOutput.sttMs,
            totalMs = engineOutput.totalMs,
            audioDurationMs = metadata.durationMs
        )
        val modelWarnings = listOfNotNull(
            if (modelSpec.speedSignal == Signal.RED) "${modelSpec.displayName} ist ein Qualitätskandidat. Laufzeit, Akku und RTF kritisch prüfen." else null,
            if (modelSpec.phoneSignal == Signal.YELLOW) "${modelSpec.displayName} kann auf dem Handy spürbar RAM/Akku belasten. Bei Hängern zuerst tiny/base vergleichen." else null
        )
        val referenceComparison = ReferenceTextComparator.compare(referenceText, engineOutput.transcript)
        val comparisonWarnings = comparisonWarnings(referenceComparison)
        val preprocessingWarnings = preprocessingWarnings(prepared, "whisper.cpp")

        BenchmarkResult(
            engine = "whisper.cpp",
            model = modelSpec.displayName,
            modelId = modelSpec.id,
            language = "de-DE",
            metadata = metadata,
            timing = timing,
            transcript = engineOutput.transcript,
            segments = engineOutput.segments,
            verdict = BenchmarkTargets.verdict(metadata.durationMs, timing.totalMs),
            warnings = metadata.validation.warnings + modelWarnings + engineOutput.warnings + preprocessingWarnings + comparisonWarnings,
            referenceComparison = referenceComparison
        )
    }

    private fun comparisonWarnings(referenceComparison: ReferenceComparison?): List<String> = listOfNotNull(
        referenceComparison?.takeIf { it.werPercent > 40.0 }?.let {
            "Referenzvergleich schwach: ${it.summary}. Dieses Modell sollte für produktive TL;DRs nicht ungeprüft verwendet werden."
        },
        referenceComparison?.takeIf { it.wordDeletions > 0 }?.let {
            "Referenzvergleich: ${it.wordDeletions} Referenz-Wörter fehlen in der Erkennung. Fehlende Negationen/Zahlen besonders prüfen."
        }
    )

    private fun preprocessingWarnings(prepared: PreparedPcmAudio, engineLabel: String): List<String> = listOfNotNull(
        prepared.preprocessing.takeIf { it.enabled && it.removedMs > 0L }?.let {
            "Nicht-Sprache-Reduktion aktiv: ca. ${formatSeconds(it.removedMs)} (${formatPercent(it.removedPercent)}) leise/pausierte PCM-Anteile vor $engineLabel entfernt. Timing-Verdict bleibt auf Originaldauer bezogen."
        }
    )
}

private fun formatSeconds(ms: Long): String = "%.2f s".format(java.util.Locale.GERMANY, ms / 1000.0)
private fun formatPercent(value: Double): String = "%.1f%%".format(java.util.Locale.GERMANY, value)
