package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.HrFacts
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.toDate
import dev.mirzohidkhon.khonfitness.ui.components.ChartPoint
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

const val LOAD_WEEKS = 52
const val LOAD_CHART_WEEKS = 12
const val LOAD_STEPS = 4
private val WEIGHT_PERCENTS = listOf(50, 60, 70, 80, 90)
private const val MIN_LOAD_CHART_POINTS = 2

/** 0 below 50 % of [max], then one more for each 10 % up to 5 at 90 % and over: `bpm * 100` against `max * percent`. */
fun loadWeight(bpm: Int, max: Int): Int = WEIGHT_PERCENTS.count { bpm * 100L >= max.toLong() * it }

/**
 * Weighted minutes: each bucket of [pictureBuckets] counts [SCORE_BUCKET_SEC] seconds times its weight, over 60, rounded
 * half up, and at most [Int.MAX_VALUE].
 */
fun bucketsLoad(facts: HrFacts.Filled, max: Int): Int {
    val sec = pictureBuckets(facts).sumOf { loadWeight(it.bpm, max).toLong() * SCORE_BUCKET_SEC }
    return Math.floorDiv(2 * sec + 60, 120L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
}

/** Null without a finished session, a filled fact, or a maximum in use. */
fun sessionLoad(session: Session, facts: HrFacts?, max: Int?): Int? {
    if (!session.finished || max == null) return null
    return bucketsLoad(facts as? HrFacts.Filled ?: return null, max)
}

/** One day of the grid: [sessions] finished there, and the sum of their loads ([dayLoad]), null when none has one. */
data class LoadDay(val date: LocalDate, val sessions: Int, val load: Int?) {
    val empty: Boolean get() = sessions == 0
}

/** The sum of [loads], at most [Int.MAX_VALUE]; null for no load. */
fun dayLoad(loads: List<Int>): Int? =
    loads.takeIf { it.isNotEmpty() }?.sumOf { it.toLong() }?.coerceAtMost(Int.MAX_VALUE.toLong())?.toInt()

/** A week from its Monday; [days] stop at today, so the current week may hold fewer than 7. */
data class LoadWeek(val monday: LocalDate, val days: List<LoadDay>) {
    val load: Long? get() = days.mapNotNull { it.load }.takeIf { it.isNotEmpty() }?.sumOf { it.toLong() }
    val sessionDays: Int get() = days.count { !it.empty }
}

/** [weeks] newest first; [highest] is the highest day load of all of them. */
data class LoadYear(val weeks: List<LoadWeek>) {
    val highest: Int? get() = weeks.flatMap { w -> w.days.mapNotNull { it.load } }.maxOrNull()
    val anyLoad: Boolean get() = weeks.any { it.load != null }
}

fun loadYear(sessions: List<Session>, facts: Map<String, HrFacts>, max: Int?, today: LocalDate): LoadYear {
    val current = weekOf(today)
    val first = current.minusWeeks(LOAD_WEEKS - 1L)
    val byDate = sessions.filter { it.finished }.groupBy { it.date.toDate() }.filterKeys { it in first..today }
    val weeks = (0 until LOAD_WEEKS).map { i ->
        val monday = current.minusWeeks(i.toLong())
        val days = (0L until 7L).map { monday.plusDays(it) }.filter { it <= today }.map { d ->
            val on = byDate[d].orEmpty()
            LoadDay(d, on.size, dayLoad(on.mapNotNull { sessionLoad(it, facts[it.id], max) }))
        }
        LoadWeek(monday, days)
    }
    return LoadYear(weeks)
}

/**
 * Step 1 up to a quarter of [highest], 2 up to a half, 3 up to three quarters, 4 above. A [highest] of 0 gives every
 * loaded day step 1.
 */
fun loadStep(load: Int, highest: Int): Int = when {
    LOAD_STEPS.toLong() * load <= highest -> 1
    2L * load <= highest -> 2
    LOAD_STEPS.toLong() * load <= 3L * highest -> 3
    else -> 4
}

/** The weeks with a value among the last [LOAD_CHART_WEEKS], oldest first; empty with fewer than 2. */
fun loadChart(year: LoadYear): List<ChartPoint> =
    year.weeks.take(LOAD_CHART_WEEKS).mapNotNull { w -> w.load?.let { ChartPoint(w.monday, it.toDouble()) } }.asReversed()
        .takeIf { it.size >= MIN_LOAD_CHART_POINTS }.orEmpty()

/** The short month name on the week whose drawn [LoadWeek.days] hold that month's first day, else null. */
fun loadMonth(week: LoadWeek): String? =
    week.days.firstOrNull { it.date.dayOfMonth == 1 }?.date?.month?.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

fun loadWeekSpoken(week: LoadWeek): String {
    val date = "${week.monday.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${week.monday.dayOfMonth}"
    val load = week.load?.let { "load $it" } ?: "no load"
    val days = week.sessionDays
    return "Week of $date, $load, $days ${if (days == 1) "day" else "days"}"
}
