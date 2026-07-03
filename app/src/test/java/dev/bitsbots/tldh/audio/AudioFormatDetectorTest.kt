package dev.bitsbots.tldh.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFormatDetectorTest {
    @Test
    fun detectsOggOpusByHeader() {
        val header = "OggS....OpusHead".toByteArray(Charsets.ISO_8859_1)
        assertEquals(AudioFormat.OGG_OPUS, AudioFormatDetector.detect(header, "voice.opus", "audio/ogg"))
    }

    @Test
    fun detectsOggUnknown() {
        val header = "OggS....Vorbis".toByteArray(Charsets.ISO_8859_1)
        assertEquals(AudioFormat.OGG_UNKNOWN, AudioFormatDetector.detect(header, "voice.ogg", "application/ogg"))
    }

    @Test
    fun rejectsUnknown() {
        val header = "nope".toByteArray()
        assertEquals(AudioFormat.UNKNOWN, AudioFormatDetector.detect(header, "file.bin", "application/octet-stream"))
    }
}
