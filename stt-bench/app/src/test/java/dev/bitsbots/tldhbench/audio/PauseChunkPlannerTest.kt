package dev.bitsbots.tldhbench.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class PauseChunkPlannerTest {
    private val rate = 16_000

    private fun speech(ms: Int): ShortArray =
        ShortArray(rate * ms / 1000) { i -> (8_000 * sin(2.0 * PI * 220.0 * i / rate)).toInt().toShort() }

    private fun pause(ms: Int): ShortArray = ShortArray(rate * ms / 1000)

    private fun join(vararg parts: ShortArray): ShortArray = parts.fold(ShortArray(0)) { acc, part -> acc + part }

    private fun assertCoversContiguously(plan: PcmChunkPlan, totalSamples: Int) {
        assertEquals(0, plan.chunks.first().startSample)
        assertEquals(totalSamples, plan.chunks.last().endSample)
        plan.chunks.zipWithNext { a, b -> assertEquals(a.endSample, b.startSample) }
    }

    @Test fun emptyAudioHasNoChunks() {
        val plan = PauseChunkPlanner.plan(ShortArray(0), rate)
        assertTrue(plan.chunks.isEmpty())
        assertEquals(0, plan.hardCutCount)
    }

    @Test fun shortAudioStaysOneChunk() {
        val samples = speech(10_000)
        val plan = PauseChunkPlanner.plan(samples, rate)
        assertEquals(listOf(PcmChunk(0, samples.size)), plan.chunks)
        assertEquals(0, plan.hardCutCount)
    }

    @Test fun longAudioIsCutInsidePauses() {
        // 6 × (12 s speech + 400 ms pause) ≈ 74 s
        val parts = (0 until 6).flatMap { listOf(speech(12_000), pause(400)) }.toTypedArray()
        val samples = join(*parts)
        val plan = PauseChunkPlanner.plan(samples, rate, maxChunkMs = 25_000L)

        assertCoversContiguously(plan, samples.size)
        assertEquals(0, plan.hardCutCount)
        assertTrue(plan.chunks.all { it.sampleCount <= rate * 25 })
        plan.chunks.dropLast(1).forEach { chunk ->
            // Every cut lands in silence, never in the middle of the sine "speech".
            assertEquals(0, samples[chunk.endSample].toInt())
        }
        // Greedy packing keeps two 12.4 s sentences per chunk instead of cutting after each one.
        assertEquals(3, plan.chunks.size)
    }

    @Test fun audioWithoutPausesFallsBackToHardCuts() {
        val samples = speech(60_000)
        val plan = PauseChunkPlanner.plan(samples, rate, maxChunkMs = 25_000L)

        assertCoversContiguously(plan, samples.size)
        assertEquals(2, plan.hardCutCount)
        assertEquals(listOf(rate * 25, rate * 25, rate * 10), plan.chunks.map { it.sampleCount })
    }

    @Test fun pausesShorterThanMinimumAreIgnored() {
        // 100 ms gaps are word gaps, not sentence pauses: 30 s with only short gaps needs one hard cut.
        val parts = (0 until 10).flatMap { listOf(speech(2_900), pause(100)) }.toTypedArray()
        val samples = join(*parts)
        val plan = PauseChunkPlanner.plan(samples, rate, maxChunkMs = 25_000L, minPauseMs = 250L)

        assertCoversContiguously(plan, samples.size)
        assertEquals(1, plan.hardCutCount)
    }
}
