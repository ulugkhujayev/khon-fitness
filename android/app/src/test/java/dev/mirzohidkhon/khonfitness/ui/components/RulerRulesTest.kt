package dev.mirzohidkhon.khonfitness.ui.components

import dev.mirzohidkhon.khonfitness.ui.screens.BODYWEIGHT_RULER
import org.junit.Assert.assertEquals
import org.junit.Test

class RulerRulesTest {
    private val r = BODYWEIGHT_RULER

    @Test fun `bodyweight ruler has a tick every 0_1 kg from 20 to 250`() {
        assertEquals(2301, r.tickCount())
        assertEquals(20.0, r.tickValue(0), 0.0)
        assertEquals(250.0, r.tickValue(2300), 0.0)
    }

    @Test fun `tick values do not drift`() {
        assertEquals(80.6, r.tickValue(r.tickIndex(80.6)), 0.0)
        assertEquals(80.6, r.valueAt(r.offsetOf(80.6)), 0.0)
    }

    @Test fun `values off the ruler land on its ends`() {
        assertEquals(0, r.tickIndex(5.0))
        assertEquals(2300, r.tickIndex(400.0))
        assertEquals(20.0, r.valueAt(-50f), 0.0)
        assertEquals(250.0, r.valueAt(1e9f), 0.0)
    }

    @Test fun `offset is ten dp per tick and snaps to the nearest one`() {
        assertEquals(6060f, r.offsetOf(80.6), 0.001f)
        assertEquals(610f, r.snapOffset(606f), 0f)
        assertEquals(600f, r.snapOffset(604.9f), 0f)
        assertEquals(0f, r.snapOffset(-30f), 0f)
        assertEquals(23000f, r.snapOffset(99999f), 0f)
    }

    @Test fun `every kilogram is major, every half kilogram half, the rest minor`() {
        assertEquals(TickKind.MAJOR, r.tickKind(600))
        assertEquals(TickKind.HALF, r.tickKind(605))
        assertEquals(TickKind.MINOR, r.tickKind(606))
    }
}
