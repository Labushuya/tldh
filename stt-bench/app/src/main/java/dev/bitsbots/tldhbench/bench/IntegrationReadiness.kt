package dev.bitsbots.tldhbench.bench

enum class IntegrationDecisionLevel {
    PASS,
    GUARDED,
    BLOCKED,
    UNKNOWN
}

data class IntegrationDecision(
    val level: IntegrationDecisionLevel,
    val title: String,
    val message: String,
    val details: String
)

object IntegrationReadiness {
    fun evaluate(result: BenchmarkResult): IntegrationDecision {
        val comparison = result.referenceComparison ?: return IntegrationDecision(
            level = IntegrationDecisionLevel.UNKNOWN,
            title = "Nicht produktbewertbar",
            message = "Kein Referenztext aktiv. Geschwindigkeit und Transkript sind messbar, Wortqualität aber nicht.",
            details = "Für echte Shared-Audios muss ein korrektes Referenztranskript eingefügt werden, sonst fehlen WER/CER/S/I/D."
        )

        val deletionRate = if (comparison.referenceWordCount > 0) {
            comparison.wordDeletions.toDouble() / comparison.referenceWordCount.toDouble() * 100.0
        } else 0.0
        val rtf = result.timing.rtf
        val speedOk = rtf == null || rtf <= 1.0
        val severeDeletionRisk = deletionRate >= 8.0 || comparison.wordDeletions >= 25
        val real = comparison.realWorldScore

        return when {
            real != null && real.normalizedWerPercent <= 12.0 && real.criticalIssues <= 2 && speedOk -> IntegrationDecision(
                level = IntegrationDecisionLevel.PASS,
                title = "Produktkandidat",
                message = "Die reale Worttreue wirkt nach Normalisierung stark genug für einen tl;dh-Testpfad.",
                details = "Raw-WER bleibt sichtbar. Für Produktbetrieb trotzdem Namen, Zahlen, Uhrzeiten und Negationen markieren und das Volltranskript einblendbar halten."
            )
            real != null && real.normalizedWerPercent <= 18.0 && real.criticalIssues <= 5 && speedOk -> IntegrationDecision(
                level = IntegrationDecisionLevel.GUARDED,
                title = "Produktnah mit Guardrails",
                message = "Die Engine ist für TL;DRs interessant, braucht aber sichtbare Unsicherheits- und Kritische-Wörter-Hinweise.",
                details = "Nutzbar als Cloud-Quality-Testmodus: Summary erzeugen, aber Rohtranskript und kritische Abweichungen immer mitliefern."
            )
            comparison.werPercent <= 15.0 && comparison.cerPercent <= 10.0 && speedOk && !severeDeletionRisk -> IntegrationDecision(
                level = IntegrationDecisionLevel.PASS,
                title = "Produktkandidat",
                message = "Qualität und Geschwindigkeit sind für einen tl;dh-Testpfad grundsätzlich geeignet.",
                details = "Trotzdem kritische Wörter wie Namen, Uhrzeiten, Zahlen und Negationen im Transkript markieren."
            )
            comparison.werPercent <= 25.0 && speedOk && !severeDeletionRisk -> IntegrationDecision(
                level = IntegrationDecisionLevel.GUARDED,
                title = "Nur mit Guardrails",
                message = "Die Engine kann für eine schnelle Rohfassung reichen, aber automatische Zusammenfassungen brauchen sichtbare Unsicherheitswarnungen.",
                details = "TL;DR nur mit eingeblendetem Volltranskript, Confidence-/Fehlerhinweisen und klarer Nutzerprüfung."
            )
            comparison.werPercent <= 35.0 && speedOk -> IntegrationDecision(
                level = IntegrationDecisionLevel.GUARDED,
                title = "Grenzfall",
                message = "Geschwindigkeit passt, aber die Wortqualität ist für belastbare TL;DRs noch zu unsicher.",
                details = "Eher als Preview-Modus nutzbar; finale Zusammenfassung nur nach expliziter Transkriptprüfung."
            )
            else -> IntegrationDecision(
                level = IntegrationDecisionLevel.BLOCKED,
                title = "Nicht produktreif",
                message = "Diese Engine-/Modellkombination sollte nicht ungeprüft für automatische tl;dh-Zusammenfassungen verwendet werden.",
                details = "Aktuell zu viele Wortabweichungen oder fehlende Wörter. Nächster Schritt: Audio-Prep/Chunking/Remote-Provider gegen dieselbe Audio messen."
            )
        }
    }
}
