package dev.bitsbots.tldh.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Line icons on a 24 grid, drawn for tl;dh (no icon font, no emoji). Strokes are tinted by the
 * Icon composable.
 */
object TldhIcons {
    val Close = icon("M6 6l12 12M18 6L6 18", stroke = 1.9f)
    val Back = icon("M15 5l-7 7 7 7", stroke = 1.9f)
    val Settings = icon("M4 7h10M18 7h2M4 17h4M12 17h8", circle(16f, 7f, 2f), circle(10f, 17f, 2f))
    val FileAudio = icon(
        "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z",
        "M14 3v5h5",
        "M10 12v5M13 14v3M7 15v2",
        stroke = 1.9f
    )
    val Chat = icon("M20 12a8 8 0 0 1-11.6 7.1L4 20l.9-4.4A8 8 0 1 1 20 12z")
    val Share = icon(circle(18f, 5f, 2.5f), circle(6f, 12f, 2.5f), circle(18f, 19f, 2.5f), "M8.2 10.8l7.6-4.4M8.2 13.2l7.6 4.4")
    val AppTile = icon("M7.5 1h9A6.5 6.5 0 0 1 23 7.5v9a6.5 6.5 0 0 1-6.5 6.5h-9A6.5 6.5 0 0 1 1 16.5v-9A6.5 6.5 0 0 1 7.5 1z", "M6.1 8v8M10.1 10v4M14.1 11.5v1", circle(18.2f, 12f, 0.6f), stroke = 1.6f)
    val Check = icon("M5 12.5l4.5 4.5L19 7.5", stroke = 2f)
    val Copy = icon("M10.5 8h7A2.5 2.5 0 0 1 20 10.5v7a2.5 2.5 0 0 1-2.5 2.5h-7A2.5 2.5 0 0 1 8 17.5v-7A2.5 2.5 0 0 1 10.5 8z", "M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2")
    val Note = icon("M6 3h9l4 4v14H6z", "M9 11h7M9 15h7")
    val Warning = icon("M12 4l9 16H3z", "M12 10v4M12 17.2v.1", stroke = 1.9f)
    val Trash = icon("M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13", stroke = 1.9f)
    val ChevronDown = icon("M6 9l6 6 6-6", stroke = 2f)
    val ChevronRight = icon("M9 5l7 7-7 7")
    val External = icon("M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5")
    val Phone = icon("M9 2.5h6a3 3 0 0 1 3 3v13a3 3 0 0 1-3 3H9a3 3 0 0 1-3-3v-13a3 3 0 0 1 3-3z", "M10.5 18.5h3")
    val NoAccount = icon(circle(12f, 8f, 3.5f), "M5 20c1.2-3.6 4-5.5 7-5.5s5.8 1.9 7 5.5", "M3 3l18 18")
    val Download = icon("M12 4v11M7.5 10.5L12 15l4.5-4.5", "M5 19.5h14", stroke = 1.9f)
    val Play = filled("M7 4.5v15l12-7.5z")
    val Stop = filled("M7 6.5A1.5 1.5 0 0 1 8.5 5h7A1.5 1.5 0 0 1 17 6.5v11a1.5 1.5 0 0 1-1.5 1.5h-7A1.5 1.5 0 0 1 7 17.5z")

    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

    private fun icon(vararg paths: String, stroke: Float = 1.8f): ImageVector {
        val builder = ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        paths.forEach { data ->
            builder.addPath(
                pathData = PathParser().parsePathString(data).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = stroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return builder.build()
    }

    private fun filled(data: String): ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = PathParser().parsePathString(data).toNodes(),
        fill = SolidColor(Color.Black)
    ).build()
}
