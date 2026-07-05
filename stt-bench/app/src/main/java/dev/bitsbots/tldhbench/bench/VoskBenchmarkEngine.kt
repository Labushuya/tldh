package dev.bitsbots.tldhbench.bench

import dev.bitsbots.tldhbench.audio.PreparedPcmAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

class VoskBenchmarkEngine {
    suspend fun transcribe(
        modelDir: File,
        pcm: PreparedPcmAudio,
        decodeMs: Long,
        totalStartedAtMs: Long
    ): BenchmarkEngineOutput = withContext(Dispatchers.Default) {
        val jsonResults = mutableListOf<String>()
        var modelLoadMs = 0L
        lateinit var model: Any
        lateinit var recognizer: Any
        val modelClass = Class.forName("org.vosk.Model")
        val recognizerClass = Class.forName("org.vosk.Recognizer")

        modelLoadMs = measureTimeMillis {
            model = modelClass.getConstructor(String::class.java).newInstance(modelDir.absolutePath)
            recognizer = createRecognizer(recognizerClass, modelClass, model, 16_000f)
            runCatching { invokeAny(recognizer, listOf("setWords", "SetWords"), true, Boolean::class.javaPrimitiveType!!) }
            runCatching { invokeAny(recognizer, listOf("setWords", "SetWords"), 1, Int::class.javaPrimitiveType!!) }
        }

        val sttMs = measureTimeMillis {
            pcm.file.inputStream().buffered().use { input ->
                val buffer = ByteArray(8_000)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    val accepted = invokeAcceptWaveform(recognizer, buffer, read)
                    if (accepted) {
                        invokeResult(recognizer)?.takeIf { it.contains("text") }?.let { jsonResults += it }
                    }
                }
            }
            invokeFinalResult(recognizer)?.takeIf { it.contains("text") }?.let { jsonResults += it }
        }

        runCatching { invokeNoArgs(recognizer, listOf("close", "free", "Free")) }
        runCatching { invokeNoArgs(model, listOf("close", "free", "Free")) }

        val (transcript, segments) = JsonTranscriptParser.parseSegments(jsonResults)
        BenchmarkEngineOutput(
            modelLoadMs = modelLoadMs,
            sttMs = sttMs,
            transcript = transcript,
            segments = segments,
            totalMs = System.currentTimeMillis() - totalStartedAtMs,
            warnings = if (transcript.isBlank()) listOf("Vosk lieferte kein verwertbares Transkript. Prüfe Sprache, Modell und Audioqualität.") else emptyList()
        )
    }

    private fun createRecognizer(clazz: Class<*>, modelClass: Class<*>, model: Any, sampleRate: Float): Any {
        val constructors = listOf(
            runCatching { clazz.getConstructor(modelClass, Float::class.javaPrimitiveType) }.getOrNull(),
            runCatching { clazz.getConstructor(modelClass, Double::class.javaPrimitiveType) }.getOrNull(),
            runCatching { clazz.getConstructor(modelClass, Int::class.javaPrimitiveType) }.getOrNull()
        ).filterNotNull()
        for (constructor in constructors) {
            val type = constructor.parameterTypes[1]
            if (type == Float::class.javaPrimitiveType) return constructor.newInstance(model, sampleRate)
            if (type == Double::class.javaPrimitiveType) return constructor.newInstance(model, sampleRate.toDouble())
            if (type == Int::class.javaPrimitiveType) return constructor.newInstance(model, sampleRate.toInt())
        }
        error("Kein kompatibler Vosk Recognizer-Konstruktor gefunden.")
    }

    private fun invokeAcceptWaveform(recognizer: Any, buffer: ByteArray, read: Int): Boolean {
        val methodNames = listOf("acceptWaveForm", "AcceptWaveform", "acceptWaveform")
        for (name in methodNames) {
            val method = runCatching { recognizer.javaClass.getMethod(name, ByteArray::class.java, Int::class.javaPrimitiveType) }.getOrNull() ?: continue
            val value = method.invoke(recognizer, buffer, read)
            return when (value) {
                null -> false
                is Boolean -> value
                is Int -> value != 0
                else -> false
            }
        }
        error("Keine kompatible Vosk acceptWaveForm-Methode gefunden.")
    }

    private fun invokeResult(recognizer: Any): String? =
        invokeStringNoArgs(recognizer, listOf("getResult", "result", "Result"))

    private fun invokeFinalResult(recognizer: Any): String? =
        invokeStringNoArgs(recognizer, listOf("getFinalResult", "finalResult", "FinalResult"))

    private fun invokeStringNoArgs(target: Any, names: List<String>): String? {
        for (name in names) {
            val method = runCatching { target.javaClass.getMethod(name) }.getOrNull() ?: continue
            return method.invoke(target)?.toString()
        }
        return null
    }

    private fun invokeNoArgs(target: Any, names: List<String>) {
        for (name in names) {
            val method = runCatching { target.javaClass.getMethod(name) }.getOrNull() ?: continue
            method.invoke(target)
            return
        }
    }

    private fun invokeAny(target: Any, names: List<String>, arg: Any, type: Class<*>) {
        for (name in names) {
            val method = runCatching { target.javaClass.getMethod(name, type) }.getOrNull() ?: continue
            method.invoke(target, arg)
            return
        }
    }
}

data class BenchmarkEngineOutput(
    val modelLoadMs: Long,
    val sttMs: Long,
    val totalMs: Long,
    val transcript: String,
    val segments: List<TranscriptSegment>,
    val warnings: List<String>
)
