package dev.bitsbots.tldh.transcription

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import dev.bitsbots.tldh.audio.AudioIngestPolicy
import dev.bitsbots.tldh.audio.AudioMetadata
import dev.bitsbots.tldh.share.SharedAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.coroutines.resume
import kotlin.system.measureTimeMillis

private const val SPIKE_MAX_DURATION_MS = 5L * 60L * 1000L
private const val RECOGNITION_TIMEOUT_MS = 5L * 60L * 1000L

class LocalTranscriptionSpike(
    private val context: Context,
    private val workDir: File
) {
    suspend fun transcribe(sharedAudio: SharedAudio, metadata: AudioMetadata): TranscriptionOutcome {
        val duration = metadata.durationMs
        if (duration != null && duration > SPIKE_MAX_DURATION_MS) {
            return TranscriptionOutcome(
                status = TranscriptionStatus.SKIPPED,
                message = "Lokaler Transkriptions-Spike ist aktuell auf ${AudioIngestPolicy.formatDuration(SPIKE_MAX_DURATION_MS)} begrenzt. Diese Audio hat ${AudioIngestPolicy.formatDuration(duration)}."
            )
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return TranscriptionOutcome(
                status = TranscriptionStatus.UNAVAILABLE,
                message = "Dateibasierte On-Device-Erkennung braucht Android 13 / API 33 oder neuer."
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            return TranscriptionOutcome(
                status = TranscriptionStatus.UNAVAILABLE,
                message = "Auf diesem Gerät ist kein On-Device-Speech-Recognizer verfügbar."
            )
        }

        return runCatching {
            var result: TranscriptionOutcome
            val elapsed = measureTimeMillis {
                val prepared = PcmAudioPreparer(context, workDir).prepare(sharedAudio.uri, metadata.durationMs)
                result = recognizePreparedAudio(prepared)
            }
            result.copy(elapsedMs = elapsed)
        }.getOrElse { error ->
            TranscriptionOutcome(
                status = TranscriptionStatus.FAILED,
                message = error.message ?: "Lokale Transkription ist im Spike fehlgeschlagen."
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun recognizePreparedAudio(prepared: PreparedPcmAudio): TranscriptionOutcome = withContext(Dispatchers.Main.immediate) {
        withTimeout(RECOGNITION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                var recognizer: SpeechRecognizer? = null
                var pfd: ParcelFileDescriptor? = null

                fun finish(outcome: TranscriptionOutcome) {
                    runCatching { pfd?.close() }
                    runCatching { recognizer?.cancel() }
                    runCatching { recognizer?.destroy() }
                    if (continuation.isActive) continuation.resume(outcome)
                }

                try {
                    pfd = ParcelFileDescriptor.open(prepared.file, ParcelFileDescriptor.MODE_READ_ONLY)
                    recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, pfd)
                        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, prepared.channelCount)
                        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, prepared.encoding)
                        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, prepared.sampleRate)
                        putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_AUDIO_SOURCE)
                    }

                    recognizer?.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
                        override fun onBeginningOfSpeech() = Unit
                        override fun onRmsChanged(rmsdB: Float) = Unit
                        override fun onBufferReceived(buffer: ByteArray?) = Unit
                        override fun onEndOfSpeech() = Unit
                        override fun onPartialResults(partialResults: android.os.Bundle?) = Unit
                        override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit

                        override fun onError(error: Int) {
                            finish(
                                TranscriptionOutcome(
                                    status = TranscriptionStatus.FAILED,
                                    message = speechErrorMessage(error)
                                )
                            )
                        }

                        override fun onResults(results: android.os.Bundle?) {
                            val text = results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                ?.trim()

                            if (text.isNullOrBlank()) {
                                finish(
                                    TranscriptionOutcome(
                                        status = TranscriptionStatus.FAILED,
                                        message = "Der lokale Recognizer hat kein verwertbares Transkript zurückgegeben."
                                    )
                                )
                            } else {
                                finish(
                                    TranscriptionOutcome(
                                        status = TranscriptionStatus.COMPLETED,
                                        transcript = text,
                                        message = "Lokale Transkription erfolgreich."
                                    )
                                )
                            }
                        }
                    })

                    continuation.invokeOnCancellation {
                        runCatching { pfd?.close() }
                        runCatching { recognizer?.cancel() }
                        runCatching { recognizer?.destroy() }
                    }
                    recognizer?.startListening(intent)
                } catch (error: Throwable) {
                    finish(
                        TranscriptionOutcome(
                            status = TranscriptionStatus.FAILED,
                            message = error.message ?: "On-Device-Recognizer konnte nicht gestartet werden."
                        )
                    )
                }
            }
        }
    }

    private fun speechErrorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Lokaler Recognizer meldet einen Audiofehler."
        SpeechRecognizer.ERROR_CLIENT -> "Lokaler Recognizer wurde vom Client abgebrochen oder nicht unterstützt."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Lokaler Recognizer meldet fehlende Berechtigungen."
        SpeechRecognizer.ERROR_NETWORK -> "Recognizer meldet Netzwerkfehler, obwohl Offline-Erkennung angefordert wurde."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Recognizer meldet Netzwerk-Timeout, obwohl Offline-Erkennung angefordert wurde."
        SpeechRecognizer.ERROR_NO_MATCH -> "Der lokale Recognizer konnte keine Sprache sicher erkennen."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Lokaler Recognizer ist gerade belegt."
        SpeechRecognizer.ERROR_SERVER -> "Recognizer-Service meldet einen internen Fehler."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Der lokale Recognizer hat keine Sprache im Audio erkannt."
        else -> "Lokaler Recognizer meldet Fehlercode $error."
    }
}
