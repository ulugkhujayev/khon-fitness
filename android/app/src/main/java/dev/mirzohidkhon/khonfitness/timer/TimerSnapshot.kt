package dev.mirzohidkhon.khonfitness.timer

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** The timer as it stood at wall-clock [savedAt]. Written on every transition, never per tick, so it survives process death. */
@Serializable
data class TimerSnapshot(val state: TimerState, val savedAt: Long) {
    /** What the live timer would show at [now]: the same tick runs over the gap, so a stretch still stops before the next Start. */
    fun advanced(now: Long): TimerState = state.tick((now - savedAt).coerceAtLeast(0L) / 1000.0)
    /**
     * [advanced] at the instant [wallMs], [elapsedMs] for a run with a clock: the clocks are checked first, each boundary
     * crossed in the gap gets its own time from the phase lengths, and a run that reaches its end there ends. An ended
     * run stays as it ended.
     */
    fun advancedAt(wallMs: Long, elapsedMs: Long): TimerState {
        if (state.done) return state
        val gap = (wallMs - savedAt).coerceAtLeast(0L) / 1000.0
        val s = state.checkClock(wallMs, elapsedMs).tickAt(gap, elapsedMs)
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

/** Private store for the one running timer. */
object TimerSnapshots {
    private const val KEY = "snapshot"
    private fun prefs(context: Context) = context.getSharedPreferences("timer_snapshot", Context.MODE_PRIVATE)
    fun save(context: Context, s: TimerState, now: Long = System.currentTimeMillis()) = prefs(context).edit { putString(KEY, TimerSnapshot(s, now).encode()) }
    fun exists(context: Context): Boolean = prefs(context).contains(KEY)
    fun load(context: Context): TimerSnapshot? = prefs(context).getString(KEY, null)?.let { text ->
        TimerSnapshot.decode(text).also { if (it == null) prefs(context).edit { remove(KEY) } }
    }
    /** Clears only [sessionId]'s snapshot, so a late cleanup never erases a timer started since. */
    fun clear(context: Context, sessionId: String) {
        if (load(context)?.state?.sessionId == sessionId) prefs(context).edit { remove(KEY) }
    }
    /** Clears the snapshot only while it is still [run]'s: the same session, [TimerState.runId] and run number. */
    fun clearRun(context: Context, run: TimerState) {
        val stored = load(context)?.state ?: return
        if (stored.sessionId == run.sessionId && stored.runId == run.runId && stored.clock?.number == run.clock?.number) prefs(context).edit { remove(KEY) }
    }
    fun clearAll(context: Context) = prefs(context).edit { remove(KEY) }
}
