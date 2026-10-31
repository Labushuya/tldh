package dev.bitsbots.tldh.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bitsbots.tldh.transcription.WhisperModelCatalog
import dev.bitsbots.tldh.transcription.WhisperModelSpec
import dev.bitsbots.tldh.ui.ModelUiState
import dev.bitsbots.tldh.ui.components.GroupDivider
import dev.bitsbots.tldh.ui.components.Pill
import dev.bitsbots.tldh.ui.components.RadioRow
import dev.bitsbots.tldh.ui.theme.TldhColors
import dev.bitsbots.tldh.ui.theme.TldhType
import dev.bitsbots.tldh.ui.theme.tabular

/** Radio list of the whisper models with download/active state; used by settings and setup. */
@Composable
fun ModelOptions(
    models: ModelUiState,
    selected: WhisperModelSpec?,
    onSelect: (WhisperModelSpec) -> Unit,
    statusFor: (WhisperModelSpec) -> Pair<String, Boolean>
) {
    Column(Modifier.selectableGroup()) {
        WhisperModelCatalog.models.forEachIndexed { index, spec ->
            if (index > 0) GroupDivider(inset = 0.dp)
            val download = models.download?.takeIf { it.spec == spec }
            RadioRow(
                selected = spec == selected,
                enabled = models.download == null,
                onClick = { onSelect(spec) }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(spec.qualityLabel, style = TldhType.BodyStrong)
                    if (spec == models.recommended) {
                        Pill(text = "empfohlen", color = TldhColors.Text, background = TldhColors.Glow.copy(alpha = 0.18f))
                    }
                }
                Text("${spec.displayName} · ${spec.description}", style = TldhType.Small, color = TldhColors.TextMuted)
                if (download != null) {
                    Text(
                        "Wird geladen · ${(download.progress * 100).toInt()} %",
                        style = TldhType.Caption.tabular(),
                        color = TldhColors.Glow,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    LinearProgress(download.progress, Modifier.padding(top = 6.dp))
                } else {
                    val (status, positive) = statusFor(spec)
                    Text(
                        status,
                        style = TldhType.Caption.tabular(),
                        color = if (positive) TldhColors.Success else TldhColors.TextMuted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
