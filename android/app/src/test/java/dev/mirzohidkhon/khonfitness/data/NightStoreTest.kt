package dev.mirzohidkhon.khonfitness.data

import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class NightStoreTest {
    private class FakeSleep(var answer: suspend (HrRange) -> List<SleepSpan>) : SleepSource {
        val calls = mutableListOf<HrRange>()
        var gate: CompletableDeferred<Unit>? = null
        val reached = CompletableDeferred<Unit>()
        override suspend fun spans(range: HrRange): List<SleepSpan> {
            synchronized(this) { calls += range }
            gate?.let { reached.complete(Unit); it.await() }
            return answer(range)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val nov3 = LocalDate.of(2026, 11, 3)
    private val night = SleepSpan("com.mi.health", at(23, 0, nov3.minusDays(1)), at(6, 0, nov3))
    private val clocks = FakeClocks(wall = at(9, 0, LocalDate.of(2026, 11, 20)))
    private val source = FakeSleep { listOf(night) }
    private val store = NightStore(source, scope, clocks) { TEST_ZONE }

    @After fun end() = scope.cancel()

    private fun refresh(force: Boolean = false) = runBlocking { withTimeout(5_000) { store.refresh(force) } }
    private fun loaded() = (store.state.value as NightsState.Loaded).nights

    @Test fun `D13 the first refresh reads the last 1100 days and loads the nights`() {
        assertEquals(NightsState.NotRead, store.state.value)
        refresh()
        assertEquals(listOf(HrRange(clocks.wall - 1_100 * DAY_MS, clocks.wall)), source.calls)
        assertEquals(7 * 3600, loaded().getValue(nov3).asleepSec)
    }

    @Test fun `D13 a refresh within 5 minutes of a good read does not read, after 6 minutes it does`() {
        refresh()
        clocks.pass(4 * MIN)
        refresh()
        assertEquals(1, source.calls.size)
        clocks.pass(2 * MIN)
        refresh()
        assertEquals(2, source.calls.size)
    }

    @Test fun `D13 force reads at once`() {
        refresh()
        refresh(force = true)
        assertEquals(2, source.calls.size)
    }

    @Test fun `D13 two calls at one time make one read`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        source.gate = gate
        withTimeout(5_000) {
            val a = async(Dispatchers.Default) { store.refresh(force = true) }
            source.reached.await()
            val b = async(Dispatchers.Default) { store.refresh() }
            val c = async(Dispatchers.Default) { store.refresh() }
            gate.complete(Unit)
            a.await(); b.await(); c.await()
        }
        assertEquals(1, source.calls.size)
        assertEquals(setOf(nov3), loaded().keys)
    }

    @Test fun `D13 a failed read keeps the nights loaded before`() {
        refresh()
        source.answer = { throw IllegalStateException("provider error") }
        refresh(force = true)
        assertEquals(2, source.calls.size)
        assertEquals(setOf(nov3), loaded().keys)
    }

    @Test fun `D13 a failed read with nothing loaded before is failed`() {
        source.answer = { throw IllegalStateException("provider error") }
        refresh()
        assertEquals(NightsState.Failed, store.state.value)
    }

    @Test fun `D13 a read that finds the permission gone moves to no access and clears the nights`() {
        refresh()
        source.answer = { throw NoSleepAccess("no sleep permission") }
        refresh(force = true)
        assertEquals(NightsState.NoAccess, store.state.value)
        source.answer = { listOf(night) }
        refresh()
        assertEquals(3, source.calls.size)
        assertEquals(setOf(nov3), loaded().keys)
    }
}
