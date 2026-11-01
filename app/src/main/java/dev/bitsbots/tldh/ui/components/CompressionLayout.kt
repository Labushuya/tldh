package dev.bitsbots.tldh.ui.components

/** One bar of the processing waveform in pixels; [processed] is 0 (not yet) .. 1 (transcribed). */
data class BarRect(val x: Float, val width: Float, val height: Float, val processed: Float)

data class CompressionFrame(
    val bars: List<BarRect>,
    /** Where whisper currently is, between the transcribed strip and the remaining audio. */
    val frontierX: Float,
    /** Centre of the mark's dot, drawn while the strip turns into the mark. */
    val dotX: Float
)

/** Sizes in pixels, so the layout stays independent of Compose and unit-testable. */
data class CompressionSizes(
    val pitch: Float,
    val barWidth: Float,
    val compressedPitch: Float,
    val compressedWidth: Float,
    val compressedHeightScale: Float,
    val markBarWidth: Float,
    val markGap: Float,
    val markHeights: List<Float>,
    val dotGap: Float,
    val dotRadius: Float
)

/**
 * "Long story, short" as motion: bars not yet transcribed keep their full spacing, transcribed bars
 * are squeezed into a dense strip, so the line gets shorter as whisper advances. The line stays
 * centred. With [morph] going from 0 to 1 at the end, the strip converges onto the three bars of the
 * tl;dh mark and the dot appears.
 */
object CompressionLayout {
    fun frame(heights: List<Float>, progress: Float, morph: Float, width: Float, sizes: CompressionSizes): CompressionFrame {
        val count = heights.size
        if (count == 0) return CompressionFrame(emptyList(), width / 2f, width / 2f)
        val done = progress.coerceIn(0f, 1f) * count
        val squeeze = FloatArray(count) { (done - it).coerceIn(0f, 1f) }
        val pitches = FloatArray(count) { lerp(sizes.pitch, sizes.compressedPitch, squeeze[it]) }
        val widths = FloatArray(count) { lerp(sizes.barWidth, sizes.compressedWidth, squeeze[it]) }
        val total = pitches.sum() - (pitches[count - 1] - widths[count - 1])
        val left = (width - total) / 2f

        val markWidth = 3 * sizes.markBarWidth + 2 * sizes.markGap + sizes.dotGap + 2 * sizes.dotRadius
        val markLeft = (width - markWidth) / 2f
        val m = morph.coerceIn(0f, 1f)

        var x = left
        var frontier = left
        val bars = ArrayList<BarRect>(count)
        for (i in 0 until count) {
            val height = heights[i] * lerp(1f, sizes.compressedHeightScale, squeeze[i])
            val group = minOf(2, i * 3 / count)
            val targetX = markLeft + group * (sizes.markBarWidth + sizes.markGap)
            bars += BarRect(
                x = lerp(x, targetX, m),
                width = lerp(widths[i], sizes.markBarWidth, m),
                height = lerp(height, sizes.markHeights[group], m),
                processed = squeeze[i]
            )
            if (squeeze[i] > 0f) frontier = x + widths[i] + (pitches[i] - widths[i]) / 2f
            x += pitches[i]
        }
        val dotX = markLeft + 3 * sizes.markBarWidth + 2 * sizes.markGap + sizes.dotGap + sizes.dotRadius
        return CompressionFrame(bars, frontier, dotX)
    }

    private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t
}
