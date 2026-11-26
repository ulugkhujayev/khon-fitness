package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.*
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.exp
import kotlin.math.roundToInt

/** A session of a picture test with its logs and its `session_hr` row (null: not read). */
data class PictureFixture(val session: Session, val logs: List<SetLog>, val rounds: List<IntervalLog>, val row: SessionHr?)

private fun epoch(day: LocalDate, zone: ZoneId, h: Int, m: Int, s: Int = 0) = day.atTime(h, m, s).atZone(zone).toInstant().toEpochMilli()

fun filledRow(sessionId: String, range: HrRange, buckets: List<HrSample>) = SessionHr(
    sessionId, range.fromMs, range.toMs, HrState.FILLED, formatSamples(buckets), rawCount = buckets.size, sampleCount = buckets.size,
    origin = "mi", origins = "mi", avgHr = meanHalfUp(buckets.map { it.bpm }), maxHr = buckets.maxOfOrNull { it.bpm }, fetchedAt = range.toMs + 60_000,
)

val GYM_LIFTS = listOf(
    "Barbell Bench Press" to 60.0, "Incline Dumbbell Press" to 24.0, "Shoulder Press" to 35.0,
    "Cable Seated Row" to 50.0, "Lat Pulldown" to 45.0, "Pull-up" to 0.0,
)

/** Seconds from 18:02 of each set check: 18 checks from 18:06 to 19:09, in set order. */
private val GYM_CHECKS = listOf(240, 470, 690, 960, 1190, 1420, 1700, 1905, 2120, 2390, 2600, 2820, 3080, 3260, 3440, 3620, 3700, 4020)

/**
 * A gym session of 18 sets (6 exercises, 3 sets each) from 18:02, the last check at 19:09, so its range ends at 19:14.
 * With [heart], buckets every 5 s from 92 to 164 bpm with a 3-minute gap and one lone bucket in it.
 */
fun gymFixture(day: LocalDate, zone: ZoneId, heart: Boolean = true, id: String = "gym", names: List<String> = GYM_LIFTS.map { it.first }): PictureFixture {
    val start = epoch(day, zone, 18, 2)
    val lastCheck = start + GYM_CHECKS.last() * 1000L
    val s = Session(id, day.iso(), start, lastCheck + 11 * 60_000, ItemType.PROGRAM, programId = "push", programName = "Push", timedFrom = start, timedTo = lastCheck + 5 * 60_000)
    var n = 0
    val logs = GYM_CHECKS.mapIndexed { i, sec ->
        val (lift, kg) = GYM_LIFTS[i / 3]
        SetLog("$id-l$i", id, i / 3, false, 0, i % 3, n++, "ex${i / 3}", names[i / 3], 8, 12, 2.5, weightKg = kg + (i % 3) * 2.5, reps = if (kg == 0.0) 10 - i % 3 else 10 - (i % 3), status = SetStatus.DONE, checkedAt = start + sec * 1000L)
    }
    val range = HrRange(s.timedFrom!!, s.timedTo!!)
    val row = if (heart) filledRow(id, range, gymBuckets(range)) else null
    return PictureFixture(s, logs, emptyList(), row)
}

private fun gymBuckets(range: HrRange): List<HrSample> {
    val length = (range.lengthMs / 1000).toInt()
    val peaks = GYM_CHECKS.mapIndexed { i, _ -> 132 + (i * 7) % 30 }.toMutableList().also { it[13] = 164 }
    return (0 until length step 5).mapNotNull { t ->
        if (t in 1800 until 1980 && t != 1890) return@mapNotNull null
        val next = GYM_CHECKS.indexOfFirst { it >= t }
        val rest = 108.0
        val bpm = when {
            next == 0 && GYM_CHECKS[0] - t > 45 -> 92.0 + (rest - 92.0) * t / (GYM_CHECKS[0] - 45)
            next >= 0 && GYM_CHECKS[next] - t <= 45 -> {
                val from = if (next == 0) rest else rest + (peaks[next - 1] - rest) * exp(-(GYM_CHECKS[next] - 45.0 - GYM_CHECKS[next - 1]) / 50.0)
                from + (peaks[next] - from) * (1 - (GYM_CHECKS[next] - t) / 45.0)
            }
            else -> {
                val prev = if (next < 0) GYM_CHECKS.lastIndex else next - 1
                rest + (peaks[prev] - rest) * exp(-(t - GYM_CHECKS[prev]) / 50.0)
            }
        }
        HrSample(t, bpm.roundToInt().coerceIn(92, 164))
    }
}

/** A 4×4 cardio session from 18:00: 8 min warm-up, 4 rounds of 4 min with 3 min between, 4 min cool-down. */
fun cardioFixture(day: LocalDate, zone: ZoneId, id: String = "cardio"): PictureFixture {
    val start = epoch(day, zone, 18, 0)
    val roundStarts = (0 until 4).map { 8 * 60 + it * 7 * 60 }
    val end = start + 37 * 60_000L
    val s = Session(id, day.iso(), start, end + 2 * 60_000, ItemType.CARDIO, exerciseId = "z2", exerciseName = "Norwegian 4×4", timeSec = 37 * 60, timedFrom = start, timedTo = end)
    val rounds = roundStarts.mapIndexed { i, r -> IntervalLog("$id-r$i", id, i, startedAt = start + r * 1000L, endedAt = start + (r + 240) * 1000L) }
    val range = hrWindow(s, zone)!!
    val length = (range.lengthMs / 1000).toInt()
    val buckets = (0 until length step 5).map { t ->
        val inRound = roundStarts.indexOfLast { t >= it }
        val bpm = when {
            inRound < 0 -> 98.0 + 37.0 * t / (8 * 60)
            t - roundStarts[inRound] < 240 -> {
                val from = if (inRound == 0) 135.0 else 128.0
                169.0 - (169.0 - from) * exp(-(t - roundStarts[inRound]) / 70.0)
            }
            else -> 122.0 + (168.0 - 122.0) * exp(-(t - roundStarts[inRound] - 240) / 60.0)
        }
        HrSample(t, bpm.roundToInt())
    }
    return PictureFixture(s, emptyList(), rounds, filledRow(id, range, buckets))
}
