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
     * Curated clean-speech corpus from the CC0 rhasspy/dataset-voice-kerstin corpus.
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
        ),

        // Additional CC0 clean-speech pool for realistic 30s/90s/4min longform scenarios.
        sample("de_rhasspy-0004", "Sie ist nach der Kreisstadt Weißenburg des Landkreises Weißenburg-Gunzenhausen benannt.", "Pool · Ortsname", "clean · mittel", "Enthält zusammengesetzte Orts-/Landkreisnamen."),
        sample("de_rhasspy-0007", "Ursprünglich wurde Bellflower und das benachbarte Paramount von niederländischen, japanischen und portugiesischen Milchbauern bewohnt.", "Pool · Eigennamen", "clean · lang", "Fremd- und Eigennamen; gut für Robustheit."),
        sample("de_rhasspy-0008", "Nun verfolgten die Helvetier die Römer und begannen, die Nachhut der Römer anzugreifen.", "Pool · Geschichte", "clean · mittel", "Prüft Namen und Verbformen."),
        sample("de_rhasspy-0010", "Auch diesmal behaupten die Behörden, sie wüssten nichts von der Existenz solcher Dateien.", "Pool · Behörden", "clean · mittel", "Prüft Umlaute und indirekte Rede."),
        sample("de_rhasspy-0011", "Er galt als hervorragender Interpret der Werke Anton Bruckners und Pjotr Iljitsch Tschaikowskis.", "Pool · Komponisten", "clean · lang", "Schwierige Namen und Genitiv-Struktur."),
        sample("de_rhasspy-0012", "Es gibt verschiedene Arten von Head-Mounted Displays, die je nach Verwendungszweck unterschiedlich ausgestattet sind.", "Pool · Technik", "clean · mittel", "Enthält englischen Fachbegriff im deutschen Satz."),
        sample("de_rhasspy-0018", "Durch schlechtes Wetter am vorgesehenen Landeort wurde die Rückkehr mehrfach verschoben.", "Pool · Ereignis", "clean · mittel", "Natürlicher Informationssatz."),
        sample("de_rhasspy-0019", "Entsprechend bewertet die Denkmalbehörde die Liebfrauenkirche als architektonisch und künstlerisch herausragendes Bauwerk.", "Pool · Kultur", "clean · lang", "Lange Wörter und Kirchenname."),
        sample("de_rhasspy-0020", "Da zwei Personen auf gleich viele Erwähnungen kamen, musste die Entscheidung per Losverfahren erfolgen.", "Pool · Entscheidung", "clean · mittel", "Zahlenwort und Verwaltungssprache."),
        sample("de_rhasspy-0021", "Sie prämiert innovative deutsch-türkische Projekte zur Verbesserung des Zusammenlebens in der Hansestadt.", "Pool · Gesellschaft", "clean · mittel", "Bindestrichwort und abstrakte Substantive."),
        sample("de_rhasspy-0022", "Nach dem Abschluss einer Uhrmacherlehre wanderte Joseph Moll durch verschiedene europäische Staaten.", "Pool · Biografie", "clean · mittel", "Name, Beruf und Ortsbezug."),
        sample("de_rhasspy-0038", "In der Literaturgeschichte bilden August Strindbergs Beziehungen zu Österreich einen seiner Schwerpunkte.", "Pool · Literatur", "clean · mittel", "Personenname und Österreich."),
        sample("de_rhasspy-0042", "Mittlerweile gibt es für praktisch jede Sportart mehrere entsprechende Computerspiel-Pendants.", "Pool · Gaming", "clean · mittel", "Alltagsnaher Begriff mit Fremdwort."),
        sample("de_rhasspy-0045", "Außerdem wurden die fünf besten Leistungen im Siebenkampf überhaupt von ihr erzielt.", "Pool · Sport", "clean · mittel", "Zahlenwort und Sportbegriff."),
        sample("de_rhasspy-0063", "Sie spielten sowohl alte Lieder von Spliff neu ein als auch eigene Stücke.", "Pool · Musik", "clean · mittel", "Bandname und Alltagssprache."),
        sample("de_rhasspy-0066", "Danach ist eine Versetzung nur zu einer anderen Dienststelle desselben Arbeitgebers zulässig.", "Pool · Arbeit", "clean · mittel", "Arbeits-/Verwaltungssprache."),
        sample("de_rhasspy-0071", "In der Geschichte der finnischen Präsidentschaftswahlen, gab es einige Abweichungen vom eigentlichen Prozedere.", "Pool · Politik", "clean · lang", "Langes Kompositum und Fremdwort."),
        sample("de_rhasspy-0072", "Die dortigen Schwestern betreuten den Kindergarten, betrieben die ambulante Krankenpflege sowie eine Nähschule.", "Pool · Alltag", "clean · mittel", "Mehrgliedrige Aufzählung."),
        sample("de_rhasspy-0094", "Als Maß für die vergleichbare Wasseraufnahme gibt es den sogenannten Cobb-Wert.", "Pool · Technikbegriff", "clean · mittel", "Fachbegriff und Bindestrich."),
        sample("de_rhasspy-0105", "Die Nachricht über seine Festnahme führte zu spontanen Straßenfesten in Freetown.", "Pool · Nachricht", "clean · mittel", "Stadtname und Ereignisbericht."),
        sample("de_rhasspy-0117", "Und das ist gegenüber einem Staatschef weder korrekt noch ehrenhaft.", "Pool · Bewertung", "clean · kurz", "Kurzer natürlicher Bewertungssatz."),
        sample("de_rhasspy-0120", "Über die Gründe der unstandesgemäßen Hochzeit wird noch mehr gerätselt.", "Pool · Alltag/formell", "clean · mittel", "Langes Adjektiv und Passivnähe."),
        sample("de_rhasspy-0135", "Mich stört nur die Protokollnotiz für die Tschechische Republik.", "Pool · Politik", "clean · mittel", "Staatenname und Verwaltungssprache."),
        sample("de_rhasspy-0147", "Frau McGuinness sagte, die Modulation sei ein Diebstahl an den Landwirten.", "Pool · Rede", "clean · mittel", "Name und indirekte Rede."),
        sample("de_rhasspy-0149", "Nach einem Volontariat bei n-tv folgten Tätigkeiten als Nachrichtenredakteur und Moderator bei dem Sender.", "Pool · Medien", "clean · lang", "Medienbegriff und Markenname."),
        sample("de_rhasspy-0165", "Obgleich die Idee eines Sicherheitsnetzes sehr interessant ist, reicht sie nicht aus.", "Pool · Argument", "clean · mittel", "Natürliches Argumentationsmuster."),
        sample("de_rhasspy-0176", "Für humane Rotaviren sind mehrere Impfstoffe für kleine Kinder zugelassen.", "Pool · Gesundheit", "clean · mittel", "Fachbegriff und sensible Wortklasse."),
        sample("de_rhasspy-0181", "Die Persönlichkeitseigenschaften, die ein angenehmes Lebensgefühl verbreiten, korrelieren mit einer besseren Funktionsfähigkeit des Immunsystems.", "Pool · Langsatz", "clean · lang", "Langer Satz mit mehreren Komposita."),
        sample("de_rhasspy-0192", "Großtechnisch kann die Dehydratisierung der Alkohole unter Druck katalytisch in der Gasphase durchgeführt werden.", "Pool · Chemie", "clean · lang", "Fachwörter und lange Wörter."),
        sample("de_rhasspy-0200", "Die Guests setzten sich zudem stark für die Weiterbildung der Erwachsenen ein.", "Pool · Fremdwort", "clean · mittel", "Englisches Wort im deutschen Satz."),
        sample("de_rhasspy-0217", "Stattdessen suchte er Anschluss an die lokale Hip-Hop- und Breakdance-Szene.", "Pool · Kultur", "clean · mittel", "Bindestrichwörter und Anglizismen."),
        sample("de_rhasspy-0224", "Nollau kündigte eine Verhaftung Guillaumes für die nächsten zwei bis drei Wochen an.", "Pool · Zeitangabe", "clean · mittel", "Namen und Zeitraum."),
        sample("de_rhasspy-0233", "Bei Kopf soll sie bleiben, bei Zahl gehen.", "Pool · Kurzregel", "clean · kurz", "Kurzer Satz mit potenziell verwechslungsanfälligen Wörtern."),
        sample("de_rhasspy-0251", "Mit darüber hinausgehenden Äußerungen würden wir uns auf sehr unsicheres Terrain begeben.", "Pool · Gespräch", "clean · mittel", "Sehr tl;dh-naher Warn-/Abwägungssatz."),
        sample("de_rhasspy-0259", "Der Rat kann den Maximalbetrag, der sich aus Ihrer zweiten Lesung ergeben hat, akzeptieren.", "Pool · Zahlenbezug", "clean · mittel", "Ordnungszahl und politische Sprache."),
        sample("de_rhasspy-0268", "Im Kopfhörerkabel muss unbedingt ein Lautstärkeregler integriert sein.", "Pool · Alltag/Technik", "clean · mittel", "Alltagsnaher technischer Satz."),
        sample("de_rhasspy-0278", "Die komplexen Lebenszyklen der meisten Parasiten beeinflussen auch die Nahrungskette und das gesamte Ökosystem.", "Pool · Natur", "clean · lang", "Mehrere lange Fachbegriffe."),
        sample("de_rhasspy-0282", "Da ja Europa immer mehr Kompetenzen erhält, muss diese Kluft kleiner werden.", "Pool · Politik/Alltag", "clean · mittel", "Natürlich gesprochener Diskurssatz."),
        sample("de_rhasspy-0295", "Die Abwanderung von Wissenschaftlern, der so genannte Braindrain, sollte sehr ernst genommen werden.", "Pool · Wissenschaft", "clean · mittel", "Fremdwort und Warnformulierung."),
        sample("de_rhasspy-0310", "In der Tat erwarten wir vom Rat nicht mehr nur starkes Engagement, sondern Taten.", "Pool · Rede", "clean · mittel", "Natürliches Statement mit Kontrast."),
        sample("de_rhasspy-0321", "Papierkrieg und Regulierung sind ein Luxus, den sich Europa nicht mehr leisten kann.", "Pool · Politik/Alltag", "clean · mittel", "Alltagsnaher politischer Satz."
        )
    )

    val starterSamples: List<ReferenceSample> = samples.take(8)
    val longFormSamples: List<ReferenceSample> = samples

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
