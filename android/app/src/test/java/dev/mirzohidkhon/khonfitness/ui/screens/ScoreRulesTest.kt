package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.components.cleanTyped
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ScoreRulesTest {
    private val zone: ZoneId = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 11, 27)
    private val nowMs = epochAt(today, zone, 12, 0)

    private fun session(id: String = "s", ex: Exercise = HIGH_EXERCISE, lengthSec: Int = 1800, day: LocalDate = today.minusDays(1)) = scoreSession(id, day, zone, ex, lengthSec)
    private fun buckets(vararg bpm: Int, fromSec: Int = 0) = bpm.mapIndexed { i, b -> HrSample(fromSec + i * SCORE_BUCKET_SEC, b) }
    private fun facts(s: Session, b: List<HrSample>) = hrFacts(s, filledFor(s, zone, b), zone)
    private fun round(s: Session, index: Int, fromSec: Int?, toSec: Int?) =
        IntervalLog("${s.id}-r$index", s.id, index, startedAt = fromSec?.let { s.timedFrom!! + it * 1000L }, endedAt = toSec?.let { s.timedFrom!! + it * 1000L })
    private fun range(s: Session) = hrWindow(s, zone)!!

    @Test fun maximumInUse() {
        assertEquals(186, maxInUse(186, 190))
        assertEquals(183, maxInUse(null, 183))
        assertNull(maxInUse(null, null))
    }

    @Test fun measuredMaximumOfTheLast365Days() {
        val recent = session("recent", day = today.minusDays(10))
        val older = session("older", day = today.minusDays(200))
        val rows = listOf(filledFor(recent, zone, buckets(150, 183)), filledFor(older, zone, buckets(150, 181)))
        assertEquals(183, measuredMaxHr(listOf(recent, older), rows, nowMs, zone))
        val old = session("old", day = today.minusDays(367))
        val oldRow = filledFor(old, zone, buckets(150, 190))
        assertTrue(nowMs - old.finishedAt!! > 366 * 86_400_000L)
        assertNull(measuredMaxHr(listOf(old), listOf(oldRow), nowMs, zone))
        assertNull(measuredMaxHr(emptyList(), emptyList(), nowMs, zone))
    }

    @Test fun measuredMaximumIgnoresRowsThatAreNotFilled() {
        val s = session()
        val filled = filledFor(s, zone, buckets(150, 183))
        listOf(filled.copy(state = HrState.WAITING), filled.copy(state = HrState.NONE), filled.copy(state = HrState.OFF), filled.copy(fromMs = filled.fromMs + 5000)).forEach { row ->
            assertNull(row.toString(), measuredMaxHr(listOf(s), listOf(row), nowMs, zone))
        }
    }

    @Test fun sheetEntryRefusesValuesOutsideTheRange() {
        assertEquals(MaxEntry.Refused, maxEntry(99.0))
        assertEquals(MaxEntry.Refused, maxEntry(231.0))
        assertEquals(MaxEntry.Refused, maxEntry(186.5))
        assertEquals(MaxEntry.Store(100), maxEntry(100.0))
        assertEquals(MaxEntry.Store(230), maxEntry(230.0))
        assertEquals(MaxEntry.Clear, maxEntry(null))
        assertEquals("", cleanTyped("abc", 0))
    }

    @Test fun hardLineIsTheCeilingOf85Percent() {
        assertEquals(159, hardLine(186))
        assertEquals(159, hardLine(187))
        assertEquals(SCORE_BUCKET_SEC.toLong(), hardSeconds(buckets(157, 158, 159), 186))
        assertEquals("11:35", minSec(hardSeconds(List(139) { HrSample(it * 5, 160) }, 186)))
    }

    @Test fun zoneFrom60To70Percent() {
        assertEquals(HrZone(112, 130), hrZone(186))
        assertEquals(ZoneShares(25, 50, 25), zoneShares(buckets(111, 112, 130, 131), 186))
    }

    @Test fun sharesSumTo100() {
        assertEquals(ZoneShares(34, 33, 33), zoneShares(buckets(100, 120, 140), 186))
        assertEquals(listOf(34, 33, 33), percentShares(listOf(1, 1, 1)))
        assertEquals(listOf(13, 37, 50), percentShares(listOf(1, 3, 4)))
        assertEquals(listOf(0, 0, 0), percentShares(listOf(0, 0, 0)))
        for (a in 0..7) for (b in 0..7) for (c in 0..7) if (a + b + c > 0) assertEquals("$a $b $c", 100, percentShares(listOf(a, b, c)).sum())
        assertNull(zoneShares(emptyList(), 186))
        val s = session(ex = LOW_EXERCISE)
        assertEquals(emptyList<ScoreRow>(), scoreRows(s, LOW_EXERCISE, facts(s, emptyList()), emptyList(), 186))
    }

    @Test fun roundPeak() {
        val s = session()
        val work = (0 until 48).map { HrSample(600 + it * 5, 140 + it % 29) }
        val b = work + buckets(120, 119, 118, fromSec = 840)
        val r = roundScores(range(s), b, listOf(round(s, 0, 600, 840)))
        assertEquals(listOf(RoundScore(1, 168, 40)), r)
        assertEquals(emptyList<RoundScore>(), roundScores(range(s), buckets(150, fromSec = 600) + buckets(120, 119, fromSec = 700), listOf(round(s, 0, 600, 700))))
        assertEquals(emptyList<RoundScore>(), roundScores(range(s), b, listOf(round(s, 0, null, 840), round(s, 1, 600, null))))
        assertEquals(emptyList<RoundScore>(), roundScores(range(s), b, listOf(round(s, 0, -60, 840))))
    }

    @Test fun roundDrop() {
        val s = session()
        val work = buckets(150, 160, 166, fromSec = 600)
        val rest = buckets(150, 140, 130, 124, 126, fromSec = 615)
        val next = buckets(150, 160, fromSec = 640)
        val rounds = listOf(round(s, 0, 600, 615), round(s, 1, 640, 650))
        assertEquals(RoundScore(1, 166, 42), roundScores(range(s), work + rest + next, rounds)[0])
        assertNull(roundScores(range(s), work + buckets(124, fromSec = 615) + next, rounds)[0].drop)
        assertEquals(RoundScore(1, 166, -4), roundScores(range(s), work + buckets(170, 172, fromSec = 615) + next, rounds)[0])
    }

    @Test fun dropOfTheLastRoundRunsToTheRangeEnd() {
        val s = session(lengthSec = 1800)
        val work = buckets(160, 165, fromSec = 1750)
        val tail = buckets(150, 140, 135, 132, 130, 129, 128, 127, fromSec = 1760)
        assertEquals(1800, tail.last().offsetSec + 5)
        assertEquals(RoundScore(1, 165, 38), roundScores(range(s), work + tail, listOf(round(s, 0, 1750, 1760)))[0])
        assertNull(roundScores(range(s), work + buckets(150, fromSec = 1795), listOf(round(s, 0, 1750, 1760)))[0].drop)
    }

    @Test fun noFiguresWithoutAFilledFactOfAFinishedCardioSession() {
        val s = session()
        val b = buckets(150, 160, 170)
        val filled = filledFor(s, zone, b)
        val cases = mapOf(
            "waiting" to hrFacts(s, filled.copy(state = HrState.WAITING), zone),
            "none" to hrFacts(s, filled.copy(state = HrState.NONE), zone),
            "not read" to hrFacts(s, null, zone),
            "removed" to hrFacts(s, filled.copy(state = HrState.OFF), zone),
            "stale" to hrFacts(s, filled.copy(toMs = filled.toMs - 5000), zone),
            "unfinished" to hrFacts(s.copy(finishedAt = null), filled, zone),
        )
        cases.forEach { (name, f) ->
            assertEquals(name, emptyList<ScoreRow>(), scoreRows(s, HIGH_EXERCISE, f, emptyList(), 186))
            assertEquals(name, ScoreCharts(emptyList(), emptyList()), scoreCharts(listOf(s, s.copy(id = "t")), mapOf(HIGH_EXERCISE.id to HIGH_EXERCISE), mapOf(s.id to f, "t" to f), 186))
        }
        val gym = s.copy(itemType = ItemType.PROGRAM, exerciseId = null, timedTo = s.timedFrom!! + 20 * 60_000L, finishedAt = s.timedFrom!! + 20 * 60_000L)
        val gymRow = filledRow(gym.id, hrWindow(gym, zone)!!, b)
        assertEquals(emptyList<ScoreRow>(), scoreRows(gym, HIGH_EXERCISE, hrFacts(gym, gymRow, zone), emptyList(), 186))
        assertEquals(ScoreCharts(emptyList(), emptyList()), scoreCharts(listOf(gym, gym.copy(id = "g2")), mapOf(HIGH_EXERCISE.id to HIGH_EXERCISE), mapOf(gym.id to hrFacts(gym, gymRow, zone), "g2" to hrFacts(gym, gymRow, zone)), 186))
    }

    @Test fun reviewRowsByIntensity() {
        val f = intervalFixture(today.minusDays(1), zone)
        val facts = hrFacts(f.session, f.row, zone)
        val high = scoreRows(f.session, HIGH_EXERCISE, facts, f.rounds, 186)
        assertEquals(listOf("At or above 159 bpm", "Round 1", "Round 2", "Round 3", "Round 4"), high.map { it.label })
        assertTrue(high.drop(1).all { it.value.matches(Regex("peak \\d+ · drop \\d+")) })
        assertEquals(listOf("At or above 159 bpm"), scoreRows(f.session, HIGH_EXERCISE, facts, emptyList(), 186).map { it.label })
        val z = zoneFixture(today.minusDays(1), zone)
        val low = scoreRows(z.session, LOW_EXERCISE, hrFacts(z.session, z.row, zone), emptyList(), 186)
        assertEquals(listOf("Below 112 bpm", "112 to 130 bpm", "Above 130 bpm"), low.map { it.label })
        assertEquals(100, low.sumOf { it.value.removeSuffix(" %").toInt() })
        assertEquals(emptyList<ScoreRow>(), scoreRows(f.session, HIGH_EXERCISE, facts, f.rounds, null))
        assertEquals(emptyList<ScoreRow>(), scoreRows(z.session, MODERATE_EXERCISE, hrFacts(z.session, z.row, zone), emptyList(), 186))
        assertEquals(emptyList<ScoreRow>(), scoreRows(z.session, null, hrFacts(z.session, z.row, zone), emptyList(), 186))
    }

    @Test fun spokenTextsUseFullWords() {
        val s = session()
        val b = List(139) { HrSample(it * 5, 160) }
        val hard = scoreRows(s, HIGH_EXERCISE, facts(s, b), emptyList(), 186).single()
        assertEquals(ScoreRow("At or above 159 bpm", "11:35", "At or above 159 beats per minute, 11 minutes 35 seconds"), hard)
        val z = scoreRows(s, LOW_EXERCISE, facts(s, buckets(111, 112, 130, 131)), emptyList(), 186)
        assertEquals(ScoreRow("112 to 130 bpm", "50 %", "112 to 130 beats per minute, 50 percent"), z[1])
        assertEquals("Below 112 beats per minute, 25 percent", z[0].spoken)
        assertEquals(ScoreRow("Round 1", "peak 168 · drop 41", "Round 1, peak 168, drop 41 beats per minute"), roundRow(RoundScore(1, 168, 41)))
        assertEquals(ScoreRow("Round 2", "peak 150", "Round 2, peak 150 beats per minute"), roundRow(RoundScore(2, 150, null)))
        assertEquals(ScoreRow("Round 3", "peak 150 · drop −4", "Round 3, peak 150, drop minus 4 beats per minute"), roundRow(RoundScore(3, 150, -4)))
        assertEquals(MaxShown("186 bpm", true, "Maximum heart rate, 186 beats per minute"), maxShown(186, 183))
        assertEquals(MaxShown("183 bpm", false, "Maximum heart rate, measured 183 beats per minute"), maxShown(null, 183))
        assertEquals(MaxShown("Not set", false, "Maximum heart rate, not set"), maxShown(null, null))
    }

    @Test fun typeChartPointsInDateOrder() {
        val days = listOf(5L, 2L, 9L, 3L, 7L).map { today.minusDays(it) }
        val ss = days.mapIndexed { i, d -> intervalFixture(d, zone, "h$i") }
        val facts = ss.mapIndexed { i, f -> f.session.id to if (i < 3) hrFacts(f.session, f.row, zone) else hrFacts(f.session, null, zone) }.toMap()
        val exercises = mapOf(HIGH_EXERCISE.id to HIGH_EXERCISE)
        val charts = scoreCharts(ss.map { it.session }, exercises, facts, 186)
        assertEquals(listOf(today.minusDays(9), today.minusDays(5), today.minusDays(2)), charts.hardMinutes.map { it.date })
        assertTrue(charts.hardMinutes.all { it.value > 0 && !it.mark })
        assertEquals(emptyList<Any>(), charts.zonePercent)
        val one = scoreCharts(ss.map { it.session }, exercises, facts.mapValues { (k, v) -> if (k == "h0") v else HrFacts.NotRead }, 186)
        assertEquals(ScoreCharts(emptyList(), emptyList()), one)
        assertEquals(ScoreCharts(emptyList(), emptyList()), scoreCharts(ss.map { it.session }, exercises, facts, null))
        val zs = listOf(1L, 4L).map { today.minusDays(it) }.mapIndexed { i, d -> zoneFixture(d, zone, "z$i") }
        val zc = scoreCharts(zs.map { it.session }, mapOf(LOW_EXERCISE.id to LOW_EXERCISE), zs.associate { it.session.id to hrFacts(it.session, it.row, zone) }, 186)
        assertEquals(listOf(today.minusDays(4), today.minusDays(1)), zc.zonePercent.map { it.date })
    }
}
