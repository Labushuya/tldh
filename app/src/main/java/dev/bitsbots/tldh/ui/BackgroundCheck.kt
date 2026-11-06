package dev.bitsbots.tldh.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** What would stop tl;dh from finishing in the background or saying that it's done. */
data class BackgroundCheck(
    /** The user (or Android) set tl;dh's battery use to "restricted": no foreground service once the app is left. */
    val restricted: Boolean,
    /** Notifications are off, so "Zusammenfassung fertig" never shows. */
    val notificationsOff: Boolean
) {
    val ok: Boolean get() = !restricted && !notificationsOff
}

object BackgroundChecks {
    /**
     * Both checks need no permission. Before Android 13 asks for notifications, they are not
     * "off" yet; tl;dh asks once when work starts.
     */
    fun read(context: Context, notificationsAsked: Boolean): BackgroundCheck {
        val restricted = context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted == true
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val couldAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAsked
        return BackgroundCheck(restricted = restricted, notificationsOff = !enabled && !couldAsk)
    }

    fun openAppInfo(context: Context) {
        launch(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
    }

    fun openNotificationSettings(context: Context) {
        val opened = launch(
            context,
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
        if (!opened) openAppInfo(context)
    }

    private fun launch(context: Context, intent: Intent): Boolean =
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}

/** Re-checked whenever tl;dh comes back to the screen, e.g. after changing it in the system settings. */
@Composable
fun rememberBackgroundCheck(notificationsAsked: Boolean): BackgroundCheck {
    val context = LocalContext.current
    var check by remember { mutableStateOf(BackgroundChecks.read(context, notificationsAsked)) }
    LifecycleResumeEffect(notificationsAsked) {
        check = BackgroundChecks.read(context, notificationsAsked)
        onPauseOrDispose { }
    }
    return check
}
