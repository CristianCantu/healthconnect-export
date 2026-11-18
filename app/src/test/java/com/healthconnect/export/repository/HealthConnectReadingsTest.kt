package com.healthconnect.export.repository

import android.content.Context
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.metadata.Metadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Tests for the detailed readings added next to the daily aggregates (issue #85):
 * timestamp formatting, ordering across zone offsets and the payload cap.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthConnectReadingsTest {
    private val repo = HealthConnectRepository(mock<Context>())

    private fun heartRateRecord(
        offset: ZoneOffset,
        samples: List<Pair<Instant, Long>>,
        metadata: Metadata = Metadata.unknownRecordingMethod(null),
    ) = HeartRateRecord(
        startTime = samples.minOf { it.first },
        startZoneOffset = offset,
        endTime = samples.maxOf { it.first },
        endZoneOffset = offset,
        samples = samples.map { (time, bpm) -> HeartRateRecord.Sample(time = time, beatsPerMinute = bpm) },
        metadata = metadata,
    )

    // =============================================
    // localIso
    // =============================================

    @Test
    fun `localIso uses the record offset and keeps milliseconds`() {
        val instant = Instant.parse("2026-09-19T10:22:41.123Z")

        assertEquals("2026-09-19T05:22:41.123-05:00", repo.localIso(instant, ZoneOffset.ofHours(-5)))
        assertEquals("2026-09-19T13:22:41.123+03:00", repo.localIso(instant, ZoneOffset.ofHours(3)))
    }

    @Test
    fun `localIso falls back to the device zone without an offset`() {
        val instant = Instant.parse("2026-09-19T10:22:41Z")
        val expected = instant.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()

        assertEquals(expected, repo.localIso(instant, null))
    }

    // =============================================
    // heartRateReadings
    // =============================================

    @Test
    fun `heartRateReadings orders samples by instant, not by formatted string`() {
        // 06:00-05:00 is 11:00Z, 09:00+00:00 is 09:00Z: as text the second one sorts first,
        // chronologically it comes later. Both must end up in instant order.
        val earlier = heartRateRecord(ZoneOffset.ofHours(-5), listOf(Instant.parse("2026-09-19T11:00:00Z") to 60L))
        val later = heartRateRecord(ZoneOffset.UTC, listOf(Instant.parse("2026-09-19T09:00:00Z") to 90L))

        val readings = repo.heartRateReadings(listOf(earlier, later))

        assertEquals(listOf(90L, 60L), readings.map { it.bpm })
        // OffsetDateTime.toString() drops a zero seconds field, which is valid ISO-8601.
        assertEquals("2026-09-19T09:00Z", readings[0].time)
        assertEquals("2026-09-19T06:00-05:00", readings[1].time)
    }

    @Test
    fun `heartRateReadings carries the bpm, the record offset and a source package`() {
        // Records read from Health Connect always carry metadata.dataOrigin; the public
        // API cannot set it, so an unknown origin (empty package) is what a hand-built
        // record yields — the point here is that the field is filled from metadata and
        // that the record's own offset is used for the timestamp.
        val record =
            heartRateRecord(
                ZoneOffset.ofHours(3),
                listOf(Instant.parse("2026-09-19T06:00:00Z") to 72L),
            )

        val readings = repo.heartRateReadings(listOf(record))

        assertEquals(1, readings.size)
        assertEquals(72L, readings[0].bpm)
        assertEquals("2026-09-19T09:00+03:00", readings[0].time)
        assertEquals(record.metadata.dataOrigin.packageName, readings[0].source)
    }

    @Test
    fun `heartRateReadings caps a pathological day`() {
        // One sample per second for a whole day = 86 400 samples.
        val start = Instant.parse("2026-09-19T00:00:00Z")
        val record =
            heartRateRecord(
                ZoneOffset.UTC,
                (0 until 86_400).map { start.plusSeconds(it.toLong()) to 70L },
            )

        val readings = repo.heartRateReadings(listOf(record))

        assertEquals(HealthConnectRepository.MAX_READINGS_PER_DAY, readings.size)
        // Evenly subsampled: first and last samples survive, the span stays the whole day.
        assertEquals("2026-09-19T00:00Z", readings.first().time)
        assertEquals("2026-09-19T23:59:59Z", readings.last().time)
    }

    // =============================================
    // capReadings
    // =============================================

    @Test
    fun `capReadings returns short lists untouched`() {
        val readings = listOf(1, 2, 3)

        assertEquals(readings, readings.capReadings(max = 3))
        assertEquals(readings, readings.capReadings(max = 10))
        assertEquals(emptyList<Int>(), emptyList<Int>().capReadings())
    }

    @Test
    fun `capReadings subsamples evenly and keeps both ends`() {
        val readings = (0 until 100).toList()

        val capped = readings.capReadings(max = 5)

        assertEquals(5, capped.size)
        assertEquals(0, capped.first())
        assertEquals(99, capped.last())
        // No two neighbours further apart than the stride.
        assertTrue(capped.zipWithNext().all { (a, b) -> b - a <= 25 })
    }

    @Test
    fun `capReadings leaves the list alone when the cap is degenerate`() {
        val readings = (0 until 10).toList()

        assertEquals(readings, readings.capReadings(max = 1))
    }
}
