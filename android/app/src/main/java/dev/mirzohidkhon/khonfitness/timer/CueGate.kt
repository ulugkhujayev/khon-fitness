package dev.mirzohidkhon.khonfitness.timer

/**
 * Lets the speech engine start at most one cue per [gapMs]. A cue that comes sooner waits for the gap,
 * and a newer cue replaces it, so a burst of phase changes speaks only the latest one.
 */
class CueGate(private val gapMs: Long = 300) {
    private var spokenAt = Long.MIN_VALUE
    private var pending: String? = null
    private var talking = false

    /** The cue to speak now, or null when it must wait until [dueAt]. */
    fun offer(cue: String, nowMs: Long): String? { pending = cue; return poll(nowMs) }

    fun poll(nowMs: Long): String? {
        val cue = pending ?: return null
        if (spokenAt != Long.MIN_VALUE && nowMs - spokenAt < gapMs) return null
        pending = null; spokenAt = nowMs; talking = true
        return cue
    }

    fun dueAt(): Long? = pending?.let { spokenAt + gapMs }

    /** Drops the waiting cue. True only when a cue was spoken since the last cancel, so the engine needs stop(). */
    fun cancel(): Boolean { pending = null; return talking.also { talking = false } }
}
