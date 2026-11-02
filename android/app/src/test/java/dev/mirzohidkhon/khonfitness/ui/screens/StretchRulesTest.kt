package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.data.StretchMode
import dev.mirzohidkhon.khonfitness.timer.Phase
import dev.mirzohidkhon.khonfitness.timer.PhaseKind
import dev.mirzohidkhon.khonfitness.timer.TimerState
import dev.mirzohidkhon.khonfitness.ui.components.RunSegment
import dev.mirzohidkhon.khonfitness.ui.components.runSegments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StretchRulesTest {
    // ---- lists

    @Test fun `routine summary counts minutes and stretches`() {
        val stretches = listOf(Stretch("a", "A", sided = true, seconds = 30), Stretch("b", "B", seconds = 45))
        val rows = listOf(RoutineStretch("1", "r", "a", 0), RoutineStretch("2", "r", "b", 1), RoutineStretch("3", "other", "b", 0))
        assertEquals("2 min · 2 stretches", routineSummary("r", rows, stretches)) // 30 + 30 + 45 = 105 s
        assertEquals("1 min · 1 stretch", routineSummary("other", rows, stretches))
        assertEquals("0 min · 0 stretches", routineSummary("none", rows, stretches))
    }

    @Test fun `library line names sides and length`() {
        assertEquals("per side · 45 s", stretchLine(Stretch("a", "A", sided = true, seconds = 45)))
        assertEquals("10 reps", stretchLine(Stretch("a", "A", mode = StretchMode.REPS, reps = 10)))
    }

    // ---- player (contract row 7)

    private val left = Phase(PhaseKind.WORK, 40, 0, label = "Hip flexor", side = "Left")
    private val reps = Phase(PhaseKind.WORK, 60, 0, label = "Cat cow", reps = 10)
    private val hold = Phase(PhaseKind.WORK, 45, 0, label = "Plow")
    private fun run(index: Int = 0, vararg phases: Phase = arrayOf(left, reps, hold)) =
        TimerState("r:1", "Daily", phases.toList(), index = index, remaining = phases[index].seconds.toDouble(), stretch = true)

    @Test fun `the line under the name carries side, hold or reps, and length`() {
        assertEquals("Left · Hold · 0:40", sideLine(left))
        assertEquals("10 reps · 1:00", sideLine(reps))
        assertEquals("Hold · 0:45", sideLine(hold))
    }

    @Test fun `the ready screen has no status line and the running states say what happens`() {
        val ready = run()
        assertNull(playerStatus(ready, left))
        val going = ready.begin()
        assertEquals("Get ready", playerStatus(going, left))
        val holding = going.copy(preparation = 0.0)
        assertEquals("Hold", playerStatus(holding, left))
        assertEquals("Move", playerStatus(holding, reps))
        assertEquals("Paused", playerStatus(holding.copy(paused = true), left))
    }

    @Test fun `the bottom button names what comes`() {
        assertEquals("Start left side", playerAction(run(), left))
        assertEquals("Start stretch", playerAction(run(0, hold, reps), hold))
        assertEquals("Start next stretch", playerAction(run(1), reps))
        val running = run().begin().copy(preparation = 0.0)
        assertEquals("Pause", playerAction(running, left))
        assertEquals("Resume", playerAction(running.copy(paused = true), left))
    }

    @Test fun `progress text counts the place and the time left`() {
        assertEquals("1 of 3 · 2:25 remaining", playerProgress(run())) // 40 + 60 + 45
        assertEquals("3 of 3 · 0:45 remaining", playerProgress(run(2)))
    }

    @Test fun `one segment per stretch and side, done, current and ahead`() {
        assertEquals(listOf(RunSegment.DONE, RunSegment.DONE, RunSegment.CURRENT, RunSegment.AHEAD), runSegments(4, 2))
        assertEquals(listOf(RunSegment.CURRENT, RunSegment.AHEAD), runSegments(2, 0))
        assertEquals(emptyList<RunSegment>(), runSegments(0, 0))
    }
}
