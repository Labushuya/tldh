package dev.bitsbots.tldh.summarization

import dev.bitsbots.tldh.analysis.NoteFormatter
import dev.bitsbots.tldh.analysis.ReplySuggester
import dev.bitsbots.tldh.analysis.TranscriptAnalyzer
import dev.bitsbots.tldh.audio.AudioIngestPolicy
import dev.bitsbots.tldh.audio.AudioMetadata
import dev.bitsbots.tldh.transcription.TranscriptionOutcome
import dev.bitsbots.tldh.transcription.TranscriptionStatus

/**
 * Builds the result card from the transcript with rules only: the TL;DR and key points are taken
 * verbatim from the transcript (see [TranscriptAnalyzer]), so nothing is generated or invented.
 */
class RuleBasedSummaryEngine {
    fun summarize(metadata: AudioMetadata, transcription: TranscriptionOutcome): AudioSummary {
        val name = metadata.displayName ?: "geteilte Audiodatei"
        val sizeText = metadata.sizeBytes?.let { humanBytes(it) } ?: "unbekannte Größe"
        val durationText = AudioIngestPolicy.formatDuration(metadata.durationMs)
        val transcript = transcription.transcript?.trim()?.takeIf { it.isNotBlank() }
        val analysis = when {
            transcription.words.isNotEmpty() -> TranscriptAnalyzer.analyze(transcription.words)
            transcript != null -> TranscriptAnalyzer.analyze(transcript)
            else -> null
        }

        val warnings = buildList {
            addAll(metadata.validation.warnings)
            if (transcription.status != TranscriptionStatus.COMPLETED) add(transcription.message)
        }

        val technicalDetails = listOf(
            "Quelle: Android ACTION_SEND",
            "Datei: $name",
            "MIME: ${metadata.mimeType ?: "unbekannt"}; Extension: ${metadata.extension ?: "unbekannt"}",
            "Format: ${metadata.format}; Größe: $sizeText; Dauer: $durationText",
            "Limits: 50 MB, Warnung ab 3:00 min, Soft 10:00 min, Hard 15:00 min",
            "Transkription: ${transcription.engine}${transcription.modelName?.let { " · $it" } ?: ""}; ${transcription.status}" +
                (transcription.elapsedMs?.let { "; %.1f s".format(java.util.Locale.GERMANY, it / 1000.0) } ?: ""),
            "Wörter: ${transcription.words.size}, davon unsicher: ${transcription.uncertainWords.size}"
        )

        return AudioSummary(
            tldr = analysis?.tldr?.takeIf { it.isNotBlank() }
                ?: "Audio erkannt ($durationText · $sizeText), aber kein Text erkannt.",
            questions = analysis?.questions.orEmpty(),
            datesAndNumbers = analysis?.datesAndNumbers.orEmpty(),
            tasks = analysis?.tasks.orEmpty(),
            checks = analysis?.checks.orEmpty(),
            replies = analysis?.takeIf { transcript != null }?.let { ReplySuggester.suggest(it) },
            note = analysis?.takeIf { transcript != null }?.let { NoteFormatter.format(it, noteLabel(durationText)) },
            warnings = warnings,
            transcript = transcript,
            transcriptionStatus = "${transcription.engine}: ${transcription.status.name.lowercase()}",
            technicalDetails = technicalDetails
        )
    }

    private fun noteLabel(durationText: String): String =
        "${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.GERMANY).format(java.util.Date())} · $durationText"

    private fun humanBytes(bytes: Long): String {
        val mib = 1024L * 1024L
        val kib = 1024L
        return when {
            bytes >= mib -> "${bytes / mib} MB"
            bytes >= kib -> "${bytes / kib} KB"
            else -> "$bytes B"
        }
    }
}
