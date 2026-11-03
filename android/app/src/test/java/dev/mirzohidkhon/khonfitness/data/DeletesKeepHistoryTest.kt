package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.ui.screens.DayKind
import dev.mirzohidkhon.khonfitness.ui.screens.liftHistory
import dev.mirzohidkhon.khonfitness.ui.screens.logRows
import dev.mirzohidkhon.khonfitness.ui.screens.weekOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Deleting an exercise, a block or a program against a real in-memory database: the sessions and set logs that point
 * at them stay, and History still shows them (the day's Log rows, the row title, the exercise's records).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DeletesKeepHistoryTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        repo = KhonRepository(dao)
    }

    @After fun close() = db.close()

    private val day = "2026-11-01"

    // "push" (active) with block b1 holding bench, "pull" beside it; a finished Push session with two bench sets and a
    // finished cardio session on "row".
    private suspend fun seed() {
        dao.upsertExercises(listOf(
            Exercise("bench", "Bench press", Kind.STRENGTH, primaryMuscle = "chest"), Exercise("fly", "Fly", Kind.STRENGTH, primaryMuscle = "chest"),
            Exercise("row", "Rower", Kind.CARDIO, modalityId = "rowm"),
        ))
        dao.upsertPrograms(listOf(Program("push", "Push", active = true, sortOrder = 0), Program("pull", "Pull", sortOrder = 1)))
        dao.upsert(Block("b1", "push", 0))
        dao.upsertBlockExercises(listOf(BlockExercise("e1", "b1", 0, "bench"), BlockExercise("e2", "b1", 1, "fly")))
        dao.upsert(Session(id = "s1", date = day, startedAt = 1L, finishedAt = 2L, itemType = ItemType.PROGRAM, programId = "push", programName = "Push"))
        dao.upsertSetLogs(listOf(
            SetLog("l1", "s1", 0, true, 0, 0, 0, "bench", "Bench press", 8, 12, 2.5, weightKg = 60.0, reps = 10, status = SetStatus.DONE),
            SetLog("l2", "s1", 0, true, 1, 0, 1, "fly", "Fly", 8, 12, 2.5, weightKg = 14.0, reps = 12, status = SetStatus.DONE),
        ))
        dao.upsert(Session(id = "c1", date = day, startedAt = 3L, finishedAt = 4L, itemType = ItemType.CARDIO, exerciseId = "row", exerciseName = "Rower", timeSec = 900))
    }

    private suspend fun assertHistoryKept() {
        val sessions = dao.sessionsOnce()
        assertEquals(listOf("c1", "s1"), sessions.map { it.id }.sorted())
        assertEquals(listOf("l1", "l2"), dao.allSetLogsOnce().map { it.id }.sorted())
        val date = day.toDate()
        val rows = logRows(weekOf(date), date, sessions) { PlanItem(ItemType.REST) }.filter { it.date == date }
        assertEquals(listOf("s1", "c1"), rows.map { it.session?.id })
        assertEquals(listOf(DayKind.DONE, DayKind.DONE), rows.map { it.kind })
        val s1 = sessions.first { it.id == "s1" }
        assertEquals("Push", s1.programName ?: s1.exerciseName)
        assertEquals(1, liftHistory("bench", dao.allSetLogsOnce(), sessions).size)
    }

    @Test fun `deleting a program keeps its sessions and set logs in History`() = runBlocking {
        seed()
        repo.deleteProgram("push")
        assertNull(dao.program("push"))
        assertHistoryKept()
    }

    @Test fun `deleting a block keeps the sessions and set logs that came from it`() = runBlocking {
        seed()
        repo.deleteBlock("b1")
        assertEquals(emptyList<Block>(), dao.blocksFor("push"))
        assertHistoryKept()
    }

    @Test fun `removing an exercise from a block keeps its set logs`() = runBlocking {
        seed()
        repo.deleteBlockExercise("e1")
        assertHistoryKept()
    }

    @Test fun `deleting an exercise that sessions use is refused and History keeps it`() = runBlocking {
        seed()
        repo.deleteBlock("b1") // no program holds bench or fly now, only past sets
        repo.deleteExercise("bench")
        repo.deleteExercise("row")
        assertNotNull(dao.exercise("bench"))
        assertNotNull(dao.exercise("row"))
        assertHistoryKept()
    }

    @Test fun `deleting an unused exercise keeps every session`() = runBlocking {
        seed()
        dao.upsert(Exercise("curl", "Curl", Kind.STRENGTH, primaryMuscle = "biceps"))
        repo.deleteExercise("curl")
        assertNull(dao.exercise("curl"))
        assertHistoryKept()
    }
}
