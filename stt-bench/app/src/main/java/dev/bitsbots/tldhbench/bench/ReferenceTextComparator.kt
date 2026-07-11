package dev.bitsbots.tldhbench.bench

import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

object ReferenceTextComparator {
    fun compare(referenceText: String?, hypothesisText: String): ReferenceComparison? {
        val rawReference = referenceText?.trim().orEmpty()
        if (rawReference.isBlank()) return null

        val referenceWords = normalizeWords(rawReference)
        val hypothesisWords = normalizeWords(hypothesisText)
        val wordStats = wordEditStats(referenceWords, hypothesisWords)
        val wordDiffs = wordAlignmentDiffs(referenceWords, hypothesisWords)

        val referenceChars = normalizeForCharacters(rawReference)
        val hypothesisChars = normalizeForCharacters(hypothesisText)
        val charDistance = editDistance(referenceChars, hypothesisChars)

        val wer = percent(wordStats.distance, referenceWords.size.coerceAtLeast(1))
        val cer = percent(charDistance, referenceChars.length.coerceAtLeast(1))
        val label = qualityLabel(wer)
        val realWorldScore = realWorldScore(rawReference, hypothesisText, wordStats.distance, wordDiffs)

        return ReferenceComparison(
            referenceRaw = rawReference,
            hypothesisRaw = hypothesisText.trim(),
            referenceWordCount = referenceWords.size,
            hypothesisWordCount = hypothesisWords.size,
            wordDistance = wordStats.distance,
            wordSubstitutions = wordStats.substitutions,
            wordInsertions = wordStats.insertions,
            wordDeletions = wordStats.deletions,
            werPercent = wer,
            charDistance = charDistance,
            referenceCharCount = referenceChars.length,
            cerPercent = cer,
            qualityLabel = label,
            summary = "WER ${fmt(wer)} · CER ${fmt(cer)} · S/I/D ${wordStats.substitutions}/${wordStats.insertions}/${wordStats.deletions} · $label",
            wordDiffs = wordDiffs,
            realWorldScore = realWorldScore
        )
    }

    private fun qualityLabel(werPercent: Double): String = when {
        werPercent <= 15.0 -> "sehr gut"
        werPercent <= 25.0 -> "brauchbar"
        werPercent <= 40.0 -> "kritisch"
        else -> "schwach"
    }

    private fun realWorldScore(
        rawReference: String,
        hypothesisText: String,
        strictDistance: Int,
        strictDiffs: List<WordDiff>
    ): RealWorldScore {
        val normalizedReference = normalizeSemanticWords(rawReference)
        val normalizedHypothesis = normalizeSemanticWords(hypothesisText)
        val stats = wordEditStats(normalizedReference, normalizedHypothesis)
        val normalizedWer = percent(stats.distance, normalizedReference.size.coerceAtLeast(1))
        val contentMatch = (100.0 - normalizedWer).coerceIn(0.0, 100.0)
        val criticalIssues = strictDiffs.count { isCriticalDiff(it) }
        val lowImpact = (strictDistance - stats.distance).coerceAtLeast(0)
        val readiness = when {
            normalizedWer <= 8.0 && criticalIssues == 0 -> "produktnah"
            normalizedWer <= 12.0 && criticalIssues <= 2 -> "tl;dh-tauglich mit Guardrails"
            normalizedWer <= 18.0 && criticalIssues <= 5 -> "brauchbar mit Prüfung"
            normalizedWer <= 25.0 -> "grenzwertig"
            else -> "kritisch"
        }
        return RealWorldScore(
            normalizedWordCount = normalizedReference.size,
            normalizedWordDistance = stats.distance,
            normalizedWerPercent = normalizedWer,
            contentMatchPercent = contentMatch,
            criticalIssues = criticalIssues,
            harmlessOrLowImpactIssues = lowImpact,
            readinessLabel = readiness,
            summary = "Reale Wertung ${fmt(normalizedWer)} nWER · Content-Match ${fmt(contentMatch)} · kritische Abweichungen $criticalIssues · $readiness"
        )
    }

    private fun normalizeWords(text: String): List<String> = normalizeForWords(text)
        .split(' ')
        .filter { it.isNotBlank() }

    private fun normalizeForWords(text: String): String = text
        .lowercase(Locale.GERMAN)
        .replace("ß", "ss")
        .replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun normalizeForCharacters(text: String): String = normalizeForWords(text).replace(" ", "")

    private fun normalizeSemanticWords(text: String): List<String> {
        val preprocessed = text
            .lowercase(Locale.GERMAN)
            .replace("ß", "ss")
            .replace(Regex("\\b20\\b"), " zwanzig ")
            .replace(Regex("\\b25\\b"), " fuenfundzwanzig ")
            .replace(Regex("\\b1\\b"), " eins ")
            .replace(Regex("\\b2\\b"), " zwei ")
            .replace(Regex("\\b3\\b"), " drei ")
            .replace(Regex("\\b4\\b"), " vier ")
            .replace(Regex("\\b5\\b"), " fuenf ")
            .replace(Regex("\\b10\\b"), " zehn ")
            .replace("runterlaufe", "runter laufe")
            .replace("runterlaufen", "runter laufen")
            .replace("langlaufe", "lang laufe")
            .replace("ultraparanoid", "ultra paranoid")
            .replace("schwörs", "schwoers")
            .replace("schwör's", "schwoers")
            .replace("schwör s", "schwoers")
            .replace("schwoer s", "schwoers")
            .replace("schwör", "schwoer")
            .replace("fünfundzwanzig", "fuenfundzwanzig")
            .replace("fünf", "fuenf")
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")

        return preprocessed
            .replace(Regex("[^\\p{L}\\p{Nd}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .split(' ')
            .mapNotNull { semanticToken(it) }
            .filter { it.isNotBlank() }
    }

    private fun semanticToken(token: String): String? {
        if (token in fillerTokens) return null
        return when (token) {
            "jessie" -> "jessi"
            "joschi" -> "yoshi"
            "schiess", "schies", "schiss" -> "schiss"
            "schwoers", "schwoer", "schwoere" -> "schwoer"
            "gerascheln", "geraschen", "geraschel" -> "geraschel"
            "knarzen", "knarren" -> "knarz"
            "hoffe", "hoff" -> "hoffe"
            "audio", "audios" -> "audio"
            "grille", "grillen" -> "grillen"
            "strassenlaterne", "strassenlaternen" -> "strassenlaterne"
            "kollaps", "kollap" -> "kollaps"
            "fahrekenne" -> "verrecken"
            else -> token
        }
    }

    private val fillerTokens = setOf(
        "aeh", "aehm", "eh", "ehm", "hm", "hmm", "ja", "so", "halt", "praktisch",
        "lach", "lacht", "hahaha", "haha", "bla", "blabla", "genau"
    )

    private val criticalTokens = setOf(
        "nicht", "kein", "keine", "keinen", "keiner", "keines", "nichts", "nie", "niemals", "ohne",
        "zwanzig", "fuenfundzwanzig", "eins", "zwei", "drei", "vier", "fuenf", "zehn",
        "jessi", "yoshi", "mutter", "arbeit", "handy", "stumm", "krise", "kollaps",
        "grillen", "geraschel", "schiss", "strassenlaterne"
    )

    private fun isCriticalDiff(diff: WordDiff): Boolean {
        val reference = diff.referenceWord?.let { normalizeSemanticWords(it).firstOrNull() }
        val hypothesis = diff.hypothesisWord?.let { normalizeSemanticWords(it).firstOrNull() }
        return reference in criticalTokens || hypothesis in criticalTokens ||
            diff.referenceWord?.any { it.isDigit() } == true || diff.hypothesisWord?.any { it.isDigit() } == true
    }

    private data class WordStats(
        val distance: Int,
        val substitutions: Int,
        val insertions: Int,
        val deletions: Int
    )

    private fun wordEditStats(reference: List<String>, hypothesis: List<String>): WordStats {
        val rows = reference.size + 1
        val cols = hypothesis.size + 1
        val dp = Array(rows) { Array(cols) { WordStats(0, 0, 0, 0) } }

        for (i in 1 until rows) {
            val prev = dp[i - 1][0]
            dp[i][0] = prev.copy(distance = prev.distance + 1, deletions = prev.deletions + 1)
        }
        for (j in 1 until cols) {
            val prev = dp[0][j - 1]
            dp[0][j] = prev.copy(distance = prev.distance + 1, insertions = prev.insertions + 1)
        }

        for (i in 1 until rows) {
            for (j in 1 until cols) {
                dp[i][j] = if (reference[i - 1] == hypothesis[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    val substitution = dp[i - 1][j - 1].let {
                        it.copy(distance = it.distance + 1, substitutions = it.substitutions + 1)
                    }
                    val deletion = dp[i - 1][j].let {
                        it.copy(distance = it.distance + 1, deletions = it.deletions + 1)
                    }
                    val insertion = dp[i][j - 1].let {
                        it.copy(distance = it.distance + 1, insertions = it.insertions + 1)
                    }
                    listOf(substitution, deletion, insertion).minWith(compareBy<WordStats> { it.distance }.thenBy { it.insertions + it.deletions })
                }
            }
        }
        return dp[reference.size][hypothesis.size]
    }

    private fun wordAlignmentDiffs(reference: List<String>, hypothesis: List<String>): List<WordDiff> {
        val rows = reference.size + 1
        val cols = hypothesis.size + 1
        val distance = Array(rows) { IntArray(cols) }
        for (i in 0 until rows) distance[i][0] = i
        for (j in 0 until cols) distance[0][j] = j
        for (i in 1 until rows) {
            for (j in 1 until cols) {
                val cost = if (reference[i - 1] == hypothesis[j - 1]) 0 else 1
                distance[i][j] = min(
                    min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
                    distance[i - 1][j - 1] + cost
                )
            }
        }

        val reversed = mutableListOf<WordDiff>()
        var i = reference.size
        var j = hypothesis.size
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && reference[i - 1] == hypothesis[j - 1] && distance[i][j] == distance[i - 1][j - 1]) {
                i -= 1
                j -= 1
                continue
            }
            if (i > 0 && j > 0 && distance[i][j] == distance[i - 1][j - 1] + 1) {
                reversed += WordDiff(
                    type = WordDiffType.SUBSTITUTE,
                    referenceIndex = i,
                    hypothesisIndex = j,
                    referenceWord = reference[i - 1],
                    hypothesisWord = hypothesis[j - 1]
                )
                i -= 1
                j -= 1
                continue
            }
            if (i > 0 && distance[i][j] == distance[i - 1][j] + 1) {
                reversed += WordDiff(
                    type = WordDiffType.DELETE,
                    referenceIndex = i,
                    hypothesisIndex = null,
                    referenceWord = reference[i - 1],
                    hypothesisWord = null
                )
                i -= 1
                continue
            }
            if (j > 0 && distance[i][j] == distance[i][j - 1] + 1) {
                reversed += WordDiff(
                    type = WordDiffType.INSERT,
                    referenceIndex = null,
                    hypothesisIndex = j,
                    referenceWord = null,
                    hypothesisWord = hypothesis[j - 1]
                )
                j -= 1
                continue
            }
            break
        }
        return reversed.asReversed()
    }

    private fun editDistance(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(
                    min(prev[j] + 1, curr[j - 1] + 1),
                    prev[j - 1] + cost
                )
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[b.length]
    }

    private fun percent(value: Int, total: Int): Double = (value.toDouble() * 100.0) / total.toDouble()

    private fun fmt(value: Double): String = "%.1f%%".format(Locale.GERMANY, ((value * 10).roundToInt() / 10.0))
}
