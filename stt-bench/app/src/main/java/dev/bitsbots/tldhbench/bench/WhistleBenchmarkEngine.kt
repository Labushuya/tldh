package dev.bitsbots.tldhbench.bench

import dev.bitsbots.tldhbench.audio.PauseChunkPlanner
import dev.bitsbots.tldhbench.audio.PcmChunk
import dev.bitsbots.tldhbench.audio.PcmWavWriter
import dev.bitsbots.tldhbench.audio.PreparedPcmAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.concurrent.TimeUnit

internal data class WhistleEngineOutput(
    val transcript: String,
    val segments: List<TranscriptSegment>,
    val modelLoadMs: Long,
    val sttMs: Long,
    val totalMs: Long,
    val warnings: List<String> = emptyList()
)

private data class NeedleTranscription(
    val text: String,
    val words: List<TranscriptWord>,
    val ttftMs: Double?,
    val decodeTps: Double?
)

/**
 * Runs Cactus Whistle through the bundled `needle` executable, one process per audio piece.
 *
 * Running the engine as a child process instead of in-process JNI keeps native crashes or hangs out of
 * the app: a failed piece surfaces as an exit code with stderr, and cancelling the benchmark destroys
 * the process. Each call loads the 17 MB model again, so model loading is part of the measured STT time.
 */
internal class WhistleBenchmarkEngine(
    private val runtimeFile: File,
    private val modelFile: File
) {
    suspend fun transcribe(
        pcm: PreparedPcmAudio,
        mode: WhistleModeSpec,
        decodeMs: Long,
        totalStartedAtMs: Long,
        workDir: File
    ): WhistleEngineOutput {
        if (!runtimeFile.isFile || !runtimeFile.canExecute()) {
            throw IllegalStateException("Whistle-Runtime fehlt in diesem Build (${runtimeFile.name}). Nur arm64-v8a-Builds enthalten die needle-Engine.")
        }
        if (!modelFile.isFile) {
            throw IllegalStateException("Whistle-Modell fehlt: ${modelFile.name}")
        }

        val samples = readPcm16(pcm.file)
        val plan = when (mode.chunking) {
            WhistleChunking.APP_PAUSES -> PauseChunkPlanner.plan(samples, pcm.sampleRate, maxChunkMs = mode.maxChunkMs)
            WhistleChunking.ENGINE_WINDOWS -> null
        }
        val chunks = plan?.chunks ?: listOf(PcmChunk(0, samples.size))

        val segments = mutableListOf<TranscriptSegment>()
        val ttfts = mutableListOf<Double>()
        val decodeRates = mutableListOf<Double>()
        val sttStartedAt = System.currentTimeMillis()
        try {
            for ((index, chunk) in chunks.withIndex()) {
                val offsetSec = chunk.startSample.toDouble() / pcm.sampleRate
                val wavFile = writeChunkWav(samples, chunk, pcm.sampleRate, File(workDir, "whistle-chunk-$index.wav"))
                val result = runNeedle(wavFile, mode, chunk.sampleCount * 1000L / pcm.sampleRate, workDir)
                wavFile.delete()
                result.ttftMs?.let { ttfts += it }
                result.decodeTps?.let { decodeRates += it }
                if (result.text.isBlank()) continue
                val words = result.words.map { word ->
                    word.copy(startSec = word.startSec?.plus(offsetSec), endSec = word.endSec?.plus(offsetSec))
                }
                segments += TranscriptSegment(
                    startSec = words.firstOrNull()?.startSec ?: offsetSec,
                    endSec = words.lastOrNull()?.endSec ?: (chunk.endSample.toDouble() / pcm.sampleRate),
                    text = result.text,
                    words = words
                )
            }
        } finally {
            workDir.listFiles { file -> file.name.startsWith("whistle-chunk-") }?.forEach { it.delete() }
        }
        val sttMs = System.currentTimeMillis() - sttStartedAt
        val transcript = segments.joinToString(" ") { it.text }.trim()

        val warnings = buildList {
            add(
                when (mode.chunking) {
                    WhistleChunking.APP_PAUSES -> "Whistle Pausen-Chunking: ${chunks.size} Abschnitt(e) <= ${mode.maxChunkMs / 1000} s, je ein needle-Prozess mit Deutsch-Lock."
                    WhistleChunking.ENGINE_WINDOWS -> "Whistle Engine-Fenster: ein needle-Aufruf, die Engine zerlegt selbst in 30-s-Fenster. Fehlende Sätze an Fenstergrenzen sind hier ein bekanntes Risiko."
                }
            )
            plan?.hardCutCount?.takeIf { it > 0 }?.let {
                add("$it harte Schnitte ohne Sprechpause: an diesen Stellen kann ein Wort zerteilt werden.")
            }
            mode.decoderDepth?.let { add("Decoder-Tiefe $it statt voller Tiefe: schneller, aber mehr Verhörer zu erwarten.") }
            add("Modell-Laden ist in der STT-Zeit enthalten: jeder Abschnitt startet needle neu und lädt das 17-MB-Modell.")
            if (ttfts.isNotEmpty() && decodeRates.isNotEmpty()) {
                add("Whistle-Engine: Ø erstes Token ${formatNumber(ttfts.average())} ms · Ø Decode ${formatNumber(decodeRates.average())} Tokens/s.")
            }
            if (decodeMs > 10_000L) add("Decode dauerte auffällig lange (${formatNumber(decodeMs / 1000.0)} s). Audioformat oder Gerätelast prüfen.")
            if (transcript.isBlank()) add("Whistle hat ein leeres Transkript geliefert. Stille/Rauschen ergibt bei Whistle bewusst leeren Text.")
        }

        return WhistleEngineOutput(
            transcript = transcript,
            segments = segments,
            modelLoadMs = 0L,
            sttMs = sttMs,
            totalMs = System.currentTimeMillis() - totalStartedAtMs,
            warnings = warnings
        )
    }

    private suspend fun runNeedle(wavFile: File, mode: WhistleModeSpec, audioMs: Long, workDir: File): NeedleTranscription {
        val command = buildList {
            add(runtimeFile.absolutePath)
            addAll(listOf("--model", modelFile.absolutePath))
            addAll(listOf("--audio", wavFile.absolutePath))
            addAll(listOf("--audio-language", "de"))
            add("--audio-word-timestamps")
            mode.decoderDepth?.let { addAll(listOf("--audio-depth", it.toString())) }
        }
        val stdoutFile = File(workDir, "whistle-stdout.txt")
        val stderrFile = File(workDir, "whistle-stderr.txt")
        val timeoutMs = maxOf(90_000L, audioMs * 6L + 30_000L)
        val process = ProcessBuilder(command)
            .directory(workDir)
            .redirectOutput(stdoutFile)
            .redirectError(stderrFile)
            .start()
        try {
            val finished = runInterruptible(Dispatchers.IO) { process.waitFor(timeoutMs, TimeUnit.MILLISECONDS) }
            if (!finished) {
                throw IllegalStateException("Whistle-Watchdog: needle hat nach ${timeoutMs / 1000} s nicht geantwortet und wurde beendet.")
            }
            val exitCode = process.exitValue()
            val stdout = stdoutFile.readText()
            if (exitCode != 0) {
                val stderr = stderrFile.takeIf { it.isFile }?.readText().orEmpty().trim().takeLast(400)
                throw IllegalStateException("needle beendet mit Code $exitCode${if (stderr.isNotBlank()) ": $stderr" else ""}")
            }
            return parseNeedleOutput(stdout)
        } finally {
            if (process.isAlive) process.destroyForcibly()
            stdoutFile.delete()
            stderrFile.delete()
        }
    }

    private fun parseNeedleOutput(stdout: String): NeedleTranscription {
        val line = stdout.lineSequence().map { it.trim() }.lastOrNull { it.startsWith("{") }
            ?: throw IllegalStateException("needle hat kein JSON-Ergebnis geliefert: ${stdout.trim().takeLast(200)}")
        val json = JSONObject(line)
        val words = mutableListOf<TranscriptWord>()
        json.optJSONArray("words")?.let { array ->
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val word = item.optString("word", "").trim()
                if (word.isBlank()) continue
                words += TranscriptWord(
                    word = word,
                    startSec = item.optDoubleOrNull("start"),
                    endSec = item.optDoubleOrNull("end"),
                    confidence = item.optDoubleOrNull("probability")
                )
            }
        }
        return NeedleTranscription(
            text = json.optString("text", "").trim(),
            words = words,
            ttftMs = json.optDoubleOrNull("ttft_ms"),
            decodeTps = json.optDoubleOrNull("decode_tps")
        )
    }

    private fun readPcm16(file: File): ShortArray {
        val bytes = file.readBytes()
        val samples = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples)
        return samples
    }

    private fun writeChunkWav(samples: ShortArray, chunk: PcmChunk, sampleRate: Int, wavFile: File): File {
        val pcmFile = File(wavFile.parentFile, wavFile.nameWithoutExtension + ".pcm")
        val buffer = ByteBuffer.allocate(chunk.sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        buffer.asShortBuffer().put(samples, chunk.startSample, chunk.sampleCount)
        pcmFile.writeBytes(buffer.array())
        return try {
            PcmWavWriter.writePcm16MonoWav(pcmFile, wavFile, sampleRate)
        } finally {
            pcmFile.delete()
        }
    }

    private fun JSONObject.optDoubleOrNull(name: String): Double? =
        if (has(name) && !isNull(name)) optDouble(name) else null

    private fun formatNumber(value: Double): String = "%.1f".format(Locale.GERMANY, value)
}
