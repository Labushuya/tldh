package dev.bitsbots.tldh.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.bitsbots.tldh.ui.theme.TldhGlass

/**
 * Frosted glass in the spirit of macOS toolbars: a pane blurs whatever scrolls underneath it.
 *
 * The content to blur is marked with [glassSource]; it records itself into a graphics layer on
 * every draw. A [GlassPane] placed above it (as a sibling, never inside the source) draws that
 * layer again into its own blurred layer, clipped to its shape, then lays a tint and a lit
 * hairline on top.
 * Real-time blur needs Android 12 (RenderEffect); older versions get an almost opaque tint.
 */
@Stable
class GlassState internal constructor(internal val content: GraphicsLayer) {
    internal var origin by mutableStateOf(Offset.Zero)
    internal var size by mutableStateOf(IntSize.Zero)
}

@Composable
fun rememberGlassState(): GlassState {
    val content = rememberGraphicsLayer()
    return remember(content) { GlassState(content) }
}

val GlassBlurSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

fun Modifier.glassSource(state: GlassState): Modifier = this
    .onGloballyPositioned { coordinates ->
        state.origin = coordinates.positionInRoot()
        state.size = coordinates.size
    }
    .drawWithContent {
        state.content.record { this@drawWithContent.drawContent() }
        drawLayer(state.content)
    }

enum class GlassEdge { None, Outline, Top, Bottom }

/**
 * @param visibility 0..1, read while drawing: lets a bar turn to glass only once content scrolls
 *   underneath it, without recomposing on every scroll pixel.
 * @param invalidation read while drawing so the pane redraws when its source scrolls.
 */
@Composable
fun GlassPane(
    state: GlassState?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    tint: Color = TldhGlass.Tint,
    blurRadius: Dp = 28.dp,
    edge: GlassEdge = GlassEdge.Outline,
    edgeBrush: Brush = TldhGlass.PaneEdge,
    visibility: () -> Float = { 1f },
    invalidation: () -> Int = { 0 },
    content: @Composable BoxScope.() -> Unit
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    // Each pane owns its blurred copy, so panes fading at different rates don't share one alpha.
    val blurred = rememberGraphicsLayer()
    Box(
        modifier = modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .drawBehind {
                invalidation()
                val amount = visibility().coerceIn(0f, 1f)
                if (amount <= 0.001f) return@drawBehind
                val outline = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBehind)) }
                val source = state
                val blur = source != null && GlassBlurSupported && source.size.width > 0 && source.size.height > 0
                if (blur && source != null) {
                    val radius = blurRadius.toPx()
                    blurred.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                    blurred.alpha = amount
                    blurred.record(size = source.size) { drawLayer(source.content) }
                    clipPath(outline) {
                        translate(left = source.origin.x - origin.x, top = source.origin.y - origin.y) {
                            drawLayer(blurred)
                        }
                    }
                }
                val fill = if (blur) tint else tint.copy(alpha = maxOf(tint.alpha, 0.94f))
                drawPath(outline, fill.copy(alpha = fill.alpha * amount))
                val hairline = 1.dp.toPx()
                when (edge) {
                    GlassEdge.None -> Unit
                    GlassEdge.Outline -> drawPath(outline, edgeBrush, alpha = amount, style = Stroke(width = hairline))
                    GlassEdge.Top -> drawRect(
                        color = Color.White.copy(alpha = 0.08f * amount),
                        size = size.copy(height = hairline)
                    )
                    GlassEdge.Bottom -> drawRect(
                        color = Color.White.copy(alpha = 0.08f * amount),
                        topLeft = Offset(0f, size.height - hairline),
                        size = size.copy(height = hairline)
                    )
                }
            },
        content = content
    )
}
