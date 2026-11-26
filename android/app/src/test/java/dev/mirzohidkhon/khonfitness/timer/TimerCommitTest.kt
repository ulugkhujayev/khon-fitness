package dev.mirzohidkhon.khonfitness.timer

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.data.FakeClocks
import dev.mirzohidkhon.khonfitness.data.IntervalLog
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.KhonDao
import dev.mirzohidkhon.khonfitness.data.KhonDatabase
import dev.mirzohidkhon.khonfitness.data.KhonRepository
import dev.mirzohidkhon.khonfitness.data.MIN
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.TEST_ZONE
import dev.mirzohidkhon.khonfitness.data.TimerCommit
import dev.mirzohidkhon.khonfitness.data.TimerControl
import dev.mirzohidkhon.khonfitness.data.at
import dev.mirzohidkhon.khonfitness.data.hrWindow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The end of a cardio timer run against Room and the snapshot store: the run number at Start, `commitTimerRun`, the
 * snapshot rules, the run of an older version, and Replace and reset against the timer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TimerCommitTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val start = at(18, 0)
    private lateinit var clocks: FakeClocks
    private val timer = FakeTimer()

    /** Records what Replace and reset ask of the timer; [clear] and [resume] act on the real snapshot store. */
    inner class FakeTimer : TimerControl {
        val calls = mutableListOf<String>()
        override suspend fun halt() { calls += "halt" }
        override fun clear() { calls += "clear"; TimerSnapshots.clearAll(context) }
        override fun resume() { calls += "resume" }
    }

    @Before fun open() {
        clocks = FakeClocks(start)
        timer.calls.clear()
        db = Room.inMemoryDatabaseBuilder(context, KhonDatabase::class.java).allowMainThreadQueries().build()
        dao = db.dao()
        repo = newRepo()
        TimerSnapshots.clearAll(context)
        runBlocking {
            repo.seedIfEmpty()
            dao.insertSessionWithLogs(
                Session("c", "2026-11-20", at(17, 59), itemType = ItemType.CARDIO, exerciseId = "nor", exerciseName = "Norwegian 4x4"),
                emptyList(), (0 until 4).map { IntervalLog("i$it", "c", it) },
            )
        }
    }

    @After fun close() { db.close(); TimerSnapshots.clearAll(context) }

    /** One fixture per case of a loop: the database of the case before is closed first. */
    private fun fresh() { close(); open() }

    /** A new repository over the same database: the process after a restart, generation 0. */
    private fun newRepo() = KhonRepository(dao, clocks = clocks, zone = { TEST_ZONE }, timer = timer)

    private fun ready(runId: String = "r1") =
        TimerState("c", "Norwegian 4x4", buildPhases(60, 240, 180, 4), remaining = 60.0, awaitingStart = true, runId = runId)

    /** The service's Start: the number from the session, then the anchor of that instant. */
    private suspend fun TimerState.started(c: FakeClocks = clocks) = beginRun(repo.startTimerRun(sessionId), c.wall, c.elapsed)

    private fun TimerState.run(c: FakeClocks, seconds: Double, step: Double = 1.0): TimerState {
        var s = this
        var left = seconds
        while (left > 1e-9 && !s.done) {
            val dt = minOf(step, left)
            c.pass(Math.round(dt * 1000)); s = s.tickAt(dt, c.elapsed); left -= dt
        }
        return s
    }

    /** The service's end of a run: the done snapshot is saved, then the commit and its cleanup run. */
    private fun TimerState.ended(c: FakeClocks = clocks) = finishedAt(c.wall, c.elapsed).also { TimerSnapshots.save(context, it, c.wall) }
    private suspend fun TimerState.commit(r: KhonRepository = repo, generation: Long? = r.generation) = recordRun(r, context, this, generation)

    private suspend fun full(runId: String = "r1", c: FakeClocks = clocks) = ready(runId).started(c).run(c, 1563.0).ended(c)
    private suspend fun session() = dao.session("c")!!
    private suspend fun rounds() = dao.intervalLogsFor("c").map { it.startedAt to it.endedAt }
    private fun snapshot() = TimerSnapshots.load(context)?.state
    private fun pendingIds() = TimerSnapshots.pending(context).map { it.runId }

    /** Holds the first commit after it read its generation, until [release]. */
    private inner class Hold {
        val reached = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        private var first = true
        init {
            repo.beforeTimerCommit = { if (first) { first = false; reached.complete(Unit); release.await() } }
        }
    }

    private fun failRoundWrites() = db.openHelper.writableDatabase.execSQL(
        "CREATE TRIGGER fail_rounds BEFORE UPDATE OF endedAt ON interval_logs BEGIN SELECT RAISE(ABORT, 'held'); END")
    private fun failProgramInserts() = db.openHelper.writableDatabase.execSQL(
        "CREATE TRIGGER fail_programs BEFORE INSERT ON programs BEGIN SELECT RAISE(ABORT, 'held'); END")
    private fun drop(trigger: String) = db.openHelper.writableDatabase.execSQL("DROP TRIGGER $trigger")

    @Test fun `C9 C14 Start numbers each run and the commit writes duration, range and round times by index`() = runBlocking {
        val run = full()
        assertEquals(1, session().timerRun)
        assertEquals(TimerCommit.COMMITTED, run.commit())
        val s = session()
        assertEquals(1560, s.timeSec)
        assertEquals(start, s.timedFrom)
        assertEquals(start + 1_563_000, s.timedTo)
        assertEquals(1, s.timerDone)
        rounds().forEachIndexed { i, (a, b) ->
            assertEquals(start + (63 + i * 420) * 1000L, a)
            assertEquals(240_000L, b!! - a!!)
        }
        assertNull(snapshot())
        ready("r2").started()
        assertEquals(2, session().timerRun)
    }

    @Test fun `C14 rounds that did not run get null, and a deleted session writes nothing and clears the snapshot`() = runBlocking {
        val stopped = ready().started().run(clocks, 700.0).ended()
        assertEquals(TimerCommit.COMMITTED, stopped.commit())
        assertEquals(listOf(true, true, false, false), rounds().map { it.first != null })
        assertEquals(start + 700_000, rounds()[1].second)

        val next = ready("r2").started().run(clocks, 100.0).ended()
        repo.deleteSession("c")
        assertEquals(TimerCommit.GONE, next.commit())
        assertNull(dao.session("c"))
        assertNull(snapshot())
        assertEquals(emptyList<String>(), pendingIds())
    }

    @Test fun `C15 a held older run never clears the newer snapshot and never writes after the newer run started`() = runBlocking {
        for (variant in listOf("trusted", "untrusted", "typed", "clock back")) {
            fresh()
            val hold = Hold()
            val r1 = full("r1")
            val late = async { r1.commit() }
            hold.reached.await()
            if (variant == "clock back") clocks.moveWall(-3 * 60 * MIN)
            var r2 = ready("r2").started().run(clocks, 600.0)
            TimerSnapshots.save(context, r2, clocks.wall)
            if (variant == "untrusted") clocks.moveWall(20 * MIN)
            if (variant == "typed") repo.setSessionTime("c", 1500)
            r2 = r2.run(clocks, 1000.0).ended()
            if (variant == "trusted" || variant == "clock back") {
                hold.release.complete(Unit)
                assertEquals(variant, TimerCommit.DROPPED, late.await())
                assertEquals(variant, listOf("r2"), pendingIds())
            }
            assertEquals(variant, TimerCommit.COMMITTED, r2.commit())
            val after = session()
            hold.release.complete(Unit)
            assertEquals(variant, TimerCommit.DROPPED, late.await())
            assertEquals(variant, TimerCommit.DROPPED, r2.commit())
            assertEquals(variant, after, session())
            assertEquals(variant, 2, after.timerRun)
            assertEquals(variant, 2, after.timerDone)
            assertEquals(variant, if (variant == "typed") 1500 else 1560, after.timeSec)
            if (variant == "untrusted") {
                assertNull(after.timedFrom); assertNull(after.timedTo); assertTrue(rounds().all { it.first == null })
            } else assertEquals(variant, r2.clock!!.anchorWall, after.timedFrom)
        }
    }

    @Test fun `C16 AT26 a failed commit leaves nothing partial, and the next start keeps the typed duration`() = runBlocking {
        val run = ready().started().run(clocks, 1200.0).ended()
        failRoundWrites()
        assertEquals(TimerCommit.FAILED, run.commit())
        session().let { assertNull(it.timeSec); assertNull(it.timedFrom); assertNull(it.timerDone) }
        assertTrue(rounds().all { it.first == null })
        assertEquals(listOf("r1"), pendingIds())
        drop("fail_rounds")
        val opened = session()
        repo.saveCardioEdits(opened, opened.copy(timeSec = 1500), emptyMap(), clocks.wall)
        val restarted = newRepo()
        assertEquals(listOf(TimerCommit.COMMITTED), commitPending(restarted, context))
        assertEquals(emptyList<TimerCommit>(), commitPending(restarted, context))
        assertEquals(TimerCommit.DROPPED, run.commit(restarted))
        val s = session()
        assertEquals(1500, s.timeSec)
        assertEquals(start, s.timedFrom)
        assertEquals(start + 1_200_000, s.timedTo)
        assertEquals(1, s.timerDone)
        assertNull(snapshot())
        assertEquals(emptyList<String>(), pendingIds())
    }

    @Test fun `X5 results refused for good leave the pending store without a write, one refused by the generation stays`() = runBlocking {
        for (id in listOf("g", "n", "d")) dao.insertSessionWithLogs(
            Session(id, "2026-11-20", at(17, 59), itemType = ItemType.CARDIO, exerciseId = "nor", exerciseName = "Norwegian 4x4"),
            emptyList(), (0 until 4).map { IntervalLog("$id-i$it", id, it) },
        )
        fun readyFor(id: String) = ready("${id}1").copy(sessionId = id)
        readyFor("c").started().run(clocks, 1563.0).ended()
        readyFor("g").started().run(clocks, 100.0).ended()
        repo.deleteSession("g")
        readyFor("n").started().run(clocks, 100.0).ended()
        ready("n2").copy(sessionId = "n").started()
        val d1 = readyFor("d").started().run(clocks, 100.0).ended()
        assertEquals(TimerCommit.COMMITTED, d1.commit())
        TimerSnapshots.save(context, d1, clocks.wall, clocks.elapsed)
        assertEquals(listOf("c1", "g1", "n1", "d1"), pendingIds())
        val n = dao.session("n")
        val d = dao.session("d")

        val backup = repo.exportJson()
        val hold = Hold()
        val first = async { commitPending(repo, context) }
        hold.reached.await()
        repo.importJson(backup, replace = false)
        hold.release.complete(Unit)
        assertEquals(listOf(TimerCommit.RETRY, TimerCommit.GONE, TimerCommit.DROPPED, TimerCommit.DROPPED), first.await())
        assertEquals(listOf("c1"), pendingIds())
        assertNull(dao.session("g"))
        assertEquals(n, dao.session("n"))
        assertEquals(d, dao.session("d"))
        assertNull(session().timerDone)

        assertEquals(listOf(TimerCommit.COMMITTED), commitPending(repo, context))
        assertEquals(1, session().timerDone)
        assertEquals(start, session().timedFrom)
        assertEquals(emptyList<String>(), pendingIds())
    }

    @Test fun `C16a the duration goes only over the value noted at Start`() = runBlocking {
        full().commit()
        assertEquals(1560, session().timeSec)

        dao.setSessionTime("c", 1200)
        ready("r2").started().run(clocks, 1563.0).ended().commit()
        assertEquals(1560, session().timeSec)
        assertEquals(2, session().timerDone)
    }

    @Test fun `C16a AT26 a restore that reaches the end, held while the owner saves 25 minutes`() = runBlocking {
        val live = ready().started().run(clocks, 100.0)
        val snap = TimerSnapshot(live, clocks.wall, clocks.elapsed)
        clocks.pass(40 * MIN)
        val restored = snap.advancedAt(clocks.wall, clocks.elapsed).also { TimerSnapshots.save(context, it, clocks.wall, clocks.elapsed) }
        assertTrue(restored.done)
        val hold = Hold()
        val pending = async { restored.commit() }
        hold.reached.await()
        val opened = session()
        repo.saveCardioEdits(opened, opened.copy(timeSec = 1500), emptyMap(), clocks.wall)
        hold.release.complete(Unit)
        assertEquals(TimerCommit.COMMITTED, pending.await())
        assertEquals(TimerCommit.DROPPED, restored.commit())
        val s = session()
        assertEquals(1500, s.timeSec)
        assertEquals(start, s.timedFrom)
        assertEquals(start + 1_563_000, s.timedTo)
        assertEquals(start + 303_000, rounds()[0].second)
        assertNull(snapshot())
    }

    private val oldRunning = """{"state":{"sessionId":"c","title":"Norwegian 4x4","phases":[{"kind":"WORK","seconds":240,"round":1},""" +
        """{"kind":"REST","seconds":180,"round":1},{"kind":"WORK","seconds":240,"round":2}],"index":0,"remaining":100.0,""" +
        """"elapsed":140.0,"awaitingStart":false,"startedAt":1763600000000},"savedAt":1763600140000}"""

    private suspend fun endOld(text: String): TimerCommit {
        val ended = TimerSnapshot.decode(text)!!.advancedAt(1763600140000 + 30 * MIN, clocks.elapsed)
        return ended.finishedAt(1763600140000 + 30 * MIN, clocks.elapsed).also { TimerSnapshots.save(context, it) }.commit()
    }

    @Test fun `C16b AT20 a run of the older version writes its duration once and no clock field`() = runBlocking {
        assertEquals(TimerCommit.COMMITTED, endOld(oldRunning))
        assertEquals(TimerCommit.DROPPED, endOld(oldRunning))
        session().let { assertEquals(660, it.timeSec); assertNull(it.timerRun); assertNull(it.timerDone); assertNull(it.timedFrom) }
        assertTrue(rounds().all { it.first == null })
        assertNull(snapshot())
    }

    @Test fun `C16b AT20 an older run on a session with a duration or a run, and an older Ready with Stop, write nothing`() = runBlocking {
        dao.setSessionTime("c", 900)
        assertEquals(TimerCommit.DROPPED, endOld(oldRunning))
        assertEquals(900, session().timeSec)
        dao.setSessionTime("c", null)
        dao.setTimerRun("c", 1)
        assertEquals(TimerCommit.DROPPED, endOld(oldRunning))
        assertNull(session().timeSec)
        dao.upsert(session().copy(timerRun = null))
        val before = session()
        val oldReady = oldRunning.replace("\"awaitingStart\":false", "\"awaitingStart\":true")
        val stopped = TimerSnapshot.decode(oldReady)!!.state.finishedAt(clocks.wall, clocks.elapsed)
        TimerSnapshots.save(context, stopped)
        assertEquals(TimerCommit.NOTHING, stopped.commit())
        assertEquals(before, session())
        assertNull(snapshot())
    }

    @Test fun `C17 Replace and reset stop the timer, clear its snapshot after the write, and a later commit writes nothing`() = runBlocking {
        for (op in listOf("replace", "reset")) {
            fresh()
            val backup = repo.exportJson()
            val active = ready().started().run(clocks, 600.0)
            TimerSnapshots.save(context, active, clocks.wall)
            val ended = active.run(clocks, 100.0).ended()
            val generation = repo.generation
            if (op == "replace") repo.importJson(backup, replace = true) else repo.resetToSeed()
            assertEquals(op, listOf("halt", "clear"), timer.calls)
            assertNull(op, snapshot())
            val after = dao.session("c")
            assertEquals(op, TimerCommit.RETRY, ended.commit(generation = generation))
            assertEquals(op, after, dao.session("c"))
            if (op == "replace") { assertNull(after!!.timeSec); assertNull(after.timerDone); assertNull(after.timerRun) } else assertNull(after)
        }
    }

    @Test fun `C17 AT19 a failed Replace or reset keeps the old rows and the pending result, which commits once after a restart`() = runBlocking {
        for (op in listOf("replace", "reset")) {
            fresh()
            val backup = repo.exportJson()
            val run = ready().started().run(clocks, 1200.0).ended()
            failRoundWrites()
            assertEquals(op, TimerCommit.FAILED, run.commit())
            drop("fail_rounds")
            val before = session()
            failProgramInserts()
            val failed = runCatching { if (op == "replace") repo.importJson(backup, replace = true) else repo.resetToSeed() }
            assertTrue(op, failed.isFailure)
            drop("fail_programs")
            assertEquals(op, listOf("halt", "resume"), timer.calls)
            assertEquals(op, before, session())
            assertEquals(op, listOf("r1"), pendingIds())
            assertEquals(op, TimerCommit.RETRY, run.commit(generation = 0L))
            assertEquals(op, listOf("r1"), pendingIds())
            val restarted = newRepo()
            assertEquals(op, listOf(TimerCommit.COMMITTED), commitPending(restarted, context))
            assertEquals(op, emptyList<TimerCommit>(), commitPending(restarted, context))
            assertEquals(op, start, session().timedFrom)
            assertEquals(op, 1197, session().timeSec)
        }
    }

    @Test fun `AT19 a successful Replace or reset after a failed commit discards the old result`() = runBlocking {
        for (op in listOf("replace", "reset")) {
            fresh()
            val backup = repo.exportJson()
            val run = ready().started().run(clocks, 1200.0).ended()
            failRoundWrites()
            assertEquals(op, TimerCommit.FAILED, run.commit())
            drop("fail_rounds")
            if (op == "replace") repo.importJson(backup, replace = true) else repo.resetToSeed()
            assertNull(op, snapshot())
            assertEquals(op, emptyList<String>(), pendingIds())
            val restarted = newRepo()
            assertEquals(op, emptyList<TimerCommit>(), commitPending(restarted, context))
            dao.session("c")?.let { assertNull(op, it.timedFrom); assertNull(op, it.timeSec) }
        }
    }

    @Test fun `C17 a commit held across a Replace is refused by the generation`() = runBlocking {
        val backup = repo.exportJson()
        val ended = full()
        val hold = Hold()
        val held = async { ended.commit() }
        hold.reached.await()
        repo.importJson(backup, replace = true)
        hold.release.complete(Unit)
        assertEquals(TimerCommit.RETRY, held.await())
        session().let { assertNull(it.timeSec); assertNull(it.timedFrom); assertNull(it.timerRun) }
    }

    @Test fun `AT2 the newer run owns both counters, keeps 25 minutes, and its own clock fields`() = runBlocking {
        for (trusted in listOf(false, true)) {
            fresh()
            val hold = Hold()
            val r1 = full("r1")
            val late = async { r1.commit() }
            hold.reached.await()
            if (trusted) clocks.moveWall(-2 * 60 * MIN)
            var r2 = ready("r2").started().run(clocks, 300.0)
            if (!trusted) clocks.moveWall(15 * MIN)
            r2 = r2.run(clocks, 1263.0).ended()
            assertEquals(TimerCommit.COMMITTED, r2.commit())
            repo.setSessionTime("c", 1500)
            hold.release.complete(Unit)
            assertEquals(TimerCommit.DROPPED, late.await())
            assertEquals(TimerCommit.DROPPED, r2.commit())
            val s = session()
            assertEquals(2, s.timerRun)
            assertEquals(2, s.timerDone)
            assertEquals(1500, s.timeSec)
            if (trusted) {
                assertEquals(r2.clock!!.anchorWall, s.timedFrom)
                assertEquals(r2.clock!!.anchor.wallAt(r2.clock!!.endElapsed!!), s.timedTo)
                assertTrue(s.timedFrom!! < r1.clock!!.anchorWall)
                assertTrue(rounds().all { it.first != null })
            } else {
                assertNull(s.timedFrom); assertNull(s.timedTo); assertTrue(rounds().all { it.first == null })
            }
        }
    }

    @Test fun `AT3 a run started from the notification after 40 minutes on Ready has its range from that Start`() = runBlocking {
        val waited = ready().run(clocks, 40 * 60.0)
        val begun = waited.started()
        assertEquals(at(18, 40), begun.clock!!.anchorWall)
        begun.run(clocks, 20 * 60.0).ended().commit()
        val opened = session()
        repo.saveCardioEdits(opened, opened.copy(note = "ok"), emptyMap(), at(19, 1))
        val window = hrWindow(session(), TEST_ZONE)!!
        assertEquals(at(18, 40), window.fromMs)
        assertEquals(at(19, 1), window.toMs)
        val offered = (at(18, 3)..at(18, 35) step 60_000L)
        assertTrue(offered.none { it >= window.fromMs && it < window.toMs })
    }

    @Test fun `a stretch run commits nothing through the cardio path`() = runBlocking {
        val stretch = TimerState("routine:c", "Mobility", listOf(Phase(PhaseKind.WORK, 30, 0)), remaining = 30.0, stretch = true, runId = "s")
        val before = session()
        assertEquals(TimerCommit.NOTHING, stretch.begin().tick(40.0).commit())
        assertEquals(before, session())
    }

    @Test fun `the Start of a session that is gone gives no number`() = runBlocking {
        repo.deleteSession("c")
        assertNull(repo.startTimerRun("c"))
        assertNotNull(ready().beginRun(null, clocks.wall, clocks.elapsed).takeIf { !it.awaitingStart })
        Unit
    }
}
