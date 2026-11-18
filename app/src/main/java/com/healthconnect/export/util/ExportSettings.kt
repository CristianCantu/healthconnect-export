package com.healthconnect.export.util

import android.content.Context
import android.content.SharedPreferences
import com.healthconnect.export.data.ExportConfig
import com.healthconnect.export.data.ExportFormat
import com.healthconnect.export.data.ExportFrequency
import com.healthconnect.export.data.HealthDataType

/**
 * Single reader of the persisted export/webhook settings.
 *
 * Background components (WorkManager jobs, the boot receiver, the watchdog
 * alarm) run without the UI and without in-memory state, so they rebuild the
 * configuration from SharedPreferences on every run. Reading the config at run
 * time — instead of trusting a snapshot frozen in the job's input data —
 * guarantees that a changed webhook URL, token or type selection reaches the
 * already enqueued jobs.
 */
object ExportSettings {
    const val PREFS_NAME = "healthconnect_export_prefs"

    const val KEY_WEBHOOK_URL = "webhook_url"
    const val KEY_WEBHOOK_TOKEN = "webhook_auth_token"
    const val KEY_AUTO_SEND_WEBHOOK = "auto_send_webhook"
    const val KEY_AUTO_SEND_WEBHOOK_EVERY_2_HOURS = "auto_send_webhook_every_2_hours"
    const val KEY_AUTO_SYNC_DRIVE = "auto_sync_drive"
    const val KEY_SELECTED_TYPES = "selected_types"
    const val KEY_SOURCE_PACKAGE = "selected_source_package"
    const val KEY_EXPORT_FORMAT = "export_format"
    const val KEY_SCHEDULE_HOUR = "schedule_hour"
    const val KEY_FREQUENCY = "export_frequency"

    fun prefs(context: Context): SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Builds an [ExportConfig] from the persisted settings. */
    fun loadConfig(context: Context): ExportConfig {
        val prefs = prefs(context)
        return ExportConfig(
            enabledTypes = loadTypes(prefs),
            frequency = loadFrequency(context),
            autoSyncDrive = prefs.getBoolean(KEY_AUTO_SYNC_DRIVE, true),
            webhookUrl = prefs.getString(KEY_WEBHOOK_URL, "") ?: "",
            webhookAuthToken = prefs.getString(KEY_WEBHOOK_TOKEN, "") ?: "",
            autoSendWebhook = prefs.getBoolean(KEY_AUTO_SEND_WEBHOOK, false),
            autoSendWebhookEvery2Hours = prefs.getBoolean(KEY_AUTO_SEND_WEBHOOK_EVERY_2_HOURS, false),
            selectedSourcePackage = prefs.getString(KEY_SOURCE_PACKAGE, null),
            exportFormat = enumOf(prefs.getString(KEY_EXPORT_FORMAT, null), ExportFormat.entries, ExportFormat.JSON),
            scheduleHour = prefs.getInt(KEY_SCHEDULE_HOUR, -1).takeIf { it in 0..23 },
        )
    }

    fun loadFrequency(context: Context): ExportFrequency =
        enumOf(prefs(context).getString(KEY_FREQUENCY, null), ExportFrequency.entries, ExportFrequency.DAILY)

    fun saveFrequency(
        context: Context,
        frequency: ExportFrequency,
    ) {
        prefs(context).edit().putString(KEY_FREQUENCY, frequency.name).apply()
    }

    /** True when a webhook delivery can be attempted at all. */
    fun hasWebhook(config: ExportConfig): Boolean = config.webhookUrl.isNotBlank()

    /** True when either automatic delivery path is enabled (daily export or every-2-hours). */
    fun isAutoDeliveryEnabled(config: ExportConfig): Boolean = config.autoSendWebhook || config.autoSendWebhookEvery2Hours

    /**
     * True when the user saved a webhook URL, even an empty one.
     *
     * The background workers carry a configuration snapshot in their input data,
     * which WorkManager keeps for the whole life of the job. Without this check a
     * URL the user cleared kept receiving data through the old snapshot.
     */
    fun hasStoredWebhookUrl(context: Context): Boolean = prefs(context).contains(KEY_WEBHOOK_URL)

    /** True when the user saved the automatic webhook switches (as opposed to defaults). */
    fun hasStoredAutoSendWebhook(context: Context): Boolean = prefs(context).contains(KEY_AUTO_SEND_WEBHOOK)

    private fun loadTypes(prefs: SharedPreferences): Set<HealthDataType> {
        val names = prefs.getStringSet(KEY_SELECTED_TYPES, null) ?: return HealthDataType.entries.toSet()
        val types = names.mapNotNull { name -> HealthDataType.entries.firstOrNull { it.name == name } }
        return types.toSet().ifEmpty { HealthDataType.entries.toSet() }
    }

    private fun <T : Enum<T>> enumOf(
        name: String?,
        values: List<T>,
        fallback: T,
    ): T = name?.let { saved -> values.firstOrNull { it.name == saved } } ?: fallback
}
