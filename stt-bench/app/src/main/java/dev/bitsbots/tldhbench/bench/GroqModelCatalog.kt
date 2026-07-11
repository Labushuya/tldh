package dev.bitsbots.tldhbench.bench

/**
 * Remote STT candidates for the Groq Speech-to-Text benchmark path.
 * These are not local/on-device models. They are API model IDs submitted to
 * https://api.groq.com/openai/v1/audio/transcriptions.
 */
data class GroqSttModelSpec(
    val id: String,
    val displayName: String,
    val sizeLabel: String,
    val speedSignal: Signal,
    val accuracySignal: Signal,
    val privacySignal: Signal,
    val tradeoff: String,
    val notes: String
)

object GroqSttModelCatalog {
    val models: List<GroqSttModelSpec> = listOf(
        GroqSttModelSpec(
            id = "whisper-large-v3-turbo",
            displayName = "Groq Whisper Large v3 Turbo",
            sizeLabel = "remote · API",
            speedSignal = Signal.GREEN,
            accuracySignal = Signal.GREEN,
            privacySignal = Signal.YELLOW,
            tradeoff = "Remote Quality-Speed-Test: sehr schnell, multilingual, günstiger als Large v3.",
            notes = "Primärer Vergleichskandidat für echte WhatsApp-Audios: voraussichtlich wesentlich schneller als lokales Whisper small und qualitativer als tiny/base. Cloud-Upload nur mit bewusst gesetztem API-Key."
        ),
        GroqSttModelSpec(
            id = "whisper-large-v3",
            displayName = "Groq Whisper Large v3",
            sizeLabel = "remote · API",
            speedSignal = Signal.GREEN,
            accuracySignal = Signal.GREEN,
            privacySignal = Signal.YELLOW,
            tradeoff = "Remote Max-Quality-Test: laut Groq genauer als Turbo, aber teurer/langsamer.",
            notes = "Referenzkandidat für maximale Cloud-Qualität gegen dieselbe Audio-/Referenz-/WER-Pipeline. Für tl;dh nur als expliziter Remote-Modus denkbar."
        )
    )

    val defaultModel: GroqSttModelSpec = models.first()

    fun byId(id: String): GroqSttModelSpec = models.firstOrNull { it.id == id } ?: defaultModel
}
