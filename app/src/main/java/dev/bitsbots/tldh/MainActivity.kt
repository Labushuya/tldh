package dev.bitsbots.tldh

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.bitsbots.tldh.session.SessionManager
import dev.bitsbots.tldh.ui.TldhApp
import dev.bitsbots.tldh.ui.theme.TldhTheme

class MainActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager
    private var latestShareIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sessionManager = SessionManager(filesDir = filesDir, cacheDir = cacheDir)
        sessionManager.wipeOrphanedSessions()
        latestShareIntent = intent

        setContent {
            TldhTheme {
                TldhApp(
                    initialIntent = latestShareIntent,
                    sessionManager = sessionManager,
                    updaterEnabled = BuildConfig.UPDATER_ENABLED,
                    appVersion = BuildConfig.VERSION_NAME,
                    channel = getString(R.string.distribution_channel)
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        latestShareIntent = intent
        setIntent(intent)
    }

    override fun onDestroy() {
        if (isFinishing) {
            sessionManager.wipeCurrentSession()
        }
        super.onDestroy()
    }
}
