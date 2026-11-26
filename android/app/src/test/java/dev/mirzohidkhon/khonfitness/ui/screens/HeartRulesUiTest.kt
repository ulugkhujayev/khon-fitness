package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.components.ChartPoint
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartRulesUiTest {
    private val zone = TEST_ZONE
    private val day = LocalDate.of(2026, 11, 20)
    private val gymRange = HrRange(at(18, 2), at(19, 14))

    private fun gym(id: String = "g", r: HrRange? = gymRange, avg: Int? = null, date: LocalDate = day) =
        Session(id, date.iso(), r?.fromMs ?: at(18, 2, date), finishedAt = r?.toMs ?: at(18, 42, date), itemType = ItemType.PROGRAM, programId = "p", programName = "Push",
            timedFrom = r?.fromMs, timedTo = r?.toMs, avgHr = avg)

    private fun cardio(avg: Int? = null) = Session("c", day.iso(), at(7, 0), finishedAt = at(7, 31), itemType = ItemType.CARDIO, exerciseId = "z2",
        exerciseName = "Zone 2 run", timedFrom = at(7, 0), timedTo = at(7, 30), avgHr = avg)

    private fun row(s: Session, state: String = HrState.FILLED, avg: Int? = 128, max: Int? = 164, samples: String = "0:120,5:130", raw: Int = 412,
        used: Int = 398, origin: String? = "com.mi.health", origins: String = "com.mi.health", first: Long? = null, last: Long? = null, gap: Int? = 45): SessionHr {
        val r = hrWindow(s, zone)!!
        return SessionHr(s.id, r.fromMs, r.toMs, state, samples, raw, used, origin, origins, first ?: r.fromMs, last ?: (r.toMs - 60_000), gap,
            avg.takeIf { state == HrState.FILLED }, max.takeIf { state == HrState.FILLED }, fetchedAt = at(20, 0))
    }

    private fun facts(s: Session, r: SessionHr?) = hrFacts(s, r, zone)

    @Test fun `Review's line holds avg and max, one of them, or nothing, for gym and cardio`() {
        for (s in listOf(gym(), cardio())) {
            val both = facts(s, row(s))
            assertEquals("avg 128 · max 164 bpm", hrLine(s, both))
            assertEquals("average 128, maximum 164 beats per minute", hrSpoken(s, both))
            val none = facts(s, null)
            assertNull(hrLine(s, none)); assertNull(hrSpoken(s, none))
            val typed = s.copy(avgHr = 140)
            assertEquals("avg 140 bpm", hrLine(typed, none))
            assertEquals("average 140 beats per minute", hrSpoken(typed, none))
            assertEquals("avg 140 · max 164 bpm", hrLine(typed, facts(typed, row(typed))))
        }
        val s = cardio()
        val maxOnly = HrFacts.Filled(row(s).copy(avgHr = null))
        assertEquals("max 164 bpm", hrLine(s, maxOnly))
        assertEquals("maximum 164 beats per minute", hrSpoken(s, maxOnly))
    }

    @Test fun `a stale, removed, or waiting row shows no measured value anywhere`() {
        val s = gym()
        val stale = row(s).copy(fromMs = gymRange.fromMs + 60_000)
        for (r in listOf(stale, row(s, HrState.WAITING), row(s, HrState.NONE), row(s).copy(state = HrState.OFF))) {
            val f = facts(s, r)
            assertNull(r.state, hrLine(s, f)); assertNull(shownAvgHr(s, f)); assertNull(shownMaxHr(f)); assertNull(measuredAvgHr(f))
        }
        assertNull(hrLine(s.copy(timedTo = null), facts(s.copy(timedTo = null), row(s))))
    }

    @Test fun `typed wins, measured fills the gaps, nothing where neither exists, also for rounds`() {
        val s = cardio()
        val r = row(s, samples = "0:120,5:130,300:150,305:160")
        val f = facts(s, r)
        assertEquals(128, shownAvgHr(s, f))
        assertEquals(140, shownAvgHr(s.copy(avgHr = 140), f))
        assertNull(shownAvgHr(s, facts(s, null)))
        assertEquals(140, shownAvgHr(s.copy(avgHr = 140), facts(s, null)))
        val typed = IntervalLog("i1", "c", 0, avgHr = 111, startedAt = at(7, 0), endedAt = at(7, 1))
        val measured = IntervalLog("i2", "c", 1, startedAt = at(7, 5), endedAt = at(7, 6))
        val empty = IntervalLog("i3", "c", 2, startedAt = at(7, 20), endedAt = at(7, 21))
        val untimed = IntervalLog("i4", "c", 3)
        assertEquals(111, shownRoundHr(typed, f))
        assertEquals(155, shownRoundHr(measured, f))
        assertNull(shownRoundHr(empty, f))
        assertNull(shownRoundHr(untimed, f))
        assertNull(shownRoundHr(measured, facts(s, null)))
        val map = factsById(listOf(s, gym()), listOf(r), zone)
        assertEquals(HrFacts.Filled(r), map["c"]); assertEquals(HrFacts.NotRead, map["g"])
    }

    @Test fun `the list states, the 30 days, finished only, newest first`() {
        val s = gym()
        assertEquals("avg 128 · max 164", hrStateText(s, facts(s, row(s))))
        assertEquals("average 128, maximum 164 beats per minute", hrStateSpoken(s, facts(s, row(s))))
        val typed = gym(avg = 150)
        assertEquals("avg 128 · max 164", hrStateText(typed, facts(typed, row(typed))))
        assertEquals("average 128, maximum 164 beats per minute", hrStateSpoken(typed, facts(typed, row(typed))))
        assertEquals("avg 150 · max 164 bpm", hrLine(typed, facts(typed, row(typed))))
        assertEquals("Waiting for data", hrStateText(s, facts(s, row(s, HrState.WAITING))))
        assertEquals("No data", hrStateText(s, facts(s, row(s, HrState.NONE))))
        assertEquals("Removed", hrStateText(s, facts(s, row(s).copy(state = HrState.OFF))))
        assertEquals("Not read", hrStateText(s, facts(s, null)))
        assertEquals("Not read", hrStateText(s, facts(s, row(s).copy(toMs = gymRange.toMs + 1))))
        assertEquals("No time range", hrStateText(gym(r = null), facts(gym(r = null), null)))

        val today = LocalDate.of(2026, 12, 20)
        val d30 = gym("d30", date = today.minusDays(30))
        val d31 = gym("d31", date = today.minusDays(31))
        val todayEarly = gym("t1", date = today).copy(startedAt = at(7, 0, today))
        val todayLate = gym("t2", date = today).copy(startedAt = at(19, 0, today))
        val open = gym("open", date = today).copy(finishedAt = null)
        val yesterday = gym("y", date = today.minusDays(1))
        assertEquals(listOf("t2", "t1", "y", "d30"), hrDataSessions(listOf(d31, d30, todayEarly, open, yesterday, todayLate), today).map { it.id })
    }

    @Test fun `the sheet's facts for a filled, a sparse, a two-package and an empty row, absent when null`() {
        val s = gym()
        val label = { p: String -> if (p == "com.mi.health") "Mi Fitness" else p }
        assertEquals(
            listOf("Time range" to "18:02 to 19:14", "Readings" to "412 read · 398 used", "First reading" to "18:02", "Last reading" to "19:13",
                "Longest gap" to "45 s", "Source" to "Mi Fitness", "Average" to "128 bpm", "Maximum" to "164 bpm"),
            hrFactRows(s, facts(s, row(s)), emptyList(), zone, label),
        )
        val sparse = row(s, HrState.WAITING, raw = 3, used = 3, first = at(18, 3), last = at(18, 20), gap = 960)
        assertEquals(
            listOf("Time range" to "18:02 to 19:14", "Heart rate" to "Waiting for data", "Readings" to "3 read · 3 used", "First reading" to "18:03",
                "Last reading" to "18:20", "Longest gap" to "16 min", "Source" to "Mi Fitness"),
            hrFactRows(s, facts(s, sparse), emptyList(), zone, label),
        )
        val one = row(s, HrState.WAITING, raw = 1, used = 1, first = at(18, 3), last = at(18, 3), gap = 0)
        assertFalse(hrFactRows(s, facts(s, one), emptyList(), zone, label).any { it.first == "Longest gap" })
        val two = row(s, origins = "com.mi.health,com.strava")
        assertTrue("Also seen" to "com.strava" in hrFactRows(s, facts(s, two), emptyList(), zone, label))
        val empty = SessionHr("g", gymRange.fromMs, gymRange.toMs, HrState.NONE, fetchedAt = at(20, 0))
        assertEquals(listOf("Time range" to "18:02 to 19:14", "Heart rate" to "No data"), hrFactRows(s, facts(s, empty), emptyList(), zone, label))
        assertEquals(listOf("Heart rate" to "No time range"), hrFactRows(gym(r = null), HrFacts.NoRange, emptyList(), zone, label))
        assertEquals(listOf("Time range" to "18:02 to 19:14", "Heart rate" to "Removed"), hrFactRows(s, facts(s, row(s).copy(state = HrState.OFF)), emptyList(), zone, label))
    }

    @Test fun `the sheet shows each work round's average and clock times in the phone's zone`() {
        val s = cardio()
        val r = row(s, samples = "0:120,5:130,300:150,305:160")
        val rounds = listOf(IntervalLog("i1", "c", 0, startedAt = at(7, 0), endedAt = at(7, 1)), IntervalLog("i2", "c", 1, startedAt = at(7, 5), endedAt = at(7, 6)),
            IntervalLog("i3", "c", 2, startedAt = at(7, 20), endedAt = at(7, 21)))
        val rows = hrFactRows(s, facts(s, r), rounds, zone) { it }
        assertTrue(rows.toString(), "Round 1" to "125 bpm" in rows && "Round 2" to "155 bpm" in rows && rows.none { it.first == "Round 3" })
        val utc = ZoneId.of("UTC")
        val sUtc = s.copy(date = localDate(s.startedAt, utc))
        assertEquals("02:00 to 02:31", hrFactRows(sUtc, hrFacts(sUtc, r.copy(toMs = at(7, 31)), utc), emptyList(), utc) { it }.first().second)
        assertEquals("45 s", gapText(45)); assertEquals("2 min", gapText(120)); assertEquals("16 min 5 s", gapText(965))
    }

    private fun sheet(s: Session, r: SessionHr?) = hrSheet(s, r, emptyList(), zone, null, null) { it }

    @Test fun `the sheet's model holds the facts and the action flags, Remove for every row that is not off`() {
        val s = gym()
        val stale = row(s).copy(fromMs = gymRange.fromMs + 60_000)
        val withRow = sheet(s, stale)
        val withoutRow = sheet(s, null)
        assertEquals(HrFacts.NotRead, facts(s, stale)); assertEquals(HrFacts.NotRead, facts(s, null))
        assertEquals(withoutRow.facts, withRow.facts)
        assertTrue(withRow.remove); assertFalse(withoutRow.remove)
        assertTrue(withRow.reread && withoutRow.reread)
        val noRange = gym(r = null)
        val noRangeRow = SessionHr("g", gymRange.fromMs, gymRange.toMs, HrState.FILLED, fetchedAt = at(20, 0))
        assertEquals(HrFacts.NoRange, facts(noRange, noRangeRow))
        assertTrue(sheet(noRange, noRangeRow).remove); assertFalse(sheet(noRange, null).remove)
        assertFalse(sheet(noRange, null).reread)
        assertFalse(sheet(s, row(s).copy(state = HrState.OFF)).remove); assertTrue(sheet(s, row(s).copy(state = HrState.OFF)).reread)
        assertTrue(sheet(s, row(s, HrState.NONE)).remove)
        val forty = gym(r = null).copy(finishedAt = at(18, 42))
        assertEquals(HrRange(at(18, 2), at(18, 42)), sheet(forty, null).useRange)
        assertEquals("Workout" to "18:02 to 18:42", sheet(forty, null).facts.last())
        assertNull(sheet(s, null).useRange)
    }

    @Test fun `the sheet shows measured values, also where a typed one exists`() {
        val s = cardio(avg = 141)
        val r = row(s, avg = 128, samples = "0:140,5:148,300:150,305:160")
        val typedRound = IntervalLog("i1", "c", 0, avgHr = 150, startedAt = at(7, 0), endedAt = at(7, 1))
        val rows = hrFactRows(s, facts(s, r), listOf(typedRound), zone) { it }
        assertTrue(rows.toString(), "Round 1" to "144 bpm" in rows && "Average" to "128 bpm" in rows)
        assertEquals(150, shownRoundHr(typedRound, facts(s, r)))
        assertEquals("avg 141 · max 164 bpm", hrLine(s, facts(s, r)))
        val typedOnly = IntervalLog("i2", "c", 1, avgHr = 150, startedAt = at(7, 20), endedAt = at(7, 21))
        assertFalse(hrFactRows(s, facts(s, r), listOf(typedRound, typedOnly), zone) { it }.any { it.first == "Round 2" })
    }

    @Test fun `typed and measured values reach both charts of a cardio type`() {
        val typed = cardio(avg = 150)
        val measured = cardio().copy(id = "m")
        val neither = cardio().copy(id = "n")
        fun rowFor(s: Session) = row(s, avg = 144, samples = "0:140,5:148,300:150,305:160")
        val f = factsById(listOf(typed, measured, neither), listOf(rowFor(typed), rowFor(measured)), zone)
        assertEquals(listOf(ChartPoint(day, 150.0), ChartPoint(day, 144.0)), avgHrPoints(listOf(typed, measured, neither), f))
        val rounds = listOf(
            IntervalLog("t1", "c", 0, avgHr = 160, startedAt = at(7, 0), endedAt = at(7, 1)), IntervalLog("t2", "c", 1, startedAt = at(7, 0), endedAt = at(7, 1)),
            IntervalLog("m1", "m", 0, startedAt = at(7, 5), endedAt = at(7, 6)), IntervalLog("n1", "n", 0, startedAt = at(7, 0), endedAt = at(7, 1)),
        )
        assertEquals(listOf(ChartPoint(day, 152.0), ChartPoint(day, 155.0)), workHrPoints(listOf(typed, measured, neither), rounds, f))
    }

    @Test fun `a result belongs to its sheet, range and row, and a late one writes nothing`() {
        val s = gym()
        val r = row(s)
        val asked = hrReadKey(1, s, r, zone)
        assertEquals(HrReadResult(asked, HrReread.FAILED, seen = true), readResult(asked, HrReread.FAILED, asked, r))
        val off = r.copy(state = HrState.OFF, fetchedAt = at(21, 0))
        val newer = r.copy(avgHr = 130, fetchedAt = at(21, 0))
        val moved = s.copy(timedTo = gymRange.toMs + 60_000)
        for (now in listOf(hrReadKey(1, s, off, zone), hrReadKey(1, s, newer, zone), hrReadKey(1, moved, r, zone), hrReadKey(2, s, r, zone), null)) {
            assertNull(now.toString(), readResult(asked, HrReread.FAILED, now, r))
            assertNull(now.toString(), readResult(asked, HrReread.DROPPED, now, r))
        }
        assertNull(readResult(asked, HrReread.FILLED, hrReadKey(2, s, r, zone), newer))
        assertNull(readResult(asked, HrReread.FILLED, hrReadKey(1, moved, r, zone), newer))

        val pending = readResult(asked, HrReread.FILLED, asked, newer)!!
        assertEquals(hrReadKey(1, s, newer, zone), pending.key); assertFalse(pending.seen)
        assertEquals(pending, pending.after(asked))
        val shown = pending.after(hrReadKey(1, s, newer, zone))!!
        assertTrue(shown.seen)
        assertEquals("avg 130 · max 164 bpm", hrSheet(s, newer, emptyList(), zone, shown, hrReadKey(1, s, newer, zone)) { it }.line)
        assertNull(shown.after(hrReadKey(1, s, off, zone)))
        assertNull(shown.after(hrReadKey(1, moved, newer, zone)))
        assertNull(shown.after(null))

        val failed = readResult(asked, HrReread.FAILED, asked, r)!!
        assertEquals("Health Connect did not answer", hrSheet(s, r, emptyList(), zone, failed, asked) { it }.line)
        assertNull(hrSheet(s, off, emptyList(), zone, failed, hrReadKey(1, s, off, zone)) { it }.line)
        assertNull(hrSheet(s, r, emptyList(), zone, failed, hrReadKey(2, s, r, zone)) { it }.line)
    }

    @Test fun `the result line of Read again follows the facts of now`() {
        val s = gym()
        val filled = facts(s, row(s))
        assertEquals("avg 128 · max 164 bpm", rereadLine(HrReread.FILLED, s, filled))
        assertEquals("No data yet", rereadLine(HrReread.WAITING, s, facts(s, row(s, HrState.WAITING))))
        assertEquals("No data yet", rereadLine(HrReread.NONE, s, facts(s, row(s, HrState.NONE))))
        assertEquals("Health Connect did not answer", rereadLine(HrReread.FAILED, s, filled))
        assertNull(rereadLine(HrReread.DROPPED, s, filled)); assertNull(rereadLine(HrReread.NO_RANGE, s, HrFacts.NoRange)); assertNull(rereadLine(null, s, filled))
        assertNull(rereadLine(HrReread.FILLED, s, HrFacts.Removed))
        assertNull(rereadLine(HrReread.WAITING, s, HrFacts.Removed))
        assertNull(rereadLine(HrReread.FILLED, s, HrFacts.NotRead))
    }
}
