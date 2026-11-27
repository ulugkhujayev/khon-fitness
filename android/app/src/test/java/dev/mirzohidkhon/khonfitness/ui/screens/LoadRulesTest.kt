package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class LoadRulesTest {
    private val zone: ZoneId = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 11, 25)

    private fun session(id: String, day: LocalDate = today.minusDays(1), lengthSec: Int = 1800, h: Int = 18) = scoreSession(id, day, zone, HIGH_EXERCISE, lengthSec, h)
    private fun buckets(n: Int, bpm: Int, fromSec: Int = 0) = (0 until n).map { HrSample(fromSec + it * SCORE_BUCKET_SEC, bpm) }
    private fun filled(s: Session, b: List<HrSample>) = hrFacts(s, filledFor(s, zone, b), zone)

    @Test fun weightsByTheIntegerRule() {
        val bpm = listOf(99, 100, 119, 120, 139, 140, 159, 160, 179, 180, 200, 230)
        assertEquals(listOf(0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 5), bpm.map { loadWeight(it, 200) })
        val at187 = listOf(93 to 0, 94 to 1, 112 to 1, 113 to 2, 130 to 2, 131 to 3, 149 to 3, 150 to 4, 168 to 4, 169 to 5)
        assertEquals(at187.map { it.second }, at187.map { loadWeight(it.first, 187) })
    }

    @Test fun loadIsWeightedMinutesRoundedHalfUp() {
        val s = session("s")
        assertEquals(5, sessionLoad(s, filled(s, buckets(12, 190)), 200))
        assertEquals(1, sessionLoad(s, filled(s, buckets(6, 100)), 200))
        assertEquals(0, sessionLoad(s, filled(s, buckets(5, 100)), 200))
        assertEquals(0, sessionLoad(s, filled(s, buckets(40, 90)), 200))
        val gaps = buckets(6, 190) + buckets(6, 190, fromSec = 600) + HrSample(1800, 190) + HrSample(5000, 190)
        assertEquals(5, sessionLoad(s, filled(s, gaps), 200))
    }

    @Test fun eachOffsetCountsOnceTheFirstStored() {
        val s = session("s")
        assertEquals(5, sessionLoad(s, filled(s, buckets(12, 190) + buckets(12, 190)), 200))
        val firstLow = buckets(12, 120).zip(buckets(12, 190)).flatMap { (a, b) -> listOf(a, b) }
        assertEquals(2, sessionLoad(s, filled(s, firstLow), 200))
        val firstHigh = buckets(12, 190).zip(buckets(12, 120)).flatMap { (a, b) -> listOf(a, b) }
        assertEquals(5, sessionLoad(s, filled(s, firstHigh), 200))
    }

    @Test fun sumsAndThresholdsDoNotWrap() {
        val monday = LocalDate.of(2026, 11, 23)
        val week = LoadWeek(monday, listOf(LoadDay(monday, 1, Int.MAX_VALUE), LoadDay(monday.plusDays(1), 1, 1)))
        assertEquals(2_147_483_648L, week.load)
        assertEquals("Week of November 23, load 2147483648, 2 days", loadWeekSpoken(week))
        assertEquals(4, loadStep(600_000_000, 600_000_000))
        assertEquals(listOf(1, 2, 3, 4), listOf(1, 2, 3, 4).map { loadStep(it * 500_000_000, Int.MAX_VALUE) })
        assertEquals(Int.MAX_VALUE, dayLoad(listOf(Int.MAX_VALUE - 1, Int.MAX_VALUE - 1)))
        assertEquals(Int.MAX_VALUE, dayLoad(listOf(Int.MAX_VALUE, 1)))
        assertEquals(3, dayLoad(listOf(1, 2)))
        assertNull(dayLoad(emptyList()))
        assertEquals(5, loadWeight(Int.MAX_VALUE, 200))
        assertEquals(0, loadWeight(Int.MIN_VALUE, 200))
        val year = LoadYear(listOf(week, LoadWeek(monday.minusWeeks(1), listOf(LoadDay(monday.minusWeeks(1), 1, 0)))))
        assertEquals(listOf(0.0, 2_147_483_648.0), loadChart(year).map { it.value })
    }

    @Test fun monthLabelOnlyFromDrawnDays() {
        val monday = LocalDate.of(2026, 11, 30)
        assertNull(loadMonth(loadYear(emptyList(), emptyMap(), null, monday).weeks.first()))
        assertEquals("Dec", loadMonth(loadYear(emptyList(), emptyMap(), null, monday.plusDays(1)).weeks.first()))
        val full = loadYear(emptyList(), emptyMap(), null, LocalDate.of(2026, 12, 9)).weeks[1]
        assertEquals(LocalDate.of(2026, 11, 30), full.monday)
        assertEquals(7, full.days.size)
        assertEquals("Dec", loadMonth(full))
    }

    @Test fun noLoadWithoutAFilledFactOrAMaximum() {
        val s = session("s")
        val range = hrWindow(s, zone)!!
        val row = filledFor(s, zone, buckets(12, 190))
        assertNull(sessionLoad(s, hrFacts(s, row.copy(state = HrState.WAITING), zone), 200))
        assertNull(sessionLoad(s, hrFacts(s, row.copy(state = HrState.NONE), zone), 200))
        assertNull(sessionLoad(s, hrFacts(s, row.copy(state = HrState.OFF), zone), 200))
        assertNull(sessionLoad(s, hrFacts(s, row.copy(toMs = range.toMs - 1000), zone), 200))
        assertNull(sessionLoad(s, hrFacts(s, null, zone), 200))
        assertNull(sessionLoad(s, null, 200))
        assertNull(sessionLoad(s, filled(s, buckets(12, 190)), null))
        val open = s.copy(finishedAt = null)
        assertNull(sessionLoad(open, HrFacts.Filled(row), 200))
    }

    private fun yearOf(vararg pairs: Pair<Session, HrFacts?>, max: Int? = 200, on: LocalDate = today) =
        loadYear(pairs.map { it.first }, pairs.mapNotNull { (s, f) -> f?.let { s.id to it } }.toMap(), max, on)

    private fun LoadYear.day(d: LocalDate) = weeks.flatMap { it.days }.single { it.date == d }

    @Test fun dayAndWeekSums() {
        val day = today.minusDays(1)
        val a = session("a", day, lengthSec = 3600, h = 7)
        val b = session("b", day)
        val c = session("c", day.minusDays(1))
        val year = yearOf(a to filled(a, buckets(96, 190)), b to null, c to null)
        assertEquals(LoadDay(day, 2, 40), year.day(day))
        val trained = year.day(day.minusDays(1))
        assertEquals(1, trained.sessions)
        assertNull(trained.load)
        assertFalse(trained.empty)
        assertTrue(year.day(day.minusDays(2)).empty)
        assertEquals(40L, year.weeks[0].load)
        assertEquals(2, year.weeks[0].sessionDays)
        assertNull(year.weeks[1].load)
    }

    @Test fun weekAcrossAMonthAndAYearEnd() {
        val on = LocalDate.of(2026, 1, 2)
        val monday = LocalDate.of(2025, 12, 29)
        val sunday = monday.minusDays(1)
        val dec31 = session("dec31", LocalDate.of(2025, 12, 31))
        val jan1 = session("jan1", LocalDate.of(2026, 1, 1))
        val first = session("mon", monday)
        val before = session("sun", sunday)
        val year = yearOf(
            dec31 to filled(dec31, buckets(12, 190)), jan1 to filled(jan1, buckets(24, 190)),
            first to filled(first, buckets(6, 100)), before to filled(before, buckets(12, 190)), on = on,
        )
        assertEquals(monday, year.weeks[0].monday)
        assertEquals(DayOfWeek.MONDAY, year.weeks[0].days.first().date.dayOfWeek)
        assertEquals(listOf(monday, monday.plusDays(1), monday.plusDays(2), monday.plusDays(3), monday.plusDays(4)), year.weeks[0].days.map { it.date })
        assertEquals(5L + 10 + 1, year.weeks[0].load)
        assertEquals(3, year.weeks[0].sessionDays)
        assertEquals(5L, year.weeks[1].load)
        assertEquals(sunday, year.weeks[1].days.last().date)
        assertEquals("Jan", loadMonth(year.weeks[0]))
        assertNull(loadMonth(year.weeks[1]))
    }

    @Test fun stepsAgainstTheHighestDay() {
        assertEquals(listOf(1, 1, 2, 2, 3, 4, 4), listOf(10, 25, 26, 50, 75, 76, 100).map { loadStep(it, 100) })
        val s = session("s")
        val one = yearOf(s to filled(s, buckets(12, 190)))
        assertEquals(5, one.highest)
        assertEquals(4, loadStep(5, one.highest!!))
        val zero = yearOf(s to filled(s, buckets(12, 90)))
        assertEquals(0, zero.highest)
        assertEquals(1, loadStep(0, 0))
    }

    @Test fun yearIs52WeeksNewestFirst() {
        assertEquals(DayOfWeek.WEDNESDAY, today.dayOfWeek)
        val monday = weekOf(today)
        val oldest = session("oldest", monday.minusWeeks(51))
        val outside = session("outside", monday.minusWeeks(51).minusDays(1))
        val later = session("later", today.plusDays(1))
        val year = yearOf(oldest to filled(oldest, buckets(12, 190)), outside to filled(outside, buckets(12, 190)), later to filled(later, buckets(12, 190)))
        assertEquals(52, year.weeks.size)
        assertEquals((0L until 52L).map { monday.minusWeeks(it) }, year.weeks.map { it.monday })
        assertEquals(listOf(monday, monday.plusDays(1), today), year.weeks[0].days.map { it.date })
        assertTrue(year.weeks.drop(1).all { it.days.size == 7 })
        assertEquals(5L, year.weeks.last().load)
        assertEquals(5, year.highest)
        assertEquals(listOf(5L), year.weeks.mapNotNull { it.load })
    }

    @Test fun anyLoadNeedsALoadedSessionInTheYear() {
        val s = session("s")
        assertFalse(yearOf(s to null).anyLoad)
        assertFalse(yearOf(s to filled(s, buckets(12, 190)), max = null).anyLoad)
        val old = session("old", weekOf(today).minusWeeks(52))
        assertFalse(yearOf(old to filled(old, buckets(12, 190))).anyLoad)
        assertTrue(yearOf(s to filled(s, buckets(12, 190))).anyLoad)
    }

    @Test fun chartOfTheLast12Weeks() {
        val monday = weekOf(today)
        val weeks = listOf(0L, 2L, 3L, 7L, 11L, 12L)
        val pairs = weeks.mapIndexed { i, w -> session("w$w", monday.minusWeeks(w).plusDays(1)).let { it to filled(it, buckets(12 * (i + 1), 190)) } }
        val points = loadChart(yearOf(*pairs.toTypedArray(), on = monday.plusDays(1)))
        assertEquals(listOf(11L, 7L, 3L, 2L, 0L).map { monday.minusWeeks(it) }, points.map { it.date })
        assertEquals(listOf(25.0, 20.0, 15.0, 10.0, 5.0), points.map { it.value })
        val single = pairs.first()
        assertTrue(loadChart(yearOf(single)).isEmpty())
    }

    @Test fun weekRowsSpeakInFullWords() {
        val monday = LocalDate.of(2026, 11, 23)
        val week = LoadWeek(monday, (0L until 7L).map { LoadDay(monday.plusDays(it), if (it < 4) 1 else 0, if (it < 2) 206 else null) })
        assertEquals("Week of November 23, load 412, 4 days", loadWeekSpoken(week))
        val none = LoadWeek(monday, (0L until 7L).map { LoadDay(monday.plusDays(it), if (it == 0L) 1 else 0, null) })
        assertEquals("Week of November 23, no load, 1 day", loadWeekSpoken(none))
        assertEquals("Week of November 23, no load, 0 days", loadWeekSpoken(LoadWeek(monday, emptyList())))
    }
}
