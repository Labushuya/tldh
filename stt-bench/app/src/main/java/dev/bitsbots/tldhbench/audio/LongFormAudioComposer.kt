package dev.bitsbots.tldhbench.audio

import android.content.Context
import android.net.Uri
import dev.bitsbots.tldhbench.corpus.ReferenceCorpusManager
import dev.bitsbots.tldhbench.corpus.ReferenceSample
import dev.bitsbots.tldhbench.share.AudioSourceKind
import dev.bitsbots.tldhbench.share.SharedAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

private const val LONGFORM_SAMPLE_RATE = 16_000
private const val LONGFORM_CHANNELS = 1
private const val LONGFORM_BITS_PER_SAMPLE = 16
private const val BYTES_PER_SAMPLE = 2
private const val BYTES_PER_MS = LONGFORM_SAMPLE_RATE * BYTES_PER_SAMPLE / 1000
private const val DEFAULT_PAUSE_MS = 350L

data class LongFormProfile(
    val id: String,
    val title: String,
    val targetDurationMs: Long,
    val description: String
)

object LongFormProfiles {
    val profiles: List<LongFormProfile> = listOf(
        LongFormProfile(
            id = "longform-30s",
            title = "30 Sekunden",
            targetDurationMs = 30_000L,
            description = "Kurze WhatsApp-nahe Longform-Probe mit mehreren echten Referenzsätzen."
        ),
        LongFormProfile(
            id = "longform-90s",
            title = "90 Sekunden",
            targetDurationMs = 90_000L,
            description = "Realistischere Nachrichtenlänge für Speed, WER und CER unter zusammenhängender Last."
        ),
        LongFormProfile(
            id = "longform-4min",
            title = "4 Minuten",
            targetDurationMs = 4L * 60L * 1000L,
            description = "Stressprofil für lange Sprachnachrichten; bleibt bewusst unter dem 15-Minuten-Hard-Limit."
        )
    )
}

data class ComposedLongFormAudio(
    val profile: LongFormProfile,
    val file: File,
    val sharedAudio: SharedAudio,
    val referenceText: String,
    val usedSampleCount: Int,
    val actualDurationMs: Long
) {
    val displayName: String = "${profile.title} · $usedSampleCount Sätze · ${formatDuration(actualDurationMs)}"
}

class LongFormAudioComposer(
    private val context: Context,
    private val corpusManager: ReferenceCorpusManager
) {
    suspend fun compose(
        profile: LongFormProfile,
        samples: List<ReferenceSample>,
        pauseMs: Long = DEFAULT_PAUSE_MS
    ): ComposedLongFormAudio = withContext(Dispatchers.IO) {
        val available = samples.filter { corpusManager.isInstalled(it) }
        if (available.isEmpty()) {
            throw IllegalStateException("Keine Goldstandard-Audios installiert. Bitte zuerst den Goldstandard-Korpus laden.")
        }

        val root = File(context.filesDir, "generated-longform").apply { mkdirs() }
        val workDir = File(context.cacheDir, "longform-compose-${profile.id}").apply {
            deleteRecursively()
            mkdirs()
        }
        val combinedPcm = File(workDir, "combined.pcm")
        val outputWav = File(root, "${profile.id}.wav")
        val used = mutableListOf<ReferenceSample>()
        var currentDurationMs = 0L
        var index = 0
        val maxIterations = max(available.size * 12, 64)

        FileOutputStream(combinedPcm).use { output ->
            while (currentDurationMs < profile.targetDurationMs && index < maxIterations) {
                val sample = available[index % available.size]
                val decodeDir = File(workDir, "decode-$index").apply { mkdirs() }
                val prepared = PcmAudioPreparer(context, decodeDir).prepare(
                    uri = Uri.fromFile(corpusManager.sampleFile(sample)),
                    durationMs = null
                )
                val pcmBytes = prepared.file.readBytes()
                if (pcmBytes.isNotEmpty()) {
                    output.write(pcmBytes)
                    currentDurationMs += pcmBytes.size / BYTES_PER_MS
                    used += sample
                }
                if (currentDurationMs < profile.targetDurationMs) {
                    output.writeSilence(pauseMs)
                    currentDurationMs += pauseMs
                }
                index += 1
            }
        }

        if (used.isEmpty() || combinedPcm.length() <= 0L) {
            throw IllegalStateException("Longform-Audio konnte nicht erzeugt werden.")
        }
        writeWav(combinedPcm, outputWav)
        workDir.deleteRecursively()

        val actualMs = combinedPcm.length() / BYTES_PER_MS
        ComposedLongFormAudio(
            profile = profile,
            file = outputWav,
            sharedAudio = SharedAudio(
                uri = Uri.fromFile(outputWav),
                mimeType = "audio/wav",
                sourceKind = AudioSourceKind.GENERATED_LONGFORM,
                displayName = "${profile.title} Longform WAV"
            ),
            referenceText = used.joinToString(separator = " ") { it.referenceText.trim() },
            usedSampleCount = used.size,
            actualDurationMs = actualMs
        )
    }

    private fun FileOutputStream.writeSilence(durationMs: Long) {
        val bytes = ByteArray((durationMs * BYTES_PER_MS).toInt().coerceAtLeast(0))
        write(bytes)
    }

    private fun writeWav(pcmFile: File, wavFile: File) {
        val dataSize = pcmFile.length()
        FileOutputStream(wavFile).use { out ->
            out.writeAscii("RIFF")
            out.writeIntLE((36L + dataSize).toInt())
            out.writeAscii("WAVE")
            out.writeAscii("fmt ")
            out.writeIntLE(16)
            out.writeShortLE(1) // PCM
            out.writeShortLE(LONGFORM_CHANNELS)
            out.writeIntLE(LONGFORM_SAMPLE_RATE)
            out.writeIntLE(LONGFORM_SAMPLE_RATE * LONGFORM_CHANNELS * BYTES_PER_SAMPLE)
            out.writeShortLE(LONGFORM_CHANNELS * BYTES_PER_SAMPLE)
            out.writeShortLE(LONGFORM_BITS_PER_SAMPLE)
            out.writeAscii("data")
            out.writeIntLE(dataSize.toInt())
            pcmFile.inputStream().use { input -> input.copyTo(out) }
        }
    }

    private fun FileOutputStream.writeAscii(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
    }

    private fun FileOutputStream.writeIntLE(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
        write((value ushr 16) and 0xFF)
        write((value ushr 24) and 0xFF)
    }

    private fun FileOutputStream.writeShortLE(value: Int) {
        write(value and 0xFF)
        write((value ushr 8) and 0xFF)
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0) "%d:%02d min".format(minutes, seconds) else "%d s".format(seconds)
}
