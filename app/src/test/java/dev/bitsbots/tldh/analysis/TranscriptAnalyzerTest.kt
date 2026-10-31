package dev.bitsbots.tldh.analysis

import dev.bitsbots.tldh.transcription.TranscriptWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptAnalyzerTest {
    /** Builds words with probability 0.95, except those listed in [uncertain]. */
    private fun words(text: String, uncertain: Map<String, Float> = emptyMap()): List<TranscriptWord> =
        text.split(" ").mapIndexed { i, w -> TranscriptWord(w, i * 300L, i * 300L + 280L, uncertain[w] ?: 0.95f) }

    private val voiceNote =
        "Hey du, na? Ich wollte nur kurz fragen wegen Samstag. Hast du am Samstag um 17 Uhr Zeit? " +
            "Kannst du bitte die Getränke mitbringen, so für 20 Euro? Ich komme morgen nicht zum Training. Liebe Grüße!"

    @Test fun splitsSentencesButKeepsOrdinalDates() {
        val sentences = SentenceSplitter.split(words("Wir treffen uns am 3. Mai. Passt das?"))
        assertEquals(listOf("Wir treffen uns am 3. Mai.", "Passt das?"), sentences.map { it.text })
    }

    @Test fun categorizesCriticalWords() {
        assertEquals(CriticalCategory.VERNEINUNG, CriticalWordDetector.categoryOf("nicht,"))
        assertEquals(CriticalCategory.VERNEINUNG, CriticalWordDetector.categoryOf("Keine"))
        assertEquals(CriticalCategory.ZEIT, CriticalWordDetector.categoryOf("Samstag?"))
        assertEquals(CriticalCategory.ZEIT, CriticalWordDetector.categoryOf("17:30"))
        assertEquals(CriticalCategory.ZAHL, CriticalWordDetector.categoryOf("20"))
        assertEquals(CriticalCategory.ZAHL, CriticalWordDetector.categoryOf("fünfundzwanzig"))
        assertEquals(CriticalCategory.ZAHL, CriticalWordDetector.categoryOf("Euro."))
        assertEquals(CriticalCategory.BEDINGUNG, CriticalWordDetector.categoryOf("falls"))
        assertEquals(CriticalCategory.MODAL, CriticalWordDetector.categoryOf("musst"))
        assertNull(CriticalWordDetector.categoryOf("Getränke"))
        assertNull(CriticalWordDetector.categoryOf("eine"))
    }

    @Test fun extractsQuestionsTasksAndDates() {
        val analysis = TranscriptAnalyzer.analyze(words(voiceNote))
        // The greeting "Hey du, na?" is not a question to answer.
        assertEquals(listOf("Hast du am Samstag um 17 Uhr Zeit?"), analysis.questions)
        assertEquals(listOf("Kannst du bitte die Getränke mitbringen, so für 20 Euro?"), analysis.tasks)
        assertEquals(listOf("am Samstag um 17 Uhr", "20 Euro", "morgen"), analysis.datesAndNumbers)
    }

    @Test fun tldrPrefersInformativeSentencesOverGreetings() {
        val tldr = TranscriptAnalyzer.analyze(words(voiceNote)).tldr
        assertFalse(tldr.startsWith("Hey"))
        assertFalse(tldr.contains("Liebe Grüße"))
        assertTrue(tldr.contains("Samstag um 17 Uhr") || tldr.contains("Getränke"))
        assertTrue(tldr.length <= 260)
    }

    @Test fun checksListOnlyUncertainCriticalWordsMostUncertainFirst() {
        val analysis = TranscriptAnalyzer.analyze(words(voiceNote, uncertain = mapOf("nicht" to 0.55f, "17" to 0.30f, "Getränke" to 0.20f)))
        // "Getränke" is uncertain but not meaning-critical, so it is not a check.
        assertEquals(listOf("17", "nicht"), analysis.checks.map { it.word.text })
        assertEquals(CriticalCategory.ZAHL, analysis.checks[0].category)
        assertEquals("Ich komme morgen nicht zum Training.", analysis.checks[1].sentence)
    }

    @Test fun noChecksWhenEverythingIsSure() {
        assertTrue(TranscriptAnalyzer.analyze(words(voiceNote)).checks.isEmpty())
    }

    @Test fun plainTextFallbackStillSummarizes() {
        val analysis = TranscriptAnalyzer.analyze("Hast du morgen Zeit? Ich brauche Hilfe beim Umzug.")
        assertEquals(listOf("Hast du morgen Zeit?"), analysis.questions)
        assertTrue(analysis.tldr.isNotBlank())
    }

    @Test fun emptyTranscript() {
        val analysis = TranscriptAnalyzer.analyze(emptyList<TranscriptWord>())
        assertEquals("", analysis.tldr)
        assertTrue(analysis.questions.isEmpty() && analysis.checks.isEmpty())
    }
}
