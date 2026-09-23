package dev.mirzohidkhon.khonfitness.timer

import dev.mirzohidkhon.khonfitness.data.DemoStep
import dev.mirzohidkhon.khonfitness.data.StretchDemo
import dev.mirzohidkhon.khonfitness.data.forDemoSide
import kotlinx.serialization.json.Json
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
    @Test fun `guides without pause metadata keep their original repetition timing`() {
        val legacy = Json.decodeFromString<StretchDemo>("""{
            "name":"Test", "frameCount":64, "columns":8, "frameWidth":384, "frameHeight":304,
            "steps":[{"title":"Move", "instruction":"Move left", "frame":32}],
            "loopStart":32, "moving":true
        }""")
        assertEquals(0f, legacy.repHoldFraction, 0f)
        assertEquals(listOf(32, 40, 48, 55, 63),
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).map(legacy::exerciseFrame))
    }
    @Test fun `elephant pauses at each side then moves through the middle`() {
        val paused = guide.copy(repHoldFraction = 0.2f)
        for (progress in listOf(-1f, 0f, 0.1f, 0.2f)) assertEquals(32, paused.exerciseFrame(progress))
        for (progress in listOf(0.8f, 0.9f, 1f, 2f)) assertEquals(63, paused.exerciseFrame(progress))
        assertTrue(paused.exerciseFrame(0.3f) > 32)
        assertEquals(48, paused.exerciseFrame(0.5f))
        assertTrue(paused.exerciseFrame(0.7f) < 63)
        assertEquals(63, paused.exerciseFrame(null))
    }
    @Test fun `both repetition styles advance without reversing or leaving the movement frames`() {
        for (demo in listOf(guide, guide.copy(repHoldFraction = 0.2f))) {
            val frames = (0..1000).map { demo.exerciseFrame(it / 1000f) }
            assertTrue(frames.all { it in 32..63 })
            assertTrue(frames.zipWithNext().all { (previous, next) -> next >= previous })
            assertEquals((32..63).toList(), frames.distinct())
        }
    }
    @Test fun `right side reverses instructions without damaging other words`() {
        assertEquals("Right hand under your left arm. Stay upright.",
            "Left hand under your right arm. Stay upright.".forDemoSide(true))
        assertEquals("Left hand", "Left hand".forDemoSide(false))
    }
}
