package dev.bitsbots.tldh.summarization

import dev.bitsbots.tldh.audio.AudioIngestPolicy
import dev.bitsbots.tldh.audio.AudioMetadata
import dev.bitsbots.tldh.transcription.TranscriptionOutcome
import dev.bitsbots.tldh.transcription.TranscriptionStatus

class FakeSummaryEngine {
    fun summarize(metadata: AudioMetadata, transcription: TranscriptionOutcome? = null): AudioSummary {
        val name = metadata.displayName ?: "geteilte Audiodatei"
        val sizeText = metadata.sizeBytes?.let { humanBytes(it) } ?: "unbekannte Größe"
        val durationText = AudioIngestPolicy.formatDuration(metadata.durationMs)
        val transcript = transcription?.transcript?.trim()?.takeIf { it.isNotBlank() }
        val transcriptionStatus = transcription?.let { "${it.engine}: ${it.status.name.lowercase()}" }
        val warnings = buildList {
            if (transcript == null) {
                add("v0.3.0 versucht lokale Transkription. Wenn der Geräte-Recognizer sie nicht unterstützt, bleibt tl;dh sauber beim Audio-Ingest-Fallback.")
            }
            addAll(metadata.validation.warnings)
            transcription?.takeIf { it.status != TranscriptionStatus.COMPLETED }?.let { add(it.message) }
        }

        val technicalDetails = listOf(
            "Quelle: Android ACTION_SEND",
            "Datei: $name",
            "MIME: ${metadata.mimeType ?: "unbekannt"}; Extension: ${metadata.extension ?: "unbekannt"}",
            "Format: ${metadata.format}; akzeptiert: ${metadata.validation.accepted}",
            "Größe: $sizeText; Dauer: $durationText; Header-Probe: ${metadata.headerProbeBytes} Bytes",
            "Limits: 50 MB, Warnung ab 3:00 min, Soft 10:00 min, Hard 15:00 min"
        ) + listOfNotNull(transcription?.let { "Transkription: ${it.status}; ${it.message}${it.elapsedMs?.let { ms -> "; ${ms} ms" } ?: ""}" })

        return if (transcript != null) {
            AudioSummary(
                tldr = transcript.toConciseTldr(),
                keyPoints = listOf(
                    "Lokale Transkription wurde erzeugt.",
                    "Audio: $durationText · $sizeText · ${metadata.format}.",
                    "Inhalt ist als Transkript kopierbar; echte semantische Zusammenfassung folgt nach dem Whisper-/LLM-Hardening."
                ),
                tags = listOf("local-transcription", "offline-first", "audio-ingest", metadata.format.name.lowercase()),
                category = "Local Transcription Spike",
                replySuggestions = transcript.replySuggestionsFromTranscript(),
                warnings = warnings,
                transcript = transcript,
                transcriptionStatus = transcriptionStatus,
                technicalDetails = technicalDetails
            )
        } else {
            AudioSummary(
                tldr = "Audio erkannt: $durationText · $sizeText · ${metadata.format}. Lokale Transkription wurde versucht, ist auf diesem Gerät/Build aber noch nicht verfügbar.",
                keyPoints = listOf(
                    "Audio-Ingest und Guardrails funktionieren.",
                    "Transkriptionsstatus: ${transcription?.status ?: TranscriptionStatus.SKIPPED}.",
                    "Nächster Schritt: robuste Whisper-Integration statt Geräte-Recognizer-Fallback."
                ),
                tags = listOf("share-target", "offline-first", "audio-ingest", "transcription-spike", metadata.format.name.lowercase()),
                category = "Local Transcription Spike / Fallback",
                replySuggestions = listOf(
                    ReplySuggestion("kurz", "Hab's gesehen — tl;dh hat die Audio lokal geprüft."),
                    ReplySuggestion("freundlich", "Danke dir, die Sprachnachricht kam sauber in tl;dh an. Die lokale Transkription wird gerade gehärtet."),
                    ReplySuggestion("direkt", "Audio ist angekommen; Transkription ist im aktuellen Spike noch nicht verlässlich verfügbar.")
                ),
                warnings = warnings,
                transcript = null,
                transcriptionStatus = transcriptionStatus,
                technicalDetails = technicalDetails
            )
        }
    }

    private fun String.toConciseTldr(): String {
        val clean = replace(Regex("\\s+"), " ").trim()
        return if (clean.length <= 260) clean else clean.take(257).trimEnd() + "…"
    }

    private fun String.replySuggestionsFromTranscript(): List<ReplySuggestion> {
        val clean = replace(Regex("\\s+"), " ").trim()
        val snippet = if (clean.length <= 120) clean else clean.take(117).trimEnd() + "…"
        return listOf(
            ReplySuggestion("kurz", "Hab's gelesen — ich melde mich dazu."),
            ReplySuggestion("freundlich", "Danke dir, ich hab die Sprachnachricht jetzt als Text vorliegen und schaue drauf."),
            ReplySuggestion("direkt", "Ich habe den Inhalt erfasst: $snippet")
        )
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
