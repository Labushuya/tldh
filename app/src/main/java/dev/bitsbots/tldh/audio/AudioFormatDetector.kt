package dev.bitsbots.tldh.audio

/**
 * Container/codec families tl;dh recognizes. Everything marked supported is decoded by Android's
 * MediaExtractor/MediaCodec, so WhatsApp voice notes (Ogg Opus) and audio files picked from storage
 * (M4A, MP3, WAV, FLAC, AMR, AAC, WebM) all go through the same pipeline.
 */
enum class AudioFormat(val isSupported: Boolean, val label: String) {
    OGG_OPUS(true, "Ogg Opus"),
    OGG_UNKNOWN(true, "Ogg"),
    WEBM_OPUS(true, "WebM Opus"),
    WEBM(true, "WebM"),
    MP4_M4A(true, "M4A/AAC"),
    MP3(true, "MP3"),
    WAV(true, "WAV"),
    FLAC(true, "FLAC"),
    AMR(true, "AMR"),
    AAC_ADTS(true, "AAC"),
    UNKNOWN(false, "unbekannt")
}

object AudioFormatDetector {
    fun detect(header: ByteArray, displayName: String?, mimeType: String?): AudioFormat {
        val name = displayName.orEmpty().lowercase()
        val mime = mimeType.orEmpty().lowercase()
        val headerText = header.toString(Charsets.ISO_8859_1)
        val hasOggHeader = header.startsWithAscii("OggS")
        val hasWebmHeader = header.startsWithBytes(0x1A, 0x45, 0xDF, 0xA3)
        val hasIsoBaseMediaHeader = header.size >= 12 && header.copyOfRange(4, 8).toString(Charsets.ISO_8859_1) == "ftyp"
        val hasWavHeader = header.startsWithAscii("RIFF") && header.size >= 12 && header.copyOfRange(8, 12).toString(Charsets.ISO_8859_1) == "WAVE"
        val looksOpus = headerText.contains("OpusHead", ignoreCase = true) ||
            name.endsWith(".opus") ||
            mime.contains("opus")

        return when {
            hasOggHeader && looksOpus -> AudioFormat.OGG_OPUS
            hasOggHeader -> AudioFormat.OGG_UNKNOWN
            hasWebmHeader && looksOpus -> AudioFormat.WEBM_OPUS
            hasWebmHeader -> AudioFormat.WEBM
            hasIsoBaseMediaHeader -> AudioFormat.MP4_M4A
            hasWavHeader -> AudioFormat.WAV
            header.startsWithAscii("fLaC") -> AudioFormat.FLAC
            header.startsWithAscii("#!AMR") -> AudioFormat.AMR
            header.startsWithAscii("ID3") || header.isMpegAudioFrame() -> AudioFormat.MP3
            header.isAdtsFrame() -> AudioFormat.AAC_ADTS
            name.endsWith(".ogg") || name.endsWith(".opus") || mime == "application/ogg" || mime.startsWith("audio/ogg") -> AudioFormat.OGG_UNKNOWN
            name.endsWith(".webm") || mime.contains("webm") -> AudioFormat.WEBM
            name.endsWith(".m4a") || name.endsWith(".mp4") || mime.contains("mp4") || mime.contains("m4a") -> AudioFormat.MP4_M4A
            name.endsWith(".mp3") || mime == "audio/mpeg" || mime == "audio/mp3" -> AudioFormat.MP3
            name.endsWith(".wav") || mime.contains("wav") -> AudioFormat.WAV
            name.endsWith(".flac") || mime.contains("flac") -> AudioFormat.FLAC
            name.endsWith(".amr") || mime.contains("amr") -> AudioFormat.AMR
            name.endsWith(".aac") || mime == "audio/aac" || mime == "audio/aacp" -> AudioFormat.AAC_ADTS
            else -> AudioFormat.UNKNOWN
        }
    }

    /** MPEG audio frame sync (11 set bits) with a layer other than "reserved" (ADTS uses layer 00). */
    private fun ByteArray.isMpegAudioFrame(): Boolean =
        size >= 2 && this[0] == 0xFF.toByte() && (this[1].toInt() and 0xE0) == 0xE0 && (this[1].toInt() and 0x06) != 0

    /** AAC ADTS sync word: 12 set bits followed by layer 00. */
    private fun ByteArray.isAdtsFrame(): Boolean =
        size >= 2 && this[0] == 0xFF.toByte() && (this[1].toInt() and 0xF6) == 0xF0

    private fun ByteArray.startsWithAscii(value: String): Boolean {
        if (size < value.length) return false
        return value.encodeToByteArray().withIndex().all { (index, byte) -> this[index] == byte }
    }

    private fun ByteArray.startsWithBytes(vararg values: Int): Boolean {
        if (size < values.size) return false
        return values.withIndex().all { (index, value) -> this[index] == value.toByte() }
    }
}
