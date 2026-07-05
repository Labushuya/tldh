package dev.bitsbots.tldhbench.share

import android.content.Intent
import android.net.Uri

data class SharedAudio(
    val uri: Uri,
    val mimeType: String?
)

internal object ShareIntentReader {
    fun read(intent: Intent?): SharedAudio? {
        if (intent == null) return null
        if (intent.action != Intent.ACTION_SEND) return null
        val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) ?: return null
        return SharedAudio(uri = uri, mimeType = intent.type)
    }
}
