package dev.bitsbots.tldhbench.bench

import dev.bitsbots.tldhbench.audio.PcmWavWriter
import dev.bitsbots.tldhbench.audio.PreparedPcmAudio
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.system.measureTimeMillis

internal data class GroqEngineOutput(
    val transcript: String,
    val segments: List<TranscriptSegment>,
    val modelLoadMs: Long,
    val sttMs: Long,
    val totalMs: Long,
    val warnings: List<String> = emptyList()
)

/**
 * Remote Groq Speech-to-Text runner.
 *
 * The app intentionally uploads the same prepared 16 kHz mono WAV that whisper.cpp uses.
 * That keeps the local-vs-remote comparison closer than uploading arbitrary original formats
 * while still preserving the same ReferenceTextComparator/WER/CER/S/I/D pipeline.
 */
internal class GroqBenchmarkEngine {
    suspend fun transcribe(
        apiKey: String,
        modelSpec: GroqSttModelSpec,
        pcm: PreparedPcmAudio,
        prompt: String,
        decodeMs: Long,
        totalStartedAtMs: Long,
        workDir: File
    ): GroqEngineOutput {
        val cleanApiKey = apiKey.trim()
        require(cleanApiKey.isNotBlank()) { "Groq API-Key fehlt. Lege ihn im Engines-Tab unter Remote / Groq an." }

        val wavFile = PcmWavWriter.writePcm16MonoWav(
            pcmFile = pcm.file,
            wavFile = File(workDir, "tldh-groq-input-16k-mono.wav"),
            sampleRate = pcm.sampleRate
        )

        var transcript = ""
        var segments = emptyList<TranscriptSegment>()
        val sttMs = try {
            measureTimeMillis {
                val body = postTranscription(
                    apiKey = cleanApiKey,
                    modelId = modelSpec.id,
                    prompt = prompt,
                    wavFile = wavFile
                )
                val parsed = parseVerboseJson(body)
                transcript = parsed.first
                segments = parsed.second
            }
        } finally {
            runCatching { wavFile.delete() }
        }

        val warnings = buildList {
            add("Remote-STT aktiv: Audio wurde an Groq Speech-to-Text übertragen. Nur mit explizitem API-Key und bewusster Remote-Auswahl nutzen.")
            add("Groq Deutsch-Lock aktiv: language=de, response_format=verbose_json, timestamp_granularities[]=segment und word.")
            add("Vergleichbarkeit: Upload erfolgt als lokal vorbereitetes 16 kHz Mono WAV mit gewähltem Audio-Prep-Profil, nicht als Roh-OGG. Dadurch sind Audio-Prep-Matrix und WER/CER mit Local-Engines vergleichbarer.")
            add("Datenschutz: API-Key wird in dieser Bench-Version nur app-intern gespeichert; für Produktbetrieb wäre Android-Keystore/Encrypted Storage Pflicht.")
            if (decodeMs > 10_000L) add("Lokales Audio-Prep vor Remote-Upload dauerte auffällig lange (${formatSecondsLocal(decodeMs)}).")
            if (transcript.isBlank()) add("Groq hat ein leeres Transkript geliefert. API-Key, Modell, Dateigröße und Audioformat prüfen.")
        }

        return GroqEngineOutput(
            transcript = transcript,
            segments = segments,
            modelLoadMs = 0L,
            sttMs = sttMs,
            totalMs = System.currentTimeMillis() - totalStartedAtMs,
            warnings = warnings
        )
    }

    private fun postTranscription(
        apiKey: String,
        modelId: String,
        prompt: String,
        wavFile: File
    ): String {
        val boundary = "----tldhGroq${UUID.randomUUID().toString().replace("-", "")}" 
        val connection = (URL("https://api.groq.com/openai/v1/audio/transcriptions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 10 * 60_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("Accept", "application/json")
        }

        try {
            BufferedOutputStream(connection.outputStream).use { out ->
                out.writeFormField(boundary, "model", modelId)
                out.writeFormField(boundary, "language", "de")
                out.writeFormField(boundary, "temperature", "0")
                out.writeFormField(boundary, "response_format", "verbose_json")
                out.writeFormField(boundary, "timestamp_granularities[]", "segment")
                out.writeFormField(boundary, "timestamp_granularities[]", "word")
                if (prompt.isNotBlank()) out.writeFormField(boundary, "prompt", prompt.take(900))
                out.writeFileField(boundary, "file", "tldh-groq-input.wav", "audio/wav", wavFile)
                out.writeAscii("--$boundary--\r\n")
                out.flush()
            }
            val code = connection.responseCode
            val body = if (code in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            if (code !in 200..299) {
                val message = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: body.take(500).ifBlank { "HTTP $code" }
                throw IllegalStateException("Groq STT HTTP $code: $message")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun parseVerboseJson(body: String): Pair<String, List<TranscriptSegment>> {
        val json = JSONObject(body)
        val transcript = json.optString("text", "").trim()
        val segmentsJson = json.optJSONArray("segments")
        val segments = buildList {
            if (segmentsJson != null) {
                for (i in 0 until segmentsJson.length()) {
                    val item = segmentsJson.optJSONObject(i) ?: continue
                    val text = item.optString("text", "").trim()
                    if (text.isBlank()) continue
                    add(
                        TranscriptSegment(
                            startSec = item.optNullableDouble("start"),
                            endSec = item.optNullableDouble("end"),
                            text = text,
                            words = emptyList()
                        )
                    )
                }
            }
        }
        return transcript to segments
    }

    private fun JSONObject.optNullableDouble(name: String): Double? =
        if (has(name) && !isNull(name)) optDouble(name) else null

    private fun BufferedOutputStream.writeFormField(boundary: String, name: String, value: String) {
        writeAscii("--$boundary\r\n")
        writeAscii("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        writeAscii(value)
        writeAscii("\r\n")
    }

    private fun BufferedOutputStream.writeFileField(
        boundary: String,
        name: String,
        filename: String,
        contentType: String,
        file: File
    ) {
        writeAscii("--$boundary\r\n")
        writeAscii("Content-Disposition: form-data; name=\"$name\"; filename=\"$filename\"\r\n")
        writeAscii("Content-Type: $contentType\r\n\r\n")
        file.inputStream().use { it.copyTo(this) }
        writeAscii("\r\n")
    }

    private fun BufferedOutputStream.writeAscii(value: String) {
        write(value.toByteArray(Charsets.UTF_8))
    }
}

private fun formatSecondsLocal(ms: Long): String = "%.2f s".format(java.util.Locale.GERMANY, ms / 1000.0)
