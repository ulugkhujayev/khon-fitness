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
}
