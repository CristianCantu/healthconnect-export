package com.healthconnect.export.worker

import android.content.Context
import androidx.work.*
import com.healthconnect.export.data.ExportConfig
import com.healthconnect.export.repository.HealthConnectRepository
import com.healthconnect.export.repository.WebhookRepository
import com.healthconnect.export.repository.WebhookResult
import com.healthconnect.export.util.DeliveryLog
import com.healthconnect.export.util.ExportForegroundNotifier
import com.healthconnect.export.util.ExportSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Background worker that reads today's health data every 2 hours
 * and sends it to the configured webhook (without local save or Drive sync).
 */
class Every2HoursWebhookWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    companion object {
        const val WORK_NAME = "every_2_hours_webhook"
        const val KEY_CONFIG = "webhook_config"
        private const val TRIGGER = "every-2h"
        private val json = Json { ignoreUnknownKeys = true }

        fun schedule(
            context: Context,
            config: ExportConfig,
        ) {
            if (config.webhookUrl.isBlank()) {
                cancel(context)
                return
            }

            val inputData =
                workDataOf(
                    KEY_CONFIG to json.encodeToString(config),
                )

            val request =
                PeriodicWorkRequestBuilder<Every2HoursWebhookWorker>(
                    2,
                    TimeUnit.HOURS,
                ).setInputData(inputData)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                    .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    private val healthRepo = HealthConnectRepository(applicationContext)
    private val webhookRepo = WebhookRepository(applicationContext)

    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val config = resolveConfig()
            try {
                if (!ExportSettings.hasWebhook(config)) {
                    return@withContext Result.success()
                }

                // Best-effort only: Android 12+ refuses the promotion while the app
                // runs in the background, and the background read permission is what
                // makes the read legal there.
                ExportForegroundNotifier.promoteToForeground(this@Every2HoursWebhookWorker)

                // Read today's data. Days without a single record are pre-seeded by
                // the repository, so they are dropped here: an empty payload must not
                // count as a delivered day.
                val today = LocalDate.now()
                val records =
                    healthRepo
                        .readPeriodInBatch(
                            startDate = today,
                            endDate = today,
                            types = config.enabledTypes,
                            selectedSourcePackage = config.selectedSourcePackage,
                        ).filter { it.hasAnyData() }

                if (records.isEmpty()) {
                    DeliveryLog.recordIdle(applicationContext, TRIGGER, "no health data $today")
                    return@withContext Result.success()
                }

                // Send to webhook (no local save, no Drive sync)
                when (val result = webhookRepo.sendRecords(config.webhookUrl, records, config.webhookAuthToken)) {
                    is WebhookResult.Success ->
                        DeliveryOutcome.delivered(applicationContext, TRIGGER, records.map { it.date }, result.statusCode)
                    is WebhookResult.Error ->
                        DeliveryOutcome.failure(
                            applicationContext,
                            TRIGGER,
                            "HTTP ${result.statusCode}: ${result.message}",
                            retry = result.isRetryable,
                        )
                }
            } catch (e: SecurityException) {
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
        val fromInput =
            inputData.getString(KEY_CONFIG)?.let { configJson ->
                runCatching { Json.decodeFromString<ExportConfig>(configJson) }.getOrNull()
            } ?: return ExportSettings.loadConfig(applicationContext)

        if (!ExportSettings.hasStoredWebhookUrl(applicationContext)) return fromInput

        val persisted = ExportSettings.loadConfig(applicationContext)
        return fromInput.copy(
            webhookUrl = persisted.webhookUrl,
            webhookAuthToken = persisted.webhookAuthToken,
        )
    }
}
