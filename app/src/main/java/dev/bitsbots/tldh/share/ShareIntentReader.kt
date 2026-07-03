package dev.bitsbots.tldh.share

import android.content.Intent
import android.net.Uri
import android.os.Parcelable

data class SharedAudio(
    val uri: Uri,
    val mimeType: String?
)

object ShareIntentReader {
    fun read(intent: Intent?): SharedAudio? {
        if (intent == null || intent.action != Intent.ACTION_SEND) return null
        val stream = intent.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM) ?: return null
        return SharedAudio(uri = stream, mimeType = intent.type)
    }
}

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(name: String): T? {
    return if (android.os.Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(name, T::class.java)
    } else {
        getParcelableExtra(name)
    }
}
