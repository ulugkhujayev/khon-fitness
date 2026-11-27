package dev.mirzohidkhon.khonfitness.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** A time span `[startMs, endMs)`. */
data class TimeSpan(val startMs: Long, val endMs: Long)

/** One sleep session record of the provider: the package that wrote it, its bounds, and its awake stages. */
data class SleepSpan(val origin: String, val startMs: Long, val endMs: Long, val awake: List<TimeSpan> = emptyList())

/** One stage of a sleep session record, with the provider's stage type. */
data class SleepStage(val type: Int, val startMs: Long, val endMs: Long)

/** Where sleep comes from: Health Connect in the app, a fake in tests. */
fun interface SleepSource {
    /**
     * Every sleep session record that starts in [range], from every page. Throws [NoSleepAccess] when the provider is
     * missing, needs an update, or the sleep permission is not granted; any other throw is a failed read.
     */
    suspend fun spans(range: HrRange): List<SleepSpan>
}

class NoSleepAccess(message: String) : Exception(message)

/** The night before [date]: the asleep time, and the first start to the last end of the spans it counts. */
data class Night(val date: LocalDate, val asleepSec: Int, val fromMs: Long, val toMs: Long)

/** The span of one record; a stage of an [awakeTypes] type becomes awake time, every other stage counts as asleep. */
fun sleepSpan(origin: String, startMs: Long, endMs: Long, stages: List<SleepStage>, awakeTypes: Set<Int>): SleepSpan =
    SleepSpan(origin, startMs, endMs, stages.filter { it.type in awakeTypes }.map { TimeSpan(it.startMs, it.endMs) })

const val MAX_SLEEP_SPAN_MS = DAY_MS
const val MIN_NIGHT_SEC = 60

private val NIGHT_START: LocalTime = LocalTime.of(18, 0)
private val NIGHT_END: LocalTime = LocalTime.of(12, 0)

/** The window of the night of [date]: from 18:00 of the day before to 12:00 of [date], local wall times in [zone]. */
fun nightWindow(date: LocalDate, zone: ZoneId): TimeSpan = TimeSpan(
    date.minusDays(1).atTime(NIGHT_START).atZone(zone).toInstant().toEpochMilli(),
    date.atTime(NIGHT_END).atZone(zone).toInstant().toEpochMilli(),
)

/** The night whose window holds [ms]; null from 12:00 to 18:00 (a nap). */
fun nightOf(ms: Long, zone: ZoneId): LocalDate? {
    val day = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    return listOf(day, day.plusDays(1)).firstOrNull { d -> nightWindow(d, zone).let { ms >= it.startMs && ms < it.endMs } }
}

/** The total length of the union of [spans], each one with `end > start`. */
fun unionMs(spans: List<TimeSpan>): Long {
    var total = 0L
    var curStart = Long.MIN_VALUE
    var curEnd = Long.MIN_VALUE
    for (s in spans.filter { it.endMs > it.startMs }.sortedBy { it.startMs }) {
        if (s.startMs > curEnd) {
            if (curEnd > curStart) total += curEnd - curStart
            curStart = s.startMs
            curEnd = s.endMs
        } else if (s.endMs > curEnd) {
            curEnd = s.endMs
        }
    }
    if (curEnd > curStart) total += curEnd - curStart
    return total
}

private fun clip(stage: TimeSpan, to: SleepSpan) = TimeSpan(maxOf(stage.startMs, to.startMs), minOf(stage.endMs, to.endMs))

private fun asleepMs(spans: List<SleepSpan>): Long =
    unionMs(spans.map { TimeSpan(it.startMs, it.endMs) }) - unionMs(spans.flatMap { s -> s.awake.map { clip(it, s) } })

/**
 * The nights of [spans] by day in [zone]. A span with `end <= start` or longer than [MAX_SLEEP_SPAN_MS] is dropped. A
 * span belongs to the night whose window ([nightWindow]) holds its midpoint. Per night and origin, the asleep time is
 * the union of the spans minus the union of their awake stages, each clipped to its span. The origin with the most
 * asleep time is kept (a tie: the first package name); origins are never added. A night under [MIN_NIGHT_SEC] asleep
 * is dropped.
 */
fun nights(spans: List<SleepSpan>, zone: ZoneId): Map<LocalDate, Night> {
    val valid = spans.filter { it.endMs > it.startMs && it.endMs - it.startMs <= MAX_SLEEP_SPAN_MS }
    val byNight = valid.groupBy { nightOf(it.startMs + (it.endMs - it.startMs) / 2, zone) }
    val out = sortedMapOf<LocalDate, Night>()
    for ((date, inNight) in byNight) {
        if (date == null) continue
        val best = inNight.groupBy { it.origin }.entries
            .map { (origin, own) -> Triple(origin, asleepMs(own), own) }
            .sortedBy { it.first }
            .maxByOrNull { it.second } ?: continue
        val asleepSec = (best.second / 1000).toInt()
        if (asleepSec < MIN_NIGHT_SEC) continue
        out[date] = Night(date, asleepSec, best.third.minOf { it.startMs }, best.third.maxOf { it.endMs })
    }
    return out
}
