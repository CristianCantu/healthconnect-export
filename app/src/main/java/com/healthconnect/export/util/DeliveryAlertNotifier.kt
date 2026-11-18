package com.healthconnect.export.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.healthconnect.export.R

/**
 * User-visible notification for a broken background delivery pipeline.
 *
 * A permission or configuration problem used to surface only in the Schedule tab,
 * so the scheduled webhook could fail for days without the user noticing. The
 * alert is posted once per outage (a persisted flag) and cancelled on the next
 * successful delivery, which keeps a failing 2-hourly job from spamming it.
 */
object DeliveryAlertNotifier {
    const val CHANNEL_ID = "health_export_alerts"
    const val NOTIFICATION_ID = 1002

    private const val KEY_ALERT_SHOWN = "delivery_alert_shown"
    private const val TAG = "DeliveryAlert"
    private const val MAX_DETAIL_LENGTH = 180

    /**
     * Posts the alert unless one is already pending.
     *
     * Everything is guarded: a background delivery must never fail because the
     * notification could not be built (missing resources, disabled notifications,
     * a minimized/test process).
     */
    fun notifyFailure(
        context: Context,
        detail: String,
    ) {
        try {
            // Android 13+ silently drops notifications without the runtime
            // permission; returning before markShown keeps the alert pending for
            // the first failure after the user grants it.
            if (!canPostNotifications(context)) {
                Log.i(TAG, "notification permission missing — delivery alert skipped")
                return
            }
            val manager = notificationManager(context) ?: return
            if (!markShown(context)) return

            val text = context.getString(R.string.notification_delivery_failed_text, detail.take(MAX_DETAIL_LENGTH))
            ensureChannel(manager, context)
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setContentTitle(context.getString(R.string.notification_delivery_failed_title))
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "could not post delivery alert: ${e.message}")
        }
    }

    /** Cancels a pending alert; called on every successful delivery. */
    fun clear(context: Context) {
        try {
            notificationManager(context)?.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "could not cancel delivery alert: ${e.message}")
        }
        runCatching {
            DeliveryLog
                .prefs(context)
                .edit()
                .remove(KEY_ALERT_SHOWN)
                .apply()
        }
    }

    /** True when posting notifications is allowed on this device/OS version. */
    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** True when this call posted the alert, i.e. no alert was pending yet. */
    private fun markShown(context: Context): Boolean {
        val prefs = DeliveryLog.prefs(context)
        if (prefs.getBoolean(KEY_ALERT_SHOWN, false)) return false
        prefs.edit().putBoolean(KEY_ALERT_SHOWN, true).apply()
        return true
    }

    private fun ensureChannel(
        manager: NotificationManager,
        context: Context,
    ) {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_alerts),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_alerts_desc)
            }
        manager.createNotificationChannel(channel)
    }

    private fun notificationManager(context: Context): NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
}
