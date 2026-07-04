package dev.bitsbots.tldh.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioIngestPolicyTest {
    @Test
    fun acceptsWhatsappOggOpusWithinLimit() {
        val validation = AudioIngestPolicy.validate(metadata(format = AudioFormat.OGG_OPUS, sizeBytes = 6 * 1024, durationMs = 45_000))
        assertTrue(validation.accepted)
        assertTrue(validation.rejectReasons.isEmpty())
    }

    @Test
    fun warnsForLongAudioButAcceptsBelowHardLimit() {
        val validation = AudioIngestPolicy.validate(
            metadata(format = AudioFormat.OGG_OPUS, sizeBytes = 652 * 1024, durationMs = 4L * 60L * 1000L + 12_000L)
        )
        assertTrue(validation.accepted)
        assertTrue(validation.warnings.any { it.contains("länger als 3 Minuten") })
    }

    @Test
    fun rejectsAudioAboveHardDurationLimit() {
        val validation = AudioIngestPolicy.validate(
            metadata(format = AudioFormat.OGG_OPUS, sizeBytes = 4 * 1024 * 1024, durationMs = AudioIngestPolicy.HARD_DURATION_LIMIT_MS + 1)
        )
        assertFalse(validation.accepted)
        assertTrue(validation.rejectReasons.contains(AudioRejectReason.AUDIO_TOO_LONG))
    }

    @Test
    fun rejectsUnknownFormat() {
        val validation = AudioIngestPolicy.validate(metadata(format = AudioFormat.UNKNOWN, sizeBytes = 1024, durationMs = 20_000))
        assertFalse(validation.accepted)
        assertEquals(listOf(AudioRejectReason.UNSUPPORTED_FORMAT), validation.rejectReasons)
    }

    @Test
    fun rejectsOversizedAudio() {
        val validation = AudioIngestPolicy.validate(
            metadata(format = AudioFormat.OGG_OPUS, sizeBytes = AudioIngestPolicy.MAX_AUDIO_BYTES + 1, durationMs = 20_000)
        )
        assertFalse(validation.accepted)
        assertTrue(validation.rejectReasons.contains(AudioRejectReason.FILE_TOO_LARGE))
    }

    @Test
    fun formatsDurationAsMinutesAndSeconds() {
        assertEquals("4:12 min", AudioIngestPolicy.formatDuration(252_000))
        assertEquals("unbekannt", AudioIngestPolicy.formatDuration(null))
    }

    private fun metadata(format: AudioFormat, sizeBytes: Long?, durationMs: Long?) = AudioMetadata(
        uriString = "content://example/audio",
        displayName = "voice.opus",
        mimeType = "audio/ogg; codecs=opus",
        sizeBytes = sizeBytes,
        durationMs = durationMs,
        format = format,
        headerBytes = "OggS....OpusHead".toByteArray(Charsets.ISO_8859_1),
        extension = "opus",
        headerProbeBytes = 16,
        validation = AudioValidation(accepted = true, rejectReasons = emptyList(), warnings = emptyList())
    )
}
