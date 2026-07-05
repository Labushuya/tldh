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

        val referenceChars = normalizeForCharacters(rawReference)
        val hypothesisChars = normalizeForCharacters(hypothesisText)
        val charDistance = editDistance(referenceChars, hypothesisChars)

        val wer = percent(wordStats.distance, referenceWords.size.coerceAtLeast(1))
        val cer = percent(charDistance, referenceChars.length.coerceAtLeast(1))
        val label = qualityLabel(wer)

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
            summary = "WER ${fmt(wer)} · CER ${fmt(cer)} · S/I/D ${wordStats.substitutions}/${wordStats.insertions}/${wordStats.deletions} · $label"
        )
    }

    private fun qualityLabel(werPercent: Double): String = when {
        werPercent <= 15.0 -> "sehr gut"
        werPercent <= 25.0 -> "brauchbar"
        werPercent <= 40.0 -> "kritisch"
        else -> "schwach"
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
