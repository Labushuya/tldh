package dev.bitsbots.tldh.summarization

import dev.bitsbots.tldh.audio.AudioIngestPolicy
import dev.bitsbots.tldh.audio.AudioMetadata

class FakeSummaryEngine {
    fun summarize(metadata: AudioMetadata): AudioSummary {
        val name = metadata.displayName ?: "geteilte Audiodatei"
        val sizeText = metadata.sizeBytes?.let { humanBytes(it) } ?: "unbekannte Größe"
        val warnings = buildList {
            add("v0.2.0 transkribiert noch nicht. Diese Ausgabe ist bewusst ein technischer Audio-Ingest-Durchstich.")
            addAll(metadata.validation.warnings)
        }

        return AudioSummary(
            tldr = "Audio-Ingest funktioniert: tl;dh hat '$name' lokal entgegengenommen, als ${metadata.format} erkannt und gegen die MVP-Policy geprüft. Die echte Transkription wird ab v0.3.0 über whisper.cpp integriert.",
            keyPoints = listOf(
                "Quelle wurde über Android ACTION_SEND empfangen.",
                "MIME-Type: ${metadata.mimeType ?: "unbekannt"}; Extension: ${metadata.extension ?: "unbekannt"}.",
                "Dateigröße: $sizeText; Header-Probe: ${metadata.headerProbeBytes} Bytes.",
                "MVP-Limit: ${humanBytes(AudioIngestPolicy.MAX_AUDIO_BYTES)}; Format akzeptiert: ${metadata.validation.accepted}.",
                "Aktuell läuft ein Fake-Summarizer, damit Share, Ingest, Policy und UI testbar sind."
            ),
            tags = listOf("share-target", "offline-first", "audio-ingest", metadata.format.name.lowercase()),
            category = "Audio Ingest / Smoke Test",
            replySuggestions = listOf(
                ReplySuggestion("kurz", "Hab's gesehen — die Audio wurde von tl;dh lokal erkannt."),
                ReplySuggestion("freundlich", "Danke dir, die Sprachnachricht kam sauber in tl;dh an. Die echte Zusammenfassung folgt mit der Transkription."),
                ReplySuggestion("direkt", "Die Audio ist angekommen und formatseitig geprüft. Inhaltliche Auswertung kommt ab der lokalen Transkription.")
            ),
            warnings = warnings
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
