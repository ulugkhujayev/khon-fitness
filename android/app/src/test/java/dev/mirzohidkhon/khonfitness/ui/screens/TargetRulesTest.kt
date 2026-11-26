package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.BodyTarget
import dev.mirzohidkhon.khonfitness.data.Bodyweight
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
        assertEquals(Forecast.NoLine, liftForecast(target, old, true, day(84)))
        val rows = liftRows(target, old, day(84))
        assertNull(rows.result)
        assertEquals("100 kg", rows.target.value)
    }

    @Test fun `G4 a flat or falling line is not at this rate`() {
        assertEquals(Forecast.NotAtThisRate("−1.0"), liftForecast(kg("100"), lifts(rising, listOf(96.0, 94.0, 92.0, 90.0)), true, day(42)))
        assertEquals(Forecast.NotAtThisRate("±0.0"), liftForecast(kg("100"), lifts(rising, listOf(90.0, 90.0, 90.0, 90.1)), true, day(42)))
        assertEquals(Forecast.NotAtThisRate("±0.0"), liftForecast(kg("100"), lifts(rising, listOf(90.0, 90.0, 90.0, 90.0)), true, day(42)))
        val rows = liftRows(kg("100"), lifts(rising, listOf(96.0, 94.0, 92.0, 90.0)), day(42))
        assertEquals(TargetLine("At −1.0 kg/week", "Not at this rate", "At minus 1.0 kilograms per week, not at this rate"), rows.result)
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
    private fun body(kg: String, start: String?, entry: LocalDate = today.minusDays(20)) = BodyTarget(BigDecimal(kg), start?.let(::BigDecimal), entry)

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

    @Test fun `the stored average is the printed 7-day average, else the latest entry`() {
        assertEquals(BigDecimal("82.0"), bodyStart(falling, today))
        assertEquals(BigDecimal("82.3"), bodyStart(bw(7..7, 82.3), today))
        assertNull(bodyStart(emptyList(), today))
    }

    @Test fun `G11 the sheet refuses values outside the limits`() {
        for (v in listOf(0.5, 500.5, 100.3, 0.0)) assertEquals("$v", TargetEntry.Refused, targetEntry(v, TargetKind.LIFT_KG))
        for (v in listOf(0.0, 501.0, 7.5)) assertEquals("$v", TargetEntry.Refused, targetEntry(v, TargetKind.LIFT_REPS))
        for (v in listOf(19.9, 300.1, 80.25)) assertEquals("$v", TargetEntry.Refused, targetEntry(v, TargetKind.BODY))
        assertEquals(TargetEntry.Refused, targetEntry(Double.NaN, TargetKind.BODY))
        assertEquals(TargetEntry.Clear, targetEntry(null, TargetKind.BODY))
        assertEquals(TargetEntry.Store(BigDecimal("100.5")), targetEntry(100.5, TargetKind.LIFT_KG))
        assertEquals(TargetEntry.Store(BigDecimal("500.0")), targetEntry(500.0, TargetKind.LIFT_KG))
        assertEquals(TargetEntry.Store(BigDecimal("1.0")), targetEntry(1.0, TargetKind.LIFT_REPS))
        assertEquals(TargetEntry.Store(BigDecimal("80.2")), targetEntry(80.2, TargetKind.BODY))
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
}
