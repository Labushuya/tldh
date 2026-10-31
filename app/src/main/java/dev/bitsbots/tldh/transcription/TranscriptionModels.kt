package dev.bitsbots.tldh.transcription

import java.io.File

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
    val engine: String = "whisper.cpp",
    val elapsedMs: Long? = null,
    val modelName: String? = null,
    val words: List<TranscriptWord> = emptyList(),
    /** Prepared 16 kHz mono PCM16 of this session, kept until the session wipe for snippet playback. */
    val pcmFile: File? = null,
    val sampleRate: Int = 16_000
) {
    val hasTranscript: Boolean = status == TranscriptionStatus.COMPLETED && !transcript.isNullOrBlank()
    val uncertainWords: List<TranscriptWord> get() = words.filter { it.confidence != ConfidenceLevel.SICHER }
}
