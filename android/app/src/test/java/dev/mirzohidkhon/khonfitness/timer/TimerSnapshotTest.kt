package dev.mirzohidkhon.khonfitness.timer

import org.junit.Assert.*
import org.junit.Test

class TimerSnapshotTest {
    private val t0 = 1_760_000_000_000L
    private fun sec(s: Int) = t0 + s * 1000L
    // Warm-up 60, then work 240 / rest 180 for 4 rounds: 60 + 4*240 + 3*180 = 1560 s.
    private fun cardio() = TimerState("cardio", "Norwegian 4x4", buildPhases(60, 240, 180, 4), remaining = 60.0, awaitingStart = false)
    private fun stretch() = TimerState("routine:test", "Mobility", listOf(
        Phase(PhaseKind.WORK, 30, 0, side = "Left"),
        Phase(PhaseKind.WORK, 30, 0, side = "Right"),
    ), remaining = 30.0, stretch = true)

    @Test fun `a snapshot expires an hour after the timer would have ended`() {
        val snap = TimerSnapshot(cardio(), sec(0))
        assertFalse(snap.expired(sec(1560 + 3600)))
        assertTrue(snap.expired(sec(1560 + 3601)))
        val stretchSnap = TimerSnapshot(stretch(), sec(0))
        assertFalse(stretchSnap.expired(sec(60 + 3600)))
        assertTrue(stretchSnap.expired(sec(60 + 3601)))
    }

    @Test fun `a finished snapshot never expires, so its result is still written`() {
        val done = cardio().let { it.copy(index = it.phases.size, remaining = 0.0) }
        assertFalse(TimerSnapshot(done, sec(0)).expired(sec(30 * 24 * 3600)))
    }

    @Test fun `running phase counts down by the wall-clock gap`() {
        val s = TimerSnapshot(cardio().tick(20.0), sec(0)).advanced(sec(15))
        assertEquals(0, s.index)
        assertEquals(25.0, s.remaining, 1e-9)
        assertEquals(35.0, s.elapsed, 1e-9)
    }

    @Test fun `paused and unstarted timers do not move`() {
        val paused = cardio().tick(20.0).copy(paused = true)
        assertEquals(paused, TimerSnapshot(paused, sec(0)).advanced(sec(3600)))
        val ready = cardio().copy(awaitingStart = true)
        assertEquals(ready, TimerSnapshot(ready, sec(0)).advanced(sec(3600)))
    }

    @Test fun `cardio crosses several phases like the live timer`() {
        val s = TimerSnapshot(cardio(), sec(0)).advanced(sec(60 + 240 + 180 + 30))
        assertEquals(3, s.index)
        assertEquals(PhaseKind.WORK, s.phase.kind)
        assertEquals(2, s.phase.round)
        assertEquals(210.0, s.remaining, 1e-9)
        assertEquals(510.0, s.elapsed, 1e-9)
        assertFalse(s.awaitingStart)
        assertEquals(s, cardio().tick(510.0))
    }

    @Test fun `countdown then stretch stops at the next side and waits for Start`() {
        val begun = stretch().begin()
        val s = TimerSnapshot(begun, sec(0)).advanced(sec(600))
        assertEquals(1, s.index)
        assertTrue(s.awaitingStart)
        assertFalse(s.preparing)
        assertEquals(30.0, s.remaining, 1e-9)
        assertEquals(30.0, s.elapsed, 1e-9)
        assertEquals(1, s.completed)
        assertFalse(s.done)
        assertEquals(s, TimerSnapshot(s, sec(600)).advanced(sec(7200)))
    }

    @Test fun `countdown still running is consumed before exercise time`() {
        val s = TimerSnapshot(stretch().begin(), sec(0)).advanced(sec(2))
        assertEquals(1.0, s.preparation, 1e-9)
        assertEquals(30.0, s.remaining, 1e-9)
        assertEquals(0.0, s.elapsed, 1e-9)
    }

    @Test fun `timers that ran out while the process was dead finish`() {
        val c = TimerSnapshot(cardio(), sec(0)).advanced(sec(5000))
        assertTrue(c.done)
        assertEquals(1560.0, c.elapsed, 1e-9)
        val lastSide = stretch().begin().tick(3.0).tick(30.0).begin()
        val s = TimerSnapshot(lastSide, sec(0)).advanced(sec(120))
        assertTrue(s.done)
        assertEquals(2, s.completed)
        assertEquals(60.0, s.elapsed, 1e-9)
    }

    @Test fun `a clock set backwards never rewinds the timer`() {
        val s = cardio().tick(20.0)
        assertEquals(s, TimerSnapshot(s, sec(100)).advanced(sec(0)))
    }

    @Test fun `snapshot survives a JSON round trip and rejects junk`() {
        val snap = TimerSnapshot(stretch().begin().tick(1.5).copy(skipped = 1, speed = 2.0, startedAt = t0), sec(4))
        assertEquals(snap, TimerSnapshot.decode(snap.encode()))
        val ready = TimerSnapshot(stretch(), sec(0))
        assertTrue(TimerSnapshot.decode(ready.encode())!!.state.awaitingStart)
        assertNull(TimerSnapshot.decode("{not json"))
        assertNull(TimerSnapshot.decode(null))
        assertNull(TimerSnapshot.decode(TimerSnapshot(cardio().copy(phases = emptyList()), sec(0)).encode()))
    }
}
