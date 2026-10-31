package dev.bitsbots.tldh.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.bitsbots.tldh.analysis.ReplyTone
import dev.bitsbots.tldh.transcription.WhisperModelSpec
import dev.bitsbots.tldh.ui.ModelUiState
import dev.bitsbots.tldh.ui.UpdateController
import dev.bitsbots.tldh.ui.UpdateUiState
import dev.bitsbots.tldh.ui.components.GhostButton
import dev.bitsbots.tldh.ui.components.GlassScaffold
import dev.bitsbots.tldh.ui.components.Group
import dev.bitsbots.tldh.ui.components.GroupDivider
import dev.bitsbots.tldh.ui.components.IconRow
import dev.bitsbots.tldh.ui.components.OutlineButton
import dev.bitsbots.tldh.ui.components.PrimaryButton
import dev.bitsbots.tldh.ui.components.SectionLabel
import dev.bitsbots.tldh.ui.components.SegmentedControl
import dev.bitsbots.tldh.ui.components.SwitchRow
import dev.bitsbots.tldh.ui.components.TldhIcons
import dev.bitsbots.tldh.ui.theme.TldhColors
import dev.bitsbots.tldh.ui.theme.TldhMotion
import dev.bitsbots.tldh.ui.theme.TldhType
import dev.bitsbots.tldh.ui.theme.tabular
import java.util.Locale

private val toneSamples = mapOf(
    ReplyTone.KURZ to "Passt, am Samstag bin ich dabei.",
    ReplyTone.FREUNDLICH to "Ja, Samstag passt mir gut, ich freu mich!",
    ReplyTone.FOERMLICH to "Am Samstag passt es mir, ich bin gerne dabei."
)

@Composable
fun SettingsScreen(
    models: ModelUiState,
    onSelectModel: (WhisperModelSpec) -> Unit,
    onDeleteModels: () -> Unit,
    tone: ReplyTone,
    onToneChange: (ReplyTone) -> Unit,
    markUncertain: Boolean,
    onMarkUncertainChange: (Boolean) -> Unit,
    updates: UpdateController?,
    appVersion: String,
    channel: String,
    repositorySlug: String,
    onBack: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    GlassScaffold(
        topBar = {
            ScreenTitleBar(icon = TldhIcons.Back, iconDescription = "Zurück", title = "Einstellungen", subtitle = null, onIcon = onBack, large = true)
        }
    ) {
        Column(
            modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            RecognitionSection(models, onSelectModel, onDeleteRequest = { confirmDelete = true })
            RepliesSettings(tone, onToneChange, markUncertain, onMarkUncertainChange)
            if (updates != null) UpdatesSection(updates, appVersion)
            PrivacySection()
            AboutSection(repositorySlug, appVersion, channel)
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteModels()
                }) { Text("Löschen", color = TldhColors.Danger, style = TldhType.Label) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Abbrechen", color = TldhColors.Text, style = TldhType.Label) }
            },
            title = { Text("Modelle löschen?", style = TldhType.Title) },
            text = { Text("Für die nächste Sprachnachricht lädt tl;dh das Modell dann neu herunter.", style = TldhType.Body) },
            containerColor = TldhColors.SurfaceRaised,
            titleContentColor = TldhColors.Text,
            textContentColor = TldhColors.TextMuted
        )
    }
}

@Composable
private fun RecognitionSection(models: ModelUiState, onSelectModel: (WhisperModelSpec) -> Unit, onDeleteRequest: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Erkennung")
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
                    selected = models.download?.spec ?: models.active,
                    onSelect = onSelectModel,
                    statusFor = { spec ->
                        when {
                            spec == models.active -> "Aktiv · ${spec.sizeLabel}" to true
                            spec in models.installed -> "Geladen · ${spec.sizeLabel}" to false
                            else -> "Nicht geladen · ${spec.sizeLabel} Download" to false
                        }
                    }
                )
                GroupDivider(inset = 0.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Belegter Speicher", style = TldhType.Small, color = TldhColors.TextMuted)
                        Text(
                            if (models.storageBytes == 0L) "Kein Modell geladen" else "%.0f MB".format(Locale.GERMANY, models.storageBytes / 1_000_000.0),
                            style = TldhType.Row.tabular()
                        )
                    }
                    if (models.installed.isNotEmpty()) {
                        GhostButton(
                            text = "Modelle löschen",
                            onClick = onDeleteRequest,
                            contentColor = TldhColors.Danger,
                            enabled = models.download == null
                        )
                    }
                }
            }
        }
        models.error?.let {
            Text(it, style = TldhType.Caption, color = TldhColors.Danger, modifier = Modifier.padding(horizontal = 4.dp))
        }
        Text(
            "Die Empfehlung richtet sich nach Prozessor und Arbeitsspeicher deines Handys. Modelle werden einmalig geladen und danach offline genutzt.",
            style = TldhType.Caption,
            color = TldhColors.TextMuted,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun RepliesSettings(
    tone: ReplyTone,
    onToneChange: (ReplyTone) -> Unit,
    markUncertain: Boolean,
    onMarkUncertainChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Antworten & Transkript")
        Group {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Standard-Ton", style = TldhType.Body)
                SegmentedControl(options = ReplyTone.entries, selected = tone, label = { it.label }, onSelect = onToneChange)
                Text("„${toneSamples[tone].orEmpty()}“", style = TldhType.Small, color = TldhColors.TextMuted)
            }
            GroupDivider(inset = 0.dp)
            SwitchRow(
                title = "Unsichere Wörter markieren",
                subtitle = "Im Transkript gepunktet oder wellig unterstrichen",
                checked = markUncertain,
                onCheckedChange = onMarkUncertainChange
            )
        }
    }
}

@Composable
private fun UpdatesSection(updates: UpdateController, appVersion: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Updates")
        Group {
            val state = updates.state
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Version $appVersion", style = TldhType.Body.tabular())
                    val (status, color) = when (state) {
                        UpdateUiState.Idle -> (if (updates.configured) "Prüfung nur auf Knopfdruck" else "Update-Quelle im Build nicht gesetzt") to TldhColors.TextMuted
                        UpdateUiState.Checking -> "Suche nach Updates …" to TldhColors.TextMuted
                        UpdateUiState.UpToDate -> "Du hast die neueste Version" to TldhColors.Success
                        is UpdateUiState.Available -> "Version ${state.update.version} verfügbar" to TldhColors.Glow
                        is UpdateUiState.Downloading -> "Wird geladen · ${(state.progress * 100).toInt()} %" to TldhColors.TextMuted
                        is UpdateUiState.ReadyToInstall -> "Geladen und geprüft (SHA-256)" to TldhColors.Success
                        is UpdateUiState.Error -> state.message to TldhColors.Danger
                    }
                    Text(status, style = TldhType.Caption.tabular(), color = color)
                }
                when (state) {
                    UpdateUiState.Checking -> Spinner()
                    is UpdateUiState.Downloading -> Spinner()
                    is UpdateUiState.Available, is UpdateUiState.ReadyToInstall -> Unit
                    else -> OutlineButton(
                        text = "Prüfen",
                        onClick = { updates.check() },
                        enabled = updates.configured,
                        height = 44.dp
                    )
                }
            }
            when (state) {
                is UpdateUiState.Available -> Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Beim Download bleibt das Handy wach. Danach prüft tl;dh die Datei und öffnet den Android-Installer.",
                        style = TldhType.Caption,
                        color = TldhColors.TextMuted
                    )
                    PrimaryButton(
                        text = state.update.apk.sizeBytes?.let { "Laden · %.0f MB".format(Locale.GERMANY, it / 1_000_000.0) } ?: "Laden",
                        icon = TldhIcons.Download,
                        onClick = { updates.download(state.update) }
                    )
                }
                is UpdateUiState.Downloading -> LinearProgress(state.progress, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 18.dp))
                is UpdateUiState.ReadyToInstall -> PrimaryButton(
                    text = "Installieren",
                    onClick = { updates.install(state.apkFile) },
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun PrivacySection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Datenschutz")
        Group {
            IconRow(icon = TldhIcons.Phone, title = "Verarbeitung nur auf dem Handy", subtitle = "Internet nur für Modell-Download und Updates")
            GroupDivider()
            IconRow(icon = TldhIcons.Trash, title = "Audio und Transkript werden gelöscht", subtitle = "beim Schließen des Ergebnisses, spätestens beim nächsten Start")
            GroupDivider()
            IconRow(icon = TldhIcons.NoAccount, title = "Kein Konto, keine Analyse", subtitle = "Kein Tracking, keine Werbung, keine Cloud")
        }
    }
}

@Composable
private fun AboutSection(repositorySlug: String, appVersion: String, channel: String) {
    val context = LocalContext.current
    var showLicenses by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (showLicenses) 90f else 0f, tween(200, easing = TldhMotion.Enter), label = "licenses")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Über")
        Group {
            if (repositorySlug.contains('/')) {
                IconRow(
                    icon = null,
                    title = "Quellcode auf GitHub",
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$repositorySlug")))
                        }
                    },
                    trailing = { Icon(TldhIcons.External, contentDescription = null, tint = TldhColors.TextMuted, modifier = Modifier.size(20.dp)) }
                )
                GroupDivider(inset = 0.dp)
            }
            IconRow(
                icon = null,
                title = "Lizenzen",
                subtitle = "whisper.cpp, Whisper-Modelle, Bricolage Grotesque",
                onClick = { showLicenses = !showLicenses },
                trailing = {
                    Icon(
                        TldhIcons.ChevronRight,
                        contentDescription = null,
                        tint = TldhColors.TextMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )
                }
            )
            AnimatedVisibility(visible = showLicenses) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "whisper.cpp · MIT License · Copyright (c) 2023-2026 The ggml authors",
                        "OpenAI Whisper-Modelle · MIT License",
                        "Bricolage Grotesque · SIL Open Font License 1.1 · The Bricolage Grotesque Project Authors",
                        "AndroidX, Jetpack Compose, Kotlin · Apache License 2.0"
                    ).forEach { Text(it, style = TldhType.Caption, color = TldhColors.TextMuted) }
                }
            }
        }
        Text(
            "tl;dh $appVersion · $channel",
            style = TldhType.Caption.tabular(),
            color = TldhColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}
