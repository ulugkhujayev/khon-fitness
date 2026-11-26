package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `checkedAt`: a check of a pending or skipped set in an unfinished session stamps the verified time in the same
 * statement; a done set keeps its first stamp; a set of a finished session keeps its own; every path to pending or
 * skipped clears it; edits, swaps, a prefilled start, and a backup round trip never rewrite it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SetStampTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository
    private val start = at(18, 0)
    private val clocks = FakeClocks(start)
    private val anchor = Anchor(start, clocks.elapsed)

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        repo = KhonRepository(dao, clocks = clocks, zone = { TEST_ZONE })
    }

    @After fun close() = db.close()

    private fun log(id: String, weight: Double? = 60.0, reps: Int? = 8, status: String = SetStatus.PENDING, checkedAt: Long? = null, session: String = "s1") =
        SetLog(id, session, 0, false, 0, id.hashCode() and 7, 0, "bench", "Bench Press", 6, 10, 2.5, weight, reps, status, checkedAt)

    private suspend fun seed(finished: Boolean = false, vararg logs: SetLog) {
        val s = Session("s1", "2026-11-20", start, finishedAt = if (finished) start + 50 * MIN else null, itemType = ItemType.PROGRAM, programId = "p1", startedElapsed = anchor.elapsedMs)
        dao.insertSessionWithLogs(s, logs.toList(), emptyList())
    }

    private suspend fun row(id: String) = dao.allSetLogsOnce().first { it.id == id }

    @Test fun `a check stamps the first time and a second check keeps it`() = runBlocking {
        seed(false, log("a"))
        clocks.pass(10 * MIN)
        assertTrue(repo.checkSet("a"))
        assertEquals(SetStatus.DONE, row("a").status)
        assertEquals(at(18, 10), row("a").checkedAt)
        clocks.pass(5 * MIN)
        assertTrue(repo.checkSet("a"))
        assertEquals(at(18, 10), row("a").checkedAt)
    }

    @Test fun `a refused check writes no status and no time`() = runBlocking {
        seed(false, log("noReps", reps = null), log("noKg", weight = null), log("skipNoKg", weight = null, status = SetStatus.SKIPPED))
        clocks.pass(10 * MIN)
        for (id in listOf("noReps", "noKg", "skipNoKg")) {
            val before = row(id)
            assertFalse(id, repo.checkSet(id))
            assertEquals(id, before, row(id))
        }
    }

    @Test fun `a check in a finished session leaves the stamp as it is`() = runBlocking {
        seed(true, log("p"), log("sk", status = SetStatus.SKIPPED), log("odd", checkedAt = at(18, 20)))
        clocks.pass(60 * MIN)
        for (id in listOf("p", "sk")) {
            assertTrue(repo.checkSet(id))
            assertEquals(SetStatus.DONE, row(id).status)
            assertNull(id, row(id).checkedAt)
        }
        assertTrue(repo.checkSet("odd"))
        assertEquals(at(18, 20), row("odd").checkedAt)
    }

    @Test fun `every path to pending or skipped clears the stamp, and the next check stamps the new time`() = runBlocking {
        seed(false, log("a"), log("b"), log("c"))
        clocks.pass(5 * MIN)
        for (id in listOf("a", "b", "c")) assertTrue(repo.checkSet(id))
        repo.setSetStatus("a", SetStatus.PENDING)
        repo.setSetStatus("b", SetStatus.SKIPPED)
        assertNull(row("a").checkedAt)
        assertNull(row("b").checkedAt)
        assertEquals(at(18, 5), row("c").checkedAt)
        clocks.pass(10 * MIN)
        assertTrue(repo.checkSet("a"))
        assertEquals(at(18, 15), row("a").checkedAt)
        repo.setSetStatus("a", SetStatus.PENDING)
        repo.finishSession("s1")
        assertEquals(SetStatus.SKIPPED, row("a").status)
        assertNull(row("a").checkedAt)
        assertEquals(SetStatus.DONE, row("c").status)
        assertEquals(at(18, 5), row("c").checkedAt)
        assertEquals(SetStatus.SKIPPED, row("b").status)
    }

    @Test fun `a skipped set checked directly is stamped, a repeat keeps it, a refused one stays skipped without a stamp`() = runBlocking {
        seed(false, log("t"), log("w", weight = null))
        clocks.pass(8 * MIN)
        assertTrue(repo.checkSet("t"))
        assertEquals(at(18, 8), row("t").checkedAt)
        repo.setSetStatus("t", SetStatus.SKIPPED)
        assertNull(row("t").checkedAt)
        clocks.pass(10 * MIN)
        assertTrue(repo.checkSet("t"))
        assertEquals(SetStatus.DONE, row("t").status)
        assertEquals(at(18, 18), row("t").checkedAt)
        clocks.pass(10 * MIN)
        assertTrue(repo.checkSet("t"))
        assertEquals(at(18, 18), row("t").checkedAt)

        repo.setSetStatus("w", SetStatus.SKIPPED)
        assertFalse(repo.checkSet("w"))
        assertEquals(SetStatus.SKIPPED, row("w").status)
        assertNull(row("w").checkedAt)
    }

    @Test fun `weight and reps edits and an exercise swap keep the stamp`() = runBlocking {
        seed(false, log("a"))
        clocks.pass(7 * MIN)
        assertTrue(repo.checkSet("a"))
        repo.setSetWeight("a", 62.5)
        repo.setSetReps("a", 9)
        repo.swapExerciseInSession(row("a"), Exercise("ohp", "Overhead Press", Kind.STRENGTH), updateProgram = false)
        val after = row("a")
        assertEquals(62.5, after.weightKg!!, 0.0)
        assertEquals(9, after.reps)
        assertEquals("ohp", after.exerciseId)
        assertEquals(SetStatus.DONE, after.status)
        assertEquals(at(18, 7), after.checkedAt)
    }

    @Test fun `round times stay through value edits and the cardio Save`() = runBlocking {
        val s = Session("c1", "2026-11-20", start, itemType = ItemType.CARDIO, exerciseId = "row", exerciseName = "Rowing")
        dao.insertSessionWithLogs(s, emptyList(), listOf(IntervalLog("r0", "c1", 0, startedAt = at(18, 3), endedAt = at(18, 7))))
        dao.setIntervalAvgHr("r0", 150)
        dao.setIntervalWatts("r0", 210)
        repo.saveCardioEdits(s, s.copy(timeSec = 1_200), mapOf("r0" to IntervalEdit(avgHr = Edit.Set(152), watts = Edit.Set(205))))
        val r = dao.intervalLogsOnce().single()
        assertEquals(IntervalLog("r0", "c1", 0, avgHr = 152, watts = 205, startedAt = at(18, 3), endedAt = at(18, 7)), r)
    }

    @Test fun `a session prefilled from the last one starts with no stamps and leaves the old ones`() = runBlocking {
        dao.seedIfEmpty()
        val program = dao.firstActiveProgramId()!!
        val first = repo.startProgramSession(program, java.time.LocalDate.of(2026, 11, 20))!!
        assertEquals(clocks.elapsed, first.startedElapsed)
        assertEquals(clocks.wall, first.startedAt)
        val logs = dao.setLogsOnce(first.id)
        clocks.pass(10 * MIN)
        for (l in logs) {
            if (l.stepKg > 0 && l.weightKg == null) repo.setSetWeight(l.id, 40.0)
            assertTrue(repo.checkSet(l.id))
        }
        clocks.pass(5 * MIN)
        repo.finishSession(first.id)
        val oldRows = dao.setLogsOnce(first.id)
        assertTrue(oldRows.all { it.checkedAt == clocks.wall - 5 * MIN })
        clocks.pass(24 * 60 * MIN)
        val next = repo.startProgramSession(program, java.time.LocalDate.of(2026, 11, 21))!!
        val prefilled = dao.setLogsOnce(next.id)
        assertTrue(prefilled.isNotEmpty())
        assertTrue(prefilled.any { it.weightKg == 40.0 })
        assertTrue(prefilled.all { it.checkedAt == null && it.status == SetStatus.PENDING })
        assertEquals(oldRows, dao.setLogsOnce(first.id))
    }

    @Test fun `export and replace import keep every clock time`() = runBlocking {
        seed(false, log("a", status = SetStatus.DONE, checkedAt = at(18, 9)))
        dao.insertSessionWithLogs(
            Session("c1", "2026-11-20", start, finishedAt = at(18, 30), itemType = ItemType.CARDIO, timedFrom = at(18, 1), timedTo = at(18, 29)),
            emptyList(), listOf(IntervalLog("r0", "c1", 0, startedAt = at(18, 3), endedAt = at(18, 7))),
        )
        val logs = dao.allSetLogsOnce(); val rounds = dao.intervalLogsOnce()
        repo.importJson(repo.exportJson(), replace = true)
        assertEquals(logs, dao.allSetLogsOnce())
        assertEquals(rounds, dao.intervalLogsOnce())
        assertEquals(at(18, 1), dao.session("c1")!!.timedFrom)
        assertEquals(at(18, 29), dao.session("c1")!!.timedTo)
    }
}
