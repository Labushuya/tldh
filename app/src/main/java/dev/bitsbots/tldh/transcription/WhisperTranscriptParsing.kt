package dev.bitsbots.tldh.transcription

import org.json.JSONObject

/** How far a recognized word can be trusted, derived from whisper's token probabilities. */
enum class ConfidenceLevel {
    SICHER,
    UNSICHER,
    SEHR_UNSICHER;

    companion object {
        // Initial thresholds; whisper's probabilities are not calibrated, so these need tuning
        // against reference transcripts in the STT bench.
        const val SURE_THRESHOLD = 0.70f
        const val UNSURE_THRESHOLD = 0.40f

        fun of(probability: Float): ConfidenceLevel = when {
            probability >= SURE_THRESHOLD -> SICHER
            probability >= UNSURE_THRESHOLD -> UNSICHER
            else -> SEHR_UNSICHER
        }
    }
}

data class TranscriptWord(
    val text: String,
    val startMs: Long?,
    val endMs: Long?,
    /** Lowest probability among the word's letter/digit tokens, 0..1. */
    val probability: Float
) {
    val confidence: ConfidenceLevel get() = ConfidenceLevel.of(probability)
}

data class WhisperToken(
    val text: String,
    val startMs: Long?,
    val endMs: Long?,
    val probability: Float
)

data class WhisperSegment(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val tokens: List<WhisperToken>
)

/** Reads the `--output-json-full` file written by whisper-cli. */
object WhisperJsonParser {
    fun parse(json: String): List<WhisperSegment> {
        val transcription = JSONObject(json).optJSONArray("transcription") ?: return emptyList()
        return (0 until transcription.length()).mapNotNull { i ->
            val segment = transcription.optJSONObject(i) ?: return@mapNotNull null
            val offsets = segment.optJSONObject("offsets")
            val tokens = segment.optJSONArray("tokens")
            WhisperSegment(
                text = segment.optString("text", "").trim(),
                startMs = offsets?.optLong("from", 0L) ?: 0L,
                endMs = offsets?.optLong("to", 0L) ?: 0L,
                tokens = if (tokens == null) emptyList() else (0 until tokens.length()).mapNotNull { j ->
                    val token = tokens.optJSONObject(j) ?: return@mapNotNull null
                    val tokenOffsets = token.optJSONObject("offsets")
                    WhisperToken(
                        text = token.optString("text", ""),
                        startMs = tokenOffsets?.optLong("from"),
                        endMs = tokenOffsets?.optLong("to"),
                        probability = token.optDouble("p", 1.0).toFloat()
                    )
                }
            )
        }
    }
}

/**
 * Joins whisper's sub-word tokens into words. A token starting with a space opens a new word,
 * punctuation sticks to the word before it, and special tokens ([_BEG_], [_TT_150], <|de|>) are dropped.
 */
object WordAssembler {
    private val specialToken = Regex("""^\[_[A-Za-z0-9_]+]$|^<\|[^|]*\|>$""")

    fun assemble(segments: List<WhisperSegment>): List<TranscriptWord> {
        val words = mutableListOf<TranscriptWord>()
        for (segment in segments) {
            var current = mutableListOf<WhisperToken>()
            for (token in segment.tokens) {
                if (token.text.isBlank() || specialToken.matches(token.text.trim())) continue
                if (token.text.startsWith(" ") && current.isNotEmpty()) {
                    words += toWord(current)
                    current = mutableListOf()
                }
                current += token
            }
            if (current.isNotEmpty()) words += toWord(current)
        }
        return words.filter { it.text.isNotBlank() }
    }

    private fun toWord(tokens: List<WhisperToken>): TranscriptWord {
        val scored = tokens.filter { token -> token.text.any { it.isLetterOrDigit() } }
        return TranscriptWord(
            text = tokens.joinToString("") { it.text }.trim(),
            startMs = tokens.firstNotNullOfOrNull { it.startMs },
            endMs = tokens.lastOrNull { it.endMs != null }?.endMs,
            probability = scored.minOfOrNull { it.probability } ?: 1f
        )
    }
}

/** Parses whisper-cli's live stdout lines: `[00:00:00.000 --> 00:00:04.200]  text`. */
object WhisperStdout {
    private val segmentLine = Regex("""^\[\d{2}:\d{2}:\d{2}[.,]\d{3} --> \d{2}:\d{2}:\d{2}[.,]\d{3}]\s*(.*)$""")

    fun segmentText(line: String): String? =
        segmentLine.find(line.trim())?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
}
