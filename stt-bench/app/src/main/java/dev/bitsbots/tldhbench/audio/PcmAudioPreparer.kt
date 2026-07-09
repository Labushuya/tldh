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
import kotlin.math.sqrt

private const val TARGET_SAMPLE_RATE = 16_000
private const val TARGET_CHANNEL_COUNT = 1
private const val BYTES_PER_SAMPLE = 2

/**
 * Conservative preprocessing summary. The benchmark still evaluates speed against the
 * original input duration, but Vosk receives the reduced PCM stream.
 */
data class AudioPreprocessingInfo(
    val enabled: Boolean = true,
    val originalPcmDurationMs: Long? = null,
    val processedPcmDurationMs: Long? = null,
    val removedMs: Long = 0L,
    val removedPercent: Double = 0.0,
    val profileId: String = AudioPreparationProfiles.defaultProfile.id,
    val profileName: String = AudioPreparationProfiles.defaultProfile.displayName,
    val note: String = AudioPreparationProfiles.defaultProfile.description
)

data class PreparedPcmAudio(
    val file: File,
    val sampleRate: Int = TARGET_SAMPLE_RATE,
    val channelCount: Int = TARGET_CHANNEL_COUNT,
    val encoding: Int = android.media.AudioFormat.ENCODING_PCM_16BIT,
    val durationMs: Long?,
    val preprocessing: AudioPreprocessingInfo = AudioPreprocessingInfo(enabled = false)
)

internal class PcmAudioPreparer(
    private val context: Context,
    private val workDir: File
) {
    suspend fun prepare(
        uri: Uri,
        durationMs: Long?,
        profile: AudioPreparationProfile = AudioPreparationProfiles.defaultProfile
    ): PreparedPcmAudio = withContext(Dispatchers.IO) {
        workDir.mkdirs()
        val output = File(workDir, "tldh-transcription-${profile.id}-16k-mono.pcm")
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
            val resampled = resampleTo16kMono(decodedMono, sampleRate)
            val processed = applyPreparationProfile(resampled, profile)
            writePcm16(processed.samples, output)

            PreparedPcmAudio(
                file = output,
                durationMs = durationMs,
                preprocessing = processed.info
            )
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
        val frameBytes = BYTES_PER_SAMPLE * channelCount
        while (buffer.remaining() >= frameBytes) {
            var sum = 0
            for (channel in 0 until channelCount) {
                sum += buffer.getShort().toInt()
            }
            out.add((sum / channelCount).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
        }
    }

    private fun resampleTo16kMono(samples: ShortArray, inputSampleRate: Int): ShortArray {
        if (samples.isEmpty()) return samples
        if (inputSampleRate == TARGET_SAMPLE_RATE) return samples
        val ratio = inputSampleRate.toDouble() / TARGET_SAMPLE_RATE.toDouble()
        val outputCount = max(1, floor(samples.size / ratio).toInt())
        val out = ShortArray(outputCount)
        for (i in 0 until outputCount) {
            val sourcePosition = i * ratio
            val base = floor(sourcePosition).toInt().coerceIn(0, samples.lastIndex)
            val next = min(base + 1, samples.lastIndex)
            val fraction = sourcePosition - base
            val interpolated = samples[base] * (1.0 - fraction) + samples[next] * fraction
            out[i] = interpolated.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun writePcm16(samples: ShortArray, output: File) {
        FileOutputStream(output).use { stream ->
            val bytes = ByteArray(samples.size * BYTES_PER_SAMPLE)
            var offset = 0
            samples.forEach { sample ->
                bytes[offset++] = (sample.toInt() and 0xFF).toByte()
                bytes[offset++] = ((sample.toInt() shr 8) and 0xFF).toByte()
            }
            stream.write(bytes)
        }
    }


    private fun applyPreparationProfile(samples: ShortArray, profile: AudioPreparationProfile): ReducedPcm {
        val originalMs = durationMs(samples.size, TARGET_SAMPLE_RATE)
        if (samples.isEmpty()) {
            return ReducedPcm(
                samples,
                AudioPreprocessingInfo(
                    enabled = false,
                    originalPcmDurationMs = originalMs,
                    processedPcmDurationMs = originalMs,
                    removedMs = 0L,
                    profileId = profile.id,
                    profileName = profile.displayName,
                    note = profile.description
                )
            )
        }

        var working = samples
        if (profile.useVoiceBandFilter) working = VoiceBandFilter.apply(working)
        if (profile.normalizeRms) working = RmsNormalizer.normalize(working)

        return if (profile.useSilenceReduction) {
            NonSpeechReducer.reduce(working, profile = profile)
        } else {
            ReducedPcm(
                working,
                AudioPreprocessingInfo(
                    enabled = profile.normalizeRms || profile.useVoiceBandFilter,
                    originalPcmDurationMs = originalMs,
                    processedPcmDurationMs = durationMs(working.size, TARGET_SAMPLE_RATE),
                    removedMs = 0L,
                    removedPercent = 0.0,
                    profileId = profile.id,
                    profileName = profile.displayName,
                    note = profile.description
                )
            )
        }
    }

    private fun durationMs(sampleCount: Int, sampleRate: Int): Long =
        ((sampleCount.toDouble() / sampleRate.toDouble()) * 1000.0).toLong().coerceAtLeast(0L)

    private fun MediaFormat.optionalInt(key: String, fallback: Int): Int =
        if (containsKey(key)) getInteger(key) else fallback
}

private data class ReducedPcm(
    val samples: ShortArray,
    val info: AudioPreprocessingInfo
)


private object RmsNormalizer {
    private const val TARGET_RMS = 5_200.0
    private const val MAX_GAIN = 5.0

    fun normalize(samples: ShortArray): ShortArray {
        if (samples.isEmpty()) return samples
        var sum = 0.0
        samples.forEach { value ->
            val v = value.toDouble()
            sum += v * v
        }
        val rms = sqrt(sum / samples.size.toDouble()).coerceAtLeast(1.0)
        val gain = (TARGET_RMS / rms).coerceIn(0.35, MAX_GAIN)
        if (gain in 0.96..1.04) return samples
        return ShortArray(samples.size) { index ->
            (samples[index] * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }
}

private object VoiceBandFilter {
    /**
     * Lightweight speech-band shaping without external DSP dependencies:
     * - removes very low frequency rumble by subtracting a slow moving average,
     * - slightly smooths single-sample codec noise. This is deliberately mild.
     */
    fun apply(samples: ShortArray): ShortArray {
        if (samples.size < 5) return samples
        val out = ShortArray(samples.size)
        var slow = 0.0
        var previous = samples.first().toDouble()
        for (i in samples.indices) {
            val current = samples[i].toDouble()
            slow = slow * 0.995 + current * 0.005
            val highPassed = current - slow
            val smoothed = highPassed * 0.72 + previous * 0.28
            previous = highPassed
            out[i] = smoothed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }
}

/**
 * Conservative non-speech reduction:
 * - removes leading/trailing very quiet stretches,
 * - compresses longer internal silence/noise-floor stretches,
 * - keeps padding around detected speech to avoid cutting word edges,
 * - falls back to the original PCM when the gate would remove too much.
 */
private object NonSpeechReducer {
    private const val FRAME_MS = 20
    private const val MIN_REMOVED_MS_TO_REPORT = 250L

    fun reduce(
        samples: ShortArray,
        sampleRate: Int = TARGET_SAMPLE_RATE,
        profile: AudioPreparationProfile = AudioPreparationProfiles.defaultProfile
    ): ReducedPcm {
        val originalMs = durationMs(samples.size, sampleRate)
        if (samples.size < sampleRate || samples.isEmpty()) {
            return ReducedPcm(samples, AudioPreprocessingInfo(originalPcmDurationMs = originalMs, processedPcmDurationMs = originalMs))
        }

        val frameSize = max(1, sampleRate * FRAME_MS / 1000)
        val frameCount = (samples.size + frameSize - 1) / frameSize
        val rms = DoubleArray(frameCount)
        for (frame in 0 until frameCount) {
            val start = frame * frameSize
            val end = min(samples.size, start + frameSize)
            var sum = 0.0
            var count = 0
            for (i in start until end) {
                val value = samples[i].toDouble()
                sum += value * value
                count += 1
            }
            rms[frame] = if (count == 0) 0.0 else sqrt(sum / count.toDouble())
        }

        val sorted = rms.copyOf().also { it.sort() }
        val noiseFloor = sorted[(sorted.size * 0.25).toInt().coerceIn(0, sorted.lastIndex)]
        val thresholdMultiplier = if (profile.aggressiveSilenceReduction) 5.2 else 3.8
        val thresholdMin = if (profile.aggressiveSilenceReduction) 420.0 else 280.0
        val thresholdMax = if (profile.aggressiveSilenceReduction) 2_400.0 else 1_600.0
        val speechThreshold = max(thresholdMin, min(thresholdMax, noiseFloor * thresholdMultiplier))
        val keep = BooleanArray(frameCount) { rms[it] >= speechThreshold }
        val paddingMs = if (profile.aggressiveSilenceReduction) 120 else 180
        val minKeepRatio = if (profile.aggressiveSilenceReduction) 0.42 else 0.55
        val paddingFrames = max(1, paddingMs / FRAME_MS)
        val paddedKeep = BooleanArray(frameCount)
        keep.forEachIndexed { index, shouldKeep ->
            if (shouldKeep) {
                val from = max(0, index - paddingFrames)
                val to = min(frameCount - 1, index + paddingFrames)
                for (i in from..to) paddedKeep[i] = true
            }
        }

        val keptFrames = paddedKeep.count { it }
        if (keptFrames == 0 || keptFrames.toDouble() / frameCount.toDouble() < minKeepRatio) {
            return ReducedPcm(
                samples,
                AudioPreprocessingInfo(
                    originalPcmDurationMs = originalMs,
                    processedPcmDurationMs = originalMs,
                    removedMs = 0L,
                    profileId = profile.id,
                    profileName = profile.displayName,
                    note = profile.description
                )
            )
        }

        val out = ShortArrayBuilder(initialCapacity = keptFrames * frameSize)
        for (frame in 0 until frameCount) {
            if (!paddedKeep[frame]) continue
            val start = frame * frameSize
            val end = min(samples.size, start + frameSize)
            for (i in start until end) out.add(samples[i])
        }
        val reduced = out.toArray()
        val processedMs = durationMs(reduced.size, sampleRate)
        val removedMs = (originalMs - processedMs).coerceAtLeast(0L)
        val removedPercent = if (originalMs > 0L) removedMs * 100.0 / originalMs.toDouble() else 0.0
        val reportRemovedMs = if (removedMs >= MIN_REMOVED_MS_TO_REPORT) removedMs else 0L
        return ReducedPcm(
            samples = if (reportRemovedMs > 0L) reduced else samples,
            info = if (reportRemovedMs > 0L) {
                AudioPreprocessingInfo(
                    originalPcmDurationMs = originalMs,
                    processedPcmDurationMs = processedMs,
                    removedMs = removedMs,
                    removedPercent = removedPercent,
                    profileId = profile.id,
                    profileName = profile.displayName,
                    note = profile.description
                )
            } else {
                AudioPreprocessingInfo(
                    originalPcmDurationMs = originalMs,
                    processedPcmDurationMs = originalMs,
                    removedMs = 0L,
                    profileId = profile.id,
                    profileName = profile.displayName,
                    note = profile.description
                )
            }
        )
    }

    private fun durationMs(sampleCount: Int, sampleRate: Int): Long =
        ((sampleCount.toDouble() / sampleRate.toDouble()) * 1000.0).toLong().coerceAtLeast(0L)
}
