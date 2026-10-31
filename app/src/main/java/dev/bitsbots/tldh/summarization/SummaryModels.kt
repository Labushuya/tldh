package dev.bitsbots.tldh.summarization

import dev.bitsbots.tldh.analysis.CriticalFinding

data class AudioSummary(
    val tldr: String,
    val questions: List<String>,
    val datesAndNumbers: List<String>,
    val tasks: List<String>,
    /** Uncertain meaning-critical words the user should check by ear. */
    val checks: List<CriticalFinding>,
    val warnings: List<String>,
    val transcript: String? = null,
    val transcriptionStatus: String? = null,
    val technicalDetails: List<String> = emptyList()
)
