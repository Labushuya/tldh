package dev.bitsbots.tldhbench.bench

import dev.bitsbots.tldhbench.audio.AudioMetadata

enum class Signal { GREEN, YELLOW, RED }

data class VoskModelSpec(
    val id: String,
    val displayName: String,
    val directoryName: String,
    val url: String,
    val sizeLabel: String,
    val speedSignal: Signal,
    val accuracySignal: Signal,
    val deviceSignal: Signal,
    val tradeoff: String,
    val notes: String
)

object VoskModelCatalog {
    val models: List<VoskModelSpec> = listOf(
        VoskModelSpec(
            id = "small-de-0.15",
            displayName = "Small DE 0.15",
            directoryName = "vosk-model-small-de-0.15",
            url = "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip",
            sizeLabel = "45 MB",
            speedSignal = Signal.GREEN,
            accuracySignal = Signal.YELLOW,
            deviceSignal = Signal.GREEN,
            tradeoff = "Fast Mode: sehr schnell, Qualität mittel.",
            notes = "Bisher auf dem Magic V2 sehr schnell. Kandidat für lokale Sofort-Vorschau."
        ),
        VoskModelSpec(
            id = "small-de-zamia-0.3",
            displayName = "Small DE Zamia 0.3",
            directoryName = "vosk-model-small-de-zamia-0.3",
            url = "https://alphacephei.com/vosk/models/vosk-model-small-de-zamia-0.3.zip",
            sizeLabel = "49 MB",
            speedSignal = Signal.GREEN,
            accuracySignal = Signal.RED,
            deviceSignal = Signal.GREEN,
            tradeoff = "Alternativer Small-Test: schnell, laut Vosk-Liste nicht empfohlen.",
            notes = "Nur als Gegenprobe sinnvoll. Erwartung: nicht besser als Small DE 0.15."
        ),
        VoskModelSpec(
            id = "de-0.21",
            displayName = "Big DE 0.21",
            directoryName = "vosk-model-de-0.21",
            url = "https://alphacephei.com/vosk/models/vosk-model-de-0.21.zip",
            sizeLabel = "1.9 GB",
            speedSignal = Signal.RED,
            accuracySignal = Signal.GREEN,
            deviceSignal = Signal.YELLOW,
            tradeoff = "Quality Mode: deutlich größer, potenziell genauer, Handy-Risiko.",
            notes = "Eher Tower/LAN-Kandidat. Auf dem Handy nur bewusst testen, Speicher und Wartezeit beachten."
        ),
        VoskModelSpec(
            id = "de-tuda-0.6-900k",
            displayName = "Big DE TUDA 0.6",
            directoryName = "vosk-model-de-tuda-0.6-900k",
            url = "https://alphacephei.com/vosk/models/vosk-model-de-tuda-0.6-900k.zip",
            sizeLabel = "4.4 GB",
            speedSignal = Signal.RED,
            accuracySignal = Signal.GREEN,
            deviceSignal = Signal.RED,
            tradeoff = "Max-Quality-Versuch: sehr groß, eher nicht Handy-geeignet.",
            notes = "Nur als Extrem-/Tower-Vergleich gedacht. Download und Modellladen können sehr lange dauern."
        )
    )

    val defaultModel: VoskModelSpec = models.first()

    fun byId(id: String): VoskModelSpec = models.firstOrNull { it.id == id } ?: defaultModel
}

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

enum class WordDiffType {
    SUBSTITUTE,
    INSERT,
    DELETE
}

data class WordDiff(
    val type: WordDiffType,
    val referenceIndex: Int?,
    val hypothesisIndex: Int?,
    val referenceWord: String?,
    val hypothesisWord: String?
)

data class ReferenceComparison(
    val referenceRaw: String,
    val hypothesisRaw: String,
    val referenceWordCount: Int,
    val hypothesisWordCount: Int,
    val wordDistance: Int,
    val wordSubstitutions: Int,
    val wordInsertions: Int,
    val wordDeletions: Int,
    val werPercent: Double,
    val charDistance: Int,
    val referenceCharCount: Int,
    val cerPercent: Double,
    val qualityLabel: String,
    val summary: String,
    val wordDiffs: List<WordDiff> = emptyList()
)

data class BenchmarkResult(
    val engine: String,
    val model: String,
    val modelId: String,
    val language: String,
    val metadata: AudioMetadata,
    val timing: BenchmarkTiming,
    val transcript: String,
    val segments: List<TranscriptSegment>,
    val verdict: BenchmarkVerdict,
    val warnings: List<String> = emptyList(),
    val referenceComparison: ReferenceComparison? = null
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
