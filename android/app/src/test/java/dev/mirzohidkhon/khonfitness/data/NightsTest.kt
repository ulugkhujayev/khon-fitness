package dev.mirzohidkhon.khonfitness.data

import dev.mirzohidkhon.khonfitness.health.AWAKE_STAGES
import androidx.health.connect.client.records.SleepSessionRecord
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NightsTest {
    private fun day(m: Int, d: Int) = LocalDate.of(2026, m, d)
    private fun t(d: LocalDate, h: Int, m: Int, s: Int = 0, zone: ZoneId = TEST_ZONE) =
        d.atTime(h, m, s).atZone(zone).toInstant().toEpochMilli()
    private fun span(from: Long, to: Long, origin: String = "com.mi.health", awake: List<TimeSpan> = emptyList()) = SleepSpan(origin, from, to, awake)
    private fun hm(h: Int, m: Int) = h * 3600 + m * 60

    @Test fun `D2 a night per day, a nap, a reversed span, and a span over 24 hours`() {
        val spans = listOf(
            span(t(day(11, 2), 23, 10), t(day(11, 3), 6, 15)),
            span(t(day(11, 3), 14, 0), t(day(11, 3), 15, 0)),
            span(t(day(11, 4), 4, 0), t(day(11, 4), 12, 30)),
            span(t(day(11, 7), 7, 0), t(day(11, 6), 23, 0)),
            span(t(day(11, 8), 20, 0), t(day(11, 9), 21, 0)),
        )
        val nights = nights(spans, TEST_ZONE)
        assertEquals(listOf(day(11, 3), day(11, 4)), nights.keys.toList())
        assertEquals(hm(7, 5), nights.getValue(day(11, 3)).asleepSec)
        assertEquals(hm(8, 30), nights.getValue(day(11, 4)).asleepSec)
    }

    @Test fun `D2 a span of exactly 24 hours is kept`() {
        val nights = nights(listOf(span(t(day(11, 2), 20, 0), t(day(11, 3), 20, 0))), TEST_ZONE)
        assertEquals(24 * 3600, nights.getValue(day(11, 3)).asleepSec)
    }

    @Test fun `D3 a midpoint at 18 00 belongs to the next day, one at 12 00 to no night`() {
        val d = day(11, 3)
        val prev = d.minusDays(1)
        fun only(from: Long, to: Long) = nights(listOf(span(from, to)), TEST_ZONE).keys.toList()
        assertEquals(listOf(d), only(t(prev, 17, 0), t(prev, 19, 0)))
        assertEquals(emptyList<LocalDate>(), only(t(d, 11, 0), t(d, 13, 0)))
        assertEquals(listOf(d), only(t(d, 11, 0), t(d, 12, 59, 58)))
        assertEquals(emptyList<LocalDate>(), only(t(prev, 17, 0), t(prev, 18, 59, 58)))
    }

    @Test fun `D3 the window holds 18 00 and not 12 00`() {
        val w = nightWindow(day(11, 3), TEST_ZONE)
        assertEquals(t(day(11, 2), 18, 0), w.startMs)
        assertEquals(t(day(11, 3), 12, 0), w.endMs)
        assertEquals(day(11, 3), nightOf(w.startMs, TEST_ZONE))
        assertNull(nightOf(w.endMs, TEST_ZONE))
        assertEquals(day(11, 3), nightOf(w.endMs - 1, TEST_ZONE))
        assertNull(nightOf(w.startMs - 1, TEST_ZONE))
    }

    @Test fun `D4 overlapping spans count once and awake stages are clipped to their span`() {
        val prev = day(11, 2); val d = day(11, 3)
        val awake = TimeSpan(t(d, 1, 30), t(d, 1, 45))
        val spans = listOf(
            span(t(prev, 23, 0), t(d, 2, 0), awake = listOf(awake)),
            span(t(d, 1, 0), t(d, 6, 0), awake = listOf(awake, TimeSpan(t(d, 5, 50), t(d, 6, 10)))),
        )
        val n = nights(spans, TEST_ZONE).getValue(d)
        assertEquals(hm(6, 35), n.asleepSec)
        assertEquals(t(prev, 23, 0), n.fromMs)
        assertEquals(t(d, 6, 0), n.toMs)
    }

    @Test fun `D5 the origin with the most asleep time is kept and origins are never added`() {
        val prev = day(11, 2); val d = day(11, 3)
        val spans = listOf(
            span(t(prev, 23, 0), t(d, 6, 0), origin = "b.band"),
            span(t(prev, 23, 30), t(d, 6, 0), origin = "a.watch"),
        )
        val n = nights(spans, TEST_ZONE).getValue(d)
        assertEquals(hm(7, 0), n.asleepSec)
        assertEquals(t(prev, 23, 0), n.fromMs)
    }

    @Test fun `D5 a tie goes to the origin whose package sorts first`() {
        val prev = day(11, 2); val d = day(11, 3)
        val spans = listOf(
            span(t(prev, 23, 30), t(d, 6, 30), origin = "z.band"),
            span(t(prev, 23, 0), t(d, 6, 0), origin = "m.band"),
        )
        assertEquals(t(prev, 23, 0), nights(spans, TEST_ZONE).getValue(d).fromMs)
        assertEquals(t(prev, 23, 30), nights(spans.map { if (it.origin == "z.band") it.copy(origin = "a.band") else it }, TEST_ZONE).getValue(d).fromMs)
    }

    @Test fun `D5 a night under one minute asleep is dropped`() {
        val d = day(11, 3)
        assertTrue(nights(listOf(span(t(d, 3, 0), t(d, 3, 0, 40))), TEST_ZONE).isEmpty())
        assertEquals(60, nights(listOf(span(t(d, 3, 0), t(d, 3, 1))), TEST_ZONE).getValue(d).asleepSec)
    }

    @Test fun `D6 daylight saving days take their window from local wall times and lengths from instants`() {
        val ny = ZoneId.of("America/New_York")
        val spring = LocalDate.of(2026, 3, 8)
        val fall = LocalDate.of(2026, 11, 1)
        val spans = listOf(
            span(t(spring.minusDays(1), 22, 0, zone = ny), t(spring, 7, 0, zone = ny)),
            span(t(fall.minusDays(1), 22, 0, zone = ny), t(fall, 7, 0, zone = ny)),
        )
        val nights = nights(spans, ny)
        assertEquals(listOf(spring, fall), nights.keys.toList())
        assertEquals(hm(8, 0), nights.getValue(spring).asleepSec)
        assertEquals(hm(10, 0), nights.getValue(fall).asleepSec)
    }

    @Test fun `D12 only the three awake stage types become awake time`() {
        val d = day(11, 3)
        val start = t(d, 0, 0)
        val types = listOf(
            SleepSessionRecord.STAGE_TYPE_AWAKE, SleepSessionRecord.STAGE_TYPE_LIGHT, SleepSessionRecord.STAGE_TYPE_DEEP,
            SleepSessionRecord.STAGE_TYPE_REM, SleepSessionRecord.STAGE_TYPE_OUT_OF_BED, SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
            SleepSessionRecord.STAGE_TYPE_UNKNOWN, SleepSessionRecord.STAGE_TYPE_SLEEPING,
        )
        val stages = types.mapIndexed { i, type -> SleepStage(type, start + i * 3_600_000L, start + (i + 1) * 3_600_000L) }
        val s = sleepSpan("com.mi.health", start, start + 8 * 3_600_000L, stages, AWAKE_STAGES)
        assertEquals(listOf(TimeSpan(start, start + 3_600_000L), TimeSpan(start + 4 * 3_600_000L, start + 5 * 3_600_000L), TimeSpan(start + 5 * 3_600_000L, start + 6 * 3_600_000L)), s.awake)
        assertEquals(hm(5, 0), nights(listOf(s), TEST_ZONE).getValue(d).asleepSec)
        val plain = sleepSpan("com.mi.health", start, start + 8 * 3_600_000L, emptyList(), AWAKE_STAGES)
        assertEquals(emptyList<TimeSpan>(), plain.awake)
        assertEquals(hm(8, 0), nights(listOf(plain), TEST_ZONE).getValue(d).asleepSec)
    }
}
