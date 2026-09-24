package dev.mirzohidkhon.khonfitness.timer

import dev.mirzohidkhon.khonfitness.data.DemoClock
import dev.mirzohidkhon.khonfitness.data.DemoStep
import dev.mirzohidkhon.khonfitness.data.StretchDemo
import org.junit.Assert.*
import org.junit.Test

class DemoClockTest {
    private val vsync = 1_000_000_000L / 120
    private val guide = StretchDemo("Test", 64, 8, 512, 400,
        listOf(DemoStep("Set up", "", 0), DemoStep("Move", "", 32), DemoStep("Hold", "", 63)), 32, true)

    @Test fun `first frame starts where the demo stopped`() {
        assertEquals(4200L, DemoClock(4200).elapsedMs(987_654_321))
    }
    @Test fun `ten seconds of 120 Hz frames do not drift`() {
        val clock = DemoClock(0)
        val start = 5_000_000_000L
        val last = (0..1200).map { clock.elapsedMs(start + it * vsync) }.last()
        assertTrue(last in 9_999L..10_000L)
    }
    @Test fun `a 120 Hz clock shows every sprite frame in order and changes it at most 20 times a second`() {
        val clock = DemoClock(0)
        val changes = mutableListOf<Pair<Long, Int>>()
        var shown = -1
        for (i in 0..(7800L * 120 / 1000)) {
            val ms = clock.elapsedMs(i * vsync)
            val frame = guide.previewFrame(ms)
            if (frame != shown) { changes += ms to frame; shown = frame }
        }
        assertEquals((0..63).toList(), changes.map { it.second }.distinct())
        assertTrue(changes.map { it.second }.zipWithNext().all { (a, b) -> b == a + 1 })
        assertTrue(changes.map { it.first }.zipWithNext().all { (a, b) -> b - a >= 40 })
    }
}
