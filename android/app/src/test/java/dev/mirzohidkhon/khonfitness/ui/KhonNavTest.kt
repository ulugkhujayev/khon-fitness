package dev.mirzohidkhon.khonfitness.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KhonNavTest {
    @Test fun tabSwitchShiftsTowardTheTabTapped() {
        assertEquals(1, tabShift(Routes.TODAY, Routes.HISTORY))
        assertEquals(1, tabShift(Routes.TODAY, Routes.PROGRAMS))
        assertEquals(-1, tabShift(Routes.PROGRAMS, Routes.HISTORY))
        assertEquals(-1, tabShift(Routes.HISTORY, Routes.TODAY))
    }

    @Test fun noShiftOutsideATabSwitch() {
        assertNull(tabShift(Routes.TODAY, Routes.TODAY))
        assertNull(tabShift(Routes.TODAY, Routes.SETTINGS))
        assertNull(tabShift(Routes.LIBRARY, Routes.PROGRAMS))
        assertNull(tabShift(null, Routes.TODAY))
    }

    /** UI06 P5 row 10: a pushed screen belongs to the tab it was opened from, not to a tab its route looks like. */
    @Test fun aPushedScreenBelongsToTheTabUnderIt() {
        assertEquals(1, tabOf(listOf(Routes.TODAY, Routes.HISTORY, "lift/{id}")))
        assertEquals(1, tabOf(listOf(Routes.TODAY, Routes.HISTORY, "cardiotype/{id}")))
        assertEquals(1, tabOf(listOf(Routes.TODAY, Routes.HISTORY, "session/{id}", "exercise/{id}")))
        assertEquals(0, tabOf(listOf(Routes.TODAY, Routes.IMPORT)))
        assertEquals(0, tabOf(listOf(Routes.TODAY, "exercise/{id}")))
        assertEquals(2, tabOf(listOf(Routes.TODAY, Routes.PROGRAMS, Routes.LIBRARY, "lift/{id}")))
        assertEquals(0, tabOf(emptyList()))
    }
}
