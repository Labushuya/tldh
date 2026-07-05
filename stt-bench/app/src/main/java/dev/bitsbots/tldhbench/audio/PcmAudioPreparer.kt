package dev.bitsbots.tldhbench.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal data class PreparedPcmAudio(
    val file: File,
    val sampleRate: Int = TARGET_SAMPLE_RATE,
    val channelCount: Int = TARGET_CHANNEL_COUNT,
    val encoding: Int = android.media.AudioFormat.ENCODING_PCM_16BIT,
    val durationMs: Long?
)

private const val TARGET_SAMPLE_RATE = 16_000
private const val TARGET_CHANNEL_COUNT = 1

internal class PcmAudioPreparer(
    private val context: Context,
    private val workDir: File
) {
    suspend fun prepare(uri: Uri, durationMs: Long?): PreparedPcmAudio = withContext(Dispatchers.IO) {
        workDir.mkdirs()
        val output = File(workDir, "tldh-transcription-16k-mono.pcm")
        output.delete()

        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = findAudioTrack(extractor)
                ?: throw IllegalStateException("Keine Audiospur gefunden.")
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalStateException("Audiospur hat keinen MIME-Type.")
            extractor.selectTrack(trackIndex)

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            val decodedMono = decodeMonoPcm16(extractor, decoder, durationMs)
            val sampleRate = currentSampleRate ?: inputFormat.optionalInt(MediaFormat.KEY_SAMPLE_RATE, TARGET_SAMPLE_RATE)
            writeResampled16kMono(decodedMono, sampleRate, output)

            PreparedPcmAudio(file = output, durationMs = durationMs)
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { extractor.release() }
        }
    }

    private var currentSampleRate: Int? = null
    private var currentChannelCount: Int? = null

    private fun findAudioTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) return i
        }
        return null
    }

    private fun decodeMonoPcm16(
        extractor: MediaExtractor,
        decoder: MediaCodec,
        durationMs: Long?
    ): ShortArray {
        val builder = ShortArrayBuilder(initialCapacity = max(16_384, ((durationMs ?: 60_000L) / 1000L * TARGET_SAMPLE_RATE).toInt()))
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var channelCount = 1

        while (!outputDone) {
            if (!inputDone) {
                val inputIndex = decoder.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputIndex)
                    inputBuffer?.clear()
                    val sampleSize = inputBuffer?.let { extractor.readSampleData(it, 0) } ?: -1
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            when (val outputIndex = decoder.dequeueOutputBuffer(info, 10_000)) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val format = decoder.outputFormat
                    currentSampleRate = format.optionalInt(MediaFormat.KEY_SAMPLE_RATE, TARGET_SAMPLE_RATE)
                    channelCount = format.optionalInt(MediaFormat.KEY_CHANNEL_COUNT, 1).coerceAtLeast(1)
                    currentChannelCount = channelCount
                }
                else -> if (outputIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        outputBuffer.position(info.offset)
                        outputBuffer.limit(info.offset + info.size)
                        appendMonoPcm16(outputBuffer.slice(), channelCount, builder)
                    }
                    outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    decoder.releaseOutputBuffer(outputIndex, false)
                }
            }
        }
        return builder.toArray()
    }

    private fun appendMonoPcm16(buffer: ByteBuffer, channelCount: Int, out: ShortArrayBuilder) {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val frameBytes = 2 * channelCount
        while (buffer.remaining() >= frameBytes) {
            var sum = 0
            for (channel in 0 until channelCount) {
                sum += buffer.getShort().toInt()
            }
            out.add((sum / channelCount).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
        }
    }

    private fun writeResampled16kMono(samples: ShortArray, inputSampleRate: Int, output: File) {
        FileOutputStream(output).use { stream ->
            if (samples.isEmpty()) return@use
            if (inputSampleRate == TARGET_SAMPLE_RATE) {
                val bytes = ByteArray(samples.size * 2)
                var offset = 0
                samples.forEach { sample ->
                    bytes[offset++] = (sample.toInt() and 0xFF).toByte()
                    bytes[offset++] = ((sample.toInt() shr 8) and 0xFF).toByte()
                }
                stream.write(bytes)
                return@use
            }

            val ratio = inputSampleRate.toDouble() / TARGET_SAMPLE_RATE.toDouble()
            val outputCount = max(1, floor(samples.size / ratio).toInt())
            val bytes = ByteArray(outputCount * 2)
            var offset = 0
            for (i in 0 until outputCount) {
                val sourcePosition = i * ratio
                val base = floor(sourcePosition).toInt().coerceIn(0, samples.lastIndex)
                val next = min(base + 1, samples.lastIndex)
                val fraction = sourcePosition - base
                val interpolated = samples[base] * (1.0 - fraction) + samples[next] * fraction
                val sample = interpolated.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                bytes[offset++] = (sample and 0xFF).toByte()
                bytes[offset++] = ((sample shr 8) and 0xFF).toByte()
            }
            stream.write(bytes)
        }
    }

    private fun MediaFormat.optionalInt(key: String, fallback: Int): Int =
        if (containsKey(key)) getInteger(key) else fallback
}
