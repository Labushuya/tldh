package dev.bitsbots.tldhbench.bench

data class WhisperModelSpec(
    val id: String,
    val displayName: String,
    val fileName: String,
    val url: String,
    val sizeLabel: String,
    val memoryLabel: String,
    val speedSignal: Signal,
    val expectedAccuracySignal: Signal,
    val phoneSignal: Signal,
    val notes: String
)

object WhisperModelCatalog {
    val models: List<WhisperModelSpec> = listOf(
        WhisperModelSpec(
            id = "tiny",
            displayName = "Whisper tiny",
            fileName = "ggml-tiny.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin",
            sizeLabel = "75 MiB",
            memoryLabel = "~273 MB",
            speedSignal = Signal.GREEN,
            expectedAccuracySignal = Signal.YELLOW,
            phoneSignal = Signal.GREEN,
            notes = "Erster Handy-Smoke-Test: kleinster sinnvoller multilingualer Whisper-Kandidat. Erwartung: schneller, aber evtl. noch zu ungenau."
        ),
        WhisperModelSpec(
            id = "base",
            displayName = "Whisper base",
            fileName = "ggml-base.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
            sizeLabel = "142 MiB",
            memoryLabel = "~388 MB",
            speedSignal = Signal.YELLOW,
            expectedAccuracySignal = Signal.YELLOW,
            phoneSignal = Signal.GREEN,
            notes = "Wahrscheinlich wichtigster erster Qualitäts-/Speed-Kompromiss für On-Device-Tests."
        ),
        WhisperModelSpec(
            id = "small",
            displayName = "Whisper small",
            fileName = "ggml-small.bin",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin",
            sizeLabel = "466 MiB",
            memoryLabel = "~852 MB",
            speedSignal = Signal.RED,
            expectedAccuracySignal = Signal.GREEN,
            phoneSignal = Signal.YELLOW,
            notes = "Qualitätskandidat für das Magic V2, aber Speicher, Akkulast und Laufzeit kritisch beobachten."
        )
    )

    val defaultModel: WhisperModelSpec = models[1]

    fun byId(id: String): WhisperModelSpec = models.firstOrNull { it.id == id } ?: defaultModel
}
