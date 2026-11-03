package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.ui.screens.mergeBody
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * An exercise merge with its Undo (rows 9 and 10), against the real DAO in memory: the snapshot is read before the
 * merge, and Undo puts the source row and exactly the moved rows back, each way in one transaction.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MergeUndoTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        repo = KhonRepository(dao)
    }

    @After fun close() = db.close()

    private fun log(id: String, session: String, ex: String) = SetLog(id, session, 0, false, 0, 0, 0, ex, ex.uppercase(), 6, 10, 2.5, 60.0, 8, SetStatus.DONE)
    private fun cardioSession(id: String, ex: String) = Session(id = id, date = "2026-11-0${id.last()}", startedAt = 1L, finishedAt = 2L, itemType = ItemType.CARDIO, exerciseId = ex, exerciseName = ex)

    /** Strength a (3 sets over 2 sessions, 1 program row) and b (its own set and row); cardio c (session, plan day, dated change) and d (its own day). */
    private suspend fun seed() {
        dao.upsertExercises(listOf(Exercise("a", "Old Bench", Kind.STRENGTH, notes = "kept"), Exercise("b", "Bench Press", Kind.STRENGTH), Exercise("c", "Jog", Kind.CARDIO), Exercise("d", "Run", Kind.CARDIO)))
        dao.upsertSetLogs(listOf(log("l1", "s1", "a"), log("l2", "s1", "a"), log("l3", "s2", "a"), log("l4", "s3", "b")))
        dao.upsertPrograms(listOf(Program("p1", "Push", active = true)))
        dao.upsertBlocks(listOf(Block("k0", "p1", 0)))
        dao.upsertBlockExercises(listOf(BlockExercise("ra", "k0", 0, "a"), BlockExercise("rb", "k0", 1, "b")))
        dao.upsertSessions(listOf(cardioSession("c1", "c"), cardioSession("d2", "d")))
        dao.upsertWeekPlan(listOf(WeekPlan(1, ItemType.CARDIO, "c"), WeekPlan(2, ItemType.CARDIO, "d")))
        dao.upsertOverrides(listOf(DayOverride("2026-11-10", ItemType.CARDIO, "c"), DayOverride("2026-11-11", ItemType.CARDIO, "d")))
    }

    private suspend fun state() = listOf(
        dao.exercisesOnce().sortedBy { it.id }, dao.allSetLogsOnce().sortedBy { it.id }, dao.blockExercisesOnce().sortedBy { it.id },
        dao.sessionsOnce().sortedBy { it.id }, dao.weekPlanOnce().sortedBy { it.weekday }, dao.overridesOnce().sortedBy { it.date },
    )

    @Test fun `the confirm counts the sets and the sessions that move`() = runBlocking {
        seed()
        assertEquals(3 to 2, repo.mergeCounts("a"))
        assertEquals(0 to 1, repo.mergeCounts("c"))
        assertEquals("3 sets and 2 sessions move to Bench Press. Old Bench is deleted.", mergeBody("Old Bench", "Bench Press", 3, 2))
        assertEquals("1 set and 1 session move to Bench Press. Old Bench is deleted.", mergeBody("Old Bench", "Bench Press", 1, 1))
    }

    @Test fun `Undo of a strength merge restores the source row and every moved row`() = runBlocking {
        seed()
        val before = state()
        val snap = repo.mergeExerciseWithUndo("a", "b")!!
        assertNull(dao.exercise("a"))
        assertEquals(setOf("b"), dao.allSetLogsOnce().map { it.exerciseId }.toSet())
        assertEquals(listOf("b", "b"), dao.blockExercisesOnce().sortedBy { it.id }.map { it.exerciseId })
        repo.undoMerge(snap)
        assertEquals(before, state())
    }

    @Test fun `Undo of a cardio merge restores its sessions, plan day and dated change, and leaves the target's own`() = runBlocking {
        seed()
        val before = state()
        val snap = repo.mergeExerciseWithUndo("c", "d")!!
        assertEquals(listOf("d", "d"), dao.weekPlanOnce().sortedBy { it.weekday }.map { it.itemId })
        assertEquals(listOf("d", "d"), dao.overridesOnce().sortedBy { it.date }.map { it.itemId })
        assertEquals(setOf("d"), dao.sessionsOnce().map { it.exerciseId }.toSet())
        repo.undoMerge(snap)
        assertEquals(before, state())
    }

    @Test fun `a row moved to the target after the merge stays there on Undo`() = runBlocking {
        seed()
        val snap = repo.mergeExerciseWithUndo("a", "b")!!
        dao.upsertSetLogs(listOf(log("l5", "s4", "b"))) // logged on the target during the Undo window
        repo.undoMerge(snap)
        assertEquals("b", dao.allSetLogsOnce().first { it.id == "l5" }.exerciseId)
        assertEquals(listOf("a", "a", "a"), dao.allSetLogsOnce().filter { it.id in setOf("l1", "l2", "l3") }.map { it.exerciseId })
    }

    @Test fun `a long history moves back in full`() = runBlocking {
        seed()
        dao.upsertSetLogs((0 until 1_200).map { log("x$it", "h${it / 10}", "a") })
        val snap = repo.mergeExerciseWithUndo("a", "b")!!
        assertEquals(1_203, snap.setLogs.size)
        repo.undoMerge(snap)
        assertEquals(1_203, dao.allSetLogsOnce().count { it.exerciseId == "a" })
    }

    @Test fun `a merge into a missing exercise writes nothing`() = runBlocking {
        seed()
        val before = state()
        assertNull(repo.mergeExerciseWithUndo("a", "gone"))
        assertNull(repo.mergeExerciseWithUndo("gone", "b"))
        assertEquals(before, state())
    }
}
