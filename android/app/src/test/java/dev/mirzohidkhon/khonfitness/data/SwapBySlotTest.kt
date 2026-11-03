package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A swap with "Update the program too" changes the program row at the set's block and slot, not a row found by
 * exercise id (row 5); a program edited since the session started stays as it is (row 6). Real DAO, in memory.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SwapBySlotTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        repo = KhonRepository(dao)
    }

    @After fun close() = db.close()

    private fun strength(id: String) = Exercise(id, id.uppercase(), Kind.STRENGTH, stepKg = 2.5)

    /** Program p1: block 0 holds A then X, block 1 holds A again. A session started from it. */
    private suspend fun start(): Session {
        dao.upsertExercises(listOf("a", "b", "c", "x").map(::strength))
        dao.upsertPrograms(listOf(Program("p1", "Push", active = true)))
        dao.upsertBlocks(listOf(Block("k0", "p1", 0), Block("k1", "p1", 1)))
        dao.upsertBlockExercises(listOf(BlockExercise("r0a", "k0", 0, "a"), BlockExercise("r0b", "k0", 1, "x"), BlockExercise("r1a", "k1", 0, "a")))
        return repo.startProgramSession("p1")!!
    }

    private suspend fun firstLog(s: Session, block: Int, slot: Int) = dao.setLogsOnce(s.id).first { it.blockIndex == block && it.slot == slot }
    private suspend fun row(id: String) = dao.blockExercisesOnce().first { it.id == id }

    @Test fun `A to B for this session, then B to C for the program too, puts C in that program row`() = runBlocking {
        val s = start()
        repo.swapExerciseInSession(firstLog(s, 0, 0), strength("b"), updateProgram = false, programId = s.programId)
        assertEquals("a", row("r0a").exerciseId)
        repo.swapExerciseInSession(firstLog(s, 0, 0), strength("c"), updateProgram = true, programId = s.programId)
        assertEquals("c", row("r0a").exerciseId)
        // The other row with A and the other slot stay.
        assertEquals("a", row("r1a").exerciseId)
        assertEquals("x", row("r0b").exerciseId)
    }

    @Test fun `a swap changes only the sets at its block and slot`() = runBlocking {
        val s = start()
        repo.swapExerciseInSession(firstLog(s, 1, 0), strength("c"), updateProgram = true, programId = s.programId)
        val logs = dao.setLogsOnce(s.id)
        assertEquals(setOf("c"), logs.filter { it.blockIndex == 1 }.map { it.exerciseId }.toSet())
        assertEquals(setOf("a"), logs.filter { it.blockIndex == 0 && it.slot == 0 }.map { it.exerciseId }.toSet())
        assertEquals("c", row("r1a").exerciseId)
        assertEquals("a", row("r0a").exerciseId)
    }

    @Test fun `a program whose block is gone since the start keeps what it has, and the session still swaps`() = runBlocking {
        val s = start()
        dao.deleteBlockFully("k1")
        repo.swapExerciseInSession(firstLog(s, 1, 0), strength("c"), updateProgram = true, programId = s.programId)
        assertEquals(setOf("c"), dao.setLogsOnce(s.id).filter { it.blockIndex == 1 }.map { it.exerciseId }.toSet())
        assertEquals(listOf("a", "x"), dao.blockExercisesOnce().sortedBy { it.id }.map { it.exerciseId })
    }

    @Test fun `a program whose slot is gone since the start keeps what it has`() = runBlocking {
        val s = start()
        dao.deleteBlockExercise("r0b")
        repo.swapExerciseInSession(firstLog(s, 0, 1), strength("c"), updateProgram = true, programId = s.programId)
        assertEquals(setOf("c"), dao.setLogsOnce(s.id).filter { it.blockIndex == 0 && it.slot == 1 }.map { it.exerciseId }.toSet())
        assertEquals(listOf("a", "a"), dao.blockExercisesOnce().sortedBy { it.id }.map { it.exerciseId })
    }

    @Test fun `a deleted program changes nothing and does not crash`() = runBlocking {
        val s = start()
        dao.deleteProgramFully("p1")
        repo.swapExerciseInSession(firstLog(s, 0, 0), strength("c"), updateProgram = true, programId = s.programId)
        assertEquals("c", firstLog(s, 0, 0).exerciseId)
    }
}
