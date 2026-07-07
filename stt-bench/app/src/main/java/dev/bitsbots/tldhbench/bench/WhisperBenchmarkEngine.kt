package dev.bitsbots.tldhbench.bench

import dev.bitsbots.tldhbench.audio.PcmWavWriter
import dev.bitsbots.tldhbench.audio.PreparedPcmAudio
import mx.valdora.whisper.WhisperContext
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
 * First executable whisper.cpp runner for the benchmark app.
 *
 * It intentionally reuses the same app-side decode/preprocessing path as Vosk:
 * Android media input -> 16 kHz mono PCM -> WAV container -> whisper.cpp wrapper.
 * That keeps WER/CER comparisons fair and makes Vosk vs. Whisper reports comparable.
 */
internal class WhisperBenchmarkEngine {
    fun transcribe(
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

        var context: WhisperContext? = null
        var transcript = ""
        var modelLoadMs = 0L
        val sttMs = try {
            modelLoadMs = measureTimeMillis {
                context = WhisperContext(modelFile.absolutePath)
            }
            measureTimeMillis {
                transcript = context?.transcribe(wavFile).orEmpty().trim()
            }
        } finally {
            runCatching { context?.close() }
        }

        val warnings = buildList {
            add("whisper.cpp Erstintegration: Transkription läuft lokal/offline über den Android whisper.cpp Wrapper. Bitte tiny/base/small gegen dieselbe Real-Audio vergleichen, bevor tl;dh daraus Produktentscheidungen ableitet.")
            add("Dieser Whisper-Pfad liefert aktuell nur Gesamttranskript ohne Wort-/Segment-Zeitstempel. WER/CER/S/I/D funktionieren mit Referenztext trotzdem.")
            if (decodeMs > 10_000L) add("Decode dauerte auffällig lange (${formatSecondsLocal(decodeMs)}). Audioformat oder Gerätelast prüfen.")
        }

        return WhisperEngineOutput(
            transcript = transcript,
            segments = emptyList(),
            modelLoadMs = modelLoadMs,
            sttMs = sttMs,
            totalMs = System.currentTimeMillis() - totalStartedAtMs,
            warnings = warnings
        )
    }
}

private fun formatSecondsLocal(ms: Long): String = "%.2f s".format(java.util.Locale.GERMANY, ms / 1000.0)
