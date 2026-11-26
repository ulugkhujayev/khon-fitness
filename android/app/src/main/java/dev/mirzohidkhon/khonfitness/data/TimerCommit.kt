package dev.mirzohidkhon.khonfitness.data

/** The number [KhonDao.startTimerRun] gave a run, and the session's `timeSec` at that instant. */
data class TimerStart(val run: Int, val timeSec: Int?)

/** A work round's wall times; [index] is the round's `interval_logs.index`. */
data class RoundSpan(val index: Int, val startedAt: Long, val endedAt: Long)

/**
 * What the end of a cardio timer run writes ([KhonDao.commitTimerRun]). [run] is null for a run without a number (a
 * snapshot of an older version). [from], [to] and [rounds] are set only for a trusted run.
 */
data class TimerRunResult(
    val sessionId: String,
    val run: Int?,
    val durationSec: Int,
    val notedTimeSec: Int?,
    val from: Long?,
    val to: Long?,
    val rounds: List<RoundSpan>,
)

/** The outcome of a run's end. Only a [settled] outcome lets the run's snapshot go. */
enum class TimerCommit(val settled: Boolean) {
    COMMITTED(true),
    /** A newer run started, the run was committed already, or an older-version run met a session with a duration or a run. */
    DROPPED(true),
    GONE(true),
    NOTHING(true),
    /** A backup import or a reset ran since the commit call began. */
    RETRY(false),
    FAILED(false),
}

/** An ended run as its owner holds it: its [result] (null: nothing to write) and how to clear its own snapshot. */
class EndedRun(val result: TimerRunResult?, val clear: () -> Unit)

/** The running timer, as a Replace import, a reset, and the band import see it. */
interface TimerControl {
    /** Stops the live timer without writing its result; its snapshot stays. */
    suspend fun halt()
    /** Removes the timer snapshot, whatever run it holds. */
    fun clear()
    /** Brings the timer back from its snapshot, as after a process death. */
    fun resume()
    /** Whether an ended cardio run still waits for its commit, also one that the startup restore has not seen yet. */
    fun resultPending(): Boolean

    object None : TimerControl {
        override suspend fun halt() {}
        override fun clear() {}
        override fun resume() {}
        override fun resultPending(): Boolean = false
    }
}
