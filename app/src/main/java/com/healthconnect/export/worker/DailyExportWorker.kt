package com.healthconnect.export.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.healthconnect.export.data.ExportConfig
import com.healthconnect.export.data.ExportFrequency
import com.healthconnect.export.data.HealthDataType
import com.healthconnect.export.repository.GoogleDriveRepository
import com.healthconnect.export.repository.HealthConnectRepository
import com.healthconnect.export.repository.LocalExportRepository
import com.healthconnect.export.repository.WebhookRepository
import com.healthconnect.export.repository.WebhookResult
import com.healthconnect.export.util.DeliveryLog
import com.healthconnect.export.util.ExportForegroundNotifier
import com.healthconnect.export.util.ExportSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Background worker that exports yesterday's health data and optionally syncs to Drive
 */
class DailyExportWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    companion object {
        const val WORK_NAME = "daily_health_export"
        const val KEY_CONFIG = "export_config"
        private const val TAG = "DailyExportWorker"
        private const val TRIGGER = "daily"
        private val json = Json { ignoreUnknownKeys = true }

        fun schedule(
            context: Context,
            config: ExportConfig,
        ) {
            // The every-2-hours webhook has its own switch: switching the export
            // frequency to Manual must not silently disable it.
            scheduleEvery2HoursWebhook(context, config)

            if (config.frequency == ExportFrequency.MANUAL) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                return
            }

            val inputData =
                workDataOf(
                    KEY_CONFIG to json.encodeToString(config),
                )

            val requestBuilder =
                PeriodicWorkRequestBuilder<DailyExportWorker>(
                    config.frequency.hours,
                    TimeUnit.HOURS,
                )
                    .setInputData(inputData)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)

            // If a time of day is configured, delay the first run until the next
            // occurrence of that hour (subsequent runs repeat every period).
            config.scheduleHour?.let { hour ->
                val now = LocalTime.now()
                val target = LocalTime.of(hour.coerceIn(0, 23), 0)
                var delayMinutes = ChronoUnit.MINUTES.between(now, target)
                if (delayMinutes <= 0) delayMinutes += 24 * 60
                requestBuilder.setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            }

            val request = requestBuilder.build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                // UPDATE keeps the existing schedule but lets a changed
                // configuration reach the already enqueued job (KEEP froze the
                // webhook URL/token of the first enqueue forever).
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Every2HoursWebhookWorker.cancel(context)
        }

        /** Cancels only the periodic export, leaving the every-2-hours webhook job alone. */
        fun cancelDaily(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /**
         * Schedules or cancels the every-2-hours webhook periodic work.
         */
        fun scheduleEvery2HoursWebhook(
            context: Context,
            config: ExportConfig,
        ) {
            if (config.autoSendWebhookEvery2Hours && config.webhookUrl.isNotBlank()) {
                Every2HoursWebhookWorker.schedule(context, config)
            } else {
                Every2HoursWebhookWorker.cancel(context)
            }
        }

        fun cancelEvery2HoursWebhook(context: Context) {
            Every2HoursWebhookWorker.cancel(context)
        }

        fun getStatus(context: Context) = WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(WORK_NAME)
    }

    private val healthRepo = HealthConnectRepository(applicationContext)
    private val localRepo = LocalExportRepository(applicationContext)
    private val driveRepo = GoogleDriveRepository(applicationContext)
    private val webhookRepo = WebhookRepository(applicationContext)

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val config = resolveConfig()
            try {
                // Best-effort only: Android 12+ refuses the promotion while the app
                // runs in the background, and the background read permission is what
                // makes the read legal there.
                ExportForegroundNotifier.promoteToForeground(this@DailyExportWorker)

                // Export completed days only. This avoids repeatedly sending a partial
                // current-day record from a periodic worker.
                val endDate = LocalDate.now().minusDays(1)
                val startDate =
                    when (config.frequency) {
                        ExportFrequency.DAILY -> endDate
                        ExportFrequency.WEEKLY -> endDate.minusDays(6)
                        ExportFrequency.MANUAL -> endDate
                    }

                // Read health data for the period (batch mode: 1 API call per type)
                val records =
                    healthRepo.readPeriodInBatch(
                        startDate = startDate,
                        endDate = endDate,
                        types = config.enabledTypes,
                        selectedSourcePackage = config.selectedSourcePackage,
                    )

                // The repository pre-seeds every day of the window, so "empty" means
                // "no day carries a single record", not "empty list".
                val recordsWithData = records.filter { it.hasAnyData() }
                if (recordsWithData.isEmpty()) {
                    Log.w(TAG, "doWork: no health data returned for period $startDate..$endDate")
                    DeliveryLog.recordIdle(applicationContext, TRIGGER, "no data $startDate..$endDate")
                    return@withContext Result.success()
                }

                // Save locally
                val files = localRepo.saveRecords(records, config)

                // Sync to Drive if enabled and signed in
                if (config.autoSyncDrive && driveRepo.isSignedIn()) {
                    val driveResults =
                        files.map { file ->
                            driveRepo.uploadFile(file, "HealthConnectExport/${file.name}")
                        }
                    if (driveResults.any { it == null }) {
                        return@withContext DeliveryOutcome.failure(
                            applicationContext,
                            TRIGGER,
                            "Drive upload failed",
                            retry = true,
                        )
                    }
                }

                // Send to webhook if enabled
                if (config.autoSendWebhook && config.webhookUrl.isNotBlank()) {
                    when (val result = webhookRepo.sendRecords(config.webhookUrl, recordsWithData, config.webhookAuthToken)) {
                        is WebhookResult.Success ->
                            DeliveryOutcome.delivered(applicationContext, TRIGGER, recordsWithData.map { it.date }, result.statusCode)
                        is WebhookResult.Error ->
                            DeliveryOutcome.failure(
                                applicationContext,
                                TRIGGER,
                                "HTTP ${result.statusCode}: ${result.message}",
                                retry = result.isRetryable,
                            )
                    }
                } else {
                    Result.success()
                }
            } catch (e: SecurityException) {
                // Retry only when the background permission is granted; otherwise the
                // run would fail identically forever and never deliver anything.
                DeliveryOutcome.permissionFailure(applicationContext, TRIGGER, healthRepo, e)
            } catch (e: IllegalStateException) {
                DeliveryOutcome.failure(applicationContext, TRIGGER, "health connect: ${e.message}", retry = true)
            } catch (e: Exception) {
                DeliveryOutcome.failure(applicationContext, TRIGGER, e.message ?: e.toString(), retry = true)
            }
        }

    /**
     * Input data carries the configuration snapshot taken when the job was
     * enqueued; the webhook settings are refreshed from the persisted values so
     * a URL/token changed (or cleared) later still applies to that job.
     */
    private fun resolveConfig(): ExportConfig {
        val persisted = ExportSettings.loadConfig(applicationContext)
        val base =
            inputData.getString(KEY_CONFIG)?.let { configJson ->
                runCatching { Json.decodeFromString<ExportConfig>(configJson) }.getOrNull()
            } ?: ExportConfig(
                enabledTypes = HealthDataType.entries.toSet(),
                frequency = ExportFrequency.DAILY,
                autoSyncDrive = true,
            )
        if (!ExportSettings.hasStoredWebhookUrl(applicationContext)) return base

        return base.copy(
            webhookUrl = persisted.webhookUrl,
            webhookAuthToken = persisted.webhookAuthToken,
            autoSendWebhook = if (ExportSettings.hasStoredAutoSendWebhook(applicationContext)) persisted.autoSendWebhook else base.autoSendWebhook,
            autoSendWebhookEvery2Hours = persisted.autoSendWebhookEvery2Hours,
        )
    }
}
