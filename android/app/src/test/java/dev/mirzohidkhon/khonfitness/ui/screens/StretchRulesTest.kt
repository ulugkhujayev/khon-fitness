package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.runtime.saveable.SaverScope
import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.data.StretchMode
import dev.mirzohidkhon.khonfitness.data.StretchRoutine
import dev.mirzohidkhon.khonfitness.timer.Phase
import dev.mirzohidkhon.khonfitness.timer.PhaseKind
import dev.mirzohidkhon.khonfitness.timer.TimerState
import dev.mirzohidkhon.khonfitness.ui.components.RunSegment
import dev.mirzohidkhon.khonfitness.ui.components.runSegments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StretchRulesTest {
    // ---- routine draft (contract rows 3, 4, 16; attack cases E4, E5)

    @Test fun `a new routine gets its id once, when the editor opens, and stays a draft until the list holds it`() {
        val d = RoutineDraft.open("new")
        assertTrue(d.isNew)
        assertTrue(d.id.isNotEmpty())
        assertTrue(d.isDraft(stored = null))
        assertFalse(d.isDraft(StretchRoutine(d.id, "Evening")))
        assertTrue(RoutineDraft.open("new").id != d.id)
    }

    @Test fun `Done on a draft creates it with its name and Active choice, and on a stored routine creates nothing`() {
        val d = RoutineDraft(isNew = true, id = "r1")
        d.name = "  Evening "
        d.activeChoice = true
        assertEquals(StretchRoutine("r1", "Evening", active = true), d.done(stored = null, noRoutines = false))
        assertNull(d.done(StretchRoutine("r1", "Evening", active = true), noRoutines = false))
    }

    @Test fun `a blank name saves as New routine and Active defaults on only without routines`() {
        assertEquals(StretchRoutine("r1", NEW_ROUTINE_NAME, active = true), RoutineDraft(true, "r1").also { it.name = " " }.done(null, noRoutines = true))
        assertEquals(StretchRoutine("r1", NEW_ROUTINE_NAME, active = false), RoutineDraft(true, "r1").done(null, noRoutines = false))
        assertTrue(RoutineDraft(true, "r1").shownActive(null, noRoutines = true))
        assertFalse(RoutineDraft(true, "r1").shownActive(null, noRoutines = false))
    }

    @Test fun `the first stretch creates the routine under the draft's id, and a pick before the list holds it asks the same`() {
        val d = RoutineDraft(isNew = true, id = "r1")
        d.name = "Evening"
        val first = d.forFirstStretch(stored = null, noRoutines = false)
        assertEquals(StretchRoutine("r1", "Evening", active = false), first)
        // Nothing local marks it saved: a second pick or Done while that create is pending asks for the same id, which
        // the database then ignores when the first create stored it.
        assertEquals(first, d.forFirstStretch(stored = null, noRoutines = false))
        assertEquals(first, d.done(stored = null, noRoutines = false))
        assertNull(d.forFirstStretch(first, noRoutines = false))
    }

    @Test fun `a refused or lost create leaves a draft that Done saves under the same id`() {
        val d = RoutineDraft(isNew = true, id = "r1").apply { name = "Evening" }
        d.forFirstStretch(stored = null, noRoutines = false)
        val back = d.restored()
        assertTrue(back.isDraft(stored = null))
        assertEquals(StretchRoutine("r1", "Evening", active = false), back.done(stored = null, noRoutines = false))
    }

    @Test fun `a restored draft keeps its id, typed name and Active choice, and is saved once the list holds the id`() {
        val d = RoutineDraft.open("new").apply { name = "Evening"; activeChoice = false }
        val back = d.restored()
        assertEquals(d.id, back.id)
        assertTrue(back.isNew)
        assertEquals(StretchRoutine(d.id, "Evening", active = false), back.done(null, noRoutines = true))
        assertNull(back.done(StretchRoutine(d.id, "Evening"), noRoutines = true))
    }

    @Test fun `an existing routine is never a draft and its Active follows the stored row`() {
        val d = RoutineDraft.open("a")
        assertFalse(d.isNew)
        assertEquals("a", d.id)
        val stored = StretchRoutine("a", "Daily", active = true)
        assertFalse(d.isDraft(stored))
        assertNull(d.done(stored, noRoutines = true))
        assertNull(d.forFirstStretch(stored, noRoutines = true))
        assertTrue(d.shownActive(stored, noRoutines = false))
        assertFalse(d.isDraft(stored = null)) // still loading: the screen shows nothing until the row loads
    }

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

    private fun RoutineDraft.restored(): RoutineDraft {
        val saved = with(RoutineDraft.Saver) { SaverScope { true }.save(this@restored) }!!
        return RoutineDraft.Saver.restore(saved)!!
    }
}
