package dev.mirzohidkhon.khonfitness.ui.screens

import dev.mirzohidkhon.khonfitness.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PictureRulesTest {
    private val day = LocalDate.of(2026, 11, 23)
    private val zone = TEST_ZONE
    private val from = at(18, 0, day)
    private val gym = Session("g", day.iso(), from, from + 80 * MIN, ItemType.PROGRAM, programName = "Push", timedFrom = from, timedTo = from + 70 * MIN)
    private val range = hrWindow(gym, zone)!!

    private fun row(buckets: List<HrSample>, s: Session = gym, state: String = HrState.FILLED, r: HrRange = hrWindow(s, zone)!!) =
        filledRow(s.id, r, buckets).copy(state = state)

    private fun every5(fromSec: Int, toSec: Int, bpm: (Int) -> Int = { 120 }) = (fromSec until toSec step 5).map { HrSample(it, bpm(it)) }

    private fun set(id: String, setIndex: Int, checkedAt: Long?, status: String = SetStatus.DONE, name: String = "Bench") =
        SetLog(id, gym.id, 0, false, 0, setIndex, setIndex, "bench", name, 8, 12, 2.5, weightKg = 80.0, reps = 10, status = status, checkedAt = checkedAt)

    @Test fun `curve segments join gaps of 60 s and cut gaps of more, a lone bucket is a dot`() {
        assertEquals(listOf(30), curveSegments(every5(0, 150)).map { it.size })
        assertEquals(1, curveSegments(listOf(HrSample(0, 100), HrSample(60, 101))).size)
        assertEquals(listOf(1, 1), curveSegments(listOf(HrSample(0, 100), HrSample(65, 101))).map { it.size })
        val withDot = every5(0, 100) + HrSample(200, 130) + every5(300, 400)
        assertEquals(listOf(20, 1, 20), curveSegments(withDot).map { it.size })
        val facts = hrFacts(gym, row(withDot), zone)
        val p = workoutPicture(gym, facts, emptyList(), emptyList(), zone)!!
        assertEquals(listOf(20, 1, 20), p.curve.map { it.size })
        assertEquals(withDot, p.curve.flatten())
    }

    @Test fun `no curve and no chart without a filled fact of the current range`() {
        val buckets = every5(0, 600)
        val waiting = hrFacts(gym, row(buckets, state = HrState.WAITING), zone)
        val none = hrFacts(gym, row(emptyList(), state = HrState.NONE), zone)
        val removed = hrFacts(gym, row(buckets, state = HrState.OFF), zone)
        val stale = hrFacts(gym, row(buckets, r = HrRange(range.fromMs, range.toMs - MIN)), zone)
        val notRead = hrFacts(gym, null, zone)
        for (f in listOf(waiting, none, removed, stale, notRead)) {
            val p = workoutPicture(gym, f, emptyList(), emptyList(), zone)!!
            assertTrue("$f", p.curve.isEmpty())
            assertFalse("$f", p.hasChart)
            assertNull("$f", p.axis)
        }
        val noRange = gym.copy(timedFrom = null, timedTo = null)
        assertNull(workoutPicture(noRange, hrFacts(noRange, row(buckets, r = range), zone), emptyList(), emptyList(), zone))
        val unfinished = gym.copy(finishedAt = null)
        assertNull(workoutPicture(unfinished, hrFacts(unfinished, row(buckets, r = range), zone), emptyList(), emptyList(), zone))
    }

    @Test fun `marks are the stamped done sets inside the range in time order`() {
        val logs = listOf(
            set("a", 0, from + 20 * MIN),
            set("b", 1, from + 10 * MIN),
            set("before", 2, from - 1),
            set("after", 3, from + 70 * MIN),
            set("noStamp", 4, null),
            set("skipped", 5, from + 30 * MIN, status = SetStatus.SKIPPED),
            set("first", 6, from),
        )
        assertEquals(listOf("first", "b", "a"), setMarks(range, logs).map { it.id })
        val p = workoutPicture(gym, hrFacts(gym, null, zone), logs, emptyList(), zone)!!
        assertEquals(listOf(0L, 10 * MIN, 20 * MIN), p.marks)
    }

    @Test fun `set rows give the clock, the time since the mark before, and the highest bpm of the span`() {
        val m1 = at(18, 10, day); val m2 = at(18, 12, day, 5); val m3 = at(18, 15, day)
        val logs = listOf(set("a", 0, m1), set("b", 1, m2), set("c", 2, m3))
        val buckets = every5(0, 20 * 60) { t -> 100 + (t / 5) % 50 }
        fun max(fromMs: Long, toMs: Long) = buckets.filter { from + it.offsetSec * 1000L in fromMs until toMs }.maxOf { it.bpm }
        val rows = workoutPicture(gym, hrFacts(gym, row(buckets), zone), logs, emptyList(), zone)!!.rows
        assertEquals(listOf("18:10", "18:12", "18:15"), rows.map { it.clock })
        assertEquals(listOf(null, "2:05", "2:55"), rows.map { r -> r.sinceSec?.let(::minSec) })
        assertEquals(listOf(max(from, m1), max(m1, m2), max(m2, m3)), rows.map { it.maxBpm })
        assertEquals(listOf("80 kg × 10"), rows.map { it.set }.distinct())

        val noFact = workoutPicture(gym, hrFacts(gym, null, zone), logs, emptyList(), zone)!!.rows
        assertEquals(rows.map { it.copy(maxBpm = null, spoken = "") }, noFact.map { it.copy(spoken = "") })

        val sparse = listOf(HrSample(9 * 60, 140), HrSample(11 * 60, 150), HrSample(11 * 60 + 5, 151), HrSample(13 * 60, 160))
        assertEquals(listOf(null, 151, null), workoutPicture(gym, hrFacts(gym, row(sparse), zone), logs, emptyList(), zone)!!.rows.map { it.maxBpm })

        assertTrue(workoutPicture(gym, hrFacts(gym, row(buckets), zone), logs.take(1), emptyList(), zone)!!.rows.isEmpty())
    }

    @Test fun `round bands start inside the range and are cut at its end`() {
        val c = Session("c", day.iso(), from, from + 40 * MIN, ItemType.CARDIO, exerciseName = "4×4", timedFrom = from, timedTo = from + 30 * MIN)
        val r = hrWindow(c, zone)!!
        val four = (0 until 4).map { IntervalLog("r$it", "c", it, startedAt = from + (5 + it * 6) * MIN, endedAt = from + (9 + it * 6) * MIN) }
        assertEquals((0 until 4).map { RoundBand((5 + it * 6) * MIN, (9 + it * 6) * MIN) }, roundBands(r, four))
        val long = IntervalLog("long", "c", 4, startedAt = from + 29 * MIN, endedAt = from + 45 * MIN)
        val noTimes = IntervalLog("none", "c", 5)
        val zero = IntervalLog("zero", "c", 6, startedAt = from + 10 * MIN, endedAt = from + 10 * MIN)
        val bands = roundBands(r, four + long + noTimes + zero)
        assertEquals(5, bands.size)
        assertEquals(RoundBand(29 * MIN, r.lengthMs), bands.last())
        val p = workoutPicture(c, hrFacts(c, filledRow("c", r, every5(0, 600)), zone), emptyList(), four + long + noTimes + zero, zone)!!
        assertEquals(bands, p.bands)
        assertTrue(p.marks.isEmpty() && p.rows.isEmpty())
    }

    @Test fun `the bpm axis has 3 to 5 lines in steps of 10 or 20 over every bucket`() {
        fun check(buckets: List<HrSample>): BpmAxis {
            val a = bpmAxis(buckets)!!
            assertTrue("$a", a.lines.size in 3..5)
            assertTrue("$a", a.step == 10 || a.step == 20)
            assertTrue("$a", a.low <= buckets.minOf { it.bpm } && a.high >= buckets.maxOf { it.bpm } && a.high > a.low)
            return a
        }
        assertEquals(BpmAxis(90, 170, 20), check(listOf(HrSample(0, 92), HrSample(5, 164))))
        assertEquals(BpmAxis(110, 130, 10), check(listOf(HrSample(0, 120), HrSample(5, 120))))
        check(listOf(HrSample(0, 120)))
        check(listOf(HrSample(0, 120), HrSample(5, 128)))
        check(listOf(HrSample(0, 100), HrSample(5, 180)))
        for (lo in 40..150 step 7) for (span in 0..(80 - lo % 10) step 3) check(listOf(HrSample(0, lo), HrSample(5, lo + span)))
        assertNull(bpmAxis(emptyList()))
        val wide = bpmAxis(listOf(HrSample(0, 60), HrSample(5, 190)))!!
        assertTrue("$wide", wide.lines.size in 3..5 && wide.low <= 60 && wide.high >= 190)
    }

    @Test fun `the chart and a set row speak in full unit words`() {
        val f = gymFixture(day, zone)
        val p = workoutPicture(f.session, hrFacts(f.session, f.row, zone), f.logs, emptyList(), zone)!!
        assertEquals("Heart rate from 18:02 to 19:14, lowest 92 beats per minute, highest 164 beats per minute, 18 sets marked", p.spoken)
        val second = p.rows[1]
        assertEquals("Barbell Bench Press, 62.5 kilograms, 9 reps, checked at 18:09, 3 minutes 50 seconds since the set before, highest ${second.maxBpm} beats per minute", second.spoken)
        assertEquals("Barbell Bench Press, 60 kilograms, 10 reps, checked at 18:06, highest ${p.rows[0].maxBpm} beats per minute", p.rows[0].spoken)
        assertEquals("2 minutes 5 seconds", spokenMinSec(125))
        assertEquals("45 seconds", spokenMinSec(45))
        assertEquals("1 minute", spokenMinSec(60))
        val c = cardioFixture(day, zone)
        val cp = workoutPicture(c.session, hrFacts(c.session, c.row, zone), emptyList(), c.rounds, zone)!!
        assertTrue(cp.spoken, cp.spoken.startsWith("Heart rate from 18:00 to 18:38, lowest ") && cp.spoken.endsWith(", 4 rounds marked"))
    }
}
