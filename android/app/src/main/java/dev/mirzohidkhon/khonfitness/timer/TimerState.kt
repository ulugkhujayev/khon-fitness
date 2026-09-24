package dev.mirzohidkhon.khonfitness.timer

import kotlinx.serialization.Serializable

@Serializable
enum class PhaseKind { WARMUP, WORK, REST, DONE }
@Serializable
data class Phase(
    val kind: PhaseKind, val seconds: Int, val round: Int,
    /** Stretch fields: the stretch name, side ("" / "Left" / "Right"), figure key, rep count for a reps stretch. */
    val label: String = "", val side: String = "", val figure: String = "", val reps: Int = 0, val stretchId: String = "",
)

@Serializable
data class TimerState(
    val sessionId: String,
    val title: String,
    val phases: List<Phase>,
    val index: Int = 0,
    val remaining: Double = 0.0,
    val elapsed: Double = 0.0,
    val paused: Boolean = false,
    val speed: Double = 1.0,
    /** True for a stretch routine: the phases carry names and figures, the end writes a StretchSession. */
    val stretch: Boolean = false,
    val skipped: Int = 0,
    val awaitingStart: Boolean = stretch,
    /** Preparation uses real seconds and never counts as exercise time. */
    val preparation: Double = 0.0,
    val completed: Int = 0,
    val startedAt: Long = 0L,
) {
    val phase: Phase get() = phases[index.coerceIn(0, phases.size - 1)]
    val done: Boolean get() = phases.isEmpty() || index >= phases.size
    val rounds: Int get() = phases.count { it.kind == PhaseKind.WORK }
    val total: Int get() = phases.sumOf { it.seconds }
    val workRemaining: Double get() = if (done) 0.0 else remaining + phases.drop(index + 1).sumOf { it.seconds }
    val preparing: Boolean get() = preparation > 0.0
    val active: Boolean get() = !done && !paused && !awaitingStart && !preparing

    fun begin(): TimerState = if (done || !awaitingStart) this else
        copy(awaitingStart = false, paused = false, preparation = 3.0)

    fun skip(): TimerState = if (done) this else advance(skippedPhase = true)

    private fun advance(skippedPhase: Boolean = false): TimerState {
        val next = index + 1
        return copy(index = next, remaining = phases.getOrNull(next)?.seconds?.toDouble() ?: 0.0,
            preparation = 0.0, awaitingStart = stretch && next < phases.size,
            paused = if (stretch) false else paused,
            skipped = skipped + if (skippedPhase) 1 else 0,
            completed = completed + if (skippedPhase) 0 else 1)
    }

    /** Called by the service's monotonic clock. A stretch boundary discards overshoot. */
    fun tick(seconds: Double): TimerState {
        if (done || paused || awaitingStart || seconds <= 0.0) return this
        var delta = seconds
        var next = this
        if (preparing) {
            val used = minOf(delta, preparation)
            next = copy(preparation = (preparation - used).coerceAtLeast(0.0))
            delta -= used
            if (delta <= 0.0) return next
        }
        delta *= speed
        while (delta > 0.0 && !next.done) {
            val used = minOf(delta, next.remaining)
            next = next.copy(remaining = (next.remaining - used).coerceAtLeast(0.0), elapsed = next.elapsed + used)
            delta -= used
            if (next.remaining > 0.0) break
            next = next.advance()
            if (next.stretch) break
        }
        return next
    }
}

/** Builds the phase list from an interval exercise: warmup, then work/rest per round, no trailing rest. */
fun buildPhases(warmupSec: Int, workSec: Int, restSec: Int, rounds: Int): List<Phase> {
    val out = mutableListOf<Phase>()
    if (warmupSec > 0) out += Phase(PhaseKind.WARMUP, warmupSec, 0)
    for (r in 1..rounds) {
        out += Phase(PhaseKind.WORK, workSec, r)
        if (r < rounds && restSec > 0) out += Phase(PhaseKind.REST, restSec, r)
    }
    return out
}
