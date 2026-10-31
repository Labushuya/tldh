package dev.bitsbots.tldh.transcription

/**
 * Whisper models for whisper.cpp, downloaded on demand from a pinned Hugging Face revision and
 * verified by size and SHA-256. Quantized (q5_1) variants keep download size and RAM low enough for
 * older phones.
 */
data class WhisperModelSpec(
    val id: String,
    val displayName: String,
    val qualityLabel: String,
    val fileName: String,
    val sha256: String,
    val sizeBytes: Long,
    val description: String
) {
    val url: String get() = "https://huggingface.co/ggerganov/whisper.cpp/resolve/$HF_REVISION/$fileName"
    val sizeLabel: String get() = "%.0f MB".format(java.util.Locale.GERMANY, sizeBytes / 1_000_000.0)

    private companion object {
        const val HF_REVISION = "5359861c739e955e79d9a303bcbc70fb988958b1"
    }
}

data class CpuFeatures(val dotProd: Boolean, val fp16: Boolean) {
    /** Matches the libwhisper_cli_dotprod.so build (armv8.2-a+dotprod+fp16). */
    val supportsDotProdBuild: Boolean get() = dotProd && fp16

    companion object {
        /** Parses /proc/cpuinfo; a feature only counts if every core lists it. */
        fun parse(cpuinfo: String): CpuFeatures {
            val featureSets = cpuinfo.lineSequence()
                .filter { it.trimStart().startsWith("Features", ignoreCase = true) }
                .map { line -> line.substringAfter(':').trim().split(Regex("\\s+")).toSet() }
                .toList()
            if (featureSets.isEmpty()) return CpuFeatures(dotProd = false, fp16 = false)
            val common = featureSets.reduce { acc, set -> acc intersect set }
            return CpuFeatures(dotProd = "asimddp" in common, fp16 = "asimdhp" in common)
        }
    }
}

data class DeviceProfile(
    val totalRamBytes: Long,
    val cpu: CpuFeatures,
    val arm64: Boolean,
    /** Maximum clock per core in kHz (cpufreq), empty when the kernel doesn't expose it. */
    val coreMaxFreqKhz: List<Long> = emptyList()
)

/**
 * How whisper-cli decodes. Greedy decoding (one candidate per step) is what the STT bench measured;
 * whisper-cli's own default is a 5-wide beam search, several times slower on a phone CPU for a
 * barely measurable accuracy gain on voice notes.
 */
object WhisperDecoding {
    const val BEAM_SIZE = 1
    const val BEST_OF = 1

    /**
     * Threads = the phone's fast cores. Big.LITTLE chips (e.g. 1 prime + 4 performance + 3
     * efficiency cores) get slower when whisper's threads also wait on efficiency cores.
     */
    fun threadsFor(coreMaxFreqKhz: List<Long>): Int {
        val known = coreMaxFreqKhz.filter { it > 0 }
        if (known.isEmpty()) return DEFAULT_THREADS
        val fastest = known.max()
        val fastCores = known.count { it >= fastest * FAST_CORE_SHARE }
        return fastCores.coerceIn(MIN_THREADS, MAX_THREADS)
    }

    private const val DEFAULT_THREADS = 4
    private const val MIN_THREADS = 2
    private const val MAX_THREADS = 6
    private const val FAST_CORE_SHARE = 0.7
}

object WhisperModelCatalog {
    val base = WhisperModelSpec(
        id = "base-q5_1",
        displayName = "Whisper base",
        qualityLabel = "Ausgewogen",
        fileName = "ggml-base-q5_1.bin",
        sha256 = "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898",
        sizeBytes = 59_707_625L,
        description = "Läuft auch auf älteren Handys flott. Gute Erkennung bei klarer Sprache."
    )

    val small = WhisperModelSpec(
        id = "small-q5_1",
        displayName = "Whisper small",
        qualityLabel = "Genau",
        fileName = "ggml-small-q5_1.bin",
        sha256 = "ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb",
        sizeBytes = 190_085_487L,
        description = "Deutlich weniger Verhörer und robuster bei Hintergrundgeräuschen, braucht aber mehr Rechenzeit."
    )

    val models: List<WhisperModelSpec> = listOf(base, small)

    private const val SMALL_MIN_RAM_BYTES = 6L * 1024L * 1024L * 1024L

    fun byId(id: String?): WhisperModelSpec? = models.firstOrNull { it.id == id }

    /** small needs a fast CPU build and enough RAM; everything else gets base. */
    fun recommendedFor(profile: DeviceProfile): WhisperModelSpec =
        if (profile.cpu.supportsDotProdBuild && profile.totalRamBytes >= SMALL_MIN_RAM_BYTES) small else base
}
