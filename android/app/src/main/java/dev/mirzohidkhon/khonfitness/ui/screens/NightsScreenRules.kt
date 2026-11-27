package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Night
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.toDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

const val NIGHT_AXIS_MIN_HOURS = 8
const val NIGHT_AXIS_STEP_HOURS = 4
private const val TWO_HOURS_SEC = 2 * 3600

/** The top of the Nights chart in hours: the next multiple of 2 at or above the longest night, at least 8. */
fun nightAxisHours(longestSec: Int?): Int =
    maxOf(NIGHT_AXIS_MIN_HOURS, ((longestSec ?: 0) + TWO_HOURS_SEC - 1) / TWO_HOURS_SEC * 2)

/** The guide lines of the Nights chart: every 4 hours from 0 up to [topHours]. */
fun nightAxisTicks(topHours: Int): List<Int> = (0..topHours step NIGHT_AXIS_STEP_HOURS).toList()

fun nightAxisLabel(hours: Int): String = "$hours h"

/** "average · last 14 nights", or "average · 11 of 14 nights" when fewer have data. */
fun nightsAverageCaption(count: Int): String =
    if (count == LAST_NIGHTS_DAYS) "average · last $LAST_NIGHTS_DAYS nights" else "average · $count of $LAST_NIGHTS_DAYS nights"

private val monthDay: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
private val longDay: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)

/** "shortest · Nov 3"; "shortest" with no night. */
fun nightsShortestCaption(shortest: Night?): String = shortest?.let { "shortest · ${it.date.format(monthDay)}" } ?: "shortest"

/** "7 nights", "1 night". */
fun nightCount(n: Int): String = if (n == 1) "1 night" else "$n nights"

fun weekdayLetter(day: LocalDate): String = day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)

/** The finished sessions of [day], in the order they started. */
fun finishedOn(day: LocalDate, sessions: List<Session>): List<Session> =
    sessions.filter { it.finished && it.date.toDate() == day }.sortedBy { it.startedAt }

const val NIGHT_DOTS = 2

/** What one bar of the Nights chart speaks: "Tuesday, November 3, 5 hours 40 minutes, GYM", or "…, no night data". */
fun nightBarSpoken(day: LocalDate, night: Night?, titles: List<String>): String =
    (listOf(day.format(longDay), night?.let { nightLengthSpoken(it.asleepSec) } ?: NO_NIGHT_DATA.lowercase(Locale.US)) + titles).joinToString(", ")
