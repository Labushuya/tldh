package dev.bitsbots.tldh.summarization

import dev.bitsbots.tldh.audio.AudioIngestPolicy
import dev.bitsbots.tldh.audio.AudioMetadata

class FakeSummaryEngine {
    fun summarize(metadata: AudioMetadata): AudioSummary {
        val name = metadata.displayName ?: "geteilte Audiodatei"
        val sizeText = metadata.sizeBytes?.let { humanBytes(it) } ?: "unbekannte Größe"
        val durationText = AudioIngestPolicy.formatDuration(metadata.durationMs)
        val warnings = buildList {
            add("v0.2.5 transkribiert noch nicht. Diese Ausgabe ist ein technischer Audio-Ingest- und Duration-Gate-Durchstich vor der lokalen Transkription.")
            addAll(metadata.validation.warnings)
        }

        return AudioSummary(
            tldr = "Audio-Ingest funktioniert: tl;dh hat '$name' lokal entgegengenommen, als ${metadata.format} erkannt, die Dauer als $durationText gelesen und gegen die MVP-Policy geprüft. Die echte Transkription folgt im nächsten Whisper-Spike.",
            keyPoints = buildList {
                add("Quelle wurde über Android ACTION_SEND empfangen.")
                add("MIME-Type: ${metadata.mimeType ?: "unbekannt"}; Extension: ${metadata.extension ?: "unbekannt"}.")
                add("Dateigröße: $sizeText; Audiodauer: $durationText; Header-Probe: ${metadata.headerProbeBytes} Bytes.")
                add("MVP-Limit: ${humanBytes(AudioIngestPolicy.MAX_AUDIO_BYTES)}; Soft-Dauerlimit: ${AudioIngestPolicy.formatDuration(AudioIngestPolicy.SOFT_DURATION_LIMIT_MS)}; Hard-Dauerlimit: ${AudioIngestPolicy.formatDuration(AudioIngestPolicy.HARD_DURATION_LIMIT_MS)}.")
                if (metadata.validation.warnings.isNotEmpty()) {
                    add("Warnung/Guardrail aktiv: ${metadata.validation.warnings.joinToString(" ")}")
                }
                add("Format akzeptiert: ${metadata.validation.accepted}; lokale Verarbeitung bleibt ohne Cloud und ohne Telemetrie.")
                add("Aktuell läuft noch ein Fake-Summarizer, aber die Dauer-/Policy-Grenzen verhindern zu optimistische Transkriptionsläufe auf dem Gerät.")
            },
            tags = listOf("share-target", "offline-first", "audio-ingest", "duration-gate", metadata.format.name.lowercase()),
            category = "Audio Ingest / Duration Guardrail",
            replySuggestions = listOf(
                ReplySuggestion("kurz", "Hab's gesehen — die Audio wurde von tl;dh lokal erkannt und geprüft."),
                ReplySuggestion("freundlich", "Danke dir, die Sprachnachricht kam sauber in tl;dh an. Die lokale Transkription ist der nächste Schritt."),
                ReplySuggestion("direkt", "Die Audio ist angekommen, formatseitig geprüft und innerhalb der lokalen Verarbeitungsgrenzen.")
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
