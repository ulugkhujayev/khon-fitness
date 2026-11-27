package dev.mirzohidkhon.khonfitness.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class LineChartStepTest {
    @Test fun stepsAbove100KeepThreeOrFourLines() {
        assertEquals(1.0, niceStep(3.0, 3), 0.0)
        assertEquals(100.0, niceStep(290.0, 3), 0.0)
        assertEquals(1000.0, niceStep(1568.0, 3), 0.0)
        assertEquals(1000.0, niceStep(2900.0, 3), 0.0)
    }
}
