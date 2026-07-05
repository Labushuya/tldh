package dev.bitsbots.tldhbench.bench

import dev.bitsbots.tldhbench.audio.AudioMetadata

data class TranscriptWord(
    val word: String,
    val startSec: Double?,
    val endSec: Double?,
    val confidence: Double?
)

data class TranscriptSegment(
    val startSec: Double?,
    val endSec: Double?,
    val text: String,
    val words: List<TranscriptWord> = emptyList()
)

data class BenchmarkTiming(
    val decodeMs: Long,
    val modelLoadMs: Long,
    val sttMs: Long,
    val totalMs: Long,
    val audioDurationMs: Long?
) {
    val rtf: Double? = audioDurationMs?.takeIf { it > 0L }?.let { totalMs.toDouble() / it.toDouble() }
}

data class BenchmarkVerdict(
    val targetMs: Long?,
    val passed: Boolean?,
    val message: String
)

data class BenchmarkResult(
    val engine: String,
    val model: String,
    val language: String,
    val metadata: AudioMetadata,
    val timing: BenchmarkTiming,
    val transcript: String,
    val segments: List<TranscriptSegment>,
    val verdict: BenchmarkVerdict,
    val warnings: List<String> = emptyList()
)

object BenchmarkTargets {
    fun targetFor(durationMs: Long?): Long? {
        if (durationMs == null || durationMs <= 0) return null
        return when {
            durationMs <= 15_000L -> 15_000L
            durationMs <= 60_000L -> 30_000L
            durationMs <= 180_000L -> 120_000L
            else -> null
        }
    }

    fun verdict(durationMs: Long?, totalMs: Long): BenchmarkVerdict {
        val target = targetFor(durationMs)
        if (target == null) {
            return BenchmarkVerdict(
                targetMs = null,
                passed = null,
                message = "Keine harte Zielmarke: Testaudio außerhalb der definierten 15s/60s/180s-Klassen."
            )
        }
        val passed = totalMs <= target
        return BenchmarkVerdict(
            targetMs = target,
            passed = passed,
            message = if (passed) "Bestanden: Gesamtzeit innerhalb Zielmarke." else "Nicht bestanden: Gesamtzeit überschreitet Zielmarke."
        )
    }
}
