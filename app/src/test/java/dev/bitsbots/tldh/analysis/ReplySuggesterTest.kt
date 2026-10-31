package dev.bitsbots.tldh.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplySuggesterTest {
    private fun suggest(text: String) = ReplySuggester.suggest(TranscriptAnalyzer.analyze(text))

    @Test fun appointmentQuestionFillsTheTimePhrase() {
        val replies = suggest("Hey du, na? Hast du am Samstag um 17 Uhr Zeit? Wir grillen bei mir.")
        assertEquals(ReplyIntent.TERMIN, replies.intent)
        val short = replies.byTone.getValue(ReplyTone.KURZ)
        assertEquals("Passt, am Samstag um 17 Uhr bin ich dabei.", short[0])
        assertEquals("Am Samstag um 17 Uhr schaffe ich leider nicht.", short[1])
        assertTrue(replies.byTone.getValue(ReplyTone.FOERMLICH)[1].startsWith("Am Samstag um 17 Uhr ist mir leider nicht möglich."))
    }

    @Test fun singleTimeWordWorksAsSlot() {
        val replies = suggest("Hast du morgen Zeit?")
        assertEquals(ReplyIntent.TERMIN, replies.intent)
        assertEquals("Ja, morgen passt mir gut, ich freu mich!", replies.byTone.getValue(ReplyTone.FREUNDLICH)[0])
    }

    @Test fun requestBeatsPlainQuestion() {
        val replies = suggest("Kannst du bitte die Getränke mitbringen? Ist das okay für dich?")
        assertEquals(ReplyIntent.BITTE, replies.intent)
        assertEquals("Klar, mach ich gerne!", replies.byTone.getValue(ReplyTone.FREUNDLICH)[0])
    }

    @Test fun yesNoVersusOpenQuestion() {
        assertEquals(ReplyIntent.JA_NEIN_FRAGE, suggest("Ich hab das Buch gelesen. Gefällt dir der Autor?").intent)
        assertEquals(ReplyIntent.OFFENE_FRAGE, suggest("Ich hab das Buch gelesen. Wie fandest du das Ende?").intent)
    }

    @Test fun cancellationThanksAndInfo() {
        assertEquals(ReplyIntent.ABSAGE, suggest("Ich komme morgen nicht zum Training. Mir geht's nicht gut.").intent)
        assertEquals(ReplyIntent.DANK, suggest("Danke dir nochmal für gestern, war richtig schön.").intent)
        assertEquals(ReplyIntent.INFO, suggest("Ich bin jetzt zu Hause angekommen. Alles gut gelaufen.").intent)
    }

    @Test fun everyToneHasThreeReplies() {
        val replies = suggest("Hast du am Freitag Zeit?")
        ReplyTone.entries.forEach { tone -> assertEquals(3, replies.byTone.getValue(tone).size) }
    }

    @Test fun noteListsSummaryAndKeyPoints() {
        val note = NoteFormatter.format(
            TranscriptAnalyzer.analyze("Hast du am Samstag um 17 Uhr Zeit? Kannst du bitte die Getränke mitbringen?"),
            "31.10.2026 · 0:12 min"
        )
        assertTrue(note.startsWith("Notiz aus Sprachnachricht (31.10.2026 · 0:12 min)"))
        assertTrue(note.contains("Termine & Zahlen:\n• am Samstag um 17 Uhr"))
        assertTrue(note.contains("Aufgaben:\n• Kannst du bitte die Getränke mitbringen?"))
    }
}
