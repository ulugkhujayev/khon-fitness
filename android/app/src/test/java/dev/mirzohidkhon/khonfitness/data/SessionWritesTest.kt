package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Session fields edited in place (the session page, the finish sheet, the timer) against a real in-memory database:
 * each edit writes its own column, so no edit undoes another.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SessionWritesTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        repo = KhonRepository(dao)
    }

    @After fun close() = db.close()

    private fun log(id: String, order: Int, status: String) =
        SetLog(id, "s1", 0, false, 0, order, order, "bench", "Bench Press", 6, 10, 2.5, 60.0, 8, status)

    private suspend fun seed(): Session {
        val s = Session(id = "s1", date = "2026-11-02", startedAt = 1_000L, itemType = ItemType.PROGRAM, programId = "p1", programName = "Push")
        dao.upsert(s)
        dao.upsertSetLogs(listOf(log("a", 0, SetStatus.DONE), log("b", 1, SetStatus.PENDING), log("c", 2, SetStatus.SKIPPED)))
        return s
    }

    private suspend fun row() = dao.session("s1")!!

    @Test fun `effort then note then Save keeps both`() = runBlocking {
        val held = seed() // what the finish sheet holds: no effort, no note
        repo.setSessionEffort(held.id, 7)
        repo.setSessionNote(held.id, "Felt strong")
        repo.finishSession(held.id, now = 5_000L)
        assertEquals(held.copy(effort = 7, note = "Felt strong", finishedAt = 5_000L), row())
    }

    @Test fun `note then effort keeps both`() = runBlocking {
        val held = seed()
        repo.setSessionNote(held.id, "Short on time")
        repo.setSessionEffort(held.id, 2)
        assertEquals(held.copy(effort = 2, note = "Short on time"), row())
        // The page and the sheet clear a field the same way.
        repo.setSessionEffort(held.id, null)
        assertEquals(held.copy(effort = null, note = "Short on time"), row())
    }

    @Test fun `Save after an edit keeps the edit and sets finishedAt`() = runBlocking {
        val held = seed()
        repo.setSessionDate(held.id, LocalDate.of(2026, 11, 1))
        repo.setSessionEffort(held.id, 8)
        repo.finishSession(held.id, now = 9_000L)
        assertEquals(held.copy(date = "2026-11-01", effort = 8, finishedAt = 9_000L), row())
    }

    @Test fun `Save turns pending sets into skipped and leaves the others`() = runBlocking {
        seed()
        repo.finishSession("s1", now = 9_000L)
        assertEquals(listOf(SetStatus.DONE, SetStatus.SKIPPED, SetStatus.SKIPPED), dao.setLogsOnce("s1").map { it.status })
        // Weights and reps stay as logged.
        assertEquals(listOf(log("a", 0, SetStatus.DONE), log("b", 1, SetStatus.SKIPPED), log("c", 2, SetStatus.SKIPPED)), dao.setLogsOnce("s1"))
        // A missing session writes nothing and does not fail.
        repo.finishSession("gone", now = 9_000L)
    }

    @Test fun `a timer duration write keeps an effort typed meanwhile`() = runBlocking {
        val held = seed() // what the timer read before the user typed
        repo.setSessionEffort(held.id, 7)
        repo.setSessionTime(held.id, 1_800)
        assertEquals(held.copy(effort = 7, timeSec = 1_800), row())
    }

    @Test fun `concurrent writes from the page, the sheet and the timer all land`() = runBlocking {
        val held = seed()
        repeat(60) { i ->
            coroutineScope {
                launch(Dispatchers.IO) { repo.setSessionEffort("s1", i % 10 + 1) }
                launch(Dispatchers.IO) { repo.setSessionNote("s1", "note $i") }
                launch(Dispatchers.IO) { repo.setSessionTime("s1", i) }
                launch(Dispatchers.IO) { repo.setSessionDate("s1", LocalDate.of(2026, 1, 1).plusDays(i.toLong())) }
            }
            assertEquals(held.copy(effort = i % 10 + 1, note = "note $i", timeSec = i, date = LocalDate.of(2026, 1, 1).plusDays(i.toLong()).iso()), row())
        }
    }

    @Test fun `a weight typed and then a check tapped from the same row on screen keeps both`() = runBlocking {
        seed()
        // The row on screen still holds the old weight when the check is tapped.
        val onScreen = dao.setLogsOnce("s1").first { it.id == "b" }
        repo.setSetWeight(onScreen.id, 62.5)
        repo.setSetStatus(onScreen.id, SetStatus.DONE)
        repo.setSetReps(onScreen.id, 9)
        val b = dao.setLogsOnce("s1").first { it.id == "b" }
        assertEquals(62.5, b.weightKg!!, 0.0)
        assertEquals(SetStatus.DONE, b.status)
        assertEquals(9, b.reps)
    }

    @Test fun `a swap changes only the exercise columns of its sets`() = runBlocking {
        seed()
        repo.setSetWeight("b", 70.0)
        repo.swapExerciseInSession("s1", "bench", Exercise("incline", "Incline Press", Kind.STRENGTH, stepKg = 1.25), updateProgram = false, programId = null)
        val logs = dao.setLogsOnce("s1")
        assertTrue(logs.all { it.exerciseId == "incline" && it.exerciseName == "Incline Press" && it.stepKg == 1.25 })
        assertEquals(70.0, logs.first { it.id == "b" }.weightKg!!, 0.0)
        assertEquals(listOf(SetStatus.DONE, SetStatus.PENDING, SetStatus.SKIPPED), logs.map { it.status })
    }

    @Test fun `a swap with Update the program too changes only that program's block exercises`() = runBlocking {
        seed()
        dao.upsertPrograms(listOf(Program("p1", "Push", active = true), Program("p2", "Pull", sortOrder = 1)))
        dao.upsertBlocks(listOf(Block("b1", "p1", 0), Block("b2", "p2", 0)))
        dao.upsertBlockExercises(listOf(BlockExercise("e1", "b1", 0, "bench", sets = 4), BlockExercise("e2", "b2", 0, "bench")))
        repo.swapExerciseInSession("s1", "bench", Exercise("incline", "Incline Press", Kind.STRENGTH), updateProgram = true, programId = "p1")
        assertEquals(BlockExercise("e1", "b1", 0, "incline", sets = 4), dao.blockExercisesFor("b1").single())
        assertEquals("bench", dao.blockExercisesFor("b2").single().exerciseId)
    }

    @Test fun `a swap does not re-create a block exercise deleted meanwhile`() = runBlocking {
        seed()
        dao.upsertPrograms(listOf(Program("p1", "Push", active = true)))
        dao.upsertBlocks(listOf(Block("b1", "p1", 0)))
        repo.swapExerciseInSession("s1", "bench", Exercise("incline", "Incline Press", Kind.STRENGTH), updateProgram = true, programId = "p1")
        assertTrue(dao.blockExercisesFor("b1").isEmpty())
    }
}
