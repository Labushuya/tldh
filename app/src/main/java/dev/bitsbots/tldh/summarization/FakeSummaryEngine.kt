package dev.bitsbots.tldh.summarization

import dev.bitsbots.tldh.audio.AudioMetadata

class FakeSummaryEngine {
    fun summarize(metadata: AudioMetadata): AudioSummary {
        val name = metadata.displayName ?: "geteilte Audiodatei"
        val sizeText = metadata.sizeBytes?.let { "${it / 1024} KB" } ?: "unbekannte Größe"
        return AudioSummary(
            tldr = "Share-Target funktioniert: tl;dh hat '$name' lokal entgegengenommen und als ${metadata.format} erkannt. Die echte Transkription wird ab v0.3.0 über whisper.cpp integriert.",
            keyPoints = listOf(
                "Quelle wurde über Android ACTION_SEND empfangen.",
                "MIME-Type: ${metadata.mimeType ?: "unbekannt"}.",
                "Dateigröße: $sizeText.",
                "Aktuell läuft ein Fake-Summarizer, damit der End-to-End-Flow testbar ist."
            ),
            tags = listOf("share-target", "offline-first", metadata.format.name.lowercase()),
            category = "Bootstrap / Smoke Test",
            replySuggestions = listOf(
                ReplySuggestion("kurz", "Hab's gesehen — ich prüfe den Inhalt gleich."),
                ReplySuggestion("freundlich", "Danke dir, ich schaue mir die Sprachnachricht an und melde mich dazu."),
                ReplySuggestion("direkt", "Ich habe die Audio erhalten. Eine echte Zusammenfassung folgt, sobald die Transkription aktiv ist.")
            ),
            warnings = listOf("v0.1.0 transkribiert noch nicht. Diese Ausgabe ist bewusst ein technischer Durchstich.")
        )
    }
}
