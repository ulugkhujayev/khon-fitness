package dev.mirzohidkhon.khonfitness.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiRulesTest {
    @Test fun heldStepperRepeatsFasterDownToFifty() {
        assertEquals(350L, REPEAT_AFTER_MS)
        assertEquals(listOf(160L, 145L, 130L, 115L, 100L, 85L, 70L, 55L, 50L, 50L), (0..9).map(::repeatDelay))
        assertEquals(50L, repeatDelay(100))
    }

    @Test fun stepsStayRoundAndInsideBounds() {
        assertEquals(0.3, stepValue(0.2, 1, 0.1), 0.0)
        assertEquals(62.5, stepValue(60.0, 1, 2.5), 0.0)
        assertEquals(0.0, stepValue(1.0, -1, 2.5), 0.0)
        assertEquals(10.0, stepValue(10.0, 1, 1.0, min = 1.0, max = 10.0), 0.0)
    }

    @Test fun axisIsWidestLabelPlusGap() {
        assertEquals(36f, axisWidth(listOf(20f, 27.2f, 12f), 8f), 0f)
        assertEquals(8f, axisWidth(emptyList(), 8f), 0f)
    }

    @Test fun noResultsNamesTheQuery() {
        assertEquals("No Results for “row”" to "Check the spelling or try a new search.", noResults("  row "))
        assertEquals("No Results" to "Try another filter.", noResults(" "))
    }

    @Test fun searchIgnoresCaseAndSpaces() {
        assertTrue(matchesQuery("Barbell Row", " row"))
        assertTrue(matchesQuery("Squat", ""))
        assertFalse(matchesQuery("Squat", "press"))
    }

    @Test fun largeTitleFoldsSixtyToFortyEight() {
        val f = LargeTitleFold()
        assertEquals(60f, f.height, 0f)
        assertEquals(48f, f.folded, 0f)
        assertEquals(12f, f.distance, 0f)
        assertEquals(0f, f.barShift(0f), 0f)
        assertEquals(6f, f.barShift(6f), 0f)
        assertEquals(12f, f.barShift(300f), 0f)
        // The row ends centered in the folded bar: 12 up with the bar, 2 below its top.
        assertEquals(10f, f.rowShift(40f), 0f)
        assertEquals(1f, f.titleScale(0f), 0f)
        assertEquals(0.8f, f.titleScale(20f), 1e-6f)
        assertEquals(0.6f, f.titleScale(80f), 0f)
        assertEquals(0f, f.hairline(16f), 0f)
        assertEquals(0.5f, f.hairline(20f), 0f)
        assertEquals(1f, f.hairline(24f), 0f)
    }

    @Test fun largeFontKeepsTheFoldDistance() {
        val f = LargeTitleFold(row = 66f)
        assertEquals(82f, f.height, 0f)
        assertEquals(12f, f.distance, 0f)
    }

    @Test fun navHairlineFadesOverSixteen() {
        assertEquals(0f, navHairline(0f), 0f)
        assertEquals(0.5f, navHairline(8f), 0f)
        assertEquals(1f, navHairline(40f), 0f)
    }

    @Test fun barHairlineFollowsScrollInDp() {
        // 2.75 px per dp: 22 px is 8 dp, half way.
        assertEquals(0f, barHairline(0, 2.75f), 0f)
        assertEquals(0.5f, barHairline(22, 2.75f), 1e-6f)
        assertEquals(1f, barHairline(44, 2.75f), 0f)
        assertEquals(1f, barHairline(listBarOffset(1, 0), 2.75f), 0f)
    }

    @Test fun listPastItsFirstItemCountsAsScrolledAllTheWay() {
        assertEquals(0, listBarOffset(0, 0))
        assertEquals(30, listBarOffset(0, 30))
        assertEquals(Int.MAX_VALUE, listBarOffset(1, 0))
        assertEquals(Int.MAX_VALUE, listBarOffset(4, 12))
    }

    @Test fun slideSpringMatchesTheExpoSpring() {
        val s = springOf(damping = 20f, stiffness = 240f, mass = 0.8f)
        assertEquals(0.7217f, s.dampingRatio, 1e-4f)
        assertEquals(300f, s.stiffness, 1e-3f)
    }
}
