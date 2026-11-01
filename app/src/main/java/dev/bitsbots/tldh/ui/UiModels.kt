package dev.bitsbots.tldh.ui

import dev.bitsbots.tldh.share.SharedAudio
import dev.bitsbots.tldh.summarization.AudioSummary
import dev.bitsbots.tldh.transcription.TranscriptionOutcome
import dev.bitsbots.tldh.transcription.WhisperModelSpec

/** Audio waiting to be processed, e.g. while the recognition model is being set up. */
data class PendingAudio(val audio: SharedAudio, val origin: AudioOrigin)

enum class ProcessingStage { Prepare, Transcribe, Summarize }

data class ProcessingInfo(
    /** Identifies the run, so late callbacks from a cancelled run are ignored. */
    val token: Int,
    val title: String,
    val subtitle: String,
    val stage: ProcessingStage = ProcessingStage.Prepare,
    val prepareMs: Long? = null,
    val progress: Int = 0,
    val remainingSeconds: Int? = null,
    val levels: List<Float> = emptyList(),
    val liveText: String = "",
    val durationMs: Long? = null
)

sealed interface TldhUiState {
    data object Idle : TldhUiState
    data class NeedsModel(val pending: PendingAudio?) : TldhUiState
    data class Processing(val info: ProcessingInfo) : TldhUiState
    data class Result(
        val summary: AudioSummary,
        val transcription: TranscriptionOutcome,
        val title: String,
        val subtitle: String
    ) : TldhUiState
    data class Error(val title: String, val message: String) : TldhUiState
}

data class ModelDownload(val spec: WhisperModelSpec, val progress: Float)

data class ModelUiState(
    val runtimeAvailable: Boolean,
    val recommended: WhisperModelSpec,
    val installed: List<WhisperModelSpec>,
    val active: WhisperModelSpec?,
    val download: ModelDownload?,
    val error: String?
) {
    val storageBytes: Long get() = installed.sumOf { it.sizeBytes }
}
