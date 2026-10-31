package dev.bitsbots.tldh.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.bitsbots.tldh.ui.components.MarkStyle
import dev.bitsbots.tldh.ui.components.MarkedText
import dev.bitsbots.tldh.ui.components.TextMark
import dev.bitsbots.tldh.ui.theme.TldhTheme
import dev.bitsbots.tldh.ui.theme.TldhType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tapping a marked (uncertain) word must report exactly that word, so its moment can be played. */
@RunWith(AndroidJUnit4::class)
class MarkedTextTapTest {
    @get:Rule
    val compose = createComposeRule()

    private val text = "aaaaaa bbbbbb cccccc"
    private val marks = listOf(
        TextMark(0, 6, MarkStyle.Dotted, tag = 0),
        TextMark(7, 13, MarkStyle.Wavy, tag = 1),
        TextMark(14, 20, MarkStyle.Dotted, tag = 2)
    )

    @Test
    fun tapsHitTheWordUnderTheFinger() {
        val tapped = mutableListOf<Int>()
        compose.setContent {
            TldhTheme {
                // Wraps its content, so the node is exactly as wide as the single line of text.
                MarkedText(
                    text = AnnotatedString(text),
                    marks = marks,
                    style = TldhType.Transcript,
                    modifier = Modifier.testTag("text"),
                    onMarkTap = { tapped += it.tag }
                )
            }
        }
        val node = compose.onNodeWithTag("text")
        // Three equally long words left to right: their centres are at about 15 %, 50 % and 85 %.
        listOf(0.15f, 0.5f, 0.85f).forEach { fraction ->
            node.performTouchInput { click(Offset(width * fraction, height / 2f)) }
            compose.waitForIdle()
        }
        assertEquals(listOf(0, 1, 2), tapped)
    }

    @Test
    fun tapsOutsideMarkedWordsDoNothing() {
        val tapped = mutableListOf<Int>()
        compose.setContent {
            TldhTheme {
                MarkedText(
                    text = AnnotatedString("sicher sicher sicher"),
                    marks = emptyList(),
                    style = TldhType.Transcript,
                    modifier = Modifier.testTag("plain"),
                    onMarkTap = { tapped += it.tag }
                )
            }
        }
        compose.onNodeWithTag("plain").performTouchInput { click(center) }
        compose.waitForIdle()
        assertEquals(emptyList<Int>(), tapped)
    }
}
