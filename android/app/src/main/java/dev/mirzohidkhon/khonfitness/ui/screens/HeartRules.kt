package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** The average a screen shows for a session: the typed one, else the measured one of a current filled read. */
fun shownAvgHr(s: Session, facts: HrFacts): Int? = s.avgHr ?: measuredAvgHr(facts)

/** The average of a current filled read, the cardio form's placeholder. */
fun measuredAvgHr(facts: HrFacts): Int? = (facts as? HrFacts.Filled)?.row?.avgHr

/** The maximum is only ever measured. */
fun shownMaxHr(facts: HrFacts): Int? = (facts as? HrFacts.Filled)?.row?.maxHr

/** A round's average: the typed one, else the one computed from a current filled read ([roundAverage]). */
fun shownRoundHr(round: IntervalLog, facts: HrFacts): Int? = round.avgHr ?: roundAverage(facts, round)

/** The heart rate facts of a session map: a session with no row has [hrFacts] of null. */
fun factsById(sessions: List<Session>, rows: List<SessionHr>, zone: ZoneId): Map<String, HrFacts> {
    val byId = rows.associateBy { it.sessionId }
    return sessions.associate { it.id to hrFacts(it, byId[it.id], zone) }
}

private fun avgMax(avg: Int?, max: Int?): String? =
    listOfNotNull(avg?.let { "avg $it" }, max?.let { "max $it" }).joinToString(" · ").ifEmpty { null }

/** Review's line: "avg 128 · max 164 bpm", "avg 128 bpm", "max 164 bpm"; null with neither. */
fun hrLine(s: Session, facts: HrFacts): String? = avgMax(shownAvgHr(s, facts), shownMaxHr(facts))?.let { "$it bpm" }

/** [hrLine] in full words: "average 128, maximum 164 beats per minute". */
fun hrSpoken(s: Session, facts: HrFacts): String? {
    val words = listOfNotNull(shownAvgHr(s, facts)?.let { "average $it" }, shownMaxHr(facts)?.let { "maximum $it" })
    return if (words.isEmpty()) null else words.joinToString(", ") + " beats per minute"
}

/** The state of a session on "Heart rate data", as a fact. */
fun hrStateText(s: Session, facts: HrFacts): String = when (facts) {
    is HrFacts.Filled -> avgMax(shownAvgHr(s, facts), shownMaxHr(facts)) ?: "No data"
    is HrFacts.Waiting -> "Waiting for data"
    is HrFacts.None -> "No data"
    HrFacts.Removed -> "Removed"
    HrFacts.NotRead -> "Not read"
    HrFacts.NoRange -> "No time range"
}

/** [hrStateText] for a screen reader. */
fun hrStateSpoken(s: Session, facts: HrFacts): String =
    if (facts is HrFacts.Filled) hrSpoken(s, facts) ?: "No data" else hrStateText(s, facts)

/** The finished sessions of the last 30 days up to [today], newest first. */
fun hrDataSessions(sessions: List<Session>, today: LocalDate): List<Session> {
    val from = today.minusDays(30).iso()
    val to = today.iso()
    return sessions.filter { it.finished && it.date in from..to }.sortedWith(compareByDescending<Session> { it.date }.thenByDescending { it.startedAt })
}

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm")

fun clockTime(ms: Long, zone: ZoneId): String = Instant.ofEpochMilli(ms).atZone(zone).format(CLOCK)

fun clockRange(from: Long, to: Long, zone: ZoneId): String = "${clockTime(from, zone)} to ${clockTime(to, zone)}"

/** "45 s", "2 min", "16 min 5 s". */
fun gapText(sec: Int): String = when {
    sec < 60 -> "$sec s"
    sec % 60 == 0 -> "${sec / 60} min"
    else -> "${sec / 60} min ${sec % 60} s"
}

/**
 * The fact rows of the "Heart rate data" sheet, label to value, each only with a value: the range, readings, first
 * and last reading, the longest gap (not for one reading), each work round's average, the source and the other
 * packages seen, average and maximum. Readings and their details come only from a current row ([hrFacts]).
 * [appLabel] names a package.
 */
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
    shownAvgHr(s, facts)?.let { rows += "Average" to "$it bpm" }
    shownMaxHr(facts)?.let { rows += "Maximum" to "$it bpm" }
    return rows
}

/** "Read again" is there for a session with a range. */
fun canReread(facts: HrFacts): Boolean = facts != HrFacts.NoRange

/** "Remove" is there while a row exists that is not `off`. */
fun canRemove(row: SessionHr?): Boolean = row != null && row.state != HrState.OFF

/**
 * The fact line after "Read again", or null. A result shows only while it still agrees with what [hrFacts] gives now,
 * so a late result after Remove or a range change shows nothing by itself.
 */
fun rereadLine(result: HrReread?, s: Session, facts: HrFacts): String? = when (result) {
    HrReread.FILLED -> if (facts is HrFacts.Filled) hrLine(s, facts) else null
    HrReread.WAITING, HrReread.NONE -> if (facts is HrFacts.Waiting || facts is HrFacts.None) "No data yet" else null
    HrReread.FAILED -> "Health Connect did not answer"
    HrReread.NO_RANGE, HrReread.DROPPED, null -> null
}
