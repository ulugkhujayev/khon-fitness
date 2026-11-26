package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.toDate
import dev.mirzohidkhon.khonfitness.ui.components.ChartPoint
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Setup(val programId: String?, val sets: Int, val minReps: Int, val maxReps: Int)

fun setupOf(h: LiftSession): Setup {
    val first = h.sets.minBy { it.order }
    return Setup(h.session.programId, h.sets.size, first.minReps, first.maxReps)
}

data class SetupPeriod(val setup: Setup, val sessions: List<LiftSession>) {
    val programName: String get() =
        if (setup.programId == null) NO_PROGRAM
        else sessions.lastOrNull { it.session.programId == setup.programId }?.session?.programName ?: NO_PROGRAM
    val start: LocalDate get() = sessions.first().session.date.toDate()
}

const val NO_PROGRAM = "No program"

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

fun changeFacts(before: SetupPeriod, after: SetupPeriod): List<String> = listOfNotNull(
    if (before.setup.programId != after.setup.programId) "${before.programName} → ${after.programName}" else null,
    if (before.setup.sets != after.setup.sets) "${before.setup.sets} → ${after.setup.sets} sets" else null,
    if (before.setup.minReps != after.setup.minReps || before.setup.maxReps != after.setup.maxReps)
        "${repRange(before.setup)} → ${repRange(after.setup)} reps" else null,
)

fun repRange(s: Setup, dash: String = "–"): String = if (s.minReps == s.maxReps) "${s.minReps}" else "${s.minReps}$dash${s.maxReps}"

private fun setCount(s: Setup): String = "${s.sets} ${if (s.sets == 1) "set" else "sets"}"

private fun oneRep(s: Setup): Boolean = s.minReps == s.maxReps && s.minReps == 1

fun setupText(p: SetupPeriod): String = "${p.programName} · ${setupTail(p.setup)}"

fun setupTail(s: Setup): String = "${setCount(s)} · ${repRange(s)} ${if (oneRep(s)) "rep" else "reps"}"

fun spokenSetup(p: SetupPeriod): String =
    "${p.programName}, ${setCount(p.setup)}, ${repRange(p.setup, " to ")} ${if (oneRep(p.setup)) "repetition" else "repetitions"}"

fun withWeight(history: List<LiftSession>): Boolean = history.any { h -> h.sets.any { countsAsDone(it) && (it.weightKg ?: 0.0) > 0 } }

/** Times 30, so weight × (30 + reps) stays exact where weight × (1 + reps / 30) would not. */
private fun value30(h: LiftSession, withWeight: Boolean): BigDecimal? {
    val counted = h.sets.filter(::countsAsDone)
    return if (withWeight) counted.filter { (it.weightKg ?: 0.0) > 0 }.maxOfOrNull { BigDecimal.valueOf(it.weightKg!!) * BigDecimal.valueOf(30.0 + (it.reps ?: 0).toDouble()) }
    else counted.maxOfOrNull { BigDecimal.valueOf((it.reps ?: 0).toDouble() * 30) }
}

fun sessionValue(h: LiftSession, withWeight: Boolean): Double? = value30(h, withWeight)?.toDouble()?.div(30)

private val THIRTY = BigDecimal(30)
private val WEEK = BigDecimal(7)

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

fun spokenRate(rate: String?, withWeight: Boolean): String {
    if (rate == null) return "no rate"
    val unit = if (withWeight) "kilograms per week" else "repetitions per week"
    val sign = when (rate.first()) { '+' -> "plus "; '−' -> "minus "; else -> "" }
    return "$sign${rate.drop(1)} $unit"
}

fun periodDates(p: SetupPeriod, next: SetupPeriod?, today: LocalDate, dash: String = " – "): String {
    fun day(d: LocalDate) = if (d.year == today.year) monthDay(d) else "${monthDay(d)}, ${d.year}"
    if (next == null) return "Since ${day(p.start)}"
    val end = next.start.minusDays(1).coerceAtLeast(p.sessions.last().session.date.toDate())
    return if (end == p.start) day(p.start) else "${day(p.start)}$dash${day(end)}"
}

fun rateTitle(withWeight: Boolean): String = if (withWeight) "Est. 1RM per week" else "Reps per week"

data class PeriodRow(val dates: String, val programName: String, val tail: String, val rate: String?, val spoken: String)

fun periodRows(history: List<LiftSession>, today: LocalDate): List<PeriodRow> {
    val periods = setupPeriods(history)
    if (periods.size < 2) return emptyList()
    val weight = withWeight(history)
    return periods.mapIndexed { i, p ->
        val next = periods.getOrNull(i + 1)
        val rate = periodRate(p.sessions, weight)
        val spoken = "${periodDates(p, next, today, " to ")}, ${spokenSetup(p)}, ${spokenRate(rate, weight)}"
        PeriodRow(periodDates(p, next, today), p.programName, setupTail(p.setup), rate, spoken)
    }.asReversed()
}

fun bestPoints(history: List<LiftSession>): List<ChartPoint> {
    val firsts = setupPeriods(history).mapNotNull { p -> p.sessions.firstOrNull { it.best != null }?.session?.id }.toSet()
    return history.filter { it.best != null }.mapIndexed { i, h ->
        ChartPoint(h.session.date.toDate(), h.best!!.weightKg ?: 0.0, mark = h.record, change = i > 0 && h.session.id in firsts)
    }
}
