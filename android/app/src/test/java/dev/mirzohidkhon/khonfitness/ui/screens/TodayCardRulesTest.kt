package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Block
import dev.mirzohidkhon.khonfitness.data.BlockExercise
import dev.mirzohidkhon.khonfitness.data.DayOverride
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.KhonRepository
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.Modality
import dev.mirzohidkhon.khonfitness.data.Program
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.WeekPlan
import dev.mirzohidkhon.khonfitness.timer.Phase
import dev.mirzohidkhon.khonfitness.timer.PhaseKind
import dev.mirzohidkhon.khonfitness.timer.TimerState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** UI06 P2: the Today card's lines, the one-day Change, first use, the saved session and midnight. */
class TodayCardRulesTest {
    private val tue = LocalDate.of(2026, 11, 3)
    private val push = Program("push", "Push day")
    private val pull = Program("pull", "Pull day")
    private val bike = Modality("bike", "Bike", 0xFFBF5AF2)
    private val run = Modality("run", "Run", 0xFF30D158)
    private val n44 = Exercise("n44", "Norwegian 4×4", Kind.CARDIO, modalityId = "bike", intervals = true, rounds = 4, workSec = 240, restSec = 180)
    private val z2 = Exercise("z2", "Zone 2 run", Kind.CARDIO, modalityId = "run")
    private val week = listOf(WeekPlan(1, ItemType.PROGRAM, "push"), WeekPlan(2, ItemType.PROGRAM, "push"), WeekPlan(3, ItemType.CARDIO, "z2"), WeekPlan(4, ItemType.REST))
    private fun plan(vararg overrides: DayOverride, weekPlan: List<WeekPlan> = week) =
        KhonRepository.PlanData(weekPlan, overrides.toList(), listOf(push, pull), listOf(n44, z2))

    private val blocks = listOf(Block("b1", "push", 0), Block("b2", "push", 1), Block("x", "pull", 0))
    private val rows = listOf(
        BlockExercise("1", "b1", 0, "bench", sets = 4), BlockExercise("2", "b1", 1, "fly", sets = 3),
        BlockExercise("3", "b2", 0, "bench", sets = 3), BlockExercise("4", "x", 0, "row", sets = 5),
    )

    @Test fun `gym line counts exercises and sets, cardio gives its type and plan`() {
        assertEquals("2 exercises · 10 sets", todayLine(plan().itemFor(tue), blocks, rows, listOf(bike, run)))
        val bikeDay = plan(DayOverride(tue.toString(), ItemType.CARDIO, "n44")).itemFor(tue)
        assertEquals("Bike · 4 × 4 min, 3 min rest · changed for today", todayLine(bikeDay, blocks, rows, listOf(bike, run)))
        assertEquals("Run · Steady", todayLine(plan().itemFor(tue.plusDays(1)), blocks, rows, listOf(bike, run)))
        assertEquals(REST_LINE, todayLine(plan().itemFor(tue.plusDays(2)), blocks, rows, listOf(bike, run)))
    }

    @Test fun `a one-day change marks the gym line, an auto swap does not`() {
        val changed = plan(DayOverride(tue.toString(), ItemType.PROGRAM, "pull")).itemFor(tue)
        assertEquals("1 exercise · 5 sets · changed for today", todayLine(changed, blocks, rows, emptyList()))
        val swapped = plan(DayOverride(tue.toString(), ItemType.PROGRAM, "pull", auto = true)).itemFor(tue)
        assertEquals("1 exercise · 5 sets", todayLine(swapped, blocks, rows, emptyList()))
    }

    @Test fun `change sheet title names today`() {
        assertEquals("Today, Tue Nov 3", changeTitle(tue))
    }

    @Test fun `a pick writes a one-day change, the week plan's item clears it`() {
        val p = plan(DayOverride(tue.toString(), ItemType.REST))
        assertEquals(ChangePick.Override(ItemType.PROGRAM, "pull"), changePick(p, tue, ItemType.PROGRAM, "pull"))
        assertEquals(ChangePick.Override(ItemType.CARDIO, "n44"), changePick(p, tue, ItemType.CARDIO, "n44"))
        assertEquals(ChangePick.BackToPlan, changePick(p, tue, ItemType.PROGRAM, "push"))
        assertEquals(ChangePick.Override(ItemType.REST, null), changePick(plan(), tue, ItemType.REST, null))
        // Thursday is rest in the week plan: Rest goes back to it.
        assertEquals(ChangePick.BackToPlan, changePick(plan(DayOverride("2026-11-05", ItemType.PROGRAM, "push")), tue.plusDays(2), ItemType.REST, null))
    }

    @Test fun `back to plan names the week plan's item, not today's change`() {
        val p = plan(DayOverride(tue.toString(), ItemType.CARDIO, "n44"))
        assertEquals("Norwegian 4×4", p.itemFor(tue).name)
        assertEquals("Push day", weekItem(p, tue).name)
        assertEquals("Rest", weekItem(p, tue.plusDays(2)).name)
    }

    @Test fun `first use is a week plan with nothing on any day and no change today`() {
        assertTrue(hasNoPlan(plan(weekPlan = emptyList()), tue))
        assertTrue(hasNoPlan(plan(weekPlan = listOf(WeekPlan(2, ItemType.REST))), tue))
        assertFalse(hasNoPlan(plan(), tue))
        // Today rests, but the week has workouts: a plain rest day.
        assertFalse(hasNoPlan(plan(), tue.plusDays(2)))
        // A one-day change today is a plan.
        assertFalse(hasNoPlan(plan(DayOverride(tue.toString(), ItemType.PROGRAM, "push"), weekPlan = emptyList()), tue))
        assertFalse(hasNoPlan(plan(DayOverride(tue.toString(), ItemType.REST), weekPlan = emptyList()), tue))
    }

    private fun session(id: String, start: Long, finished: Long?, program: String? = "push", exercise: String? = null, date: String = tue.toString()) =
        Session(id, date, start, finished, if (program != null) ItemType.PROGRAM else ItemType.CARDIO, programId = program, exerciseId = exercise)

    @Test fun `the card shows the last saved session of today's item and counts the others`() {
        val item = plan().itemFor(tue)
        assertEquals(DoneToday(null, 0), doneToday(emptyList(), item, tue))
        val first = session("a", 1_000, 2_000)
        val second = session("b", 3_000, 4_000)
        val other = session("c", 5_000, 6_000, program = null, exercise = "z2")
        val running = session("d", 7_000, null)
        val yesterday = session("e", 1_000, 2_000, date = "2026-11-02")
        assertEquals(DoneToday(first, 0), doneToday(listOf(first, running, yesterday), item, tue))
        assertEquals(DoneToday(second, 1), doneToday(listOf(second, first), item, tue))
        assertEquals(DoneToday(second, 2), doneToday(listOf(first, second, other), item, tue))
        // Only another item's session today: the card still offers today's item.
        assertEquals(DoneToday(null, 0), doneToday(listOf(other), item, tue))
        assertEquals("+1 more today", moreLine(1))
    }

    private fun log(id: String, status: String, slot: Int = 0, block: Int = 0, set: Int = 0, order: Int = 0) =
        SetLog(id, "s", block, false, slot, set, order, "e$block$slot", "Ex $block$slot", 8, 12, 2.5, weightKg = 80.0, reps = 10, status = status)

    @Test fun `running line counts done sets for a program`() {
        val s = session("s", 0, null)
        assertEquals("In progress · 1 of 3 sets", runningLine(s, listOf(log("1", SetStatus.DONE), log("2", SetStatus.PENDING), log("3", SetStatus.SKIPPED)), null, 0))
    }

    @Test fun `running line for cardio gives the timer's state, else the time since the start`() {
        val s = session("c", 0, null, program = null, exercise = "n44")
        assertEquals("In progress · 34 min", runningLine(s, emptyList(), null, 34 * 60_000L))
        val phases = listOf(Phase(PhaseKind.WARMUP, 60, 0), Phase(PhaseKind.WORK, 240, 1), Phase(PhaseKind.REST, 180, 1), Phase(PhaseKind.WORK, 240, 2))
        val t = TimerState("c", "Norwegian 4×4", phases, awaitingStart = false)
        assertEquals("Warm-up", runningLine(s, emptyList(), t, 0))
        assertEquals("Round 1 of 2 · Work", runningLine(s, emptyList(), t.copy(index = 1), 0))
        assertEquals("Paused · Round 1 of 2 · Rest", runningLine(s, emptyList(), t.copy(index = 2, paused = true), 0))
        assertEquals("Timer done", runningLine(s, emptyList(), t.copy(index = 4), 0))
        // The timer of another session says nothing about this one.
        assertEquals("In progress · 34 min", runningLine(s, emptyList(), t.copy(sessionId = "other"), 34 * 60_000L))
    }

    @Test fun `review lists every set by exercise in session order, a set not done as skipped`() {
        val logs = listOf(
            log("4", SetStatus.DONE, slot = 0, block = 1, order = 4), log("1", SetStatus.DONE, slot = 0, set = 0, order = 0),
            log("2", SetStatus.DONE, slot = 1, set = 0, order = 1), log("3", SetStatus.SKIPPED, slot = 0, set = 1, order = 2),
        )
        val ex = reviewExercises(logs)
        assertEquals(listOf(listOf("1", "3"), listOf("2"), listOf("4")), ex.map { sets -> sets.map { it.id } })
        assertEquals("80 kg × 10", reviewSet(logs[1]))
        assertEquals("skipped", reviewSet(logs[3]))
        assertEquals("skipped", reviewSet(logs[1].copy(status = SetStatus.PENDING)))
        assertEquals("82.5 kg × 8", reviewSet(logs[1].copy(weightKg = 82.5, reps = 8)))
    }

    @Test fun `today turns to the new date at midnight`() {
        val zone = ZoneId.of("Asia/Tashkent")
        val before = LocalDateTime.of(2026, 11, 3, 23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        assertEquals(tue, todayOf(before, zone))
        assertEquals(tue.plusDays(1), todayOf(before + 1_000, zone))
    }

    @Test fun `pace needs both time and distance`() {
        assertEquals("5:00 /km", paceLine(1500, 5000))
        assertNull(paceLine(null, 5000))
        assertNull(paceLine(1500, 0))
    }
}
