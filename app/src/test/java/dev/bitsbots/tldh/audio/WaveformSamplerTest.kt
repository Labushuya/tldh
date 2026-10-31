package dev.bitsbots.tldh.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WaveformSamplerTest {
    @Test
    fun loudPartsGetTallerBars() {
        val quiet = ShortArray(1_000) { if (it % 2 == 0) 300 else -300 }
        val loud = ShortArray(1_000) { if (it % 2 == 0) 12_000 else -12_000 }
        val levels = WaveformSampler.levelsFromSamples(quiet + loud, bars = 2)
        assertEquals(2, levels.size)
        assertEquals(1f, levels[1], 0.0001f)
        assertTrue(levels[0] < 0.3f)
        assertTrue(levels[0] >= 0.08f)
    }

    @Test
    fun silenceStillDrawsMinimalBars() {
        val levels = WaveformSampler.levelsFromSamples(ShortArray(500), bars = 4)
        assertEquals(List(4) { 0.08f }, levels)
    }

    @Test
    fun fileAndArrayAgree() {
        val samples = ShortArray(4_321) { (kotlin.math.sin(it / 40.0) * 8_000 * (it % 700) / 700).toInt().toShort() }
        val file = File.createTempFile("tldh-wave", ".pcm")
        try {
            val buffer = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            samples.forEach { buffer.putShort(it) }
            file.writeBytes(buffer.array())
            val fromFile = WaveformSampler.levels(file, bars = 48)
            val fromArray = WaveformSampler.levelsFromSamples(samples, bars = 48)
            assertEquals(fromArray.size, fromFile.size)
            fromArray.indices.forEach { assertEquals(fromArray[it], fromFile[it], 0.0001f) }
        } finally {
            file.delete()
        }
    }
}
