package dev.bitsbots.tldhbench.bench

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BenchmarkTargetsTest {
    @Test fun targetForShortAudio() {
        assertEquals(15_000L, BenchmarkTargets.targetFor(13_000L))
    }

    @Test fun targetForOneMinuteAudio() {
        assertEquals(30_000L, BenchmarkTargets.targetFor(60_000L))
    }

    @Test fun targetForThreeMinuteAudio() {
        assertEquals(120_000L, BenchmarkTargets.targetFor(180_000L))
    }

    @Test fun noTargetForLongAudio() {
        assertNull(BenchmarkTargets.targetFor(245_000L))
    }

    @Test fun passingVerdict() {
        assertTrue(BenchmarkTargets.verdict(13_000L, 5_000L).passed == true)
    }

    @Test fun failingVerdict() {
        assertFalse(BenchmarkTargets.verdict(60_000L, 40_000L).passed == true)
    }
}
