package dev.bitsbots.tldhbench.audio

/**
 * Reproducible audio preparation profiles for comparing the same real-world audio
 * against the same reference text. These profiles intentionally stay simple and
 * deterministic so WER/CER differences remain attributable to the prep strategy.
 */
data class AudioPreparationProfile(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val description: String,
    val useSilenceReduction: Boolean,
    val aggressiveSilenceReduction: Boolean = false,
    val normalizeRms: Boolean = false,
    val useVoiceBandFilter: Boolean = false
)

object AudioPreparationProfiles {
    val original = AudioPreparationProfile(
        id = "original",
        displayName = "Original",
        shortLabel = "Original",
        description = "Nur Decode auf 16 kHz mono PCM. Keine Normalisierung, kein Gate.",
        useSilenceReduction = false
    )

    val current = AudioPreparationProfile(
        id = "current-basic-gate",
        displayName = "Basic Gate",
        shortLabel = "Basic",
        description = "Bisheriger konservativer Pfad: leise Anfangs-/Endbereiche und lange Pausen reduzieren.",
        useSilenceReduction = true
    )

    val normalized = AudioPreparationProfile(
        id = "normalized",
        displayName = "Normalisiert",
        shortLabel = "Norm",
        description = "RMS/Loudness-Normalisierung ohne Silence Gate. Testet, ob Pegel/Opus-Lautheit der Hauptfaktor ist.",
        useSilenceReduction = false,
        normalizeRms = true
    )

    val voiceBand = AudioPreparationProfile(
        id = "voice-band-basic",
        displayName = "Voice-Band + Basic",
        shortLabel = "VoiceBand",
        description = "Milde Sprachband-Filterung, Normalisierung und konservative Nicht-Sprache-Reduktion.",
        useSilenceReduction = true,
        normalizeRms = true,
        useVoiceBandFilter = true
    )

    val aggressive = AudioPreparationProfile(
        id = "aggressive-gate",
        displayName = "Aggressives Gate",
        shortLabel = "AggroGate",
        description = "Stärkere Pausen-/Noise-Floor-Reduktion. Kann Laufzeit senken, aber Wortanfänge beschädigen.",
        useSilenceReduction = true,
        aggressiveSilenceReduction = true,
        normalizeRms = true
    )

    val all: List<AudioPreparationProfile> = listOf(original, current, normalized, voiceBand, aggressive)
    val matrixProfiles: List<AudioPreparationProfile> = all
    val defaultProfile: AudioPreparationProfile = current

    fun byId(id: String?): AudioPreparationProfile = all.firstOrNull { it.id == id } ?: defaultProfile
}
