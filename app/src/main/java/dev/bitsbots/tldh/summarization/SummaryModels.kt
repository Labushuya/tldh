package dev.bitsbots.tldh.summarization

data class ReplySuggestion(
    val tone: String,
    val text: String
)

data class AudioSummary(
    val tldr: String,
    val keyPoints: List<String>,
    val tags: List<String>,
    val category: String?,
    val replySuggestions: List<ReplySuggestion>,
    val warnings: List<String>
)
