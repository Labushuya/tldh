package dev.bitsbots.tldh.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bitsbots.tldh.analysis.ReplyTone
import dev.bitsbots.tldh.engine.TldhEngine
import dev.bitsbots.tldh.share.SharedAudio
import dev.bitsbots.tldh.ui.screens.ErrorScreen
import dev.bitsbots.tldh.ui.screens.ProcessingScreen
import dev.bitsbots.tldh.ui.screens.RecognitionStatus
import dev.bitsbots.tldh.ui.screens.ResultScreen
import dev.bitsbots.tldh.ui.screens.SettingsScreen
import dev.bitsbots.tldh.ui.screens.SetupScreen
import dev.bitsbots.tldh.ui.screens.StartScreen
import dev.bitsbots.tldh.ui.screens.StatusTone
import dev.bitsbots.tldh.ui.theme.LocalReducedMotion
import dev.bitsbots.tldh.ui.theme.TldhColors
import dev.bitsbots.tldh.ui.theme.TldhMotion
import kotlinx.coroutines.delay

private const val PREFS = "tldh_settings"
private const val KEY_TONE = "reply_tone"
private const val KEY_MARK_UNCERTAIN = "mark_uncertain"
private const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"

/** What is on screen: the settings page or the current step of the voice-note flow. */
private sealed interface Screen {
    val key: String

    data object Settings : Screen {
        override val key: String = "settings"
    }

    data class Flow(val state: TldhUiState) : Screen {
        override val key: String
            get() = when (state) {
                TldhUiState.Idle -> "start"
                is TldhUiState.NeedsModel -> "setup"
                is TldhUiState.Processing -> "processing"
                is TldhUiState.Result -> "result"
                is TldhUiState.Error -> "error"
            }
    }
}

/**
 * The window onto [TldhEngine]. Holds no work of its own: state comes from the engine, so the
 * screen can be recreated (rotation) or closed while a voice note keeps processing.
 */
@Composable
fun TldhApp(
    engine: TldhEngine,
    appVersion: String,
    channel: String,
    repositorySlug: String
) {
    val context = LocalContext.current
    val reducedMotion = LocalReducedMotion.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    val flow by engine.flow.collectAsStateWithLifecycle()
    val models by engine.models.collectAsStateWithLifecycle()
    val toast by engine.toast.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var tone by remember {
        mutableStateOf(ReplyTone.entries.firstOrNull { it.name == prefs.getString(KEY_TONE, null) } ?: ReplyTone.FREUNDLICH)
    }
    var markUncertain by remember { mutableStateOf(prefs.getBoolean(KEY_MARK_UNCERTAIN, true)) }

    LaunchedEffect(toast) {
        val message = toast
        if (message != null) {
            delay(3200)
            engine.consumeToast(message)
        }
    }

    // A new voice note arriving while settings are open takes over the screen.
    val flowKey = Screen.Flow(flow).key
    LaunchedEffect(flowKey) {
        if (flow is TldhUiState.Processing || flow is TldhUiState.NeedsModel) showSettings = false
    }

    // Android 13+: ask once, when background work first starts, so tl;dh can say when it's done.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val working = flow is TldhUiState.Processing || models.download != null
    LaunchedEffect(working) {
        if (working &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false)
        ) {
            prefs.edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            engine.process(PendingAudio(SharedAudio(uri = uri, mimeType = context.contentResolver.getType(uri)), AudioOrigin.FILE))
        }
    }

    fun openFile() {
        runCatching { openDocument.launch(arrayOf("audio/*", "application/ogg")) }
    }

    fun changeTone(selected: ReplyTone) {
        tone = selected
        prefs.edit().putString(KEY_TONE, selected.name).apply()
    }

    BackHandler(enabled = showSettings || flow !is TldhUiState.Idle) {
        when {
            showSettings -> showSettings = false
            flow is TldhUiState.Result -> engine.endSession(TldhEngine.TOAST_SESSION_DELETED)
            flow is TldhUiState.Processing -> engine.endSession(TldhEngine.TOAST_CANCELLED)
            flow is TldhUiState.NeedsModel -> engine.closeSetup()
            else -> engine.endSession(null)
        }
    }

    val recognition = when {
        !models.runtimeAvailable -> RecognitionStatus("Nicht verfügbar", StatusTone.Unavailable)
        models.download != null -> RecognitionStatus("Wird geladen · ${((models.download?.progress ?: 0f) * 100).toInt()} %", StatusTone.Busy)
        else -> models.active?.let { RecognitionStatus("${it.displayName} · bereit", StatusTone.Ready) }
            ?: RecognitionStatus("Nicht eingerichtet", StatusTone.Attention)
    }

    AnimatedContent(
        targetState = if (showSettings) Screen.Settings else Screen.Flow(flow),
        contentKey = { it.key },
        transitionSpec = {
            if (reducedMotion) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                (fadeIn(tween(TldhMotion.ENTER_MS, easing = TldhMotion.Enter)) +
                    slideInVertically(tween(TldhMotion.ENTER_MS, easing = TldhMotion.Enter)) { it / 24 }) togetherWith
                    fadeOut(tween(140))
            }
        },
        label = "screen",
        modifier = Modifier
            .fillMaxSize()
            .background(TldhColors.Background)
    ) { screen ->
        when (screen) {
            Screen.Settings -> SettingsScreen(
                models = models,
                onSelectModel = { spec -> if (spec in models.installed) engine.activateModel(spec) else engine.downloadModel(spec) },
                onDeleteModels = { engine.deleteModels() },
                tone = tone,
                onToneChange = { changeTone(it) },
                markUncertain = markUncertain,
                onMarkUncertainChange = { enabled ->
                    markUncertain = enabled
                    prefs.edit().putBoolean(KEY_MARK_UNCERTAIN, enabled).apply()
                },
                updates = engine.updates,
                appVersion = appVersion,
                channel = channel,
                repositorySlug = repositorySlug,
                onBack = { showSettings = false }
            )
            is Screen.Flow -> when (val state = screen.state) {
                TldhUiState.Idle -> StartScreen(
                    recognition = recognition,
                    toast = toast,
                    onOpenFile = { openFile() },
                    onOpenSettings = { showSettings = true }
                )
                is TldhUiState.NeedsModel -> SetupScreen(
                    models = models,
                    hasPendingAudio = state.pending != null,
                    onDownload = { spec -> engine.downloadModel(spec) },
                    onUseInstalled = { spec -> engine.useInstalledModel(spec) },
                    onClose = { engine.closeSetup() }
                )
                is TldhUiState.Processing -> ProcessingScreen(
                    info = state.info,
                    onCancel = { engine.endSession(TldhEngine.TOAST_CANCELLED) }
                )
                is TldhUiState.Result -> ResultScreen(
                    result = state,
                    markUncertain = markUncertain,
                    tone = tone,
                    onToneChange = { changeTone(it) },
                    onClose = { engine.endSession(TldhEngine.TOAST_SESSION_DELETED) }
                )
                is TldhUiState.Error -> ErrorScreen(
                    title = state.title,
                    message = state.message,
                    onOpenFile = { openFile() },
                    onClose = { engine.endSession(null) }
                )
            }
        }
    }
}
