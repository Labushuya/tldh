package dev.bitsbots.tldh.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UiTextTest {
    @Test
    fun recognizesWhatsAppVoiceNotes() {
        assertEquals("WhatsApp", UiText.sourceLabel(AudioOrigin.SHARE, "PTT-20260704-WA0000.opus"))
        assertEquals("WhatsApp", UiText.sourceLabel(AudioOrigin.FILE, "AUD-20261031-WA0012.m4a"))
        assertEquals("Geteilt", UiText.sourceLabel(AudioOrigin.SHARE, "voice.ogg"))
        assertEquals("Memo vom Montag", UiText.sourceLabel(AudioOrigin.FILE, "Memo vom Montag.mp3"))
        assertEquals("Datei", UiText.sourceLabel(AudioOrigin.FILE, null))
    }

    @Test
    fun shortensLongFileNames() {
        val label = UiText.sourceLabel(AudioOrigin.FILE, "Besprechung mit dem ganzen Team über das Budget.m4a")
        assertTrue(label.endsWith("…"))
        assertTrue(label.length <= 26)
    }

    @Test
    fun formatsDurationsAndTimes() {
        assertEquals("1:42 min", UiText.duration(102_000))
        assertNull(UiText.duration(null))
        assertEquals("WhatsApp · 1:42 min", UiText.subtitle(AudioOrigin.SHARE, "PTT-20260704-WA0000.opus", 102_000))
        assertEquals("0:09", UiText.timestamp(9_400))
        assertEquals("in 21 s transkribiert", UiText.transcribedIn(21_200))
        assertEquals("in 1:05 min transkribiert", UiText.transcribedIn(65_000))
    }

    @Test
    fun estimatesRemainingTimeOnlyWhenMeaningful() {
        assertNull(UiText.estimateRemainingSeconds(progress = 2, elapsedMs = 4_000))
        assertNull(UiText.estimateRemainingSeconds(progress = 50, elapsedMs = 800))
        assertEquals(20, UiText.estimateRemainingSeconds(progress = 50, elapsedMs = 20_000))
        assertEquals("Noch etwa 20 Sekunden.", UiText.remainingText(19))
        assertEquals("Gleich fertig.", UiText.remainingText(3))
        assertEquals("Noch etwa 3 Minuten.", UiText.remainingText(170))
    }

    @Test
    fun livePreviewKeepsTheNewestWords() {
        val text = (1..80).joinToString(" ") { "wort$it" }
        val preview = UiText.livePreview(text, maxChars = 60)
        assertTrue(preview.startsWith("… "))
        assertTrue(preview.endsWith("wort80"))
    }

    @Test
    fun findsWholeWordsIgnoringPunctuation() {
        val sentence = "Ich komme morgen nicht, sag das bitte Jonas."
        assertEquals(17 until 22, UiText.findWord(sentence, "nicht,"))
        assertNull(UiText.findWord("Das ist nichtig.", "nicht"))
        assertEquals(38 until 43, UiText.findWord(sentence, "Jonas."))
    }

    @Test
    fun marksUncertainWordsInsideTheTldr() {
        val tldr = "Hast du am Samstag um 17 Uhr Zeit? Kannst du bitte die Getränke mitbringen?"
        val marks = UiText.marksIn(
            tldr,
            listOf(
                Triple("Hast du am Samstag um 17 Uhr Zeit?", "17", true),
                Triple("Ich komme morgen nicht zum Training.", "nicht", false)
            )
        )
        assertEquals(listOf(MarkSpan(22, 24, true)), marks)
    }
}
