package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.saveable.SaverScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.ui.screens.BlockDraft
import dev.mirzohidkhon.khonfitness.ui.screens.ProgramDraft
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The program and block editors' writes against a real in-memory database: drafts write nothing until Done or the
 * first block / exercise, deletes leave no orphans, and each edit writes its own columns onto the stored row.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ProgramWritesTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        repo = KhonRepository(dao)
    }

    // The drafts hold snapshot state. Once a Compose test in this JVM has started Compose's global snapshot manager,
    // a draft write posts the apply notification to the main looper. Run it here: Robolectric drops the queue after
    // the test, and the dropped message leaves AndroidUiDispatcher marked as scheduled for good, so no later Compose
    // test gets idle.
    @After fun close() { db.close(); shadowOf(Looper.getMainLooper()).idle() }

    // Two programs, "push" active with two blocks, "pull" with one; Monday plans push, a dated change holds push too.
    private suspend fun seed() {
        dao.upsertPrograms(listOf(Program("push", "Push", active = true, sortOrder = 0), Program("pull", "Pull", sortOrder = 1)))
        dao.upsertBlocks(listOf(Block("b1", "push", 0), Block("b2", "push", 1), Block("b3", "pull", 0)))
        dao.upsertBlockExercises(listOf(BlockExercise("e1", "b1", 0, "bench"), BlockExercise("e2", "b1", 1, "fly"), BlockExercise("e3", "b2", 0, "dip"), BlockExercise("e4", "b3", 0, "row")))
        dao.upsert(WeekPlan(1, ItemType.PROGRAM, "push"))
        dao.upsert(WeekPlan(2, ItemType.PROGRAM, "pull"))
        dao.upsert(DayOverride("2026-11-05", ItemType.PROGRAM, "push"))
    }

    // ---- drafts

    @Test fun `a new program goes after the others and takes the active mark only when chosen`() = runBlocking {
        seed()
        repo.createProgram(Program("legs", "Legs", active = false))
        assertEquals(2, dao.program("legs")!!.sortOrder)
        assertEquals("push", dao.activeProgram()!!.id)
        repo.createProgram(Program("arms", "Arms", active = true))
        assertEquals(listOf("arms"), dao.programsOnce().filter { it.active }.map { it.id })
    }

    @Test fun `a new program left with no Add block and no Done writes nothing`() = runBlocking {
        seed()
        val draft = ProgramDraft(isNew = true)
        draft.name = "Legs"; draft.activeChoice = true
        // Leaving the editor runs no code: there is nothing to undo.
        assertNull(draft.programId("new"))
        assertEquals(listOf("pull", "push"), dao.programsOnce().map { it.id }.sorted())
        assertEquals("push", dao.activeProgram()!!.id)
    }

    @Test fun `Add block then leave keeps one program with that block, saved as Done would`() = runBlocking {
        seed()
        val draft = ProgramDraft(isNew = true)
        draft.name = "Legs"; draft.activeChoice = true
        repo.createProgram(draft.programForBlock("legs", onlyProgram = false)!!)
        val block = BlockDraft(isNew = true)
        val (b, first) = block.firstPick("b9", "legs", superset = true, exerciseRowId = "e9", slot = 0, exerciseId = "squat")!!
        repo.createBlock(b, first)
        // Leave without Done: nothing runs.
        assertEquals(Program("legs", "Legs", active = true, sortOrder = 2), dao.program("legs"))
        assertEquals(listOf("legs"), dao.programsOnce().filter { it.active }.map { it.id })
        assertEquals(listOf("b9"), dao.blocksFor("legs").map { it.id })
        assertEquals(3, dao.programsOnce().size)
    }

    @Test fun `Add block then leave before the first exercise still keeps the program`() = runBlocking {
        seed()
        val draft = ProgramDraft(isNew = true)
        repo.createProgram(draft.programForBlock("legs", onlyProgram = false)!!)
        assertEquals(Program("legs", "New program", active = false, sortOrder = 2), dao.program("legs"))
        assertEquals("push", dao.activeProgram()!!.id)
    }

    @Test fun `Add block, then a restored editor and Done updates the same row`() = runBlocking {
        seed()
        val draft = ProgramDraft(isNew = true)
        repo.createProgram(draft.programForBlock("legs", onlyProgram = false)!!)
        // Process death: the editor comes back from its saved state.
        val restored = with(ProgramDraft.Saver) { SaverScope { true }.save(draft) }!!.let { ProgramDraft.Saver.restore(it)!! }
        restored.name = "Legs"
        val save = restored.done("other", dao.program("legs"), onlyProgram = false)
        assertNull(save.create)
        save.rename?.let { repo.setProgramName(restored.programId("new")!!, it) }
        assertEquals(listOf("legs", "pull", "push"), dao.programsOnce().map { it.id }.sorted())
        assertEquals("Legs", dao.program("legs")!!.name)
    }

    @Test fun `a new block left before its first exercise writes nothing, the first exercise saves both`() = runBlocking {
        seed()
        val draft = BlockDraft(isNew = true)
        assertEquals(3, dao.blocksOnce().size) // nothing written while it is only a draft
        val (b, first) = draft.firstPick("b9", "push", superset = false, exerciseRowId = "e9", slot = 0, exerciseId = "squat")!!
        repo.createBlock(b, first)
        assertEquals(Block("b9", "push", 2, superset = false), dao.blocksOnce().find { it.id == "b9" })
        assertEquals(listOf(BlockExercise("e9", "b9", 0, "squat")), dao.blockExercisesFor("b9"))
    }

    // ---- deletes

    @Test fun `deleting a program leaves no blocks, block exercises or plan days that point at it`() = runBlocking {
        seed()
        repo.deleteProgram("push")
        assertNull(dao.program("push"))
        assertEquals(listOf("b3"), dao.blocksOnce().map { it.id })
        assertEquals(listOf("e4"), dao.blockExercisesOnce().map { it.id })
        assertEquals(WeekPlan(1, ItemType.REST, null), dao.weekPlanOnce().find { it.weekday == 1 })
        assertEquals(WeekPlan(2, ItemType.PROGRAM, "pull"), dao.weekPlanOnce().find { it.weekday == 2 })
        assertEquals(DayOverride("2026-11-05", ItemType.REST, null), dao.overridesOnce().single())
    }

    @Test fun `deleting the active program moves the mark to the first one left`() = runBlocking {
        seed()
        repo.deleteProgram("push")
        assertEquals("pull", dao.activeProgram()!!.id)
    }

    @Test fun `deleting a block takes its exercises and nothing else`() = runBlocking {
        seed()
        repo.deleteBlock("b1")
        assertEquals(listOf("b2", "b3"), dao.blocksOnce().map { it.id }.sorted())
        assertEquals(listOf("e3", "e4"), dao.blockExercisesOnce().map { it.id }.sorted())
    }

    @Test fun `removing an exercise from a block keeps the other one`() = runBlocking {
        seed()
        repo.deleteBlockExercise("e1")
        assertEquals(listOf("e2"), dao.blockExercisesFor("b1").map { it.id })
    }

    // ---- targeted edits

    @Test fun `a rename does not undo an active mark that moved after the editor opened`() = runBlocking {
        seed()
        dao.program("pull") // what the editor holds: pull, not active
        repo.setActiveProgram("pull") // its Active switch writes at once
        repo.setProgramName("pull", "Pull day") // Done
        assertEquals(Program("pull", "Pull day", active = true, sortOrder = 1), dao.program("pull"))
    }

    @Test fun `rep range edits clamp against the stored row and keep a sets edit`() = runBlocking {
        seed()
        dao.blockExercisesFor("b1").first() // what the screen held: 3 sets, 8 to 12
        repo.setBlockExerciseSets("e1", 5)
        repo.setBlockExerciseMinReps("e1", 14)
        assertEquals(BlockExercise("e1", "b1", 0, "bench", sets = 5, minReps = 14, maxReps = 14), dao.blockExercisesFor("b1").first())
        repo.setBlockExerciseMaxReps("e1", 6)
        assertEquals(BlockExercise("e1", "b1", 0, "bench", sets = 5, minReps = 6, maxReps = 6), dao.blockExercisesFor("b1").first())
        repo.setBlockExerciseExercise("e1", "incline")
        assertEquals("incline", dao.blockExercisesFor("b1").first().exerciseId)
    }

    @Test fun `a superset switch keeps a move that landed after the editor opened`() = runBlocking {
        seed()
        repo.moveBlock("b1", 1)
        repo.setBlockSuperset("b1", false)
        assertEquals(Block("b1", "push", 1, superset = false), dao.blocksOnce().find { it.id == "b1" })
    }

    @Test fun `moving a block renumbers its program from 0 and stays inside it`() = runBlocking {
        seed()
        repo.moveBlock("b2", -1)
        assertEquals(listOf("b2", "b1"), dao.blocksFor("push").map { it.id })
        assertEquals(listOf(0, 1), dao.blocksFor("push").map { it.sortOrder })
        repo.moveBlock("b2", -1)
        assertEquals(listOf("b2", "b1"), dao.blocksFor("push").map { it.id })
        assertEquals(0, dao.blocksFor("pull").single().sortOrder)
    }

    // ---- slot picks

    @Test fun `a slot row saved after the screen read its list gets the pick, not a second row`() = runBlocking {
        seed()
        dao.blockExercisesFor("b2") // what the screen held: only slot 0
        dao.upsert(BlockExercise("e5", "b2", 1, "fly", sets = 4)) // slot 1 saved meanwhile
        repo.setSlotExercise("b2", 1, "pullover", newRowId = "new")
        assertEquals(listOf(BlockExercise("e3", "b2", 0, "dip"), BlockExercise("e5", "b2", 1, "pullover", sets = 4)), dao.blockExercisesFor("b2"))
    }

    @Test fun `a pick for an empty slot adds its row, and for a deleted block writes nothing`() = runBlocking {
        seed()
        repo.setSlotExercise("b2", 1, "fly", newRowId = "new")
        assertEquals(BlockExercise("new", "b2", 1, "fly"), dao.blockExercisesFor("b2").last())
        repo.deleteBlock("b2")
        repo.setSlotExercise("b2", 0, "dip", newRowId = "late")
        assertEquals(emptyList<BlockExercise>(), dao.blockExercisesFor("b2"))
    }
}
