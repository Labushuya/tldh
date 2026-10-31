package dev.bitsbots.tldh

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.bitsbots.tldh.engine.TldhEngine
import dev.bitsbots.tldh.engine.WorkNotifications
import dev.bitsbots.tldh.share.ShareIntentReader
import dev.bitsbots.tldh.ui.AudioOrigin
import dev.bitsbots.tldh.ui.PendingAudio
import dev.bitsbots.tldh.ui.TldhApp
import dev.bitsbots.tldh.ui.theme.TldhTheme

/**
 * Only the window: all work lives in [TldhEngine], so recreating this activity (rotation, dark
 * mode, font size) never touches a running voice note.
 */
class MainActivity : ComponentActivity() {
    private val engine: TldhEngine by lazy { TldhEngine.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // tl;dh is always dark: light system-bar icons regardless of the system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        // A recreated activity gets its original intent again; it was handled the first time.
        if (savedInstanceState == null) handleShare(intent)

        setContent {
            TldhTheme {
                TldhApp(
                    engine = engine,
                    appVersion = BuildConfig.VERSION_NAME,
                    channel = getString(R.string.distribution_channel),
                    repositorySlug = BuildConfig.GITHUB_REPOSITORY
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShare(intent)
    }

    override fun onStart() {
        super.onStart()
        engine.uiVisible = true
        WorkNotifications.cancelFinished(this)
    }

    override fun onStop() {
        engine.uiVisible = false
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing && !isChangingConfigurations) engine.onUiFinished()
        super.onDestroy()
    }

    private fun handleShare(intent: Intent?) {
        if (intent == null) return
        // Reopening from recents replays the old share intent; that voice note is already handled.
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        ShareIntentReader.read(intent)?.let { engine.process(PendingAudio(it, AudioOrigin.SHARE)) }
    }
}
