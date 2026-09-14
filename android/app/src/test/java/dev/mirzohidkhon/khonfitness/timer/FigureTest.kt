package dev.mirzohidkhon.khonfitness.timer

import androidx.compose.ui.geometry.Offset
import dev.mirzohidkhon.khonfitness.ui.components.Figures
import org.junit.Assert.*
import org.junit.Test

class FigureTest {
    @Test fun `plow feet are beyond the head on the floor`() {
        val p = Figures.of("plow").second
        assertTrue(p.footF.x < p.head.x && p.footB.x < p.head.x)
        assertTrue(p.hip.y < p.shoulder.y)
        assertEquals(86f, p.footF.y, 1f)
    }

    @Test fun `both ninety ninety knees form a right angle`() {
        val p = Figures.of("ninety").second
        fun dot(a: Offset, b: Offset) = a.x * b.x + a.y * b.y
        assertEquals(0f, dot(p.hip - p.kneeF, p.footF - p.kneeF), .01f)
        assertEquals(0f, dot(p.hip - p.kneeB, p.footB - p.kneeB), .01f)
    }

    @Test fun `needle rests the shoulder and side of head near the floor`() {
        val p = Figures.of("needle").second
        assertTrue(p.shoulder.y >= 80f)
        assertTrue(p.head.y + 4.5f >= 84f)
        assertTrue(p.handF.x > p.shoulder.x)
        assertTrue(p.hip.y < p.shoulder.y)
    }

    @Test fun `rotating arm keeps its length throughout the greatest stretch`() {
        val (a, b) = Figures.of("wgs")
        for (i in 0..100) {
            val t = i / 100f
            val p = a.rotateArm(b, t)
            val expected = (a.elbowF - a.shoulder).getDistance() * (1 - t) + (b.elbowF - b.shoulder).getDistance() * t
            assertEquals(expected, (p.elbowF - p.shoulder).getDistance(), .001f)
            assertTrue(p.handF.x in 0f..100f && p.handF.y in 0f..100f)
            assertEquals(a.footF, p.footF)
            assertEquals(a.footB, p.footB)
        }
    }

    @Test fun `held stretches do not loop in previews`() {
        for (key in listOf("needle", "ninety", "plow")) assertFalse(key in Figures.moving)
        assertTrue("wgs" in Figures.moving)
    }
}
