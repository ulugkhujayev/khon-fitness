package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.*
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PlanTabRulesTest {
    private val push = Program("push", "Push", active = true)
    private val pull = Program("pull", "Pull", sortOrder = 1)
    private val run = Exercise("z2run", "Zone 2 run", Kind.CARDIO, modalityId = "run")
    private val bike = Exercise("nor", "Norwegian 4x4", Kind.CARDIO, modalityId = "bike", intervals = true, rounds = 4, workSec = 240, restSec = 180)
    private val types = listOf(Modality("run", "Run", 0xFF7CE0A8), Modality("bike", "Bike", 0xFFB39DFF))
    private val wednesday = LocalDate.of(2026, 11, 4)

    private fun plan(week: List<WeekPlan>, overrides: List<DayOverride> = emptyList()) =
        KhonRepository.PlanData(week, overrides, listOf(push, pull), listOf(run, bike))

    @Test fun `a program row says its weekdays from the template, or that it is not in the week`() {
        val p = plan(listOf(WeekPlan(4, ItemType.PROGRAM, "push"), WeekPlan(1, ItemType.PROGRAM, "push"), WeekPlan(2, ItemType.CARDIO, "z2run")))
        assertEquals("4 blocks · 22 sets · Every Mon, Thu", programLine(4, 22, p.weekdaysOf(ItemType.PROGRAM, "push")))
        assertEquals("1 block · 1 set · Not in the week", programLine(1, 1, p.weekdaysOf(ItemType.PROGRAM, "pull")))
        assertEquals("Every Tue", everyLine(p.weekdaysOf(ItemType.CARDIO, "z2run")))
    }

    @Test fun `a cardio row says its plan and its weekdays`() {
        assertEquals("4 × 4 min, 3 min rest · Every Fri", cardioLine(bike, listOf(5)))
        assertEquals("8 × 20 s · Not in the week", cardioLine(bike.copy(rounds = 8, workSec = 20, restSec = 0), emptyList()))
        assertEquals("Steady · Every Wed, Sun", cardioLine(run, listOf(3, 7)))
    }

    @Test fun `the week grid runs Monday to Sunday, marks today, and shows a cardio workout by its name`() {
        val cells = plan(listOf(WeekPlan(1, ItemType.PROGRAM, "push"), WeekPlan(3, ItemType.CARDIO, "z2run"), WeekPlan(5, ItemType.CARDIO, "nor"))).weekCells(wednesday)
        assertEquals((2..8).map { LocalDate.of(2026, 11, it) }, cells.map { it.date })
        assertEquals(listOf("Push", "Rest", "Zone 2 run", "Rest", "Norwegian 4x4", "Rest", "Rest"), cells.map { it.name })
        assertEquals(listOf(false, true, false, true, false, true, true), cells.map { it.rest })
        assertEquals(listOf(wednesday), cells.filter { it.today }.map { it.date })
    }

    @Test fun `a one-day change shows its item and a mark, an automatic freeze shows no mark`() {
        val cells = plan(
            listOf(WeekPlan(1, ItemType.PROGRAM, "push"), WeekPlan(5, ItemType.PROGRAM, "push")),
            listOf(DayOverride("2026-11-06", ItemType.PROGRAM, "pull"), DayOverride("2026-11-02", ItemType.PROGRAM, "push", auto = true)),
        ).weekCells(wednesday)
        assertEquals("Pull", cells[4].name)
        assertEquals(listOf(false, false, false, false, true, false, false), cells.map { it.changed })
        assertEquals("Friday, Pull. Changed for this day only.", cells[4].spoken("Pull"))
        assertEquals("Wednesday, Rest. Today.", cells[2].spoken("Rest"))
    }

    @Test fun `a one-day change between two cardio workouts of one type shows the other workout's name`() {
        val tempo = Exercise("tempo", "Tempo run", Kind.CARDIO, modalityId = "run")
        val cells = KhonRepository.PlanData(
            listOf(WeekPlan(3, ItemType.CARDIO, "z2run"), WeekPlan(5, ItemType.CARDIO, "z2run")),
            listOf(DayOverride("2026-11-06", ItemType.CARDIO, "tempo")), listOf(push), listOf(run, tempo),
        ).weekCells(wednesday)
        assertEquals("Zone 2 run", cells[2].name)
        assertEquals("Tempo run", cells[4].name)
        assertEquals(listOf(false, false, false, false, true, false, false), cells.map { it.changed })
    }

    @Test fun `with no plan every day is rest`() {
        val cells = KhonRepository.PlanData(emptyList(), emptyList(), emptyList(), emptyList()).weekCells(wednesday)
        assertEquals(List(7) { "Rest" }, cells.map { it.name })
    }

    @Test fun `the exercise list groups live strength exercises by muscle and keeps the archived apart`() {
        val list = listOf(
            Exercise("row", "Barbell row", Kind.STRENGTH, primaryMuscle = "back"),
            Exercise("bench", "Bench press", Kind.STRENGTH, primaryMuscle = "chest"),
            Exercise("fly", "Cable fly", Kind.STRENGTH, primaryMuscle = "chest"),
            Exercise("dip", "Dip", Kind.STRENGTH, primaryMuscle = "chest", archived = true),
            Exercise("odd", "Odd lift", Kind.STRENGTH),
            run,
        )
        val all = libraryList(list, "")
        assertEquals(listOf("Back" to listOf("row"), "Chest" to listOf("bench", "fly"), "Other" to listOf("odd")), all.groups.map { (g, ex) -> g to ex.map { it.id } })
        assertEquals(listOf("dip"), all.archived.map { it.id })
        assertEquals("Archived (1)", archivedTitle(all.archived.size))
        // A muscle named like the archived row is a group of its own.
        assertEquals(listOf("Archived" to listOf("odd")), libraryList(listOf(Exercise("odd", "Odd lift", Kind.STRENGTH, primaryMuscle = "archived")), "").groups.map { (g, ex) -> g to ex.map { it.id } })
        assertEquals(listOf("Chest" to listOf("fly")), libraryList(list, " cable ").groups.map { (g, ex) -> g to ex.map { it.id } })
        assertEquals(listOf("dip"), libraryList(list, "di").archived.map { it.id })
        assertEquals(LibraryList(emptyList(), emptyList()), libraryList(list, "run"))
    }

    @Test fun `the type picker lists live types and the workout's own archived type`() {
        val all = types + Modality("row", "Row", 0xFFFF8FA3, archived = true)
        assertEquals(listOf("run", "bike"), typeChoices(all, "run").map { it.id })
        assertEquals(listOf("run", "bike", "row"), typeChoices(all, "row").map { it.id })
    }
}
