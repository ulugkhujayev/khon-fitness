package dev.mirzohidkhon.khonfitness.timer

import dev.mirzohidkhon.khonfitness.data.DemoStep
import dev.mirzohidkhon.khonfitness.data.StretchDemo
import dev.mirzohidkhon.khonfitness.data.forDemoSide
import org.junit.Assert.*
import org.junit.Test

class StretchDemoTest {
    private val guide = StretchDemo("Test", 64, 8, 384, 304,
        listOf(DemoStep("Set up", "Left leg", 0), DemoStep("Move", "Move left", 32), DemoStep("Hold", "Hold", 63)), 32, true)

    @Test fun `preview starts in setup and pauses at each instruction`() {
        assertEquals(0, guide.previewFrame(0))
        assertEquals(0, guide.previewFrame(2500))
        assertEquals(16, guide.previewFrame(3400))
        assertEquals(32, guide.previewFrame(4400))
        assertEquals(63, guide.previewFrame(7600))
        assertEquals(0, guide.previewFrame(7800))
    }
    @Test fun `all preview frames stay inside the sheet`() {
        for (ms in 0L..20000L step 33) assertTrue(guide.previewFrame(ms) in 0..63)
    }
    @Test fun `instruction describes the movement being demonstrated`() {
        assertEquals(0, guide.stepAt(0))
        assertEquals(1, guide.stepAt(1))
        assertEquals(1, guide.stepAt(32))
        assertEquals(2, guide.stepAt(33))
    }
    @Test fun `exercise repetitions skip the introductory setup`() {
        assertEquals(32, guide.exerciseFrame(0f))
        assertEquals(63, guide.exerciseFrame(1f))
        assertEquals(32, guide.exerciseFrame(-1f))
    }
    @Test fun `timed holds show the held position`() { assertEquals(63, guide.exerciseFrame(null)) }
    @Test fun `right side reverses instructions without damaging other words`() {
        assertEquals("Right hand under your left arm. Stay upright.",
            "Left hand under your right arm. Stay upright.".forDemoSide(true))
        assertEquals("Left hand", "Left hand".forDemoSide(false))
    }
}
