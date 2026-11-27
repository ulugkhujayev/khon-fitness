package dev.mirzohidkhon.khonfitness.health

import android.content.Context
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dev.mirzohidkhon.khonfitness.data.HrRange
import dev.mirzohidkhon.khonfitness.data.NoSleepAccess
import dev.mirzohidkhon.khonfitness.data.SleepSource
import dev.mirzohidkhon.khonfitness.data.SleepSpan
import dev.mirzohidkhon.khonfitness.data.SleepStage
import dev.mirzohidkhon.khonfitness.data.askRange
import dev.mirzohidkhon.khonfitness.data.readEveryPage
import dev.mirzohidkhon.khonfitness.data.sleepSpan
import java.time.Instant

/** Sleep sessions from Health Connect. It reads only; it needs only the sleep read permission and never asks for one. */
class HealthSleepSource(private val context: Context) : SleepSource {
    override suspend fun spans(range: HrRange): List<SleepSpan> {
        if (HealthImport.status(context) != HcStatus.AVAILABLE) throw NoSleepAccess("Health Connect is not available")
        val client = HealthImport.client(context)
        if (SLEEP_READ !in client.permissionController.getGrantedPermissions()) throw NoSleepAccess("no sleep permission")
        val ask = askRange(range)
        val filter = TimeRangeFilter.between(Instant.ofEpochMilli(ask.fromMs), Instant.ofEpochMilli(ask.toMs))
        return try {
            readEveryPage { token ->
                val page = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, filter, pageToken = token))
                page.records.map { it.span() } to page.pageToken
            }
        } catch (e: SecurityException) {
            throw NoSleepAccess("sleep permission revoked")
        }
    }

    companion object {
        val SLEEP_READ = HealthPermission.getReadPermission(SleepSessionRecord::class)
    }
}

val AWAKE_STAGES = setOf(
    SleepSessionRecord.STAGE_TYPE_AWAKE,
    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED,
    SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
)

fun SleepSessionRecord.span(): SleepSpan = sleepSpan(
    metadata.dataOrigin.packageName,
    startTime.toEpochMilli(),
    endTime.toEpochMilli(),
    stages.map { SleepStage(it.stage, it.startTime.toEpochMilli(), it.endTime.toEpochMilli()) },
    AWAKE_STAGES,
)
