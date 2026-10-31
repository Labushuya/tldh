package dev.bitsbots.tldh.analysis

import java.util.Locale

enum class ReplyIntent(val label: String) {
    TERMIN("Terminanfrage"),
    BITTE("Bitte"),
    JA_NEIN_FRAGE("Ja/Nein-Frage"),
    OFFENE_FRAGE("Offene Frage"),
    ABSAGE("Absage"),
    DANK("Dank"),
    INFO("Info")
}

enum class ReplyTone(val label: String) {
    KURZ("kurz"),
    FREUNDLICH("freundlich"),
    FOERMLICH("förmlich")
}

data class ReplySuggestions(
    val intent: ReplyIntent,
    /** Three replies per tone: agree, decline or alternative, defer. */
    val byTone: Map<ReplyTone, List<String>>
)

/**
 * Picks reply templates from what the voice note asks for. The rules look at the extracted
 * questions, requests and dates; the only content filled into a template is a time phrase taken
 * verbatim from the transcript, such as "am Samstag um 17 Uhr".
 */
object ReplySuggester {
    private val wQuestionWords = setOf(
        "wer", "wen", "wem", "wessen", "was", "wann", "wo", "wohin", "woher", "wie", "wieso", "weshalb",
        "warum", "welche", "welcher", "welches", "welchen", "wieviel", "wie viel", "womit", "wofür", "worüber"
    )
    private val declineVerbs = listOf("komm", "schaff", "kann", "klappt", "geht", "pack")

    fun suggest(analysis: TranscriptAnalysis): ReplySuggestions {
        val questions = analysis.questions
        val timeQuestion = questions.firstOrNull { sentence -> sentence.split(" ").any { CriticalWordDetector.categoryOf(it) == CriticalCategory.ZEIT } }
        val intent = when {
            timeQuestion != null -> ReplyIntent.TERMIN
            analysis.tasks.isNotEmpty() -> ReplyIntent.BITTE
            questions.any { !isWQuestion(it) } -> ReplyIntent.JA_NEIN_FRAGE
            questions.isNotEmpty() -> ReplyIntent.OFFENE_FRAGE
            analysis.sentences.any { isCancellation(it) } -> ReplyIntent.ABSAGE
            analysis.sentences.any { it.lowercase(Locale.GERMAN).contains("danke") || it.lowercase(Locale.GERMAN).contains("vielen dank") } -> ReplyIntent.DANK
            else -> ReplyIntent.INFO
        }
        val time = timeQuestion?.let { question -> timePhraseIn(question, analysis.datesAndNumbers) }
        return ReplySuggestions(intent, ReplyTone.entries.associateWith { tone -> templates(intent, tone, time) })
    }

    private fun isWQuestion(question: String): Boolean {
        val normalized = question.lowercase(Locale.GERMAN).trimStart()
        return wQuestionWords.any { normalized.startsWith("$it ") || normalized.startsWith("$it,") }
    }

    private fun isCancellation(sentence: String): Boolean {
        if (sentence.trimEnd().endsWith('?')) return false
        val categories = sentence.split(" ").mapNotNull { CriticalWordDetector.categoryOf(it) }.toSet()
        if (CriticalCategory.VERNEINUNG !in categories) return false
        val normalized = sentence.lowercase(Locale.GERMAN)
        return CriticalCategory.ZEIT in categories || declineVerbs.any { normalized.contains(it) }
    }

    /** The extracted time phrase that occurs in the question, e.g. "am Samstag um 17 Uhr". */
    private fun timePhraseIn(question: String, datesAndNumbers: List<String>): String? =
        datesAndNumbers.firstOrNull { phrase ->
            question.contains(phrase) && phrase.split(" ").any { CriticalWordDetector.categoryOf(it) == CriticalCategory.ZEIT }
        }

    private fun templates(intent: ReplyIntent, tone: ReplyTone, time: String?): List<String> {
        // "am Samstag um 17 Uhr" in mid-sentence, "Am Samstag um 17 Uhr" at the start.
        val t = time ?: "dann"
        val tStart = t.replaceFirstChar { it.titlecase(Locale.GERMAN) }
        return when (intent) {
            ReplyIntent.TERMIN -> when (tone) {
                ReplyTone.KURZ -> listOf("Passt, $t bin ich dabei.", "$tStart schaffe ich leider nicht.", "Ich schau nach und melde mich.")
                ReplyTone.FREUNDLICH -> listOf(
                    "Ja, $t passt mir gut, ich freu mich!",
                    "$tStart klappt bei mir leider nicht. Geht es auch etwas später?",
                    "Gute Idee! Ich schau kurz in meinen Kalender und melde mich gleich."
                )
                ReplyTone.FOERMLICH -> listOf(
                    "$tStart passt mir, ich bin gerne dabei.",
                    "$tStart ist mir leider nicht möglich. Hätten Sie einen Alternativtermin?",
                    "Ich prüfe meinen Kalender und melde mich zeitnah bei Ihnen."
                )
            }
            ReplyIntent.BITTE -> when (tone) {
                ReplyTone.KURZ -> listOf("Klar, mach ich.", "Sorry, das schaffe ich nicht.", "Ich schau, ob's klappt.")
                ReplyTone.FREUNDLICH -> listOf(
                    "Klar, mach ich gerne!",
                    "Sorry, das schaffe ich diesmal leider nicht.",
                    "Ich schau, ob ich das hinbekomme, und melde mich."
                )
                ReplyTone.FOERMLICH -> listOf(
                    "Selbstverständlich, das übernehme ich gerne.",
                    "Das ist mir leider nicht möglich.",
                    "Ich prüfe das und melde mich bei Ihnen."
                )
            }
            ReplyIntent.JA_NEIN_FRAGE -> when (tone) {
                ReplyTone.KURZ -> listOf("Ja, passt.", "Nein, leider nicht.", "Sag ich dir gleich.")
                ReplyTone.FREUNDLICH -> listOf("Ja, gerne!", "Leider nein, sorry!", "Muss ich kurz checken, ich melde mich gleich.")
                ReplyTone.FOERMLICH -> listOf("Ja, sehr gerne.", "Leider nein.", "Ich kläre das und melde mich bei Ihnen.")
            }
            ReplyIntent.OFFENE_FRAGE -> when (tone) {
                ReplyTone.KURZ -> listOf("Gute Frage, ich schau nach.", "Weiß ich noch nicht, sag ich dir später.", "Ruf mich kurz an?")
                ReplyTone.FREUNDLICH -> listOf(
                    "Gute Frage! Ich schau nach und melde mich gleich.",
                    "Das weiß ich gerade noch nicht, ich sag dir später Bescheid.",
                    "Lass uns kurz telefonieren, dann klären wir das."
                )
                ReplyTone.FOERMLICH -> listOf(
                    "Danke für Ihre Frage, ich kläre das und melde mich.",
                    "Das kann ich Ihnen im Moment noch nicht sagen, ich gebe Ihnen zeitnah Bescheid.",
                    "Gerne können wir das kurz telefonisch besprechen."
                )
            }
            ReplyIntent.ABSAGE -> when (tone) {
                ReplyTone.KURZ -> listOf("Kein Problem, danke für Bescheid.", "Schade, dann ein andermal.", "Alles klar.")
                ReplyTone.FREUNDLICH -> listOf(
                    "Kein Problem, danke dir fürs Bescheid sagen!",
                    "Schade! Dann holen wir das bald nach.",
                    "Alles gut, bis zum nächsten Mal!"
                )
                ReplyTone.FOERMLICH -> listOf(
                    "Vielen Dank für die Information, das ist kein Problem.",
                    "Schade, dann finden wir gerne einen anderen Termin.",
                    "Alles klar, vielen Dank."
                )
            }
            ReplyIntent.DANK -> when (tone) {
                ReplyTone.KURZ -> listOf("Gerne!", "Kein Ding.", "Immer gern.")
                ReplyTone.FREUNDLICH -> listOf("Sehr gerne, freut mich!", "Kein Problem, jederzeit!", "Gerne geschehen 😊")
                ReplyTone.FOERMLICH -> listOf("Sehr gerne.", "Gern geschehen.", "Keine Ursache, jederzeit gerne.")
            }
            ReplyIntent.INFO -> when (tone) {
                ReplyTone.KURZ -> listOf("Danke für die Info!", "Alles klar.", "Okay, passt.")
                ReplyTone.FREUNDLICH -> listOf("Danke dir für die Info!", "Alles klar, danke fürs Bescheid sagen!", "Super, danke!")
                ReplyTone.FOERMLICH -> listOf("Vielen Dank für die Information.", "Alles klar, danke.", "Danke, ich habe es zur Kenntnis genommen.")
            }
        }
    }
}

/** A copy-ready note of the voice note's content, for pasting into a chat or notes app. */
object NoteFormatter {
    fun format(analysis: TranscriptAnalysis, recordedLabel: String): String = buildString {
        appendLine("Notiz aus Sprachnachricht ($recordedLabel)")
        appendLine()
        appendLine(analysis.tldr)
        listOf(
            "Fragen" to analysis.questions,
            "Termine & Zahlen" to analysis.datesAndNumbers,
            "Aufgaben" to analysis.tasks
        ).filter { it.second.isNotEmpty() }.forEach { (title, items) ->
            appendLine()
            appendLine("$title:")
            items.forEach { appendLine("• $it") }
        }
    }.trimEnd()
}
