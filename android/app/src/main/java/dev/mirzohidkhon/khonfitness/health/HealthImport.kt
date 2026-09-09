package dev.mirzohidkhon.khonfitness.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import dev.mirzohidkhon.khonfitness.BuildConfig
import dev.mirzohidkhon.khonfitness.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** One band session that is not in the app yet. */
data class ImportDraft(
    val sourceId: String,
    val start: Instant,
    val end: Instant,
    val typeName: String,
    val modalityId: String?,
    val distanceM: Int?,
    val avgHr: Int?,
    val hrSamples: List<Pair<Instant, Int>>,
    val origins: List<String>,
    val laps: Int? = null,
    val poolLength: Int? = null,
    val mergedIds: List<String> = listOf(sourceId),
) {
    val date: LocalDate get() = start.atZone(ZoneId.systemDefault()).toLocalDate()
    val seconds: Int get() = ChronoUnit.SECONDS.between(start, end).toInt()
    val originLabel: String get() = origins.joinToString(" + ") { HealthImport.appName(it) }
}

enum class HcStatus { AVAILABLE, NOT_INSTALLED, UPDATE_REQUIRED }

object HealthImport {
    val readPermissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
    )

    fun status(context: Context): HcStatus = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HcStatus.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HcStatus.UPDATE_REQUIRED
        else -> HcStatus.NOT_INSTALLED
    }

    fun client(context: Context): HealthConnectClient = HealthConnectClient.getOrCreate(context)

    fun appName(pkg: String): String = when (pkg) {
        "com.xiaomi.wearable" -> "Mi Fitness"
        "com.mi.health" -> "Mi Fitness"
        "com.sec.android.app.shealth" -> "Samsung Health"
        "com.google.android.apps.fitness" -> "Google Fit"
        "com.garmin.android.apps.connectmobile" -> "Garmin"
        "com.strava" -> "Strava"
        else -> pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    suspend fun hasPermissions(context: Context): Boolean =
        client(context).permissionController.getGrantedPermissions().containsAll(readPermissions)

    /** Maps a Health Connect exercise type to one of the seeded modality ids. */
    fun modalityFor(type: Int): Pair<String, String?> = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "Run" to "run"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL, ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Swim" to "swim"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING, ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Bike" to "bike"
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING, ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "Row" to "row"
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT" to null
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walk" to null
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Hike" to null
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "Elliptical" to null
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Strength" to null
        else -> "Workout" to null
    }

    /** Sessions from the last [days] days that the app has not imported yet. */
    suspend fun drafts(context: Context, days: Long, alreadyImported: Set<String>): List<ImportDraft> {
        val c = client(context)
        val end = Instant.now(); val start = end.minus(days, ChronoUnit.DAYS)
        val sessions = c.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(start, end))).records
        return sessions.filter { it.metadata.id !in alreadyImported && (BuildConfig.DEBUG || it.metadata.dataOrigin.packageName != context.packageName) }.map { rec ->
            val range = TimeRangeFilter.between(rec.startTime, rec.endTime)
            val distance = runCatching { c.aggregate(AggregateRequest(setOf(DistanceRecord.DISTANCE_TOTAL), range))[DistanceRecord.DISTANCE_TOTAL]?.inMeters }.getOrNull()
            val samples = runCatching { c.readRecords(ReadRecordsRequest(HeartRateRecord::class, range)).records.flatMap { r -> r.samples.map { it.time to it.beatsPerMinute.toInt() } }.sortedBy { it.first } }.getOrDefault(emptyList())
            val (name, modality) = modalityFor(rec.exerciseType)
            val lapLengths = rec.laps.mapNotNull { it.length?.inMeters }.filter { it > 0 }
            ImportDraft(
                sourceId = rec.metadata.id, start = rec.startTime, end = rec.endTime, typeName = name,
                modalityId = modality, distanceM = distance?.toInt()?.takeIf { it > 0 }, avgHr = samples.takeIf { it.isNotEmpty() }?.let { s -> s.map { it.second }.average().toInt() },
                hrSamples = samples, origins = listOf(rec.metadata.dataOrigin.packageName),
                laps = rec.laps.size.takeIf { it > 0 }, poolLength = lapLengths.takeIf { it.isNotEmpty() }?.sorted()?.let { it[it.size / 2].toInt() },
            )
        }.sortedByDescending { it.start }.let(::mergeOverlaps)
    }

    /** The band and the watch both record the same workout. Sessions that overlap in time become one draft. */
    fun mergeOverlaps(drafts: List<ImportDraft>): List<ImportDraft> {
        val out = mutableListOf<ImportDraft>()
        for (d in drafts) {
            val i = out.indexOfFirst { o -> o.modalityId == d.modalityId && d.start < o.end && d.end > o.start }
            if (i < 0) { out += d; continue }
            val o = out[i]
            val base = if (d.hrSamples.size > o.hrSamples.size) d else o
            val other = if (base === d) o else d
            out[i] = base.copy(
                start = minOf(o.start, d.start), end = maxOf(o.end, d.end),
                distanceM = base.distanceM ?: other.distanceM, avgHr = base.avgHr ?: other.avgHr,
                laps = base.laps ?: other.laps, poolLength = base.poolLength ?: other.poolLength,
                origins = (o.origins + d.origins).distinct(), mergedIds = (o.mergedIds + d.mergedIds).distinct(),
            )
        }
        return out
    }

    /** How many band sessions wait for import. Cheap enough to run when Today opens. */
    suspend fun pendingCount(context: Context, alreadyImported: Set<String>): Int {
        if (status(context) != HcStatus.AVAILABLE || !hasPermissions(context)) return 0
        val c = client(context)
        val end = Instant.now(); val start = end.minus(60, ChronoUnit.DAYS)
        val sessions = c.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(start, end))).records
            .filter { it.metadata.id !in alreadyImported && (BuildConfig.DEBUG || it.metadata.dataOrigin.packageName != context.packageName) }
        return mergeOverlaps(sessions.map { rec -> val (n, m) = modalityFor(rec.exerciseType); ImportDraft(rec.metadata.id, rec.startTime, rec.endTime, n, m, null, null, emptyList(), listOf(rec.metadata.dataOrigin.packageName)) }).size
    }

    /** Average heart rate inside each work interval, from the session start and the exercise structure. */
    fun intervalHeartRates(draft: ImportDraft, ex: Exercise): List<Int?> {
        if (!ex.intervals || ex.rounds <= 0) return emptyList()
        return (0 until ex.rounds).map { k ->
            val from = draft.start.plusSeconds((ex.warmupSec + k * (ex.workSec + ex.restSec)).toLong())
            val to = from.plusSeconds(ex.workSec.toLong())
            draft.hrSamples.filter { it.first >= from && it.first < to }.map { it.second }.takeIf { it.isNotEmpty() }?.average()?.toInt()
        }
    }

    /** Debug only: writes a fake 30 min run with heart rate and distance so the import can be tested on an emulator. */
    suspend fun writeTestRun(context: Context) {
        val c = client(context)
        val end = Instant.now().minus(1, ChronoUnit.HOURS); val start = end.minus(30, ChronoUnit.MINUTES)
        val zone = ZoneOffset.systemDefault().rules.getOffset(start)
        val meta = Metadata.manualEntry()
        val hr = HeartRateRecord(startTime = start, startZoneOffset = zone, endTime = end, endZoneOffset = zone, metadata = meta,
            samples = (0 until 30).map { m -> HeartRateRecord.Sample(start.plus(m.toLong(), ChronoUnit.MINUTES), (135 + (m % 7) * 3).toLong()) })
        val dist = DistanceRecord(startTime = start, startZoneOffset = zone, endTime = end, endZoneOffset = zone, metadata = meta, distance = Length.meters(5200.0))
        val session = ExerciseSessionRecord(startTime = start, startZoneOffset = zone, endTime = end, endZoneOffset = zone, metadata = meta, exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, title = "Evening run")
        c.insertRecords(listOf(session, hr, dist))
    }
    val writePermissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
    )
}
