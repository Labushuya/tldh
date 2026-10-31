package dev.bitsbots.tldh.ui

import java.util.Locale

/** Where the audio came from: shared into tl;dh (WhatsApp) or picked with "Audiodatei öffnen". */
enum class AudioOrigin { SHARE, FILE }

/** A marked word inside a piece of text, as character offsets [start, end). */
data class MarkSpan(val start: Int, val end: Int, val strong: Boolean)

/** Pure text formatting for the screens, kept free of Android types so it is unit-testable. */
object UiText {
    private val whatsAppName = Regex("""^(PTT|AUD)-\d{8}-WA\d+""", RegexOption.IGNORE_CASE)

    fun title(origin: AudioOrigin): String = when (origin) {
        AudioOrigin.SHARE -> "Sprachnachricht"
        AudioOrigin.FILE -> "Audiodatei"
    }

    fun sourceLabel(origin: AudioOrigin, displayName: String?): String {
        val name = displayName?.trim().orEmpty()
        if (whatsAppName.containsMatchIn(name) || name.contains("whatsapp", ignoreCase = true)) return "WhatsApp"
        if (origin == AudioOrigin.SHARE || name.isEmpty()) return if (origin == AudioOrigin.SHARE) "Geteilt" else "Datei"
        val base = name.substringBeforeLast('.', name)
        return if (base.length > 26) base.take(25).trimEnd() + "…" else base
    }

    fun subtitle(origin: AudioOrigin, displayName: String?, durationMs: Long?): String =
        listOfNotNull(sourceLabel(origin, displayName), duration(durationMs)).joinToString(" · ")

    /** "1:42 min", or null when the duration is unknown. */
    fun duration(ms: Long?): String? {
        if (ms == null || ms <= 0L) return null
        val seconds = (ms + 500L) / 1000L
        return "%d:%02d min".format(Locale.GERMANY, seconds / 60, seconds % 60)
    }

    /** "0:09" for snippet buttons and timestamps. */
    fun timestamp(ms: Long): String {
        val seconds = (ms.coerceAtLeast(0L)) / 1000L
        return "%d:%02d".format(Locale.GERMANY, seconds / 60, seconds % 60)
    }

    /** "in 21 s transkribiert" / "in 1:05 min transkribiert". */
    fun transcribedIn(ms: Long?): String? {
        if (ms == null || ms <= 0L) return null
        val seconds = (ms + 500L) / 1000L
        return if (seconds < 60) "in $seconds s transkribiert" else "in %d:%02d min transkribiert".format(Locale.GERMANY, seconds / 60, seconds % 60)
    }

    /** Remaining seconds, extrapolated from the share already done; null while too early to say. */
    fun estimateRemainingSeconds(progress: Int, elapsedMs: Long): Int? {
        if (progress < 5 || progress >= 100 || elapsedMs < 1_500L) return null
        val remainingMs = elapsedMs * (100 - progress) / progress
        return ((remainingMs + 999L) / 1000L).toInt()
    }

    fun remainingText(seconds: Int?): String = when {
        seconds == null -> "Läuft komplett auf deinem Handy."
        seconds <= 5 -> "Gleich fertig."
        seconds < 60 -> "Noch etwa ${roundTo5(seconds)} Sekunden."
        seconds < 90 -> "Noch etwa eine Minute."
        else -> "Noch etwa ${(seconds + 30) / 60} Minuten."
    }

    private fun roundTo5(seconds: Int): Int = maxOf(5, ((seconds + 2) / 5) * 5)

    /** The tail of the live transcript, so the newest words stay visible. */
    fun livePreview(text: String, maxChars: Int = 220): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= maxChars) return clean
        val tail = clean.takeLast(maxChars)
        val cut = tail.indexOf(' ').takeIf { it in 0..30 } ?: 0
        return "… " + tail.substring(cut).trimStart()
    }

    /** Offsets of [word] (edge punctuation ignored) as a whole word inside [text], or null. */
    fun findWord(text: String, word: String, from: Int = 0, to: Int = text.length): IntRange? {
        val needle = word.trim().trim { !it.isLetterOrDigit() && it != '€' && it != '%' }
        if (needle.isEmpty()) return null
        var index = text.indexOf(needle, startIndex = from.coerceAtLeast(0))
        while (index >= 0 && index + needle.length <= to) {
            val before = text.getOrNull(index - 1)
            val after = text.getOrNull(index + needle.length)
            val wholeWord = (before == null || !before.isLetterOrDigit()) && (after == null || !after.isLetterOrDigit())
            if (wholeWord) return index until index + needle.length
            index = text.indexOf(needle, startIndex = index + 1)
        }
        return null
    }

    /**
     * Marks uncertain critical words inside the TL;DR. The TL;DR is made of verbatim transcript
     * sentences, so a check's word is marked where its sentence appears in the TL;DR.
     */
    fun marksIn(text: String, checks: List<Triple<String, String, Boolean>>): List<MarkSpan> =
        checks.mapNotNull { (sentence, word, strong) ->
            val sentenceStart = text.indexOf(sentence.trim()).takeIf { it >= 0 } ?: return@mapNotNull null
            findWord(text, word, sentenceStart, sentenceStart + sentence.trim().length)?.let { MarkSpan(it.first, it.last + 1, strong) }
        }.distinctBy { it.start }.sortedBy { it.start }
}
