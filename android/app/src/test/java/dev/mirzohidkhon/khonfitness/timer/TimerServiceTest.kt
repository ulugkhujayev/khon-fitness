package dev.mirzohidkhon.khonfitness.timer

import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.IntervalLog
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.TimerCommit
import java.time.Duration
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
}
