package com.healthconnect.export.worker

import android.content.Context
import androidx.work.ListenableWorker
import com.healthconnect.export.repository.HealthConnectRepository
import com.healthconnect.export.util.DeliveryAlertNotifier
import com.healthconnect.export.util.DeliveryLog

/**
 * Shared "retry or not" decision for the background delivery workers.
 *
 * Two rules came out of the stalled-delivery review:
 *  - a Health Connect permission rejection must not spin forever with exponential
 *    backoff while the user cannot grant anything; the run reports success, the
 *    failure is logged and the user is notified, and the next scheduled run
 *    heals itself as soon as the permission is in place;
 *  - a permanent webhook error (4xx) must not be retried either, only network
 *    and server problems are worth another attempt.
 */
internal object DeliveryOutcome {
    /** Records a delivery attempt that failed and decides whether WorkManager retries. */
    fun failure(
        context: Context,
        trigger: String,
        detail: String,
        retry: Boolean,
    ): ListenableWorker.Result {
        DeliveryLog.recordFailure(context, trigger, detail)
        DeliveryAlertNotifier.notifyFailure(context, detail)
        return if (retry) ListenableWorker.Result.retry() else ListenableWorker.Result.success()
    }

    /**
     * A ReadRecords call was rejected by Health Connect.
     *
     * Retrying only makes sense when the background read permission is actually
     * granted; otherwise every attempt fails the same way until the user grants it.
     */
    suspend fun permissionFailure(
        context: Context,
        trigger: String,
        healthRepo: HealthConnectRepository,
        error: Throwable,
    ): ListenableWorker.Result {
        // A failing permission probe is treated as "granted" so the job keeps
        // retrying instead of silently giving up.
        val missingBackgroundPermission = runCatching { !healthRepo.hasBackgroundReadPermission() }.getOrDefault(false)
        val detail =
            if (missingBackgroundPermission) {
                "permissions: ${error.message} (READ_HEALTH_DATA_IN_BACKGROUND not granted)"
            } else {
                "permissions: ${error.message}"
            }
        return failure(context, trigger, detail, retry = !missingBackgroundPermission)
    }

    /** Records a successful delivery and dismisses a pending failure alert. */
    fun delivered(
        context: Context,
        trigger: String,
        deliveredDates: List<String>,
        statusCode: Int?,
    ): ListenableWorker.Result {
        DeliveryLog.recordSuccess(context, trigger, deliveredDates, statusCode = statusCode)
        DeliveryAlertNotifier.clear(context)
        return ListenableWorker.Result.success()
    }
}
