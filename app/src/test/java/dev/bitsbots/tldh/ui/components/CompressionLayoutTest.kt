package dev.bitsbots.tldh.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressionLayoutTest {
    private val sizes = CompressionSizes(
        pitch = 6f,
        barWidth = 3f,
        compressedPitch = 2.4f,
        compressedWidth = 1.6f,
        compressedHeightScale = 0.62f,
        markBarWidth = 6f,
        markGap = 3f,
        markHeights = listOf(44f, 28f, 14f),
        dotGap = 4f,
        dotRadius = 3.5f
    )
    private val width = 360f
    private val heights = List(60) { 20f + (it % 7) * 5f }

    private fun span(frame: CompressionFrame): Pair<Float, Float> {
        val first = frame.bars.first()
        val last = frame.bars.last()
        return first.x to last.x + last.width
    }

    @Test
    fun untouchedLineKeepsFullSpacingAndIsCentred() {
        val frame = CompressionLayout.frame(heights, progress = 0f, morph = 0f, width = width, sizes = sizes)
        val (left, right) = span(frame)
        val expected = 60 * 6f - (6f - 3f)
        assertEquals(expected, right - left, 0.01f)
        assertEquals(width - right, left, 0.01f)
        assertTrue(frame.bars.all { it.processed == 0f && it.width == 3f })
        assertEquals(heights, frame.bars.map { it.height })
    }

    @Test
    fun finishedLineIsFullyCompressed() {
        val frame = CompressionLayout.frame(heights, progress = 1f, morph = 0f, width = width, sizes = sizes)
        val (left, right) = span(frame)
        assertEquals(60 * 2.4f - (2.4f - 1.6f), right - left, 0.01f)
        assertEquals(width - right, left, 0.01f)
        assertTrue(frame.bars.all { it.processed == 1f })
        frame.bars.forEachIndexed { i, bar -> assertEquals(heights[i] * 0.62f, bar.height, 0.01f) }
    }

    @Test
    fun lineShortensAsWhisperAdvances() {
        var previous = Float.MAX_VALUE
        for (step in 0..20) {
            val (left, right) = span(CompressionLayout.frame(heights, step / 20f, 0f, width, sizes))
            assertTrue("step $step", right - left < previous)
            previous = right - left
        }
    }

    @Test
    fun frontierSitsBetweenTranscribedAndRemainingBars() {
        val frame = CompressionLayout.frame(heights, progress = 0.5f, morph = 0f, width = width, sizes = sizes)
        val done = frame.bars.take(30)
        val open = frame.bars.drop(30)
        assertTrue(done.all { it.processed == 1f })
        assertTrue(open.all { it.processed == 0f })
        assertTrue(frame.frontierX > done.last().x + done.last().width)
        assertTrue(frame.frontierX < open.first().x)
    }

    @Test
    fun morphTurnsTheStripIntoTheMark() {
        val frame = CompressionLayout.frame(heights, progress = 1f, morph = 1f, width = width, sizes = sizes)
        val columns = frame.bars.map { it.x }.distinct().sorted()
        assertEquals(3, columns.size)
        assertEquals(6f + 3f, columns[1] - columns[0], 0.01f)
        assertEquals(6f + 3f, columns[2] - columns[1], 0.01f)
        assertTrue(frame.bars.all { it.width == 6f })
        assertEquals(listOf(44f, 28f, 14f), frame.bars.map { it.height }.distinct())
        // Bars, gaps and dot together are centred.
        val markWidth = 3 * 6f + 2 * 3f + 4f + 2 * 3.5f
        assertEquals((width - markWidth) / 2f, columns[0], 0.01f)
        assertEquals(columns[2] + 6f + 4f + 3.5f, frame.dotX, 0.01f)
    }

    @Test
    fun emptyLevelsGiveAnEmptyFrame() {
        val frame = CompressionLayout.frame(emptyList(), progress = 0.4f, morph = 0f, width = width, sizes = sizes)
        assertTrue(frame.bars.isEmpty())
        assertEquals(width / 2f, frame.frontierX, 0.01f)
    }
}
