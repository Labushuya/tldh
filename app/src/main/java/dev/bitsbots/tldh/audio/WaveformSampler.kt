package dev.bitsbots.tldh.audio

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import kotlin.math.sqrt

/**
 * Loudness envelope of the session's prepared 16-bit mono PCM, drawn as the waveform while the
 * voice note is transcribed. Reads the file in a single streaming pass.
 */
object WaveformSampler {
    private const val MIN_LEVEL = 0.08f

    fun levels(pcmFile: File, bars: Int): List<Float> {
        val totalSamples = pcmFile.length() / 2
        if (bars <= 0 || totalSamples <= 0L) return emptyList()
        val accumulator = Accumulator(bars, samplesPerBar(totalSamples, bars))
        BufferedInputStream(FileInputStream(pcmFile), BUFFER_BYTES).use { input ->
            val buffer = ByteArray(BUFFER_BYTES)
            var low = -1
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                for (i in 0 until read) {
                    val byte = buffer[i].toInt() and 0xFF
                    if (low < 0) {
                        low = byte
                    } else {
                        accumulator.add(((byte shl 8) or low).toShort())
                        low = -1
                    }
                }
            }
        }
        return accumulator.finish()
    }

    fun levelsFromSamples(samples: ShortArray, bars: Int): List<Float> {
        if (bars <= 0 || samples.isEmpty()) return emptyList()
        val accumulator = Accumulator(bars, samplesPerBar(samples.size.toLong(), bars))
        samples.forEach(accumulator::add)
        return accumulator.finish()
    }

    private fun samplesPerBar(totalSamples: Long, bars: Int): Long = maxOf(1L, (totalSamples + bars - 1) / bars)

    private class Accumulator(private val bars: Int, private val perBar: Long) {
        private val rms = FloatArray(bars)
        private var bar = 0
        private var sum = 0.0
        private var count = 0L

        fun add(sample: Short) {
            val value = sample / 32768.0
            sum += value * value
            count++
            if (count == perBar) flush()
        }

        private fun flush() {
            if (bar < bars && count > 0) rms[bar++] = sqrt(sum / count).toFloat()
            sum = 0.0
            count = 0
        }

        fun finish(): List<Float> {
            flush()
            val loudest = rms.maxOrNull() ?: 0f
            if (loudest <= 0f) return List(bars) { MIN_LEVEL }
            // Square root compresses the range so quiet speech still shows as a visible bar.
            return rms.map { sqrt(it / loudest).coerceIn(MIN_LEVEL, 1f) }
        }
    }

    private const val BUFFER_BYTES = 64 * 1024
}
