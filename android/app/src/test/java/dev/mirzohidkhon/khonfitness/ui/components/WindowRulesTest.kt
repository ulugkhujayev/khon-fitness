package dev.mirzohidkhon.khonfitness.ui.components

import dev.mirzohidkhon.khonfitness.data.EatingWindow
import dev.mirzohidkhon.khonfitness.data.WindowDay
import dev.mirzohidkhon.khonfitness.data.iso
import dev.mirzohidkhon.khonfitness.ui.screens.windowStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowRulesTest {
    private val window = EatingWindow(enabled = true, startMinute = 12 * 60, endMinute = 20 * 60)
    private val today = LocalDate.of(2026, 10, 28)
    private fun millis(at: LocalDateTime) = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test fun `state text at the boundaries opens on the opening minute and closes on the closing minute`() {
        assertEquals("Closed · opens 12:00", windowStatus(window, null, today.atTime(11, 59)))
        assertEquals("Open · closes in 8 h 0 m", windowStatus(window, null, today.atTime(12, 0)))
        assertEquals("Open · closes in 1 m", windowStatus(window, null, today.atTime(19, 59)))
        assertEquals("Closed · opens 12:00", windowStatus(window, null, today.atTime(20, 0)))
    }

    @Test fun `state text around midnight follows a late open to midnight and a new day to the hours`() {
        val late = WindowDay(today.iso(), openedAt = millis(today.atTime(22, 0)))
        assertEquals("Open · closes in 1 h 30 m", windowStatus(window, late, today.atTime(22, 30)))
        assertEquals("Open · closes in 1 m", windowStatus(window, late, today.atTime(23, 59)))
        // After midnight the late open belongs to yesterday: the new day has no record and waits for the hours.
        assertEquals("Closed · opens 12:00", windowStatus(window, null, today.plusDays(1).atTime(0, 0)))
        val early = WindowDay(today.iso(), openedAt = millis(today.atTime(0, 30)))
        assertEquals("Open · closes in 19 h 0 m", windowStatus(window, early, today.atTime(1, 0)))
        assertEquals("Off", windowStatus(window.copy(enabled = false), late, today.atTime(22, 30)))
    }

    @Test fun `strip is 28 days oldest first with today last and a manual open or close as changed`() {
        val days = listOf(
            WindowDay(today.minusDays(27).iso(), openedAt = 1L),
            WindowDay(today.minusDays(3).iso(), closedAt = 1L),
            WindowDay(today.minusDays(2).iso(), note = "only a note"),
            WindowDay(today.iso(), openedAt = 1L),
            WindowDay(today.minusDays(40).iso(), openedAt = 1L),
        )
        val strip = windowStrip(days, today)
        assertEquals(28, strip.size)
        assertEquals(StripCell.CHANGED, strip[0])
        assertEquals(StripCell.CHANGED, strip[24])
        assertEquals(StripCell.KEPT, strip[25])
        assertEquals(StripCell.TODAY, strip[27])
        assertEquals(25, strip.count { it == StripCell.KEPT })
    }

    @Test fun `kept count leaves today out, even when today was changed`() {
        val days = (1..7).map { WindowDay(today.minusDays(it.toLong()).iso(), closedAt = 1L) } + WindowDay(today.iso(), openedAt = 1L)
        assertEquals("Kept the hours on 20 of 27 days.", stripSummary(windowStrip(days, today)))
        assertEquals("Kept the hours on 27 of 27 days.", stripSummary(windowStrip(emptyList(), today)))
    }

    @Test fun `day bar shares place the hours and now on a day from 00 00 to 24 00`() {
        val s = dayShares(window, today.atTime(18, 0))
        assertEquals(0.5f, s.from, 1e-6f)
        assertEquals(20f / 24f, s.to, 1e-6f)
        assertEquals(0.75f, s.now, 1e-6f)
        assertEquals(0f, dayShares(window, today.atTime(0, 0)).now, 1e-6f)
        assertEquals(1439f / 1440f, dayShares(window.copy(endMinute = 1439), today.atTime(23, 59)).to, 1e-6f)
    }

    @Test fun `day bar labels center under the ends, stay inside the bar, and keep apart`() {
        // 12:00 to 20:00 on a 240 wide bar: centered under 120 and 200.
        assertEquals(100f to 180f, dayBarLabels(120f, 200f, 240f, 40f, 40f, 4f))
        // Hours from midnight to the last minute: pushed inside both ends.
        assertEquals(0f to 200f, dayBarLabels(0f, 239.8f, 240f, 40f, 40f, 4f))
        // Short hours (10 apart): each label moves out by half the overlap, so they end 44 apart.
        assertEquals(63f to 107f, dayBarLabels(100f, 110f, 240f, 40f, 40f, 4f))
        // Close together at the right end: spread, then moved left to fit.
        assertEquals(156f to 200f, dayBarLabels(230f, 239f, 240f, 40f, 40f, 4f))
    }

    @Test fun `now mark centers on now and stays inside the bar`() {
        assertEquals(179f, dayBarMark(0.75f, 240f, 2f), 1e-4f)
        assertEquals(0f, dayBarMark(0f, 240f, 2f), 1e-4f)
        assertEquals(238f, dayBarMark(1f, 240f, 2f), 1e-4f)
    }
}
