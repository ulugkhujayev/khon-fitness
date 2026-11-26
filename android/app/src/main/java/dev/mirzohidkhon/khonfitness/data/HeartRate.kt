package dev.mirzohidkhon.khonfitness.data

import java.time.Instant
import java.time.ZoneId

/** A half-open time range `[fromMs, toMs)` in epoch ms. */
data class HrRange(val fromMs: Long, val toMs: Long) {
    val lengthMs: Long get() = toMs - fromMs
}

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
val GYM_RANGE_MS = 10 * MINUTE_MS..4 * HOUR_MS
val CARDIO_RANGE_MS = 2 * MINUTE_MS..6 * HOUR_MS
const val CARDIO_TAIL_MS = 60_000L
const val BPM_MIN = 30
const val BPM_MAX = 230

fun localDate(epochMs: Long, zone: ZoneId): String = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().iso()

/**
 * The heart rate range of a finished session from its stored `timedFrom`, `timedTo`, or null. The session's date must
 * be the local date of `timedFrom` or of `startedAt`. Gym: `[timedFrom, timedTo)` of 10 minutes to 4 hours. App cardio:
 * `[timedFrom, min(timedTo + 60 s, finishedAt))`; imported cardio: `[timedFrom, timedTo)`; both of 2 minutes to 6 hours.
 */
fun hrWindow(session: Session, zone: ZoneId): HrRange? {
    val finishedAt = session.finishedAt ?: return null
    val from = session.timedFrom ?: return null
    val to = session.timedTo ?: return null
    if (from >= to) return null
    if (session.date != localDate(from, zone) && session.date != localDate(session.startedAt, zone)) return null
    return when (session.itemType) {
        ItemType.PROGRAM -> HrRange(from, to).takeIf { it.lengthMs in GYM_RANGE_MS }
        ItemType.CARDIO -> {
            val end = if (session.sourceId == null) minOf(to + CARDIO_TAIL_MS, finishedAt) else to
            HrRange(from, end).takeIf { it.lengthMs in CARDIO_RANGE_MS }
        }
        else -> null
    }
}

/** What a screen may show of a session's heart rate ([hrFacts]). */
sealed interface HrFacts {
    data object NoRange : HrFacts
    data object Removed : HrFacts
    data object NotRead : HrFacts
    data class Filled(val row: SessionHr) : HrFacts
    data class Waiting(val row: SessionHr) : HrFacts
    data class None(val row: SessionHr) : HrFacts
}

/**
 * The one rule for showing heart rate: no range hides every row; an `off` row is removed; a row is shown only when it
 * was read for the session's current range ([hrWindow]); else the session is not read, and nothing of a stale row shows.
 */
fun hrFacts(session: Session, row: SessionHr?, zone: ZoneId): HrFacts {
    val range = hrWindow(session, zone) ?: return HrFacts.NoRange
    if (row == null) return HrFacts.NotRead
    if (row.state == HrState.OFF) return HrFacts.Removed
    if (row.sessionId != session.id || row.fromMs != range.fromMs || row.toMs != range.toMs) return HrFacts.NotRead
    return when (row.state) {
        HrState.FILLED -> HrFacts.Filled(row)
        HrState.WAITING -> HrFacts.Waiting(row)
        HrState.NONE -> HrFacts.None(row)
        else -> HrFacts.NotRead
    }
}

/** One 5-second bucket of a [SessionHr]: seconds from the row's `fromMs`, and the bucket's bpm. */
data class HrSample(val offsetSec: Int, val bpm: Int)

/** The `offsetSec:bpm` list of a row; empty text is no sample; null when any pair does not parse. */
fun parseSamples(text: String): List<HrSample>? {
    if (text.isEmpty()) return emptyList()
    return text.split(",").map { pair ->
        val parts = pair.split(":")
        if (parts.size != 2) return null
        HrSample(parts[0].toIntOrNull() ?: return null, parts[1].toIntOrNull() ?: return null)
    }
}

fun formatSamples(samples: List<HrSample>): String = samples.joinToString(",") { "${it.offsetSec}:${it.bpm}" }

/** The mean of [values] rounded half up; null for no value. */
fun meanHalfUp(values: List<Int>): Int? {
    if (values.isEmpty()) return null
    val n = values.size.toLong()
    return Math.floorDiv(2 * values.sumOf { it.toLong() } + n, 2 * n).toInt()
}

/**
 * The average of one work round, computed when shown: the mean of the buckets of a filled row whose time lies in the
 * round's `[startedAt, endedAt)`, when 2 or more buckets are inside; else null.
 */
fun roundAverage(facts: HrFacts, round: IntervalLog): Int? {
    val row = (facts as? HrFacts.Filled)?.row ?: return null
    val start = round.startedAt ?: return null
    val end = round.endedAt ?: return null
    val inside = parseSamples(row.samples).orEmpty().filter { row.fromMs + it.offsetSec * 1000L in start until end }
    return if (inside.size >= 2) meanHalfUp(inside.map { it.bpm }) else null
}
