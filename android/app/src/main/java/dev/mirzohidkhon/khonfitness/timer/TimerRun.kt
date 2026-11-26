package dev.mirzohidkhon.khonfitness.timer

import dev.mirzohidkhon.khonfitness.data.Anchor
import dev.mirzohidkhon.khonfitness.data.RoundSpan
import dev.mirzohidkhon.khonfitness.data.TimerRunResult
import dev.mirzohidkhon.khonfitness.data.TimerStart
import dev.mirzohidkhon.khonfitness.data.inStep
import kotlinx.serialization.Serializable

/** A work round of a run in elapsed realtime: [end] stays null while the round runs. */
@Serializable
data class RoundTime(val round: Int, val start: Long, val end: Long? = null)

/**
 * The clock facts of one cardio run: its [number] ([TimerStart.run]), the anchor taken at Start, the session's
 * [notedTimeSec] at Start, and the elapsed realtime of each round boundary and of the end. [broken] is set for good
 * once the clocks were not in step at a check. [closed] is set by [finishedAt], after the check at the end.
 */
@Serializable
data class RunClock(
    val number: Int,
    val anchorWall: Long,
    val anchorElapsed: Long,
    val notedTimeSec: Int? = null,
    val broken: Boolean = false,
    val rounds: List<RoundTime> = emptyList(),
    val endElapsed: Long? = null,
    val closed: Boolean = false,
) {
    val anchor: Anchor get() = Anchor(anchorWall, anchorElapsed)
}

/**
 * The Start action of a run that waits on Ready, at the instant [wallMs], [elapsedMs]: the anchor and, for a cardio
 * run with a [start], its number and the noted duration. Without [start] the run has no clock, like a snapshot of an
 * older version.
 */
fun TimerState.beginRun(start: TimerStart?, wallMs: Long, elapsedMs: Long): TimerState {
    if (done || !awaitingStart) return this
    val begun = begin()
    if (stretch || start == null) return begun
    return begun.copy(clock = RunClock(start.run, wallMs, elapsedMs, start.timeSec))
}

internal fun TimerState.workBegins(at: Long?): TimerState {
    val c = clock ?: return this
    if (at == null || done || awaitingStart || preparing || phase.kind != PhaseKind.WORK) return this
    if (c.rounds.any { it.round == phase.round }) return this
    return copy(clock = c.copy(rounds = c.rounds + RoundTime(phase.round, at)))
}

internal fun TimerState.workEnds(at: Long?): TimerState {
    val c = clock ?: return this
    if (at == null || done || phase.kind != PhaseKind.WORK) return this
    return copy(clock = c.copy(rounds = c.rounds.map { if (it.round == phase.round && it.end == null) it.copy(end = at) else it }))
}

internal fun TimerState.ended(at: Long?): TimerState {
    val c = clock ?: return this
    if (at == null || c.endElapsed != null) return this
    return copy(clock = c.copy(endElapsed = at))
}

/** Skip at elapsed realtime [elapsedMs]: the round in work ends there, and the round that follows starts there. */
fun TimerState.skipAt(elapsedMs: Long): TimerState {
    if (done) return this
    val next = workEnds(elapsedMs).skip()
    return if (next.done) next.ended(elapsedMs) else next.workBegins(elapsedMs)
}

/** The end of the run at [elapsedMs] (its last second, Stop, or a restore past its end): an open round ends there. */
fun TimerState.endAt(elapsedMs: Long): TimerState = workEnds(elapsedMs).ended(elapsedMs)

/**
 * The state that the end of a run at [wallMs], [elapsedMs] leaves (Finish, Stop, or the last phase): done, clock
 * checked. A run that ended already stays as it ended, so a retry of its commit is not checked again.
 */
fun TimerState.finishedAt(wallMs: Long, elapsedMs: Long): TimerState {
    if (clock?.closed == true) return this
    val ended = endAt(elapsedMs).checkClock(wallMs, elapsedMs)
    return ended.copy(index = phases.size, remaining = 0.0, clock = ended.clock?.copy(closed = true))
}

/** The run stays trusted only while the wall clock and elapsed realtime moved by the same amount since its anchor. */
fun TimerState.checkClock(wallMs: Long, elapsedMs: Long): TimerState {
    val c = clock ?: return this
    if (c.broken || inStep(c.anchor, wallMs, elapsedMs)) return this
    return copy(clock = c.copy(broken = true))
}

internal fun TimerState.untrusted(): TimerState = clock?.let { copy(clock = it.copy(broken = true)) } ?: this

val TimerState.trusted: Boolean get() = clock?.let { !it.broken && it.endElapsed != null } == true

/**
 * What the end of this cardio run commits: null for a stretch run and for a run that never left Ready. A run without
 * a clock gives no number; an untrusted run gives no range and no round times.
 */
fun TimerState.runResult(): TimerRunResult? {
    if (stretch || awaitingStart) return null
    val duration = elapsed.toInt()
    val c = clock ?: return TimerRunResult(sessionId, null, duration, null, null, null, emptyList())
    val end = c.endElapsed
    if (!trusted || end == null) return TimerRunResult(sessionId, c.number, duration, c.notedTimeSec, null, null, emptyList())
    val a = c.anchor
    val rounds = c.rounds.mapNotNull { r -> r.end?.let { RoundSpan(r.round - 1, a.wallAt(r.start), a.wallAt(it)) } }
    return TimerRunResult(sessionId, c.number, duration, c.notedTimeSec, c.anchorWall, a.wallAt(end), rounds)
}
