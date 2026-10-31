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
    fun detectsWebmOpus() {
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

    @Test
    fun detectsFilesPickedFromStorage() {
        val id3 = "ID3".toByteArray(Charsets.ISO_8859_1) + ByteArray(8)
        assertEquals(AudioFormat.MP3, AudioFormatDetector.detect(id3, "memo.mp3", "audio/mpeg"))
        val mpegFrame = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64)
        assertEquals(AudioFormat.MP3, AudioFormatDetector.detect(mpegFrame, null, null))
        val adts = byteArrayOf(0xFF.toByte(), 0xF1.toByte(), 0x50, 0x80.toByte())
        assertEquals(AudioFormat.AAC_ADTS, AudioFormatDetector.detect(adts, null, null))
        val wav = "RIFF".toByteArray(Charsets.ISO_8859_1) + byteArrayOf(0x24, 0, 0, 0) + "WAVEfmt ".toByteArray(Charsets.ISO_8859_1)
        assertEquals(AudioFormat.WAV, AudioFormatDetector.detect(wav, "aufnahme.wav", "audio/x-wav"))
        assertEquals(AudioFormat.FLAC, AudioFormatDetector.detect("fLaC....".toByteArray(Charsets.ISO_8859_1), null, null))
        assertEquals(AudioFormat.AMR, AudioFormatDetector.detect("#!AMR\n".toByteArray(Charsets.ISO_8859_1), null, null))
    }

    @Test
    fun everyKnownFormatIsSupported() {
        AudioFormat.entries.forEach { format ->
            assertEquals(format.name, format != AudioFormat.UNKNOWN, format.isSupported)
        }
    }
}
