package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.BodyTarget
import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.data.LiftTarget
import dev.mirzohidkhon.khonfitness.data.bodyTargetValid
import dev.mirzohidkhon.khonfitness.data.liftTargetValid
import dev.mirzohidkhon.khonfitness.data.toDate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

const val TARGET_TREND_DAYS = 56L
const val TARGET_HORIZON_DAYS = 730L

/** When the present trend reaches a target, from measured values only. [rate] is the text the row names. */
sealed interface Forecast {
    /** [date] is the session that reached a lift target, or the entry date of a bodyweight target typed at its average. */
    data class Reached(val date: LocalDate?) : Forecast
    data class OnDate(val rate: String, val date: LocalDate) : Forecast
    data class Beyond(val rate: String) : Forecast
    data class NotAtThisRate(val rate: String) : Forecast
    data object NoLine : Forecast
}

private val THIRTY = BigDecimal(30)
private val WEEK = BigDecimal(7)

private fun forecastIn(rate: String, days: BigDecimal, today: LocalDate): Forecast =
    if (days > BigDecimal(TARGET_HORIZON_DAYS)) Forecast.Beyond(rate) else Forecast.OnDate(rate, today.plusDays(days.toLong()))

/**
 * The first session on or after the entry date at or above the target is "Reached". Else the least-squares line of
 * [periodRate] through the valued sessions of the last [TARGET_TREND_DAYS] days: the first day from [today] on which
 * the line is at or above the target, or [Forecast.Beyond] after [TARGET_HORIZON_DAYS] days.
 */
fun liftForecast(target: LiftTarget, history: List<LiftSession>, withWeight: Boolean, today: LocalDate): Forecast {
    val target30 = target.value * THIRTY
    history.firstOrNull { h ->
        val d = h.session.date.toDate()
        d >= target.date && d <= today && value30(h, withWeight)?.let { it >= target30 } == true
    }?.let { return Forecast.Reached(it.session.date.toDate()) }

    val from = today.minusDays(TARGET_TREND_DAYS - 1)
    val window = history.filter { h -> h.session.date.toDate().let { it >= from && it <= today } }
    val rate = periodRate(window, withWeight) ?: return Forecast.NoLine
    val points = window.mapNotNull { h -> value30(h, withWeight)?.let { h.session.date.toDate() to it } }
    val first = points.first().first
    val xs = points.map { BigDecimal(ChronoUnit.DAYS.between(first, it.first)) }
    val vs = points.map { it.second }
    val n = BigDecimal(points.size)
    val sx = xs.fold(BigDecimal.ZERO, BigDecimal::add)
    val sv = vs.fold(BigDecimal.ZERO, BigDecimal::add)
    val sxx = xs.fold(BigDecimal.ZERO) { s, x -> s + x * x }
    val sxv = xs.zip(vs).fold(BigDecimal.ZERO) { s, (x, v) -> s + x * v }
    val rise = n * sxv - sx * sv
    if (rise.signum() <= 0 || rate.startsWith("±")) return Forecast.NotAtThisRate(rate)
    val run = n * sxx - sx * sx
    // The line is sv / 30n + rise / (30 run) × (x − sx / n); it meets the target at this x.
    val meets = (sx * rise + (THIRTY * n * target.value - sv) * run).divide(n * rise, 0, RoundingMode.CEILING)
    val todayX = BigDecimal(ChronoUnit.DAYS.between(first, today))
    return forecastIn(rate, (meets - todayX).max(BigDecimal.ZERO), today)
}

/**
 * The direction is the sign of the target against the average stored with it, else against the present 7-day average
 * ([trendText]). Reached once the present average is at or beyond the target that way. Else the date that the present
 * weekly change needs for the distance, rounded up to a whole day.
 */
fun bodyForecast(target: BodyTarget, all: List<Bodyweight>, today: LocalDate): Forecast {
    val now = weekEntries(all, today)
    val n = now?.let { BigDecimal(it.size) }
    val nowSum = now?.let(::decimalSum)
    val gap = if (now != null) target.kg * n!! - nowSum!! else null
    val direction = target.startAvg?.let { (target.kg - it).signum() }?.takeIf { it != 0 } ?: gap?.signum() ?: return Forecast.NoLine
    if (direction == 0) return Forecast.Reached(target.date)
    if (gap == null) return Forecast.NoLine
    if (gap.signum() != direction) return Forecast.Reached(null)
    val before = weekEntries(all, today.minusDays(7)) ?: return Forecast.NoLine
    val nb = BigDecimal(before.size)
    val scaled = nowSum!! * nb - decimalSum(before) * n!!
    val rate = signedTenths(scaled, n * nb)
    if (rate.startsWith("±") || scaled.signum() != direction) return Forecast.NotAtThisRate(rate)
    val days = (gap.abs() * nb * WEEK).divide(scaled.abs(), 0, RoundingMode.CEILING)
    return forecastIn(rate, days, today)
}

/** The average stored with a new bodyweight target: the 7-day average as [trendText] prints it, else the latest entry. */
fun bodyStart(all: List<Bodyweight>, today: LocalDate): BigDecimal? =
    weekEntries(all, today)?.let { decimalSum(it).divide(BigDecimal(it.size), 1, RoundingMode.HALF_UP) }
        ?: latestBodyweight(all, today)?.let { BigDecimal.valueOf(it.kg) }

enum class TargetKind(val title: String, val spokenTitle: String, val unit: String, val step: Double, val decimals: Int, val spokenUnit: String) {
    BODY("Target bodyweight", "Target bodyweight", "kg", 0.1, 2, "kilograms"),
    LIFT_KG("Target est. 1RM", "Target estimated one rep max", "kg", 0.5, 2, "kilograms"),
    LIFT_REPS("Target reps", "Target repetitions", "reps", 1.0, 1, "repetitions"),
}

sealed interface TargetEntry {
    data object Clear : TargetEntry
    data class Store(val value: BigDecimal) : TargetEntry
    data object Refused : TargetEntry
}

fun targetEntry(draft: Double?, kind: TargetKind): TargetEntry {
    if (draft == null) return TargetEntry.Clear
    if (draft.isNaN() || draft.isInfinite()) return TargetEntry.Refused
    val v = BigDecimal.valueOf(draft)
    val ok = when (kind) {
        TargetKind.BODY -> bodyTargetValid(v)
        TargetKind.LIFT_KG -> liftTargetValid(v, reps = false)
        TargetKind.LIFT_REPS -> liftTargetValid(v, reps = true)
    }
    return if (ok) TargetEntry.Store(v) else TargetEntry.Refused
}

data class TargetLine(val label: String, val value: String?, val spoken: String)

data class TargetRows(val target: TargetLine, val set: Boolean, val result: TargetLine?)

private val spokenMonthDay: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d", Locale.US)

fun targetDate(d: LocalDate, today: LocalDate): String = if (d.year == today.year) monthDay(d) else "${monthDay(d)}, ${d.year}"

fun spokenTargetDate(d: LocalDate, today: LocalDate): String =
    if (d.year == today.year) d.format(spokenMonthDay) else "${d.format(spokenMonthDay)}, ${d.year}"

fun targetValue(value: BigDecimal, kind: TargetKind): String = when (kind) {
    TargetKind.BODY -> "${value.setScale(1, RoundingMode.HALF_UP).toPlainString()} kg"
    TargetKind.LIFT_KG -> "${value.stripTrailingZeros().toPlainString()} kg"
    TargetKind.LIFT_REPS -> value.stripTrailingZeros().toPlainString().let { if (it == "1") "1 rep" else "$it reps" }
}

private fun spokenValue(value: BigDecimal, kind: TargetKind): String {
    val shown = targetValue(value, kind).substringBefore(' ')
    val unit = if (shown == "1") kind.spokenUnit.removeSuffix("s") else kind.spokenUnit
    return "$shown $unit"
}

/** The rows of a "Target" group: the target, then the result when the trend gives one. */
fun targetRows(value: BigDecimal?, kind: TargetKind, forecast: Forecast?, today: LocalDate): TargetRows {
    val shown = value?.let { targetValue(it, kind) }
    val target = TargetLine("Target", shown ?: "Not set", "Target, ${value?.let { spokenValue(it, kind) } ?: "not set"}")
    if (value == null || forecast == null) return TargetRows(target, false, null)
    val unit = if (kind == TargetKind.LIFT_REPS) "reps/week" else "kg/week"
    fun at(rate: String) = "At $rate $unit"
    fun spokenAt(rate: String) = "At ${spokenRate(rate, kind != TargetKind.LIFT_REPS)}"
    val result = when (forecast) {
        Forecast.NoLine -> null
        is Forecast.Reached ->
            if (kind == TargetKind.BODY || forecast.date == null) TargetLine("Reached", null, "Reached")
            else TargetLine("Reached", targetDate(forecast.date, today), "Reached, ${spokenTargetDate(forecast.date, today)}")
        is Forecast.OnDate -> TargetLine(at(forecast.rate), targetDate(forecast.date, today), "${spokenAt(forecast.rate)}, ${spokenTargetDate(forecast.date, today)}")
        is Forecast.Beyond -> TargetLine(at(forecast.rate), "More than 2 years", "${spokenAt(forecast.rate)}, more than 2 years")
        is Forecast.NotAtThisRate -> TargetLine(at(forecast.rate), "Not at this rate", "${spokenAt(forecast.rate)}, not at this rate")
    }
    return TargetRows(target, true, result)
}

/** A stored lift target counts only while its unit matches the exercise's: est. 1RM with weight, else reps. */
fun liftTargetFor(stored: LiftTarget?, withWeight: Boolean): LiftTarget? = stored?.takeIf { it.reps != withWeight }

fun liftRows(stored: LiftTarget?, history: List<LiftSession>, today: LocalDate): TargetRows {
    val weight = withWeight(history)
    val kind = if (weight) TargetKind.LIFT_KG else TargetKind.LIFT_REPS
    val target = liftTargetFor(stored, weight)
    return targetRows(target?.value, kind, target?.let { liftForecast(it, history, weight, today) }, today)
}

fun bodyRows(stored: BodyTarget?, all: List<Bodyweight>, today: LocalDate): TargetRows =
    targetRows(stored?.kg, TargetKind.BODY, stored?.let { bodyForecast(it, all, today) }, today)
