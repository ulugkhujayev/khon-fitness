package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.KhonRepository
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.PlanItem
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.StretchSession
import dev.mirzohidkhon.khonfitness.data.iso
import dev.mirzohidkhon.khonfitness.data.toDate
import dev.mirzohidkhon.khonfitness.ui.components.ChartPoint
import dev.mirzohidkhon.khonfitness.ui.components.fmt
import dev.mirzohidkhon.khonfitness.ui.components.matchesQuery
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---- Input: what happened up to today

/** The records History reads, each dated up to today: sessions, their sets, stretch runs and bodyweights. */
data class HistoryData(
    val sessions: List<Session> = emptyList(), val logs: List<SetLog> = emptyList(),
    val stretches: List<StretchSession> = emptyList(), val bodyweights: List<Bodyweight> = emptyList(),
)

/**
 * The one date filter of History: a session, stretch run or bodyweight dated after [today] (a wrong phone clock, an
 * import) counts nowhere, and neither do the sets of such a session or of a session that is gone. Every History
 * screen reads its records through this filter.
 */
fun historyData(today: LocalDate, sessions: List<Session>, logs: List<SetLog> = emptyList(), stretches: List<StretchSession> = emptyList(), bodyweights: List<Bodyweight> = emptyList()): HistoryData {
    val past = sessions.filter { it.date.toDate() <= today }
    val ids = past.map { it.id }.toSet()
    return HistoryData(past, logs.filter { it.sessionId in ids }, stretches.filter { it.date.toDate() <= today }, bodyweights.filter { it.date.toDate() <= today })
}

// ---- Log: the week

private val monthDay: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/** "Oct 29". */
fun monthDay(date: LocalDate): String = date.format(monthDay)

/** Monday of the week that holds [date]. */
fun weekOf(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

/** The week header: "Nov 2 – 8, 2026", "Oct 26 – Nov 1, 2026", "Dec 29, 2025 – Jan 4, 2026". */
fun weekTitle(monday: LocalDate): String {
    val end = monday.plusDays(6)
    return when {
        monday.year != end.year -> "${monthDay(monday)}, ${monday.year} – ${monthDay(end)}, ${end.year}"
        monday.month != end.month -> "${monthDay(monday)} – ${monthDay(end)}, ${end.year}"
        else -> "${monthDay(monday)} – ${end.dayOfMonth}, ${end.year}"
    }
}

/** The weeks the Log pages through: from the week of the first session (this week when none) to this week. */
fun logWeeks(sessions: List<Session>, today: LocalDate): ClosedRange<LocalDate> {
    val last = weekOf(today)
    val first = sessions.minOfOrNull { it.date }?.toDate()?.let(::weekOf)?.coerceAtMost(last) ?: last
    return first..last
}

/**
 * The plan History shows for a day: the resolved item, except that a past day whose program or cardio workout is gone
 * stays planned of its type (no program or exercise on the item; [planName] names it). Today and later resolve as the
 * Today tab does: a missing item is rest.
 */
fun historyPlanFor(plan: KhonRepository.PlanData, date: LocalDate, today: LocalDate): PlanItem {
    val item = plan.itemFor(date)
    if (!item.isRest || date >= today) return item
    val o = plan.overrides.find { it.date == date.iso() }
    val type = o?.itemType ?: plan.plan.find { it.weekday == date.dayOfWeek.value }?.itemType ?: ItemType.REST
    return if (type == ItemType.PROGRAM || type == ItemType.CARDIO) PlanItem(type, overridden = o?.auto == false) else item
}

/** A plan item's name in History: "Deleted program" or "Deleted workout" when its program or exercise is gone. */
fun planName(item: PlanItem): String = item.program?.name ?: item.cardio?.name ?: when (item.itemType) {
    ItemType.PROGRAM -> "Deleted program"
    ItemType.CARDIO -> "Deleted workout"
    else -> "Rest"
}

/** What a Log row shows for a day: a session, or the plan of a day without one. */
enum class DayKind { DONE, RUNNING, REST, SKIPPED, TODAY, PLANNED }

data class LogRow(val date: LocalDate, val kind: DayKind, val item: PlanItem, val session: Session? = null)

/**
 * The rows of a week, Monday to Sunday: one per session of a day in start order (finished = done, open = running),
 * else one for the plan: rest, skipped (a past planned day), today's item, or a later planned item.
 */
fun logRows(monday: LocalDate, today: LocalDate, sessions: List<Session>, planFor: (LocalDate) -> PlanItem): List<LogRow> =
    (0L until 7L).flatMap { i ->
        val date = monday.plusDays(i)
        val item = planFor(date)
        val day = sessions.filter { it.date.toDate() == date }.sortedBy { it.startedAt }
        if (day.isNotEmpty()) day.map { LogRow(date, if (it.finished) DayKind.DONE else DayKind.RUNNING, item, it) }
        else listOf(LogRow(date, when { item.isRest -> DayKind.REST; date < today -> DayKind.SKIPPED; date == today -> DayKind.TODAY; else -> DayKind.PLANNED }, item))
    }

/**
 * A week in numbers. A planned day (not rest; past days keep the item they had, a one-day change counts) is done
 * once it has a finished session; [missed] are planned days before today without one, [toGo] those from today on.
 * The totals count every finished session of the week: done sets, kg lifted, cardio seconds and meters.
 */
data class WeekSummary(val done: Int, val planned: Int, val missed: Int, val toGo: Int, val sets: Int, val liftedKg: Double, val cardioSec: Long, val meters: Long, val stretches: Int = 0)

fun weekSummary(monday: LocalDate, today: LocalDate, sessions: List<Session>, logs: List<SetLog>, planFor: (LocalDate) -> PlanItem, stretches: List<StretchSession> = emptyList(), exercises: List<Exercise> = emptyList()): WeekSummary {
    val days = (0L until 7L).map { monday.plusDays(it) }
    val week = sessions.filter { it.finished && it.date.toDate() in monday..days.last() }
    val doneDays = week.map { it.date.toDate() }.toSet()
    val planned = days.filter { !planFor(it).isRest }
    val open = planned.filter { it !in doneDays }
    val ids = week.filter { it.itemType == ItemType.PROGRAM }.map { it.id }.toSet()
    val sets = logs.filter { it.sessionId in ids && it.status == SetStatus.DONE }
    val cardio = week.filter { it.itemType == ItemType.CARDIO }
    val byId = exercises.associateBy { it.id }
    return WeekSummary(
        done = planned.size - open.size, planned = planned.size,
        missed = open.count { it < today }, toGo = open.count { it >= today },
        sets = sets.size, liftedKg = sets.sumOf { lifted(it) },
        cardioSec = cardio.sumOf { (it.timeSec ?: 0).toLong() }, meters = cardio.sumOf { (shownMeters(it, isSwim(byId[it.exerciseId])) ?: 0).toLong() },
        stretches = stretches.count { it.date.toDate() in monday..days.last() },
    )
}

/** The week card's right side: "2 to go" while this week has open days ahead, then "1 missed" or "Full week". */
fun weekTail(w: WeekSummary): String = when {
    w.toGo > 0 -> "${w.toGo} to go"
    w.missed > 0 -> "${w.missed} missed"
    w.planned == 0 -> ""
    else -> "Full week"
}

/** "34 sets · 9.8 t lifted · 95 min cardio · 15.2 km · 3 stretches". */
fun weekTotals(w: WeekSummary): String =
    "${count(w.sets, "set")} · ${tons(w.liftedKg)} lifted · ${roundMinutes(w.cardioSec)} min cardio · ${fmt(w.meters / 1000.0, 1).ifEmpty { "0" }} km · ${stretchCount(w.stretches)}"

private fun stretchCount(n: Int) = if (n == 1) "1 stretch" else "$n stretches"

/**
 * The stretch runs of a day, in start order, one line each: "Stretch: Daily · 10 min · 9 of 9", or
 * "Stretch: Hips · 4 min · skipped" when none was done.
 */
fun stretchLines(date: LocalDate, stretches: List<StretchSession>): List<String> =
    stretches.filter { it.date == date.iso() }.sortedBy { it.startedAt }.map { s ->
        val done = if (s.completed == 0 && s.skipped > 0) "skipped" else "${s.completed} of ${s.completed + s.skipped}"
        "Stretch: ${s.routineName} · ${sessionMinutes(s.totalSec)} · $done"
    }

/** One part of the week bar: a done day takes its session's color, an open planned day gray, a rest day near black. */
enum class BarPart { DONE, OPEN, REST }

/** The week bar, Monday to Sunday, with the first finished session of each done day. */
fun weekBar(monday: LocalDate, sessions: List<Session>, planFor: (LocalDate) -> PlanItem): List<Pair<BarPart, Session?>> =
    (0L until 7L).map { i ->
        val date = monday.plusDays(i)
        val done = sessions.filter { it.finished && it.date.toDate() == date }.minByOrNull { it.startedAt }
        when { done != null -> BarPart.DONE to done; planFor(date).isRest -> BarPart.REST to null; else -> BarPart.OPEN to null }
    }

// ---- Session lines

/** Weight × reps of one set; a set without weight lifts nothing. */
private fun lifted(l: SetLog) = (l.weightKg ?: 0.0) * (l.reps ?: 0)

/** "3.1 t". */
fun tons(kg: Double): String = "${fmt(kg / 1000.0, 1).ifEmpty { "0" }} t"

private fun count(n: Int, word: String) = "$n ${if (n == 1) word else word + "s"}"

/** Distance of a cardio session in meters: the distance, else laps × pool length; null when that passes [Int.MAX_VALUE]. */
fun meters(s: Session): Int? = s.distanceM ?: s.laps?.let { laps -> (laps.toLong() * (s.poolLength ?: 25)).takeIf { it <= Int.MAX_VALUE }?.toInt() }

private const val SLOWEST_SHOWN_METERS_PER_HOUR = 1000L

/** The distance to show and to pace by. A band on a stationary bike reports a few metres for a whole session: those are left out. */
fun shownMeters(s: Session, swim: Boolean): Int? {
    val m = meters(s) ?: return null
    val seconds = s.timeSec ?: 0
    return m.takeIf { swim || seconds <= 0 || m * 3600L >= SLOWEST_SHOWN_METERS_PER_HOUR * seconds }
}

/** Pace as "5:12 /km", or "2:05 /100 m" for a swim; null without time or distance. */
fun pace(seconds: Long?, meters: Long?, swim: Boolean): String? {
    if (seconds == null || seconds <= 0 || meters == null || meters <= 0) return null
    val per = Math.round(seconds / (meters / if (swim) 100.0 else 1000.0))
    // A no-break space and a word joiner after the slash keep "6:02 /km" on one line.
    return "%d:%02d\u00A0/\u2060%s".format(Locale.US, per / 60, per % 60, if (swim) "100\u00A0m" else "km")
}

/** The swim rule kept from the old History: pace per 100 m for the swim type. */
fun isSwim(ex: Exercise?): Boolean = ex?.modalityId == "swim"

/** "10 of 12 sets": done sets of all sets of a program session. */
fun setsLine(sessionId: String, logs: List<SetLog>): String {
    val mine = logs.filter { it.sessionId == sessionId }
    return "${mine.count { it.status == SetStatus.DONE }} of ${count(mine.size, "set")}"
}

/** Whole minutes from seconds, rounded to the nearest: the one minute rule of History. Sum seconds first, then round. */
fun roundMinutes(sec: Int): Int = (sec + 30) / 60

fun roundMinutes(sec: Long): Long = (sec + 30) / 60

/**
 * A saved session's time in History and on an import draft: [roundMinutes] of its seconds, "58 min", "65 min"; 3 min
 * 40 s is "4 min". Today's done line and the finish summary count the same minutes ([sessionTime]). Only a running
 * session's elapsed time ([workoutTime]) counts whole minutes.
 */
fun sessionMinutes(sec: Int) = "${roundMinutes(sec)} min"

/** Seconds from a session's start to [end], never below 0. */
fun sessionSeconds(startedAt: Long, end: Long): Int = ((end - startedAt) / 1000).toInt().coerceAtLeast(0)

/**
 * The stat line of a finished session in the Log. Gym: "10 of 12 sets · 3.1 t · 58 min". Cardio:
 * "5.2 km · 28 min · 5:23 /km", with "Health Connect" after an imported one.
 */
fun sessionLine(s: Session, logs: List<SetLog>, ex: Exercise?): String {
    if (s.itemType == ItemType.PROGRAM) {
        val done = logs.filter { it.sessionId == s.id && it.status == SetStatus.DONE }
        val time = s.finishedAt?.let { sessionMinutes(sessionSeconds(s.startedAt, it)) }
        return listOfNotNull(setsLine(s.id, logs), tons(done.sumOf { lifted(it) }), time).joinToString(" · ")
    }
    val m = shownMeters(s, isSwim(ex))
    return listOfNotNull(
        m?.let { "${fmt(it / 1000.0, 2)} km" }, s.timeSec?.let { sessionMinutes(it) }, pace(s.timeSec?.toLong(), m?.toLong(), isSwim(ex)),
        if (s.sourceId != null) "Health Connect" else null,
    ).joinToString(" · ").ifEmpty { "Saved" }
}

// ---- Progress: personal records

/**
 * One session of an exercise: every saved set in order (skipped ones too), the best counted set (null when none
 * counts, e.g. all skipped), whether that set was a new record, and its rep records in set order ([repRecordsOf]).
 */
data class LiftSession(val session: Session, val sets: List<SetLog>, val best: SetLog?, val record: Boolean, val repRecords: List<SetLog> = emptyList())

private val heavier = compareBy<SetLog>({ it.weightKg ?: 0.0 }, { it.reps ?: 0 })

/**
 * The record rule, for History and the finish sheet alike: the best of [sets] is the heaviest counted set ([countsAsDone];
 * a set without weight counts as 0 kg), then the most reps at that weight, then the earlier set. Null when none counts.
 */
fun bestSet(sets: List<SetLog>): SetLog? = sets.filter(::countsAsDone).reduceOrNull { b, l -> if (heavier.compare(l, b) > 0) l else b }

/** Whether [best] is a record over [top], the best before it: heavier, or as heavy with more reps. A tie keeps [top]. */
fun beats(best: SetLog?, top: SetLog?): Boolean = best != null && (top == null || heavier.compare(best, top) > 0)

private fun kg(l: SetLog) = l.weightKg ?: 0.0
private fun reps(l: SetLog) = l.reps ?: 0

/**
 * The rep records of one session's [sets] (set order) against the counted sets of the other sessions ([others]):
 * a counted set above 0 kg with more reps than every other set at its weight or heavier, when at least one such set
 * exists. A set of the same session as heavy or heavier with as many reps or more takes its place (of two equal sets,
 * the earlier one stays), and so does the session's weight record set ([weightRecord]).
 */
fun repRecordsOf(sets: List<SetLog>, others: List<SetLog>, weightRecord: SetLog? = null): List<SetLog> {
    val counted = sets.filter(::countsAsDone)
    val before = others.filter(::countsAsDone)
    return counted.filterIndexed { i, l ->
        val w = kg(l)
        val heavier = before.filter { kg(it) >= w }
        w > 0 && l !== weightRecord && heavier.isNotEmpty() && reps(l) > heavier.maxOf(::reps) &&
            counted.withIndex().none { (k, m) -> k != i && kg(m) >= w && reps(m) >= reps(l) && (kg(m) > w || reps(m) > reps(l) || k < i) }
    }
}

/**
 * Every finished session of an exercise with a saved set, oldest first. Only counted sets are record candidates: the
 * best set of a session is the heaviest weight, then the most reps at that weight, then the earlier set. A session
 * is a record when its best beats every earlier best; a tie keeps the record with the earlier session. 0-rep and
 * skipped sets never count. Rep records count against the sets of earlier sessions only.
 */
fun liftHistory(exerciseId: String, logs: List<SetLog>, sessions: List<Session>): List<LiftSession> {
    val finished = sessions.filter { it.finished }.associateBy { it.id }
    var top: SetLog? = null
    val earlier = mutableListOf<SetLog>()
    return logs.filter { it.exerciseId == exerciseId && it.sessionId in finished }
        .groupBy { it.sessionId }
        .map { (id, sets) -> finished.getValue(id) to sets.sortedBy { it.order } }
        .sortedWith(compareBy({ it.first.date }, { it.first.startedAt }))
        .map { (s, sets) ->
            val best = bestSet(sets)
            val record = beats(best, top)
            if (record) top = best
            val repSets = repRecordsOf(sets, earlier, if (record) best else null)
            earlier += sets.filter(::countsAsDone)
            LiftSession(s, sets, best, record, repSets)
        }
}

/** Every exercise's history with a record, by exercise id. */
fun liftHistories(logs: List<SetLog>, sessions: List<Session>): Map<String, List<LiftSession>> =
    logs.map { it.exerciseId }.distinct().associateWith { liftHistory(it, logs, sessions) }.filterValues { h -> h.any { it.record } }

/** How many exercises of a session set a record, not counting an exercise's first record. */
fun recordCount(sessionId: String, histories: Map<String, List<LiftSession>>): Int =
    histories.values.count { h -> h.filter { it.record }.drop(1).any { it.session.id == sessionId } }

/** How many exercises of a session set a rep record and no weight record ([recordCount] counts those). */
fun repRecordCount(sessionId: String, histories: Map<String, List<LiftSession>>): Int =
    histories.values.count { h -> h.any { it.session.id == sessionId && it.repRecords.isNotEmpty() && !it.record } }

/**
 * The most reps by weight of an exercise: every counted set that no other matches or beats in both weight and reps
 * (of equal sets, the earliest), heaviest first, each with its session.
 */
fun repBests(history: List<LiftSession>): List<Pair<SetLog, Session>> {
    val all = history.flatMap { h -> h.sets.filter(::countsAsDone).map { it to h.session } }
    return all.filterIndexed { i, (l, _) ->
        all.withIndex().none { (k, p) -> val m = p.first; k != i && kg(m) >= kg(l) && reps(m) >= reps(l) && (kg(m) > kg(l) || reps(m) > reps(l) || k < i) }
    }.sortedByDescending { kg(it.first) }
}

/** An exercise's personal record: the best set and the session that set it. */
data class PersonalRecord(val exerciseId: String, val name: String, val best: SetLog, val session: Session)

/** The record of every exercise with sets, the newest record first. Names come from [exercises], else the set's snapshot. */
fun personalRecords(histories: Map<String, List<LiftSession>>, exercises: List<Exercise>): List<PersonalRecord> =
    histories.mapNotNull { (id, h) ->
        val r = h.lastOrNull { it.record } ?: return@mapNotNull null
        val best = r.best ?: return@mapNotNull null
        PersonalRecord(id, exercises.find { it.id == id }?.name ?: best.exerciseName, best, r.session)
    }.sortedWith(compareByDescending<PersonalRecord> { it.session.date }.thenBy { it.name })

/** An exercise in the full list: its id, name, record (null without one), and whether the library archived it. */
data class LiftEntry(val exerciseId: String, val name: String, val record: PersonalRecord?, val archived: Boolean = false)

/**
 * Every strength exercise for the "All exercises" list: the library's, archived ones too, and every exercise with a
 * saved set in a finished session that the library lost, named by the set's snapshot. [query] keeps the names that
 * contain it, any case.
 */
fun liftEntries(records: List<PersonalRecord>, exercises: List<Exercise>, data: HistoryData, query: String = ""): List<LiftEntry> {
    val byId = records.associateBy { it.exerciseId }
    val library = exercises.filter { it.kind == Kind.STRENGTH }.map { LiftEntry(it.id, it.name, byId[it.id], it.archived) }
    val finished = data.sessions.filter { it.finished }.map { it.id }.toSet()
    val lost = data.logs.filter { l -> l.sessionId in finished && exercises.none { it.id == l.exerciseId } }
        .groupBy { it.exerciseId }.map { (id, sets) -> LiftEntry(id, byId[id]?.name ?: sets.first().exerciseName, byId[id]) }
    return (library + lost).filter { matchesQuery(it.name, query) }.sortedBy { it.name.lowercase() }
}

/** "62.5 kg × 6"; a set without weight reads "12 reps". */
fun setText(l: SetLog): String = if ((l.weightKg ?: 0.0) > 0) "${fmt(l.weightKg)} kg × ${l.reps ?: 0}" else count(l.reps ?: 0, "rep")

/** "62.5×6", the short form in a session's set list; "×12" without weight; "60×8 skipped" for a skipped set. */
fun shortSet(l: SetLog): String = "${if ((l.weightKg ?: 0.0) > 0) fmt(l.weightKg) else ""}×${l.reps ?: 0}" + if (l.status == SetStatus.SKIPPED) " skipped" else ""

// ---- Progress: bodyweight and cardio

/** The bodyweight chart's ranges: the last 30 days, 90 days, or all. */
enum class BodyRange(val label: String, val days: Long?) { MONTH("1M", 30), QUARTER("3M", 90), ALL("All", null) }

/** The entries of a range up to [today], oldest first; a later-dated entry is in no range. */
fun bodyweightsIn(range: BodyRange, all: List<Bodyweight>, today: LocalDate): List<Bodyweight> {
    val from = range.days?.let { today.minusDays(it) }
    return all.filter { val d = it.date.toDate(); (from == null || d >= from) && d <= today }.sortedBy { it.date }
}

/** The latest entry up to [today]. */
fun latestBodyweight(all: List<Bodyweight>, today: LocalDate): Bodyweight? = all.filter { it.date.toDate() <= today }.maxByOrNull { it.date }

/** "−0.4 kg since Oct 5": the latest entry against the first one of the range. */
fun bodyweightChange(inRange: List<Bodyweight>): String? {
    if (inRange.size < 2) return null
    val d = inRange.last().kg - inRange.first().kg
    val sign = when { d > 0.049 -> "+"; d < -0.049 -> "−"; else -> "±" }
    return "$sign${String.format(Locale.US, "%.1f", Math.abs(d))} kg since ${monthDay(inRange.first().date.toDate()).replace(' ', '\u00A0')}"
}

internal fun weekEntries(all: List<Bodyweight>, end: LocalDate): List<Bodyweight>? =
    all.filter { val d = it.date.toDate(); d >= end.minusDays(6) && d <= end }.takeIf { it.size >= 3 }

/** The mean of the entries dated from 6 days before [end] to [end]; null with fewer than 3. */
fun weekAverage(all: List<Bodyweight>, end: LocalDate): Double? = weekEntries(all, end)?.let { week -> week.sumOf { it.kg } / week.size }

/** The change of [weekAverage] over the last week: this week's against the one before; null without both. */
fun weekRate(all: List<Bodyweight>, today: LocalDate): Double? {
    val now = weekAverage(all, today) ?: return null
    val before = weekAverage(all, today.minusDays(7)) ?: return null
    return now - before
}

/** The entries' sum as the decimals they were entered as, so a mean or a change that ends in 5 rounds up as written. */
internal fun decimalSum(week: List<Bodyweight>): BigDecimal = week.fold(BigDecimal.ZERO) { s, b -> s + BigDecimal.valueOf(b.kg) }

/** [denominator] must be above 0: the sign comes from [numerator] alone. */
fun signedTenths(numerator: BigDecimal, denominator: BigDecimal): String {
    val size = numerator.abs().divide(denominator, 1, RoundingMode.HALF_UP)
    val sign = when { size.signum() == 0 -> "±"; numerator.signum() > 0 -> "+"; else -> "−" }
    return "$sign${size.toPlainString()}"
}

/**
 * "7-day avg 83.2 kg · −0.3 kg/week"; without a rate only the average; null without an average. Both figures round
 * half up on their decimal value, the rate on its size before its sign, so ±0.25 reads ±0.3 and "±" means below 0.05.
 */
fun trendText(all: List<Bodyweight>, today: LocalDate): String? {
    val now = weekEntries(all, today) ?: return null
    val nowSum = decimalSum(now)
    val avg = nowSum.divide(BigDecimal(now.size), 1, RoundingMode.HALF_UP)
    val rate = weekEntries(all, today.minusDays(7))?.let { before ->
        val scaled = nowSum * BigDecimal(before.size) - decimalSum(before) * BigDecimal(now.size)
        " · ${signedTenths(scaled, BigDecimal(now.size) * BigDecimal(before.size))} kg/week"
    }.orEmpty()
    return "7-day avg ${avg.toPlainString()} kg$rate"
}

/** The trend line of the chart: for each entry of [inRange], the [weekAverage] that ends on its date, from [all] entries. */
fun trendPoints(all: List<Bodyweight>, inRange: List<Bodyweight>): List<ChartPoint> =
    inRange.mapNotNull { e -> val d = e.date.toDate(); weekAverage(all, d)?.let { ChartPoint(d, it) } }

/** A cardio type over a time span: its sessions oldest first, totals, and pace from the sessions with both time and distance. */
data class CardioType(val id: String, val sessions: List<Session>, val meters: Long, val seconds: Long, val pace: String?)

/**
 * Every type (the exercise's modality; "" when the exercise is gone) with a finished cardio session up to [today],
 * in [order] of the type ids, the rest after. Each holds its sessions of the last [days] days: none for a type used
 * only before them.
 */
fun cardioTypes(sessions: List<Session>, exercises: List<Exercise>, today: LocalDate, days: Long = 30, order: List<String> = emptyList()): List<CardioType> {
    val from = today.minusDays(days)
    val byId = exercises.associateBy { it.id }
    return sessions.filter { it.finished && it.itemType == ItemType.CARDIO && it.date.toDate() <= today }
        .groupBy { byId[it.exerciseId]?.modalityId ?: "" }
        .map { (type, all) -> cardioType(type, all.filter { it.date.toDate() >= from }) }
        .sortedBy { order.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } }
}

/** Every type with a finished cardio session in [period], with the sessions of the period. */
fun cardioTypesIn(period: Period, sessions: List<Session>, exercises: List<Exercise>): List<CardioType> {
    val byId = exercises.associateBy { it.id }
    return sessions.filter { it.itemType == ItemType.CARDIO && it.countsIn(period) }
        .groupBy { byId[it.exerciseId]?.modalityId ?: "" }
        .map { (type, all) -> cardioType(type, all) }
}

private fun cardioType(type: String, sessions: List<Session>): CardioType {
    val sorted = sessions.sortedWith(compareBy({ it.date }, { it.startedAt }))
    val swim = type == "swim"
    val both = sorted.filter { (it.timeSec ?: 0) > 0 && (shownMeters(it, swim) ?: 0) > 0 }
    fun totalSeconds(list: List<Session>) = list.sumOf { (it.timeSec ?: 0).toLong() }
    fun totalMeters(list: List<Session>) = list.sumOf { (shownMeters(it, swim) ?: 0).toLong() }
    return CardioType(type, sorted, totalMeters(sorted), totalSeconds(sorted), pace(totalSeconds(both), totalMeters(both), swim))
}

/** "2 h 5 m", or "45 m" under an hour: [roundMinutes] of the seconds. */
fun durationText(sec: Long): String {
    val m = roundMinutes(sec)
    return if (m >= 60) "${m / 60} h ${m % 60} m" else "$m m"
}

/** "4 sessions · 21.4 km · 2 h 5 m · 5:51 /km", or "No sessions in 30 days" for a type without one in the span. */
fun cardioLine(t: CardioType, days: Long = 30): String {
    if (t.sessions.isEmpty()) return "No sessions in $days days"
    val km = t.meters.takeIf { it > 0 }?.let { "${fmt(it / 1000.0, 1)} km" }
    return listOfNotNull(count(t.sessions.size, "session"), km, durationText(t.seconds), t.pace).joinToString(" · ")
}
