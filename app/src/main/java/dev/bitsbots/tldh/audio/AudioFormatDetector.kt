package dev.bitsbots.tldh.audio

enum class AudioFormat(val isSupportedForMvp: Boolean) {
    OGG_OPUS(true),
    OGG_UNKNOWN(true),
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
        val hasIsoBaseMediaHeader = header.size >= 12 && header.copyOfRange(4, 8).toString(Charsets.ISO_8859_1) == "ftyp"
        val looksOpus = headerText.contains("OpusHead", ignoreCase = true) ||
            name.endsWith(".opus") ||
            mime.contains("opus")

        return when {
            hasOggHeader && looksOpus -> AudioFormat.OGG_OPUS
            hasOggHeader || name.endsWith(".ogg") || mime == "application/ogg" || mime == "audio/ogg" -> AudioFormat.OGG_UNKNOWN
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
