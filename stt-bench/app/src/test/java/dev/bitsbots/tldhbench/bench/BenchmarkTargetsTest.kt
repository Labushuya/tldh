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

    @Test fun targetForFourMinuteAudioUsesRealtimeLongformTarget() {
        assertEquals(245_000L, BenchmarkTargets.targetFor(245_000L))
    }

    @Test fun targetForSixMinuteAudioUsesRealtimeLongformTarget() {
        assertEquals(360_000L, BenchmarkTargets.targetFor(360_000L))
    }

    @Test fun noTargetBeyondSixTwentyAudio() {
        assertNull(BenchmarkTargets.targetFor(381_000L))
    }

    @Test fun passingVerdict() {
        assertTrue(BenchmarkTargets.verdict(13_000L, 5_000L).passed == true)
    }

    @Test fun failingVerdict() {
        assertFalse(BenchmarkTargets.verdict(60_000L, 40_000L).passed == true)
    }

    @Test fun longformPassingVerdictUsesRealtimeTarget() {
        val verdict = BenchmarkTargets.verdict(245_000L, 100_000L)
        assertEquals(245_000L, verdict.targetMs)
        assertTrue(verdict.passed == true)
        assertTrue(verdict.message.contains("Longform bestanden"))
    }

    @Test fun longformFailingVerdictUsesRealtimeTarget() {
        val verdict = BenchmarkTargets.verdict(245_000L, 300_000L)
        assertEquals(245_000L, verdict.targetMs)
        assertFalse(verdict.passed == true)
        assertTrue(verdict.message.contains("Longform nicht bestanden"))
    }
}
