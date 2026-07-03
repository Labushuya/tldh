package dev.bitsbots.tldh.audio

enum class AudioFormat {
    OGG_OPUS,
    OGG_UNKNOWN,
    UNKNOWN
}

object AudioFormatDetector {
    fun detect(header: ByteArray, displayName: String?, mimeType: String?): AudioFormat {
        val name = displayName.orEmpty().lowercase()
        val mime = mimeType.orEmpty().lowercase()
        val hasOggHeader = header.size >= 4 && header[0] == 'O'.code.toByte() &&
            header[1] == 'g'.code.toByte() && header[2] == 'g'.code.toByte() && header[3] == 'S'.code.toByte()
        val headerText = header.toString(Charsets.ISO_8859_1)
        val looksOpus = headerText.contains("OpusHead", ignoreCase = true) ||
            name.endsWith(".opus") || mime.contains("opus")

        return when {
            hasOggHeader && looksOpus -> AudioFormat.OGG_OPUS
            hasOggHeader || name.endsWith(".ogg") || mime == "application/ogg" -> AudioFormat.OGG_UNKNOWN
            else -> AudioFormat.UNKNOWN
        }
    }
}
