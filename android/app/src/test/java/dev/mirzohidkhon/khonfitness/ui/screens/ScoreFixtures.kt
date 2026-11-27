package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.*
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.exp
import kotlin.math.roundToInt

val HIGH_EXERCISE = Exercise("nor4x4", "Norwegian 4x4", Kind.CARDIO, modalityId = "bike", intensity = Intensity.HIGH, intervals = true, workSec = 240, restSec = 180, rounds = 4, fields = "time,avgHr,watts")
val LOW_EXERCISE = Exercise("z2run", "Zone 2 run", Kind.CARDIO, modalityId = "run", intensity = Intensity.LOW, fields = "distance,time,avgHr")
val MODERATE_EXERCISE = Exercise("tempo", "Tempo run", Kind.CARDIO, modalityId = "run", intensity = Intensity.MODERATE, fields = "distance,time,avgHr")

fun epochAt(day: LocalDate, zone: ZoneId, h: Int, m: Int, s: Int = 0): Long = day.atTime(h, m, s).atZone(zone).toInstant().toEpochMilli()

fun scoreSession(id: String, day: LocalDate, zone: ZoneId, ex: Exercise, lengthSec: Int, h: Int = 18, m: Int = 0): Session {
    val from = epochAt(day, zone, h, m)
    val to = from + lengthSec * 1000L
    return Session(id, day.iso(), from, to, ItemType.CARDIO, exerciseId = ex.id, exerciseName = ex.name, timeSec = lengthSec, timedFrom = from, timedTo = to)
}

fun filledFor(s: Session, zone: ZoneId, buckets: List<HrSample>): SessionHr = filledRow(s.id, hrWindow(s, zone)!!, buckets)

private const val WARM_UP_SEC = 8 * 60
private const val WORK_SEC = 4 * 60
private const val REST_SEC = 3 * 60
private const val COOL_DOWN_SEC = 4 * 60

fun intervalFixture(day: LocalDate, zone: ZoneId, id: String = "hiit", rounds: Int = 4, h: Int = 18, lift: Int = 0): PictureFixture {
    val length = WARM_UP_SEC + rounds * WORK_SEC + (rounds - 1) * REST_SEC + COOL_DOWN_SEC
    val s = scoreSession(id, day, zone, HIGH_EXERCISE, length, h)
    val start = s.timedFrom!!
    val roundStarts = (0 until rounds).map { WARM_UP_SEC + it * (WORK_SEC + REST_SEC) }
    val logs = roundStarts.mapIndexed { i, r -> IntervalLog("$id-r$i", id, i, startedAt = start + r * 1000L, endedAt = start + (r + WORK_SEC) * 1000L) }
    val range = hrWindow(s, zone)!!
    val buckets = bucketStartsSec(range).toList().map { t ->
        val inRound = roundStarts.indexOfLast { t >= it }
        val bpm = when {
            inRound < 0 -> 98.0 + 37.0 * t / WARM_UP_SEC
            t - roundStarts[inRound] < WORK_SEC -> {
                val from = if (inRound == 0) 135.0 else 128.0
                val peak = 166.0 + inRound % 3
                peak - (peak - from) * exp(-(t - roundStarts[inRound]) / 70.0)
            }
            else -> 122.0 + (166.0 - 122.0) * exp(-(t - roundStarts[inRound] - WORK_SEC) / 60.0)
        }
        HrSample(t, bpm.roundToInt() + lift)
    }
    return PictureFixture(s, emptyList(), logs, filledRow(id, range, buckets))
}

fun zoneFixture(day: LocalDate, zone: ZoneId, id: String = "z2", h: Int = 7, lift: Int = 0): PictureFixture {
    val s = scoreSession(id, day, zone, LOW_EXERCISE, 45 * 60, h)
    val range = hrWindow(s, zone)!!
    val buckets = bucketStartsSec(range).toList().map { t ->
        val bpm = when {
            t < 300 -> 95.0 + 25.0 * t / 300
            t > 40 * 60 -> 128.0 + 6.0 * (t - 40 * 60) / 300
            else -> 124.0 + 4.0 * kotlin.math.sin(t / 90.0)
        }
        HrSample(t, bpm.roundToInt() + lift)
    }
    return PictureFixture(s, emptyList(), emptyList(), filledRow(id, range, buckets))
}
