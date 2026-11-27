package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Night
import dev.mirzohidkhon.khonfitness.data.NightsState
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.toDate
import java.time.LocalDate
import java.time.ZoneId

/** "7 h 05 m"; "45 m" under an hour. Minutes are rounded as [roundMinutes]. */
fun nightLength(sec: Int): String {
    val m = roundMinutes(sec)
    return if (m < 60) "$m m" else "${m / 60} h ${(m % 60).toString().padStart(2, '0')} m"
}

/** "7 hours 5 minutes", "45 minutes", "1 hour 1 minute"; a whole hour has no minutes: "7 hours". */
fun nightLengthSpoken(sec: Int): String {
    val m = roundMinutes(sec)
    fun unit(n: Int, one: String) = if (n == 1) "1 $one" else "$n ${one}s"
    return when {
        m < 60 -> unit(m, "minute")
        m % 60 == 0 -> unit(m / 60, "hour")
        else -> "${unit(m / 60, "hour")} ${unit(m % 60, "minute")}"
    }
}

/** "23:10 – 06:15". */
fun nightClock(n: Night, zone: ZoneId): String = "${clockTime(n.fromMs, zone)} – ${clockTime(n.toMs, zone)}"

/** "Night 7 h 05 m · 23:10 – 06:15". */
fun nightLine(n: Night, zone: ZoneId): String = "Night ${nightLength(n.asleepSec)} · ${nightClock(n, zone)}"

/** "Night 7 hours 5 minutes, 23:10 to 06:15". */
fun nightLineSpoken(n: Night, zone: ZoneId): String =
    "Night ${nightLengthSpoken(n.asleepSec)}, ${clockRange(n.fromMs, n.toMs, zone)}"

const val NO_NIGHT_DATA = "No night data"

/**
 * The night line of [day] in the History Log: [nightLine] when it has a night; [NO_NIGHT_DATA] when the nights are
 * loaded, [day] is not after [today], and [day] is on or after the first day with a night; else null.
 */
fun nightLogLine(day: LocalDate, state: NightsState, today: LocalDate, zone: ZoneId): String? {
    val nights = (state as? NightsState.Loaded)?.nights ?: return null
    nights[day]?.let { return nightLine(it, zone) }
    val first = nights.keys.minOrNull() ?: return null
    return if (day <= today && day >= first) NO_NIGHT_DATA else null
}

/** What [nightLogLine] speaks. */
fun nightLogSpoken(day: LocalDate, state: NightsState, today: LocalDate, zone: ZoneId): String? {
    val night = (state as? NightsState.Loaded)?.nights?.get(day)
    return if (night != null) nightLineSpoken(night, zone) else nightLogLine(day, state, today, zone)
}

data class NightFact(val label: String, val value: String, val spoken: String)

/** The Review rows of the night before a session: "Night before" with the length, "Asleep" with the clock range. */
fun nightReviewRows(night: Night?, zone: ZoneId): List<NightFact> = if (night == null) emptyList() else listOf(
    NightFact("Night before", nightLength(night.asleepSec), "Night before, ${nightLengthSpoken(night.asleepSec)}"),
    NightFact("Asleep", nightClock(night, zone), "Asleep, ${clockRange(night.fromMs, night.toMs, zone)}"),
)

/** The average of [sec] in whole seconds (integer division of the sum); null for none. */
fun averageSec(sec: List<Int>): Int? = if (sec.isEmpty()) null else (sec.sumOf { it.toLong() } / sec.size).toInt()

const val LAST_NIGHTS_DAYS = 14

/** The [LAST_NIGHTS_DAYS] days that end today, oldest first, each with its night or null. */
data class LastNights(val days: List<Pair<LocalDate, Night?>>, val averageSec: Int?, val count: Int, val shortest: Night?)

/** The shortest night is the newest of the shortest ones. */
fun lastNights(nights: Map<LocalDate, Night>, today: LocalDate): LastNights {
    val days = (LAST_NIGHTS_DAYS - 1 downTo 0).map { today.minusDays(it.toLong()) }.map { it to nights[it] }
    val found = days.mapNotNull { it.second }
    val shortest = found.sortedByDescending { it.date }.minByOrNull { it.asleepSec }
    return LastNights(days, averageSec(found.map { it.asleepSec }), found.size, shortest)
}

const val NIGHT_BEFORE_DAYS = 56

/** The nights before the sessions of one title: the distinct days with a night, oldest first. */
data class TitleNights(val title: String, val days: List<LocalDate>, val averageSec: Int) {
    val count: Int get() = days.size
}

/**
 * The night before the finished sessions of the [NIGHT_BEFORE_DAYS] days that end [today], per title ([sessionName]).
 * A title without a night is left out. Most nights first, then the title.
 */
fun nightBeforeSessions(sessions: List<Session>, nights: Map<LocalDate, Night>, today: LocalDate): List<TitleNights> {
    val from = today.minusDays(NIGHT_BEFORE_DAYS - 1L)
    return sessions.filter { it.finished }
        .map { sessionName(it) to it.date.toDate() }
        .filter { (_, d) -> d in from..today }
        .groupBy({ it.first }, { it.second })
        .mapNotNull { (title, dates) ->
            val days = dates.distinct().filter { it in nights }.sorted()
            averageSec(days.map { nights.getValue(it).asleepSec })?.let { TitleNights(title, days, it) }
        }
        .sortedWith(compareByDescending<TitleNights> { it.count }.thenBy { it.title })
}
