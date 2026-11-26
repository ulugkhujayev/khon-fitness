package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.*
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.exp
import kotlin.math.roundToInt

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

const val BUCKET_STEP_SEC = 5

fun bucketStartsSec(range: HrRange): Sequence<Int> = generateSequence(0) { it + BUCKET_STEP_SEC }.takeWhile { it * 1000L < range.lengthMs }

val GYM_GAP_SEC = 1800 until 1980
const val GYM_LONE_BUCKET_SEC = 1890
private const val GYM_RANGE_TAIL_MS = 5 * 60_000L
private const val GYM_FINISH_AFTER_LAST_CHECK_MS = 11 * 60_000L

private val GYM_CHECK_SEC_FROM_START = listOf(240, 470, 690, 960, 1190, 1420, 1700, 1905, 2120, 2390, 2600, 2820, 3080, 3260, 3440, 3620, 3700, 4020)

fun gymFixture(day: LocalDate, zone: ZoneId, heart: Boolean = true, id: String = "gym", names: List<String> = GYM_LIFTS.map { it.first }): PictureFixture {
    val start = epoch(day, zone, 18, 2)
    val lastCheck = start + GYM_CHECK_SEC_FROM_START.last() * 1000L
    val s = Session(id, day.iso(), start, lastCheck + GYM_FINISH_AFTER_LAST_CHECK_MS, ItemType.PROGRAM, programId = "push", programName = "Push", timedFrom = start, timedTo = lastCheck + GYM_RANGE_TAIL_MS)
    var n = 0
    val logs = GYM_CHECK_SEC_FROM_START.mapIndexed { i, sec ->
        val (lift, kg) = GYM_LIFTS[i / 3]
        SetLog("$id-l$i", id, i / 3, false, 0, i % 3, n++, "ex${i / 3}", names[i / 3], 8, 12, 2.5, weightKg = kg + (i % 3) * 2.5, reps = if (kg == 0.0) 10 - i % 3 else 10 - (i % 3), status = SetStatus.DONE, checkedAt = start + sec * 1000L)
    }
    val range = HrRange(s.timedFrom!!, s.timedTo!!)
    val row = if (heart) filledRow(id, range, gymBuckets(range)) else null
    return PictureFixture(s, logs, emptyList(), row)
}

private fun gymBuckets(range: HrRange): List<HrSample> {
    val peaks = GYM_CHECK_SEC_FROM_START.mapIndexed { i, _ -> 132 + (i * 7) % 30 }.toMutableList().also { it[13] = 164 }
    return bucketStartsSec(range).toList().mapNotNull { t ->
        if (t in GYM_GAP_SEC && t != GYM_LONE_BUCKET_SEC) return@mapNotNull null
        val next = GYM_CHECK_SEC_FROM_START.indexOfFirst { it >= t }
        val rest = 108.0
        val bpm = when {
            next == 0 && GYM_CHECK_SEC_FROM_START[0] - t > 45 -> 92.0 + (rest - 92.0) * t / (GYM_CHECK_SEC_FROM_START[0] - 45)
            next >= 0 && GYM_CHECK_SEC_FROM_START[next] - t <= 45 -> {
                val from = if (next == 0) rest else rest + (peaks[next - 1] - rest) * exp(-(GYM_CHECK_SEC_FROM_START[next] - 45.0 - GYM_CHECK_SEC_FROM_START[next - 1]) / 50.0)
                from + (peaks[next] - from) * (1 - (GYM_CHECK_SEC_FROM_START[next] - t) / 45.0)
            }
            else -> {
                val prev = if (next < 0) GYM_CHECK_SEC_FROM_START.lastIndex else next - 1
                rest + (peaks[prev] - rest) * exp(-(t - GYM_CHECK_SEC_FROM_START[prev]) / 50.0)
            }
        }
        HrSample(t, bpm.roundToInt().coerceIn(92, 164))
    }
}

private const val CARDIO_WARM_UP_SEC = 8 * 60
private const val CARDIO_ROUND_SEC = 4 * 60
private const val CARDIO_REST_SEC = 3 * 60
private const val CARDIO_ROUNDS = 4
private const val CARDIO_COOL_DOWN_SEC = 4 * 60
private const val CARDIO_TIME_SEC = CARDIO_WARM_UP_SEC + CARDIO_ROUNDS * CARDIO_ROUND_SEC + (CARDIO_ROUNDS - 1) * CARDIO_REST_SEC + CARDIO_COOL_DOWN_SEC

fun cardioFixture(day: LocalDate, zone: ZoneId, id: String = "cardio"): PictureFixture {
    val start = epoch(day, zone, 18, 0)
    val roundStarts = (0 until CARDIO_ROUNDS).map { CARDIO_WARM_UP_SEC + it * (CARDIO_ROUND_SEC + CARDIO_REST_SEC) }
    val end = start + CARDIO_TIME_SEC * 1000L
    val s = Session(id, day.iso(), start, end + 2 * 60_000, ItemType.CARDIO, exerciseId = "z2", exerciseName = "Norwegian 4×4", timeSec = CARDIO_TIME_SEC, timedFrom = start, timedTo = end)
    val rounds = roundStarts.mapIndexed { i, r -> IntervalLog("$id-r$i", id, i, startedAt = start + r * 1000L, endedAt = start + (r + CARDIO_ROUND_SEC) * 1000L) }
    val range = hrWindow(s, zone)!!
    val buckets = bucketStartsSec(range).toList().map { t ->
        val inRound = roundStarts.indexOfLast { t >= it }
        val bpm = when {
            inRound < 0 -> 98.0 + 37.0 * t / CARDIO_WARM_UP_SEC
            t - roundStarts[inRound] < CARDIO_ROUND_SEC -> {
                val from = if (inRound == 0) 135.0 else 128.0
                169.0 - (169.0 - from) * exp(-(t - roundStarts[inRound]) / 70.0)
            }
            else -> 122.0 + (168.0 - 122.0) * exp(-(t - roundStarts[inRound] - CARDIO_ROUND_SEC) / 60.0)
        }
        HrSample(t, bpm.roundToInt())
    }
    return PictureFixture(s, emptyList(), rounds, filledRow(id, range, buckets))
}
