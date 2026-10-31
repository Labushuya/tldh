package dev.bitsbots.tldh.summarization

import dev.bitsbots.tldh.audio.AudioIngestPolicy
import dev.bitsbots.tldh.audio.AudioMetadata
import dev.bitsbots.tldh.transcription.TranscriptionOutcome
import dev.bitsbots.tldh.transcription.TranscriptionStatus

/**
 * Placeholder until the rule-based summary of v0.5.0: the TL;DR shows the first sentences of the
 * transcript as a preview. It never invents content beyond the transcript.
 */
class PreviewSummaryEngine {
    fun summarize(metadata: AudioMetadata, transcription: TranscriptionOutcome): AudioSummary {
        val name = metadata.displayName ?: "geteilte Audiodatei"
        val sizeText = metadata.sizeBytes?.let { humanBytes(it) } ?: "unbekannte Größe"
        val durationText = AudioIngestPolicy.formatDuration(metadata.durationMs)
        val transcript = transcription.transcript?.trim()?.takeIf { it.isNotBlank() }
        val uncertainCount = transcription.uncertainWords.size

        val warnings = buildList {
            addAll(metadata.validation.warnings)
            if (transcription.status != TranscriptionStatus.COMPLETED) add(transcription.message)
            if (uncertainCount > 0) {
                add("$uncertainCount Wörter sind unsicher erkannt und im Transkript markiert. Antippen spielt die Stelle ab.")
            }
        }

        val technicalDetails = listOf(
            "Quelle: Android ACTION_SEND",
            "Datei: $name",
            "MIME: ${metadata.mimeType ?: "unbekannt"}; Extension: ${metadata.extension ?: "unbekannt"}",
            "Format: ${metadata.format}; Größe: $sizeText; Dauer: $durationText",
            "Limits: 50 MB, Warnung ab 3:00 min, Soft 10:00 min, Hard 15:00 min",
            "Transkription: ${transcription.engine}${transcription.modelName?.let { " · $it" } ?: ""}; ${transcription.status}" +
                (transcription.elapsedMs?.let { "; %.1f s".format(java.util.Locale.GERMANY, it / 1000.0) } ?: "")
        )

        return if (transcript != null) {
            AudioSummary(
                tldr = transcript.leadingSentences(),
                keyPoints = listOf(
                    "Transkript mit ${transcription.words.size.takeIf { it > 0 } ?: transcript.split(' ').size} Wörtern, davon $uncertainCount unsicher.",
                    "Audio: $durationText · $sizeText · ${metadata.format}.",
                    "Vorschau aus den ersten Sätzen. Die Zusammenfassung mit Kernaussagen und Warnungen folgt in v0.5.0."
                ),
                tags = emptyList(),
                category = null,
                replySuggestions = emptyList(),
                warnings = warnings,
                transcript = transcript,
                transcriptionStatus = "${transcription.engine}: ${transcription.status.name.lowercase()}",
                technicalDetails = technicalDetails
            )
        } else {
            AudioSummary(
                tldr = "Audio erkannt ($durationText · $sizeText), aber kein Text erkannt.",
                keyPoints = listOf(
                    "Transkriptionsstatus: ${transcription.status}.",
                    transcription.message
                ),
                tags = emptyList(),
                category = null,
                replySuggestions = emptyList(),
                warnings = warnings,
                transcript = null,
                transcriptionStatus = "${transcription.engine}: ${transcription.status.name.lowercase()}",
                technicalDetails = technicalDetails
            )
        }
    }

    private fun String.leadingSentences(maxChars: Int = 240): String {
        val clean = replace(Regex("\\s+"), " ").trim()
        val sentences = clean.split(Regex("(?<=[.!?])\\s+"))
        val preview = StringBuilder()
        for (sentence in sentences) {
            if (preview.isNotEmpty() && preview.length + sentence.length + 1 > maxChars) break
            if (preview.isNotEmpty()) preview.append(' ')
            preview.append(sentence)
        }
        val text = preview.toString()
        return if (text.length <= maxChars) text else text.take(maxChars - 1).trimEnd() + "…"
    }

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
