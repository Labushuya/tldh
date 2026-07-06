package dev.bitsbots.tldhbench.share

import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Explicitly tracks where the active benchmark audio came from.
 * This prevents a newly shared WhatsApp/Telegram audio from being shown as if an older
 * Goldstandard/Longform selection were still active.
 */
enum class AudioSourceKind {
    EXTERNAL_SHARE,
    GOLDSTANDARD_SAMPLE,
    GENERATED_LONGFORM
}

data class SharedAudio(
    val uri: Uri,
    val mimeType: String?,
    val sourceKind: AudioSourceKind = AudioSourceKind.EXTERNAL_SHARE,
    val displayName: String? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

internal object ShareIntentReader {
    fun read(intent: Intent?): SharedAudio? {
        if (intent == null) return null
        if (intent.action != Intent.ACTION_SEND) return null
        val uri = extractStreamUri(intent) ?: return null
        return SharedAudio(
            uri = uri,
            mimeType = intent.type,
            sourceKind = AudioSourceKind.EXTERNAL_SHARE,
            displayName = uri.lastPathSegment?.substringAfterLast('/')
        )
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
