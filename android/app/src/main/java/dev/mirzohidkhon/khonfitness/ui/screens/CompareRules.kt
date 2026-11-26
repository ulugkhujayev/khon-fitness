package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.MUSCLE_GROUPS
import dev.mirzohidkhon.khonfitness.data.Program
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.StretchSession
import dev.mirzohidkhon.khonfitness.data.toDate
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/** A span of dates, [from] and [to] both inclusive. */
data class Period(val from: LocalDate, val to: LocalDate) {
    init { require(from <= to) { "$from is after $to" } }
    val days: Int get() = (ChronoUnit.DAYS.between(from, to) + 1).toInt()
    val weeks: Double get() = days / 7.0
    operator fun contains(date: LocalDate): Boolean = date in from..to
}

fun Session.countsIn(p: Period): Boolean = finished && date.toDate() in p

/** The fixed periods of the compare sheet, each pair of neighbors without a gap or an overlap. */
enum class Preset(val label: String) {
    LAST_4("Last 4 weeks"), BEFORE_4("The 4 weeks before"),
    LAST_12("Last 12 weeks"), BEFORE_12("The 12 weeks before"),
    THIS_YEAR("This year"), LAST_YEAR("Last year");

    fun period(today: LocalDate): Period = when (this) {
        LAST_4 -> Period(today.minusDays(27), today)
        BEFORE_4 -> Period(today.minusDays(55), today.minusDays(28))
        LAST_12 -> Period(today.minusDays(83), today)
        BEFORE_12 -> Period(today.minusDays(167), today.minusDays(84))
        THIS_YEAR -> Period(today.withDayOfYear(1), today)
        LAST_YEAR -> today.minusYears(1).let { Period(it.withDayOfYear(1), it.withDayOfYear(it.lengthOfYear())) }
    }
}

/** From the first finished session of a program to its last; null without one. */
fun programPeriod(programId: String, sessions: List<Session>): Period? {
    val dates = sessions.filter { it.finished && it.itemType == ItemType.PROGRAM && it.programId == programId }.map { it.date.toDate() }
    return if (dates.isEmpty()) null else Period(dates.min(), dates.max())
}

fun programChoices(programs: List<Program>, sessions: List<Session>): List<Pair<Program, Period>> =
    programs.mapNotNull { p -> programPeriod(p.id, sessions)?.let { p to it } }

sealed interface PeriodChoice {
    data class Fixed(val preset: Preset) : PeriodChoice
    data class OfProgram(val programId: String) : PeriodChoice
    data class Custom(val period: Period) : PeriodChoice
}

fun choiceKey(c: PeriodChoice): String = when (c) {
    is PeriodChoice.Fixed -> "preset:${c.preset.name}"
    is PeriodChoice.OfProgram -> "program:${c.programId}"
    is PeriodChoice.Custom -> "custom:${c.period.from}:${c.period.to}"
}

/** The choice of a saved key; null for a key this version cannot read. */
fun parseChoice(key: String): PeriodChoice? = runCatching {
    val kind = key.substringBefore(':'); val rest = key.substringAfter(':')
    when (kind) {
        "preset" -> PeriodChoice.Fixed(Preset.valueOf(rest))
        "program" -> PeriodChoice.OfProgram(rest)
        "custom" -> PeriodChoice.Custom(Period(LocalDate.parse(rest.substringBefore(':')), LocalDate.parse(rest.substringAfter(':'))))
        else -> null
    }
}.getOrNull()

/** The period of a choice; null for a program without a finished session. */
fun choicePeriod(c: PeriodChoice, today: LocalDate, sessions: List<Session>): Period? = when (c) {
    is PeriodChoice.Fixed -> c.preset.period(today)
    is PeriodChoice.OfProgram -> programPeriod(c.programId, sessions)
    is PeriodChoice.Custom -> c.period
}

/** A new From; a From after the To moves the To to the same day. */
fun withFrom(p: Period, from: LocalDate): Period = Period(from, maxOf(from, p.to))

/** A new To; a To before the From moves the From to the same day. */
fun withTo(p: Period, to: LocalDate): Period = Period(minOf(p.from, to), to)

data class PeriodStats(
    val period: Period,
    val gymSessions: Int = 0,
    val sets: Int = 0,
    val liftedKg: Double = 0.0,
    val cardioSessions: Int = 0,
    val cardioSec: Int = 0,
    val meters: Int = 0,
    val stretches: Int = 0,
    val bodyweightKg: Double? = null,
    val bodyweights: Int = 0,
    val musclesPerWeek: Map<String, Double> = emptyMap(),
    val bestSets: Map<String, SetLog> = emptyMap(),
    val cardio: List<CardioType> = emptyList(),
) {
    val setsPerWeek: Double get() = sets / period.weeks
}

fun periodStats(
    period: Period, sessions: List<Session>, logs: List<SetLog>, exercises: List<Exercise>,
    stretches: List<StretchSession>, bodyweights: List<Bodyweight>,
): PeriodStats {
    val inPeriod = sessions.filter { it.countsIn(period) }
    val gym = inPeriod.filter { it.itemType == ItemType.PROGRAM }
    val cardio = inPeriod.filter { it.itemType == ItemType.CARDIO }
    val gymIds = gym.map { it.id }.toSet()
    val gymSets = logs.filter { it.sessionId in gymIds }
    val counted = gymSets.filter(::countsAsDone)
    val weights = bodyweights.filter { it.date.toDate() in period }
    return PeriodStats(
        period = period,
        gymSessions = gym.size,
        sets = counted.size,
        liftedKg = counted.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) },
        cardioSessions = cardio.size,
        cardioSec = cardio.sumOf { it.timeSec ?: 0 },
        meters = cardio.sumOf { meters(it) ?: 0 },
        stretches = stretches.count { it.date.toDate() in period },
        bodyweightKg = if (weights.isEmpty()) null else weights.sumOf { it.kg } / weights.size,
        bodyweights = weights.size,
        musclesPerWeek = muscleSets(period, logs, sessions, exercises).mapValues { it.value / period.weeks },
        bestSets = gymSets.groupBy { it.exerciseId }.mapNotNull { (id, sets) -> bestSet(sets)?.let { id to it } }.toMap(),
        cardio = cardioTypesIn(period, sessions, exercises),
    )
}

fun muscleSets(period: Period, logs: List<SetLog>, sessions: List<Session>, exercises: List<Exercise>): Map<String, Double> {
    val ids = sessions.filter { it.countsIn(period) }.map { it.id }.toSet()
    val exById = exercises.associateBy { it.id }
    val acc = LinkedHashMap<String, Double>()
    for (l in logs) {
        if (l.status != SetStatus.DONE || l.sessionId !in ids) continue
        val ex = exById[l.exerciseId] ?: continue
        forEachMuscleShare(ex) { m, share -> acc[m] = (acc[m] ?: 0.0) + share }
    }
    return acc
}

inline fun forEachMuscleShare(ex: Exercise, add: (muscle: String, share: Double) -> Unit) {
    ex.primaryMuscle?.let { add(it, 1.0) }
    ex.secondaryList.forEach { add(it, 0.5) }
}

/** "Oct 3 – Oct 30"; a date outside [today]'s year carries its year: "Dec 1, 2025 – Jan 5". One day is "Nov 27". */
fun periodRange(p: Period, today: LocalDate): String {
    fun day(d: LocalDate) = if (d.year == today.year) monthDay(d) else "${monthDay(d)}, ${d.year}"
    return if (p.from == p.to) day(p.from) else "${day(p.from)} – ${day(p.to)}"
}

const val NO_VALUE = "–"

private fun oneDecimal(v: Double) = String.format(Locale.US, "%.1f", v)

/** "15.2 km". */
fun kmText(meters: Int): String = "${oneDecimal(meters / 1000.0)} km"

data class CompareRow(val label: String, val left: String, val right: String)

data class CompareGroup(val title: String?, val rows: List<CompareRow>)

/**
 * The fact groups of the compare screen, the left period first in every row. A group without rows is left out, and so
 * is a cardio row whose two values are [NO_VALUE]. Exercises are named by [exercises], else the set's snapshot; a
 * cardio type by [typeName], in [order] of the type ids, the rest after.
 */
fun compareGroups(
    left: PeriodStats, right: PeriodStats, exercises: List<Exercise>,
    typeName: (String) -> String = { "Other" }, order: List<String> = emptyList(),
): List<CompareGroup> {
    fun row(label: String, f: (PeriodStats) -> String) = CompareRow(label, f(left), f(right))
    val totals = CompareGroup(null, listOf(
        row("Days") { it.period.days.toString() },
        row("Gym sessions") { it.gymSessions.toString() },
        row("Sets") { it.sets.toString() },
        row("Sets per week") { oneDecimal(it.setsPerWeek) },
        row("Lifted") { tons(it.liftedKg) },
        row("Cardio sessions") { it.cardioSessions.toString() },
        row("Cardio time") { durationText(it.cardioSec) },
        row("Distance") { kmText(it.meters) },
        row("Stretching") { it.stretches.toString() },
    ))
    val body = CompareGroup("Bodyweight", if (left.bodyweights + right.bodyweights == 0) emptyList() else listOf(
        row("Average") { s -> s.bodyweightKg?.let { "${oneDecimal(it)} kg" } ?: NO_VALUE },
        row("Entries") { it.bodyweights.toString() },
    ))
    val muscles = (left.musclesPerWeek.keys + right.musclesPerWeek.keys)
        .filter { (left.musclesPerWeek[it] ?: 0.0) > 0 || (right.musclesPerWeek[it] ?: 0.0) > 0 }
        .sortedWith(compareBy<String> { MUSCLE_GROUPS.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }.thenBy { it })
        .map { m -> row(m.replaceFirstChar { it.uppercase() }) { oneDecimal(it.musclesPerWeek[m] ?: 0.0) } }
    val names = exercises.associate { it.id to it.name }
    val best = (left.bestSets.keys + right.bestSets.keys)
        .map { id -> id to (names[id] ?: (left.bestSets[id] ?: right.bestSets.getValue(id)).exerciseName) }
        .sortedBy { it.second.lowercase() }
        .map { (id, name) -> row(name) { s -> s.bestSets[id]?.let(::setText) ?: NO_VALUE } }
    val types = (left.cardio.map { it.id } + right.cardio.map { it.id }).distinct()
        .sortedBy { order.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }
    val cardio = types.flatMap { id ->
        fun of(s: PeriodStats) = s.cardio.find { it.id == id }
        val name = typeName(id)
        listOf(
            row("$name · sessions") { (of(it)?.sessions?.size ?: 0).toString() },
            row("$name · distance") { s -> of(s)?.meters?.takeIf { it > 0 }?.let(::kmText) ?: NO_VALUE },
            row("$name · pace") { of(it)?.pace ?: NO_VALUE },
        )
    }.filterNot { it.left == NO_VALUE && it.right == NO_VALUE }
    return listOf(totals, body, CompareGroup("Sets per muscle · per week", muscles), CompareGroup("Best set", best), CompareGroup("Cardio", cardio))
        .filter { it.rows.isNotEmpty() }
}

/** What a fact row speaks: "Sets, 34 in Oct 3 – Oct 30, 41 in Oct 31 – Nov 27"; a missing value reads "none". */
fun compareSpoken(row: CompareRow, leftRange: String, rightRange: String): String {
    fun value(v: String) = if (v == NO_VALUE) "none" else v
    return "${row.label}, ${value(row.left)} in $leftRange, ${value(row.right)} in $rightRange"
}
