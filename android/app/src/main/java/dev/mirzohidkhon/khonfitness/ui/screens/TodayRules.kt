package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.data.EatingWindow
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.WindowDay
import dev.mirzohidkhon.khonfitness.data.iso
import dev.mirzohidkhon.khonfitness.data.toDate
import dev.mirzohidkhon.khonfitness.health.ImportDraft
import dev.mirzohidkhon.khonfitness.ui.components.Ruler
import dev.mirzohidkhon.khonfitness.ui.components.fmt
import dev.mirzohidkhon.khonfitness.update.Updater
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Time since a session's start: "34 min", "1 h 5 min". */
fun workoutTime(startedAt: Long, now: Long): String {
    val m = maxOf(0L, (now - startedAt) / 60_000)
    return if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"
}

/** Today's card while a session runs: "In progress · 34 min", no time in its first minute. */
fun progressLine(startedAt: Long, now: Long): String =
    if (now - startedAt < 60_000) "In progress" else "In progress · ${workoutTime(startedAt, now)}"

/** Sets done of all sets of a running program session, for Today's progress bar. */
data class SetProgress(val done: Int, val total: Int)

fun setProgress(sessionId: String, logs: List<SetLog>): SetProgress {
    val mine = logs.filter { it.sessionId == sessionId }
    return SetProgress(mine.count { it.status == SetStatus.DONE }, mine.size)
}

/** Green line on Today once today's item is done: "Done · 12 sets · 3.4 t" or "Done · 30 min · 5 km". */
fun doneLine(s: Session, logs: List<SetLog>): String {
    if (s.itemType == ItemType.PROGRAM) {
        val done = logs.filter { it.sessionId == s.id && it.status == SetStatus.DONE }
        return "Done · ${done.size} ${if (done.size == 1) "set" else "sets"} · ${fmt(done.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) } / 1000.0, 1)} t"
    }
    return "Done · " + listOfNotNull(s.timeSec?.let { "${it / 60} min" }, s.distanceM?.let { "${fmt(it / 1000.0)} km" }).joinToString(" · ")
}

/** A routine's length and size: "10 min · 9 stretches", "1 min · 1 stretch". */
fun routineSummary(seconds: Int, count: Int): String = "${(seconds + 30) / 60} min · $count ${if (count == 1) "stretch" else "stretches"}"

/** The stretch row's line: the summary, after "Done today · " once stretched today. */
fun routineLine(summary: String, doneToday: Boolean): String = if (doneToday) "Done today · $summary" else summary

/** The window row's line: "Open · closes in 3 h 20 m", "Closed · opens 12:00", or "Off". */
fun windowStatus(w: EatingWindow?, day: WindowDay?, now: LocalDateTime): String {
    val (open, text) = windowSummary(w, day, now)
    return if (w != null && w.enabled) "${if (open) "Open" else "Closed"} · $text" else text
}

/** Today's band row: "2 new sessions" over "From the band, ready to import". */
fun pendingTitle(n: Int): String = if (n == 1) "1 new session" else "$n new sessions"
const val PENDING_LINE = "From the band, ready to import"

/** Import's list title: "1 new session from the band", "3 new sessions from the band". */
fun pendingLine(n: Int): String = "${pendingTitle(n)} from the band"

/** The import button: "Import 1 session", "Import all 3 sessions", or "Import 2 matched" when some need a pick. */
fun importAllLabel(ready: Int, total: Int): String = when {
    ready < total -> "Import $ready matched"
    total == 1 -> "Import 1 session"
    else -> "Import all $total sessions"
}

/** An import row's second line: "Mon, Oct 5 · 32 min · 5.2 km · 141 bpm · Mi Fitness", the app that wrote it last. */
fun draftStats(d: ImportDraft): String =
    listOfNotNull(d.date.format(shortDate), "${d.seconds / 60} min", d.distanceM?.let { "${fmt(it / 1000.0)} km" }, d.avgHr?.let { "$it bpm" }, d.originLabel.ifEmpty { null }).joinToString(" · ")

/** Today's update row: its name, its line in accent, and whether the APK is downloading (a tap then does nothing). */
data class UpdateRow(val title: String, val line: String, val downloading: Boolean)

/** "Update to v0.5.0" over "Tap to install", or "Downloading 42%" while it downloads; none without a newer release. */
fun updateRow(result: Updater.Result?, progress: Float?): UpdateRow? {
    val u = result as? Updater.Result.Available ?: return null
    return UpdateRow("Update to ${u.release.tag_name}", if (progress == null) "Tap to install" else "Downloading ${(progress * 100).toInt()}%", progress != null)
}

/** Whether a tap on install starts a download: a newer release is there and no download runs yet. */
fun startsUpdate(result: Updater.Result?, progress: Float?): Boolean = result is Updater.Result.Available && progress == null

/** Today's bodyweight ruler: 20 to 250 kg in 0.1 kg ticks 10 dp apart, a label on every kilogram. */
val BODYWEIGHT_RULER = Ruler(min = 20.0, max = 250.0, step = 0.1, gap = 10f, majorEvery = 10)

/** Where the ruler starts before the first entry: a typical adult weight, so the first pick is a short drag. */
const val DEFAULT_KG = 70.0

/** The ruler starts at the latest entry. Bodyweights come sorted by date. */
fun startKg(bodyweights: List<Bodyweight>): Double = bodyweights.lastOrNull()?.kg ?: DEFAULT_KG

private val monthDay = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/**
 * "+0.2 kg since Oct 5": the picked value against the last entry before today, so an update of today's entry
 * still compares with the day before. Empty when there is no earlier entry.
 */
fun changeSince(bodyweights: List<Bodyweight>, today: LocalDate, kg: Double): String {
    val before = bodyweights.lastOrNull { it.date < today.iso() } ?: return ""
    val since = before.date.toDate().format(monthDay)
    val diff = Math.round((kg - before.kg) * 10) / 10.0
    if (diff == 0.0) return "Same as $since"
    return "${if (diff > 0) "+" else "−"}${fmt(Math.abs(diff))} kg since $since"
}

/** The entry button: its text, and whether a tap saves. */
data class EntryAction(val text: String, val enabled: Boolean)

/**
 * Add for a new day, Update when today has another value (a save replaces it), Saved (off) when the value is today's
 * entry. While the typed text is blank or not above 0 ([valid] off) the button is Add or Update, off: a tap saves nothing.
 */
fun entryAction(bodyweights: List<Bodyweight>, today: LocalDate, kg: Double, valid: Boolean = true): EntryAction {
    val todays = bodyweights.find { it.date == today.iso() }
    val text = if (todays != null) "Update" else "Add"
    if (!valid || kg <= 0) return EntryAction(text, false)
    if (todays?.kg == kg) return EntryAction("Saved", false)
    return EntryAction(text, true)
}
