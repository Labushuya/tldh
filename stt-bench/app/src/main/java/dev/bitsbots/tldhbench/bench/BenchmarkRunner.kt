package dev.bitsbots.tldhbench.bench

import android.content.Context
import dev.bitsbots.tldhbench.audio.AudioIngestor
import dev.bitsbots.tldhbench.audio.PcmAudioPreparer
import dev.bitsbots.tldhbench.models.VoskModelManager
import dev.bitsbots.tldhbench.share.SharedAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

class BenchmarkRunner(private val context: Context) {
    private val modelManager = VoskModelManager(context)

    suspend fun runVoskSmallDe(sharedAudio: SharedAudio): BenchmarkResult = withContext(Dispatchers.Default) {
        if (!modelManager.isInstalled()) {
            throw IllegalStateException("Vosk small German ist nicht installiert. Erst Modell herunterladen/installieren.")
        }
        val started = System.currentTimeMillis()
        val metadata = AudioIngestor(context).inspect(sharedAudio)
        val workDir = File(context.cacheDir, "bench-work").apply { deleteRecursively(); mkdirs() }
        var prepared: dev.bitsbots.tldhbench.audio.PreparedPcmAudio
        val decodeMs = measureTimeMillis {
            prepared = PcmAudioPreparer(context, workDir).prepare(sharedAudio.uri, metadata.durationMs)
        }
        val engineOutput = VoskBenchmarkEngine().transcribe(
            modelDir = modelManager.expectedModelDir,
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
        BenchmarkResult(
            engine = "Vosk",
            model = "vosk-model-small-de-0.15",
            language = "de-DE",
            metadata = metadata,
            timing = timing,
            transcript = engineOutput.transcript,
            segments = engineOutput.segments,
            verdict = BenchmarkTargets.verdict(metadata.durationMs, timing.totalMs),
            warnings = metadata.validation.warnings + engineOutput.warnings
        )
    }
}
