package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.health.HealthHeartSource
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The heart rate fill against Room with a fake source: the result of a read, the age rules, the commit that denies by
 * default, the cleanup, the explicit read and "Remove", the one runner and its triggers, and a missing provider.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HeartFillTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository
    private lateinit var fill: HeartFill
    private lateinit var scope: CoroutineScope
    private lateinit var heart: FakeHeart
    private lateinit var clocks: FakeClocks
    private var zone: ZoneId = TEST_ZONE
    private val gymRange = HrRange(at(18, 0), at(18, 40))
    private val json = Json { ignoreUnknownKeys = true }

    @Before fun open() {
        heart = FakeHeart()
        clocks = FakeClocks(at(19, 0))
        zone = TEST_ZONE
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        repo = KhonRepository(dao, clocks = clocks, zone = { zone }, requestFill = { fill.fillHeartRate() })
        fill = HeartFill(dao, heart, scope, { repo.generation }, clocks, { zone })
    }

    @After fun close() { scope.cancel(); db.close() }

    private fun fresh() { close(); open() }

    private fun range(day: LocalDate, fromH: Int = 18, minutes: Long = 40) = HrRange(at(fromH, 0, day), at(fromH, 0, day) + minutes * MIN)

    private suspend fun gym(id: String, r: HrRange = gymRange): Session {
        val s = Session(id, localDate(r.fromMs, TEST_ZONE), r.fromMs, finishedAt = r.toMs, itemType = ItemType.PROGRAM, programId = "p",
            timedFrom = r.fromMs, timedTo = r.toMs)
        dao.upsert(s)
        return s
    }

    private suspend fun row(id: String) = dao.sessionHr(id)
    private fun filledRow(id: String, r: HrRange = gymRange, avg: Int = 120) =
        SessionHr(id, r.fromMs, r.toMs, HrState.FILLED, "0:$avg", 1, 1, "mi", "mi", r.fromMs, r.fromMs, null, avg, avg, fetchedAt = at(18, 50))
    private fun waitingRow(id: String, r: HrRange = gymRange) =
        SessionHr(id, r.fromMs, r.toMs, HrState.WAITING, "0:120", 3, 3, "mi", "mi", r.fromMs, r.fromMs + 2 * MIN, 60, fetchedAt = at(18, 50))
    private val fails: suspend (HrRange) -> List<HeartRecord> = { error("provider failed") }

    private suspend fun awaitPasses(n: Int) { withTimeout(10_000) { fill.passes.first { it >= n } } }

    @Test fun `C23 a complete read writes only session_hr, the sessions and rounds stay byte-equal`() = runBlocking {
        gym("g")
        val cardio = Session("c", "2026-11-20", at(17, 0), finishedAt = at(17, 31), itemType = ItemType.CARDIO, exerciseId = "nor",
            timeSec = 1800, distanceM = 12000, avgHr = 150, watts = 210, timedFrom = at(17, 0), timedTo = at(17, 30))
        dao.insertSessionWithLogs(cardio, emptyList(), (0 until 4).map {
            IntervalLog("i$it", "c", it, avgHr = 160 + it, watts = 200 + it, startedAt = at(17, 1 + it * 7), endedAt = at(17, 5 + it * 7))
        })
        heart.answer = { everyMinute(it, 140) }
        val before = dao.sessionsOnce().sortedBy { it.id } to dao.intervalLogsOnce().sortedBy { it.id }
        fill.pass()
        assertEquals(HrState.FILLED, row("g")!!.state)
        assertEquals(140, row("c")!!.avgHr)
        assertEquals(HrRange(at(17, 0), at(17, 31)), HrRange(row("c")!!.fromMs, row("c")!!.toMs))
        assertEquals(before, dao.sessionsOnce().sortedBy { it.id } to dao.intervalLogsOnce().sortedBy { it.id })
    }

    @Test fun `C24 sparse waits and fills later, seven days is the waiting limit, a failed read changes nothing`() = runBlocking {
        gym("g")
        heart.answer = { sparse(it) }
        fill.pass()
        assertEquals(HrState.WAITING, row("g")!!.state)
        assertNull(row("g")!!.avgHr)
        assertEquals(3, row("g")!!.sampleCount)
        clocks.pass(60 * MIN)
        heart.answer = { everyMinute(it) }
        fill.pass()
        assertEquals(HrState.FILLED, row("g")!!.state)

        fresh()
        val end = gymRange.toMs
        gym("seven")
        gym("over", HrRange(gymRange.fromMs, end - 1))
        clocks.wall = end + WAITING_MS
        heart.answer = { sparse(it) }
        fill.pass()
        assertEquals(HrState.WAITING, row("seven")!!.state)
        assertEquals(HrState.NONE, row("over")!!.state)
        val waiting = row("seven")
        clocks.wall = end + 8 * DAY_MS
        heart.answer = fails
        heart.calls.clear()
        fill.pass()
        assertEquals(waiting, row("seven"))
        assertEquals(listOf(gymRange), heart.calls)
    }

    @Test fun `C25 a pass reads a range that ended 30 days ago exactly, not one just over or in the future`() = runBlocking {
        gym("edge")
        gym("old", HrRange(gymRange.fromMs, gymRange.toMs - 1))
        clocks.wall = gymRange.toMs + FILL_AGE_MS
        val today = LocalDate.of(2026, 12, 20)
        gym("future", range(today, 19))
        heart.answer = { everyMinute(it) }
        fill.pass()
        assertEquals(listOf(gymRange), heart.calls)
        assertEquals(HrState.FILLED, row("edge")!!.state)
        assertNull(row("old")); assertNull(row("future"))
    }

    @Test fun `C26 a result is dropped when the session, its range, the data or the row changed during the read`() = runBlocking {
        val cases: List<Pair<String, suspend (String) -> Unit>> = listOf(
            "deleted" to { _ -> repo.deleteSession("g") },
            "date moved" to { _ -> repo.setSessionDate("g", LocalDate.of(2026, 11, 21)) },
            "newer timer run" to { _ -> dao.setTimerClock("g", gymRange.fromMs, gymRange.toMs - 5 * MIN, 2) },
            "replace" to { file -> repo.importJson(file, replace = true) },
            "merge" to { file -> repo.importJson(file, replace = false) },
            "remove" to { _ -> assertTrue(fill.removeHeartRate("g")) },
        )
        for ((name, change) in cases) {
            fresh()
            gym("g")
            val file = repo.exportJson()
            heart.answer = { everyMinute(it) }
            heart.hold()
            val read = async { fill.pass() }
            heart.reached.await()
            change(file)
            heart.release()
            read.await()
            if (name == "remove") assertEquals(name, HrState.OFF, row("g")!!.state) else assertNull(name, row("g"))
        }
    }

    @Test fun `AT7 a zone change that moves both days drops a held result`() = runBlocking {
        val r = HrRange(at(0, 15), at(0, 55))
        dao.upsert(Session("z", "2026-11-20", at(0, 10), finishedAt = r.toMs, itemType = ItemType.PROGRAM, timedFrom = r.fromMs, timedTo = r.toMs))
        heart.answer = { everyMinute(it) }
        heart.hold()
        val read = async { fill.rereadHeartRate("z") }
        heart.reached.await()
        zone = ZoneOffset.UTC
        heart.release()
        assertEquals(HrReread.DROPPED, read.await())
        assertNull(row("z"))
        assertNull(hrWindow(dao.session("z")!!, ZoneOffset.UTC))
    }

    @Test fun `C27 the pass deletes stale rows without a read, keeps off rows, and checks again in its delete`() = runBlocking {
        gym("lost"); dao.upsertSessionHr(filledRow("lost")); repo.setSessionDate("lost", LocalDate.of(2026, 11, 22))
        gym("moved"); dao.upsertSessionHr(filledRow("moved")); dao.setTimerClock("moved", gymRange.fromMs, gymRange.toMs - 5 * MIN, 1)
        gym("off"); val off = filledRow("off").copy(state = HrState.OFF, samples = "", avgHr = null, maxHr = null)
        dao.upsertSessionHr(off); dao.setTimerClock("off", gymRange.fromMs, gymRange.toMs - 5 * MIN, 1)
        heart.answer = fails
        fill.pass()
        assertNull(row("lost")); assertNull(row("moved"))
        assertEquals(off, row("off"))
        assertEquals(listOf(HrRange(gymRange.fromMs, gymRange.toMs - 5 * MIN)), heart.calls)
        assertNull(row("moved"))
    }

    @Test fun `C27 AT4 a held cleanup keeps a row an explicit reread or Remove committed meanwhile`() = runBlocking {
        for (remove in listOf(false, true)) {
            fresh()
            gym("g"); dao.upsertSessionHr(filledRow("g"))
            val now = HrRange(gymRange.fromMs, gymRange.toMs - 5 * MIN)
            dao.setTimerClock("g", now.fromMs, now.toMs, 2)
            val reached = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            fill.beforeCleanup = { reached.complete(Unit); release.await() }
            heart.answer = { everyMinute(it, 150) }
            val pass = async { fill.pass() }
            reached.await()
            if (remove) assertTrue(fill.removeHeartRate("g")) else assertEquals(HrReread.FILLED, fill.rereadHeartRate("g"))
            heart.answer = fails
            release.complete(Unit)
            pass.await()
            val kept = row("g")!!
            assertEquals(now, HrRange(kept.fromMs, kept.toMs))
            if (remove) {
                assertEquals(HrState.OFF, kept.state)
            } else {
                val facts = hrFacts(dao.session("g")!!, kept, TEST_ZONE)
                assertTrue(facts is HrFacts.Filled)
                assertEquals(150, (facts as HrFacts.Filled).row.avgHr)
            }
        }
    }

    @Test fun `C28 the explicit read replaces only on a commit, reads any row and age, and two calls share one read`() = runBlocking {
        gym("g"); val old = filledRow("g"); dao.upsertSessionHr(old)
        heart.answer = fails
        assertEquals(HrReread.FAILED, fill.rereadHeartRate("g"))
        assertEquals(old, row("g"))
        heart.answer = { everyMinute(it, 150) }
        assertEquals(HrReread.FILLED, fill.rereadHeartRate("g"))
        assertEquals(150, row("g")!!.avgHr)
        assertTrue(fill.removeHeartRate("g"))
        assertEquals(HrReread.FILLED, fill.rereadHeartRate("g"))
        dao.upsertSessionHr(row("g")!!.copy(state = HrState.NONE, avgHr = null, maxHr = null))
        heart.answer = { sparse(it) }
        assertEquals(HrReread.WAITING, fill.rereadHeartRate("g"))

        val oldDay = LocalDate.of(2026, 9, 1)
        gym("aged", range(oldDay))
        heart.answer = { everyMinute(it) }
        assertEquals(HrReread.FILLED, fill.rereadHeartRate("aged"))
        heart.answer = { sparse(it) }
        assertEquals(HrReread.NONE, fill.rereadHeartRate("aged"))

        dao.upsert(Session("open", "2026-11-20", at(18, 0), itemType = ItemType.PROGRAM))
        assertEquals(HrReread.NO_RANGE, fill.rereadHeartRate("open"))
        assertEquals(HrReread.NO_RANGE, fill.rereadHeartRate("missing"))
        assertNull(row("open"))

        heart.calls.clear()
        heart.answer = { everyMinute(it) }
        heart.hold()
        val a = async { fill.rereadHeartRate("g") }
        val b = async { fill.rereadHeartRate("g") }
        heart.reached.await()
        heart.release()
        assertEquals(HrReread.FILLED, a.await())
        assertEquals(HrReread.FILLED, b.await())
        assertEquals(1, heart.calls.size)
    }

    @Test fun `C30 a pass and an explicit read never read at the same time`() = runBlocking {
        gym("a"); gym("b", HrRange(at(17, 0), at(17, 40)))
        heart.answer = { everyMinute(it) }
        heart.hold()
        val pass = async { fill.pass() }
        heart.reached.await()
        val explicit = async { fill.rereadHeartRate("a") }
        delay(200)
        assertEquals(1, heart.calls.size)
        heart.release()
        pass.await(); explicit.await()
        assertEquals(1, heart.maxActive)
    }

    @Test fun `C29 Remove turns a row off, writes an off row for a range, and nothing without one`() = runBlocking {
        val session = gym("g"); val old = filledRow("g"); dao.upsertSessionHr(old)
        assertTrue(fill.removeHeartRate("g"))
        val off = row("g")!!
        assertEquals(HrState.OFF, off.state)
        assertEquals("", off.samples)
        assertNull(off.avgHr); assertNull(off.maxHr); assertNull(off.origin); assertEquals(0, off.sampleCount)
        assertTrue(off.fetchedAt > old.fetchedAt)
        assertEquals(session, dao.session("g"))
        heart.answer = { everyMinute(it) }
        fill.pass()
        assertTrue(heart.calls.isEmpty())
        assertEquals(off, row("g"))

        gym("bare")
        assertTrue(fill.removeHeartRate("bare"))
        assertEquals(SessionHr("bare", gymRange.fromMs, gymRange.toMs, HrState.OFF, fetchedAt = clocks.wall), row("bare"))
        dao.upsert(Session("none", "2026-11-20", at(18, 0), finishedAt = at(18, 40), itemType = ItemType.PROGRAM))
        assertFalse(fill.removeHeartRate("none"))
        assertNull(row("none"))
        assertFalse(fill.removeHeartRate("missing"))
    }

    @Test fun `C30 AT30 Finish and Save return during a read, the extra pass reads both, foreground counts elapsed time`() = runBlocking {
        gym("s1")
        val anchorElapsed = clocks.elapsed - 40 * MIN
        dao.insertSessionWithLogs(
            Session("s2", "2026-11-20", at(18, 20), itemType = ItemType.PROGRAM, programId = "p", startedElapsed = anchorElapsed),
            listOf(SetLog("x", "s2", 0, false, 0, 0, 0, "bench", "Bench", 6, 10, 2.5, 60.0, 8, SetStatus.DONE, at(18, 55))), emptyList(),
        )
        val s3 = Session("s3", "2026-11-20", at(18, 10), itemType = ItemType.CARDIO, exerciseId = "nor", timeSec = 2400, timedFrom = at(18, 10), timedTo = at(18, 50))
        dao.upsert(s3)
        heart.answer = { everyMinute(it) }
        heart.hold()
        fill.fillHeartRate()
        withTimeout(10_000) { heart.reached.await() }
        withTimeout(2_000) { repo.finishSession("s2") }
        assertTrue(dao.session("s2")!!.finished)
        withTimeout(2_000) { assertEquals(CardioSaveResult.Saved, repo.saveCardioEdits(s3, s3.copy(effort = 7), emptyMap(), at(18, 51))) }
        fill.foreground()
        assertEquals(0, fill.passes.value)
        assertEquals(1, heart.calls.size)
        heart.release()
        awaitPasses(2)
        delay(200)
        assertEquals(2, fill.passes.value)
        assertEquals(gymRange, heart.calls[0])
        assertEquals(setOf(HrRange(at(18, 20), at(19, 0)), HrRange(at(18, 10), at(18, 51))), heart.calls.drop(1).toSet())
        assertEquals(3, heart.calls.size)
        assertEquals(1, heart.maxActive)
        assertEquals(HrState.FILLED, row("s2")!!.state)
        assertEquals(HrState.FILLED, row("s3")!!.state)

        clocks.moveWall(60 * MIN)
        clocks.elapsed += 5 * MIN
        fill.foreground()
        delay(300)
        assertEquals(2, fill.passes.value)
        clocks.elapsed += 5 * MIN
        fill.foreground()
        awaitPasses(3)
    }

    @Test fun `C31 a missing provider or permission writes nothing and the next trigger after it returns fills`() = runBlocking {
        gym("g")
        val real = HeartFill(dao, HealthHeartSource(ApplicationProvider.getApplicationContext()), scope, { repo.generation }, clocks, { zone })
        assertEquals(HrReread.FAILED, real.rereadHeartRate("g"))
        real.pass()
        assertNull(row("g"))
        assertEquals("android.permission.health.READ_HEART_RATE", HealthHeartSource.HEART_READ)

        heart.answer = { error("no heart rate permission") }
        fill.fillHeartRate()
        awaitPasses(1)
        assertNull(row("g"))
        heart.answer = { everyMinute(it) }
        fill.fillHeartRate()
        awaitPasses(2)
        assertEquals(HrState.FILLED, row("g")!!.state)
    }

    @Test fun `AT23 a held read loses to a Merge that brings another row, the explicit call reports it`() = runBlocking {
        gym("g"); val waiting = waitingRow("g"); dao.upsertSessionHr(waiting)
        val file = json.decodeFromString<Backup>(repo.exportJson())
        val other = json.encodeToString(file.copy(sessionHr = listOf(filledRow("g", avg = 99))))
        heart.answer = { everyMinute(it) }
        for (explicit in listOf(false, true)) {
            heart.hold()
            val read = async { if (explicit) fill.rereadHeartRate("g") else { fill.pass(); null } }
            heart.reached.await()
            repo.importJson(other, replace = false)
            heart.release()
            val result = read.await()
            if (explicit) assertEquals(HrReread.DROPPED, result)
            assertEquals(waiting, row("g"))
        }
        assertEquals(2, heart.calls.size)
    }

    @Test fun `AT29 Remove beats a held read, the pass skips the off row, an explicit read fills it again`() = runBlocking {
        gym("g"); dao.upsertSessionHr(waitingRow("g"))
        heart.answer = { everyMinute(it) }
        heart.hold()
        val read = async { fill.pass() }
        heart.reached.await()
        assertTrue(fill.removeHeartRate("g"))
        heart.release()
        read.await()
        assertEquals(HrState.OFF, row("g")!!.state)
        fill.pass()
        assertEquals(1, heart.calls.size)
        assertEquals(HrState.OFF, row("g")!!.state)
        assertEquals(HrReread.FILLED, fill.rereadHeartRate("g"))
        assertEquals(HrState.FILLED, row("g")!!.state)
        assertNotNull(row("g")!!.avgHr)
    }
}
