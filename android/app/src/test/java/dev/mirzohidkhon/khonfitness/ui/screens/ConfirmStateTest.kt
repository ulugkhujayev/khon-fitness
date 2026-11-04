package dev.mirzohidkhon.khonfitness.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmStateTest {
    @Test fun askOpensForTheItem() {
        val c = ConfirmState<String>()
        assertNull(c.item)
        c.ask("Squat")
        assertEquals("Squat", c.item)
    }

    @Test fun confirmClosesAndRunsTheActionOnce() {
        val c = ConfirmState<String>()
        val ran = mutableListOf<String>()
        c.ask("Squat")
        c.confirm { ran += it }
        // A second tap on the red button before the sheet goes finds nothing open.
        c.confirm { ran += it }
        assertNull(c.item)
        assertEquals(listOf("Squat"), ran)
    }

    @Test fun cancelRunsNothing() {
        val c = ConfirmState<String>()
        val ran = mutableListOf<String>()
        c.ask("Squat")
        c.close()
        c.confirm { ran += it }
        assertNull(c.item)
        assertTrue(ran.isEmpty())
    }
}
