package dev.mirzohidkhon.khonfitness.data


/** The two phone clocks: the wall clock (epoch ms, the owner can change it) and elapsed realtime (ms since boot). */
interface Clocks {
    fun wallMs(): Long
    fun elapsedMs(): Long
}

object SystemClocks : Clocks {
    override fun wallMs(): Long = System.currentTimeMillis()
    override fun elapsedMs(): Long = android.os.SystemClock.elapsedRealtime()
}

/** A wall time and an elapsed-realtime value taken at one instant. */
data class Anchor(val wallMs: Long, val elapsedMs: Long) {
    /** The wall time at [elapsedMs] when the wall clock has not moved since the anchor. */
    fun wallAt(elapsedMs: Long): Long = wallMs + (elapsedMs - this.elapsedMs)
}

const val CLOCK_STEP_MS = 2_000L

/**
 * Whether the wall clock and elapsed realtime moved by the same amount since [anchor], within [CLOCK_STEP_MS]. A missing
 * anchor or an elapsed value below the anchor's (a reboot) is not in step.
 */
fun inStep(anchor: Anchor?, wallMs: Long, elapsedMs: Long): Boolean {
    if (anchor == null || elapsedMs < anchor.elapsedMs) return false
    val drift = try {
        Math.subtractExact(Math.subtractExact(wallMs, anchor.wallMs), Math.subtractExact(elapsedMs, anchor.elapsedMs))
    } catch (e: ArithmeticException) {
        return false
    }
    return drift in -CLOCK_STEP_MS..CLOCK_STEP_MS
}

/** [wallMs] as a clock fact: itself when the clocks are in step since [anchor], else null. */
fun verifiedWall(anchor: Anchor?, wallMs: Long, elapsedMs: Long): Long? = if (inStep(anchor, wallMs, elapsedMs)) wallMs else null

/** The anchor a gym session took at its start, or null for a session without one. */
val Session.anchor: Anchor? get() = startedElapsed?.let { Anchor(startedAt, it) }
