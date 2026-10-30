package dev.bitsbots.tldhbench.bench

/**
 * Cactus Compute Whistle: 16.9 MB multilingual speech-to-text (en, de, fr, es, it, nl, pl), Apache-2.0.
 * The model file is downloaded on demand from a pinned Hugging Face revision and verified by SHA-256.
 * The `needle` engine binary that runs it is bundled into the APK at build time (see app/build.gradle.kts).
 */
object WhistleModel {
    const val displayName = "Whistle 16.9 MB"
    const val fileName = "whistle.cact"
    const val revision = "741118687e1f008f06d1351a402a731274789790"
    const val url = "https://huggingface.co/Cactus-Compute/whistle/resolve/$revision/whistle.cact"
    const val sha256 = "b6e02f048568ac5d01a2042556c658061e699acbc0aa2a1439f52f3d461dffeb"
    const val sizeBytes = 16_919_407L
    const val sizeLabel = "16.9 MB"

    /** Bundled as a native library so Android extracts it into an executable location. */
    const val runtimeLibraryName = "libneedle_cli.so"
}

enum class WhistleChunking {
    /** The app splits the audio at pauses into pieces of at most [WhistleModeSpec.maxChunkMs]. */
    APP_PAUSES,

    /** One engine call; the engine windows audio beyond 30 s itself. */
    ENGINE_WINDOWS
}

/** Whistle uses one model file; the "models" of this engine are run modes on top of it. */
data class WhistleModeSpec(
    val id: String,
    val displayName: String,
    val chunking: WhistleChunking,
    val decoderDepth: Int?,
    val maxChunkMs: Long,
    val speedSignal: Signal,
    val accuracySignal: Signal,
    val notes: String
)

object WhistleModeCatalog {
    val modes: List<WhistleModeSpec> = listOf(
        WhistleModeSpec(
            id = "app-pauses",
            displayName = "Whistle · Pausen-Chunking",
            chunking = WhistleChunking.APP_PAUSES,
            decoderDepth = null,
            maxChunkMs = 25_000L,
            speedSignal = Signal.GREEN,
            accuracySignal = Signal.YELLOW,
            notes = "Empfohlen. Die App schneidet an Sprechpausen in Stücke <= 25 s. Im Desktop-Test verhinderte das die Satz-Auslassungen der Engine-Fenster (4-min-Datei: 8 statt 42 fehlende Wörter)."
        ),
        WhistleModeSpec(
            id = "engine-windows",
            displayName = "Whistle · Engine-Fenster",
            chunking = WhistleChunking.ENGINE_WINDOWS,
            decoderDepth = null,
            maxChunkMs = 30_000L,
            speedSignal = Signal.GREEN,
            accuracySignal = Signal.RED,
            notes = "Vergleichsmodus: ein Aufruf, die Engine zerlegt selbst in 30-s-Fenster. Im Desktop-Test fehlten an einer Fenstergrenze drei Sätze am Stück. Nur zum Gegenprüfen auf dem Gerät."
        ),
        WhistleModeSpec(
            id = "app-pauses-depth4",
            displayName = "Whistle · Pausen-Chunking · Tiefe 4",
            chunking = WhistleChunking.APP_PAUSES,
            decoderDepth = 4,
            maxChunkMs = 25_000L,
            speedSignal = Signal.GREEN,
            accuracySignal = Signal.RED,
            notes = "Tempo-Experiment: nur 4 statt 8 Decoder-Schichten. Decoder auf dem Desktop ca. 1,6× schneller, aber mehr Verhörer. Prüfen, ob sich das auf dem Handy lohnt."
        )
    )

    val defaultMode: WhistleModeSpec = modes.first()

    fun byId(id: String?): WhistleModeSpec = modes.firstOrNull { it.id == id } ?: defaultMode
}
