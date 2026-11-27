package dev.mirzohidkhon.khonfitness.timer

import dev.mirzohidkhon.khonfitness.data.DemoStep
import dev.mirzohidkhon.khonfitness.data.StepNode
import dev.mirzohidkhon.khonfitness.data.StretchDemo
import dev.mirzohidkhon.khonfitness.data.stepNodes
import dev.mirzohidkhon.khonfitness.data.forDemoSide
import org.junit.Assert.*
import org.junit.Test

class StretchDemoTest {
    private val guide = StretchDemo("Test",
        listOf(DemoStep("Set up", "Left leg", "setup"), DemoStep("Move", "Move left", "start"), DemoStep("Hold", "Hold", "hold")),
        moving = true, reps = listOf("start", "hold"))
    private val still = guide.copy(moving = false, reps = emptyList())

    @Test fun `preview shows each step for 2,6 s in turn, then starts again`() {
        assertEquals(listOf(0, 0, 1, 1, 2, 2, 0), listOf(0L, 2599, 2600, 5199, 5200, 7799, 7800).map(guide::previewStep))
        assertEquals(0, guide.previewStep(-5))
    }
    @Test fun `the timeline line fills toward the next step during a step`() {
        assertEquals(0f, guide.previewFill(0), 0f)
        assertEquals(0.25f, guide.previewFill(1300), 0.001f)
        assertEquals(0.5f, guide.previewFill(2600), 0f)
        assertEquals(0.75f, guide.previewFill(3900), 0.001f)
        assertEquals(1f, guide.previewFill(5200), 0f)
        assertEquals(1f, guide.previewFill(7000), 0f)
        val fills = (0L until 7800L step 20).map(guide::previewFill)
        assertTrue(fills.zipWithNext().all { (a, b) -> b >= a })
    }
    @Test fun `a selected step fills the line up to its node`() {
        assertEquals(listOf(0f, 0.5f, 1f), (0..2).map(guide::stepFill))
    }
    @Test fun `a one-step guide has no line to fill`() {
        val one = guide.copy(steps = guide.steps.take(1))
        assertEquals(0f, one.previewFill(4000), 0f)
        assertEquals(0f, one.stepFill(0), 0f)
    }
    @Test fun `a timed hold shows the last step and its pose`() {
        assertEquals("hold", guide.exercisePose(null))
        assertEquals(2, guide.exerciseStep(null))
        assertEquals("hold", still.exercisePose(0.2f))
        assertEquals(2, still.exerciseStep(0.2f))
    }
    @Test fun `reps show the first rep pose below half way and the second from half way`() {
        assertEquals(listOf("start", "start", "hold", "hold"), listOf(0f, 0.49f, 0.5f, 1f).map(guide::exercisePose))
        assertEquals(listOf(1, 1, 2, 2), listOf(0f, 0.49f, 0.5f, 1f).map(guide::exerciseStep))
    }
    @Test fun `a rep pose no step names keeps the last instruction`() {
        assertEquals(2, guide.copy(reps = listOf("other", "hold")).exerciseStep(0f))
    }
    @Test fun `right side reverses instructions without damaging other words`() {
        assertEquals("Right hand under your left arm. Stay upright.",
            "Left hand under your right arm. Stay upright.".forDemoSide(true))
        assertEquals("Left hand", "Left hand".forDemoSide(false))
    }
    @Test fun `timeline nodes before the shown step are done, the shown one stands out, later ones wait`() {
        assertEquals(listOf(StepNode.SHOWN, StepNode.LATER, StepNode.LATER), stepNodes(3, 0))
        assertEquals(listOf(StepNode.DONE, StepNode.SHOWN, StepNode.LATER), stepNodes(3, 1))
        assertEquals(listOf(StepNode.DONE, StepNode.DONE, StepNode.SHOWN), stepNodes(3, 2))
    }
}
