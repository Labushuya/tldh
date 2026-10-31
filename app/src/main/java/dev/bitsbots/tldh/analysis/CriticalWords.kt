package dev.bitsbots.tldh.analysis

import dev.bitsbots.tldh.transcription.ConfidenceLevel
import dev.bitsbots.tldh.transcription.TranscriptWord
import java.util.Locale

/** Word classes whose misrecognition can change or invert what a voice note says. */
enum class CriticalCategory(val label: String) {
    VERNEINUNG("Verneinung"),
    ZEIT("Zeit/Datum"),
    ZAHL("Zahl/Betrag"),
    BEDINGUNG("Bedingung"),
    MODAL("Muss/Kann")
}

data class CriticalFinding(
    val wordIndex: Int,
    val word: TranscriptWord,
    val category: CriticalCategory,
    /** The sentence the word appears in, as recognized. */
    val sentence: String
)

data class Sentence(val text: String, val firstWord: Int, val lastWord: Int)

internal object Tokens {
    private val edgePunctuation = Regex("""^[^\p{L}\p{N}€%]+|[^\p{L}\p{N}€%]+$""")

    /** Lowercases and strips surrounding punctuation, keeping inner separators like in "17:30". */
    fun normalize(word: String): String = word.lowercase(Locale.GERMAN).replace(edgePunctuation, "")
}

object SentenceSplitter {
    private val abbreviations = setOf("z.b.", "ca.", "dr.", "bzw.", "usw.", "evtl.", "ggf.", "u.a.", "etc.", "nr.", "str.", "d.h.")
    private val ordinal = Regex("""^\d{1,2}\.$""")

    fun split(words: List<TranscriptWord>): List<Sentence> {
        val sentences = mutableListOf<Sentence>()
        var start = 0
        words.forEachIndexed { index, word ->
            val text = word.text.trimEnd('"', '\'', '“', '”', '»', '«', ')')
            val endsSentence = text.endsWith('.') || text.endsWith('!') || text.endsWith('?')
            val isAbbreviation = text.lowercase(Locale.GERMAN) in abbreviations || ordinal.matches(text)
            if ((endsSentence && !isAbbreviation) || index == words.lastIndex) {
                sentences += Sentence(words.subList(start, index + 1).joinToString(" ") { it.text }, start, index)
                start = index + 1
            }
        }
        return sentences
    }
}

/**
 * Rule-based detection of meaning-critical words. German capitalizes every noun, so names cannot be
 * told apart by capitalization alone; they are left to a later version with a proper name list.
 */
object CriticalWordDetector {
    private val negations = setOf(
        "nicht", "kein", "keine", "keinen", "keinem", "keiner", "keines", "nie", "niemals", "nichts",
        "niemand", "nirgends", "nirgendwo", "ohne", "weder", "nein", "nö", "nee"
    )
    private val times = setOf(
        "heute", "morgen", "übermorgen", "gestern", "vorgestern", "jetzt", "gleich", "sofort", "bald",
        "später", "demnächst", "nachher", "vorhin", "früh", "frühs", "morgens", "vormittag", "vormittags",
        "mittag", "mittags", "nachmittag", "nachmittags", "abend", "abends", "nacht", "nachts", "uhr",
        "halb", "viertel", "montag", "dienstag", "mittwoch", "donnerstag", "freitag", "samstag", "sonnabend",
        "sonntag", "montags", "dienstags", "mittwochs", "donnerstags", "freitags", "samstags", "sonntags",
        "wochenende", "woche", "januar", "februar", "märz", "april", "mai", "juni", "juli", "august",
        "september", "oktober", "november", "dezember", "nächste", "nächsten", "nächstes", "kommende",
        "kommenden", "spätestens", "frühestens", "pünktlich"
    )
    private val numberWords = setOf(
        "null", "eins", "zwei", "drei", "vier", "fünf", "sechs", "sieben", "acht", "neun", "zehn", "elf",
        "zwölf", "dreizehn", "vierzehn", "fünfzehn", "sechzehn", "siebzehn", "achtzehn", "neunzehn",
        "hundert", "tausend", "million", "millionen", "euro", "€", "cent", "prozent", "%", "doppelt", "hälfte"
    )
    private val tens = Regex("""^((ein|zwei|drei|vier|fünf|sechs|sieben|acht|neun)und)?(zwanzig|dreißig|vierzig|fünfzig|sechzig|siebzig|achtzig|neunzig)$""")
    private val largeNumber = Regex("""^\p{L}*(hundert|tausend)\p{L}*$""")
    private val conditions = setOf("wenn", "falls", "außer", "sofern", "solange", "nur", "erst", "unbedingt")
    private val modals = setOf(
        "muss", "musst", "müssen", "müsst", "müsste", "müssten", "darf", "darfst", "dürfen", "dürfte",
        "kann", "kannst", "können", "könnt", "könnte", "könntest", "soll", "sollst", "sollen", "sollte",
        "solltest", "will", "willst", "wollen"
    )

    fun categoryOf(word: String): CriticalCategory? {
        val token = Tokens.normalize(word)
        if (token.isEmpty()) return null
        return when {
            token in negations -> CriticalCategory.VERNEINUNG
            token in times || token.contains(':') && token.any { it.isDigit() } -> CriticalCategory.ZEIT
            token.any { it.isDigit() } || token in numberWords || tens.matches(token) || largeNumber.matches(token) -> CriticalCategory.ZAHL
            token in conditions -> CriticalCategory.BEDINGUNG
            token in modals -> CriticalCategory.MODAL
            else -> null
        }
    }

    /** All critical words, in transcript order. */
    fun findAll(words: List<TranscriptWord>, sentences: List<Sentence>): List<CriticalFinding> {
        val sentenceOf = IntArray(words.size)
        sentences.forEachIndexed { s, sentence -> for (i in sentence.firstWord..sentence.lastWord) sentenceOf[i] = s }
        return words.mapIndexedNotNull { index, word ->
            categoryOf(word.text)?.let { category ->
                CriticalFinding(index, word, category, sentences.getOrNull(sentenceOf[index])?.text ?: word.text)
            }
        }
    }

    /** Critical words recognized with low confidence: the ones the user should check by ear. */
    fun uncertain(findings: List<CriticalFinding>): List<CriticalFinding> =
        findings
            .filter { it.word.confidence != ConfidenceLevel.SICHER }
            .sortedWith(compareBy<CriticalFinding> { it.word.confidence != ConfidenceLevel.SEHR_UNSICHER }.thenBy { it.wordIndex })
}
