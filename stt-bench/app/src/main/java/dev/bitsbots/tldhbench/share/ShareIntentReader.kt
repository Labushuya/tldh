package dev.bitsbots.tldhbench.share

import android.content.Intent
import android.net.Uri
import android.os.Build

data class SharedAudio(
    val uri: Uri,
    val mimeType: String?
)

internal object ShareIntentReader {
    fun read(intent: Intent?): SharedAudio? {
        if (intent == null) return null
        if (intent.action != Intent.ACTION_SEND) return null
        val uri = extractStreamUri(intent) ?: return null
        return SharedAudio(uri = uri, mimeType = intent.type)
    }

    @Suppress("DEPRECATION")
    private fun extractStreamUri(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }
}
