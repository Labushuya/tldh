package dev.bitsbots.tldh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.bitsbots.tldh.transcription.WhisperModelCatalog
import dev.bitsbots.tldh.transcription.WhisperModelSpec
import dev.bitsbots.tldh.ui.ModelUiState
import dev.bitsbots.tldh.ui.components.GlassScaffold
import dev.bitsbots.tldh.ui.components.Group
import dev.bitsbots.tldh.ui.components.IconRow
import dev.bitsbots.tldh.ui.components.OutlineButton
import dev.bitsbots.tldh.ui.components.PrimaryButton
import dev.bitsbots.tldh.ui.components.TldhIcons
import dev.bitsbots.tldh.ui.theme.TldhColors
import dev.bitsbots.tldh.ui.theme.TldhType
import dev.bitsbots.tldh.ui.theme.tabular

/** One-time setup: pick and download a recognition model, then continue with the pending audio. */
@Composable
fun SetupScreen(
    models: ModelUiState,
    hasPendingAudio: Boolean,
    onDownload: (WhisperModelSpec) -> Unit,
    onUseInstalled: (WhisperModelSpec) -> Unit,
    onClose: () -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf(models.recommended.id) }
    val selected = WhisperModelCatalog.byId(selectedId) ?: models.recommended
    val download = models.download
    GlassScaffold(
        topBar = {
            ScreenTitleBar(
                icon = TldhIcons.Close,
                iconDescription = "Schließen",
                title = if (hasPendingAudio) "Sprachnachricht wartet" else "Erkennung einrichten",
                subtitle = null,
                onIcon = onClose
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    !models.runtimeAvailable -> OutlineButton(text = "Zurück", onClick = onClose, modifier = Modifier.fillMaxWidth())
                    download != null -> {
                        Text(
                            "Wird geladen · ${(download.progress * 100).toInt()} % von ${download.spec.sizeLabel}",
                            style = TldhType.Label.tabular()
                        )
                        LinearProgress(download.progress)
                        Text(
                            if (hasPendingAudio) "Danach wird deine Sprachnachricht sofort verarbeitet." else "Danach ist tl;dh startklar.",
                            style = TldhType.Caption,
                            color = TldhColors.TextMuted
                        )
                    }
                    selected in models.installed -> PrimaryButton(
                        text = if (hasPendingAudio) "Verwenden und loslegen" else "Verwenden",
                        onClick = { onUseInstalled(selected) }
                    )
                    else -> PrimaryButton(
                        text = "Laden und loslegen · ${selected.sizeLabel}",
                        icon = TldhIcons.Download,
                        onClick = { onDownload(selected) }
                    )
                }
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Einmal einrichten", style = TldhType.Headline, modifier = Modifier.semantics { heading() })
                Text(
                    "tl;dh erkennt Sprache direkt auf deinem Handy. Dafür braucht es einmalig ein Erkennungsmodell, danach läuft alles offline.",
                    style = TldhType.Body,
                    color = TldhColors.TextMuted
                )
            }
            Group {
                if (!models.runtimeAvailable) {
                    IconRow(
                        icon = TldhIcons.Warning,
                        iconTint = TldhColors.Danger,
                        title = "Auf diesem Handy nicht verfügbar",
                        subtitle = "tl;dh enthält die Spracherkennung nur für 64-Bit-ARM-Handys (arm64-v8a)."
                    )
                } else {
                    ModelOptions(
                        models = models,
                        selected = download?.spec ?: selected,
                        onSelect = { selectedId = it.id },
                        statusFor = { spec ->
                            if (spec in models.installed) "Bereits geladen" to true else "${spec.sizeLabel} Download, einmalig" to false
                        }
                    )
                }
            }
            models.error?.let { message ->
                IconRow(icon = TldhIcons.Warning, iconTint = TldhColors.Danger, title = message, modifier = Modifier.padding(horizontal = 0.dp))
            }
            Text(
                "Tipp: Über WLAN laden. Die Empfehlung richtet sich nach Prozessor und Arbeitsspeicher deines Handys.",
                style = TldhType.Caption,
                color = TldhColors.TextMuted,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/** Something went wrong: say what, and offer the two ways forward. */
@Composable
fun ErrorScreen(title: String, message: String, onOpenFile: () -> Unit, onClose: () -> Unit) {
    GlassScaffold(
        topBar = {
            ScreenTitleBar(icon = TldhIcons.Close, iconDescription = "Zurück zum Start", title = "tl;dh", subtitle = null, onIcon = onClose)
        },
        bottomBar = {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryButton(text = "Andere Datei öffnen", icon = TldhIcons.FileAudio, onClick = onOpenFile)
                OutlineButton(text = "Zurück zum Start", onClick = onClose, modifier = Modifier.fillMaxWidth())
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(top = 28.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(TldhColors.Danger.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(TldhIcons.Warning, contentDescription = null, tint = TldhColors.Danger, modifier = Modifier.size(28.dp))
            }
            Text(title, style = TldhType.Headline, modifier = Modifier.semantics { heading() })
            Text(message, style = TldhType.Body, color = TldhColors.TextMuted)
        }
    }
}
