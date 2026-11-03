package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Block
import dev.mirzohidkhon.khonfitness.data.BlockExercise
import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.data.EatingWindow
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.KhonRepository
import dev.mirzohidkhon.khonfitness.data.Modality
import dev.mirzohidkhon.khonfitness.data.PlanItem
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.WindowDay
import dev.mirzohidkhon.khonfitness.data.iso
import dev.mirzohidkhon.khonfitness.data.toDate
import dev.mirzohidkhon.khonfitness.health.ImportDraft
import dev.mirzohidkhon.khonfitness.timer.PhaseKind
import dev.mirzohidkhon.khonfitness.timer.TimerState
import dev.mirzohidkhon.khonfitness.ui.components.fmt
import dev.mirzohidkhon.khonfitness.ui.components.stepValue
import dev.mirzohidkhon.khonfitness.update.Updater
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Time since a session's start: "34 min", "1 h 5 min". */
fun workoutTime(startedAt: Long, now: Long): String {
    val m = maxOf(0L, (now - startedAt) / 60_000)
    return if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"
}

/** Today's card while a cardio session runs without the timer: "In progress · 34 min", no time in its first minute. */
fun progressLine(startedAt: Long, now: Long): String =
    if (now - startedAt < 60_000) "In progress" else "In progress · ${workoutTime(startedAt, now)}"

/** Sets done of all sets of a program session: Today's progress bar and its lines. */
data class SetProgress(val done: Int, val total: Int)

fun setProgress(sessionId: String, logs: List<SetLog>): SetProgress {
    val mine = logs.filter { it.sessionId == sessionId }
    return SetProgress(mine.count { it.status == SetStatus.DONE }, mine.size)
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


/** The date Today shows for [now]: a new day at midnight, when the minute clock ticks or the app resumes. */
fun todayOf(now: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

/** The week plan's item for [date], without a one-day change: what "Back to plan" returns to. */
fun weekItem(plan: KhonRepository.PlanData, date: LocalDate): PlanItem =
    KhonRepository.PlanData(plan.plan, plan.overrides.filter { it.date != date.iso() }, plan.programs, plan.exercises).itemFor(date)

/**
 * First use: no program and no cardio workout to plan or change to. Today then offers "Set up your week". A week of
 * rest days with workouts to pick is a rest day with Change.
 */
fun hasNoPlan(plan: KhonRepository.PlanData): Boolean =
    plan.programs.isEmpty() && plan.exercises.none { it.kind == Kind.CARDIO && !it.archived }

private val sheetDate = DateTimeFormatter.ofPattern("EEE MMM d", Locale.US)

/** The Change sheet's title: "Today, Tue Nov 3". */
fun changeTitle(date: LocalDate): String = "Today, ${date.format(sheetDate)}"

/** What a pick in the Change sheet writes: a one-day change ([ChangePick.Override]), or none when it is the week plan's item. */
sealed interface ChangePick {
    data class Override(val itemType: String, val itemId: String?) : ChangePick
    data object BackToPlan : ChangePick
}

fun changePick(plan: KhonRepository.PlanData, date: LocalDate, itemType: String, itemId: String?): ChangePick {
    val week = weekItem(plan, date)
    val weekId = week.program?.id ?: week.cardio?.id
    return if (week.itemType == itemType && (itemType == ItemType.REST || weekId == itemId)) ChangePick.BackToPlan else ChangePick.Override(itemType, itemId)
}

/**
 * The card's line for today's item: "6 exercises · 18 sets" for a program, "Bike · 4 × 4 min, 3 min rest" or
 * "Run · Steady" for cardio, then " · changed for today" when a one-day change holds it.
 */
fun todayLine(item: PlanItem, blocks: List<Block>, blockExercises: List<BlockExercise>, modalities: List<Modality>): String {
    val line = when (item.itemType) {
        ItemType.PROGRAM -> {
            val ids = blocks.filter { it.programId == item.program?.id }.map { it.id }.toSet()
            val rows = blockExercises.filter { it.blockId in ids }
            val n = rows.map { it.exerciseId }.distinct().size
            "$n ${if (n == 1) "exercise" else "exercises"} · ${rows.sumOf { it.sets }} ${if (rows.sumOf { it.sets } == 1) "set" else "sets"}"
        }
        ItemType.CARDIO -> item.cardio?.let { ex ->
            val type = modalities.find { it.id == ex.modalityId }?.name
            val plan = if (ex.intervals) "${ex.rounds} × ${dur(ex.workSec)}, " + (if (ex.restSec > 0) "${dur(ex.restSec)} rest" else "no rest") else "Steady"
            listOfNotNull(type, plan).joinToString(" · ")
        } ?: ""
        else -> REST_LINE
    }
    return if (item.overridden && !item.isRest) "$line · changed for today" else line
}

const val REST_LINE = "Nothing planned. Stretch and recover."

/** "12 sets" when every set is done, else "3 of 12 sets". */
fun setsLine(p: SetProgress): String = if (p.done == p.total) "${p.total} ${if (p.total == 1) "set" else "sets"}" else "${p.done} of ${p.total} sets"

/**
 * The running card's line: "In progress · 3 of 12 sets" for a program; for cardio the interval timer's state
 * ("Round 2 of 4 · Work", "Paused · Round 2 of 4", "Warm-up", "Timer done") when it runs this session, "Timer stopped · 1:35" after
 * the timer's Stop ([timerStopped]), else the time since its start.
 */
fun runningLine(s: Session, logs: List<SetLog>, timer: TimerState?, now: Long): String {
    if (s.itemType == ItemType.PROGRAM) { val p = setProgress(s.id, logs); return "In progress · ${p.done} of ${p.total} sets" }
    if (timerStopped(s, timer?.sessionId)) return "Timer stopped · " + dev.mirzohidkhon.khonfitness.timer.mmss((s.timeSec ?: 0).toDouble())
    val t = timer?.takeIf { it.sessionId == s.id && !it.stretch } ?: return progressLine(s.startedAt, now)
    if (t.done) return "Timer done"
    val phase = when (t.phase.kind) {
        PhaseKind.WARMUP -> "Warm-up"
        PhaseKind.WORK -> "Round ${t.phase.round} of ${t.rounds} · Work"
        PhaseKind.REST -> "Round ${t.phase.round} of ${t.rounds} · Rest"
        PhaseKind.DONE -> "Timer done"
    }
    return if (t.paused) "Paused · $phase" else phase
}

/** "5:46 /km" from seconds and metres; null without both. */
fun paceLine(timeSec: Int?, distanceM: Int?): String? {
    if (timeSec == null || distanceM == null || timeSec <= 0 || distanceM <= 0) return null
    val p = Math.round(timeSec / (distanceM / 1000.0)).toInt()
    return "${p / 60}:${"%02d".format(p % 60)} /km"
}

/**
 * A saved session's stats: "12 sets · 3.1 t · 58 min" ("3 of 12 sets" when some were skipped) for a program,
 * "5.2 km · 30 min · 5:46 /km" for cardio. A part with no value is left out.
 */
fun statLine(s: Session, logs: List<SetLog>): String {
    if (s.itemType == ItemType.PROGRAM) {
        val mine = logs.filter { it.sessionId == s.id }
        val done = mine.filter { it.status == SetStatus.DONE }
        val tons = fmt(done.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) } / 1000.0, 1)
        return listOfNotNull(setsLine(SetProgress(done.size, mine.size)), "$tons t", s.finishedAt?.let { workoutTime(s.startedAt, it) }).joinToString(" · ")
    }
    return listOfNotNull(s.distanceM?.let { "${fmt(it / 1000.0)} km" }, s.timeSec?.let { "${it / 60} min" }, paceLine(s.timeSec, s.distanceM)).joinToString(" · ")
}

/** Green line on Today once today's item is done: "Done · 3 of 12 sets · 3.1 t · 58 min", "Done · 5.2 km · 30 min · 5:46 /km". */
fun doneLine(s: Session, logs: List<SetLog>): String = statLine(s, logs).let { if (it.isEmpty()) "Done" else "Done · $it" }

/**
 * Today's saved session for the card (the last one saved today, whatever the plan held) and how many other sessions
 * today saved. Once one is saved, the card shows it under its saved name and Change is gone.
 */
data class DoneToday(val session: Session?, val more: Int)

fun doneToday(sessions: List<Session>, date: LocalDate): DoneToday {
    val finished = sessions.filter { it.date == date.iso() && it.finished }
    return DoneToday(finished.maxByOrNull { it.finishedAt ?: it.startedAt }, maxOf(0, finished.size - 1))
}

/** "+1 more today": the other sessions saved today; the line opens History. */
fun moreLine(n: Int): String = "+$n more today"

/** The bodyweight range a typed or stepped value must be inside. */
const val KG_MIN = 20.0
const val KG_MAX = 250.0
const val KG_STEP = 0.1
const val KG_ERROR = "Enter 20-250 kg"

/** Where the entry starts before the first entry: a typical adult weight, so the first pick is a few steps. */
const val DEFAULT_KG = 75.0

/** The entry starts at the latest entry. Bodyweights come sorted by date. */
fun startKg(bodyweights: List<Bodyweight>): Double = bodyweights.lastOrNull()?.kg ?: DEFAULT_KG

/** The entry's text for a value: always one decimal, "83.0". */
fun kgText(kg: Double): String = String.format(Locale.US, "%.1f", kg)

private val KG_TEXT = Regex("""\d{1,3}([.]\d*)?|[.]\d+""")

/** The longest text a weight takes: "250.0" and a slip. */
const val KG_CHARS = 7

/**
 * The typed text as kilograms to 0.1: "83,5" and "83.5" both read; blank, not a number, longer than [KG_CHARS], or
 * outside 20-250 is null.
 */
fun parseKg(text: String): Double? {
    val t = text.trim().replace(',', '.')
    if (t.length > KG_CHARS || !KG_TEXT.matches(t)) return null
    val v = t.toDoubleOrNull() ?: return null
    if (v < KG_MIN || v > KG_MAX) return null
    return Math.round(v * 10) / 10.0
}

/** One − ([sign] -1) or + (1) from the text: from its number inside the range, else from [fallback]. */
fun stepKg(text: String, sign: Int, fallback: Double): Double {
    val base = parseKg(text) ?: text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { !it.isNaN() }?.coerceIn(KG_MIN, KG_MAX) ?: fallback
    return stepValue(base, sign, KG_STEP, KG_MIN, KG_MAX)
}

private val monthDay = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/**
 * The status line under "Bodyweight": "No entry yet"; "Last: 83.4 kg · Oct 30" before today's entry; then
 * "Saved for today · −0.4 kg since Oct 30" against the last entry before today ("Saved for today" when there is none).
 */
fun bodyweightStatus(bodyweights: List<Bodyweight>, today: LocalDate): String {
    val latest = bodyweights.lastOrNull() ?: return "No entry yet"
    val todays = bodyweights.find { it.date == today.iso() } ?: return "Last: ${kgText(latest.kg)} kg · ${latest.date.toDate().format(monthDay)}"
    val before = bodyweights.lastOrNull { it.date < today.iso() } ?: return "Saved for today"
    val since = before.date.toDate().format(monthDay)
    val diff = Math.round((todays.kg - before.kg) * 10) / 10.0
    if (diff == 0.0) return "Saved for today · same as $since"
    return "Saved for today · ${if (diff > 0) "+" else "−"}${fmt(Math.abs(diff))} kg since $since"
}

/** The entry button: its text, and whether a tap saves. */
data class EntryAction(val text: String, val enabled: Boolean)

/**
 * Save for a new day, Update when today has another value (a save replaces it), Saved (off) when the value is today's
 * entry. While the typed text is not a weight ([kg] null) the button is Save or Update, off: a tap saves nothing.
 */
fun entryAction(bodyweights: List<Bodyweight>, today: LocalDate, kg: Double?): EntryAction {
    val todays = bodyweights.find { it.date == today.iso() }
    val text = if (todays != null) "Update" else "Save"
    if (kg == null) return EntryAction(text, false)
    if (todays?.kg == kg) return EntryAction("Saved", false)
    return EntryAction(text, true)
}
