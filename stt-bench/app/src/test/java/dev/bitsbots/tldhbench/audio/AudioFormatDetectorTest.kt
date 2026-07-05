package dev.bitsbots.tldhbench.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFormatDetectorTest {
    @Test fun detectsFlacReferenceAudioAsSupported() {
        val format = AudioFormatDetector.detect(
            header = "fLaCtest".encodeToByteArray(),
            displayName = "de_rhasspy-0016.flac",
            mimeType = "audio/flac"
        )
        assertEquals(AudioFormat.FLAC, format)
        assertTrue(format.isSupportedForMvp)
    }

    @Test fun detectsWaveReferenceAudioAsSupported() {
        val header = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0, 0, 0, 0,
            'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte()
        )
        val format = AudioFormatDetector.detect(header, "sample.wav", "audio/wav")
        assertEquals(AudioFormat.WAV_PCM, format)
        assertTrue(format.isSupportedForMvp)
    }

    @Test fun keepsWebmUnsupportedForNow() {
        val format = AudioFormatDetector.detect(
            header = byteArrayOf(0x1A.toByte(), 0x45.toByte(), 0xDF.toByte(), 0xA3.toByte()),
            displayName = "sample.webm",
            mimeType = "audio/webm; codecs=opus"
        )
        assertEquals(AudioFormat.WEBM_OPUS, format)
    }
}
