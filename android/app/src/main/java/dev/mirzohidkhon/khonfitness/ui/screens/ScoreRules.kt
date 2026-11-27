package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.HrFacts
import dev.mirzohidkhon.khonfitness.data.HrRange
import dev.mirzohidkhon.khonfitness.data.HrSample
import dev.mirzohidkhon.khonfitness.data.Intensity
import dev.mirzohidkhon.khonfitness.data.IntervalLog
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.MAX_HR_RANGE
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SessionHr
import dev.mirzohidkhon.khonfitness.data.hrFacts
import dev.mirzohidkhon.khonfitness.data.minusMs
import dev.mirzohidkhon.khonfitness.data.toDate
import dev.mirzohidkhon.khonfitness.ui.components.ChartPoint
import java.time.ZoneId

const val SCORE_BUCKET_SEC = 5
const val MEASURED_MAX_DAYS = 365L
const val HARD_PERCENT = 85
const val ZONE_LOW_PERCENT = 60
const val ZONE_HIGH_PERCENT = 70
private const val DAY_MS = 86_400_000L
private const val MIN_ROUND_BUCKETS = 2
private const val MIN_REST_BUCKETS = 2
private const val MIN_CHART_POINTS = 2

fun measuredMaxHr(sessions: List<Session>, rows: List<SessionHr>, nowMs: Long, zone: ZoneId): Int? {
    val byId = rows.associateBy { it.sessionId }
    val from = minusMs(nowMs, MEASURED_MAX_DAYS * DAY_MS)
    return sessions.filter { s -> s.finishedAt?.let { it in from..nowMs } == true }
        .mapNotNull { s -> (hrFacts(s, byId[s.id], zone) as? HrFacts.Filled)?.row?.maxHr }
        .maxOrNull()
}

fun maxInUse(typed: Int?, measured: Int?): Int? = typed?.takeIf { it in MAX_HR_RANGE } ?: measured

sealed interface MaxEntry {
    data object Clear : MaxEntry
    data class Store(val bpm: Int) : MaxEntry
    data object Refused : MaxEntry
}

private val WHOLE_BPM = Regex("[0-9]+")

fun maxEntry(text: String): MaxEntry {
    if (text.isEmpty()) return MaxEntry.Clear
    val bpm = text.takeIf(WHOLE_BPM::matches)?.toIntOrNull()?.takeIf { it in MAX_HR_RANGE } ?: return MaxEntry.Refused
    return MaxEntry.Store(bpm)
}

private fun ceilPercent(max: Int, percent: Int): Int = Math.floorDiv(percent * max + 99, 100)

fun hardLine(max: Int): Int = ceilPercent(max, HARD_PERCENT)

data class HrZone(val low: Int, val high: Int)

fun hrZone(max: Int): HrZone = HrZone(ceilPercent(max, ZONE_LOW_PERCENT), Math.floorDiv(ZONE_HIGH_PERCENT * max, 100))

fun hardSeconds(buckets: List<HrSample>, max: Int): Long = buckets.count { it.bpm >= hardLine(max) } * SCORE_BUCKET_SEC.toLong()

data class ZoneShares(val below: Int, val inside: Int, val above: Int)

fun percentShares(counts: List<Int>): List<Int> {
    val n = counts.sum()
    if (n == 0) return counts.map { 0 }
    val base = counts.map { 100 * it / n }.toMutableList()
    val order = counts.indices.sortedWith(compareByDescending<Int> { 100 * counts[it] % n }.thenBy { it })
    repeat(100 - base.sum()) { k -> base[order[k]]++ }
    return base
}

fun zoneShares(buckets: List<HrSample>, max: Int): ZoneShares? {
    if (buckets.isEmpty()) return null
    val z = hrZone(max)
    val (below, inside, above) = percentShares(listOf(buckets.count { it.bpm < z.low }, buckets.count { it.bpm in z.low..z.high }, buckets.count { it.bpm > z.high }))
    return ZoneShares(below, inside, above)
}

data class RoundScore(val number: Int, val peak: Int?, val drop: Int?)

private class RoundClock(val start: Long, val end: Long)

private fun IntervalLog.clock(): RoundClock? {
    val start = startedAt ?: return null
    val end = endedAt ?: return null
    return if (start < end) RoundClock(start, end) else null
}

private fun IntervalLog.hasBrokenTimes(): Boolean = (startedAt != null || endedAt != null) && clock() == null

private fun roundTimesInOrder(sorted: List<IntervalLog>): Boolean =
    sorted.none { it.hasBrokenTimes() } && sorted.mapNotNull { it.clock() }.zipWithNext().all { (a, b) -> a.end <= b.start }

fun roundScores(range: HrRange, buckets: List<HrSample>, rounds: List<IntervalLog>): List<RoundScore> {
    val sorted = rounds.sortedBy { it.index }
    if (!roundTimesInOrder(sorted)) return sorted.indices.map { RoundScore(it + 1, null, null) }
    fun within(from: Long, to: Long) = buckets.filter { bucketAtMs(range, it) in from until to }
    return sorted.indices.map { i ->
        val none = RoundScore(i + 1, null, null)
        val c = sorted[i].clock()
        if (c == null || !range.holds(c.start)) return@map none
        val work = within(c.start, c.end)
        if (work.size < MIN_ROUND_BUCKETS) return@map none
        val restEnd = if (i == sorted.lastIndex) range.toMs else sorted[i + 1].startedAt
        val rest = restEnd?.let { within(c.end, minOf(it, range.toMs)) }.orEmpty()
        val drop = if (rest.size >= MIN_REST_BUCKETS) work.last().bpm - rest.minOf { it.bpm } else null
        RoundScore(i + 1, work.maxOf { it.bpm }, drop)
    }
}

data class ScoreRow(val label: String, val value: String, val spoken: String)

private fun signed(n: Int): String = if (n < 0) "−${-n}" else "$n"

private fun spokenSigned(n: Int): String = if (n < 0) "minus ${-n}" else "$n"

fun roundRow(r: RoundScore): ScoreRow? {
    val peak = r.peak ?: return null
    return ScoreRow(
        "Round ${r.number}",
        "peak $peak" + (r.drop?.let { " · drop ${signed(it)}" } ?: ""),
        "Round ${r.number}, peak $peak" + (r.drop?.let { ", drop ${spokenSigned(it)}" } ?: "") + " beats per minute",
    )
}

private fun shareRow(label: String, spokenLabel: String, percent: Int) = ScoreRow(label, "$percent %", "$spokenLabel, $percent percent")

fun scoreRows(session: Session, exercise: Exercise?, facts: HrFacts, rounds: List<IntervalLog>, max: Int?): List<ScoreRow> {
    if (max == null || session.itemType != ItemType.CARDIO) return emptyList()
    val row = (facts as? HrFacts.Filled)?.row ?: return emptyList()
    val buckets = pictureBuckets(facts)
    if (buckets.isEmpty()) return emptyList()
    val head = when (exercise?.intensity) {
        Intensity.HIGH -> {
            val line = hardLine(max)
            val sec = hardSeconds(buckets, max)
            listOf(ScoreRow("At or above $line bpm", minSec(sec), "At or above $line beats per minute, ${spokenMinSec(sec)}"))
        }
        Intensity.LOW -> {
            val z = hrZone(max)
            val s = zoneShares(buckets, max) ?: return emptyList()
            listOf(
                shareRow("Below ${z.low} bpm", "Below ${z.low} beats per minute", s.below),
                shareRow("${z.low} to ${z.high} bpm", "${z.low} to ${z.high} beats per minute", s.inside),
                shareRow("Above ${z.high} bpm", "Above ${z.high} beats per minute", s.above),
            )
        }
        else -> return emptyList()
    }
    return head + roundScores(HrRange(row.fromMs, row.toMs), buckets, rounds).mapNotNull(::roundRow)
}

data class ScoreCharts(val hardMinutes: List<ChartPoint>, val zonePercent: List<ChartPoint>)

fun scoreCharts(sessions: List<Session>, exercises: Map<String, Exercise>, facts: Map<String, HrFacts>, max: Int?): ScoreCharts {
    if (max == null) return ScoreCharts(emptyList(), emptyList())
    val hard = mutableListOf<ChartPoint>()
    val zone = mutableListOf<ChartPoint>()
    sessions.filter { it.itemType == ItemType.CARDIO }.sortedWith(compareBy({ it.date }, { it.startedAt })).forEach { s ->
        val f = facts[s.id] as? HrFacts.Filled ?: return@forEach
        val buckets = pictureBuckets(f)
        if (buckets.isEmpty()) return@forEach
        val date = s.date.toDate()
        when (exercises[s.exerciseId]?.intensity) {
            Intensity.HIGH -> hard += ChartPoint(date, hardSeconds(buckets, max) / 60.0)
            Intensity.LOW -> zoneShares(buckets, max)?.let { zone += ChartPoint(date, it.inside.toDouble()) }
        }
    }
    return ScoreCharts(hard.takeIf { it.size >= MIN_CHART_POINTS }.orEmpty(), zone.takeIf { it.size >= MIN_CHART_POINTS }.orEmpty())
}

data class MaxShown(val text: String, val typed: Boolean, val spoken: String)

fun maxShown(typed: Int?, measured: Int?): MaxShown = when {
    typed != null -> MaxShown("$typed bpm", true, "Maximum heart rate, $typed beats per minute")
    measured != null -> MaxShown("$measured bpm", false, "Maximum heart rate, measured $measured beats per minute")
    else -> MaxShown("Not set", false, "Maximum heart rate, not set")
}
