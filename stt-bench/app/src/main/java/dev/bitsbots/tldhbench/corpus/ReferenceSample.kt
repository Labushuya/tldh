package dev.bitsbots.tldhbench.corpus

data class ReferenceSample(
    val id: String,
    val title: String,
    val speaker: String,
    val sourceLabel: String,
    val licenseLabel: String,
    val audioUrl: String,
    val fileName: String,
    val mimeType: String,
    val referenceText: String,
    val difficultyLabel: String,
    val notes: String
)

object BuiltInReferenceCorpus {
    /**
     * Small, curated clean-speech starter set from the CC0 rhasspy/dataset-voice-kerstin corpus.
     * The files are intentionally not bundled into the APK; they are downloaded on demand so the
     * benchmark app stays small and the source remains transparent.
     */
    val samples: List<ReferenceSample> = listOf(
        sample(
            id = "de_rhasspy-0016",
            text = "Ich danke ihm nochmals für seine Arbeit.",
            title = "Kurz · Dank / Alltag",
            difficulty = "clean · kurz",
            notes = "Sehr kurzer Baseline-Satz. Gut, um Modell-Load vs. STT-Zeit zu trennen."
        ),
        sample(
            id = "de_rhasspy-0041",
            text = "An diesem gilt es sich zu orientieren.",
            title = "Kurz · abstrakter Satz",
            difficulty = "clean · kurz",
            notes = "Kurzer Satz mit Funktionswörtern; gute Kontrolle für Auslassungen."
        ),
        sample(
            id = "de_rhasspy-0079",
            text = "Zu diesem wichtigen Punkt haben Sie sich eben nur sehr vage geäußert, Herr Kommissar.",
            title = "Mittel · formelle Rede",
            difficulty = "clean · mittel",
            notes = "Prüft längeren Satz, Höflichkeitsform und Titel."
        ),
        sample(
            id = "de_rhasspy-0103",
            text = "Wichtig ist natürlich, dass wir unsere Standpunkte trotz bestehender Differenzen einander annähern.",
            title = "Mittel · Nebensatz",
            difficulty = "clean · mittel",
            notes = "Prüft Nebensatzstruktur und längere Wörter."
        ),
        sample(
            id = "de_rhasspy-0123",
            text = "Ich hoffe nur, dass die Mitgliedstaaten diese Regelung nun auch besser durchsetzen werden.",
            title = "Mittel · Politik/Regelung",
            difficulty = "clean · mittel",
            notes = "Guter Vergleich für zusammengesetzte Wörter und Endungen."
        ),
        sample(
            id = "de_rhasspy-0138",
            text = "Deshalb wollte ich Ihnen Zeit zum Nachdenken geben.",
            title = "Kurz · Alltag/formell",
            difficulty = "clean · kurz",
            notes = "Nahe an natürlicher Nachricht, aber sauber eingesprochen."
        ),
        sample(
            id = "de_rhasspy-0243",
            text = "Ich denke, so kommen wir weiter, und ich wüsste gern, ob das möglich wäre.",
            title = "Mittel · Gesprächssatz",
            difficulty = "clean · mittel",
            notes = "Am ehesten dialogisch; brauchbar als tl;dh-naher Baseline-Satz."
        ),
        sample(
            id = "de_rhasspy-0270",
            text = "Die jungen Leute gehen weg.",
            title = "Kurz · einfacher Satz",
            difficulty = "clean · kurz",
            notes = "Einfacher Kontrollsatz für Basis-Erkennung."
        )
    )

    fun byId(id: String): ReferenceSample? = samples.firstOrNull { it.id == id }

    private fun sample(
        id: String,
        text: String,
        title: String,
        difficulty: String,
        notes: String
    ): ReferenceSample = ReferenceSample(
        id = id,
        title = title,
        speaker = "Kerstin",
        sourceLabel = "rhasspy/dataset-voice-kerstin",
        licenseLabel = "CC0-1.0",
        audioUrl = "https://raw.githubusercontent.com/rhasspy/dataset-voice-kerstin/master/verified/$id.flac",
        fileName = "$id.flac",
        mimeType = "audio/flac",
        referenceText = text,
        difficultyLabel = difficulty,
        notes = notes
    )
}
