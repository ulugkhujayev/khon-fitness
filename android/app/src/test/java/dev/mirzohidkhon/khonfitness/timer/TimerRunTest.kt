package dev.mirzohidkhon.khonfitness.timer

import dev.mirzohidkhon.khonfitness.data.FakeClocks
import dev.mirzohidkhon.khonfitness.data.MIN
import dev.mirzohidkhon.khonfitness.data.RoundSpan
import dev.mirzohidkhon.khonfitness.data.TimerStart
import dev.mirzohidkhon.khonfitness.data.at
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The timed run without Android: the service's Start, tick, Pause, Skip, Stop and restore steps on fake clocks.
 * Phases: 3 s countdown, warmup 60, then 4 rounds of work 240 and rest 180, so work of round r starts at
 * 63 + (r - 1) * 420 s after Start and the run ends at 1563 s.
 */
class TimerRunTest {
    private val start = at(18, 0)

    private fun ready() = TimerState("c", "Norwegian 4x4", buildPhases(60, 240, 180, 4), remaining = 60.0, awaitingStart = true, runId = "r1")

    private fun TimerState.started(clocks: FakeClocks, run: Int = 1, timeSec: Int? = null) =
        beginRun(TimerStart(run, timeSec), clocks.wall, clocks.elapsed)

    /** The service loop: [seconds] of true time in ticks of [step]. */
    private fun TimerState.run(clocks: FakeClocks, seconds: Double, step: Double = 0.7): TimerState {
        var s = this
        var left = seconds
        while (left > 1e-9 && !s.done) {
            val dt = minOf(step, left)
            clocks.pass(Math.round(dt * 1000))
            s = s.tickAt(dt, clocks.elapsed)
            left -= dt
        }
        return s
    }

    private fun TimerState.toEnd(clocks: FakeClocks) = run(clocks, 4000.0).also { assertTrue(it.done) }
    private fun TimerState.finish(clocks: FakeClocks) = finishedAt(clocks.wall, clocks.elapsed)
    private fun sec(s: Int) = start + s * 1000L

    @Test fun `C9 a full run has its range and four rounds of work length inside it`() {
        val clocks = FakeClocks(start)
        val result = ready().started(clocks).toEnd(clocks).finish(clocks).runResult()!!
        assertEquals(1, result.run)
        assertEquals(start, result.from)
        assertEquals(sec(1563), result.to)
        assertEquals(1560, result.durationSec)
        assertEquals(listOf(0, 1, 2, 3), result.rounds.map { it.index })
        result.rounds.forEachIndexed { i, r ->
            assertEquals(240_000.0, (r.endedAt - r.startedAt).toDouble(), 1000.0)
            assertEquals(sec(63 + i * 420).toDouble(), r.startedAt.toDouble(), 1000.0)
            assertTrue(r.startedAt >= result.from!! && r.endedAt <= result.to!!)
        }
        result.rounds.zipWithNext().forEach { (a, b) -> assertTrue(a.endedAt <= b.startedAt) }
    }

    @Test fun `C10 a pause stays inside its round, Skip ends work, a Skip in rest starts the next round`() {
        val clocks = FakeClocks(start)
        var s = ready().started(clocks).run(clocks, 600.0)
        assertEquals(2, s.phase.round)
        s = s.copy(paused = true).run(clocks, 30.0).copy(paused = false)
        s = s.run(clocks, 1000.0 + 30 - 630)
        assertEquals(PhaseKind.WORK, s.phase.kind)
        assertEquals(3, s.phase.round)
        s = s.skipAt(clocks.elapsed)
        assertEquals(PhaseKind.REST, s.phase.kind)
        s = s.run(clocks, 10.0).skipAt(clocks.elapsed)
        assertEquals(4, s.phase.round)
        val r = s.toEnd(clocks).finish(clocks).runResult()!!.rounds
        assertEquals(sec(483).toDouble(), r[1].startedAt.toDouble(), 1000.0)
        assertEquals(sec(753).toDouble(), r[1].endedAt.toDouble(), 1000.0)
        assertEquals(sec(933).toDouble(), r[2].startedAt.toDouble(), 1000.0)
        assertEquals(sec(1030), r[2].endedAt)
        assertEquals(sec(1040), r[3].startedAt)
        assertEquals(sec(1280).toDouble(), r[3].endedAt.toDouble(), 1000.0)
    }

    @Test fun `C11 Stop in round 3 ends it there, Stop in warmup keeps the range only`() {
        val clocks = FakeClocks(start)
        val r = ready().started(clocks).run(clocks, 1000.0).finish(clocks).runResult()!!
        assertEquals(listOf(0, 1, 2), r.rounds.map { it.index })
        assertEquals(sec(1000), r.rounds[2].endedAt)
        assertEquals(sec(1000), r.to)
        val warm = FakeClocks(start)
        val w = ready().started(warm).run(warm, 30.0).finish(warm).runResult()!!
        assertTrue(w.rounds.isEmpty())
        assertEquals(start, w.from)
        assertEquals(sec(30), w.to)
    }

    @Test fun `C11 AT3 time on Ready is no part of the run, and Stop on Ready stores nothing`() {
        val clocks = FakeClocks(start)
        val waited = ready().run(clocks, 40 * 60.0)
        assertTrue(waited.awaitingStart)
        assertNull(waited.finish(clocks).runResult())
        val r = waited.started(clocks).run(clocks, 20 * 60.0).finish(clocks).runResult()!!
        assertEquals(at(18, 40), r.from)
        assertEquals(at(19, 0), r.to)
    }

    @Test fun `C12 a restore on the same boot gives each crossed boundary its own time`() {
        val clocks = FakeClocks(start)
        val inRound1 = ready().started(clocks).run(clocks, 100.0)
        val snap = TimerSnapshot.decode(TimerSnapshot(inRound1, clocks.wall, clocks.elapsed).encode())!!
        clocks.pass(10 * MIN)
        val restored = snap.advancedAt(clocks.wall, clocks.elapsed)
        assertEquals(2, restored.phase.round)
        assertTrue(restored.clock!!.rounds[0].end != null)
        val r = restored.toEnd(clocks).finish(clocks).runResult()!!
        assertNotNull(r.from)
        assertEquals(sec(303).toDouble(), r.rounds[0].endedAt.toDouble(), 1000.0)
        assertEquals(sec(483).toDouble(), r.rounds[1].startedAt.toDouble(), 1000.0)
        assertEquals(sec(1563).toDouble(), r.to!!.toDouble(), 1000.0)
    }

    @Test fun `C12 a paused snapshot consumes no phase time and its round holds the pause`() {
        val clocks = FakeClocks(start)
        val paused = ready().started(clocks).run(clocks, 100.0).copy(paused = true)
        val snap = TimerSnapshot(paused, clocks.wall, clocks.elapsed)
        clocks.pass(10 * MIN)
        val restored = snap.advancedAt(clocks.wall, clocks.elapsed)
        assertEquals(paused.remaining, restored.remaining, 1e-9)
        val r = restored.copy(paused = false).toEnd(clocks).finish(clocks).runResult()!!
        assertEquals(sec(63).toDouble(), r.rounds[0].startedAt.toDouble(), 1000.0)
        assertEquals(sec(100 + 600 + 203).toDouble(), r.rounds[0].endedAt.toDouble(), 1000.0)
    }

    @Test fun `X7 a trusted restore places every boundary from the anchor and the phase lengths, whatever the allowed drift`() {
        for (drift in listOf(1_500L, -1_500L)) {
            val clocks = FakeClocks(start)
            val begun = ready().started(clocks)
            val snap = TimerSnapshot.decode(TimerSnapshot(begun, clocks.wall, clocks.elapsed).encode())!!
            clocks.pass(10 * MIN)
            clocks.moveWall(drift)
            val restored = snap.advancedAt(clocks.wall, clocks.elapsed)
            assertFalse("$drift", restored.clock!!.broken)
            assertEquals("$drift", 2, restored.phase.round)
            val r = restored.finishedAt(clocks.wall, clocks.elapsed).runResult()!!
            assertEquals("$drift", start, r.from)
            assertEquals("$drift", sec(600), r.to)
            assertEquals("$drift", listOf(RoundSpan(0, sec(63), sec(303)), RoundSpan(1, sec(483), sec(600))), r.rounds)
        }
    }

    @Test fun `X7 a snapshot without the elapsed value advances by the wall clock and its run is untrusted`() {
        val clocks = FakeClocks(start)
        val begun = ready().started(clocks)
        val old = TimerSnapshot.decode(TimerSnapshot(begun, clocks.wall).encode().replace(",\"savedElapsed\":${clocks.elapsed}", ""))!!
        assertNull(old.savedElapsed)
        clocks.pass(10 * MIN)
        clocks.moveWall(1_500L)
        val restored = old.advancedAt(clocks.wall, clocks.elapsed)
        assertTrue(restored.clock!!.broken)
        assertEquals(601.5 - 3.0, restored.elapsed, 1e-6)
        val r = restored.finishedAt(clocks.wall, clocks.elapsed).runResult()!!
        assertEquals(1, r.run)
        assertNull(r.from)
        assertNull(r.to)
        assertTrue(r.rounds.isEmpty())
    }

    @Test fun `Y4 a restore at an elapsed value below the saved one is untrusted for good, also above the anchor`() {
        val clocks = FakeClocks(start, elapsed = 9_000L)
        val begun = ready().started(clocks)
        val saved = begun.run(clocks, 1.0)
        assertEquals(10_000L, saved.instant)
        val snap = TimerSnapshot.decode(TimerSnapshot(saved, clocks.wall, 10_000L).encode())!!
        val restoreWall = start + 1_500L
        val restoreElapsed = 9_500L
        assertTrue(dev.mirzohidkhon.khonfitness.data.inStep(begun.clock!!.anchor, restoreWall, restoreElapsed))
        val restored = snap.advancedAt(restoreWall, restoreElapsed)
        assertTrue(restored.clock!!.broken)
        assertEquals(2.0 - 0.5, restored.preparation, 1e-9)
        assertEquals(restoreElapsed, restored.instant)
        val later = FakeClocks(restoreWall, restoreElapsed)
        val r = restored.toEnd(later).finish(later).runResult()!!
        assertEquals(1, r.run)
        assertEquals(1560, r.durationSec)
        assertNull(r.from)
        assertNull(r.to)
        assertTrue(r.rounds.isEmpty())
    }

    @Test fun `Y5 a restore stands at its own instant, so a second restore from its save loses no time`() {
        val clocks = FakeClocks(start)
        val inRound1 = ready().started(clocks).run(clocks, 100.0)
        val first = TimerSnapshot(inRound1, clocks.wall, inRound1.instant!!)
        clocks.pass(10 * MIN)
        val once = first.advancedAt(clocks.wall, clocks.elapsed)
        assertEquals(clocks.elapsed, once.instant)
        val second = TimerSnapshot(once, clocks.wall, once.instant!!)
        clocks.pass(1_234L)
        val twice = second.advancedAt(clocks.wall, clocks.elapsed)
        val direct = first.advancedAt(clocks.wall, clocks.elapsed)
        assertEquals(direct.index, twice.index)
        assertEquals(direct.remaining, twice.remaining, 1e-6)
        assertEquals(direct.clock, twice.clock)
        val r = twice.toEnd(clocks).finish(clocks).runResult()!!
        assertEquals(listOf(RoundSpan(0, sec(63), sec(303)), RoundSpan(1, sec(483), sec(723))), r.rounds.take(2))
    }

    @Test fun `C13 a wall clock change, a jump in a pause, and a reboot make the run untrusted for good`() {
        val live = FakeClocks(start)
        val moved = ready().started(live).run(live, 600.0).also { live.moveWall(60 * MIN) }.toEnd(live).finish(live)
        assertFalse(moved.trusted)
        moved.runResult()!!.let { assertNull(it.from); assertNull(it.to); assertTrue(it.rounds.isEmpty()); assertEquals(1560, it.durationSec) }

        val pause = FakeClocks(start)
        val jumped = ready().started(pause).run(pause, 600.0).copy(paused = true).also { pause.moveWall(-5 * MIN) }
            .run(pause, 60.0).copy(paused = false).toEnd(pause).finish(pause)
        assertNull(jumped.runResult()!!.from)

        val boot = FakeClocks(start)
        val beforeReboot = ready().started(boot).run(boot, 100.0)
        val snap = TimerSnapshot(beforeReboot, boot.wall, boot.elapsed)
        val afterReboot = FakeClocks(boot.wall + 2 * MIN, elapsed = 30_000L)
        val restored = snap.advancedAt(afterReboot.wall, afterReboot.elapsed)
        assertTrue(restored.clock!!.broken)
        assertNull(restored.toEnd(afterReboot).finish(afterReboot).runResult()!!.from)

        val drift = FakeClocks(start)
        val small = ready().started(drift).run(drift, 600.0).also { drift.moveWall(1000) }.toEnd(drift).finish(drift)
        assertTrue(small.trusted)
        assertNotNull(small.runResult()!!.from)
    }

    @Test fun `C13 C16b a snapshot of the old version decodes and gives a run without a number or clock`() {
        val old = """{"state":{"sessionId":"c","title":"Norwegian 4x4","phases":[{"kind":"WORK","seconds":240,"round":1},""" +
            """{"kind":"REST","seconds":180,"round":1},{"kind":"WORK","seconds":240,"round":2}],"index":0,"remaining":100.0,""" +
            """"elapsed":140.0,"awaitingStart":false,"startedAt":1763600000000},"savedAt":1763600140000}"""
        val snap = TimerSnapshot.decode(old)!!
        assertNull(snap.state.clock)
        assertEquals("", snap.state.runId)
        val ended = snap.advancedAt(1763600140000 + 30 * MIN, 9_000_000L)
        assertTrue(ended.done)
        val r = ended.finishedAt(1763600140000 + 30 * MIN, 9_000_000L).runResult()!!
        assertNull(r.run)
        assertNull(r.from)
        assertTrue(r.rounds.isEmpty())
        assertEquals(660, r.durationSec)

        val oldReady = TimerSnapshot.decode(old.replace("\"awaitingStart\":false", "\"awaitingStart\":true"))!!
        assertNull(oldReady.state.finishedAt(0, 0).runResult())
    }

    @Test fun `C16 an ended run retried after a reboot keeps the times it verified at its end`() {
        val clocks = FakeClocks(start)
        val ended = ready().started(clocks).toEnd(clocks).finish(clocks)
        val snap = TimerSnapshot.decode(TimerSnapshot(ended, clocks.wall).encode())!!
        val rebooted = FakeClocks(clocks.wall + 60 * MIN, elapsed = 20_000L)
        val retried = snap.advancedAt(rebooted.wall, rebooted.elapsed).finish(rebooted)
        assertEquals(ended, retried)
        assertEquals(start, retried.runResult()!!.from)
    }

    @Test fun `the timed tick moves the timer exactly as the plain tick`() {
        val clocks = FakeClocks(start)
        var timed = ready().started(clocks)
        var plain = timed
        repeat(3000) {
            clocks.pass(700)
            timed = timed.tickAt(0.7, clocks.elapsed)
            plain = plain.tick(0.7)
            assertEquals(plain.copy(clock = null, instant = null), timed.copy(clock = null, instant = null))
            if (!timed.done) assertEquals(clocks.elapsed, timed.instant)
        }
    }

    @Test fun `a stretch run and a run without a number take no clock`() {
        val clocks = FakeClocks(start)
        val stretch = TimerState("routine:s", "Mobility", listOf(Phase(PhaseKind.WORK, 30, 0)), remaining = 30.0, stretch = true)
        assertNull(stretch.beginRun(TimerStart(1, null), clocks.wall, clocks.elapsed).clock)
        assertNull(stretch.beginRun(null, 0, 0).runResult())
        val noNumber = ready().beginRun(null, clocks.wall, clocks.elapsed)
        assertFalse(noNumber.awaitingStart)
        assertNull(noNumber.clock)
    }
}
