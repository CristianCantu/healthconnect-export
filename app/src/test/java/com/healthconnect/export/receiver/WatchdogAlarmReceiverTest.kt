package com.healthconnect.export.receiver

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.healthconnect.export.util.DeliveryLog
import com.healthconnect.export.util.ExportSettings
import com.healthconnect.export.worker.DeliveryWatchdog
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WatchdogAlarmReceiverTest {
    private lateinit var context: Context
    private val receiver = WatchdogAlarmReceiver()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().build())
        ExportSettings
            .prefs(context)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `alarm enqueues the catch-up delivery`() {
        // The alarm only fills a real gap: an automatic delivery must be configured
        // and the last success must be older than the watchdog interval.
        ExportSettings
            .prefs(context)
            .edit()
            .putString(ExportSettings.KEY_WEBHOOK_URL, "https://example.com/hook")
            .putBoolean(ExportSettings.KEY_AUTO_SEND_WEBHOOK, true)
            .commit()
        DeliveryLog.recordSuccess(
            context = context,
            trigger = "seed",
            deliveredDates = listOf("2026-09-20"),
            now = System.currentTimeMillis() - 30L * 3_600_000L,
            statusCode = 200,
        )

        receiver.onReceive(context, Intent())

        val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(DeliveryWatchdog.CATCH_UP_WORK_NAME).get()
        assertNotNull(infos.first())
    }

    @Test
    fun `alarm without automatic delivery enqueues no catch-up`() {
        receiver.onReceive(context, Intent())

        val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(DeliveryWatchdog.CATCH_UP_WORK_NAME).get()
        org.junit.Assert.assertTrue(infos.isEmpty())
    }
}
