package dev.bitsbots.tldh.summarization

import dev.bitsbots.tldh.analysis.CriticalFinding
import dev.bitsbots.tldh.analysis.ReplySuggestions

data class AudioSummary(
    val tldr: String,
    val questions: List<String>,
    val datesAndNumbers: List<String>,
    val tasks: List<String>,
    /** Uncertain meaning-critical words the user should check by ear. */
    val checks: List<CriticalFinding>,
    val replies: ReplySuggestions?,
    /** Copy-ready note of the voice note's content. */
    val note: String?,
    val warnings: List<String>,
    val transcript: String? = null,
    val transcriptionStatus: String? = null,
    val technicalDetails: List<String> = emptyList()
)
