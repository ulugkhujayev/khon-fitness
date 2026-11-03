package dev.mirzohidkhon.khonfitness.timer

import org.junit.Assert.*
import org.junit.Test

class TimerStateTest {
    private fun stretch() = TimerState("routine:test", "Mobility", listOf(
        Phase(PhaseKind.WORK, 5, 0, side = "Left"),
        Phase(PhaseKind.WORK, 5, 0, side = "Right"),
        Phase(PhaseKind.WORK, 5, 0),
    ), remaining = 5.0, stretch = true)

    @Test fun `opening a stretch waits without consuming time`() {
        val s = stretch()
        assertTrue(s.awaitingStart)
        assertEquals(s, s.tick(30.0))
    }

    @Test fun `start counts three two one before consuming exercise time`() {
        var s = stretch().begin()
        assertEquals(3.0, s.preparation, 0.0)
        for (n in listOf(2.0, 1.0, 0.0)) {
            s = s.tick(1.0)
            assertEquals(n, s.preparation, 0.0)
            assertEquals(5.0, s.remaining, 0.0)
            assertEquals(0.0, s.elapsed, 0.0)
        }
        assertEquals(4.0, s.tick(1.0).remaining, 0.0)
    }

    @Test fun `side changes and following exercises wait for a tap`() {
        var s = stretch().begin().tick(3.0).tick(5.5)
        assertEquals(1, s.index)
        assertEquals(1, s.completed)
        assertTrue(s.awaitingStart)
        assertEquals(5.0, s.remaining, 0.0)
        assertEquals(s, s.tick(120.0))
        s = s.begin().tick(3.0).tick(5.0)
        assertEquals(2, s.index)
        assertTrue(s.awaitingStart)
    }

    @Test fun `pause freezes preparation and resume preserves it`() {
        val s = stretch().begin().tick(1.0).copy(paused = true)
        assertEquals(s, s.tick(20.0))
        assertEquals(1.0, s.copy(paused = false).tick(1.0).preparation, 0.0)
    }

    @Test fun `double start does not reset countdown or exercise`() {
        val preparing = stretch().begin().tick(1.0)
        assertEquals(preparing, preparing.begin())
        val running = preparing.tick(2.0).tick(1.0)
        assertEquals(running, running.begin())
    }

    @Test fun `skip never starts another stretch or inflates completed count`() {
        val s = stretch().begin().tick(3.0).tick(2.0).skip()
        assertTrue(s.awaitingStart)
        assertEquals(0, s.completed)
        assertEquals(1, s.skipped)
        assertEquals(2.0, s.elapsed, 0.0)
        assertEquals(10.0, s.workRemaining, 0.0)
    }

    @Test fun `only completed work counts when stopping early`() {
        val s = stretch().begin().tick(3.0).tick(5.0).begin().tick(3.0).tick(2.0)
        assertEquals(1, s.completed)
        assertEquals(7.0, s.elapsed, 0.0)
    }

    @Test fun `last stretch completes once with no trailing preparation`() {
        var s = stretch()
        repeat(3) { s = s.begin().tick(3.0).tick(5.0) }
        assertTrue(s.done)
        assertEquals(3, s.completed)
        assertEquals(s, s.tick(10.0))
        assertEquals(s, s.skip())
    }

    @Test fun `cardio phases still advance automatically and account for delayed ticks`() {
        var s = TimerState("cardio", "Intervals", buildPhases(0, 5, 2, 2), remaining = 5.0)
        assertFalse(s.awaitingStart)
        s = s.tick(6.0)
        assertEquals(PhaseKind.REST, s.phase.kind)
        assertEquals(1.0, s.remaining, 0.0)
        s = s.tick(8.0)
        assertTrue(s.done)
        assertEquals(12.0, s.elapsed, 0.0)
    }

    @Test fun `test speed does not accelerate the three second preparation`() {
        val s = stretch().copy(speed = 20.0).begin().tick(1.0)
        assertEquals(2.0, s.preparation, 0.0)
        assertEquals(0.0, s.elapsed, 0.0)
    }

    /** UI06 P5 row 4: Skip then Back returns to the stretch skipped, its timer reset, and takes the skip back. */
    @Test fun `back after skip returns to the stretch before with its timer reset`() {
        val nine = TimerState("routine:nine", "Daily", (1..9).map { Phase(PhaseKind.WORK, 40, 0, label = "S$it") }, remaining = 40.0, stretch = true)
        var s = nine.skip().skip()
        assertEquals(2, s.index)
        s = s.begin().tick(3.0).tick(10.0)
        assertEquals(30.0, s.remaining, 0.0)
        s = s.skip()
        assertEquals(3, s.index)
        assertEquals(3, s.skipped)
        s = s.back()
        assertEquals("S3", s.phase.label)
        assertEquals(40.0, s.remaining, 0.0)
        assertTrue(s.awaitingStart)
        assertEquals(0.0, s.preparation, 0.0)
        assertEquals(2, s.skipped)
        assertEquals(10.0, s.elapsed, 0.0)
    }

    @Test fun `back takes back a completed stretch and is off on the first`() {
        val first = stretch()
        assertFalse(first.canGoBack)
        assertEquals(first, first.back())
        var s = first.begin().tick(3.0).tick(5.0)
        assertEquals(1, s.index)
        assertEquals(1, s.completed)
        assertTrue(s.canGoBack)
        s = s.back()
        assertEquals(0, s.index)
        assertEquals(0, s.completed)
        assertEquals(5.0, s.remaining, 0.0)
        assertFalse(s.canGoBack)
        // An interval timer has no Back.
        val timer = TimerState("t", "Run", buildPhases(0, 60, 30, 2), index = 1, remaining = 30.0)
        assertFalse(timer.canGoBack)
        assertEquals(timer, timer.back())
    }
}
