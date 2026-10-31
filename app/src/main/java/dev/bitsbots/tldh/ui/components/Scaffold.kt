package dev.bitsbots.tldh.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.bitsbots.tldh.ui.theme.LocalReducedMotion
import dev.bitsbots.tldh.ui.theme.TldhColors
import dev.bitsbots.tldh.ui.theme.TldhGlass
import dev.bitsbots.tldh.ui.theme.TldhMotion
import dev.bitsbots.tldh.ui.theme.TldhType

/**
 * Screen frame: scrolling content with an optional top bar and bottom dock. Both float above the
 * content and turn to frosted glass only while content is actually underneath them.
 */
@Composable
fun GlassScaffold(
    modifier: Modifier = Modifier,
    glass: GlassState = rememberGlassState(),
    scrollState: ScrollState = rememberScrollState(),
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    var topHeight by remember { mutableIntStateOf(0) }
    var bottomHeight by remember { mutableIntStateOf(0) }
    val fadeDistance = with(density) { 16.dp.toPx() }
    Box(
        modifier
            .fillMaxSize()
            .background(TldhColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .glassSource(glass)
                .verticalScroll(scrollState)
                .then(
                    if (topBar == null) Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    else Modifier.padding(top = with(density) { topHeight.toDp() })
                )
                .then(
                    if (bottomBar == null) Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    else Modifier.padding(bottom = with(density) { bottomHeight.toDp() })
                )
                .padding(contentPadding),
            content = content
        )
        if (topBar != null) {
            GlassPane(
                state = glass,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onSizeChanged { topHeight = it.height },
                edge = GlassEdge.Bottom,
                visibility = { scrollState.value / fadeDistance },
                invalidation = { scrollState.value }
            ) {
                Box(Modifier.windowInsetsPadding(WindowInsets.statusBars)) { topBar() }
            }
        }
        if (bottomBar != null) {
            GlassPane(
                state = glass,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { bottomHeight = it.height },
                edge = GlassEdge.Top,
                visibility = { (scrollState.maxValue - scrollState.value) / fadeDistance },
                invalidation = { scrollState.value }
            ) {
                Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) { bottomBar() }
            }
        }
        overlay()
    }
}

/** Short confirmation on a floating glass pane, e.g. after a session was deleted. */
@Composable
fun GlassToast(
    message: String?,
    glass: GlassState?,
    modifier: Modifier = Modifier,
    icon: ImageVector = TldhIcons.Check
) {
    var lastMessage by remember { mutableStateOf("") }
    LaunchedEffect(message) { if (message != null) lastMessage = message }
    val reduced = LocalReducedMotion.current
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn(tween(if (reduced) 0 else TldhMotion.ENTER_MS, easing = TldhMotion.Enter)) +
            slideInVertically(tween(if (reduced) 0 else TldhMotion.ENTER_MS, easing = TldhMotion.Enter)) { it / 3 },
        exit = fadeOut(tween(if (reduced) 0 else 180)) +
            slideOutVertically(tween(if (reduced) 0 else 180)) { it / 4 }
    ) {
        GlassPane(
            state = glass,
            shape = RoundedCornerShape(16.dp),
            tint = TldhGlass.PaneTint,
            edge = GlassEdge.Outline,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = TldhColors.Success, modifier = Modifier.size(20.dp))
                Text(message ?: lastMessage, style = TldhType.Small)
            }
        }
    }
}
