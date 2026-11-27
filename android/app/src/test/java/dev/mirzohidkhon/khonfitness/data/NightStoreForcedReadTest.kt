package dev.mirzohidkhon.khonfitness.data

import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NightStoreForcedReadTest {
    @Test fun `a forced refresh during a read without access reads again and loads the nights`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val zone = ZoneId.of("Asia/Tashkent")
        val day = LocalDate.of(2026, 11, 26)
        fun at(hour: Int) = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        val granted = AtomicBoolean(false)
        val reads = AtomicInteger()
        val firstStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val source = SleepSource {
            val access = granted.get()
            reads.incrementAndGet()
            firstStarted.complete(Unit)
            release.await()
            if (!access) throw NoSleepAccess("no access when the read began")
            listOf(SleepSpan("band", at(0), at(7)))
        }
        val store = NightStore(source, scope, FakeClocks(wall = at(9))) { zone }
        try {
            val first = launch { store.refresh() }
            firstStarted.await()
            granted.set(true)
            val forced = launch { store.refresh(force = true) }
            yield()
            release.complete(Unit)
            joinAll(first, forced)
            assertEquals(2, reads.get())
            val state = store.state.value
            assertTrue("$state", state is NightsState.Loaded && state.nights.isNotEmpty())
        } finally {
            scope.cancel()
        }
    }
}
