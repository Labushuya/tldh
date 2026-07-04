package dev.bitsbots.tldh.audio

object AudioIngestPolicy {
    const val MAX_HEADER_PROBE_BYTES: Int = 32 * 1024
    const val MAX_AUDIO_BYTES: Long = 50L * 1024L * 1024L
    const val MIN_NON_EMPTY_BYTES: Long = 1L

    fun validate(metadata: AudioMetadata): AudioValidation {
        val reasons = buildList {
            val size = metadata.sizeBytes
            if (size != null) {
                if (size < MIN_NON_EMPTY_BYTES) add(AudioRejectReason.EMPTY_FILE)
                if (size > MAX_AUDIO_BYTES) add(AudioRejectReason.FILE_TOO_LARGE)
            }
            if (!metadata.format.isSupportedForMvp) add(AudioRejectReason.UNSUPPORTED_FORMAT)
            if (metadata.headerBytes.isEmpty()) add(AudioRejectReason.EMPTY_HEADER_PROBE)
        }

        val warnings = buildList {
            if (metadata.sizeBytes == null) add("Dateigröße konnte vom Content Provider nicht bestimmt werden.")
            if (metadata.displayName == null) add("Dateiname konnte vom Content Provider nicht bestimmt werden.")
            if (metadata.mimeType == null) add("MIME-Type konnte vom Content Provider nicht bestimmt werden.")
            if (metadata.format == AudioFormat.OGG_UNKNOWN) add("OGG erkannt, aber Opus-Header noch nicht sicher bestätigt.")
        }

        return AudioValidation(
            accepted = reasons.isEmpty(),
            rejectReasons = reasons,
            warnings = warnings
        )
    }
}

data class AudioValidation(
    val accepted: Boolean,
    val rejectReasons: List<AudioRejectReason>,
    val warnings: List<String>
)

enum class AudioRejectReason(val userMessage: String) {
    EMPTY_FILE("Die geteilte Audiodatei ist leer."),
    FILE_TOO_LARGE("Die Audiodatei überschreitet das aktuelle MVP-Limit von 50 MB."),
    UNSUPPORTED_FORMAT("Das Audioformat wird im MVP noch nicht unterstützt."),
    EMPTY_HEADER_PROBE("Die Datei konnte nicht ausreichend gelesen werden, um das Format zu prüfen.")
}

class AudioIngestException(
    val metadata: AudioMetadata?,
    val reasons: List<AudioRejectReason>
) : IllegalArgumentException(reasons.joinToString(" ") { it.userMessage })
