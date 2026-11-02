package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.data.EatingWindow
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.WindowDay
import dev.mirzohidkhon.khonfitness.health.ImportDraft
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayRulesTest {
    private val start = 1_000_000L
    private fun min(m: Long) = start + m * 60_000

    @Test fun `progress line has no time in the first minute, then minutes, then hours`() {
        assertEquals("In progress", progressLine(start, start + 59_999))
        assertEquals("In progress · 1 min", progressLine(start, min(1)))
        assertEquals("In progress · 34 min", progressLine(start, min(34)))
        assertEquals("In progress · 1 h 5 min", progressLine(start, min(65)))
    }

    private fun log(id: String, session: String, status: String) =
        SetLog(id, session, 0, false, 0, 0, 0, "e", "Bench", 8, 12, 2.5, status = status)

    @Test fun `set progress counts the done sets of the running session only`() {
        val logs = listOf(log("1", "s", SetStatus.DONE), log("2", "s", SetStatus.SKIPPED), log("3", "s", SetStatus.PENDING), log("4", "other", SetStatus.DONE))
        assertEquals(SetProgress(1, 3), setProgress("s", logs))
    }

    @Test fun `routine line says its length and count, singular and plural, and done today`() {
        assertEquals("10 min · 9 stretches", routineSummary(600, 9))
        assertEquals("1 min · 1 stretch", routineSummary(45, 1))
        assertEquals("10 min · 9 stretches", routineLine("10 min · 9 stretches", doneToday = false))
        assertEquals("Done today · 10 min · 9 stretches", routineLine("10 min · 9 stretches", doneToday = true))
    }

    private val window = EatingWindow(enabled = true, startMinute = 12 * 60, endMinute = 20 * 60)
    private val day = LocalDate.of(2026, 10, 5)

    @Test fun `window line is open with time to close, closed with the opening hour, or off`() {
        assertEquals("Open · closes in 3 h 20 m", windowStatus(window, null, day.atTime(16, 40)))
        assertEquals("Closed · opens 12:00", windowStatus(window, null, day.atTime(9, 0)))
        assertEquals("Off", windowStatus(window.copy(enabled = false), null, day.atTime(16, 40)))
        assertEquals("Off", windowStatus(null, null, day.atTime(16, 40)))
    }

    @Test fun `window line follows a manual close`() {
        val closed = WindowDay(day.toString(), closedAt = day.atTime(15, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        assertEquals("Closed · opens 12:00", windowStatus(window, closed, LocalDateTime.of(day, java.time.LocalTime.of(16, 40))))
    }

    @Test fun `band row and import title count sessions, singular and plural`() {
        assertEquals("1 new session", pendingTitle(1))
        assertEquals("2 new sessions", pendingTitle(2))
        assertEquals("1 new session from the band", pendingLine(1))
        assertEquals("3 new sessions from the band", pendingLine(3))
    }

    @Test fun `import button names one, all, or the matched ones`() {
        assertEquals("Import 1 session", importAllLabel(1, 1))
        assertEquals("Import all 3 sessions", importAllLabel(3, 3))
        assertEquals("Import 2 matched", importAllLabel(2, 3))
    }

    @Test fun `import row line gives the date and the numbers it has`() {
        val zone = ZoneId.systemDefault()
        val from = LocalDateTime.of(2026, 10, 5, 7, 0).atZone(zone).toInstant()
        val d = ImportDraft("x", from, from.plusSeconds(32 * 60 + 40), "Running", "run", 5200, 141, emptyList(), listOf("com.xiaomi.wearable"))
        assertEquals("${LocalDate.of(2026, 10, 5).format(shortDate)} · 32 min · 5.2 km · 141 bpm", draftStats(d))
        assertEquals("${LocalDate.of(2026, 10, 5).format(shortDate)} · 32 min", draftStats(d.copy(distanceM = null, avgHr = null)))
    }

    private val today = LocalDate.of(2026, 10, 7)
    private val weights = listOf(Bodyweight("2026-10-01", 80.0), Bodyweight("2026-10-05", 80.4))

    @Test fun `weight change compares with the last day before today`() {
        assertEquals("+0.2 kg since Oct 5", changeSince(weights, today, 80.6))
        assertEquals("−0.4 kg since Oct 5", changeSince(weights, today, 80.0))
        assertEquals("Same as Oct 5", changeSince(weights, today, 80.4))
        // Today's own entry is not the base: an update still compares with the day before.
        assertEquals("+0.2 kg since Oct 5", changeSince(weights + Bodyweight("2026-10-07", 81.0), today, 80.6))
        assertEquals("", changeSince(listOf(Bodyweight("2026-10-07", 81.0)), today, 80.6))
    }

    @Test fun `entry button is Add for a new day, Update for another value, Saved when equal`() {
        assertEquals(EntryAction("Add", true), entryAction(weights, today, 80.6))
        val withToday = weights + Bodyweight("2026-10-07", 80.6)
        assertEquals(EntryAction("Update", true), entryAction(withToday, today, 80.7))
        assertEquals(EntryAction("Saved", false), entryAction(withToday, today, 80.6))
    }

    @Test fun `ruler starts at the latest entry, or at 70 kg before the first`() {
        assertEquals(80.4, startKg(weights), 0.0)
        assertEquals(70.0, startKg(emptyList()), 0.0)
    }
}
