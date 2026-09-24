package dev.mirzohidkhon.khonfitness.timer

import dev.mirzohidkhon.khonfitness.data.EatingWindow
import dev.mirzohidkhon.khonfitness.data.EatingWindowRules
import dev.mirzohidkhon.khonfitness.data.WindowDay
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EatingWindowRulesTest {
    private val hours = EatingWindow(enabled = true, startMinute = 12 * 60, endMinute = 20 * 60)
    private val date = "2026-09-23"
    private fun at(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 9, 23, hour, minute)
    private fun epoch(time: LocalDateTime) = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun `open now after scheduled end stays open until manually closed or midnight`() {
        val opened = WindowDay(date, openedAt = epoch(at(20, 30)))
        val late = EatingWindowRules.state(hours, opened, at(20, 31))
        assertTrue(late.open)
        assertEquals(LocalDateTime.of(2026, 9, 24, 0, 0), late.changesAt)

        val closed = EatingWindowRules.state(hours, opened.copy(closedAt = epoch(at(20, 45))), at(20, 46))
        assertFalse(closed.open)
    }

    @Test fun `opening early does not reopen after the scheduled end`() {
        val opened = WindowDay(date, openedAt = epoch(at(11, 30)))
        assertTrue(EatingWindowRules.state(hours, opened, at(11, 31)).open)
        assertFalse(EatingWindowRules.state(hours, opened, at(20, 31)).open)
    }
}
