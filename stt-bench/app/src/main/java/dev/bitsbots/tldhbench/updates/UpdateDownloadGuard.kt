package dev.bitsbots.tldhbench.updates

import android.content.Context
import android.os.PowerManager

class UpdateDownloadGuard(private val context: Context) {
    suspend fun <T> runGuardedDownload(block: suspend () -> T): T {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "${context.packageName}:UpdateDownload"
        ).apply { setReferenceCounted(false) }
        return try {
            wakeLock.acquire(MAX_DOWNLOAD_WAKELOCK_MS)
            block()
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private companion object {
        const val MAX_DOWNLOAD_WAKELOCK_MS = 10 * 60 * 1000L
    }
}
