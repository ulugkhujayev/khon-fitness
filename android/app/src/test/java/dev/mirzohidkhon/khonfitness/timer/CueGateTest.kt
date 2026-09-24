package dev.mirzohidkhon.khonfitness.timer

import org.junit.Assert.*
import org.junit.Test

class CueGateTest {
    @Test fun `a lone cue speaks at once`() {
        val gate = CueGate()
        assertEquals("Go", gate.offer("Go", 1000))
        assertNull(gate.dueAt())
    }
    @Test fun `rapid skips speak only the latest cue after the gap`() {
        val gate = CueGate()
        assertEquals("Ready for a", gate.offer("Ready for a", 0))
        assertNull(gate.offer("Ready for b", 100))
        assertNull(gate.offer("Ready for c", 200))
        assertEquals(300L, gate.dueAt())
        assertNull(gate.poll(299))
        assertEquals("Ready for c", gate.poll(300))
        assertNull(gate.poll(900))
    }
    @Test fun `cues spaced wider than the gap all speak`() {
        val gate = CueGate()
        assertEquals(listOf("3", "2", "1"), listOf(0L, 1000L, 2000L).zip(listOf("3", "2", "1")).map { (t, cue) -> gate.offer(cue, t) })
    }
    @Test fun `no burst ever speaks twice inside the gap`() {
        val gate = CueGate()
        val spoken = mutableListOf<Long>()
        for (t in 0L..3000L step 10) {
            if (t % 70 == 0L) gate.offer("cue $t", t)?.let { spoken += t }
            gate.poll(t)?.let { spoken += t }
        }
        assertTrue(spoken.zipWithNext().all { (a, b) -> b - a >= 300 })
        assertTrue(spoken.size >= 9)
    }
    @Test fun `cancel drops the waiting cue and asks for stop only after speech`() {
        val gate = CueGate()
        assertFalse(gate.cancel())
        gate.offer("a", 0)
        gate.offer("b", 100)
        assertTrue(gate.cancel())
        assertFalse(gate.cancel())
        assertNull(gate.poll(400))
        assertNull(gate.dueAt())
    }
}
