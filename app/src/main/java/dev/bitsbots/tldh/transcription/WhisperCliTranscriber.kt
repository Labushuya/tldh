package dev.bitsbots.tldh.transcription

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** The whisper-cli build bundled for this CPU, extracted by Android into the native library dir. */
data class WhisperRuntime(val executable: File, val variant: String)

object WhisperRuntimeLocator {
    fun locate(context: Context, cpu: CpuFeatures): WhisperRuntime? {
        val libDir = File(context.applicationInfo.nativeLibraryDir)
        val dotProd = File(libDir, "libwhisper_cli_dotprod.so")
        val baseline = File(libDir, "libwhisper_cli.so")
        return when {
            cpu.supportsDotProdBuild && dotProd.canExecute() -> WhisperRuntime(dotProd, "armv8.2-a+dotprod+fp16")
            baseline.canExecute() -> WhisperRuntime(baseline, "armv8-a")
            else -> null
        }
    }
}

data class WhisperRunResult(
    val transcript: String,
    val segments: List<WhisperSegment>,
    val words: List<TranscriptWord>,
    val elapsedMs: Long
)

/**
 * Runs whisper-cli as a child process per audio. A native crash or hang only ends this run, and
 * cancelling the coroutine destroys the process. The full JSON output carries per-token
 * probabilities and timestamps; a plain-text output is written alongside as a fallback.
 */
class WhisperCliTranscriber(private val runtime: WhisperRuntime) {
    suspend fun transcribe(
        pcmFile: File,
        sampleRate: Int,
        model: File,
        workDir: File,
        audioDurationMs: Long?,
        threads: Int = 4,
        onSegment: (String) -> Unit = {},
        onProgress: (Int) -> Unit
    ): WhisperRunResult = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        val wav = WavWriter.writePcm16Mono(pcmFile, File(workDir, "tldh-whisper-input.wav"), sampleRate)
        val outputBase = File(workDir, "tldh-whisper-output")
        val command = listOf(
            runtime.executable.absolutePath,
            "-m", model.absolutePath,
            "-f", wav.absolutePath,
            "-l", "de",
            "-t", threads.toString(),
            "-bs", WhisperDecoding.BEAM_SIZE.toString(),
            "-bo", WhisperDecoding.BEST_OF.toString(),
            "-ojf",
            "-otxt",
            "-of", outputBase.absolutePath,
            "-pp"
        )
        val process = ProcessBuilder(command)
            .directory(workDir)
            .start()
        // whisper-cli prints each finished segment to stdout (flushed), so the UI can show the
        // first recognized sentences while the rest is still being transcribed.
        val stdoutReader = thread(name = "whisper-cli-stdout", isDaemon = true) {
            runCatching {
                process.inputStream.bufferedReader().forEachLine { line ->
                    WhisperStdout.segmentText(line)?.let(onSegment)
                }
            }
        }
        val stderrTail = ArrayDeque<String>()
        val progressPattern = Regex("""progress\s*=\s*(\d+)%""")
        val stderrReader = thread(name = "whisper-cli-stderr", isDaemon = true) {
            runCatching {
                process.errorStream.bufferedReader().forEachLine { line ->
                    progressPattern.find(line)?.let { onProgress(it.groupValues[1].toInt().coerceIn(0, 100)) }
                    synchronized(stderrTail) {
                        stderrTail.addLast(line)
                        if (stderrTail.size > 20) stderrTail.removeFirst()
                    }
                }
            }
        }
        try {
            val timeoutMs = maxOf(10L * 60_000L, (audioDurationMs ?: 0L) * 10L)
            val finished = runInterruptible { process.waitFor(timeoutMs, TimeUnit.MILLISECONDS) }
            if (!finished) {
                throw IllegalStateException("Transkription nach ${timeoutMs / 60_000} min abgebrochen (Zeitlimit).")
            }
            stderrReader.join(2_000)
            stdoutReader.join(2_000)
            val exitCode = process.exitValue()
            if (exitCode != 0) {
                val tail = synchronized(stderrTail) { stderrTail.takeLast(4).joinToString(" | ") }
                throw IllegalStateException("whisper.cpp beendet mit Code $exitCode${if (tail.isNotBlank()) ": $tail" else ""}")
            }

            val jsonFile = File("${outputBase.absolutePath}.json")
            val segments = runCatching { WhisperJsonParser.parse(jsonFile.readText()) }.getOrNull()
            val words = segments?.let { WordAssembler.assemble(it) }.orEmpty()
            val transcript = segments?.joinToString(" ") { it.text }?.takeIf { it.isNotBlank() }
                ?: File("${outputBase.absolutePath}.txt").takeIf { it.isFile }?.readText()
                ?: ""
            WhisperRunResult(
                transcript = transcript.replace(Regex("\\s+"), " ").trim(),
                segments = segments.orEmpty(),
                words = words,
                elapsedMs = System.currentTimeMillis() - started
            )
        } finally {
            if (process.isAlive) process.destroyForcibly()
            listOf(wav, File("${outputBase.absolutePath}.json"), File("${outputBase.absolutePath}.txt")).forEach { it.delete() }
        }
    }
}

internal object WavWriter {
    fun writePcm16Mono(pcmFile: File, wavFile: File, sampleRate: Int): File {
        val dataSize = pcmFile.length()
        require(dataSize > 0L) { "Audio konnte nicht dekodiert werden (keine PCM-Daten)." }
        FileOutputStream(wavFile).use { output ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt((36L + dataSize).toInt())
                put("WAVE".toByteArray(Charsets.US_ASCII))
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16)
                putShort(1)
                putShort(1)
                putInt(sampleRate)
                putInt(sampleRate * 2)
                putShort(2)
                putShort(16)
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(dataSize.toInt())
            }
            output.write(header.array())
            FileInputStream(pcmFile).use { it.copyTo(output) }
        }
        return wavFile
    }
}
