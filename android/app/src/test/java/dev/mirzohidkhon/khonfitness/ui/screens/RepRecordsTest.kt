package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.iso
import dev.mirzohidkhon.khonfitness.ui.components.ChartPoint
import dev.mirzohidkhon.khonfitness.ui.components.trendAt
import dev.mirzohidkhon.khonfitness.ui.components.valueBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Rep records next to weight records, the most reps by weight, and the 7-day bodyweight trend. */
class RepRecordsTest {
    private val start = LocalDate.of(2026, 10, 1)
    private fun gym(id: String, day: Long, at: Long = 0, finished: Boolean = true) =
        Session(id, start.plusDays(day).iso(), startedAt = at, finishedAt = if (finished) at + 1 else null, itemType = ItemType.PROGRAM, programId = "p", programName = "Push")
    private var n = 0
    private fun set(session: String, kg: Double?, reps: Int?, ex: String = "bench", status: String = SetStatus.DONE) =
        SetLog("l${n++}", session, 0, false, 0, n, n, ex, "Bench press", 6, 10, 2.5, weightKg = kg, reps = reps, status = status)

    private fun text(sets: List<SetLog>) = sets.map { "${(it.weightKg ?: 0.0).toInt()}x${it.reps}" }
    private fun history(logs: List<SetLog>, sessions: List<Session>, ex: String = "bench") = liftHistory(ex, logs, sessions)

    // ---- Rep records in History

    @Test fun `more reps under a heavier earlier record is a rep record, not a weight record`() {
        val sessions = listOf(gym("a", 0), gym("b", 2), gym("c", 4))
        val logs = listOf(set("a", 85.0, 3), set("b", 80.0, 6), set("c", 80.0, 8))
        val h = history(logs, sessions)
        assertEquals(listOf("80x8"), text(h.last().repRecords))
        assertEquals(false, h.last().record)
        val all = liftHistories(logs, sessions)
        assertEquals(1, repRecordCount("c", all))
        assertEquals(0, recordCount("c", all))
    }

    @Test fun `equal reps or a lighter set matched at a heavier weight is no rep record`() {
        val sessions = listOf(gym("a", 0), gym("b", 2), gym("c", 4))
        val logs = listOf(set("a", 80.0, 6), set("b", 80.0, 6), set("c", 75.0, 6))
        val h = history(logs, sessions)
        assertTrue(h.all { it.repRecords.isEmpty() })
        val all = liftHistories(logs, sessions)
        assertEquals(0, repRecordCount("b", all))
        assertEquals(0, repRecordCount("c", all))
    }

    @Test fun `a set matched by a heavier set of the same session is no rep record`() {
        // Earlier 80x6 and 70x7: 80x8 beats 80x6 on reps at the same weight, so it is the weight record and not a rep record.
        val sameTop = history(listOf(set("a", 80.0, 6), set("a", 70.0, 7), set("b", 80.0, 8), set("b", 70.0, 8)), listOf(gym("a", 0), gym("b", 2)))
        assertEquals(true, sameTop.last().record)
        assertEquals(emptyList<String>(), text(sameTop.last().repRecords))
        val sameTopBoth = history(listOf(set("a", 80.0, 6), set("a", 70.0, 7), set("b", 80.0, 8), set("b", 70.0, 10)), listOf(gym("a", 0), gym("b", 2)))
        assertEquals(listOf("70x10"), text(sameTopBoth.last().repRecords))
        // Under a heavier record (85x3), 80x8 is a rep record too.
        val sessions = listOf(gym("a", 0), gym("b", 2))
        val only = history(listOf(set("a", 85.0, 3), set("a", 80.0, 6), set("a", 70.0, 7), set("b", 80.0, 8), set("b", 70.0, 8)), sessions)
        assertEquals(listOf("80x8"), text(only.last().repRecords))
        val both = history(listOf(set("a", 85.0, 3), set("a", 80.0, 6), set("a", 70.0, 7), set("b", 80.0, 8), set("b", 70.0, 10)), sessions)
        assertEquals(listOf("80x8", "70x10"), text(both.last().repRecords))
    }

    @Test fun `a weight record session keeps its rep record sets, but counts only as a PR`() {
        val sessions = listOf(gym("a", 0), gym("b", 2))
        val logs = listOf(set("a", 80.0, 6), set("b", 85.0, 3), set("b", 80.0, 8))
        val h = history(logs, sessions).last()
        assertEquals(true, h.record)
        assertEquals("85x3", text(listOf(h.best!!)).single())
        assertEquals(listOf("80x8"), text(h.repRecords))
        val all = liftHistories(logs, sessions)
        assertEquals(0, repRecordCount("b", all))
        assertEquals(1, recordCount("b", all))
    }

    @Test fun `the first session, 0 kg, uncounted sets and open sessions never give or serve as rep records`() {
        val first = history(listOf(set("a", 80.0, 6), set("a", 70.0, 10)), listOf(gym("a", 0)))
        assertTrue(first.single().repRecords.isEmpty())
        // A 0 kg exercise: more reps is the weight record.
        val push = history(listOf(set("a", null, 10, ex = "push"), set("b", null, 15, ex = "push")), listOf(gym("a", 0), gym("b", 2)), ex = "push")
        assertEquals(true, push.last().record)
        assertTrue(push.last().repRecords.isEmpty())
        // 0 kg under a weighted earlier set: still none at 0 kg.
        val mixed = history(listOf(set("a", 10.0, 5), set("b", 0.0, 12)), listOf(gym("a", 0), gym("b", 2)))
        assertTrue(mixed.last().repRecords.isEmpty())
        // Skipped and 0-rep sets are no candidate and no earlier set.
        val skipped = history(
            listOf(set("a", 85.0, 3), set("a", 80.0, 12, status = SetStatus.SKIPPED), set("a", 80.0, 0), set("b", 80.0, 9, status = SetStatus.SKIPPED), set("b", 80.0, 0), set("b", 80.0, 5)),
            listOf(gym("a", 0), gym("b", 2)),
        )
        assertEquals(listOf("80x5"), text(skipped.last().repRecords))
        // An open session never counts and is never earlier.
        val open = history(listOf(set("a", 85.0, 3), set("o", 80.0, 20), set("b", 80.0, 8)), listOf(gym("a", 0), gym("o", 1, finished = false), gym("b", 2)))
        assertEquals(2, open.size)
        assertEquals(listOf("80x8"), text(open.last().repRecords))
        // Two sessions on one date: start order decides which is earlier.
        val sameDay = history(listOf(set("a", 85.0, 3), set("late", 80.0, 8), set("early", 80.0, 6)), listOf(gym("a", 0), gym("late", 2, at = 200), gym("early", 2, at = 100)))
        assertEquals(listOf("early", "late"), sameDay.drop(1).map { it.session.id })
        assertEquals(listOf("80x6"), text(sameDay[1].repRecords))
        assertEquals(listOf("80x8"), text(sameDay[2].repRecords))
    }

    @Test fun `of two equal sets above the earlier best, the earlier one in set order is the rep record`() {
        val logs = listOf(set("a", 85.0, 3), set("a", 80.0, 6), set("b", 80.0, 8), set("b", 80.0, 8))
        val h = history(logs, listOf(gym("a", 0), gym("b", 2))).last()
        assertEquals(1, h.repRecords.size)
        assertTrue(h.repRecords.single() === h.sets[0])
    }

    // ---- The finish sheet

    @Test fun `the finish sheet counts new bests and rep records apart, against every other session`() {
        val old = listOf(set("old", 100.0, 5, ex = "squat"), set("old", 85.0, 3), set("old", 80.0, 6))
        val today = listOf(set("s", 105.0, 5, ex = "squat"), set("s", 80.0, 8), set("s", 80.0, 8, ex = "squat"))
        val sum = summarize(today, old + today, "s", 0L, 60_000L)
        assertEquals(1, sum.bests)
        assertEquals(1, sum.repRecords)
        // An old session edited from History counts against every other one, later ones too.
        val later = listOf(set("later", 80.0, 7))
        val edited = listOf(set("s2", 80.0, 8))
        assertEquals(1, summarize(edited, old + later + edited, "s2", 0L, 60_000L).repRecords)
        assertEquals(0, summarize(edited, old + later + listOf(set("later2", 80.0, 9)) + edited, "s2", 0L, 60_000L).repRecords)
        // A first session has nothing to beat.
        assertEquals(0, summarize(today, today, "s", 0L, 60_000L).repRecords)
    }

    // ---- Most reps by weight

    @Test fun `the most reps by weight are the sets no other set matches, heaviest first, each with its first session`() {
        val sessions = listOf(gym("a", 0), gym("b", 2), gym("c", 4), gym("d", 6))
        val logs = listOf(set("a", 85.0, 3), set("a", 80.0, 6), set("b", 80.0, 8), set("b", 70.0, 8), set("c", 60.0, 15), set("d", 80.0, 8))
        val bests = repBests(history(logs, sessions))
        assertEquals(listOf("85x3", "80x8", "60x15"), text(bests.map { it.first }))
        assertEquals(listOf("a", "b", "c"), bests.map { it.second.id })
        assertEquals(1, repBests(history(listOf(set("a", 80.0, 8), set("b", 70.0, 6)), listOf(gym("a", 0), gym("b", 2)))).size)
    }

    // ---- Bodyweight trend

    private val today = LocalDate.of(2026, 10, 20)
    private fun bw(daysAgo: Long, kg: Double) = Bodyweight(today.minusDays(daysAgo).iso(), kg)

    @Test fun `the week average needs 3 entries in 7 days, the rate needs two such weeks`() {
        val two = listOf(bw(0, 83.0), bw(3, 83.4))
        assertNull(weekAverage(two, today))
        assertNull(trendText(two, today))
        val three = two + bw(6, 83.2)
        assertEquals(83.2, weekAverage(three, today)!!, 1e-9)
        assertEquals("7-day avg 83.2 kg", trendText(three, today))
        val olderTwo = three + bw(7, 84.0) + bw(10, 84.0)
        assertEquals("7-day avg 83.2 kg", trendText(olderTwo, today))
        assertNull(weekRate(olderTwo, today))
        val olderThree = olderTwo + bw(13, 83.8)
        assertEquals(83.2 - (84.0 + 84.0 + 83.8) / 3, weekRate(olderThree, today)!!, 1e-9)
        assertEquals("7-day avg 83.2 kg · −0.7 kg/week", trendText(olderThree, today))
        // An entry after today counts nowhere; today - 6 is this week, today - 7 the week before.
        assertEquals(83.2, weekAverage(three + Bodyweight(today.plusDays(1).iso(), 99.0), today)!!, 1e-9)
        assertNull(weekAverage(listOf(bw(0, 83.0), bw(3, 83.4), bw(7, 83.2)), today))
        assertEquals(83.2, weekAverage(listOf(bw(7, 83.2), bw(8, 83.2), bw(13, 83.2)), today.minusDays(7))!!, 1e-9)
    }

    @Test fun `the rate reads plus, minus, or plus-minus inside 0,05 kg`() {
        fun rate(d: Double) = trendText(listOf(bw(0, 80.0 + d), bw(1, 80.0 + d), bw(2, 80.0 + d), bw(7, 80.0), bw(8, 80.0), bw(9, 80.0)), today)!!.substringAfter(" · ")
        assertEquals("+0.3 kg/week", rate(0.30))
        assertEquals("−0.3 kg/week", rate(-0.30))
        assertEquals("±0.0 kg/week", rate(0.04))
        assertEquals("±0.0 kg/week", rate(-0.04))
    }

    @Test fun `the rate rounds its size half up before its sign, and the average rounds half up as entered`() {
        val flat = listOf(bw(7, 80.0), bw(8, 80.0), bw(9, 80.0), bw(10, 80.0))
        fun rate(vararg kg: Double) = trendText(kg.mapIndexed { i, v -> bw(i.toLong(), v) } + flat, today)
        assertEquals("7-day avg 79.8 kg · −0.3 kg/week", rate(79.7, 79.7, 79.8, 79.8))
        assertEquals("7-day avg 80.3 kg · +0.3 kg/week", rate(80.2, 80.2, 80.3, 80.3))
        assertEquals("7-day avg 80.0 kg · −0.1 kg/week", rate(79.9, 79.9, 80.0, 80.0))
        assertEquals("7-day avg 80.1 kg · +0.1 kg/week", rate(80.0, 80.0, 80.1, 80.1))
        assertEquals("7-day avg 80.0 kg · ±0.0 kg/week", rate(79.96, 79.96, 79.96, 79.96))
        assertEquals("7-day avg 80.0 kg · ±0.0 kg/week", rate(80.04, 80.04, 80.04, 80.04))
        assertEquals("7-day avg 79.5 kg", trendText(listOf(bw(3, 79.0), bw(2, 79.0), bw(1, 79.2), bw(0, 80.6)), today))
        assertEquals("7-day avg 79.5 kg", bodyweightStatus(listOf(bw(3, 79.0), bw(2, 79.0), bw(1, 79.2), bw(0, 80.6)), today))
    }

    @Test fun `Today's line is the trend once today's entry is saved and the week has an average`() {
        val week = listOf(bw(6, 83.4), bw(3, 83.2))
        assertEquals("Last: 83.2 kg · Oct 17", bodyweightStatus(week, today))
        assertEquals("Saved for today · −0.2 kg since Oct 17", bodyweightStatus(listOf(bw(3, 83.2), bw(0, 83.0)), today))
        assertEquals("7-day avg 83.2 kg", bodyweightStatus(week + bw(0, 83.0), today))
        val rate = listOf(bw(13, 83.5), bw(10, 83.5), bw(7, 83.5)) + week + bw(0, 83.0)
        assertEquals("7-day avg 83.2 kg · −0.3 kg/week", bodyweightStatus(rate.sortedBy { it.date }, today))
    }

    @Test fun `the trend points use entries before the range and need 3 in each window, the scale covers both lines`() {
        val all = listOf(bw(40, 85.0), bw(33, 85.0), bw(31, 85.0), bw(29, 84.0), bw(27, 84.0), bw(20, 83.0), bw(2, 82.0))
        val inRange = bodyweightsIn(BodyRange.MONTH, all, today)
        assertEquals(listOf(29L, 27L, 20L, 2L), inRange.map { java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(it.date), today) })
        val points = trendPoints(all, inRange)
        // Day 29 takes 33 and 31 from before the range; day 20 and day 2 have one entry in their 7 days: no point.
        assertEquals(listOf(ChartPoint(today.minusDays(29), (85.0 + 85.0 + 84.0) / 3), ChartPoint(today.minusDays(27), 84.5)), points)
        val main = inRange.map { ChartPoint(LocalDate.parse(it.date), it.kg) }
        val at = trendAt(main, points)
        assertEquals(listOf(0 to (85.0 + 85.0 + 84.0) / 3, 1 to 84.5), at)
        assertEquals(82.0 to 84.0, valueBounds(main, emptyList()))
        assertEquals(82.0 to 90.0, valueBounds(main, listOf(0 to 90.0)))
        assertEquals(emptyList<Pair<Int, Double>>(), trendAt(main, listOf(ChartPoint(today.minusDays(5), 80.0))))
    }

    // ---- The Log line

    @Test fun `the Log line keeps each dot with the count after it, in green and in the text color`() {
        val line = doneLine("2 of 2 sets · 1.2 t · 58 min", 1, 2)
        assertEquals("2 of 2 sets · 1.2 t · 58 min ·\u00A01 PR ·\u00A02\u00A0rep\u00A0PR", line.text)
        assertTrue(Regex("·[ \n]").find(line.text.substringAfter("58 min")) == null)
        fun style(part: String) = line.spanStyles.single { line.text.substring(it.start, it.end) == part }.item
        assertEquals(dev.mirzohidkhon.khonfitness.ui.theme.K.Green, style("1 PR").color)
        assertEquals(androidx.compose.ui.text.font.FontWeight.SemiBold, style("1 PR").fontWeight)
        assertEquals(dev.mirzohidkhon.khonfitness.ui.theme.K.Text, style("2\u00A0rep\u00A0PR").color)
        assertEquals(androidx.compose.ui.text.font.FontWeight.SemiBold, style("2\u00A0rep\u00A0PR").fontWeight)
        assertEquals("58 min ·\u00A01\u00A0rep\u00A0PR", doneLine("58 min", 0, 1).text)
        assertEquals("58 min", doneLine("58 min", 0, 0).text)
    }
}
