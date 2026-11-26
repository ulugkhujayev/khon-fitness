package dev.mirzohidkhon.khonfitness.health

import android.content.Context
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dev.mirzohidkhon.khonfitness.data.HeartPage
import dev.mirzohidkhon.khonfitness.data.HeartPoint
import dev.mirzohidkhon.khonfitness.data.HeartRecord
import dev.mirzohidkhon.khonfitness.data.HeartSource
import dev.mirzohidkhon.khonfitness.data.HrRange
import dev.mirzohidkhon.khonfitness.data.readAllPages
import java.time.Instant

/** Heart rate from Health Connect. It reads only; it needs only the heart rate read permission and never asks for one. */
class HealthHeartSource(private val context: Context) : HeartSource {
    override suspend fun records(range: HrRange): List<HeartRecord> {
        check(HealthImport.status(context) == HcStatus.AVAILABLE) { "Health Connect is not available" }
        val client = HealthImport.client(context)
        check(HEART_READ in client.permissionController.getGrantedPermissions()) { "no heart rate permission" }
        val filter = TimeRangeFilter.between(Instant.ofEpochMilli(range.fromMs), Instant.ofEpochMilli(range.toMs))
        return readAllPages { token ->
            val page = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, filter, pageToken = token))
            HeartPage(page.records.map { it.toHeartRecord() }, page.pageToken)
        }
    }

    companion object {
        val HEART_READ = HealthPermission.getReadPermission(HeartRateRecord::class)
    }
}

private fun HeartRateRecord.toHeartRecord() = HeartRecord(
    metadata.dataOrigin.packageName,
    samples.map { HeartPoint(it.time.toEpochMilli(), it.beatsPerMinute.coerceIn(0L, 1000L).toInt()) },
)
