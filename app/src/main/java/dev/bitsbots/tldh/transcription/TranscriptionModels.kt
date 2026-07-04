package dev.bitsbots.tldh.transcription

enum class TranscriptionStatus {
    COMPLETED,
    SKIPPED,
    UNAVAILABLE,
    FAILED
}

data class TranscriptionOutcome(
    val status: TranscriptionStatus,
    val transcript: String? = null,
    val message: String,
    val engine: String = "Android on-device file recognizer",
    val elapsedMs: Long? = null
) {
    val hasTranscript: Boolean = status == TranscriptionStatus.COMPLETED && !transcript.isNullOrBlank()
}
