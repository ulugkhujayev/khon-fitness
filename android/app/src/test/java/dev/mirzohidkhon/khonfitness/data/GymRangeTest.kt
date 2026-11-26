package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gym Finish stores `[startedAt, min(finishedAt, last stamp + 5 min))` only with a verified stamp and clocks in step at
 * Finish; "Use this time range" stores the shown bounds only while they are still the session's; later edits never
 * change a stored range; deleting a session deletes its heart rate row.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class GymRangeTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository
    private val start = at(18, 0)
    private val clocks = FakeClocks(start)
    private val startElapsed = clocks.elapsed
    private var fills = 0

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        repo = KhonRepository(dao, clocks = clocks, zone = { TEST_ZONE }, requestFill = { fills++ })
    }

    @After fun close() = db.close()

    private fun log(id: String, order: Int, checkedAt: Long? = null, status: String = SetStatus.PENDING, session: String = "g") =
        SetLog(id, session, 0, false, 0, order, order, "bench", "Bench Press", 6, 10, 2.5, 60.0, 8, status, checkedAt)

    private suspend fun gym(id: String = "g", anchored: Boolean = true, vararg logs: SetLog): Session {
        val s = Session(id, "2026-11-20", start, itemType = ItemType.PROGRAM, programId = "p1", startedElapsed = if (anchored) startElapsed else null)
        dao.insertSessionWithLogs(s, logs.toList(), emptyList())
        return s
    }

    private suspend fun session(id: String = "g") = dao.session(id)!!

    /** True time moves to [trueWall]; the phone clock keeps an offset the test gave it. */
    private fun passTo(trueWall: Long) { clocks.pass(trueWall - (start + (clocks.elapsed - startElapsed))) }

    private suspend fun checkAt(trueWall: Long, id: String) { passTo(trueWall); assertTrue(id, repo.checkSet(id)) }

    @Test fun `Finish stores the range from the start to the last stamp plus five minutes, at most the finish`() = runBlocking {
        gym("g", true, log("a", 0), log("b", 1), log("c", 2))
        checkAt(at(18, 10), "a"); checkAt(at(18, 40), "b")
        passTo(at(18, 42)); repo.finishSession("g")
        assertEquals(start, session().timedFrom)
        assertEquals(at(18, 42), session().timedTo)
        assertEquals(SetStatus.SKIPPED, dao.setLogsOnce("g")[2].status)
        assertEquals(1, fills)

        gym("late", true, log("x", 0, session = "late"), log("y", 1, session = "late"))
        clocks.elapsed = startElapsed; clocks.wall = start
        checkAt(at(18, 10), "x"); checkAt(at(18, 40), "y")
        passTo(at(19, 30)); repo.finishSession("late")
        assertEquals(start, session("late").timedFrom)
        assertEquals(at(18, 45), session("late").timedTo)
    }

    @Test fun `a stamp outside the session is ignored, no done set gives no range, a second Finish changes nothing`() = runBlocking {
        gym("g", true, log("early", 0, checkedAt = at(17, 50), status = SetStatus.DONE), log("a", 1))
        checkAt(at(18, 20), "a")
        passTo(at(18, 50)); repo.finishSession("g")
        assertEquals(start, session().timedFrom)
        assertEquals(at(18, 25), session().timedTo)
        val once = session()
        passTo(at(19, 10)); repo.setSetStatus("a", SetStatus.PENDING); val afterUncheck = dao.setLogsOnce("g")
        repo.finishSession("g")
        assertEquals(once, session())
        assertEquals(afterUncheck, dao.setLogsOnce("g"))
        assertEquals(1, fills)

        gym("none", true, log("p", 0, session = "none"))
        repo.finishSession("none")
        assertTrue(session("none").finished)
        assertNull(session("none").timedFrom); assertNull(session("none").timedTo)
    }

    @Test fun `a check while the clocks are not in step has no stamp and Finish then stores no range`() = runBlocking {
        gym("g", true, log("a", 0), log("b", 1))
        checkAt(at(18, 8), "a")
        passTo(at(18, 10)); clocks.moveWall(40 * MIN)
        checkAt(at(18, 15), "b")
        assertEquals(at(18, 8), dao.setLogsOnce("g")[0].checkedAt)
        assertNull(dao.setLogsOnce("g")[1].checkedAt)
        assertEquals(SetStatus.DONE, dao.setLogsOnce("g")[1].status)
        passTo(at(18, 17)); repo.finishSession("g")
        assertEquals(at(18, 57), session().finishedAt)
        assertNull(session().timedFrom); assertNull(session().timedTo)
        assertEquals(listOf(SetStatus.DONE, SetStatus.DONE), dao.setLogsOnce("g").map { it.status })
    }

    @Test fun `a clock moved back before Finish gives the range to the last in-step stamp plus five minutes`() = runBlocking {
        gym("g", true, log("a", 0), log("b", 1))
        checkAt(at(18, 8), "a")
        passTo(at(18, 10)); clocks.moveWall(40 * MIN)
        checkAt(at(18, 15), "b")
        clocks.moveWall(-40 * MIN)
        passTo(at(18, 30)); repo.finishSession("g")
        assertEquals(start, session().timedFrom)
        assertEquals(at(18, 13), session().timedTo)
    }

    @Test fun `a session without an anchor or after a reboot is saved with no stamp and no range`() = runBlocking {
        gym("old", false, log("a", 0, session = "old"))
        checkAt(at(18, 20), "a")
        assertNull(dao.setLogsOnce("old")[0].checkedAt)
        passTo(at(18, 40)); repo.finishSession("old")
        assertTrue(session("old").finished)
        assertNull(session("old").timedFrom)

        clocks.wall = start; clocks.elapsed = startElapsed
        gym("boot", true, log("b", 0, session = "boot"), log("c", 1, session = "boot"))
        checkAt(at(18, 10), "b")
        clocks.wall = at(18, 20); clocks.elapsed = 30_000L
        assertTrue(repo.checkSet("c"))
        assertNull(dao.setLogsOnce("boot")[1].checkedAt)
        clocks.wall = at(18, 30); clocks.elapsed = 40 * MIN
        repo.finishSession("boot")
        assertEquals(at(18, 30), session("boot").finishedAt)
        assertNull(session("boot").timedFrom)
        assertEquals(listOf(SetStatus.DONE, SetStatus.DONE), dao.setLogsOnce("boot").map { it.status })
    }

    @Test fun `edits after Finish never change the stored range`() = runBlocking {
        gym("g", true, log("a", 0), log("b", 1))
        checkAt(at(18, 10), "a"); checkAt(at(18, 30), "b")
        passTo(at(18, 40)); repo.finishSession("g")
        val range = session().timedFrom to session().timedTo
        assertEquals(start to at(18, 35), range)
        repo.setSetStatus("b", SetStatus.PENDING)
        repo.setSetReps("a", 12)
        dao.upsert(log("added", 2, status = SetStatus.DONE, checkedAt = at(18, 39)))
        repo.setSessionNote("g", "edited"); repo.setSessionEffort("g", 8)
        passTo(at(19, 0)); repo.finishSession("g")
        assertEquals(range, session().timedFrom to session().timedTo)
    }

    @Test fun `AT1 a forward clock during the session leaves both checks and no range or heart rate`() = runBlocking {
        gym("g", true, log("a", 0), log("b", 1))
        checkAt(at(18, 8), "a")
        passTo(at(18, 10)); clocks.moveWall(40 * MIN)
        checkAt(at(18, 15), "b")
        passTo(at(18, 17)); repo.finishSession("g")
        clocks.moveWall(-40 * MIN)
        val s = session()
        assertEquals(listOf(SetStatus.DONE, SetStatus.DONE), dao.setLogsOnce("g").map { it.status })
        assertNull(dao.setLogsOnce("g")[1].checkedAt)
        assertNull(hrWindow(s, TEST_ZONE))
        val offered = SessionHr("g", at(18, 24), at(18, 54), HrState.FILLED, "0:130,5:131", 2, 2, "mi", "mi", avgHr = 130, maxHr = 131, fetchedAt = at(19, 0))
        dao.upsertSessionHr(offered)
        assertEquals(HrFacts.NoRange, hrFacts(s, dao.sessionHr("g"), TEST_ZONE))
    }

    private suspend fun finished(id: String, minutes: Long, date: String = "2026-11-20", itemType: String = ItemType.PROGRAM, range: Boolean = false): Session {
        val s = Session(id, date, start, finishedAt = start + minutes * MIN, itemType = itemType,
            timedFrom = if (range) start else null, timedTo = if (range) start + minutes * MIN else null)
        dao.upsert(s)
        return s
    }

    @Test fun `confirmRange stores the shown bounds of a finished gym session without a range and refuses every other one`() = runBlocking {
        val ok = finished("ok", 40)
        assertEquals(HrRange(start, at(18, 40)), confirmableBounds(ok, TEST_ZONE))
        assertEquals(ConfirmRangeResult.Confirmed, repo.confirmRange("ok", start, at(18, 40)))
        assertEquals(start, session("ok").timedFrom); assertEquals(at(18, 40), session("ok").timedTo)
        assertEquals(1, fills)

        val cases = listOf(
            finished("short", 9) to RangeRefusal.LENGTH,
            finished("long", 241) to RangeRefusal.LENGTH,
            finished("moved", 40, date = "2026-11-21") to RangeRefusal.DATE_MOVED,
            finished("has", 40, range = true) to RangeRefusal.HAS_RANGE,
            finished("cardio", 40, itemType = ItemType.CARDIO) to RangeRefusal.NOT_GYM,
        )
        dao.upsert(Session("open", "2026-11-20", start, itemType = ItemType.PROGRAM))
        val open = dao.session("open")!!
        for ((s, reason) in cases + (open to RangeRefusal.NOT_FINISHED)) {
            assertNull(s.id, confirmableBounds(s, TEST_ZONE))
            assertEquals(s.id, ConfirmRangeResult.Refused(reason), repo.confirmRange(s.id, s.startedAt, s.finishedAt ?: at(18, 40)))
            assertEquals(s.id, s, session(s.id))
        }
        assertEquals(ConfirmRangeResult.Refused(RangeRefusal.MISSING), repo.confirmRange("gone", start, at(18, 40)))
        assertEquals(ConfirmRangeResult.Refused(RangeRefusal.HAS_RANGE), repo.confirmRange("ok", start, at(18, 40)))
        assertEquals(1, fills)
        assertEquals(listOf("ok"), dao.sessionsOnce().filter { it.timedFrom != null && it.id != "has" }.map { it.id })
    }

    @Test fun `AT22 a confirmation of bounds a merge changed is refused, the new bounds are accepted`() = runBlocking {
        finished("g", 40)
        val shown = confirmableBounds(session(), TEST_ZONE)!!
        assertEquals(HrRange(at(18, 0), at(18, 40)), shown)
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
        val file = json.decodeFromString<Backup>(repo.exportJson())
        val moved = file.copy(sessions = file.sessions.map { it.copy(startedAt = at(19, 0), finishedAt = at(19, 40)) })
        repo.importJson(json.encodeToString(moved), replace = false)
        assertEquals(ConfirmRangeResult.Refused(RangeRefusal.BOUNDS_CHANGED), repo.confirmRange("g", shown.fromMs, shown.toMs))
        assertNull(session().timedFrom)
        assertEquals(0, fills)
        val fresh = confirmableBounds(session(), TEST_ZONE)!!
        assertEquals(HrRange(at(19, 0), at(19, 40)), fresh)
        assertEquals(ConfirmRangeResult.Confirmed, repo.confirmRange("g", fresh.fromMs, fresh.toMs))
        assertEquals(at(19, 0) to at(19, 40), session().timedFrom to session().timedTo)
        assertEquals(1, fills)
    }

    private fun hr(id: String) = SessionHr(id, start, at(18, 40), HrState.WAITING, "", 3, 3, "mi", "mi", fetchedAt = at(19, 0))

    @Test fun `the heart rate row goes with its session by every delete path and stays through upserts and edits`() = runBlocking {
        val fileWithoutRows = repo.exportJson()
        finished("kept", 40, range = true); dao.upsertSessionHr(hr("kept"))
        dao.upsert(session("kept").copy(note = "upsert"))
        repo.setSessionDate("kept", java.time.LocalDate.of(2026, 11, 20)); repo.setSessionNote("kept", "edit")
        repo.saveCardioEdits(session("kept"), session("kept").copy(effort = 5), emptyMap())
        repo.importJson(repo.exportJson(), replace = false)
        assertEquals(hr("kept"), dao.sessionHr("kept"))

        finished("del", 40, range = true); dao.upsertSessionHr(hr("del"))
        repo.deleteSession("del")
        assertNull(dao.sessionHr("del"))
        assertNull(dao.session("del"))

        repo.importJson(fileWithoutRows, replace = true)
        assertTrue(dao.sessionHrOnce().isEmpty())

        dao.upsertSessionHr(hr("kept"))
        repo.resetToSeed()
        assertTrue(dao.sessionHrOnce().isEmpty())
    }
}
