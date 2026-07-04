package dev.bitsbots.tldh

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import dev.bitsbots.tldh.session.SessionManager
import dev.bitsbots.tldh.ui.TldhApp
import dev.bitsbots.tldh.ui.theme.TldhTheme

class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager
    private val shareIntentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sessionManager = SessionManager(filesDir = filesDir, cacheDir = cacheDir)
        sessionManager.wipeOrphanedSessions()
        shareIntentState.value = intent

        setContent {
            TldhTheme {
                TldhApp(
                    currentIntent = shareIntentState.value,
                    sessionManager = sessionManager,
                    updaterEnabled = BuildConfig.UPDATER_ENABLED,
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
        shareIntentState.value = intent
    }

    override fun onDestroy() {
        if (isFinishing) {
            sessionManager.wipeCurrentSession()
        }
        super.onDestroy()
    }
}
