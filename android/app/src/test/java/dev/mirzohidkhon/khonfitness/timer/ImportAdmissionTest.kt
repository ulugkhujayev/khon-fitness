package dev.mirzohidkhon.khonfitness.timer

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.data.CardioSaveResult
import dev.mirzohidkhon.khonfitness.data.EndedRun
import dev.mirzohidkhon.khonfitness.data.FakeClocks
import dev.mirzohidkhon.khonfitness.data.HrRange
import dev.mirzohidkhon.khonfitness.data.ImportResult
import dev.mirzohidkhon.khonfitness.data.IntervalLog
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.KhonDao
import dev.mirzohidkhon.khonfitness.data.KhonDatabase
import dev.mirzohidkhon.khonfitness.data.KhonRepository
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SystemClocks
import dev.mirzohidkhon.khonfitness.data.TEST_ZONE
import dev.mirzohidkhon.khonfitness.data.TimerCommit
import dev.mirzohidkhon.khonfitness.data.at
import dev.mirzohidkhon.khonfitness.data.runRange
import dev.mirzohidkhon.khonfitness.health.HealthImport
import dev.mirzohidkhon.khonfitness.health.ImportDraft
import java.time.Instant
import java.time.LocalDate
import java.util.Collections
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ImportAdmissionTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository
    private lateinit var clocks: FakeClocks
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val oldZone = TimeZone.getDefault()
    private val day = LocalDate.of(2026, 11, 20)
    private var fills = 0
    private lateinit var bike: dev.mirzohidkhon.khonfitness.data.Exercise

    private val draft = band("b1", at(18, 0), at(18, 27))

    @Before fun open() {
        TimeZone.setDefault(TimeZone.getTimeZone(TEST_ZONE))
        clocks = FakeClocks(at(18, 0))
        TimerService.wall = clocks
        fills = 0
        db = Room.inMemoryDatabaseBuilder(context, KhonDatabase::class.java).build()
        dao = db.dao()
        repo = newRepo()
        TimerSnapshots.clearAll(context)
        runBlocking {
            repo.seedIfEmpty()
            bike = dao.exercisesOnce().single { it.id == "nor4x4" }
            cardio("c", at(17, 59))
        }
    }

    @After fun close() {
        db.close()
        TimerSnapshots.clearAll(context)
        TimerService.wall = SystemClocks
        TimeZone.setDefault(oldZone)
    }

    private fun newRepo() = KhonRepository(dao, clocks = clocks, zone = { TEST_ZONE }, requestFill = { fills++ }, timer = ServiceTimerControl(context))

    private suspend fun cardio(id: String, startedAt: Long, date: String = day.toString()) = dao.insertSessionWithLogs(
        Session(id, date, startedAt, itemType = ItemType.CARDIO, exerciseId = bike.id, exerciseName = bike.name),
        emptyList(), (0 until 4).map { IntervalLog("$id-i$it", id, it) },
    )

    private fun band(id: String, from: Long, to: Long) =
        ImportDraft(id, Instant.ofEpochMilli(from), Instant.ofEpochMilli(to), "Bike", "bike", null, null, emptyList(), listOf("com.mi.health"))

    private fun ready(sessionId: String = "c", runId: String = "r1") =
        TimerState(sessionId, "Norwegian 4x4", buildPhases(60, 240, 180, 4), remaining = 60.0, awaitingStart = true, runId = runId)

    private suspend fun TimerState.started() = beginRun(repo.startTimerRun(sessionId), clocks.wall, clocks.elapsed)

    private fun TimerState.run(seconds: Double): TimerState {
        var s = this
        var left = seconds
        while (left > 1e-9 && !s.done) {
            val dt = minOf(1.0, left)
            clocks.pass(Math.round(dt * 1000)); s = s.tickAt(dt, clocks.elapsed); left -= dt
        }
        return s
    }

    private fun TimerState.ended() = finishedAt(clocks.wall, clocks.elapsed).also { TimerSnapshots.save(context, it, clocks.wall) }

    private suspend fun full() = ready().started().run(1563.0).ended()

    private suspend fun offered(d: ImportDraft = draft) = HealthImport.offered(listOf(d), repo.importLocal()).size
    private suspend fun counted(d: ImportDraft = draft) = HealthImport.countOffered(listOf(d), repo.importLocal())

    private suspend fun nothingImported(rounds: Int = 4) {
        assertTrue(dao.sessionsOnce().none { it.sourceId != null })
        assertEquals(rounds, dao.intervalLogsOnce().size)
        assertFalse("b1" in repo.importLocal().claimed)
        assertEquals(0, fills)
        assertEquals(1, offered())
        assertEquals(1, counted())
    }

    private inner class HoldFirstCommitBeforeLock {
        val reached = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        private var first = true
        init {
            repo.beforeTimerCommit = { if (first) { first = false; reached.complete(Unit); release.await() } }
        }
    }

    private fun failRoundWrites() = db.openHelper.writableDatabase.execSQL(
        "CREATE TRIGGER fail_rounds BEFORE UPDATE OF endedAt ON interval_logs BEGIN SELECT RAISE(ABORT, 'held'); END")

    @Test fun `D7b AT07 a result held before its write refuses the import, and after its commit the import is covered`() = runBlocking {
        assertEquals(1, offered())
        val hold = HoldFirstCommitBeforeLock()
        val done = full()
        val ending = async(Dispatchers.IO) { endRun(repo, context, done) }
        withTimeout(5_000) { hold.reached.await() }
        assertEquals(ImportResult.TIMER_PENDING, repo.importDraft(draft, bike))
        nothingImported()
        hold.release.complete(Unit)
        assertTrue(withTimeout(5_000) { ending.await() })
        assertEquals(HrRange(at(18, 0), at(18, 0) + 1_563_000), runRange(dao.session("c")!!, TEST_ZONE))
        assertEquals(0, offered())
        assertEquals(0, counted())
        assertEquals(ImportResult.COVERED, repo.importDraft(draft, bike))
        assertTrue(dao.sessionsOnce().none { it.sourceId != null })
        assertEquals(0, fills)
    }

    @Test fun `AT07 AT07a a failed timer write keeps the result pending, also on a second submit, until the retry commits`() = runBlocking {
        failRoundWrites()
        val done = full()
        assertTrue(endRun(repo, context, done))
        assertEquals(listOf("r1"), TimerSnapshots.pending(context).map { it.runId })
        assertEquals(ImportResult.TIMER_PENDING, repo.importDraft(draft, bike))
        assertEquals(ImportResult.TIMER_PENDING, repo.importDraft(draft, bike))
        nothingImported()
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_rounds")
        assertEquals(listOf(TimerCommit.COMMITTED), drain(repo, context))
        assertEquals(ImportResult.COVERED, repo.importDraft(draft, bike))
        assertEquals(0, fills)
    }

    @Test fun `AT07 an import while the timer still runs saves`() = runBlocking {
        val running = ready().started().run(600.0)
        TimerSnapshots.save(context, running, clocks.wall, clocks.elapsed)
        assertNotNull(TimerSnapshots.load(context))
        assertEquals(ImportResult.SAVED, repo.importDraft(draft, bike))
        assertEquals("b1", dao.sessionsOnce().single { it.sourceId != null }.sourceId)
        assertEquals(1, fills)
    }

    @Test fun `AT07a an import held before admission meets a run that ended meanwhile and is refused`() = runBlocking {
        val reached = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        repo.beforeImportAdmission = { reached.complete(Unit); release.await() }
        val running = ready().started().run(1563.0)
        val import = async(Dispatchers.IO) { repo.importDraft(draft, bike) }
        withTimeout(5_000) { reached.await() }
        repo.beforeImportAdmission = {}
        val hold = HoldFirstCommitBeforeLock()
        val done = running.ended()
        val ending = async(Dispatchers.IO) { endRun(repo, context, done) }
        withTimeout(5_000) { hold.reached.await() }
        release.complete(Unit)
        assertEquals(ImportResult.TIMER_PENDING, withTimeout(5_000) { import.await() })
        nothingImported()
        hold.release.complete(Unit)
        assertTrue(withTimeout(5_000) { ending.await() })
        assertEquals(ImportResult.COVERED, repo.importDraft(draft, bike))
    }

    private fun TimerState.settledRun(passing: () -> Unit = {}): EndedRun {
        passing()
        val s = this
        return EndedRun(runResult()) { TimerSnapshots.settle(context, s); TimerSnapshots.clearRun(context, s) }
    }

    @Test fun `AT07a D7c a timer commit that reached the lock passes it only after the admitted import released it`() = runBlocking {
        val reached = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val order = Collections.synchronizedList(mutableListOf<String>())
        val running = ready().started().run(1563.0)
        repo.afterImportChecks = { reached.complete(Unit); release.await(); order += "import released" }
        val import = async(Dispatchers.IO) { repo.importDraft(draft, bike) }
        withTimeout(5_000) { reached.await() }
        val done = running.ended()
        val atLock = CompletableDeferred<Unit>()
        repo.beforeTimerCommit = { atLock.complete(Unit) }
        val ending = async(start = CoroutineStart.UNDISPATCHED) { repo.endTimerRun { done.settledRun { order += "timer passed the lock" } } }
        assertTrue(atLock.isCompleted)
        assertEquals(emptyList<String>(), order.toList())
        release.complete(Unit)
        assertEquals(ImportResult.SAVED, withTimeout(5_000) { import.await() })
        assertEquals(TimerCommit.COMMITTED, withTimeout(5_000) { ending.await() })
        assertEquals(listOf("import released", "timer passed the lock"), order.toList())
        val app = dao.session("c")!!
        assertEquals(1, app.timerDone)
        assertEquals(at(18, 0), app.timedFrom)
        assertEquals("b1", dao.sessionsOnce().single { it.sourceId != null }.sourceId)
        assertEquals(1, fills)
        assertEquals(emptyList<PendingRun>(), TimerSnapshots.pending(context))
    }

    @Test fun `D7b a covering run that ends while the admitted import waits for Room refuses it at its write`() = runBlocking {
        val running = ready().started().run(1563.0)
        assertFalse(TimerSnapshots.resultPending(context, clocks.wall, clocks.elapsed))
        val held = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val roomHolder = async { db.withTransaction { held.complete(Unit); release.await() } }
        withTimeout(5_000) { held.await() }
        val import = async(start = CoroutineStart.UNDISPATCHED) { repo.importDraft(draft, bike) }
        assertFalse(import.isCompleted)
        val done = running.ended()
        release.complete(Unit)
        assertEquals(ImportResult.TIMER_PENDING, withTimeout(5_000) { import.await() })
        roomHolder.await()
        nothingImported()
        assertTrue(endRun(repo, context, done))
        assertEquals(ImportResult.COVERED, repo.importDraft(draft, bike))
        assertEquals(0, fills)
    }

    @Test fun `D7b a result pending at submit refuses the import that waits behind its commit, also when the commit covers nothing`() = runBlocking {
        val started = ready().started().run(300.0)
        clocks.moveWall(10 * 60_000L)
        val done = started.run(1263.0).ended()
        val insideLock = CountDownLatch(1)
        val letGo = CountDownLatch(1)
        val committing = async(Dispatchers.IO) { repo.endTimerRun { done.settledRun { insideLock.countDown(); letGo.await() } } }
        assertTrue(insideLock.await(5, TimeUnit.SECONDS))
        val import = async(start = CoroutineStart.UNDISPATCHED) { repo.importDraft(draft, bike) }
        assertFalse(import.isCompleted)
        letGo.countDown()
        assertEquals(TimerCommit.COMMITTED, withTimeout(5_000) { committing.await() })
        assertFalse(TimerSnapshots.hasPending(context))
        assertNull(dao.session("c")!!.timedFrom)
        assertEquals(ImportResult.TIMER_PENDING, withTimeout(5_000) { import.await() })
        nothingImported()
        assertEquals(ImportResult.SAVED, repo.importDraft(draft, bike))
        assertEquals(1, fills)
    }

    @Test fun `AT07a on a cold start an import before the restore sees an ended snapshot as pending`() = runBlocking {
        val done = ready().started().run(1563.0).finishedAt(clocks.wall, clocks.elapsed)
        context.getSharedPreferences("timer_snapshot", Context.MODE_PRIVATE).edit()
            .putString("snapshot", TimerSnapshot(done, clocks.wall, clocks.elapsed).encode()).commit()
        clocks.pass(60_000)
        repo = newRepo()
        assertEquals(ImportResult.TIMER_PENDING, repo.importDraft(draft, bike))
        nothingImported()
        assertNull(TimerSnapshots.load(context))
        assertEquals(ImportResult.TIMER_PENDING, repo.importDraft(draft, bike))
        assertEquals(listOf(TimerCommit.COMMITTED), drain(repo, context))
        assertEquals(ImportResult.COVERED, repo.importDraft(draft, bike))
    }

    @Test fun `AT07a on a cold start an import before the restore sees a snapshot that reaches its end as pending`() = runBlocking {
        val running = ready().started().run(600.0)
        TimerSnapshots.save(context, running, clocks.wall, clocks.elapsed)
        clocks.pass(30 * 60_000L)
        repo = newRepo()
        assertEquals(ImportResult.TIMER_PENDING, repo.importDraft(draft, bike))
        nothingImported()
        val restored = TimerSnapshots.load(context)!!.advancedAt(clocks.wall, clocks.elapsed)
        assertTrue(restored.done)
        TimerSnapshots.save(context, restored, clocks.wall)
        assertTrue(endRun(repo, context, restored))
        assertEquals(HrRange(at(18, 0), at(18, 0) + 1_563_000), runRange(dao.session("c")!!, TEST_ZONE))
        assertEquals(ImportResult.COVERED, repo.importDraft(draft, bike))
    }

    @Test fun `AT07a a covering result in Room wins over its pending entry when the cleanup has not run`() = runBlocking {
        val done = full()
        assertEquals(TimerCommit.COMMITTED, repo.endTimerRun { EndedRun(done.runResult()) {} })
        assertTrue(TimerSnapshots.hasPending(context))
        assertEquals(ImportResult.COVERED, repo.importDraft(draft, bike))
        assertEquals(0, fills)
    }

    @Test fun `AT07a a result that commits without a covering range frees the import, which saves once`() = runBlocking {
        val started = ready().started().run(300.0)
        clocks.moveWall(10 * 60_000L)
        val done = started.run(1263.0).ended()
        assertEquals(ImportResult.TIMER_PENDING, repo.importDraft(draft, bike))
        assertTrue(endRun(repo, context, done))
        val app = dao.session("c")!!
        assertEquals(1, app.timerDone)
        assertNull(app.timedFrom)
        assertEquals(ImportResult.SAVED, repo.importDraft(draft, bike))
        assertEquals(ImportResult.ALREADY_IMPORTED, repo.importDraft(draft, bike))
        assertEquals(1, dao.sessionsOnce().count { it.sourceId == "b1" })
        assertEquals(1, fills)
    }

    @Test fun `AT01 D1 D1a a run made at 23 59 with its timer after midnight covers the band run before and after Save`() = runBlocking {
        val next = day.plusDays(1)
        cardio("late", at(23, 59))
        clocks.wall = at(0, 1, next)
        val late = band("b2", at(0, 0, next), at(0, 22, next))
        val done = ready("late").started().run(1200.0).ended()
        assertEquals(1, offered(late))
        val old = HealthImport.offered(listOf(late), repo.importLocal()).single()
        assertEquals(TimerCommit.COMMITTED, recordRun(repo, context, done, repo.generation))
        val s = dao.session("late")!!
        assertNull(s.finishedAt)
        assertEquals(HrRange(at(0, 1, next), at(0, 21, next)), runRange(s, TEST_ZONE))
        assertEquals(0, offered(late))
        assertEquals(0, counted(late))
        assertEquals(ImportResult.COVERED, repo.importDraft(old, bike))
        assertEquals(CardioSaveResult.Saved, repo.saveCardioEdits(s, s, emptyMap(), clocks.wall))
        val saved = dao.session("late")!!
        assertEquals(day.toString(), saved.date)
        assertNotNull(saved.finishedAt)
        assertEquals(0, offered(late) + counted(late))
        assertEquals(ImportResult.COVERED, repo.importDraft(old, bike))
        assertTrue(dao.sessionsOnce().none { it.sourceId != null })
    }
}
