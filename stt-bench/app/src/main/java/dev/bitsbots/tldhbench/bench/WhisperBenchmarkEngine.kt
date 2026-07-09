package dev.bitsbots.tldhbench.bench

import android.content.Context
import dev.bitsbots.tldhbench.audio.PcmWavWriter
import dev.bitsbots.tldhbench.audio.PreparedPcmAudio
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import dev.ffmpegkit.whisper.WhisperModel
import java.io.File
import kotlin.system.measureTimeMillis

internal data class WhisperEngineOutput(
    val transcript: String,
    val segments: List<TranscriptSegment>,
    val modelLoadMs: Long,
    val sttMs: Long,
    val totalMs: Long,
    val warnings: List<String> = emptyList()
)

/**
 * Executable whisper.cpp runner with explicit German transcription lock.
 *
 * v0.3.4-v0.3.6 used a minimal wrapper whose public API exposed only transcribe(audioFile)
 * and could not force the recognition language. On German WhatsApp audio this made results
 * look like Whisper quality was terrible, while native logs showed non-German decoding.
 *
 * v0.3.7 switches to an Android whisper.cpp wrapper that exposes WhisperConfig(language = "de").
 * The app still uses the same app-side decode/preprocessing path as Vosk:
 * Android media input -> 16 kHz mono PCM -> WAV container -> whisper.cpp.
 */
internal class WhisperBenchmarkEngine(private val context: Context) {
    suspend fun transcribe(
        modelFile: File,
        pcm: PreparedPcmAudio,
        decodeMs: Long,
        totalStartedAtMs: Long,
        workDir: File
    ): WhisperEngineOutput {
        if (!modelFile.isFile) {
            throw IllegalStateException("Whisper-Modell fehlt: ${modelFile.name}")
        }
        val wavFile = PcmWavWriter.writePcm16MonoWav(
            pcmFile = pcm.file,
            wavFile = File(workDir, "tldh-whisper-input-16k-mono.wav"),
            sampleRate = pcm.sampleRate
        )

        var model: WhisperModel? = null
        var transcript = ""
        var segments = emptyList<TranscriptSegment>()
        var modelLoadMs = 0L
        val sttMs = try {
            modelLoadMs = measureTimeMillis {
                model = Whisper.loadModel(context, modelFile.absolutePath)
            }
            measureTimeMillis {
                val result = Whisper.transcribe(
                    model ?: error("Whisper-Modell wurde nicht geladen."),
                    wavFile.absolutePath,
                    WhisperConfig(language = "de")
                )
                transcript = result.text.trim()
                segments = result.segments.mapNotNull { segment ->
                    val text = segment.text.trim()
                    if (text.isBlank()) {
                        null
                    } else {
                        TranscriptSegment(
                            startSec = segment.startMs / 1000.0,
                            endSec = segment.endMs / 1000.0,
                            text = text,
                            words = emptyList()
                        )
                    }
                }
            }
        } finally {
            model?.let { runCatching { Whisper.releaseModel(it) } }
            runCatching { wavFile.delete() }
            // Native whisper.cpp wrappers can keep memory outside the JVM heap for a short time.
            // This does not guarantee recovery from a native crash, but helps after large/small-model runs.
            System.gc()
        }

        val warnings = buildList {
            add("whisper.cpp Deutsch-Lock aktiv: dieser Lauf nutzt WhisperConfig(language = \"de\") und darf wieder gegen Vosk verglichen werden.")
            add("Der alte Whisper-Wrapper aus v0.3.4-v0.3.6 ist entfernt, weil er keine Sprachvorgabe exponiert hat und die nativen Logs auf nicht-deutsche Transkription hindeuteten.")
            add("Whisper liefert hier Segment-Zeitstempel, aber keine Wort-Confidence. WER/CER/S/I/D funktionieren mit Referenztext trotzdem.")
            add("Whisper v0.3.9 Reliability: native Läufe bleiben serialisiert; die UI zeigt Benchmark-Läufe bewusst indeterminate mit Abbrechen-Aktion statt irreführender 0%-Progressanzeige.")
            if (decodeMs > 10_000L) add("Decode dauerte auffällig lange (${formatSecondsLocal(decodeMs)}). Audioformat oder Gerätelast prüfen.")
            if (transcript.isBlank()) add("Whisper hat ein leeres Transkript geliefert. Modell/Audio erneut testen und bei Wiederholung tiny/base gegenprüfen.")
        }

        return WhisperEngineOutput(
            transcript = transcript,
            segments = segments,
            modelLoadMs = modelLoadMs,
            sttMs = sttMs,
            totalMs = System.currentTimeMillis() - totalStartedAtMs,
            warnings = warnings
        )
    }
}

private fun formatSecondsLocal(ms: Long): String = "%.2f s".format(java.util.Locale.GERMANY, ms / 1000.0)
