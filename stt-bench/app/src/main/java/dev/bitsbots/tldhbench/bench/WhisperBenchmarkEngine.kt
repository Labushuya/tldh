package dev.bitsbots.tldhbench.bench

import android.content.Context
import dev.bitsbots.tldhbench.audio.PcmWavWriter
import dev.bitsbots.tldhbench.audio.PreparedPcmAudio
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import dev.ffmpegkit.whisper.WhisperModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * v0.3.8 adds a defensive runner guard because the Android whisper.cpp wrapper can leave
 * native state unstable after failed/heavy runs. We avoid concurrent native Whisper calls,
 * use a fresh WAV path per run, always release the native model, remove temp files, and
 * surface actionable failure hints instead of leaving the UI in a permanently busy state.
 */
internal class WhisperBenchmarkEngine(private val context: Context) {
    suspend fun transcribe(
        modelFile: File,
        pcm: PreparedPcmAudio,
        decodeMs: Long,
        totalStartedAtMs: Long,
        workDir: File
    ): WhisperEngineOutput = whisperMutex.withLock {
        if (!modelFile.isFile) {
            throw IllegalStateException("Whisper-Modell fehlt: ${modelFile.name}")
        }
        val wavFile = PcmWavWriter.writePcm16MonoWav(
            pcmFile = pcm.file,
            wavFile = File(workDir, "tldh-whisper-input-${System.currentTimeMillis()}.wav"),
            sampleRate = pcm.sampleRate
        )

        var model: WhisperModel? = null
        var transcript = ""
        var segments = emptyList<TranscriptSegment>()
        var modelLoadMs = 0L
        val sttMs: Long
        try {
            sttMs = try {
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
            } catch (oom: OutOfMemoryError) {
                throw IllegalStateException(
                    "whisper.cpp hat nicht genug Speicher für ${modelFile.name}. App neu starten und zuerst tiny/base testen; small nur einzeln ohne Batch.",
                    oom
                )
            } catch (t: Throwable) {
                throw IllegalStateException(
                    "whisper.cpp Lauf fehlgeschlagen (${t.message ?: t::class.java.simpleName}). Falls Folge-Läufe hängen: App über den Task-Switcher schließen und neu öffnen; v0.3.8 bereinigt Temp-Dateien und Native-Modelle pro Lauf.",
                    t
                )
            }
        } finally {
            model?.let { runCatching { Whisper.releaseModel(it) } }
            runCatching { wavFile.delete() }
            System.gc()
            System.runFinalization()
        }

        val warnings = buildList {
            add("whisper.cpp Deutsch-Lock aktiv: dieser Lauf nutzt WhisperConfig(language = \"de\") und darf gegen Vosk verglichen werden.")
            add("whisper.cpp Runner-Guard aktiv: einzelne native Whisper-Läufe werden seriell ausgeführt, Temp-WAVs nach jedem Lauf entfernt und Modelle explizit freigegeben.")
            add("Whisper liefert hier Segment-Zeitstempel, aber keine Wort-Confidence. WER/CER/S/I/D funktionieren mit Referenztext trotzdem.")
            if (decodeMs > 10_000L) add("Decode dauerte auffällig lange (${formatSecondsLocal(decodeMs)}). Audioformat oder Gerätelast prüfen.")
        }

        WhisperEngineOutput(
            transcript = transcript,
            segments = segments,
            modelLoadMs = modelLoadMs,
            sttMs = sttMs,
            totalMs = System.currentTimeMillis() - totalStartedAtMs,
            warnings = warnings
        )
    }

    companion object {
        private val whisperMutex = Mutex()
    }
}

private fun formatSecondsLocal(ms: Long): String = "%.2f s".format(java.util.Locale.GERMANY, ms / 1000.0)
