package dev.bitsbots.tldh.ui

import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.bitsbots.tldh.analysis.ReplyTone
import dev.bitsbots.tldh.audio.AudioFormat
import dev.bitsbots.tldh.audio.AudioMetadata
import dev.bitsbots.tldh.audio.AudioValidation
import dev.bitsbots.tldh.summarization.RuleBasedSummaryEngine
import dev.bitsbots.tldh.transcription.TranscriptWord
import dev.bitsbots.tldh.transcription.TranscriptionOutcome
import dev.bitsbots.tldh.transcription.TranscriptionStatus
import dev.bitsbots.tldh.ui.screens.ResultScreen
import dev.bitsbots.tldh.ui.theme.TldhTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The result screen with a long voice note (about 5 minutes, 900 words, many uncertain words)
 * must render and react quickly; Android shows "app isn't responding" after 5 s.
 */
@RunWith(AndroidJUnit4::class)
class ResultScreenStressTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun longTranscriptWithManyUncertainWordsStaysResponsive() {
        val result = longResult(words = 900)

        val shown = timed("first frame") {
            compose.setContent {
                TldhTheme {
                    ResultScreen(result = result, markUncertain = true, tone = ReplyTone.FREUNDLICH, onToneChange = {}, onClose = {})
                }
            }
            compose.waitForIdle()
            compose.onRoot().captureToImage()
        }
        val expand = timed("expand transcript") {
            compose.onNodeWithText("Ganz anzeigen").performScrollTo().performClick()
            compose.waitForIdle()
            compose.onRoot().captureToImage()
        }
        val scroll = timed("scroll 6x") {
            repeat(6) {
                compose.onRoot().performTouchInput { swipeUp() }
                compose.waitForIdle()
                compose.onRoot().captureToImage()
            }
        }
        assertTrue("first frame took $shown ms", shown < LIMIT_MS)
        assertTrue("expanding the transcript took $expand ms", expand < LIMIT_MS)
        assertTrue("six scroll steps took $scroll ms", scroll < 2 * LIMIT_MS)
    }

    private fun timed(label: String, block: () -> Unit): Long {
        val start = SystemClock.uptimeMillis()
        block()
        val ms = SystemClock.uptimeMillis() - start
        Log.i(TAG, "$label: $ms ms")
        return ms
    }

    private fun longResult(words: Int): TldhUiState.Result {
        val vocabulary = listOf(
            "Hey", "ich", "wollte", "nur", "kurz", "fragen", "wegen", "Samstag", "hast", "du", "um", "17", "Uhr",
            "Zeit", "wir", "würden", "bei", "mir", "grillen", "kannst", "bitte", "die", "Getränke", "mitbringen",
            "so", "für", "20", "Euro", "ich", "komme", "morgen", "nicht", "zum", "Training", "sag", "das", "Jonas"
        )
        val transcriptWords = (0 until words).map { i ->
            val base = vocabulary[i % vocabulary.size]
            val text = if (i % 14 == 13) "$base." else if (i % 23 == 22) "$base?" else base
            val probability = when (i % 5) {
                0 -> 0.32f
                1 -> 0.58f
                else -> 0.93f
            }
            TranscriptWord(text = text, startMs = i * 320L, endMs = i * 320L + 260L, probability = probability)
        }
        val transcript = transcriptWords.joinToString(" ") { it.text }
        val outcome = TranscriptionOutcome(
            status = TranscriptionStatus.COMPLETED,
            transcript = transcript,
            message = "Lokal transkribiert.",
            elapsedMs = 61_000L,
            modelName = "Whisper small",
            words = transcriptWords
        )
        val metadata = AudioMetadata(
            uriString = "content://test/voice.opus",
            displayName = "PTT-20261031-WA0001.opus",
            mimeType = "audio/ogg",
            sizeBytes = 900_000L,
            durationMs = 285_000L,
            format = AudioFormat.OGG_OPUS,
            headerBytes = ByteArray(0),
            extension = "opus",
            headerProbeBytes = 0,
            validation = AudioValidation(accepted = true, rejectReasons = emptyList(), warnings = emptyList())
        )
        val summary = RuleBasedSummaryEngine().summarize(metadata, outcome)
        return TldhUiState.Result(summary, outcome, "Sprachnachricht", "WhatsApp · 4:45 min · in 61 s transkribiert")
    }

    private companion object {
        const val TAG = "TldhStress"
        const val LIMIT_MS = 2_500L
    }
}
