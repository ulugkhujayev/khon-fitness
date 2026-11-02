package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.KhonRepository
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.Program
import dev.mirzohidkhon.khonfitness.data.WeekPlan
import dev.mirzohidkhon.khonfitness.data.moveInOrder
import dev.mirzohidkhon.khonfitness.update.Updater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorRulesTest {
    // ---- program draft (contract rows 7 and 8)

    @Test fun `a new program left without Done writes nothing and shows no Delete`() {
        val d = ProgramDraft(isNew = true)
        d.name = "Push"
        assertFalse(d.showDelete(programCount = 3))
        assertNull(d.programId("new"))
        assertNull(d.dropOnLeave())
    }

    @Test fun `Done on a new program creates it once with the typed name and the chosen Active`() {
        val d = ProgramDraft(isNew = true)
        d.name = "  Push  "; d.activeChoice = true
        val save = d.done("p1", saved = null, onlyProgram = false)
        assertEquals(Program("p1", "Push", active = true), save.create)
        assertEquals("p1", d.programId("new"))
        assertNull(d.done("p2", saved = Program("p1", "Push", active = true), onlyProgram = false).create)
        assertNull(d.dropOnLeave())
    }

    @Test fun `an empty name falls back and Active is on by default only for the first program`() {
        assertEquals(Program("a", NEW_PROGRAM_NAME, active = true), ProgramDraft(true).done("a", null, onlyProgram = true).create)
        assertEquals(Program("b", NEW_PROGRAM_NAME, active = false), ProgramDraft(true).done("b", null, onlyProgram = false).create)
    }

    @Test fun `Add block saves the program once, active only when it is the only one`() {
        val d = ProgramDraft(isNew = true)
        d.activeChoice = true
        assertEquals(Program("p1", NEW_PROGRAM_NAME, active = false), d.programForBlock("p1", onlyProgram = false))
        assertNull(d.programForBlock("p2", onlyProgram = false))
        assertEquals("p1", d.programId("new"))
        assertEquals(Program("solo", NEW_PROGRAM_NAME, active = true), ProgramDraft(true).programForBlock("solo", onlyProgram = true))
    }

    @Test fun `a program saved by Add block is dropped when left without Done`() {
        val d = ProgramDraft(isNew = true)
        d.programForBlock("p1", onlyProgram = false)
        assertEquals("p1", d.dropOnLeave())
    }

    @Test fun `Done on a program saved by Add block keeps it, renames it and applies the waiting Active choice`() {
        val d = ProgramDraft(isNew = true)
        d.activeChoice = true
        d.programForBlock("p1", onlyProgram = false)
        d.name = "Legs"
        val save = d.done("unused", saved = Program("p1", NEW_PROGRAM_NAME, active = false), onlyProgram = false)
        assertEquals(ProgramSave(rename = "Legs", activate = true), save)
        assertNull(d.dropOnLeave())
    }

    @Test fun `an existing program saves only a changed name and is never dropped`() {
        val d = ProgramDraft(isNew = false)
        assertEquals(ProgramSave(), d.done("x", Program("p", "Push", active = false), onlyProgram = false))
        d.name = "Push"
        assertEquals(ProgramSave(), d.done("x", Program("p", "Push"), onlyProgram = false))
        d.name = "Pull"
        assertEquals(ProgramSave(rename = "Pull"), d.done("x", Program("p", "Push"), onlyProgram = false))
        assertNull(d.dropOnLeave())
        assertTrue(d.showDelete(2)); assertFalse(d.showDelete(1))
    }

    @Test fun `the active program's switch is fixed on, another program's is free`() {
        val saved = ProgramDraft(isNew = false)
        assertTrue(saved.activeLocked(savedActive = true, onlyProgram = false))
        assertFalse(saved.activeLocked(savedActive = false, onlyProgram = false))
        assertTrue(ProgramDraft(true).activeLocked(savedActive = false, onlyProgram = true))
        assertFalse(ProgramDraft(true).activeLocked(savedActive = false, onlyProgram = false))
    }

    @Test fun `the draft model drops the provisional program when the editor leaves the stack`() {
        val model = ProgramDraftModel()
        val dropped = mutableListOf<String>()
        model.drop = { dropped += it }
        model.draft(isNew = true).programForBlock("p1", onlyProgram = false)
        assertTrue(model.draft(isNew = true) === model.draft(isNew = true))
        model.onCleared()
        assertEquals(listOf("p1"), dropped)
    }

    @Test fun `the draft model drops nothing after Done`() {
        val model = ProgramDraftModel()
        val dropped = mutableListOf<String>()
        model.drop = { dropped += it }
        val d = model.draft(isNew = true)
        d.programForBlock("p1", onlyProgram = false)
        d.done("x", Program("p1", NEW_PROGRAM_NAME), onlyProgram = false)
        model.onCleared()
        assertEquals(emptyList<String>(), dropped)
    }

    // ---- block draft (contract rows 9 and 10)

    @Test fun `a new block has nothing saved until its first exercise, then saves both once`() {
        val d = BlockDraft(isNew = true)
        assertNull(d.blockId("new"))
        val pick = d.firstPick("b1", "p1", superset = false, exerciseRowId = "be1", slot = 0, exerciseId = "bench")
        assertNotNull(pick)
        val (block, first) = pick!!
        assertEquals("b1", block.id); assertEquals("p1", block.programId); assertFalse(block.superset)
        assertEquals("b1", first.blockId); assertEquals("bench", first.exerciseId); assertEquals(0, first.slot)
        assertEquals("b1", d.blockId("new"))
        assertNull(d.firstPick("b2", "p1", false, "be2", 1, "row"))
    }

    @Test fun `an existing block edits the route's block and never creates one`() {
        val d = BlockDraft(isNew = false)
        assertEquals("b7", d.blockId("b7"))
        assertNull(d.firstPick("b8", "p1", true, "be", 0, "bench"))
    }

    @Test fun `the free slot is A first, then B, then none`() {
        assertEquals(0, freeSlot(emptyList()))
        assertEquals(1, freeSlot(listOf(0)))
        assertEquals(0, freeSlot(listOf(1)))
        assertNull(freeSlot(listOf(0, 1)))
    }

    @Test fun `move in order stays inside the list`() {
        assertEquals(listOf("b", "a", "c"), moveInOrder(listOf("a", "b", "c"), "a", 1))
        assertEquals(listOf("a", "c", "b"), moveInOrder(listOf("a", "b", "c"), "c", -1))
        assertEquals(listOf("a", "b", "c"), moveInOrder(listOf("a", "b", "c"), "a", -1))
        assertEquals(listOf("a", "b"), moveInOrder(listOf("a", "b"), "x", 1))
    }

    // ---- kind lock (contract rows 2 and 3)

    @Test fun `kind is locked once used or while the count loads, never for a new exercise`() {
        assertTrue(kindLocked(isNew = false, usage = 2))
        assertTrue(kindLocked(isNew = false, usage = null))
        assertFalse(kindLocked(isNew = false, usage = 0))
        assertFalse(kindLocked(isNew = true, usage = null))
        assertTrue(canDeleteExercise(0)); assertFalse(canDeleteExercise(1)); assertFalse(canDeleteExercise(null))
    }

    // ---- lists

    @Test fun `modality use counts its live cardio exercises`() {
        val ex = listOf(
            Exercise("a", "Run", Kind.CARDIO, modalityId = "m"), Exercise("b", "Sprint", Kind.CARDIO, modalityId = "m"),
            Exercise("c", "Old", Kind.CARDIO, modalityId = "m", archived = true), Exercise("d", "Bike", Kind.CARDIO, modalityId = "n"),
        )
        assertEquals("2 exercises", modalityUse("m", ex))
        assertEquals("1 exercise", modalityUse("n", ex))
        assertEquals("No exercises", modalityUse("z", ex))
    }

    private fun plan(vararg days: Pair<Int, String>) = KhonRepository.PlanData(
        plan = days.map { (d, type) -> WeekPlan(d, type, if (type == ItemType.PROGRAM) "p" else if (type == ItemType.CARDIO) "run" else null) },
        overrides = emptyList(), programs = listOf(Program("p", "Push")), exercises = listOf(Exercise("run", "Run", Kind.CARDIO)),
    )

    @Test fun `week summary counts training and rest days, singular and plural`() {
        assertEquals("5 training days · 2 rest days", plan(1 to ItemType.PROGRAM, 2 to ItemType.PROGRAM, 3 to ItemType.CARDIO, 4 to ItemType.PROGRAM, 5 to ItemType.PROGRAM, 6 to ItemType.REST).weekSummary())
        assertEquals("1 training day · 6 rest days", plan(3 to ItemType.CARDIO).weekSummary())
        assertEquals("6 training days · 1 rest day", plan(1 to ItemType.PROGRAM, 2 to ItemType.PROGRAM, 3 to ItemType.PROGRAM, 4 to ItemType.PROGRAM, 5 to ItemType.CARDIO, 6 to ItemType.CARDIO).weekSummary())
        assertEquals("0 training days · 7 rest days", plan().weekSummary())
    }

    @Test fun `a template day that points at a missing program is rest`() {
        val data = KhonRepository.PlanData(listOf(WeekPlan(1, ItemType.PROGRAM, "gone")), emptyList(), listOf(Program("p", "Push")), emptyList())
        assertTrue(data.templateItem(1).isRest)
        assertEquals("0 training days · 7 rest days", data.weekSummary())
    }

    @Test fun `sets and blocks count in singular and plural`() {
        assertEquals("1 set", setsText(1)); assertEquals("12 sets", setsText(12))
        assertEquals("1 block", blocksText(1)); assertEquals("0 blocks", blocksText(0))
    }

    // ---- settings

    @Test fun `update status follows the updater`() {
        assertEquals(UpdateStatus("Check for update", "Checking…", null), updateStatus(null, null))
        assertEquals("v0.4.8 is the latest version.", updateStatus(Updater.Result.UpToDate("0.4.8"), null).note)
        val failed = updateStatus(Updater.Result.Failed("No network"), null)
        assertEquals("No network", failed.note); assertTrue(failed.error)
        val release = Updater.Release(tag_name = "v0.5.0", name = null, body = null, assets = emptyList())
        val available = Updater.Result.Available(release, Updater.Asset("khon.apk", "https://x/khon.apk"))
        assertEquals("Update to v0.5.0", updateStatus(available, null).title)
        assertNull(updateStatus(available, null).value)
        assertEquals("Downloading 42%", updateStatus(available, 0.42f).value)
        val failedDownload = updateStatus(available, null, downloadError = "Download failed")
        assertEquals("Download failed", failedDownload.note); assertTrue(failedDownload.error)
    }
}
