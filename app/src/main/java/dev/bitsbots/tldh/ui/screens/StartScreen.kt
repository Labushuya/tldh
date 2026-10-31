package dev.bitsbots.tldh.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bitsbots.tldh.ui.components.BreathingWaveform
import dev.bitsbots.tldh.ui.components.GlassScaffold
import dev.bitsbots.tldh.ui.components.GlassToast
import dev.bitsbots.tldh.ui.components.Group
import dev.bitsbots.tldh.ui.components.GroupDivider
import dev.bitsbots.tldh.ui.components.IconRow
import dev.bitsbots.tldh.ui.components.Pill
import dev.bitsbots.tldh.ui.components.PrimaryButton
import dev.bitsbots.tldh.ui.components.RoundIconButton
import dev.bitsbots.tldh.ui.components.TldhIcons
import dev.bitsbots.tldh.ui.components.TldhMark
import dev.bitsbots.tldh.ui.components.Wordmark
import dev.bitsbots.tldh.ui.components.rememberGlassState
import dev.bitsbots.tldh.ui.theme.TldhColors
import dev.bitsbots.tldh.ui.theme.TldhType

enum class StatusTone { Ready, Attention, Busy, Unavailable }

data class RecognitionStatus(val label: String, val tone: StatusTone)

@Composable
fun StartScreen(
    recognition: RecognitionStatus,
    toast: String?,
    onOpenFile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val glass = rememberGlassState()
    GlassScaffold(
        glass = glass,
        bottomBar = {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryButton(text = "Audiodatei öffnen", icon = TldhIcons.FileAudio, onClick = onOpenFile)
                Text(
                    "Oder direkt aus WhatsApp teilen",
                    style = TldhType.Caption,
                    color = TldhColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        overlay = {
            GlassToast(
                message = toast,
                glass = glass,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 16.dp, end = 16.dp, bottom = 128.dp)
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(end = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TldhMark(size = 32.dp)
            Wordmark()
            Spacer(Modifier.weight(1f))
            RoundIconButton(
                icon = TldhIcons.Settings,
                contentDescription = "Einstellungen",
                onClick = onOpenSettings,
                tint = TldhColors.TextMuted,
                modifier = Modifier.padding(end = 0.dp)
            )
        }
        Column(
            modifier = Modifier.padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Long story,\nshort.",
                    style = TldhType.Display,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    "Teile eine Sprachnachricht an tl;dh und lies in Sekunden, worum es geht. Alles bleibt auf deinem Handy.",
                    style = TldhType.Body,
                    color = TldhColors.TextMuted,
                    modifier = Modifier.widthIn(max = 340.dp)
                )
            }
            BreathingWaveform()
            Group {
                Text(
                    "So geht's",
                    style = TldhType.BodyStrong,
                    modifier = Modifier
                        .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)
                        .semantics { heading() }
                )
                IconRow(icon = TldhIcons.Chat, title = "In WhatsApp die Sprachnachricht gedrückt halten")
                GroupDivider()
                IconRow(icon = TldhIcons.Share, title = "„Teilen“ tippen")
                GroupDivider()
                IconRow(icon = TldhIcons.AppTile, title = "tl;dh auswählen", modifier = Modifier.padding(bottom = 4.dp))
            }
            Column {
                GroupDivider(inset = 0.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(role = Role.Button, onClickLabel = "Erkennung einstellen", onClick = onOpenSettings),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Erkennung", style = TldhType.Small, color = TldhColors.TextMuted)
                    val color = when (recognition.tone) {
                        StatusTone.Ready -> TldhColors.Success
                        StatusTone.Attention -> TldhColors.Unsure
                        StatusTone.Busy -> TldhColors.Glow
                        StatusTone.Unavailable -> TldhColors.Danger
                    }
                    Pill(text = recognition.label, color = color)
                }
                GroupDivider(inset = 0.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Datenschutz", style = TldhType.Small, color = TldhColors.TextMuted)
                    Text("Offline · nichts wird hochgeladen", style = TldhType.Small)
                }
                GroupDivider(inset = 0.dp)
            }
        }
    }
}
