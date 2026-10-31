package dev.bitsbots.tldh.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

/**
 * Plays a short slice of the session's prepared 16 kHz mono PCM16 file, so a single uncertain word
 * can be checked by ear without listening to the whole voice note.
 */
class SnippetPlayer {
    private var track: AudioTrack? = null

    suspend fun play(pcmFile: File, sampleRate: Int, startMs: Long, endMs: Long, paddingMs: Long = 600L) {
        val bytes = withContext(Dispatchers.IO) { readSlice(pcmFile, sampleRate, startMs - paddingMs, endMs + paddingMs) }
        if (bytes.isEmpty()) return
        stop()
        val newTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(bytes.size)
            .build()
        newTrack.write(bytes, 0, bytes.size)
        newTrack.play()
        track = newTrack
    }

    fun stop() {
        track?.let { runCatching { it.stop() }; it.release() }
        track = null
    }

    private fun readSlice(pcmFile: File, sampleRate: Int, fromMs: Long, toMs: Long): ByteArray {
        if (!pcmFile.isFile) return ByteArray(0)
        val bytesPerMs = sampleRate * 2 / 1000.0
        RandomAccessFile(pcmFile, "r").use { file ->
            val start = ((fromMs.coerceAtLeast(0L) * bytesPerMs).toLong() and 1L.inv()).coerceAtMost(file.length())
            val end = ((toMs * bytesPerMs).toLong() and 1L.inv()).coerceIn(start, file.length())
            val buffer = ByteArray((end - start).toInt())
            file.seek(start)
            file.readFully(buffer)
            return buffer
        }
    }
}
