package dev.mirzohidkhon.khonfitness.timer

import dev.mirzohidkhon.khonfitness.data.DemoClock
import dev.mirzohidkhon.khonfitness.data.DemoStep
import dev.mirzohidkhon.khonfitness.data.StretchDemo
import org.junit.Assert.*
import org.junit.Test

class DemoClockTest {
    private val vsync = 1_000_000_000L / 120
    private val guide = StretchDemo("Test",
        listOf(DemoStep("Set up", "", "start"), DemoStep("Move", "", "start"), DemoStep("Hold", "", "hold")), moving = false)

    @Test fun `first frame starts where the demo stopped`() {
        assertEquals(4200L, DemoClock(4200).elapsedMs(987_654_321))
    }
    @Test fun `ten seconds of 120 Hz frames do not drift`() {
        val clock = DemoClock(0)
        val start = 5_000_000_000L
        val last = (0..1200).map { clock.elapsedMs(start + it * vsync) }.last()
        assertTrue(last in 9_999L..10_000L)
    }
    @Test fun `a 120 Hz clock shows every step in order, each for 2,6 s`() {
        val clock = DemoClock(0)
        val changes = mutableListOf<Pair<Long, Int>>()
        var shown = -1
        for (i in 0..(10_000L * 120 / 1000)) {
            val ms = clock.elapsedMs(i * vsync)
            val step = guide.previewStep(ms)
            if (step != shown) { changes += ms to step; shown = step }
        }
        assertEquals(listOf(0, 1, 2, 0), changes.map { it.second })
        assertTrue(changes.map { it.first }.zipWithNext().all { (a, b) -> b - a in 2600L..2609L })
    }
}
