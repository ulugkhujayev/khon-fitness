package dev.mirzohidkhon.khonfitness.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The reader as pure functions: records to a reading ([readHeart]), every page ([readAllPages]), a reading to a row ([hrRow]). */
class HeartReadTest {
    private val range = HrRange(at(18, 0), at(18, 30))
    private fun p(h: Int, m: Int, bpm: Int, s: Int = 0) = HeartPoint(at(h, m, second = s), bpm)

    @Test fun `C21 crossing records, bounds, bpm limits, duplicates, packages and one instant`() {
        val records = listOf(
            HeartRecord("com.a", listOf(p(17, 56, 100), p(18, 0, 100), p(18, 1, 29), p(18, 2, 30))),
            HeartRecord("com.a", listOf(p(18, 26, 230), p(18, 29, 231, 59), p(18, 30, 120), p(18, 33, 100))),
            HeartRecord("com.a", listOf(p(17, 51, 100), p(18, 10, 110), p(18, 10, 140), p(18, 39, 100))),
            HeartRecord("com.a", listOf(p(18, 10, 110))),
            HeartRecord("com.b", listOf(p(18, 5, 90), p(18, 6, 90))),
        )
        val r = readHeart(records, range)
        assertEquals(15, r.rawCount)
        assertEquals(4, r.sampleCount)
        assertEquals("com.a", r.origin)
        assertEquals(listOf("com.a", "com.b"), r.origins)
        assertEquals(at(18, 0), r.firstMs)
        assertEquals(at(18, 26), r.lastMs)
        assertEquals(16 * 60, r.maxGapSec)
        assertEquals(listOf(HrSample(0, 100), HrSample(120, 30), HrSample(600, 110), HrSample(1560, 230)), r.buckets)
        assertEquals(118, r.avgHr)
        assertEquals(230, r.maxHr)
        assertFalse(r.complete)
    }

    @Test fun `C21 the package with more accepted samples wins, a tie goes to the first by name`() {
        val more = readHeart(listOf(HeartRecord("com.z", listOf(p(18, 1, 100), p(18, 2, 100))), HeartRecord("com.a", listOf(p(18, 3, 90)))), range)
        assertEquals("com.z", more.origin)
        assertEquals(2, more.sampleCount)
        val tie = readHeart(listOf(HeartRecord("com.z", listOf(p(18, 1, 100))), HeartRecord("com.a", listOf(p(18, 3, 90)))), range)
        assertEquals("com.a", tie.origin)
        assertEquals(listOf(HrSample(180, 90)), tie.buckets)
        val none = readHeart(listOf(HeartRecord("com.z", listOf(p(17, 1, 100)))), range)
        assertNull(none.origin); assertNull(none.firstMs); assertNull(none.maxGapSec); assertNull(none.avgHr)
        assertEquals(1, none.rawCount); assertEquals(0, none.sampleCount); assertEquals(listOf("com.z"), none.origins)
    }

    @Test fun `C21 every page is read and a throw on page 2 fails the whole read`() = runBlocking {
        val pages = mapOf(null to HeartPage(listOf(HeartRecord("a", listOf(p(18, 1, 90)))), "2"), "2" to HeartPage(listOf(HeartRecord("a", listOf(p(18, 2, 91)))), ""))
        val asked = mutableListOf<String?>()
        val all = readAllPages { asked += it; pages.getValue(it) }
        assertEquals(listOf(null, "2"), asked)
        assertEquals(2, all.size)
        val failed = runCatching { readAllPages { if (it == null) HeartPage(emptyList(), "2") else error("page 2") } }
        assertTrue(failed.isFailure)
    }

    @Test fun `AT5 records that cross both bounds contribute, their outside samples count only as raw`() {
        val first = HeartRecord("mi", listOf(p(17, 59, 200)) + (0..11).map { p(18, it, 130) })
        val second = HeartRecord("mi", (18..29).map { p(18, it, 130) } + p(18, 30, 200))
        val r = readHeart(listOf(first, second), range)
        assertEquals(26, r.rawCount)
        assertEquals(24, r.sampleCount)
        assertTrue(r.complete)
        assertEquals(130, r.avgHr)
        assertEquals(130, r.maxHr)
        assertEquals(HrState.FILLED, hrRow("s", range, r, at(19, 0), null).state)
    }

    @Test fun `C22 only ten instants over half of the range are filled`() {
        val forty = HrRange(at(18, 0), at(18, 40))
        fun row(points: List<HeartPoint>) = hrRow("s", forty, readHeart(listOf(HeartRecord("mi", points)), forty), at(19, 0), null)
        val identical = row(List(10) { p(18, 5, 120) })
        val oneMinute = row((0 until 10).map { p(18, 5, 120, s = it * 5) })
        val twenty = row(listOf(p(18, 5, 120)) + (6..13).map { p(18, it, 130) } + p(18, 25, 140))
        val justUnder = row(listOf(p(18, 5, 120, s = 1)) + (6..13).map { p(18, it, 130) } + p(18, 25, 140))
        val nine = row((0 until 9).map { p(18, it * 4, 120) })
        assertEquals(HrState.FILLED, twenty.state)
        assertEquals(130, twenty.avgHr)
        assertEquals(140, twenty.maxHr)
        for (r in listOf(identical, oneMinute, justUnder, nine)) {
            assertEquals(HrState.WAITING, r.state)
            assertNull(r.avgHr); assertNull(r.maxHr)
            assertTrue(r.samples.isNotEmpty())
            assertTrue(r.sampleCount > 0)
        }
        assertEquals(1, identical.sampleCount)
        assertEquals(10, oneMinute.sampleCount)
        assertEquals(9, nine.sampleCount)
    }

    @Test fun `a row after a waiting row has a later fetchedAt also when the clock did not move`() {
        val r = readHeart(emptyList(), range)
        val old = hrRow("s", range, r, at(19, 0), null)
        assertEquals(at(19, 0), old.fetchedAt)
        assertEquals(at(19, 0) + 1, hrRow("s", range, r, at(19, 0), old).fetchedAt)
        assertEquals(at(20, 0), hrRow("s", range, r, at(20, 0), old).fetchedAt)
    }
}
