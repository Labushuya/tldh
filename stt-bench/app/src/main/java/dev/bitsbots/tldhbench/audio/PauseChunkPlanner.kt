package dev.bitsbots.tldhbench.audio

import kotlin.math.log10
import kotlin.math.sqrt

data class PcmChunk(val startSample: Int, val endSample: Int) {
    val sampleCount: Int get() = endSample - startSample
}

data class PcmChunkPlan(
    val chunks: List<PcmChunk>,
    /** Cuts placed mid-speech because no pause was found within the maximum chunk length. */
    val hardCutCount: Int
)

/**
 * Splits mono PCM16 into pieces of at most [maxChunkMs], cutting in the middle of pauses.
 *
 * Whistle windows audio beyond 30 s itself, but in the desktop test that internal windowing dropped
 * whole sentences at a window boundary. Cutting at pauses on the app side avoids splitting words and
 * keeps every piece inside one engine pass.
 */
object PauseChunkPlanner {
    fun plan(
        samples: ShortArray,
        sampleRate: Int,
        maxChunkMs: Long = 25_000L,
        minPauseMs: Long = 250L,
        silenceDbfs: Double = -40.0,
        frameMs: Int = 20
    ): PcmChunkPlan {
        if (samples.isEmpty()) return PcmChunkPlan(emptyList(), 0)
        val maxSamples = (maxChunkMs * sampleRate / 1000L).toInt().coerceAtLeast(1)
        if (samples.size <= maxSamples) return PcmChunkPlan(listOf(PcmChunk(0, samples.size)), 0)

        val pauseCuts = pauseMidpoints(samples, sampleRate, minPauseMs, silenceDbfs, frameMs)
        val cuts = mutableListOf(0)
        var hardCuts = 0
        var position = 0
        var pauseIndex = 0
        while (samples.size - position > maxSamples) {
            val limit = position + maxSamples
            var next = -1
            while (pauseIndex < pauseCuts.size && pauseCuts[pauseIndex] <= limit) {
                if (pauseCuts[pauseIndex] > position) next = pauseCuts[pauseIndex]
                pauseIndex++
            }
            if (next < 0) {
                next = limit
                hardCuts++
            }
            cuts += next
            position = next
        }
        cuts += samples.size
        val chunks = cuts.zipWithNext { start, end -> PcmChunk(start, end) }.filter { it.sampleCount > 0 }
        return PcmChunkPlan(chunks, hardCuts)
    }

    private fun pauseMidpoints(
        samples: ShortArray,
        sampleRate: Int,
        minPauseMs: Long,
        silenceDbfs: Double,
        frameMs: Int
    ): List<Int> {
        val frame = (sampleRate * frameMs / 1000).coerceAtLeast(1)
        val frameCount = samples.size / frame
        val minPauseFrames = (minPauseMs / frameMs).toInt().coerceAtLeast(1)
        val midpoints = mutableListOf<Int>()
        var runStart = -1
        for (index in 0..frameCount) {
            val quiet = index < frameCount && frameDbfs(samples, index * frame, frame) < silenceDbfs
            if (quiet && runStart < 0) {
                runStart = index
            } else if (!quiet && runStart >= 0) {
                if (index - runStart >= minPauseFrames) midpoints += (runStart + index) / 2 * frame
                runStart = -1
            }
        }
        return midpoints
    }

    private fun frameDbfs(samples: ShortArray, offset: Int, length: Int): Double {
        var sum = 0.0
        for (i in offset until offset + length) {
            val value = samples[i].toDouble()
            sum += value * value
        }
        val rms = sqrt(sum / length)
        return 20.0 * log10(rms / 32768.0 + 1e-12)
    }
}
