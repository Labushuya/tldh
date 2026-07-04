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
    fun detectsOggOpusByWhatsappMime() {
        val header = "OggS....payload".toByteArray(Charsets.ISO_8859_1)
        assertEquals(AudioFormat.OGG_OPUS, AudioFormatDetector.detect(header, "PTT-20260704-WA0000.opus", "audio/ogg; codecs=opus"))
    }

    @Test
    fun detectsOggUnknown() {
        val header = "OggS....Vorbis".toByteArray(Charsets.ISO_8859_1)
        assertEquals(AudioFormat.OGG_UNKNOWN, AudioFormatDetector.detect(header, "voice.ogg", "application/ogg"))
    }

    @Test
    fun detectsWebmOpusAsKnownButNotMvpSupported() {
        val header = byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte()) + "OpusHead".toByteArray(Charsets.ISO_8859_1)
        assertEquals(AudioFormat.WEBM_OPUS, AudioFormatDetector.detect(header, "voice.webm", "audio/webm; codecs=opus"))
    }

    @Test
    fun detectsMp4M4a() {
        val header = byteArrayOf(0, 0, 0, 24) + "ftypM4A ".toByteArray(Charsets.ISO_8859_1)
        assertEquals(AudioFormat.MP4_M4A, AudioFormatDetector.detect(header, "voice.m4a", "audio/mp4"))
    }

    @Test
    fun rejectsUnknown() {
        val header = "nope".toByteArray()
        assertEquals(AudioFormat.UNKNOWN, AudioFormatDetector.detect(header, "file.bin", "application/octet-stream"))
    }
}
