package dev.bitsbots.tldh.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dev.bitsbots.tldh.MainActivity
import dev.bitsbots.tldh.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

/**
 * Keeps tl;dh alive while a voice note is transcribed or a model/update downloads, so the work
 * continues when the user switches apps, rotates the phone or turns the display off. It shows a
 * silent progress notification (Android defers it for short jobs) and holds a partial wake lock;
 * it never keeps the display on. It stops itself as soon as nothing is running.
 */
class TldhWorkService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observer: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var foregroundAsTranscription: Boolean? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        WorkNotifications.ensureChannels(this)
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:work")
            .apply {
                setReferenceCounted(false)
                acquire(MAX_WAKE_LOCK_MS)
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val engine = TldhEngine.get(this)
        if (intent?.action == ACTION_CANCEL) engine.cancelWork()
        val status = engine.currentWork()
        // Android requires startForeground() after startForegroundService(), even if the work
        // already finished in the meantime.
        goForeground(status ?: WorkStatus("tl;dh", "Wird beendet …", null, transcribing = false))
        if (status == null) {
            stop()
            return START_NOT_STICKY
        }
        if (observer == null) {
            observer = scope.launch {
                engine.work.conflate().collect { next ->
                    if (next == null) {
                        stop()
                    } else {
                        show(next)
                        delay(NOTIFY_INTERVAL_MS)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    /** Android 15+: the system ends long foreground work after its daily budget. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stop()
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        super.onDestroy()
    }

    private fun show(status: WorkStatus) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && foregroundAsTranscription != status.transcribing) {
            goForeground(status)
        } else {
            getSystemService(NotificationManager::class.java).notify(WorkNotifications.ID_WORK, WorkNotifications.work(this, status))
        }
    }

    private fun goForeground(status: WorkStatus) {
        val type = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM && status.transcribing ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            else -> 0
        }
        ServiceCompat.startForeground(this, WorkNotifications.ID_WORK, WorkNotifications.work(this, status), type)
        foregroundAsTranscription = status.transcribing
    }

    private fun stop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val ACTION_CANCEL = "dev.bitsbots.tldh.action.CANCEL_WORK"
        private const val MAX_WAKE_LOCK_MS = 60L * 60L * 1000L
        private const val NOTIFY_INTERVAL_MS = 500L
    }
}

/** Notification channels and builders. Content of the voice note never goes into a notification. */
object WorkNotifications {
    private const val CHANNEL_WORK = "tldh_work"
    private const val CHANNEL_DONE = "tldh_done"
    const val ID_WORK = 1001
    private const val ID_DONE = 1002

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_WORK, "Laufende Verarbeitung", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Fortschritt, während tl;dh im Hintergrund transkribiert oder lädt."
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Zusammenfassung fertig", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Meldung, wenn eine Sprachnachricht fertig zusammengefasst ist und du gerade nicht in tl;dh bist."
            }
        )
    }

    fun work(context: Context, status: WorkStatus): Notification =
        NotificationCompat.Builder(context, CHANNEL_WORK)
            .setSmallIcon(R.drawable.ic_stat_tldh)
            .setColor(ContextCompat.getColor(context, R.color.tldh_accent))
            .setContentTitle(status.title)
            .setContentText(status.text)
            .setProgress(100, status.progress ?: 0, status.progress == null)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openApp(context))
            .addAction(0, "Abbrechen", cancel(context))
            .build()

    /** Posted only while the user is outside the app; carries no content of the voice note. */
    fun notifyFinished(context: Context, success: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_tldh)
            .setColor(ContextCompat.getColor(context, R.color.tldh_accent))
            .setContentTitle(if (success) "Zusammenfassung fertig" else "Das hat nicht geklappt")
            .setContentText(if (success) "Antippen, um die Sprachnachricht zu lesen." else "Antippen für Details.")
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openApp(context))
            .build()
        manager.notify(ID_DONE, notification)
    }

    fun cancelFinished(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(ID_DONE)
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun cancel(context: Context): PendingIntent = PendingIntent.getService(
        context,
        1,
        Intent(context, TldhWorkService::class.java).setAction(TldhWorkService.ACTION_CANCEL),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}
