package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** The week plan screen's note on what a weekday change does. */
const val WEEK_PLAN_NOTE = "Each weekday repeats every week. To change one day only, use Change on Today."

/** "Mon" for 1. */
fun weekdayShort(weekday: Int): String = DayOfWeek.of(weekday).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)

/** The weekdays (1 = Monday) whose template holds [itemId] as [itemType], in week order. */
fun KhonRepository.PlanData.weekdaysOf(itemType: String, itemId: String): List<Int> =
    plan.filter { it.itemType == itemType && it.itemId == itemId }.map { it.weekday }.sorted()

/** "Every Mon, Thu", or "Not in the week" when the template never holds the item. */
fun everyLine(weekdays: List<Int>): String = if (weekdays.isEmpty()) "Not in the week" else "Every " + weekdays.joinToString(", ") { weekdayShort(it) }

/** A gym program's line: "4 blocks · 22 sets · Every Tue, Sat". */
fun programLine(blocks: Int, sets: Int, weekdays: List<Int>): String = "${blocksText(blocks)} · ${setsText(sets)} · ${everyLine(weekdays)}"

/** A cardio workout's line: "4 × 4 min, 3 min rest · Every Thu", "Steady · Not in the week". */
fun cardioLine(ex: Exercise, weekdays: List<Int>): String {
    val plan = if (ex.intervals) "${ex.rounds} × ${dur(ex.workSec)}" + (if (ex.restSec > 0) ", ${dur(ex.restSec)} rest" else "") else "Steady"
    return "$plan · ${everyLine(weekdays)}"
}

/**
 * One day of the Plan tab's week strip: the item's name, whether it is today, whether a one-day change set it, its
 * [symbol], and a cardio workout's type id ([typeId]), whose color the symbol takes.
 */
data class WeekCell(
    val date: LocalDate, val name: String, val rest: Boolean, val today: Boolean, val changed: Boolean,
    val symbol: DaySymbol = DaySymbol.REST, val typeId: String? = null,
)

/** The symbol a day shows on the week strip: a gym program, a run, a ride, a swim, any other cardio, rest. */
enum class DaySymbol { GYM, RUN, BIKE, SWIM, CARDIO, REST }

/** A day's symbol: gym for a program; for cardio, by the type id (run, bike, swim; any other or none is CARDIO). */
fun daySymbol(item: PlanItem): DaySymbol = when (item.itemType) {
    ItemType.PROGRAM -> DaySymbol.GYM
    ItemType.CARDIO -> when (item.cardio?.modalityId) { "run" -> DaySymbol.RUN; "bike" -> DaySymbol.BIKE; "swim" -> DaySymbol.SWIM; else -> DaySymbol.CARDIO }
    else -> DaySymbol.REST
}

/** The line under the week strip for the selected day: "Wed · Rest", "Thu · Norwegian 4×4". */
fun WeekCell.line(): String = "${weekdayShort(date.dayOfWeek.value)} · $name"

/**
 * The week of [today], Monday to Sunday, as the plan resolves it: a dated change wins over the template. Each day
 * shows its item's own name, so two cardio workouts of one type ("Zone 2 run", "Tempo run") stay apart.
 */
fun KhonRepository.PlanData.weekCells(today: LocalDate): List<WeekCell> {
    val monday = today.minusDays(today.dayOfWeek.value - 1L)
    return (0L until 7L).map { monday.plusDays(it) }.map { d ->
        val item = itemFor(d)
        WeekCell(d, item.name, item.isRest, d == today, item.overridden, daySymbol(item), item.cardio?.modalityId)
    }
}

/** What a week cell says aloud: "Monday, Push. Today. Changed for this day only." */
fun WeekCell.spoken(fullName: String): String = buildString {
    append(date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)).append(", ").append(fullName).append('.')
    if (today) append(" Today.")
    if (changed) append(" Changed for this day only.")
}

/**
 * The Exercises list: live strength exercises that match [query] in [groups] by primary muscle (groups and names in
 * alphabetical order), and the archived ones that match in [archived], which hide under one row at the end
 * ([archivedTitle]). Cardio workouts live on Plan.
 */
data class LibraryList(val groups: List<Pair<String, List<Exercise>>>, val archived: List<Exercise>)

fun libraryList(exercises: List<Exercise>, query: String): LibraryList {
    val shown = exercises.filter { it.kind == Kind.STRENGTH && (query.isBlank() || it.name.contains(query.trim(), true)) }.sortedBy { it.name.lowercase() }
    val (archived, live) = shown.partition { it.archived }
    return LibraryList(live.groupBy { it.primaryMuscle?.replaceFirstChar { c -> c.uppercase() } ?: "Other" }.toSortedMap().toList(), archived)
}

/** The Exercises list's last row, which opens and closes the archived exercises: "Archived (3)". */
fun archivedTitle(count: Int) = "Archived ($count)"

/** The cardio workout editor's note under Type: the type is stored on the workout, not on each session. */
const val TYPE_NOTE = "Changes the color and type of past sessions too."

/** The types the Type picker lists: the live ones, and the workout's own type [current] even when archived. */
fun typeChoices(types: List<Modality>, current: String?): List<Modality> = types.filter { !it.archived || it.id == current }
