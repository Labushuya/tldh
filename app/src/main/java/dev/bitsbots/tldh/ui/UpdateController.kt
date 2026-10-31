package dev.bitsbots.tldh.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import dev.bitsbots.tldh.updates.ApkInstaller
import dev.bitsbots.tldh.updates.GitHubReleaseClient
import dev.bitsbots.tldh.updates.StableReleaseSelector
import dev.bitsbots.tldh.updates.StableUpdate
import dev.bitsbots.tldh.updates.UpdateDownloadGuard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val update: StableUpdate) : UpdateUiState
    data class Downloading(val update: StableUpdate, val progress: Float) : UpdateUiState
    data class ReadyToInstall(val update: StableUpdate, val apkFile: File) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

/**
 * Manual stable-release updater. Owned by the engine, so a running download survives rotation and
 * leaving the app. Checks only run when the user taps, and downloads are SHA-256 verified.
 */
class UpdateController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val appVersion: String,
    private val repositorySlug: String,
    private val onDownloadStarted: () -> Unit
) {
    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()
    private var downloadJob: Job? = null

    private var current: UpdateUiState
        get() = _state.value
        set(value) {
            _state.value = value
        }

    val configured: Boolean = repositorySlug.contains('/')

    fun check() {
        if (!configured || current == UpdateUiState.Checking) return
        scope.launch {
            current = UpdateUiState.Checking
            current = try {
                val releases = GitHubReleaseClient(repositorySlug).fetchStableReleases()
                val update = StableReleaseSelector().select(appVersion, releases)
                if (update == null) UpdateUiState.UpToDate else UpdateUiState.Available(update)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                UpdateUiState.Error(error.message ?: "Update-Prüfung fehlgeschlagen.")
            }
        }
    }

    fun download(update: StableUpdate) {
        if (current is UpdateUiState.Downloading) return
        current = UpdateUiState.Downloading(update, 0f)
        onDownloadStarted()
        downloadJob = scope.launch {
            current = try {
                val file = UpdateDownloadGuard(context).runGuardedDownload {
                    GitHubReleaseClient(repositorySlug).downloadAsset(
                        asset = update.apk,
                        destinationDir = File(context.cacheDir, "updates"),
                        // Called synchronously by the download loop, so it can't overtake the result.
                        progress = { progress -> current = UpdateUiState.Downloading(update, progress) }
                    )
                }
                UpdateUiState.ReadyToInstall(update, file)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                UpdateUiState.Error(error.message ?: "Download fehlgeschlagen.")
            }
        }
    }

    fun install(apkFile: File) {
        context.startActivity(ApkInstaller(context).createInstallIntent(apkFile))
    }

    fun cancelDownload() {
        if (current is UpdateUiState.Downloading) {
            downloadJob?.cancel()
            current = UpdateUiState.Idle
        }
    }

    fun reset() {
        current = UpdateUiState.Idle
    }
}

/** Copies text; voice-note content is private, so Android 13+ hides it from the clipboard preview. */
fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("tl;dh", text)
    clip.description.extras = PersistableBundle().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        } else {
            putBoolean("android.content.extra.IS_SENSITIVE", true)
        }
    }
    clipboard.setPrimaryClip(clip)
}
