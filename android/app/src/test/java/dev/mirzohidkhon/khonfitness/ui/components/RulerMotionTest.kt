package dev.mirzohidkhon.khonfitness.ui.components

import dev.mirzohidkhon.khonfitness.ui.screens.BODYWEIGHT_RULER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulerMotionTest {
    private val r = BODYWEIGHT_RULER

    /** A parent like Today's entry: it holds the value, the ruler's reports write it. */
    private class Parent(var kg: Double)

    private fun frame(m: RulerMotion, p: Parent, id: Int, to: Float) {
        m.move(id, to, p.kg)?.let { p.kg = it }
    }

    @Test fun `a drag reports each new tick once`() {
        val m = RulerMotion(r, 80.0)
        val id = m.start()
        assertEquals(80.1, m.move(id, 6010f, 80.0)!!, 0.0)
        assertNull(m.move(id, 6012f, 80.1))
        assertEquals(80.2, m.move(id, 6018f, 80.1)!!, 0.0)
        assertEquals(6018f, m.offset, 0f)
    }

    @Test fun `typed value during a fling stops it and the fling never writes over it`() {
        val p = Parent(80.0)
        val m = RulerMotion(r, p.kg)
        val id = m.start()
        frame(m, p, id, 6100f)
        assertEquals(81.0, p.kg, 0.0)
        // The figure takes a typed 75.5 while the ruler still flings.
        p.kg = 75.5
        frame(m, p, id, 6200f)
        frame(m, p, id, 6300f)
        assertEquals(75.5, p.kg, 0.0)
        assertFalse(m.owns(id))
        assertEquals(r.offsetOf(75.5), m.offset, 0f)
        assertEquals(75.5, m.shown, 0.0)
    }

    @Test fun `screen reader adjust during a fling stops it`() {
        val p = Parent(80.0)
        val m = RulerMotion(r, p.kg)
        val id = m.start()
        frame(m, p, id, 6100f)
        // The adjust takes the ruler first, then reports its value.
        assertTrue(m.outside(81.1))
        p.kg = 81.1
        frame(m, p, id, 6300f)
        assertEquals(81.1, p.kg, 0.0)
        assertFalse(m.owns(id))
        assertEquals(r.offsetOf(81.1), m.offset, 0f)
    }

    @Test fun `outside value moves an idle ruler with no report, the same value moves nothing`() {
        val m = RulerMotion(r, 80.0)
        assertTrue(m.outside(70.0))
        assertEquals(5000f, m.offset, 0f)
        assertFalse(m.outside(70.0))
    }

    @Test fun `a value the ruler reported is not outside input`() {
        val p = Parent(80.0)
        val m = RulerMotion(r, p.kg)
        val id = m.start()
        frame(m, p, id, 6100f)
        assertFalse(m.outside(p.kg))
        assertTrue(m.owns(id))
    }

    @Test fun `a new drag takes the ruler from the fling before it`() {
        val m = RulerMotion(r, 80.0)
        val fling = m.start()
        val drag = m.start()
        assertNull(m.move(fling, 6100f, 80.0))
        assertEquals(80.5, m.move(drag, 6050f, 80.0)!!, 0.0)
        // The old fling ends late: the new drag keeps the ruler.
        m.end(fling)
        assertTrue(m.owns(drag))
        m.end(drag)
        assertFalse(m.owns(drag))
    }

    @Test fun `a drag can start again after outside input`() {
        val m = RulerMotion(r, 80.0)
        m.start()
        m.outside(70.0)
        val id = m.start()
        assertEquals(70.1, m.move(id, 5010f, 70.0)!!, 0.0)
    }

    @Test fun `the ruler stays inside its ends and a fling stops there`() {
        val m = RulerMotion(r, 21.0)
        val id = m.start()
        assertEquals(20.0, m.move(id, -400f, 21.0)!!, 0.0)
        assertEquals(0f, m.offset, 0f)
        assertEquals(23000f, r.maxOffset(), 0f)
        assertEquals(23000f, r.clampOffset(99999f), 0f)
        assertTrue(r.atEnd(0f))
        assertTrue(r.atEnd(23000f))
        assertFalse(r.atEnd(6000f))
    }

    @Test fun `typed figure takes only a number above 0`() {
        assertNull(typedFigure(""))
        assertNull(typedFigure("0"))
        assertNull(typedFigure("0."))
        assertNull(typedFigure("."))
        assertEquals(0.5, typedFigure("0.5")!!, 0.0)
        assertEquals(80.0, typedFigure("80.")!!, 0.0)
        assertEquals(80.6, typedFigure("80.6")!!, 0.0)
    }
}
