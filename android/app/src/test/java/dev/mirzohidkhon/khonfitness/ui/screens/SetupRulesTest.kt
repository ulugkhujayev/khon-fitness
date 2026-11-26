package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.iso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** Setup periods of an exercise, the change between two, the value of a session and the rate of a period. */
class SetupRulesTest {
    private val start = LocalDate.of(2026, 10, 1)
    private var n = 0

    /** One bench session: [sets] rows of [kg] × [reps] in a [min]–[max] range. */
    private data class Day(val id: String, val day: Long, val program: String? = "p3", val name: String? = "TP v3", val sets: Int = 3, val min: Int = 8, val max: Int = 12, val kg: Double = 60.0, val reps: Int = 10, val status: String = SetStatus.DONE, val finished: Boolean = true, val at: Long = 0)

    private fun build(days: List<Day>): Pair<List<Session>, List<SetLog>> {
        val sessions = days.map { d ->
            Session(d.id, start.plusDays(d.day).iso(), startedAt = d.at, finishedAt = if (d.finished) d.at + 1 else null, itemType = ItemType.PROGRAM, programId = d.program, programName = d.name)
        }
        val logs = days.flatMap { d ->
            (0 until d.sets).map { k -> SetLog("l${n++}", d.id, 0, false, 0, k, k, "bench", "Bench", d.min, d.max, 2.5, weightKg = d.kg, reps = d.reps, status = d.status) }
        }
        return sessions to logs
    }

    private fun history(days: List<Day>) = build(days).let { (s, l) -> liftHistory("bench", l, s) }
    private fun periods(days: List<Day>) = setupPeriods(history(days))
    private fun ids(p: SetupPeriod) = p.sessions.map { it.session.id }

    // ---- G1: periods

    @Test fun `three sessions of 3 sets then three of 4 sets are two periods, the second from the fourth session`() {
        val p = periods((0 until 6).map { Day("s$it", it * 2L, sets = if (it < 3) 3 else 4) })
        assertEquals(2, p.size)
        assertEquals(listOf("s3", "s4", "s5"), ids(p[1]))
        assertEquals(listOf("3 → 4 sets"), changeFacts(p[0], p[1]))
    }

    // ---- G2: the program

    @Test fun `a new program id is a change with both names, a renamed program is none, no program is its own setup`() {
        val changed = periods(listOf(Day("a", 0), Day("b", 2), Day("c", 4, program = "p4", name = "TP v4"), Day("d", 6, program = "p4", name = "TP v4")))
        assertEquals(2, changed.size)
        assertEquals(listOf("TP v3 → TP v4"), changeFacts(changed[0], changed[1]))

        val renamed = periods(listOf(Day("a", 0), Day("b", 2), Day("c", 4, name = "Push day"), Day("d", 6, name = "Push day")))
        assertEquals(1, renamed.size)
        assertEquals("Push day", renamed.single().programName)

        val none = periods(listOf(Day("a", 0), Day("b", 2), Day("c", 4, program = null, name = null), Day("d", 6, program = null, name = null)))
        assertEquals(2, none.size)
        assertEquals(listOf("TP v3 → No program"), changeFacts(none[0], none[1]))
        assertEquals("No program · 3 sets · 8–12 reps", setupText(none[1]))
    }

    // ---- G3: reps, and three facts at once

    @Test fun `a new rep range is a change, and three changes in one session give three facts in order`() {
        val reps = periods(listOf(Day("a", 0), Day("b", 2), Day("c", 4, min = 5, max = 8), Day("d", 6, min = 5, max = 8)))
        assertEquals(listOf("8–12 → 5–8 reps"), changeFacts(reps[0], reps[1]))
        val all = periods(listOf(Day("a", 0), Day("b", 2), Day("c", 4, program = "p4", name = "TP v4", sets = 4, min = 5, max = 8), Day("d", 6, program = "p4", name = "TP v4", sets = 4, min = 5, max = 8)))
        assertEquals(2, all.size)
        assertEquals(listOf("TP v3 → TP v4", "3 → 4 sets", "8–12 → 5–8 reps"), changeFacts(all[0], all[1]))
    }

    // ---- G4: a one-day deviation

    private fun pattern(setups: String) = periods(setups.mapIndexed { i, c -> Day("s$i", i * 2L, sets = if (c == 'A') 3 else 4) })

    @Test fun `a single session that is not the latest is no change`() {
        val aabaa = pattern("AABAA")
        assertEquals(1, aabaa.size)
        assertEquals(5, aabaa.single().sessions.size)
        assertEquals(3, aabaa.single().setup.sets)
        val aabb = pattern("AABB")
        assertEquals(listOf(listOf("s0", "s1"), listOf("s2", "s3")), aabb.map(::ids))
        val aab = pattern("AAB")
        assertEquals(listOf(listOf("s0", "s1"), listOf("s2")), aab.map(::ids))
        val baa = pattern("BAA")
        assertEquals(listOf(listOf("s0", "s1", "s2")), baa.map(::ids))
        assertEquals(3, baa.single().setup.sets)
    }

    // ---- G5: which sessions

    @Test fun `an unfinished session is out, an all skipped one counts for the setup only, two on one date stay in order`() {
        val h = history(listOf(Day("a", 0), Day("open", 1, sets = 4, finished = false), Day("b", 2), Day("skip", 4, status = SetStatus.SKIPPED), Day("c", 6, at = 1), Day("d", 6, at = 2)))
        assertEquals(listOf("a", "b", "skip", "c", "d"), h.map { it.session.id })
        val skip = h.first { it.session.id == "skip" }
        assertEquals(Setup("p3", 3, 8, 12), setupOf(skip))
        assertNull(sessionValue(skip, withWeight(h)))
        assertEquals(1, setupPeriods(h).size)
    }

    @Test fun `the setup counts every row of the exercise and takes the reps of the first row in execution order`() {
        val s = Session("a", start.iso(), 0, 1, ItemType.PROGRAM, programId = "p", programName = "P")
        val logs = listOf(
            SetLog("x", "a", 0, false, 0, 1, 2, "bench", "Bench", 6, 10, 2.5, 60.0, 8, SetStatus.DONE),
            SetLog("y", "a", 0, false, 0, 0, 1, "bench", "Bench", 5, 8, 2.5, 60.0, 8, SetStatus.SKIPPED),
            SetLog("z", "a", 0, false, 0, 2, 3, "bench", "Bench", 6, 10, 2.5, null, null, SetStatus.PENDING),
            SetLog("o", "a", 0, false, 0, 0, 0, "squat", "Squat", 1, 1, 2.5, 100.0, 1, SetStatus.DONE),
        )
        assertEquals(Setup("p", 3, 5, 8), setupOf(liftHistory("bench", logs, listOf(s)).single()))
    }

    // ---- G6: the value of a session

    private fun one(vararg sets: Pair<Double, Int>): LiftSession {
        val s = Session("v${n++}", start.iso(), 0, 1, ItemType.PROGRAM, programId = "p")
        val logs = sets.mapIndexed { i, (kg, reps) -> SetLog("l${n++}", s.id, 0, false, 0, i, i, "bench", "Bench", 8, 12, 2.5, kg, reps, SetStatus.DONE) }
        return liftHistory("bench", logs, listOf(s)).single()
    }

    @Test fun `the value is the greatest Epley estimate with weight, nothing for 0 kg, the most reps without weight`() {
        assertEquals(120.0, sessionValue(one(100.0 to 5, 90.0 to 10), true)!!, 1e-9)
        assertNull(sessionValue(one(0.0 to 12), true))
        val bodyweight = listOf(one(0.0 to 12), one(0.0 to 15))
        assertEquals(false, withWeight(bodyweight))
        assertEquals(15.0, sessionValue(one(0.0 to 12, 0.0 to 15), false)!!, 1e-9)
        assertEquals(true, withWeight(listOf(one(0.0 to 12), one(20.0 to 5))))
    }

    @Test fun `a session without a counted set has no value`() {
        val h = history(listOf(Day("a", 0, reps = 0)))
        assertNull(sessionValue(h.single(), true))
        assertNull(sessionValue(h.single(), false))
    }

    // ---- G7, G8: the rate of a period

    /** One session per (day, reps) with a single set without weight; null reps saves 0 reps, a set that does not count. */
    private fun repsOn(vararg dayValue: Pair<Long, Int?>): List<LiftSession> = history(dayValue.mapIndexed { i, (d, v) ->
        Day("r$i", d, kg = 0.0, reps = v ?: 0, sets = 1)
    })

    @Test fun `the rate is the least squares slope per week`() {
        assertEquals("+1.0", periodRate(repsOn(0L to 100, 7L to 101, 14L to 102, 21L to 103), false))
        assertEquals("−1.0", periodRate(repsOn(0L to 103, 7L to 102, 14L to 101, 21L to 100), false))
        assertEquals("±0.0", periodRate(repsOn(0L to 100, 7L to 100, 14L to 100, 21L to 100), false))
        assertEquals("+1.0", periodRate(repsOn(0L to 100, 7L to 104, 14L to 102), false))
    }

    @Test fun `the rate of weighted sessions is in kilograms of the estimate`() {
        // Estimates 110, 140, 124 a week apart: a slope of 1 kg a day.
        val h = history(listOf(Day("a", 0, kg = 100.0, reps = 3, sets = 1), Day("b", 7, kg = 100.0, reps = 12, sets = 1), Day("c", 14, kg = 120.0, reps = 1, sets = 1)))
        assertEquals(listOf(110.0, 140.0, 124.0), h.map { sessionValue(it, true)!! })
        assertEquals("+7.0", periodRate(h, true))
    }

    @Test fun `a rate needs three valued sessions over 14 days or more`() {
        assertNull(periodRate(repsOn(0L to 10, 30L to 12), false))
        assertNull(periodRate(repsOn(0L to 10, 6L to 11, 13L to 12), false))
        assertNotNull(periodRate(repsOn(0L to 10, 7L to 11, 14L to 12), false))
        assertNull(periodRate(repsOn(0L to 10, 7L to null, 14L to null, 21L to 12, 28L to null), false))
        assertEquals("+0.5", periodRate(repsOn(0L to 10, 7L to 11, 7L to 10, 14L to 11), false))
    }

    // ---- G9: rounding

    @Test fun `the rate rounds as the bodyweight trend does`() {
        assertEquals("+0.3", signedTenths(BigDecimal("0.25"), BigDecimal.ONE))
        assertEquals("−0.3", signedTenths(BigDecimal("-0.25"), BigDecimal.ONE))
        assertEquals("±0.0", signedTenths(BigDecimal("0.04"), BigDecimal.ONE))
        assertEquals("±0.0", signedTenths(BigDecimal("-0.04"), BigDecimal.ONE))
        // × 10 reps the estimate is 4/3 of the weight: 100, 100.25, 100.5 a week apart is exactly +0.25 a week.
        val weighted = history(listOf(Day("a", 0, kg = 75.0, reps = 10, sets = 1), Day("b", 7, kg = 75.1875, reps = 10, sets = 1), Day("c", 14, kg = 75.375, reps = 10, sets = 1)))
        assertEquals("+0.3", periodRate(weighted, true))
        val falling = history(listOf(Day("a", 0, kg = 75.375, reps = 10, sets = 1), Day("b", 7, kg = 75.1875, reps = 10, sets = 1), Day("c", 14, kg = 75.0, reps = 10, sets = 1)))
        assertEquals("−0.3", periodRate(falling, true))
        val small = history(listOf(Day("a", 0, kg = 75.0, reps = 10, sets = 1), Day("b", 7, kg = 75.03, reps = 10, sets = 1), Day("c", 14, kg = 75.06, reps = 10, sets = 1)))
        assertEquals("±0.0", periodRate(small, true))
    }

    // ---- G10, G11: the group and its rows

    private val today = LocalDate.of(2026, 11, 27)

    @Test fun `one period has no rows, three periods are three rows newest first, a reps exercise is titled so`() {
        assertTrue(periodRows(history(listOf(Day("a", 0), Day("b", 2), Day("c", 4))), today).isEmpty())
        val three = history(listOf(Day("a", 0), Day("b", 2), Day("c", 4, sets = 4), Day("d", 6, sets = 4), Day("e", 33, sets = 5), Day("f", 35, sets = 5)))
        val rows = periodRows(three, today)
        assertEquals(listOf("Since Nov 3", "Oct 5 – Nov 2", "Oct 1 – Oct 4"), rows.map { it.dates })
        assertEquals(listOf("5 sets · 8–12 reps", "4 sets · 8–12 reps", "3 sets · 8–12 reps"), rows.map { it.tail })
        assertEquals(listOf(start.plusDays(4), start.plusDays(33)), changeDates(three))
        assertEquals("Est. 1RM per week", rateTitle(withWeight(three)))
        val reps = history(listOf(Day("a", 0, kg = 0.0), Day("b", 2, kg = 0.0), Day("c", 4, kg = 0.0, sets = 4), Day("d", 6, kg = 0.0, sets = 4)))
        assertEquals(false, withWeight(reps))
        assertEquals("Reps per week", rateTitle(withWeight(reps)))
    }

    @Test fun `old dates carry their year, one rep number reads alone, and a row speaks its parts`() {
        val old = history(listOf(Day("a", -300), Day("b", -298), Day("c", 4, sets = 4, min = 5, max = 5), Day("d", 6, sets = 4, min = 5, max = 5)))
        val rows = periodRows(old, today)
        assertEquals("Since Oct 5", rows[0].dates)
        assertEquals("Dec 5, 2025 – Oct 4", rows[1].dates)
        assertEquals("4 sets · 5 reps", rows[0].tail)
        assertEquals("Since Oct 5, TP v3, 4 sets, 5 reps, no rate", rows[0].spoken)
        assertEquals("Dec 5, 2025 to Oct 4, TP v3, 3 sets, 8 to 12 reps, no rate", rows[1].spoken)
        assertEquals("Since Dec 5, 2025", periodDates(setupPeriods(old).first(), null, today))
    }

    @Test fun `the spoken rate has its sign and unit in words`() {
        assertEquals("plus 0.8 kilograms per week", spokenRate("+0.8", true))
        assertEquals("minus 0.3 reps per week", spokenRate("−0.3", false))
        assertEquals("0.0 kilograms per week", spokenRate("±0.0", true))
        assertEquals("no rate", spokenRate(null, true))
    }
}
