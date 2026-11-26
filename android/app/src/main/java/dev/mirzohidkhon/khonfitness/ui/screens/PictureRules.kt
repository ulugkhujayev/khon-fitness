package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.HrFacts
import dev.mirzohidkhon.khonfitness.data.HrRange
import dev.mirzohidkhon.khonfitness.data.HrSample
import dev.mirzohidkhon.khonfitness.data.IntervalLog
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.hrWindow
import dev.mirzohidkhon.khonfitness.data.parseSamples
import dev.mirzohidkhon.khonfitness.ui.components.fmt
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val CURVE_MAX_JOINED_GAP_SEC = 60
private const val AXIS_MIN_LINES = 3
private const val AXIS_MAX_LINES = 5
private const val AXIS_BASE_MULTIPLE = 10
private val AXIS_STEPS = listOf(10, 20, 30, 40, 50, 100)

data class BpmAxis(val low: Int, val high: Int, val step: Int) {
    val lines: List<Int> get() = (low..high step step).toList()
}

data class SetMarkRow(val name: String, val set: String, val atMs: Long, val clock: String, val sinceSec: Long?, val maxBpm: Int?, val spoken: String)

data class RoundBand(val fromMs: Long, val toMs: Long)

data class WorkoutPicture(
    val range: HrRange,
    val curve: List<List<HrSample>>,
    val axis: BpmAxis?,
    val markOffsetsMs: List<Long>,
    val bands: List<RoundBand>,
    val rows: List<SetMarkRow>,
    val startClock: String,
    val endClock: String,
    val spoken: String,
) {
    val hasChart: Boolean get() = curve.isNotEmpty()
}

private val clockFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun clockAt(epochMs: Long, zone: ZoneId): String = Instant.ofEpochMilli(epochMs).atZone(zone).format(clockFmt)

fun bucketAtMs(range: HrRange, b: HrSample): Long = range.fromMs + b.offsetSec * 1000L

fun HrRange.holds(ms: Long): Boolean = ms >= fromMs && ms < toMs

fun pictureBuckets(facts: HrFacts): List<HrSample> {
    val row = (facts as? HrFacts.Filled)?.row ?: return emptyList()
    val range = HrRange(row.fromMs, row.toMs)
    return parseSamples(row.samples).orEmpty().filter { range.holds(bucketAtMs(range, it)) }.sortedBy { it.offsetSec }
}

fun curveSegments(buckets: List<HrSample>): List<List<HrSample>> {
    val segments = mutableListOf<MutableList<HrSample>>()
    for (b in buckets) {
        val last = segments.lastOrNull()?.last()
        if (last == null || b.offsetSec - last.offsetSec > CURVE_MAX_JOINED_GAP_SEC) segments += mutableListOf(b) else segments.last() += b
    }
    return segments
}

fun setMarks(range: HrRange, logs: List<SetLog>): List<SetLog> =
    logs.filter { it.status == SetStatus.DONE && it.checkedAt?.let(range::holds) == true }.sortedBy { it.checkedAt }

fun roundBands(range: HrRange, rounds: List<IntervalLog>): List<RoundBand> = rounds.sortedBy { it.index }.mapNotNull { r ->
    val start = r.startedAt ?: return@mapNotNull null
    val end = r.endedAt ?: return@mapNotNull null
    if (start >= end || !range.holds(start)) return@mapNotNull null
    RoundBand(start - range.fromMs, minOf(end, range.toMs) - range.fromMs)
}

fun bpmAxis(buckets: List<HrSample>): BpmAxis? {
    if (buckets.isEmpty()) return null
    val min = buckets.minOf { it.bpm }
    val max = buckets.maxOf { it.bpm }
    val base = Math.floorDiv(min, AXIS_BASE_MULTIPLE) * AXIS_BASE_MULTIPLE
    val step = AXIS_STEPS.firstOrNull { s -> ceilDiv(max - base, s) + 1 <= AXIS_MAX_LINES } ?: AXIS_STEPS.last()
    return growAboveThenBelowToMinLines(BpmAxis(base, base + ceilDiv(max - base, step) * step, step))
}

private fun growAboveThenBelowToMinLines(axis: BpmAxis): BpmAxis {
    var a = axis
    var above = true
    while (a.lines.size < AXIS_MIN_LINES) {
        a = if (above || a.low - a.step < 0) a.copy(high = a.high + a.step) else a.copy(low = a.low - a.step)
        above = !above
    }
    return a
}

private fun ceilDiv(a: Int, b: Int): Int = Math.floorDiv(a + b - 1, b)

fun minSec(sec: Long): String = "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"

private fun plural(n: Long, word: String) = "$n ${if (n == 1L) word else word + "s"}"

fun spokenMinSec(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return listOfNotNull(plural(m, "minute").takeIf { m > 0 }, plural(s, "second").takeIf { s > 0 || m == 0L }).joinToString(" ")
}

private fun spokenSet(l: SetLog): String {
    val reps = plural((l.reps ?: 0).toLong(), "rep")
    return if ((l.weightKg ?: 0.0) > 0) "${fmt(l.weightKg)} kilograms, $reps" else reps
}

private const val MIN_MARKS_FOR_ROWS = 2
private const val MIN_BUCKETS_FOR_ROW_BPM = 2

fun setMarkRows(range: HrRange, marked: List<SetLog>, buckets: List<HrSample>, zone: ZoneId): List<SetMarkRow> {
    if (marked.size < MIN_MARKS_FOR_ROWS) return emptyList()
    return marked.mapIndexed { i, l ->
        val at = l.checkedAt!!
        val prev = if (i == 0) null else marked[i - 1].checkedAt!!
        val spanFrom = prev ?: range.fromMs
        val inside = buckets.filter { bucketAtMs(range, it) in spanFrom until at }
        val maxBpm = if (inside.size >= MIN_BUCKETS_FOR_ROW_BPM) inside.maxOf { it.bpm } else null
        val since = prev?.let { (at - it) / 1000 }
        val clock = clockAt(at, zone)
        val spoken = listOfNotNull(
            l.exerciseName, spokenSet(l), "checked at $clock",
            since?.let { "${spokenMinSec(it)} since the set before" },
            maxBpm?.let { "highest $it beats per minute" },
        ).joinToString(", ")
        SetMarkRow(l.exerciseName, setText(l), at, clock, since, maxBpm, spoken)
    }
}

fun workoutPicture(session: Session, facts: HrFacts, logs: List<SetLog>, rounds: List<IntervalLog>, zone: ZoneId): WorkoutPicture? {
    val range = hrWindow(session, zone) ?: return null
    if (facts is HrFacts.NoRange) return null
    val buckets = pictureBuckets(facts)
    val gym = session.itemType == ItemType.PROGRAM
    val marked = if (gym) setMarks(range, logs) else emptyList()
    val bands = if (gym) emptyList() else roundBands(range, rounds)
    val startClock = clockAt(range.fromMs, zone)
    val endClock = clockAt(range.toMs, zone)
    val spoken = if (buckets.isEmpty()) "" else listOfNotNull(
        "Heart rate from $startClock to $endClock",
        "lowest ${buckets.minOf { it.bpm }} beats per minute",
        "highest ${buckets.maxOf { it.bpm }} beats per minute",
        if (gym) plural(marked.size.toLong(), "set") + " marked" else bands.size.takeIf { it > 0 }?.let { plural(it.toLong(), "round") + " marked" },
    ).joinToString(", ")
    return WorkoutPicture(
        range = range,
        curve = curveSegments(buckets),
        axis = bpmAxis(buckets),
        markOffsetsMs = marked.map { it.checkedAt!! - range.fromMs },
        bands = bands,
        rows = setMarkRows(range, marked, buckets, zone),
        startClock = startClock,
        endClock = endClock,
        spoken = spoken,
    )
}
