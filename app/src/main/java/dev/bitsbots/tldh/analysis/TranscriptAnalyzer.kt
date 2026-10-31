package dev.bitsbots.tldh.analysis

import dev.bitsbots.tldh.transcription.TranscriptWord
import java.util.Locale
import kotlin.math.sqrt

data class TranscriptAnalysis(
    val tldr: String,
    val questions: List<String>,
    val datesAndNumbers: List<String>,
    val tasks: List<String>,
    /** Uncertain meaning-critical words, most uncertain first. */
    val checks: List<CriticalFinding>
)

/**
 * Rule-based summary of a voice-note transcript. Everything it shows is taken verbatim from the
 * transcript: the TL;DR picks the most informative sentences, the key points pull out questions,
 * dates/numbers and requests. Nothing is generated, so nothing can be invented.
 */
object TranscriptAnalyzer {
    private const val TLDR_MAX_CHARS = 260
    private const val TLDR_MAX_SENTENCES = 2

    private val requestPhrases = listOf(
        "bitte", "kannst du", "könntest du", "kann ich dich", "würdest du", "magst du", "denk dran",
        "denkst du dran", "vergiss nicht", "nicht vergessen", "sag mir", "sag bescheid", "gib mir bescheid",
        "gib bescheid", "meld dich", "melde dich", "ruf mich", "ruf an", "schick mir", "schreib mir",
        "musst du", "du musst", "wir müssen", "bring mal", "bringst du"
    )
    private val greetingStarts = listOf(
        "hey", "hi", "hallo", "moin", "servus", "na ", "huhu", "guten morgen", "guten abend", "liebe grüße",
        "lg", "bis dann", "bis später", "bis morgen", "tschüss", "ciao", "hab dich lieb", "danke dir", "okay", "ok "
    )
    private val stopwords = setOf(
        "aber", "alle", "alles", "also", "auch", "dann", "dass", "denn", "dein", "deine", "diese", "dieser",
        "dieses", "doch", "eben", "eigentlich", "einen", "einem", "einer", "eine", "einfach", "etwas", "gerade",
        "haben", "habe", "hast", "hatte", "halt", "hier", "immer", "irgendwie", "jetzt", "kein", "keine",
        "machen", "mache", "mein", "meine", "mich", "mir", "noch", "oder", "schon", "sein", "seine", "sich",
        "sind", "sowieso", "über", "unsere", "viel", "vielleicht", "wäre", "war", "waren", "weil", "wenn",
        "werde", "werden", "wieder", "wird", "wirklich", "wollte", "würde", "ähm", "äh", "naja", "genau",
        "okay", "dich", "dir", "euch", "uns", "nicht", "mal", "ganz", "sehr", "nach", "beim", "vom", "zum", "zur"
    )
    private val phraseLeft = setOf(
        "am", "um", "ab", "bis", "gegen", "nach", "vor", "seit", "im", "in", "zum", "zur", "den", "dem",
        "diesen", "diesem", "nächsten", "nächste", "kommenden", "ca", "etwa", "so", "spätestens"
    )
    private val phraseRight = setOf(
        "uhr", "euro", "€", "cent", "prozent", "%", "minuten", "minute", "stunden", "stunde", "tage", "tag",
        "wochen", "woche", "monate", "monat", "jahre", "jahr", "mal", "abend", "abends", "früh", "morgen", "mittag"
    )

    fun analyze(words: List<TranscriptWord>): TranscriptAnalysis {
        if (words.isEmpty()) return TranscriptAnalysis("", emptyList(), emptyList(), emptyList(), emptyList())
        val sentences = SentenceSplitter.split(words)
        val findings = CriticalWordDetector.findAll(words, sentences)

        val questions = mutableListOf<String>()
        val tasks = mutableListOf<String>()
        sentences.forEach { sentence ->
            val text = sentence.text.trim()
            when {
                isGreeting(text, sentence.lastWord - sentence.firstWord + 1) -> Unit
                isRequest(text) -> tasks += text
                text.endsWith('?') -> questions += text
            }
        }

        return TranscriptAnalysis(
            tldr = tldr(sentences, words, findings),
            questions = questions.distinct().take(3),
            datesAndNumbers = dateAndNumberPhrases(words, findings).take(5),
            tasks = tasks.distinct().take(3),
            checks = CriticalWordDetector.uncertain(findings)
        )
    }

    /** Fallback when only plain text is available (no word timestamps or probabilities). */
    fun analyze(transcript: String): TranscriptAnalysis =
        analyze(transcript.split(Regex("\\s+")).filter { it.isNotBlank() }.map { TranscriptWord(it, null, null, 1f) })

    private fun isRequest(sentence: String): Boolean {
        val normalized = " " + sentence.lowercase(Locale.GERMAN).replace(Regex("[^\\p{L}\\p{N} ]"), " ") + " "
        return requestPhrases.any { " $it " in normalized }
    }

    private fun isGreeting(sentence: String, wordCount: Int): Boolean {
        val normalized = sentence.lowercase(Locale.GERMAN).trim() + " "
        return wordCount <= 6 && greetingStarts.any { normalized.startsWith(it) }
    }

    private fun tldr(sentences: List<Sentence>, words: List<TranscriptWord>, findings: List<CriticalFinding>): String {
        val contentFrequency = words
            .map { Tokens.normalize(it.text) }
            .filter { it.length >= 4 && it !in stopwords }
            .groupingBy { it }
            .eachCount()
        val criticalBySentence = findings.groupBy { finding -> sentences.indexOfFirst { finding.wordIndex in it.firstWord..it.lastWord } }

        val scored = sentences.mapIndexed { index, sentence ->
            val wordCount = sentence.lastWord - sentence.firstWord + 1
            val tokens = (sentence.firstWord..sentence.lastWord).map { Tokens.normalize(words[it].text) }
            val keywordScore = tokens.sumOf { (contentFrequency[it] ?: 0).takeIf { count -> count >= 2 } ?: 0 } / sqrt(wordCount.toDouble())
            val categories = criticalBySentence[index].orEmpty().map { it.category }.toSet()
            var score = keywordScore
            if (sentence.text.trimEnd().endsWith('?')) score += 2.0
            if (isRequest(sentence.text)) score += 2.0
            if (CriticalCategory.ZEIT in categories) score += 1.5
            if (CriticalCategory.ZAHL in categories) score += 1.0
            if (CriticalCategory.VERNEINUNG in categories) score += 0.5
            if (isGreeting(sentence.text, wordCount)) score -= 3.0
            if (wordCount < 4) score -= 1.0
            index to score
        }

        val chosen = mutableListOf<Int>()
        var length = 0
        for ((index, _) in scored.sortedByDescending { it.second }) {
            val sentenceLength = sentences[index].text.length
            if (chosen.isNotEmpty() && (chosen.size >= TLDR_MAX_SENTENCES || length + sentenceLength + 1 > TLDR_MAX_CHARS)) continue
            chosen += index
            length += sentenceLength + 1
        }
        val text = chosen.sorted().joinToString(" ") { sentences[it].text.trim() }
        return if (text.length <= TLDR_MAX_CHARS) text else text.take(TLDR_MAX_CHARS - 1).trimEnd() + "…"
    }

    /** Grows each time/number word into a short phrase such as "am Samstag um 17 Uhr" or "20 Euro". */
    private fun dateAndNumberPhrases(words: List<TranscriptWord>, findings: List<CriticalFinding>): List<String> {
        val anchors = findings.filter { it.category == CriticalCategory.ZEIT || it.category == CriticalCategory.ZAHL }.map { it.wordIndex }.toSet()
        fun token(i: Int) = Tokens.normalize(words[i].text)
        val phrases = mutableListOf<String>()
        var previousEnd = -1
        var index = 0
        while (index < words.size) {
            if (index !in anchors) {
                index++
                continue
            }
            var start = index
            while (start - 1 > previousEnd && !endsSentence(words[start - 1].text) && token(start - 1) in phraseLeft) start--
            var end = index
            while (end < words.lastIndex && !endsSentence(words[end].text)) {
                val next = end + 1
                end = when {
                    next in anchors || token(next) in phraseRight -> next
                    // Bridge "Samstag um 17": a preposition followed by another time/number word.
                    token(next) in phraseLeft && next + 1 in anchors && !endsSentence(words[next].text) -> next + 1
                    else -> break
                }
            }
            phrases += (start..end).joinToString(" ") { words[it].text }.trimEnd(',', '.', '!', '?', ';', ':')
            previousEnd = end
            index = end + 1
        }
        val unique = phrases.distinct()
        // Drop a phrase that is already part of a longer one ("Samstag" next to "am Samstag um 17 Uhr").
        return unique.filter { phrase ->
            unique.none { other -> other != phrase && " ${other.lowercase(Locale.GERMAN)} ".contains(" ${phrase.lowercase(Locale.GERMAN)} ") }
        }
    }

    private fun endsSentence(word: String): Boolean = word.endsWith('.') || word.endsWith('!') || word.endsWith('?')
}
