package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.components.ChartPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun shownAvgHr(s: Session, facts: HrFacts): Int? = s.avgHr ?: measuredAvgHr(facts)

fun measuredAvgHr(facts: HrFacts): Int? = (facts as? HrFacts.Filled)?.row?.avgHr

fun shownMaxHr(facts: HrFacts): Int? = (facts as? HrFacts.Filled)?.row?.maxHr

fun shownRoundHr(round: IntervalLog, facts: HrFacts): Int? = round.avgHr ?: roundAverage(facts, round)

fun factsById(sessions: List<Session>, rows: List<SessionHr>, zone: ZoneId): Map<String, HrFacts> {
    val byId = rows.associateBy { it.sessionId }
    return sessions.associate { it.id to hrFacts(it, byId[it.id], zone) }
}

fun rowOf(rows: List<SessionHr>, sessionId: String): SessionHr? = rows.find { it.sessionId == sessionId }

fun avgHrPoints(sessions: List<Session>, facts: Map<String, HrFacts>): List<ChartPoint> =
    sessions.mapNotNull { s -> shownAvgHr(s, facts.getValue(s.id))?.let { ChartPoint(s.date.toDate(), it.toDouble()) } }

fun workHrPoints(sessions: List<Session>, rounds: List<IntervalLog>, facts: Map<String, HrFacts>): List<ChartPoint> =
    sessions.mapNotNull { s ->
        rounds.filter { it.sessionId == s.id }.mapNotNull { shownRoundHr(it, facts.getValue(s.id)) }.takeIf { it.isNotEmpty() }
            ?.let { ChartPoint(s.date.toDate(), it.average()) }
    }

private fun avgMax(avg: Int?, max: Int?): String? =
    listOfNotNull(avg?.let { "avg $it" }, max?.let { "max $it" }).joinToString(" · ").ifEmpty { null }

fun hrLine(s: Session, facts: HrFacts): String? = avgMax(shownAvgHr(s, facts), shownMaxHr(facts))?.let { "$it bpm" }

private fun spokenAvgMax(avg: Int?, max: Int?): String? {
    val words = listOfNotNull(avg?.let { "average $it" }, max?.let { "maximum $it" })
    return if (words.isEmpty()) null else words.joinToString(", ") + " beats per minute"
}

fun hrSpoken(s: Session, facts: HrFacts): String? = spokenAvgMax(shownAvgHr(s, facts), shownMaxHr(facts))

fun hrStateText(s: Session, facts: HrFacts): String = when (facts) {
    is HrFacts.Filled -> avgMax(facts.row.avgHr, facts.row.maxHr) ?: "No data"
    is HrFacts.Waiting -> "Waiting for data"
    is HrFacts.None -> "No data"
    HrFacts.Removed -> "Removed"
    HrFacts.NotRead -> "Not read"
    HrFacts.NoRange -> "No time range"
}

fun hrStateSpoken(s: Session, facts: HrFacts): String =
    if (facts is HrFacts.Filled) spokenAvgMax(facts.row.avgHr, facts.row.maxHr) ?: "No data" else hrStateText(s, facts)

fun hrDataSessions(sessions: List<Session>, today: LocalDate): List<Session> {
    val from = today.minusDays(30).iso()
    val to = today.iso()
    return sessions.filter { it.finished && it.date in from..to }.sortedWith(compareByDescending<Session> { it.date }.thenByDescending { it.startedAt })
}

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm")

fun clockTime(ms: Long, zone: ZoneId): String = Instant.ofEpochMilli(ms).atZone(zone).format(CLOCK)

fun clockRange(from: Long, to: Long, zone: ZoneId): String = "${clockTime(from, zone)} to ${clockTime(to, zone)}"

fun gapText(sec: Int): String = when {
    sec < 60 -> "$sec s"
    sec % 60 == 0 -> "${sec / 60} min"
    else -> "${sec / 60} min ${sec % 60} s"
}

fun hrFactRows(s: Session, facts: HrFacts, rounds: List<IntervalLog>, zone: ZoneId, appLabel: (String) -> String): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    hrWindow(s, zone)?.let { rows += "Time range" to clockRange(it.fromMs, it.toMs, zone) }
    val row = when (facts) {
        is HrFacts.Filled -> facts.row
        is HrFacts.Waiting -> facts.row
        is HrFacts.None -> facts.row
        else -> null
    }
    if (facts !is HrFacts.Filled) rows += "Heart rate" to hrStateText(s, facts)
    if (row != null) {
        if (row.rawCount > 0) rows += "Readings" to "${row.rawCount} read · ${row.sampleCount} used"
        row.firstMs?.let { rows += "First reading" to clockTime(it, zone) }
        row.lastMs?.let { rows += "Last reading" to clockTime(it, zone) }
        row.maxGapSec?.takeIf { row.sampleCount >= 2 }?.let { rows += "Longest gap" to gapText(it) }
    }
    rounds.sortedBy { it.index }.forEachIndexed { i, r -> roundAverage(facts, r)?.let { rows += "Round ${i + 1}" to "$it bpm" } }
    if (row != null) {
        row.origin?.let { rows += "Source" to appLabel(it) }
        val others = row.origins.split(",").filter { it.isNotBlank() && it != row.origin }
        if (others.isNotEmpty()) rows += "Also seen" to others.joinToString(", ") { appLabel(it) }
    }
    measuredAvgHr(facts)?.let { rows += "Average" to "$it bpm" }
    shownMaxHr(facts)?.let { rows += "Maximum" to "$it bpm" }
    return rows
}

data class HrReadKey(val sheet: Int, val sessionId: String, val range: HrRange?, val row: SessionHr?)

fun hrReadKey(sheet: Int, s: Session, row: SessionHr?, zone: ZoneId) = HrReadKey(sheet, s.id, hrWindow(s, zone), row)

data class HrReadResult(val key: HrReadKey, val outcome: HrReread, val seen: Boolean)

private val COMMITTED = setOf(HrReread.FILLED, HrReread.WAITING, HrReread.NONE)

fun readResult(asked: HrReadKey, outcome: HrReread, now: HrReadKey?, left: SessionHr?): HrReadResult? = when {
    now == null || now.sheet != asked.sheet || now.sessionId != asked.sessionId || now.range != asked.range -> null
    outcome in COMMITTED -> HrReadResult(asked.copy(row = left), outcome, seen = now.row == left)
    now == asked -> HrReadResult(asked, outcome, seen = true)
    else -> null
}

fun HrReadResult.after(now: HrReadKey?): HrReadResult? = when {
    key == now -> if (seen) this else copy(seen = true)
    seen -> null
    else -> this
}

fun rereadLine(result: HrReread?, s: Session, facts: HrFacts): String? = when (result) {
    HrReread.FILLED -> if (facts is HrFacts.Filled) hrLine(s, facts) else null
    HrReread.WAITING, HrReread.NONE -> if (facts is HrFacts.Waiting || facts is HrFacts.None) "No data yet" else null
    HrReread.FAILED -> "Health Connect did not answer"
    HrReread.NO_RANGE, HrReread.DROPPED, null -> null
}

data class HrSheet(
    val facts: List<Pair<String, String>>,
    val reread: Boolean,
    val remove: Boolean,
    val useRange: HrRange?,
    val line: String?,
)

fun hrSheet(
    s: Session, row: SessionHr?, rounds: List<IntervalLog>, zone: ZoneId, result: HrReadResult?, now: HrReadKey?, appLabel: (String) -> String,
): HrSheet {
    val facts = hrFacts(s, row, zone)
    val bounds = confirmableBounds(s, zone)
    return HrSheet(
        facts = hrFactRows(s, facts, rounds, zone, appLabel) + listOfNotNull(bounds?.let { "Workout" to clockRange(it.fromMs, it.toMs, zone) }),
        reread = facts != HrFacts.NoRange,
        remove = row != null && row.state != HrState.OFF,
        useRange = bounds,
        line = result?.takeIf { now != null && it.key == now }?.let { rereadLine(it.outcome, s, facts) },
    )
}
