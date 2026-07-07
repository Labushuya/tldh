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

        return when {
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
                details = "Aktuell zu viele Wortabweichungen oder fehlende Wörter. Nächster Schritt: whisper.cpp / sherpa-onnx / LAN-Quality-Mode gegen dieselbe Audio messen."
            )
        }
    }
}
