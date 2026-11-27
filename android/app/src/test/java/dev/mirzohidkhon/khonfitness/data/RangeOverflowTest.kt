package dev.mirzohidkhon.khonfitness.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The range computations near the ends of Long: no sum or difference wraps. */
class RangeOverflowTest {
    private val top = Long.MAX_VALUE

    @Test fun `the app cardio tail at the largest time ends at finishedAt`() {
        val from = top - 30 * MIN
        val cardio = Session("c", localDate(from, TEST_ZONE), from, finishedAt = top, itemType = ItemType.CARDIO, exerciseId = "nor",
            timedFrom = from, timedTo = top - 10_000)
        assertEquals(HrRange(from, top), hrWindow(cardio, TEST_ZONE))
    }

    @Test fun `a range whose length does not fit in a Long is no range`() {
        val low = Long.MIN_VALUE + 1
        val gym = Session("g", localDate(low, TEST_ZONE), low, finishedAt = top, itemType = ItemType.PROGRAM, timedFrom = low, timedTo = top)
        assertNull(hrWindow(gym, TEST_ZONE))
        assertEquals(RangeRefusal.LENGTH, confirmableRange(gym.copy(timedFrom = null, timedTo = null), TEST_ZONE))
    }

    @Test fun `the gym Finish tail after a stamp at the largest time ends at finishedAt`() {
        val start = top - 30 * MIN
        val gym = Session("g", localDate(start, TEST_ZONE), start, itemType = ItemType.PROGRAM, startedElapsed = 0L)
        assertEquals(HrRange(start, top), gymRange(gym, listOf(top - MIN), top, 30 * MIN))
    }
}
