package com.healthconnect.export.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import com.healthconnect.export.R

/**
 * Foreground-service notification used while a scheduled export runs.
 *
 * Health Connect on Android 14 refuses to read data from the background unless
 * the app either holds `READ_HEALTH_DATA_IN_BACKGROUND` or is in the foreground.
 * The permission is the supported route and is declared/requested by the app;
 * the promotion below is a best-effort extra for the cases where the system
 * still allows a background app to start a foreground service (visible app, or
 * the user granted the battery-optimization exemption).
 */
object ExportForegroundNotifier {
    const val CHANNEL_ID = "health_export"
    const val NOTIFICATION_ID = 1001

    private const val TAG = "ExportForeground"

    /**
     * Service type used while an export runs.
     *
     * `health` is deliberately NOT used: Android requires BODY_SENSORS /
     * ACTIVITY_RECOGNITION for that type, which only makes sense for apps reading
     * body sensors directly. This app reads Health Connect (a permission-gated
     * provider), and the work it does is synchronising data to storage/Drive/webhook,
     * so `dataSync` is both the accurate type and the one that needs no extra
     * runtime permission.
     */
    private const val SERVICE_TYPE = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC

    private const val CHANNEL_NAME = "notification_channel_export"
    private const val CHANNEL_DESC = "notification_channel_export_desc"
    private const val TITLE = "notification_export_title"
    private const val TEXT = "notification_export_text"

    /**
     * Builds the [ForegroundInfo] for a running export.
     *
     * The service type must match `foregroundServiceType` in the manifest,
     * otherwise Android 14 refuses the promotion.
     */
    fun foregroundInfo(
        context: Context,
        foregroundServiceType: Int,
    ): ForegroundInfo {
        ensureChannel(context)
        return ForegroundInfo(
            NOTIFICATION_ID,
            buildNotification(context),
            foregroundServiceType,
        )
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_export),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_export_desc)
                setShowBadge(false)
            }
        manager.createNotificationChannel(channel)
    }

    /**
     * Promotes the running worker to a foreground service.
     *
     * Android 12+ rejects the promotion with `ForegroundServiceStartNotAllowedException`
     * when the app itself runs in the background (a WorkManager job is not an
     * exemption), so a `false` result is normal for a scheduled run and must not
     * abort the export: the background read permission is what makes the read
     * legal. The reason is logged so a real failure is still visible.
     */
    suspend fun promoteToForeground(worker: CoroutineWorker): Boolean =
        try {
            worker.setForeground(
                foregroundInfo(worker.applicationContext, SERVICE_TYPE),
            )
            true
        } catch (e: Exception) {
            Log.w(TAG, "could not enter foreground: ${e.message}", e)
            false
        }

    private fun buildNotification(context: Context): Notification =
        NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_export_title))
            .setContentText(context.getString(R.string.notification_export_text))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
}
