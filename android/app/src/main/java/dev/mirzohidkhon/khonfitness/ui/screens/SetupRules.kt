package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.toDate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** What a session ran for one exercise. The program's name is not part of it: a renamed program is the same setup. */
data class Setup(val programId: String?, val sets: Int, val minReps: Int, val maxReps: Int)

/** The exercise's set rows of the session (every status), and the rep range of the first one in execution order. */
fun setupOf(h: LiftSession): Setup {
    val first = h.sets.minBy { it.order }
    return Setup(h.session.programId, h.sets.size, first.minReps, first.maxReps)
}

/**
 * A run of sessions with one [setup], oldest first. [programName] is the name of the period's latest session that ran
 * its program, "No program" without one.
 */
data class SetupPeriod(val setup: Setup, val sessions: List<LiftSession>) {
    val programName: String get() =
        if (setup.programId == null) NO_PROGRAM
        else sessions.lastOrNull { it.session.programId == setup.programId }?.session?.programName ?: NO_PROGRAM
    val start: LocalDate get() = sessions.first().session.date.toDate()
}

const val NO_PROGRAM = "No program"

/**
 * The periods of an exercise's [history] (oldest first): runs of consecutive sessions with the same [Setup]. A run of
 * one session that is not the latest run joins the period before it, else the next one; the latest run is a period
 * of its own unless its setup is that of the period before.
 */
fun setupPeriods(history: List<LiftSession>): List<SetupPeriod> {
    val runs = mutableListOf<MutableList<LiftSession>>()
    for (h in history) {
        val last = runs.lastOrNull()
        if (last != null && setupOf(last.first()) == setupOf(h)) last += h else runs += mutableListOf(h)
    }
    val periods = mutableListOf<SetupPeriod>()
    val waiting = mutableListOf<LiftSession>()
    runs.forEachIndexed { i, run ->
        val setup = setupOf(run.first())
        val before = periods.lastOrNull()
        when {
            run.size == 1 && i < runs.lastIndex && before == null -> waiting += run
            run.size == 1 && i < runs.lastIndex && before != null -> periods[periods.lastIndex] = before.copy(sessions = before.sessions + run)
            before != null && before.setup == setup -> periods[periods.lastIndex] = before.copy(sessions = before.sessions + run)
            else -> { periods += SetupPeriod(setup, waiting + run); waiting.clear() }
        }
    }
    return periods
}

/** What changed from [before] to [after], in this order: program, sets, reps; only what differs. */
fun changeFacts(before: SetupPeriod, after: SetupPeriod): List<String> = listOfNotNull(
    if (before.setup.programId != after.setup.programId) "${before.programName} → ${after.programName}" else null,
    if (before.setup.sets != after.setup.sets) "${before.setup.sets} → ${after.setup.sets} sets" else null,
    if (before.setup.minReps != after.setup.minReps || before.setup.maxReps != after.setup.maxReps)
        "${repRange(before.setup)} → ${repRange(after.setup)} reps" else null,
)

/** "8–12", or "5" when both ends are one number. */
fun repRange(s: Setup): String = if (s.minReps == s.maxReps) "${s.minReps}" else "${s.minReps}–${s.maxReps}"

/** "TP v4 · 4 sets · 5–8 reps", "No program · 1 set · 5 reps". */
fun setupText(p: SetupPeriod): String = "${p.programName} · ${setupTail(p.setup)}"

/** The part of [setupText] after the program name: "4 sets · 5–8 reps". */
fun setupTail(s: Setup): String = "${s.sets} ${if (s.sets == 1) "set" else "sets"} · ${repRange(s)} ${if (s.minReps == s.maxReps && s.minReps == 1) "rep" else "reps"}"

/** Whether any counted set of the exercise's whole [history] is above 0 kg. */
fun withWeight(history: List<LiftSession>): Boolean = history.any { h -> h.sets.any { countsAsDone(it) && (it.weightKg ?: 0.0) > 0 } }

/**
 * A session's value, times 30 so the Epley estimate stays exact: with weight, the greatest weight × (30 + reps) of its
 * counted sets above 0 kg; without, the most reps of a counted set × 30. Null when no set gives one.
 */
private fun value30(h: LiftSession, withWeight: Boolean): BigDecimal? {
    val counted = h.sets.filter(::countsAsDone)
    return if (withWeight) counted.filter { (it.weightKg ?: 0.0) > 0 }.maxOfOrNull { BigDecimal.valueOf(it.weightKg!!) * BigDecimal(30 + (it.reps ?: 0)) }
    else counted.maxOfOrNull { BigDecimal((it.reps ?: 0) * 30) }
}

/** The estimated one-rep maximum (Epley: weight × (1 + reps / 30)) of the best counted set, or the most reps. */
fun sessionValue(h: LiftSession, withWeight: Boolean): Double? = value30(h, withWeight)?.toDouble()?.div(30)

private val THIRTY = BigDecimal(30)
private val WEEK = BigDecimal(7)

/**
 * The least-squares slope of the [sessions]' values against their dates, per week, as "+0.8", "−0.3", "±0.0". Null
 * with fewer than 3 valued sessions or less than 14 days from the first valued session to the last.
 */
fun periodRate(sessions: List<LiftSession>, withWeight: Boolean): String? {
    val points = sessions.mapNotNull { h -> value30(h, withWeight)?.let { h.session.date.toDate() to it } }
    if (points.size < 3) return null
    val first = points.first().first
    if (ChronoUnit.DAYS.between(first, points.last().first) < 14) return null
    val xs = points.map { BigDecimal(ChronoUnit.DAYS.between(first, it.first)) }
    val vs = points.map { it.second }
    val n = BigDecimal(points.size)
    val sx = xs.fold(BigDecimal.ZERO, BigDecimal::add)
    val sv = vs.fold(BigDecimal.ZERO, BigDecimal::add)
    val sxx = xs.fold(BigDecimal.ZERO) { s, x -> s + x * x }
    val sxv = xs.zip(vs).fold(BigDecimal.ZERO) { s, (x, v) -> s + x * v }
    return signedTenths(WEEK * (n * sxv - sx * sv), THIRTY * (n * sxx - sx * sx))
}

/** "plus 0.8 kilograms per week", "minus 1.0 reps per week", "0.0 kilograms per week", or "no rate". */
fun spokenRate(rate: String?, withWeight: Boolean): String {
    if (rate == null) return "no rate"
    val unit = if (withWeight) "kilograms per week" else "reps per week"
    val sign = when (rate.first()) { '+' -> "plus "; '−' -> "minus "; else -> "" }
    return "$sign${rate.drop(1)} $unit"
}

/** "Since Nov 3" for the latest period, else "Oct 2 – Nov 2" up to the day before [next] starts; a year not [today]'s is added. */
fun periodDates(p: SetupPeriod, next: SetupPeriod?, today: LocalDate): String {
    fun day(d: LocalDate) = if (d.year == today.year) monthDay(d) else "${monthDay(d)}, ${d.year}"
    if (next == null) return "Since ${day(p.start)}"
    val end = next.start.minusDays(1).coerceAtLeast(p.sessions.last().session.date.toDate())
    return if (end == p.start) day(p.start) else "${day(p.start)} – ${day(end)}"
}

/** The title of the rates group: "Est. 1RM per week", or "Reps per week" for an exercise that never had weight. */
fun rateTitle(withWeight: Boolean): String = if (withWeight) "Est. 1RM per week" else "Reps per week"

/** One row of the rates group. */
data class PeriodRow(val dates: String, val programName: String, val tail: String, val rate: String?, val spoken: String)

/** The rows of the rates group, newest first; empty when the exercise has fewer than two periods. */
fun periodRows(history: List<LiftSession>, today: LocalDate): List<PeriodRow> {
    val periods = setupPeriods(history)
    if (periods.size < 2) return emptyList()
    val weight = withWeight(history)
    return periods.mapIndexed { i, p ->
        val dates = periodDates(p, periods.getOrNull(i + 1), today)
        val rate = periodRate(p.sessions, weight)
        PeriodRow(dates, p.programName, setupTail(p.setup), rate, "$dates, ${setupText(p).replace(" · ", ", ")}, ${spokenRate(rate, weight)}".replace(" – ", " to ").replace("–", " to "))
    }.asReversed()
}

/** The dates of the first session of each period after the first, for the chart's change lines. */
fun changeDates(history: List<LiftSession>): List<LocalDate> = setupPeriods(history).drop(1).map { it.start }
