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

/** Two neighbor buckets further apart than this are not joined. */
const val CURVE_GAP_SEC = 60

/** The bpm grid of the picture: lines from [low] to [high] every [step]. */
data class BpmAxis(val low: Int, val high: Int, val step: Int) {
    val lines: List<Int> get() = (low..high step step).toList()
}

/** A set in "Sets in order": [sinceSec] from the mark before it (null for the first), [maxBpm] in that span. */
data class SetMarkRow(val name: String, val set: String, val atMs: Long, val clock: String, val sinceSec: Long?, val maxBpm: Int?, val spoken: String)

/** A work round's band, in ms from the range start. */
data class RoundBand(val fromMs: Long, val toMs: Long)

/**
 * The heart rate picture of a finished session with a range. [curve] holds segments of buckets (seconds from the range
 * start); empty without a filled fact. [marks] are the set checks and [bands] the rounds, in ms from the range start.
 */
data class WorkoutPicture(
    val range: HrRange,
    val curve: List<List<HrSample>>,
    val axis: BpmAxis?,
    val marks: List<Long>,
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

/** The buckets of a filled fact inside its range, in time order; empty for any other fact. */
fun pictureBuckets(facts: HrFacts): List<HrSample> {
    val row = (facts as? HrFacts.Filled)?.row ?: return emptyList()
    val lengthSec = (row.toMs - row.fromMs) / 1000
    return parseSamples(row.samples).orEmpty().filter { it.offsetSec in 0 until lengthSec }.sortedBy { it.offsetSec }
}

/** The buckets cut where two neighbors lie more than [CURVE_GAP_SEC] apart; a segment of one bucket is a dot. */
fun curveSegments(buckets: List<HrSample>): List<List<HrSample>> {
    val segments = mutableListOf<MutableList<HrSample>>()
    for (b in buckets) {
        val last = segments.lastOrNull()?.last()
        if (last == null || b.offsetSec - last.offsetSec > CURVE_GAP_SEC) segments += mutableListOf(b) else segments.last() += b
    }
    return segments
}

/** Done sets checked inside [range], in time order. */
fun setMarks(range: HrRange, logs: List<SetLog>): List<SetLog> =
    logs.filter { it.status == SetStatus.DONE && it.checkedAt?.let { at -> at >= range.fromMs && at < range.toMs } == true }.sortedBy { it.checkedAt }

/** Rounds with `startedAt < endedAt` that start inside [range], cut at its end. */
fun roundBands(range: HrRange, rounds: List<IntervalLog>): List<RoundBand> = rounds.sortedBy { it.index }.mapNotNull { r ->
    val start = r.startedAt ?: return@mapNotNull null
    val end = r.endedAt ?: return@mapNotNull null
    if (start >= end || start < range.fromMs || start >= range.toMs) return@mapNotNull null
    RoundBand(start - range.fromMs, minOf(end, range.toMs) - range.fromMs)
}

/**
 * The bpm grid over [buckets]: from a multiple of 10 at or under the lowest, in steps of 10 or 20 (wider only when 20
 * gives more than 5 lines), so that 3 to 5 lines cover every bucket. Too few lines grow the grid above, then below.
 */
fun bpmAxis(buckets: List<HrSample>): BpmAxis? {
    if (buckets.isEmpty()) return null
    val min = buckets.minOf { it.bpm }
    val max = buckets.maxOf { it.bpm }
    val base = Math.floorDiv(min, 10) * 10
    val step = listOf(10, 20, 30, 40, 50, 100).firstOrNull { s -> ceilDiv(max - base, s) + 1 <= 5 } ?: 100
    var low = base
    var high = base + ceilDiv(max - base, step) * step
    var above = true
    while ((high - low) / step + 1 < 3) {
        if (above || low - step < 0) high += step else low -= step
        above = !above
    }
    return BpmAxis(low, high, step)
}

private fun ceilDiv(a: Int, b: Int): Int = Math.floorDiv(a + b - 1, b)

/** "2:05" */
fun minSec(sec: Long): String = "${sec / 60}:${(sec % 60).toString().padStart(2, '0')}"

private fun plural(n: Long, word: String) = "$n ${if (n == 1L) word else word + "s"}"

/** "2 minutes 5 seconds", "45 seconds", "3 minutes". */
fun spokenMinSec(sec: Long): String {
    val m = sec / 60
    val s = sec % 60
    return listOfNotNull(plural(m, "minute").takeIf { m > 0 }, plural(s, "second").takeIf { s > 0 || m == 0L }).joinToString(" ")
}

private fun spokenSet(l: SetLog): String {
    val reps = plural((l.reps ?: 0).toLong(), "rep")
    return if ((l.weightKg ?: 0.0) > 0) "${fmt(l.weightKg)} kilograms, $reps" else reps
}

/**
 * The rows of "Sets in order": one per marked set, in time order, when 2 or more are marked. The bpm of a row is the
 * highest bucket from the mark before it (the range start for the first) up to its check, when 2 or more are inside.
 */
fun setMarkRows(range: HrRange, marked: List<SetLog>, buckets: List<HrSample>, zone: ZoneId): List<SetMarkRow> {
    if (marked.size < 2) return emptyList()
    return marked.mapIndexed { i, l ->
        val at = l.checkedAt!!
        val prev = if (i == 0) null else marked[i - 1].checkedAt!!
        val spanFrom = prev ?: range.fromMs
        val inside = buckets.filter { range.fromMs + it.offsetSec * 1000L in spanFrom until at }
        val maxBpm = if (inside.size >= 2) inside.maxOf { it.bpm } else null
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

/**
 * The picture of [session] from its [facts] and logs, or null when the session has no range ([hrWindow]: unfinished,
 * moved, or without stored times). Gym sessions get marks and rows from [logs]; cardio sessions bands from [rounds].
 */
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
        marks = marked.map { it.checkedAt!! - range.fromMs },
        bands = bands,
        rows = setMarkRows(range, marked, buckets, zone),
        startClock = startClock,
        endClock = endClock,
        spoken = spoken,
    )
}
