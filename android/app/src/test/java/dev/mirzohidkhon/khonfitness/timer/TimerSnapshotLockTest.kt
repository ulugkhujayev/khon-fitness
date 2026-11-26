package dev.mirzohidkhon.khonfitness.timer

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.data.TimerStart
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A cleanup is held after it read the store and before it removes; a save of a newer run from another thread then
 * waits for it and lands whole.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TimerSnapshotLockTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val prefs get() = app.getSharedPreferences("timer_snapshot", Context.MODE_PRIVATE)

    @After fun close() = TimerSnapshots.clearAll(app)

    private fun ready(id: String, session: String = "c") =
        TimerState(session, "Cardio", buildPhases(60, 240, 0, 1), remaining = 60.0, awaitingStart = true, runId = id)
    private fun running(id: String, run: Int, session: String = "c") = ready(id, session).beginRun(TimerStart(run, null), 0L, 0L)
    private fun ended(id: String, run: Int, session: String = "c") = running(id, run, session).tickAt(100.0, 100_000L).finishedAt(100_000L, 100_000L)

    /**
     * Runs [cleanup] on a context whose first read of [key] waits for [release]; while it waits, [save] runs on another
     * thread. Returns whether the save finished while the cleanup was held.
     */
    private fun heldCleanup(key: String, cleanup: (Context) -> Unit, save: (Context) -> Unit): Boolean {
        val reached = CountDownLatch(1)
        val release = CountDownLatch(1)
        val gate = AtomicBoolean(true)
        val store = prefs
        val wrapped = object : SharedPreferences by store {
            override fun getString(k: String?, defValue: String?): String? {
                val value = store.getString(k, defValue)
                if (k == key && gate.getAndSet(false)) { reached.countDown(); release.await(5, TimeUnit.SECONDS) }
                return value
            }
        }
        val context = object : ContextWrapper(app) {
            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = wrapped
        }
        val workers = Executors.newFixedThreadPool(2)
        try {
            val clear = workers.submit { cleanup(context) }
            assertTrue("the cleanup reads the store", reached.await(5, TimeUnit.SECONDS))
            val saving = workers.submit { save(context) }
            Thread.sleep(300)
            val savedWhileHeld = saving.isDone
            release.countDown()
            clear.get(5, TimeUnit.SECONDS)
            saving.get(5, TimeUnit.SECONDS)
            return savedWhileHeld
        } finally {
            workers.shutdownNow()
        }
    }

    @Test fun `X2 a save between the comparison and the removal of a cleanup waits, and the newer snapshot stays`() {
        val r1 = running("r1", 1)
        TimerSnapshots.save(app, r1)
        val r2 = running("r2", 2)
        assertFalse(heldCleanup("snapshot", { TimerSnapshots.clearRun(it, r1) }, { TimerSnapshots.save(it, r2) }))
        assertEquals("r2", TimerSnapshots.load(app)?.state?.runId)
    }

    @Test fun `X2 a result that ends between the comparison and the removal of a settle stays pending`() {
        val r1 = ended("r1", 1)
        TimerSnapshots.save(app, r1)
        val other = ended("o1", 1, session = "d")
        assertFalse(heldCleanup("pending", { TimerSnapshots.settle(it, r1) }, { TimerSnapshots.save(it, other) }))
        assertEquals(listOf("o1"), TimerSnapshots.pending(app).map { it.runId })
    }

    @Test fun `X3 the cleanup after a stretch run and the removal of an invalid snapshot keep a newer cardio snapshot`() {
        val stretch = TimerState("routine:s", "Mobility", listOf(Phase(PhaseKind.WORK, 30, 0)), remaining = 30.0, stretch = true, runId = "s")
            .begin().tick(40.0)
        assertTrue(stretch.done)
        val cardio = running("r2", 2)
        val saveStretch: () -> Unit = { TimerSnapshots.save(app, stretch) }
        val saveInvalid: () -> Unit = { prefs.edit().putString("snapshot", "{").commit() }
        val cleanups = listOf<Pair<() -> Unit, (Context) -> Unit>>(
            saveStretch to { c -> TimerSnapshots.clearRun(c, stretch) },
            saveStretch to { c -> TimerSnapshots.clear(c, stretch.sessionId) },
            saveInvalid to { c -> TimerSnapshots.load(c) },
        )
        for ((i, pair) in cleanups.withIndex()) {
            val (setup, cleanup) = pair
            TimerSnapshots.clearAll(app)
            setup()
            assertFalse("case $i", heldCleanup("snapshot", cleanup) { TimerSnapshots.save(it, cardio) })
            assertEquals("case $i", "r2", TimerSnapshots.load(app)?.state?.runId)
        }
    }

    @Test fun `the live slot never holds an ended cardio run, and Ready, Stop on Ready, and another timer leave the pending store alone`() {
        val r1 = ended("r1", 1)
        TimerSnapshots.save(app, running("r1", 1))
        TimerSnapshots.save(app, r1)
        assertNull(TimerSnapshots.load(app))
        assertEquals(listOf("r1"), TimerSnapshots.pending(app).map { it.runId })
        val other = ready("o", session = "d")
        TimerSnapshots.save(app, other)
        TimerSnapshots.save(app, other.finishedAt(0L, 0L))
        TimerSnapshots.clearRun(app, other)
        TimerSnapshots.save(app, ready("o2", session = "d"))
        assertEquals(listOf(PendingRun.of(r1)), TimerSnapshots.pending(app))
        assertTrue(TimerSnapshots.hasPending(app))
        TimerSnapshots.settle(app, ended("r1", 2))
        TimerSnapshots.settle(app, ended("other", 1))
        assertEquals(listOf("r1"), TimerSnapshots.pending(app).map { it.runId })
        TimerSnapshots.settle(app, r1)
        assertFalse(TimerSnapshots.hasPending(app))
        assertEquals("o2", TimerSnapshots.load(app)?.state?.runId)
    }

    @Test fun `an ended cardio run that an older build left in the live slot moves to the pending store`() {
        val r1 = ended("r1", 1)
        prefs.edit().putString("snapshot", TimerSnapshot(r1, 0L).encode()).commit()
        assertTrue(TimerSnapshots.exists(app))
        assertNull(TimerSnapshots.load(app))
        assertEquals(listOf(PendingRun.of(r1)), TimerSnapshots.pending(app))
        assertFalse(prefs.contains("snapshot"))
    }
}
