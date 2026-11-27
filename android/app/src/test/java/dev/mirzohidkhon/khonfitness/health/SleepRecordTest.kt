package dev.mirzohidkhon.khonfitness.health

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import dev.mirzohidkhon.khonfitness.data.SleepSpan
import dev.mirzohidkhon.khonfitness.data.TimeSpan
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepRecordTest {
    @Test fun `D1 the manifest asks for sleep and the Import set keeps the three old permissions`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android.permission.health.READ_SLEEP"))
        assertEquals("android.permission.health.READ_SLEEP", HealthSleepSource.SLEEP_READ)
        assertEquals(setOf(
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
        ), HealthImport.readPermissions)
        assertFalse(HealthSleepSource.SLEEP_READ in HealthImport.readPermissions)
    }

    private val base = Instant.parse("2026-11-02T18:00:00Z")
    private fun at(min: Long) = base.plusSeconds(min * 60)

    @Test fun `D12 a record maps its origin, bounds, and only its awake stages`() {
        val types = listOf(
            SleepSessionRecord.STAGE_TYPE_AWAKE, SleepSessionRecord.STAGE_TYPE_LIGHT, SleepSessionRecord.STAGE_TYPE_DEEP,
            SleepSessionRecord.STAGE_TYPE_REM, SleepSessionRecord.STAGE_TYPE_OUT_OF_BED, SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
            SleepSessionRecord.STAGE_TYPE_UNKNOWN, SleepSessionRecord.STAGE_TYPE_SLEEPING,
        )
        val record = SleepSessionRecord(
            startTime = at(0), startZoneOffset = ZoneOffset.ofHours(5), endTime = at(80), endZoneOffset = ZoneOffset.ofHours(5),
            metadata = Metadata.manualEntry(),
            stages = types.mapIndexed { i, type -> SleepSessionRecord.Stage(at(i * 10L), at(i * 10L + 10), type) },
        )
        val span = record.span()
        val ms = { min: Long -> at(min).toEpochMilli() }
        assertEquals(ms(0), span.startMs)
        assertEquals(ms(80), span.endMs)
        assertEquals(listOf(TimeSpan(ms(0), ms(10)), TimeSpan(ms(40), ms(50)), TimeSpan(ms(50), ms(60))), span.awake)
        val plain = SleepSessionRecord(
            startTime = at(0), startZoneOffset = null, endTime = at(80), endZoneOffset = null, metadata = Metadata.manualEntry(),
        ).span()
        assertEquals(SleepSpan(plain.origin, ms(0), ms(80), emptyList()), plain)
    }
}
