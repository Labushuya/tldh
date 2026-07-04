package dev.bitsbots.tldh.audio

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioIngestPolicyTest {
    @Test
    fun acceptsWhatsappOggOpusWithinLimit() {
        val validation = AudioIngestPolicy.validate(metadata(format = AudioFormat.OGG_OPUS, sizeBytes = 6 * 1024))
        assertTrue(validation.accepted)
        assertTrue(validation.rejectReasons.isEmpty())
    }

    @Test
    fun rejectsUnknownFormat() {
        val validation = AudioIngestPolicy.validate(metadata(format = AudioFormat.UNKNOWN, sizeBytes = 1024))
        assertFalse(validation.accepted)
        assertEquals(listOf(AudioRejectReason.UNSUPPORTED_FORMAT), validation.rejectReasons)
    }

    @Test
    fun rejectsOversizedAudio() {
        val validation = AudioIngestPolicy.validate(
            metadata(format = AudioFormat.OGG_OPUS, sizeBytes = AudioIngestPolicy.MAX_AUDIO_BYTES + 1)
        )
        assertFalse(validation.accepted)
        assertTrue(validation.rejectReasons.contains(AudioRejectReason.FILE_TOO_LARGE))
    }

    private fun metadata(format: AudioFormat, sizeBytes: Long?) = AudioMetadata(
        uri = Uri.parse("content://example/audio"),
        displayName = "voice.opus",
        mimeType = "audio/ogg; codecs=opus",
        sizeBytes = sizeBytes,
        format = format,
        headerBytes = "OggS....OpusHead".toByteArray(Charsets.ISO_8859_1),
        extension = "opus",
        headerProbeBytes = 16,
        validation = AudioValidation(accepted = true, rejectReasons = emptyList(), warnings = emptyList())
    )
}
