package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.BodyTarget
import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.data.EntryAverage
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.LiftTarget
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.iso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class TargetRulesTest {
    private val day0 = LocalDate.of(2026, 10, 1)
    private fun day(n: Long) = day0.plusDays(n)
    private var k = 0

    /** Sessions on [days] whose est. 1RM is [values]: one set of 30 reps at half the value, so value = kg × 60 / 30. */
    private fun lifts(days: List<Long>, values: List<Double>): List<LiftSession> = history(days.zip(values) { d, v -> Triple(d, v / 2, 30) })

    private fun reps(days: List<Long>, reps: List<Int>): List<LiftSession> = history(days.zip(reps) { d, r -> Triple(d, 0.0, r) })

    private fun history(rows: List<Triple<Long, Double, Int>>): List<LiftSession> {
        val sessions = rows.map { (d, _, _) -> Session("s$d", day(d).iso(), startedAt = d, finishedAt = d + 1, itemType = ItemType.PROGRAM, programId = "p", programName = "P") }
        val logs = rows.map { (d, kg, r) -> SetLog("l${k++}", "s$d", 0, false, 0, 0, 0, "bench", "Bench", 5, 8, 2.5, weightKg = kg, reps = r, status = SetStatus.DONE) }
        return liftHistory("bench", logs, sessions)
    }

    private fun kg(v: String, entry: LocalDate = day0) = LiftTarget(BigDecimal(v), false, entry)
    private val rising = listOf(0L, 14, 28, 42)

    @Test fun `G2 four sessions over 42 days at 1 kg per week reach 100 kg on day 70`() {
        val h = lifts(rising, listOf(90.0, 92.0, 94.0, 96.0))
        val f = liftForecast(kg("100"), h, withWeight = true, today = day(42))
        assertEquals(Forecast.OnDate("+1.0", day(70)), f)
        val rows = liftRows(kg("100"), h, day(42))
        assertEquals("At +1.0 kg/week", rows.result!!.label)
        assertEquals("Dec 10", rows.result!!.value)
        assertEquals("100 kg", rows.target.value)
    }

    @Test fun `G3 no line without 3 sessions over 14 days inside the last 56 days`() {
        val target = kg("100")
        assertEquals(Forecast.NoLine, liftForecast(target, lifts(listOf(0, 20), listOf(90.0, 92.0)), true, day(20)))
        assertEquals(Forecast.NoLine, liftForecast(target, lifts(listOf(0, 6, 13), listOf(90.0, 92.0, 94.0)), true, day(13)))
        val old = lifts(listOf(0, 14, 28), listOf(90.0, 92.0, 94.0))
        assertTrue(liftForecast(target, old, true, day(55)) is Forecast.OnDate)
        assertEquals(Forecast.Stale(day(28)), liftForecast(target, old, true, day(84)))
        val rows = liftRows(target, old, day(84))
        assertEquals("No session since Oct 29", rows.result!!.label)
        assertNull(rows.result!!.value)
        assertEquals("100 kg", rows.target.value)
    }

    @Test fun `G4 a flat or falling line is not at this rate`() {
        assertEquals(Forecast.NotAtThisRate("−1.0"), liftForecast(kg("100"), lifts(rising, listOf(96.0, 94.0, 92.0, 90.0)), true, day(42)))
        assertEquals(Forecast.NotAtThisRate("±0.0"), liftForecast(kg("100"), lifts(rising, listOf(90.0, 90.0, 90.0, 90.1)), true, day(42)))
        assertEquals(Forecast.NotAtThisRate("±0.0"), liftForecast(kg("100"), lifts(rising, listOf(90.0, 90.0, 90.0, 90.0)), true, day(42)))
        val rows = liftRows(kg("100"), lifts(rising, listOf(96.0, 94.0, 92.0, 90.0)), day(42))
        assertEquals(TargetLine("At −1.0 kg/week", "Not at this rate", "Not at this rate"), rows.result)
    }

    @Test fun `G5 the first session on or after the entry date at the target reaches it`() {
        val onEntry = lifts(rising, listOf(90.0, 92.0, 94.0, 100.0))
        assertEquals(Forecast.Reached(day(42)), liftForecast(kg("100", day(42)), onEntry, true, day(42)))

        val beforeOnly = lifts(rising, listOf(101.0, 92.0, 94.0, 96.0))
        val f = liftForecast(kg("100", day(30)), beforeOnly, true, day(42))
        assertFalse(f is Forecast.Reached)
        assertEquals(Forecast.NotAtThisRate("−0.7"), f)

        val later = lifts(rising, listOf(90.0, 101.0, 102.0, 96.0))
        assertEquals(Forecast.Reached(day(14)), liftForecast(kg("100", day(1)), later, true, day(42)))
        val rows = liftRows(kg("100", day(1)), later, day(42))
        assertEquals(TargetLine("Reached", "Oct 15", "Reached, October 15"), rows.result)
    }

    @Test fun `a reached target stays reached when the newest session is older than 28 days`() {
        val history = lifts(rising, listOf(90.0, 101.0, 102.0, 96.0))
        assertEquals(Forecast.Reached(day(14)), liftForecast(kg("100", day(1)), history, true, day(71)))
        assertEquals(TargetLine("Reached", "Oct 15", "Reached, October 15"), liftRows(kg("100", day(1)), history, day(71)).result)
    }

    @Test fun `G6 731 days ahead is more than 2 years, 730 is a date`() {
        val h = lifts(rising, listOf(90.0, 92.0, 94.0, 96.0))
        assertEquals(Forecast.Beyond("+1.0"), liftForecast(kg("200.5"), h, true, day(43)))
        assertEquals(Forecast.OnDate("+1.0", day(44).plusDays(730)), liftForecast(kg("200.5"), h, true, day(44)))
        assertEquals(TargetLine("At +1.0 kg/week", "More than 2 years", "At plus 1.0 kilograms per week, more than 2 years"), liftRows(kg("200.5"), h, day(43)).result)
        assertEquals("Nov 13, 2028", liftRows(kg("200.5"), h, day(44)).result!!.value)
    }

    @Test fun `G7 an exercise without weight uses the reps line`() {
        val h = reps(rising, listOf(8, 9, 10, 11))
        val target = LiftTarget(BigDecimal(15), true, day0)
        assertEquals(Forecast.OnDate("+0.5", day(98)), liftForecast(target, h, withWeight = false, today = day(42)))
        val rows = liftRows(target, h, day(42))
        assertEquals("15 reps", rows.target.value)
        assertEquals("Target, 15 repetitions", rows.target.spoken)
        assertEquals(TargetLine("At +0.5 reps/week", "Jan 7, 2027", "At plus 0.5 repetitions per week, January 7, 2027"), rows.result)
    }

    @Test fun `a stored target of the other unit reads as not set`() {
        val rows = liftRows(LiftTarget(BigDecimal(15), true, day0), lifts(rising, listOf(90.0, 92.0, 94.0, 96.0)), day(42))
        assertEquals(TargetLine("Target", "Not set", "Target, not set"), rows.target)
        assertFalse(rows.set)
        assertNull(rows.result)
    }

    @Test fun `G13 an exercise with no session shows the target row only`() {
        val rows = liftRows(LiftTarget(BigDecimal(12), true, day0), emptyList(), day(42))
        assertEquals("12 reps", rows.target.value)
        assertNull(rows.result)
        assertEquals(TargetKind.LIFT_REPS, if (withWeight(emptyList())) TargetKind.LIFT_KG else TargetKind.LIFT_REPS)
    }

    private val today = LocalDate.of(2026, 11, 27)
    private fun bw(daysAgo: IntRange, kg: Double) = daysAgo.map { Bodyweight(today.minusDays(it.toLong()).iso(), kg) }
    private val falling = bw(0..2, 82.0) + bw(7..9, 82.3)
    private fun body(kg: String, start: String?, entry: LocalDate = today.minusDays(20)) = BodyTarget(BigDecimal(kg), start?.let { EntryAverage(BigDecimal(it), 1) }, entry)

    @Test fun `G8 2 kg at 0_3 kg per week is 47 days`() {
        assertEquals("7-day avg 82.0 kg · −0.3 kg/week", trendText(falling, today))
        assertEquals(Forecast.OnDate("−0.3", today.plusDays(47)), bodyForecast(body("80.0", "83.2"), falling, today))
        val rows = bodyRows(body("80.0", "83.2"), falling, today)
        assertEquals(TargetLine("Target", "80.0 kg", "Target, 80.0 kilograms"), rows.target)
        assertEquals(TargetLine("At −0.3 kg/week", "Jan 13, 2027", "At minus 0.3 kilograms per week, January 13, 2027"), rows.result)
    }

    @Test fun `G9 the direction comes from the stored average, else the present one`() {
        assertEquals(Forecast.NotAtThisRate("−0.3"), bodyForecast(body("85.0", "83.2"), falling, today))
        assertEquals(Forecast.Reached(null), bodyForecast(body("82.5", "83.2"), falling, today))
        assertEquals(TargetLine("Reached", null, "Reached"), bodyRows(body("82.5", "83.2"), falling, today).result)
        assertEquals(Forecast.OnDate("−0.3", today.plusDays(47)), bodyForecast(body("80.0", null), falling, today))
        assertEquals(Forecast.NotAtThisRate("−0.3"), bodyForecast(body("85.0", null), falling, today))
        assertEquals(Forecast.Reached(today.minusDays(20)), bodyForecast(body("82.0", "82.0"), falling, today))
        assertEquals(Forecast.Reached(today.minusDays(20)), bodyForecast(body("82.0", null), falling, today))
        assertEquals(TargetLine("Reached", null, "Reached"), bodyRows(body("82.0", null), falling, today).result)
    }

    @Test fun `G10 without both weeks there is no date`() {
        val thisWeek = bw(0..2, 82.0)
        assertEquals(Forecast.NoLine, bodyForecast(body("80.0", "83.2"), thisWeek, today))
        assertNull(bodyRows(body("80.0", "83.2"), thisWeek, today).result)
        val older = bw(10..13, 82.0)
        assertEquals(Forecast.NoLine, bodyForecast(body("80.0", "83.2"), older, today))
        assertEquals(Forecast.NoLine, bodyForecast(body("80.0", null), older, today))
        assertEquals("80.0 kg", bodyRows(body("80.0", null), older, today).target.value)
    }

    @Test fun `a slow bodyweight rate past 730 days is more than 2 years`() {
        val slow = bw(0..2, 82.0) + bw(7..9, 82.1)
        assertEquals(Forecast.Beyond("−0.1"), bodyForecast(body("60.0", "83.2"), slow, today))
    }

    @Test fun `the stored average is the exact sum and count of the 7-day entries, else the latest entry`() {
        assertEquals(EntryAverage(BigDecimal("246.0"), 3), bodyStart(falling, today))
        assertEquals(EntryAverage(BigDecimal("82.3"), 1), bodyStart(bw(7..7, 82.3), today))
        assertNull(bodyStart(emptyList(), today))
    }

    @Test fun `G11 the sheet refuses values outside the limits`() {
        for (v in listOf("0.5", "500.5", "100.3", "0")) assertEquals(v, TargetEntry.Refused, targetEntry(v, TargetKind.LIFT_KG))
        for (v in listOf("0", "501", "7.5")) assertEquals(v, TargetEntry.Refused, targetEntry(v, TargetKind.LIFT_REPS))
        for (v in listOf("19.9", "300.1", "80.25")) assertEquals(v, TargetEntry.Refused, targetEntry(v, TargetKind.BODY))
        assertEquals(TargetEntry.Clear, targetEntry("", TargetKind.BODY))
        assertEquals(TargetEntry.Store(BigDecimal("100.5")), targetEntry("100.5", TargetKind.LIFT_KG))
        assertEquals(TargetEntry.Store(BigDecimal("500")), targetEntry("500.0", TargetKind.LIFT_KG))
        assertEquals(TargetEntry.Store(BigDecimal("1")), targetEntry("1", TargetKind.LIFT_REPS))
        assertEquals(TargetEntry.Store(BigDecimal("80.2")), targetEntry("80.2", TargetKind.BODY))
    }

    @Test fun `X1 the sheet refuses text that is not a plain decimal on the kind's step`() {
        val bad = listOf("abc", "8x0", "-80", "+80", " 80", "80 ", "80.001", "80.25", "100.501", "7.05", "1,000.5", "1.000,5", "80..5", ".", ",", "\u0668\u0660", "\uFF18\uFF10", "1e2", "NaN", "Infinity", "0x50")
        for (text in bad) for (kind in TargetKind.entries) {
            val e = targetEntry(text, kind)
            assertEquals("$text $kind", TargetEntry.Refused, e)
        }
        for (text in listOf("12.0", "12,0", "12.", "1.5", ",5")) assertEquals(text, TargetEntry.Refused, targetEntry(text, TargetKind.LIFT_REPS))
        assertEquals(TargetEntry.Refused, targetEntry("100.25", TargetKind.LIFT_KG))
        assertEquals(TargetEntry.Refused, targetEntry("100.3", TargetKind.LIFT_KG))
        assertEquals(TargetEntry.Refused, targetEntry("80.05", TargetKind.BODY))
        assertEquals(TargetEntry.Refused, targetEntry("80.10", TargetKind.BODY))
        assertEquals(TargetEntry.Refused, targetEntry("80,00", TargetKind.BODY))
        for (kind in TargetKind.entries) {
            assertEquals(TargetEntry.Refused, targetEntry("00000000000000080", kind))
            assertEquals(TargetEntry.Refused, targetEntry("9".repeat(30_000) + "x", kind))
        }
    }

    @Test fun `X2 the sheet keeps leading zeros and reads a decimal comma`() {
        assertEquals(TargetEntry.Store(BigDecimal("80.0")), targetEntry("0080", TargetKind.BODY))
        assertEquals(TargetEntry.Store(BigDecimal("80.0")), targetEntry("80.0", TargetKind.BODY))
        assertEquals(TargetEntry.Store(BigDecimal("80.5")), targetEntry("80,5", TargetKind.BODY))
        assertEquals(TargetEntry.Store(BigDecimal("80.0")), targetEntry("0000000000000080", TargetKind.BODY))
        assertEquals(TargetEntry.Store(BigDecimal("100")), targetEntry("100,0", TargetKind.LIFT_KG))
        assertEquals(TargetEntry.Store(BigDecimal("100.5")), targetEntry("100.50", TargetKind.LIFT_KG))
        assertEquals(TargetEntry.Store(BigDecimal("15")), targetEntry("0000015", TargetKind.LIFT_KG))
        assertEquals(TargetEntry.Store(BigDecimal("15")), targetEntry("0000015", TargetKind.LIFT_REPS))
        assertEquals(TargetEntry.Store(BigDecimal("12")), targetEntry("12", TargetKind.LIFT_REPS))
        assertEquals(TargetEntry.Store(BigDecimal("80.0")), targetEntry("80.", TargetKind.BODY))
        assertEquals(TargetEntry.Store(BigDecimal("1")), targetEntry("1", TargetKind.LIFT_KG))
        assertEquals(TargetEntry.Store(BigDecimal("20.0")), targetEntry("20", TargetKind.BODY))
        assertEquals(TargetEntry.Store(BigDecimal("300.0")), targetEntry("300.0", TargetKind.BODY))
        for (kind in TargetKind.entries) assertEquals(TargetEntry.Clear, targetEntry("", kind))
    }

    @Test fun `X3 an exact entry average above the target keeps the falling direction`() {
        val entry = LocalDate.of(2026, 11, 20)
        val earlier = listOf(80.0, 80.0, 80.1).mapIndexed { i, kg -> Bodyweight(entry.minusDays(i.toLong()).iso(), kg) }
        val start = bodyStart(earlier, entry)
        assertEquals(EntryAverage(BigDecimal("240.1"), 3), start)
        val current = (0L..2L).map { Bodyweight(entry.plusDays(7).minusDays(it).iso(), 79.9) }
        val target = BodyTarget(BigDecimal("80.0"), start, entry)
        assertEquals(Forecast.Reached(null), bodyForecast(target, earlier + current, entry.plusDays(7)))
        assertEquals("Reached", bodyRows(target, earlier + current, entry.plusDays(7)).result?.label)
    }

    @Test fun `X4 an exact present average of 80_04 has not reached 80_0 going down, and an old record reads`() {
        val now = listOf(80.0, 80.1, 80.0, 80.1, 80.0).mapIndexed { i, kg -> Bodyweight(today.minusDays(i.toLong()).iso(), kg) }
        val before = bw(7..9, 80.5)
        val target = BodyTarget(BigDecimal("80.0"), EntryAverage(BigDecimal("249.0"), 3), today.minusDays(20))
        assertEquals("7-day avg 80.0 kg · −0.5 kg/week", trendText(now + before, today))
        val f = bodyForecast(target, now + before, today)
        assertFalse(f.toString(), f is Forecast.Reached)
        assertEquals(Forecast.OnDate("−0.5", today.plusDays(1)), f)
        val old = dev.mirzohidkhon.khonfitness.data.Targets.readBody("80.0|83.2|2026-11-07")
        assertEquals(BodyTarget(BigDecimal("80.0"), EntryAverage(BigDecimal("83.2"), 1), LocalDate.of(2026, 11, 7)), old)
        assertEquals(Forecast.OnDate("−0.3", today.plusDays(47)), bodyForecast(old!!, falling, today))
    }

    @Test fun `X5 a stored entry date after today reads as today's entry`() {
        val late = today.plusDays(30)
        assertEquals(Forecast.Reached(today), bodyForecast(body("82.0", "82.0", late), falling, today))
        assertEquals(Forecast.OnDate("−0.3", today.plusDays(47)), bodyForecast(body("80.0", "83.2", late), falling, today))
        val onToday = lifts(listOf(0, 14, 28, 42), listOf(90.0, 92.0, 94.0, 100.0))
        assertEquals(Forecast.Reached(day(42)), liftForecast(kg("100", day(42).plusDays(9)), onToday, true, day(42)))
    }

    @Test fun `F6 a session 55 days old is in the window and one 56 days old is not`() {
        val target = kg("100", day(56))
        assertTrue(liftForecast(target, lifts(listOf(1, 15, 29), listOf(90.0, 92.0, 94.0)), true, day(56)) is Forecast.OnDate)
        assertEquals(Forecast.NoLine, liftForecast(target, lifts(listOf(0, 15, 29), listOf(90.0, 92.0, 94.0)), true, day(56)))
    }

    @Test fun `F6 a span of exactly 14 days gives a date and 13 days does not`() {
        val target = kg("100", day(14))
        assertTrue(liftForecast(target, lifts(listOf(0, 7, 14), listOf(90.0, 92.0, 94.0)), true, day(14)) is Forecast.OnDate)
        assertEquals(Forecast.NoLine, liftForecast(target, lifts(listOf(1, 7, 14), listOf(90.0, 92.0, 94.0)), true, day(14)))
    }

    @Test fun `F6 a bodyweight rate of minus 0_26 kg per week needs 54 days for 2 kg, not the 47 of the printed rate`() {
        val now = bw(0..2, 82.0)
        val before = listOf(82.3, 82.3, 82.3, 82.2, 82.2).mapIndexed { i, kg -> Bodyweight(today.minusDays(7L + i).iso(), kg) }
        assertEquals("7-day avg 82.0 kg · −0.3 kg/week", trendText(now + before, today))
        assertEquals(Forecast.OnDate("−0.3", today.plusDays(54)), bodyForecast(body("80.0", "83.2"), now + before, today))
    }

    @Test fun `F6 a lift date uses the exact slope, not the printed rate`() {
        val h = lifts(rising, listOf(90.0, 91.5, 93.0, 94.0))
        assertEquals(Forecast.OnDate("+0.7", day(42).plusDays(61)), liftForecast(kg("100"), h, true, day(42)))
    }

    @Test fun `a line at or above the target today with no session at it gives no date`() {
        val h = lifts(rising, listOf(90.0, 96.0, 96.0, 95.0))
        assertEquals(Forecast.Passed("+0.8"), liftForecast(kg("96.5", day(0)), h, true, day(42)))
        assertEquals(Forecast.Passed("+0.8"), liftForecast(kg("96", day(30)), h, true, day(42)))
    }

    @Test fun `X7 each result state speaks its text`() {
        val far = LocalDate.of(2027, 3, 14)
        for (kind in TargetKind.entries) {
            val unit = if (kind == TargetKind.LIFT_REPS) "repetitions per week" else "kilograms per week"
            fun spoken(f: Forecast) = targetRows(BigDecimal(100), kind, f, today).result?.spoken
            assertEquals(kind.name, "Not at this rate", spoken(Forecast.NotAtThisRate("−1.0")))
            assertEquals(kind.name, "Not at this rate", spoken(Forecast.NotAtThisRate("±0.0")))
            assertEquals(kind.name, "At plus 12.5 $unit, more than 2 years", spoken(Forecast.Beyond("+12.5")))
            assertEquals(kind.name, "At minus 0.3 $unit, March 14, 2027", spoken(Forecast.OnDate("−0.3", far)))
            assertNull(kind.name, spoken(Forecast.NoLine))
            assertEquals(kind.name, "Not at this rate", targetRows(BigDecimal(100), kind, Forecast.NotAtThisRate("−1.0"), today).result?.value)
            assertEquals(kind.name, "More than 2 years", targetRows(BigDecimal(100), kind, Forecast.Beyond("+12.5"), today).result?.value)
        }
        assertEquals("Reached", targetRows(BigDecimal(80), TargetKind.BODY, Forecast.Reached(null), today).result?.spoken)
        assertEquals("Reached", targetRows(BigDecimal(80), TargetKind.BODY, Forecast.Reached(today), today).result?.spoken)
        assertEquals("Reached, November 20", targetRows(BigDecimal(12), TargetKind.LIFT_REPS, Forecast.Reached(LocalDate.of(2026, 11, 20)), today).result?.spoken)
    }

    @Test fun `G15 every row speaks in full words`() {
        assertEquals("Target, 100 kilograms", targetRows(BigDecimal(100), TargetKind.LIFT_KG, null, today).target.spoken)
        assertEquals("Target, 1 repetition", targetRows(BigDecimal.ONE, TargetKind.LIFT_REPS, null, today).target.spoken)
        assertEquals("Target, not set", targetRows(null, TargetKind.BODY, null, today).target.spoken)
        assertEquals("At plus 0.4 kilograms per week, March 14, 2027", targetRows(BigDecimal(100), TargetKind.LIFT_KG, Forecast.OnDate("+0.4", LocalDate.of(2027, 3, 14)), today).result!!.spoken)
        assertEquals("Reached, November 20", targetRows(BigDecimal(100), TargetKind.LIFT_KG, Forecast.Reached(LocalDate.of(2026, 11, 20)), today).result!!.spoken)
        assertEquals("Mar 14, 2027", targetRows(BigDecimal(100), TargetKind.LIFT_KG, Forecast.OnDate("+0.4", LocalDate.of(2027, 3, 14)), today).result!!.value)
        assertEquals("Nov 20", targetRows(BigDecimal(100), TargetKind.LIFT_KG, Forecast.Reached(LocalDate.of(2026, 11, 20)), today).result!!.value)
    }

    @Test fun `no stored value can overflow or divide by zero`() {
        val h = lifts(rising, listOf(2.0, 2.5, 3.0, 3.5))
        assertTrue(liftForecast(kg("500.0"), h, true, day(42)) is Forecast.Beyond)
        val tiny = bw(0..2, 20.0) + bw(7..9, 300.0)
        assertTrue(bodyForecast(body("300.0", "20.0"), tiny, today) is Forecast.NotAtThisRate)
        assertTrue(bodyForecast(body("20.0", "300.0"), tiny, today) is Forecast.Reached)
    }

    @Test fun `a newest session more than 28 days old gives no rate and no date`() {
        val h = lifts(listOf(20, 27, 34, 41), listOf(90.0, 91.0, 92.0, 93.0))
        assertEquals(Forecast.OnDate("+1.0", day(160)), liftForecast(kg("110"), h, true, day(69)))
        assertEquals(Forecast.Stale(day(41)), liftForecast(kg("110"), h, true, day(70)))
        val rows = liftRows(kg("110"), h, day(70))
        assertEquals("No session since Nov 11", rows.result!!.label)
        assertNull(rows.result!!.value)
        assertEquals("No session since November 11", rows.result!!.spoken)
        assertEquals(Forecast.Stale(day(41)), liftForecast(kg("110"), h + lifts(listOf(75), listOf(95.0)), true, day(70)))
    }

    @Test fun `a line that is at the target today or was there before gives no date`() {
        val h = lifts(rising, listOf(90.0, 92.0, 94.0, 96.0))
        assertEquals(Forecast.OnDate("+1.0", day(57)), liftForecast(kg("98.1"), h, true, day(56)))
        assertEquals(Forecast.Passed("+1.0"), liftForecast(kg("98"), h, true, day(56)))
        assertEquals(Forecast.Passed("+1.0"), liftForecast(kg("97"), h, true, day(56)))
        val rows = liftRows(kg("97"), h, day(56))
        assertEquals("At +1.0 kg/week", rows.result!!.label)
        assertEquals("No date", rows.result!!.value)
        assertEquals("At plus 1.0 kilograms per week, no date", rows.result!!.spoken)
    }
}
