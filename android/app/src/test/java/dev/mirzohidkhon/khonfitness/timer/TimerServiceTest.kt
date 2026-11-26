package dev.mirzohidkhon.khonfitness.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.Clocks
import dev.mirzohidkhon.khonfitness.data.SystemClocks
import dev.mirzohidkhon.khonfitness.data.IntervalLog
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.TimerCommit
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowNotificationManager
import org.robolectric.shadows.ShadowSystemClock

/** A notification channel that takes [delayMs] of elapsed time to create: the slow part of a foreground start. */
@Implements(NotificationManager::class)
class SlowForegroundShadow : ShadowNotificationManager() {
    @Implementation
    override fun createNotificationChannel(channel: NotificationChannel) {
        if (delayMs > 0) ShadowSystemClock.advanceBy(Duration.ofMillis(delayMs))
        super.createNotificationChannel(channel)
    }

    companion object { var delayMs = 0L }
}

/**
 * The real [TimerService] under Robolectric: runs end through the service's own loop and Stop, their commits and
 * publications run on the app scope, and the main looper is driven by the test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = KhonApp::class)
class TimerServiceTest {
    private val app = ApplicationProvider.getApplicationContext<KhonApp>()
    private val dao get() = app.db.dao()
    private val appJob get() = app.scope.coroutineContext[Job]!!
    private val main get() = shadowOf(Looper.getMainLooper())
    private var controller: ServiceController<TimerService>? = null

    @Before fun open() {
        TimerService.state.value = null
        TimerService.finished.value = null
        TimerSnapshots.clearAll(app)
        runBlocking {
            for (id in listOf("a", "b")) dao.insertSessionWithLogs(
                Session(id, "2026-11-20", System.currentTimeMillis(), itemType = ItemType.CARDIO, exerciseId = "nor", exerciseName = "Norwegian 4x4"),
                emptyList(), (0 until 2).map { IntervalLog("$id$it", id, it) },
            )
        }
    }

    @After fun close() {
        SlowForegroundShadow.delayMs = 0
        TimerService.wall = SystemClocks
        app.repo.beforeTimerCommit = {}
        controller?.destroy()
        TimerService.state.value = null
        TimerService.finished.value = null
        TimerSnapshots.clearAll(app)
    }

    private fun service(): TimerService = (controller ?: Robolectric.buildService(TimerService::class.java).create().also { controller = it }).get()

    /** The process after a restart: the old service is gone, a new one gets the intent. */
    private fun newService(): TimerService {
        controller?.destroy()
        controller = null
        TimerService.state.value = null
        return service()
    }

    private fun send(action: String, id: String? = null, work: Int = 5, rounds: Int = 1, rest: Int = 0) {
        val i = Intent().setAction(action)
        if (id != null) i.putExtra("sessionId", id).putExtra("warmup", 0).putExtra("work", work).putExtra("rest", rest).putExtra("rounds", rounds)
        service().onStartCommand(i, 0, 1)
    }

    private fun awaitMain(message: String, condition: () -> Boolean) {
        repeat(2_000) {
            if (condition()) return
            main.idle()
            Thread.sleep(5)
        }
        assertTrue(message, condition())
    }

    /** Runs [action] and waits until every app-scope job it started has ended. */
    private fun settled(message: String, action: () -> Unit) {
        val before = appJob.children.toSet()
        action()
        awaitMain(message) { appJob.children.none { it !in before && it.isActive } }
    }

    private fun begin(id: String, work: Int = 5, rounds: Int = 1, rest: Int = 0): TimerState {
        send(TimerService.ACTION_START, id, work, rounds, rest)
        val ready = TimerService.state.value!!
        send(TimerService.ACTION_BEGIN)
        awaitMain("the run gets its number") { TimerService.state.value?.clock != null }
        return TimerService.state.value!!.also { assertEquals(ready.runId, it.runId) }
    }

    /** The service loop until its last second ended the run. */
    private fun loopToEnd() {
        repeat(1_000) {
            if (TimerService.state.value?.done != false) return
            main.idleFor(Duration.ofMillis(50))
        }
        assertTrue("the loop ends the run", TimerService.state.value?.done != false)
    }

    private fun session(id: String) = runBlocking { dao.session(id)!! }
    private fun pendingIds() = TimerSnapshots.pending(app).map { it.runId }

    /** Holds the database in a write transaction on another thread until the returned latch opens: a Start write waits. */
    private fun holdDatabase(): CountDownLatch {
        val held = CountDownLatch(1)
        val release = CountDownLatch(1)
        thread { app.db.runInTransaction { held.countDown(); release.await(20, TimeUnit.SECONDS) } }
        assertTrue("the database is held", held.await(5, TimeUnit.SECONDS))
        return release
    }

    /** Holds the first timer commit after it read its generation, until the returned value completes. */
    private fun holdFirstCommit(): Pair<CompletableDeferred<Unit>, CompletableDeferred<Unit>> {
        val reached = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var first = true
        app.repo.beforeTimerCommit = { if (first) { first = false; reached.complete(Unit); release.await() } }
        return reached to release
    }

    /** A Start whose number write fails: the run goes on without a number. */
    private fun beginWithoutNumber(id: String, work: Int): TimerState {
        send(TimerService.ACTION_START, id, work)
        send(TimerService.ACTION_BEGIN)
        awaitMain("the run begins") { TimerService.state.value?.awaitingStart == false }
        return TimerService.state.value!!.also { assertNull(it.clock) }
    }

    private fun idleMain() = repeat(20) { main.idle(); Thread.sleep(5) }

    /** Robolectric moves only its elapsed clock; this wall clock moves with it, so the service's runs stay in step. */
    private fun wallInStep() {
        val offset = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        TimerService.wall = object : Clocks {
            override fun wallMs() = offset + SystemClock.elapsedRealtime()
            override fun elapsedMs() = SystemClock.elapsedRealtime()
        }
    }

    private fun failRoundsOf(id: String) = app.db.openHelper.writableDatabase.execSQL(
        "CREATE TRIGGER fail_$id BEFORE UPDATE OF endedAt ON interval_logs WHEN NEW.sessionId = '$id' BEGIN SELECT RAISE(ABORT, 'held'); END")
    private fun drop(trigger: String) = app.db.openHelper.writableDatabase.execSQL("DROP TRIGGER $trigger")

    @Test fun `C15 a held older run that the service ended never touches the newer run, which then commits through the service`() {
        val reached = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var first = true
        app.repo.beforeTimerCommit = { if (first) { first = false; reached.complete(Unit); release.await() } }
        val r1 = begin("a")
        val before = appJob.children.toSet()
        loopToEnd()
        awaitMain("R1 reaches the commit hold") { reached.isCompleted }
        val held = appJob.children.single { it !in before }

        val r2 = begin("a")
        assertEquals(2, r2.clock!!.number)
        assertTrue(r2.runId != r1.runId)
        awaitMain("R2's snapshot is saved") { TimerSnapshots.load(app)?.state?.runId == r2.runId }

        release.complete(Unit)
        awaitMain("R1's commit returns") { held.isCompleted }
        main.idle()
        assertEquals(r2.runId, TimerService.state.value?.runId)
        assertFalse(TimerService.state.value!!.done)
        assertNull(TimerService.finished.value)
        assertEquals(r2.runId, TimerSnapshots.load(app)?.state?.runId)
        assertEquals(emptyList<String>(), pendingIds())
        session("a").let { assertEquals(2, it.timerRun); assertNull(it.timerDone); assertNull(it.timeSec); assertNull(it.timedFrom) }

        settled("R2's commit returns") { loopToEnd() }
        main.idle()
        assertEquals("a", TimerService.finished.value?.sessionId)
        assertNull(TimerService.state.value)
        val s = session("a")
        assertEquals(2, s.timerDone)
        assertEquals(5, s.timeSec)
        assertNull(TimerSnapshots.load(app))
        assertEquals(emptyList<String>(), pendingIds())
    }

    @Test fun `X4 a failed result stays pending through Ready, Stop on Ready, and a run of another session, and commits once at the start`() {
        failRoundsOf("a")
        val r1 = begin("a")
        settled("R1's commit fails") { loopToEnd() }
        assertEquals(listOf(r1.runId), pendingIds())
        assertNull(session("a").timerDone)

        send(TimerService.ACTION_START, "b")
        main.idle()
        assertTrue(TimerService.state.value!!.awaitingStart)
        assertEquals(listOf(r1.runId), pendingIds())
        settled("Stop on Ready ends") { send(TimerService.ACTION_STOP); main.idle() }
        main.idle()
        assertNull(TimerService.state.value)
        assertEquals(listOf(r1.runId), pendingIds())

        val b = begin("b")
        settled("the run of b commits and the retry of a fails again") { loopToEnd() }
        session("b").let { assertEquals(1, it.timerDone); assertEquals(5, it.timeSec) }
        assertEquals(listOf(r1.runId), pendingIds())
        assertNull(session("a").timerDone)
        assertTrue(b.runId != r1.runId)

        drop("fail_a")
        val restarted = newService()
        settled("the start commits the pending result") { restarted.onStartCommand(Intent().setAction(TimerService.ACTION_RESTORE), 0, 1); main.idle() }
        session("a").let { assertEquals(1, it.timerDone); assertEquals(5, it.timeSec) }
        assertEquals(emptyList<String>(), pendingIds())
        assertEquals(emptyList<TimerCommit>(), runBlocking { commitPending(app) })
    }

    @Test fun `X4 Ready and Stop on Ready leave a pending result alone, and the next run's end commits it`() {
        failRoundsOf("a")
        val r1 = begin("a")
        settled("R1's commit fails") { loopToEnd() }
        assertEquals(listOf(r1.runId), pendingIds())
        drop("fail_a")

        send(TimerService.ACTION_START, "b")
        main.idle()
        assertEquals(listOf(r1.runId), pendingIds())
        settled("Stop on Ready ends") { send(TimerService.ACTION_STOP); main.idle() }
        assertEquals(listOf(r1.runId), pendingIds())
        assertNull(session("a").timerDone)

        begin("b")
        settled("the run of b and the retry of a commit") { loopToEnd() }
        session("b").let { assertEquals(1, it.timerDone); assertEquals(5, it.timeSec) }
        session("a").let { assertEquals(1, it.timerDone); assertEquals(5, it.timeSec) }
        assertEquals(emptyList<String>(), pendingIds())
    }

    @Test fun `C17 a real service paused in an active round, a failed Replace, and the same run resumes`() {
        val backup = runBlocking { app.repo.exportJson() }
        val live = begin("a", work = 60, rounds = 2, rest = 30)
        repeat(100) { main.idleFor(Duration.ofMillis(50)) }
        assertTrue(TimerService.state.value!!.active)
        assertEquals(PhaseKind.WORK, TimerService.state.value!!.phase.kind)
        send(TimerService.ACTION_PAUSE)
        main.idle()
        val paused = TimerService.state.value!!
        assertTrue(paused.paused)
        awaitMain("the paused snapshot is saved") { TimerSnapshots.load(app)?.state?.paused == true }
        val rowBefore = session("a")

        app.db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_programs BEFORE INSERT ON programs BEGIN SELECT RAISE(ABORT, 'held'); END")
        shadowOf(app).clearStartedServices()
        val replace = app.scope.async { runCatching { app.repo.importJson(backup, replace = true) } }
        awaitMain("the Replace returns") { replace.isCompleted }
        assertTrue(runBlocking { replace.await() }.isFailure)
        drop("fail_programs")
        assertNull(TimerService.state.value)
        assertEquals(live.runId, TimerSnapshots.load(app)?.state?.runId)
        assertEquals(rowBefore, session("a"))

        val resume = shadowOf(app).nextStartedService
        assertNotNull(resume)
        assertEquals(TimerService.ACTION_RESTORE, resume.action)
        newService().onStartCommand(resume, 0, 1)
        awaitMain("the run comes back") { TimerService.state.value != null }
        val back = TimerService.state.value!!
        assertEquals(live.runId, back.runId)
        assertEquals(1, back.clock!!.number)
        assertTrue(back.paused)
        assertEquals(paused.index, back.index)
        assertEquals(paused.remaining, back.remaining, 1e-9)
        assertEquals(paused.clock!!.rounds, back.clock!!.rounds)
        assertEquals(rowBefore, session("a"))
    }

    @Test fun `Y1 a Stop given while A's Start write is held leaves timer B, opened meanwhile, and its snapshot alone`() {
        for (command in listOf(TimerService.ACTION_STOP, TimerService.ACTION_SKIP, TimerService.ACTION_PAUSE, TimerService.ACTION_RESUME)) {
            send(TimerService.ACTION_START, "a")
            val a = TimerService.state.value!!
            val release = holdDatabase()
            send(TimerService.ACTION_BEGIN)
            idleMain()
            send(command)
            idleMain()
            send(TimerService.ACTION_START, "b")
            main.idle()
            val b = TimerService.state.value!!
            assertTrue(command, b.awaitingStart && b.sessionId == "b")
            val timerRunBefore = session("a").timerRun ?: 0
            release.countDown()
            awaitMain("$command: A's Start write returns") { (session("a").timerRun ?: 0) == timerRunBefore + 1 }
            idleMain()
            assertEquals(command, b, TimerService.state.value)
            assertNull(command, TimerService.finished.value)
            assertEquals(command, b.runId, TimerSnapshots.load(app)?.state?.runId)
            assertTrue(command, TimerSnapshots.load(app)!!.state.awaitingStart)
            assertEquals(command, emptyList<String>(), pendingIds())
            assertTrue(command, a.runId != b.runId)
            assertNull(command, session("a").timerDone)
            newService()
            TimerSnapshots.clearAll(app)
        }
    }

    @Test fun `Y1 a Stop or a Pause given while the run's own Start write is held acts on that run once it has its number`() {
        send(TimerService.ACTION_START, "a")
        var release = holdDatabase()
        send(TimerService.ACTION_BEGIN)
        idleMain()
        send(TimerService.ACTION_PAUSE)
        release.countDown()
        awaitMain("the paused run has its number") { TimerService.state.value?.clock?.number == 1 }
        idleMain()
        assertTrue(TimerService.state.value!!.paused)

        newService()
        TimerSnapshots.clearAll(app)
        send(TimerService.ACTION_START, "b")
        release = holdDatabase()
        send(TimerService.ACTION_BEGIN)
        idleMain()
        settled("the stopped run commits") { send(TimerService.ACTION_STOP); release.countDown(); awaitMain("b has its number") { session("b").timerRun == 1 } }
        awaitMain("the end is published") { TimerService.finished.value != null }
        assertEquals("b", TimerService.finished.value!!.sessionId)
        assertNull(TimerService.state.value)
        assertEquals(1, session("b").timerDone)
        assertEquals(emptyList<String>(), pendingIds())
        assertEquals(emptyList<TimerCommit>(), runBlocking { commitPending(app) })
    }

    @Test fun `Y1 timer B opened during A's held Start write begins with its own write`() {
        send(TimerService.ACTION_START, "a")
        val release = holdDatabase()
        send(TimerService.ACTION_BEGIN)
        idleMain()
        send(TimerService.ACTION_START, "b")
        val b = TimerService.state.value!!
        send(TimerService.ACTION_BEGIN)
        idleMain()
        release.countDown()
        awaitMain("B gets its number") { TimerService.state.value?.clock?.number == 1 }
        assertEquals(b.runId, TimerService.state.value!!.runId)
        assertEquals(1, session("b").timerRun)
        assertNull(session("a").timerDone)
    }

    private fun failStarts() = app.db.openHelper.writableDatabase.execSQL(
        "CREATE TRIGGER fail_start BEFORE UPDATE OF timerRun ON sessions BEGIN SELECT RAISE(ABORT, 'held'); END")
    private fun failDuration(id: String, seconds: Int) = app.db.openHelper.writableDatabase.execSQL(
        "CREATE TRIGGER fail_time_$id BEFORE UPDATE OF timeSec ON sessions WHEN NEW.id = '$id' AND NEW.timeSec = $seconds BEGIN SELECT RAISE(ABORT, 'held'); END")

    @Test fun `Y2 two results without numbers for one session keep their own entries and commit in the order of their ends`() {
        failStarts()
        failDuration("a", seconds = 5)
        val a = beginWithoutNumber("a", work = 5)
        settled("A's result fails") { loopToEnd() }
        assertEquals(listOf(a.runId), pendingIds())
        val b = beginWithoutNumber("a", work = 7)
        settled("B ends while A still fails") { loopToEnd() }
        assertEquals(listOf(a.runId, b.runId), pendingIds())
        assertNull(session("a").timeSec)

        drop("fail_time_a")
        assertEquals(listOf(TimerCommit.COMMITTED, TimerCommit.DROPPED), runBlocking { commitPending(app) })
        assertEquals(5, session("a").timeSec)
        assertNull(session("a").timerRun)
        assertEquals(emptyList<String>(), pendingIds())
    }

    @Test fun `Y2 a result without a number whose commit is in flight when the next run of its session ends commits first`() {
        failStarts()
        val (reached, release) = holdFirstCommit()
        val a = beginWithoutNumber("a", work = 5)
        loopToEnd()
        awaitMain("A's commit is held") { reached.isCompleted }
        val b = beginWithoutNumber("a", work = 7)
        loopToEnd()
        awaitMain("B is staged") { pendingIds().size == 2 }
        assertEquals(listOf(a.runId, b.runId), pendingIds())
        assertNull(session("a").timeSec)
        release.complete(Unit)
        awaitMain("both results settle") { pendingIds().isEmpty() }
        idleMain()
        assertEquals(5, session("a").timeSec)
    }

    @Test @Config(shadows = [SlowForegroundShadow::class])
    fun `Y5 a real service restore that saves after a slow foreground start, restored again at once, keeps every boundary on the anchor`() {
        wallInStep()
        val live = begin("a", work = 60, rounds = 2, rest = 30)
        val anchor = live.clock!!.anchorElapsed
        repeat(200) { main.idleFor(Duration.ofMillis(50)) }
        assertEquals(PhaseKind.WORK, TimerService.state.value!!.phase.kind)
        fun expectedRemaining(at: Long) = 60.0 - (at - anchor - 3_000) / 1000.0
        fun restoreAfterDeath(): TimerState {
            newService().onStartCommand(Intent().setAction(TimerService.ACTION_RESTORE), 0, 1)
            awaitMain("the run comes back") { TimerService.state.value != null }
            return TimerService.state.value!!
        }

        newService()
        ShadowSystemClock.advanceBy(Duration.ofSeconds(20))
        SlowForegroundShadow.delayMs = 700
        val restoredAt = SystemClock.elapsedRealtime()
        val first = restoreAfterDeath()
        assertEquals(restoredAt, first.instant)
        assertEquals(expectedRemaining(restoredAt), first.remaining, 1e-6)
        assertEquals(restoredAt, TimerSnapshots.load(app)!!.savedElapsed)

        val againAt = SystemClock.elapsedRealtime()
        assertEquals(restoredAt + 700, againAt)
        val second = restoreAfterDeath()
        assertEquals(expectedRemaining(againAt), second.remaining, 1e-6)
        SlowForegroundShadow.delayMs = 0

        var clock = second.clock!!
        repeat(4_000) {
            val s = TimerService.state.value ?: return@repeat
            clock = s.clock!!
            if (s.done) return@repeat
            main.idleFor(Duration.ofMillis(50))
        }
        assertEquals(listOf(RoundTime(1, anchor + 3_000, anchor + 63_000), RoundTime(2, anchor + 93_000, anchor + 153_000)), clock.rounds)
        assertEquals(anchor + 153_000, clock.endElapsed)
    }

    @Test fun `Y6 a startup drain refused by the generation and a run that ended while the process was dead commit in one pass`() {
        wallInStep()
        failRoundsOf("a")
        val a = begin("a")
        settled("A's commit fails") { loopToEnd() }
        assertEquals(listOf(a.runId), pendingIds())
        drop("fail_a")
        val b = begin("b")
        repeat(20) { main.idleFor(Duration.ofMillis(50)) }
        assertEquals(b.runId, TimerSnapshots.load(app)?.state?.runId)

        controller?.destroy()
        controller = null
        TimerService.state.value = null
        ShadowSystemClock.advanceBy(Duration.ofMinutes(1))
        val backup = runBlocking { app.repo.exportJson() }
        val (reached, release) = holdFirstCommit()
        service().onStartCommand(Intent().setAction(TimerService.ACTION_RESTORE), 0, 1)
        awaitMain("the drain reaches its first commit") { reached.isCompleted }
        assertEquals(listOf(a.runId, b.runId), pendingIds())
        assertNull(TimerSnapshots.load(app))
        runBlocking { app.repo.importJson(backup, replace = false) }
        release.complete(Unit)
        awaitMain("both results commit in this launch") { pendingIds().isEmpty() }
        session("a").let { assertEquals(1, it.timerDone); assertEquals(5, it.timeSec) }
        session("b").let { assertEquals(1, it.timerDone); assertEquals(5, it.timeSec) }
    }
}
