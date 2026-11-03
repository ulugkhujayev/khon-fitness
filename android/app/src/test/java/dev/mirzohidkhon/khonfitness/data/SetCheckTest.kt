package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.ui.screens.Once
import dev.mirzohidkhon.khonfitness.ui.screens.discardBody
import dev.mirzohidkhon.khonfitness.ui.screens.setMissing
import dev.mirzohidkhon.khonfitness.ui.screens.summarize
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A set's check needs reps above 0 and, on a weighted exercise, a weight above 0 (row 2); the stored row decides. The
 * finish summary counts a 0-rep set as not done and never as a new best (row 3).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SetCheckTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        repo = KhonRepository(dao)
    }

    @After fun close() = db.close()

    private fun log(id: String, weight: Double?, reps: Int?, step: Double = 2.5, status: String = SetStatus.PENDING, session: String = "s1", ex: String = "bench") =
        SetLog(id, session, 0, false, 0, 0, 0, ex, "Bench Press", 6, 10, step, weight, reps, status)

    private suspend fun status(id: String) = dao.setLogsOnce("s1").first { it.id == id }.status

    @Test fun `the check writes only for a set with weight and reps`() = runBlocking {
        dao.upsertSetLogs(listOf(log("ok", 60.0, 8), log("noReps", 60.0, null), log("zeroReps", 60.0, 0), log("noKg", null, 8), log("zeroKg", 0.0, 8)))
        assertTrue(repo.checkSet("ok"))
        for (id in listOf("noReps", "zeroReps", "noKg", "zeroKg")) {
            assertFalse(id, repo.checkSet(id))
            assertEquals(id, SetStatus.PENDING, status(id))
        }
        assertEquals(SetStatus.DONE, status("ok"))
    }

    @Test fun `a bodyweight set (step 0 kg) checks with reps only`() = runBlocking {
        dao.upsertSetLogs(listOf(log("push", null, 12, step = 0.0), log("pushNoReps", null, 0, step = 0.0)))
        assertTrue(repo.checkSet("push"))
        assertFalse(repo.checkSet("pushNoReps"))
    }

    @Test fun `a weight typed just before the check counts`() = runBlocking {
        dao.upsertSetLogs(listOf(log("a", null, 8)))
        repo.setSetWeight("a", 62.5) // sent first, as the field saves before the tap on the check
        assertTrue(repo.checkSet("a"))
    }

    @Test fun `the row says what is missing, in field order`() {
        assertEquals(listOf("Enter weight", "Enter reps"), setMissing(log("a", null, null)))
        assertEquals(listOf("Enter reps"), setMissing(log("a", 60.0, 0)))
        assertEquals(listOf("Enter weight"), setMissing(log("a", 0.0, 8)))
        assertEquals(emptyList<String>(), setMissing(log("a", null, 10, step = 0.0)))
        assertEquals(emptyList<String>(), setMissing(log("a", 60.0, 8)))
    }

    @Test fun `a 0-rep set checked by old data counts as not done and never as a new best`() {
        val before = log("old", 50.0, 8, status = SetStatus.DONE, session = "s0")
        val today = listOf(log("a", 55.0, 8, status = SetStatus.DONE), log("zero", 200.0, 0, status = SetStatus.DONE))
        val s = summarize(today, today + before, "s1", 0L, 60_000L)
        assertEquals(1, s.done)
        assertEquals(2, s.total)
        assertEquals(440.0, s.volume, 0.0)
        // 55 x 8 beats 50 x 8; the 200 kg x 0 set does not count as one more.
        assertEquals(1, s.bests)
        val onlyZero = summarize(listOf(log("zero", 200.0, 0, status = SetStatus.DONE)), listOf(before), "s1", 0L, 60_000L)
        assertEquals(0, onlyZero.done)
        assertEquals(0, onlyZero.bests)
    }

    @Test fun `an old 0-rep set does not stand as the best to beat`() {
        val oldZero = log("old", 300.0, 0, status = SetStatus.DONE, session = "s0")
        val today = listOf(log("a", 60.0, 8, status = SetStatus.DONE))
        assertEquals(1, summarize(today, today + oldZero, "s1", 0L, 60_000L).bests)
    }

    @Test fun `the discard line counts the checked sets`() {
        assertEquals("3 checked sets are lost.", discardBody(3))
        assertEquals("1 checked set is lost.", discardBody(1))
        assertEquals("Nothing is checked yet.", discardBody(0))
    }

    @Test fun `Once runs its action the first time only`() {
        val once = Once()
        var n = 0
        once { n++ }; once { n++ }
        assertEquals(1, n)
    }
}
