package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Night
import dev.mirzohidkhon.khonfitness.data.NightsState
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.iso
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NightRulesTest {
    private val zone = ZoneId.of("Asia/Tashkent")
    private fun d(m: Int, day: Int) = LocalDate.of(2026, m, day)
    private fun ms(date: LocalDate, h: Int, m: Int) = date.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
    private fun night(date: LocalDate, sec: Int) = Night(date, sec, ms(date.minusDays(1), 23, 10), ms(date, 6, 15))

    @Test fun `D7 night lengths`() {
        assertEquals("7 h 05 m", nightLength(25_500))
        assertEquals("45 m", nightLength(2_700))
        assertEquals("1 h 01 m", nightLength(3_660))
        assertEquals("7 h 06 m", nightLength(25_530))
        assertEquals("7 h 05 m", nightLength(25_529))
        assertEquals("7 h 00 m", nightLength(25_200))
        assertEquals("7 hours 5 minutes", nightLengthSpoken(25_500))
        assertEquals("45 minutes", nightLengthSpoken(2_700))
        assertEquals("1 hour 1 minute", nightLengthSpoken(3_660))
        assertEquals("7 hours 6 minutes", nightLengthSpoken(25_530))
        assertEquals("7 hours", nightLengthSpoken(25_200))
    }

    @Test fun `D7 the night line and its spoken text`() {
        val n = night(d(11, 3), 25_500)
        assertEquals("Night 7 h 05 m · 23:10 – 06:15", nightLine(n, zone))
        assertEquals("Night 7 hours 5 minutes, 23:10 to 06:15", nightLineSpoken(n, zone))
        assertEquals("23:10 – 06:15", nightClock(n, zone))
    }

    @Test fun `D7 the Review rows`() {
        val n = Night(d(11, 3), 20_400, ms(d(11, 3), 0, 35), ms(d(11, 3), 6, 15))
        assertEquals(listOf("Night before" to "5 h 40 m", "Asleep" to "00:35 – 06:15"), nightReviewRows(n, zone).map { it.label to it.value })
        assertEquals("Asleep, 00:35 to 06:15", nightReviewRows(n, zone)[1].spoken)
        assertTrue(nightReviewRows(null, zone).isEmpty())
    }

    @Test fun `D8 the Log line of a day`() {
        val today = d(11, 6)
        val loaded = NightsState.Loaded(mapOf(d(11, 3) to night(d(11, 3), 25_500), d(11, 4) to night(d(11, 4), 24_000)))
        assertNull(nightLogLine(d(11, 2), loaded, today, zone))
        assertEquals("Night 7 h 05 m · 23:10 – 06:15", nightLogLine(d(11, 3), loaded, today, zone))
        assertEquals("No night data", nightLogLine(d(11, 5), loaded, today, zone))
        assertEquals("No night data", nightLogLine(today, loaded, today, zone))
        assertNull(nightLogLine(d(11, 7), loaded, today, zone))
        assertNull(nightLogLine(d(11, 3), NightsState.NotRead, today, zone))
        assertNull(nightLogLine(d(11, 5), NightsState.NoAccess, today, zone))
        assertNull(nightLogLine(d(11, 5), NightsState.Failed, today, zone))
        assertNull(nightLogLine(d(11, 5), NightsState.Loaded(emptyMap()), today, zone))
        assertEquals("Night 7 hours 5 minutes, 23:10 to 06:15", nightLogSpoken(d(11, 3), loaded, today, zone))
        assertEquals("No night data", nightLogSpoken(d(11, 5), loaded, today, zone))
    }

    @Test fun `D9 the last 14 nights`() {
        val today = d(11, 27)
        val secs = listOf(25_000, 26_000, 27_001, 24_000, 28_000, 20_000, 29_000, 21_000, 20_000, 30_000, 25_500)
        val days = (0 until 14).map { today.minusDays(it.toLong()) }.filter { it.dayOfMonth !in setOf(20, 22, 25) }
        val nights = days.zip(secs).associate { (day, s) -> day to night(day, s) } + (today.minusDays(14) to night(today.minusDays(14), 60))
        val last = lastNights(nights, today)
        assertEquals(14, last.days.size)
        assertEquals(today.minusDays(13), last.days.first().first)
        assertEquals(today, last.days.last().first)
        assertEquals(11, last.count)
        assertEquals((secs.sumOf { it.toLong() } / 11).toInt(), last.averageSec)
        val twenty = days.zip(secs).filter { it.second == 20_000 }.map { it.first }
        assertEquals(twenty.max(), last.shortest!!.date)
        assertEquals(20_000, last.shortest!!.asleepSec)
        val none = lastNights(emptyMap(), today)
        assertNull(none.averageSec)
        assertEquals(0, none.count)
        assertNull(none.shortest)
        assertTrue(none.days.all { it.second == null })
    }

    private fun session(id: String, date: LocalDate, program: String? = null, exercise: String? = null, finished: Boolean = true) =
        Session(id, date.iso(), startedAt = 1, finishedAt = if (finished) 2 else null, itemType = if (program != null) ItemType.PROGRAM else ItemType.CARDIO,
            programName = program, exerciseName = exercise)

    @Test fun `D10 the night before a session over the last 8 weeks`() {
        val today = d(11, 27)
        val nights = listOf(d(11, 27), d(11, 20), d(11, 10), d(11, 5), today.minusDays(55), today.minusDays(56)).associateWith { night(it, 25_000 + it.dayOfMonth) }
        val sessions = listOf(
            session("p1", d(11, 27), program = "Push"),
            session("p2", d(11, 27), program = "Push"),
            session("p3", d(11, 20), program = "Push"),
            session("p4", today.minusDays(56), program = "Push"),
            session("r1", d(11, 10), exercise = "Easy run"),
            session("r2", d(11, 5), exercise = "Easy run"),
            session("r3", today.minusDays(55), exercise = "Easy run"),
            session("o1", d(11, 20), program = "Legs", finished = false),
            session("x1", d(11, 6), program = "Pull"),
            session("s1", d(11, 20)),
        )
        val rows = nightBeforeSessions(sessions, nights, today)
        assertEquals(listOf("Easy run", "Push", "Session"), rows.map { it.title })
        assertEquals(listOf(today.minusDays(55), d(11, 5), d(11, 10)), rows[0].days)
        assertEquals(3, rows[0].count)
        assertEquals(listOf(d(11, 20), d(11, 27)), rows[1].days)
        assertEquals((nights.getValue(d(11, 20)).asleepSec + nights.getValue(d(11, 27)).asleepSec) / 2, rows[1].averageSec)
        assertEquals(1, rows[2].count)
    }

    @Test fun `D10 equal counts order by title`() {
        val today = d(11, 27)
        val nights = mapOf(today to night(today, 25_000))
        val rows = nightBeforeSessions(listOf(session("b", today, program = "Pull"), session("a", today, program = "Legs")), nights, today)
        assertEquals(listOf("Legs", "Pull"), rows.map { it.title })
    }

    @Test fun `D11 a Nights group after Bodyweight in Compare`() {
        val left = Period(d(10, 1), d(10, 31))
        val right = Period(d(11, 1), d(11, 27))
        val nights = (1..26).map { d(10, it) }.associateWith { night(it, 25_000 + it.dayOfMonth) } + (d(9, 30) to night(d(9, 30), 1_000))
        val sum = (1..26).sumOf { 25_000 + it }
        val l = periodStats(left, emptyList(), emptyList(), emptyList(), emptyList(), listOf(dev.mirzohidkhon.khonfitness.data.Bodyweight(d(10, 2).iso(), 80.0)), nights)
        val r = periodStats(right, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), nights)
        assertEquals(26, l.nights)
        assertEquals(sum / 26, l.nightAvgSec)
        assertEquals(0, r.nights)
        assertNull(r.nightAvgSec)
        val groups = compareGroups(l, r, emptyList())
        val titles = groups.map { it.title }
        assertEquals(titles.indexOf("Bodyweight") + 1, titles.indexOf("Nights"))
        val rows = groups.single { it.title == "Nights" }.rows
        assertEquals(listOf(
            CompareRow("Sleep per night", nightLength(sum / 26), NO_VALUE),
            CompareRow("Nights with data", "26", "0"),
        ), rows)
        val empty = periodStats(left, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertTrue(compareGroups(empty, empty, emptyList()).none { it.title == "Nights" })
        assertTrue(compareGroups(periodStats(left, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyMap()), r, emptyList()).none { it.title == "Nights" })
    }
}
