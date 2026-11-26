package dev.mirzohidkhon.khonfitness.timer

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.core.content.edit
import dev.mirzohidkhon.khonfitness.data.Anchor
import dev.mirzohidkhon.khonfitness.data.RoundSpan
import dev.mirzohidkhon.khonfitness.data.TimerRunResult
import dev.mirzohidkhon.khonfitness.data.inStep
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * The timer as it stood at wall-clock [savedAt] and elapsed realtime [savedElapsed] (null in a snapshot of an older
 * version). Written on every transition, never per tick, so it survives process death.
 */
@Serializable
data class TimerSnapshot(val state: TimerState, val savedAt: Long, val savedElapsed: Long? = null) {
    /** What the live timer would show at [now]: the same tick runs over the gap, so a stretch still stops before the next Start. */
    fun advanced(now: Long): TimerState = state.tick((now - savedAt).coerceAtLeast(0L) / 1000.0)
    /**
     * [advanced] at the instant [wallMs], [elapsedMs] for a run with a clock: the clocks are checked first, each boundary
     * crossed in the gap gets its own time from the phase lengths, and a run that reaches its end there ends. An ended
     * run stays as it ended. A restore in step advances by elapsed realtime, so a drift of the wall clock moves no
     * boundary; a snapshot without [savedElapsed] advances by the wall clock and its run is untrusted.
     */
    fun advancedAt(wallMs: Long, elapsedMs: Long): TimerState {
        if (state.done) return state
        val checked = if (savedElapsed == null) state.untrusted() else state.checkClock(wallMs, elapsedMs)
        val byElapsed = savedElapsed != null && elapsedMs >= savedElapsed &&
            (checked.clock?.broken == false || inStep(Anchor(savedAt, savedElapsed), wallMs, elapsedMs))
        val gapMs = if (byElapsed) elapsedMs - savedElapsed!! else wallMs - savedAt
        val s = checked.tickAt(gapMs.coerceAtLeast(0L) / 1000.0, elapsedMs)
        return if (s.done) s.finishedAt(wallMs, elapsedMs) else s
    }
    /** Too old to trust: more than an hour past the moment the timer would have ended. The workout stays open for a manual entry. */
    fun expired(now: Long): Boolean = !state.done && now - savedAt > ((state.remaining + state.phases.drop(state.index + 1).sumOf { it.seconds }) * 1000).toLong() + 3_600_000L
    fun encode(): String = json.encodeToString(this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun decode(text: String?): TimerSnapshot? = text?.let { runCatching { json.decodeFromString<TimerSnapshot>(it) }.getOrNull() }?.takeIf { it.state.phases.isNotEmpty() }
    }
}

@Serializable
data class PendingRound(val index: Int, val startedAt: Long, val endedAt: Long)

/** The result of an ended cardio run that waits for its commit: one entry per session id and run number. */
@Serializable
data class PendingRun(
    val sessionId: String,
    val run: Int?,
    val runId: String,
    val durationSec: Int,
    val notedTimeSec: Int?,
    val from: Long?,
    val to: Long?,
    val rounds: List<PendingRound> = emptyList(),
) {
    fun result(): TimerRunResult =
        TimerRunResult(sessionId, run, durationSec, notedTimeSec, from, to, rounds.map { RoundSpan(it.index, it.startedAt, it.endedAt) })
    fun sameSlot(other: PendingRun): Boolean = sessionId == other.sessionId && run == other.run
    fun isRunOf(s: TimerState): Boolean = sessionId == s.sessionId && run == s.clock?.number && runId == s.runId

    companion object {
        fun of(s: TimerState): PendingRun? = s.runResult()?.let { r ->
            PendingRun(r.sessionId, r.run, s.runId, r.durationSec, r.notedTimeSec, r.from, r.to, r.rounds.map { PendingRound(it.index, it.startedAt, it.endedAt) })
        }
    }
}

/** Whether [a] and [b] are states of one run: the same session, [TimerState.runId] and run number. */
fun sameRun(a: TimerState?, b: TimerState?): Boolean =
    a != null && b != null && a.sessionId == b.sessionId && a.runId == b.runId && a.clock?.number == b.clock?.number

/**
 * Private store of the timer: the live slot holds one timer that waits, runs, or is paused (an ended stretch run until
 * its session is written), and the pending store holds the results of ended cardio runs until their commit settles.
 * Every read, write, and compare-and-remove runs under one lock, so no save lands between a comparison and its removal.
 */
object TimerSnapshots {
    private const val KEY = "snapshot"
    private const val PENDING = "pending"
    private val lock = Any()
    private val json = Json { ignoreUnknownKeys = true }
    private fun prefs(context: Context) = context.getSharedPreferences("timer_snapshot", Context.MODE_PRIVATE)

    /** Saves the live timer. An ended cardio run never takes the live slot: it goes to the pending store ([end]). */
    fun save(context: Context, s: TimerState, now: Long = System.currentTimeMillis(), elapsed: Long = SystemClock.elapsedRealtime()) = synchronized(lock) {
        val p = prefs(context)
        if (s.done && !s.stretch) endLocked(p, s) else p.edit { putString(KEY, TimerSnapshot(s, now, elapsed).encode()) }
    }

    /**
     * The end of run [s]: a cardio result joins the pending store (it replaces the entry of the same session and run
     * number), and the live slot is cleared while it still holds this run. A run that never left Ready adds nothing.
     */
    fun end(context: Context, s: TimerState) = synchronized(lock) { endLocked(prefs(context), s) }

    private fun endLocked(p: SharedPreferences, s: TimerState) {
        val entry = PendingRun.of(s)
        val live = sameRun(decodeLive(p)?.state, s)
        if (entry == null && !live) return
        p.edit {
            if (entry != null) putString(PENDING, json.encodeToString(pendingLocked(p).filterNot { it.sameSlot(entry) } + entry))
            if (live) remove(KEY)
        }
    }

    fun exists(context: Context): Boolean = synchronized(lock) { prefs(context).let { it.contains(KEY) || pendingLocked(it).isNotEmpty() } }

    /** Whether any ended cardio run waits for its commit. */
    fun hasPending(context: Context): Boolean = synchronized(lock) { pendingLocked(prefs(context)).isNotEmpty() }

    fun pending(context: Context): List<PendingRun> = synchronized(lock) { pendingLocked(prefs(context)) }

    /**
     * The live timer. A slot that does not decode is removed; an ended cardio run that an older version left in the
     * slot moves to the pending store.
     */
    fun load(context: Context): TimerSnapshot? = synchronized(lock) {
        val p = prefs(context)
        val text = p.getString(KEY, null) ?: return@synchronized null
        val snap = TimerSnapshot.decode(text)
        when {
            snap == null -> { p.edit { remove(KEY) }; null }
            snap.state.done && !snap.state.stretch -> { endLocked(p, snap.state); null }
            else -> snap
        }
    }

    /** Clears the live slot only while it holds [sessionId]'s timer. */
    fun clear(context: Context, sessionId: String) = synchronized(lock) {
        val p = prefs(context)
        if (decodeLive(p)?.state?.sessionId == sessionId) p.edit { remove(KEY) }
    }

    /** Clears the live slot only while it is still [run]'s: the same session, [TimerState.runId] and run number. */
    fun clearRun(context: Context, run: TimerState) = synchronized(lock) {
        val p = prefs(context)
        if (sameRun(decodeLive(p)?.state, run)) p.edit { remove(KEY) }
    }

    /** Removes the pending result of run [s] after its commit settled; another run's entry stays. */
    fun settle(context: Context, s: TimerState) = removePending(context) { it.isRunOf(s) }

    fun settle(context: Context, entry: PendingRun) = removePending(context) { it == entry }

    private fun removePending(context: Context, match: (PendingRun) -> Boolean) = synchronized(lock) {
        val p = prefs(context)
        val all = pendingLocked(p)
        val kept = all.filterNot(match)
        if (kept.size != all.size) p.edit { if (kept.isEmpty()) remove(PENDING) else putString(PENDING, json.encodeToString(kept)) }
    }

    /** Removes the live slot and every pending result, whatever run they hold. */
    fun clearAll(context: Context) = synchronized(lock) { prefs(context).edit { remove(KEY); remove(PENDING) } }

    private fun decodeLive(p: SharedPreferences): TimerSnapshot? = TimerSnapshot.decode(p.getString(KEY, null))

    private fun pendingLocked(p: SharedPreferences): List<PendingRun> =
        p.getString(PENDING, null)?.let { runCatching { json.decodeFromString<List<PendingRun>>(it) }.getOrNull() }.orEmpty()
}
