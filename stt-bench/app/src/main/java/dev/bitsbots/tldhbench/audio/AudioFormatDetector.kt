package dev.bitsbots.tldhbench.audio

enum class AudioFormat(val isSupportedForMvp: Boolean) {
    OGG_OPUS(true),
    OGG_UNKNOWN(true),
    FLAC(true),
    WAV_PCM(true),
    MP3(true),
    WEBM_OPUS(false),
    MP4_M4A(false),
    UNKNOWN(false)
}

object AudioFormatDetector {
    fun detect(header: ByteArray, displayName: String?, mimeType: String?): AudioFormat {
        val name = displayName.orEmpty().lowercase()
        val mime = mimeType.orEmpty().lowercase()
        val headerText = header.toString(Charsets.ISO_8859_1)
        val hasOggHeader = header.startsWithAscii("OggS")
        val hasWebmHeader = header.startsWithBytes(0x1A, 0x45, 0xDF, 0xA3)
        val hasFlacHeader = header.startsWithAscii("fLaC")
        val hasRiffWaveHeader = header.startsWithAscii("RIFF") && header.size >= 12 && header.copyOfRange(8, 12).toString(Charsets.ISO_8859_1) == "WAVE"
        val hasMp3FrameHeader = header.size >= 2 && header[0] == 0xFF.toByte() && (header[1].toInt() and 0xE0) == 0xE0
        val hasIsoBaseMediaHeader = header.size >= 12 && header.copyOfRange(4, 8).toString(Charsets.ISO_8859_1) == "ftyp"
        val looksOpus = headerText.contains("OpusHead", ignoreCase = true) ||
            name.endsWith(".opus") ||
            mime.contains("opus")

        return when {
            hasOggHeader && looksOpus -> AudioFormat.OGG_OPUS
            hasOggHeader || name.endsWith(".ogg") || mime == "application/ogg" || mime == "audio/ogg" -> AudioFormat.OGG_UNKNOWN
            hasFlacHeader || name.endsWith(".flac") || mime == "audio/flac" || mime == "audio/x-flac" -> AudioFormat.FLAC
            hasRiffWaveHeader || name.endsWith(".wav") || mime == "audio/wav" || mime == "audio/x-wav" || mime == "audio/wave" -> AudioFormat.WAV_PCM
            hasMp3FrameHeader || name.endsWith(".mp3") || mime == "audio/mpeg" || mime == "audio/mp3" -> AudioFormat.MP3
            hasWebmHeader && looksOpus -> AudioFormat.WEBM_OPUS
            hasIsoBaseMediaHeader || name.endsWith(".m4a") || name.endsWith(".mp4") || mime.contains("mp4") -> AudioFormat.MP4_M4A
            else -> AudioFormat.UNKNOWN
        }
    }

    private fun ByteArray.startsWithAscii(value: String): Boolean {
        if (size < value.length) return false
        return value.encodeToByteArray().withIndex().all { (index, byte) -> this[index] == byte }
    }

    private fun ByteArray.startsWithBytes(vararg values: Int): Boolean {
        if (size < values.size) return false
        return values.withIndex().all { (index, value) -> this[index] == value.toByte() }
    }
}
