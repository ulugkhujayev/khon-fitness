package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** [hrWindow], [hrFacts] and [roundAverage]: one table row per rule and per bound. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HeartRateRulesTest {
    private val mon = LocalDate.of(2026, 11, 23)
    private val tue = mon.plusDays(1)
    private val start = at(18, 0, mon)

    private fun gym(minutes: Long, date: String = mon.iso(), finished: Boolean = true, from: Long? = start, lengthMs: Long = minutes * MIN) = Session(
        "g", date, start, finishedAt = if (finished) start + lengthMs + 2 * MIN else null, itemType = ItemType.PROGRAM,
        timedFrom = from, timedTo = from?.plus(lengthMs),
    )

    private fun cardio(lengthMs: Long, finishedAfterToMs: Long, imported: Boolean = false) = Session(
        "c", mon.iso(), start, finishedAt = start + lengthMs + finishedAfterToMs, itemType = ItemType.CARDIO, exerciseId = "row",
        sourceId = if (imported) "hc-1" else null, timedFrom = start, timedTo = start + lengthMs,
    )

    @Test fun `hrWindow table`() {
        val h = 60 * MIN
        val rows: List<Triple<String, Session, HrRange?>> = listOf(
            Triple("unfinished", gym(40, finished = false), null),
            Triple("no timedFrom", gym(40, from = null), null),
            Triple("no timedTo", gym(40).copy(timedTo = null), null),
            Triple("from equals to", gym(40).copy(timedTo = start), null),
            Triple("from after to", gym(40).copy(timedTo = start - MIN), null),
            Triple("date moved", gym(40, date = tue.plusDays(1).iso()), null),
            Triple("gym 9 min", gym(9), null),
            Triple("gym 10 min - 1 ms", gym(0, lengthMs = 10 * MIN - 1), null),
            Triple("gym 10 min", gym(10), HrRange(start, start + 10 * MIN)),
            Triple("gym 4 h", gym(240), HrRange(start, start + 4 * h)),
            Triple("gym 4 h + 1 ms", gym(0, lengthMs = 4 * h + 1), null),
            Triple("gym 4 h 1 min", gym(241), null),
            Triple("app cardio tail inside finishedAt", cardio(30 * MIN, 2 * MIN), HrRange(start, start + 31 * MIN)),
            Triple("app cardio tail cut at finishedAt", cardio(30 * MIN, 20_000), HrRange(start, start + 30 * MIN + 20_000)),
            Triple("imported cardio has no tail", cardio(30 * MIN, 2 * MIN, imported = true), HrRange(start, start + 30 * MIN)),
            Triple("cardio 2 min", cardio(2 * MIN, 0), HrRange(start, start + 2 * MIN)),
            Triple("cardio 2 min - 1 ms", cardio(2 * MIN - 1, 0), null),
            Triple("imported 2 min - 1 ms", cardio(2 * MIN - 1, MIN, imported = true), null),
            Triple("cardio 6 h", cardio(6 * h, 0), HrRange(start, start + 6 * h)),
            Triple("cardio 6 h + 1 ms", cardio(6 * h + 1, 0), null),
            Triple("cardio 6 h - 30 s with tail", cardio(6 * h - 30_000, 2 * MIN), null),
            Triple("imported 6 h + 1 ms", cardio(6 * h + 1, 0, imported = true), null),
            Triple("rest item", gym(40).copy(itemType = ItemType.REST), null),
        )
        for ((name, s, expected) in rows) assertEquals(name, expected, hrWindow(s, TEST_ZONE))
    }

    @Test fun `the date rule keeps a range across midnight on either day of the workout`() {
        val late = at(23, 30, mon)
        val crossing = Session("g", mon.iso(), late, finishedAt = late + 61 * MIN, itemType = ItemType.PROGRAM, timedFrom = late, timedTo = late + 60 * MIN)
        assertEquals(HrRange(late, late + 60 * MIN), hrWindow(crossing, TEST_ZONE))
        assertNull("the end's date is not a day of the start", hrWindow(crossing.copy(date = tue.iso()), TEST_ZONE))
        val made = at(23, 59, mon)
        val timer = at(0, 1, tue)
        val c = Session("c", mon.iso(), made, finishedAt = at(0, 22, tue), itemType = ItemType.CARDIO, timedFrom = timer, timedTo = at(0, 21, tue))
        assertEquals(HrRange(timer, at(0, 22, tue)), hrWindow(c, TEST_ZONE))
        assertEquals(HrRange(timer, at(0, 22, tue)), hrWindow(c.copy(date = tue.iso()), TEST_ZONE))
        assertNull(hrWindow(c.copy(date = tue.plusDays(1).iso()), TEST_ZONE))
        assertNull(hrWindow(c.copy(date = mon.minusDays(1).iso()), TEST_ZONE))
        assertNull("in Tokyo both instants fall on Tuesday", hrWindow(c, ZoneId.of("Asia/Tokyo")))
        assertEquals(HrRange(timer, at(0, 22, tue)), hrWindow(c.copy(date = tue.iso()), ZoneId.of("Asia/Tokyo")))
    }

    private val range = HrRange(start, start + 40 * MIN)
    private val session = gym(40)
    private fun row(state: String, from: Long = range.fromMs, to: Long = range.toMs, samples: String = "0:120,5:124") =
        SessionHr("g", from, to, state, samples, 12, 10, "com.mi.health", "com.mi.health", start, start + 30 * MIN, 60,
            avgHr = if (state == HrState.FILLED) 122 else null, maxHr = if (state == HrState.FILLED) 124 else null, fetchedAt = start + 50 * MIN)

    @Test fun `hrFacts table`() {
        assertEquals(range, hrWindow(session, TEST_ZONE))
        val rows: List<Triple<String, Pair<Session, SessionHr?>, HrFacts>> = listOf(
            Triple("no range with a filled row", session.copy(timedFrom = null) to row(HrState.FILLED), HrFacts.NoRange),
            Triple("no range with an off row", session.copy(finishedAt = null) to row(HrState.OFF), HrFacts.NoRange),
            Triple("off", session to row(HrState.OFF, samples = ""), HrFacts.Removed),
            Triple("off for an old range", session to row(HrState.OFF, from = start - MIN, samples = ""), HrFacts.Removed),
            Triple("filled", session to row(HrState.FILLED), HrFacts.Filled(row(HrState.FILLED))),
            Triple("waiting", session to row(HrState.WAITING), HrFacts.Waiting(row(HrState.WAITING))),
            Triple("none", session to row(HrState.NONE), HrFacts.None(row(HrState.NONE))),
            Triple("no row", session to null, HrFacts.NotRead),
            Triple("filled for another start", session to row(HrState.FILLED, from = start + MIN), HrFacts.NotRead),
            Triple("filled for another end", session to row(HrState.FILLED, to = range.toMs - 1), HrFacts.NotRead),
            Triple("filled of another session", session to row(HrState.FILLED).copy(sessionId = "x"), HrFacts.NotRead),
            Triple("unknown state", session to row("half"), HrFacts.NotRead),
        )
        for ((name, input, expected) in rows) assertEquals(name, expected, hrFacts(input.first, input.second, TEST_ZONE))
    }

    @Test fun `the round average is the half-up mean of the buckets inside the round, from a filled row only`() {
        val samples = "0:100,60:110,65:121,120:140,125:150"
        val filled = HrFacts.Filled(row(HrState.FILLED, samples = samples))
        fun round(fromSec: Int?, toSec: Int?) = IntervalLog("r", "g", 0, startedAt = fromSec?.let { start + it * 1000L }, endedAt = toSec?.let { start + it * 1000L })
        assertEquals("buckets at 60 and 65; 120 is at the end", 116, roundAverage(filled, round(60, 120)))
        assertEquals("half up: (110 + 121) / 2 = 115.5", 116, roundAverage(filled, round(60, 66)))
        assertEquals("the bucket at startedAt counts", 145, roundAverage(filled, round(120, 126)))
        assertNull("one bucket inside", roundAverage(filled, round(0, 60)))
        assertNull("no bucket", roundAverage(filled, round(200, 300)))
        assertNull("no times", roundAverage(filled, round(null, null)))
        assertNull("no end", roundAverage(filled, round(60, null)))
        assertNull("waiting row", roundAverage(HrFacts.Waiting(row(HrState.WAITING, samples = samples)), round(60, 130)))
        assertNull("none row", roundAverage(HrFacts.None(row(HrState.NONE, samples = samples)), round(60, 130)))
        assertNull("not read", roundAverage(HrFacts.NotRead, round(60, 130)))
        assertEquals(listOf(HrSample(0, 100), HrSample(60, 110)), parseSamples("0:100,60:110"))
        assertEquals(emptyList<HrSample>(), parseSamples(""))
        assertNull(parseSamples("0:100,x:1"))
        assertNull(parseSamples("0:100,5"))
        assertEquals(116, meanHalfUp(listOf(115, 116)))
        assertEquals(115, meanHalfUp(listOf(115, 115, 116)))
    }

    @Test fun `AT21 a session made before midnight and timed after it keeps its range on both days, not on a third`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        try {
            val dao = db.dao()
            val repo = KhonRepository(dao, clocks = FakeClocks(at(23, 59, mon)), zone = { TEST_ZONE })
            val made = at(23, 59, mon)
            dao.upsert(Session("c", mon.iso(), made, itemType = ItemType.CARDIO, exerciseId = "row", exerciseName = "Rowing"))
            dao.upsert(dao.session("c")!!.copy(timedFrom = at(0, 1, tue), timedTo = at(0, 21, tue), timeSec = 20 * 60))
            val opened = dao.session("c")!!
            repo.saveCardioEdits(opened, opened.copy(effort = 6), emptyMap(), now = at(0, 22, tue))
            val window = hrWindow(dao.session("c")!!, TEST_ZONE)!!
            assertEquals(HrRange(at(0, 1, tue), at(0, 22, tue)), window)
            val filled = SessionHr("c", window.fromMs, window.toMs, HrState.FILLED, "0:130,5:132", 240, 240, "mi", "mi", avgHr = 131, maxHr = 140, fetchedAt = at(1, 0, tue))
            dao.upsertSessionHr(filled)
            fun facts() = runBlocking { hrFacts(dao.session("c")!!, dao.sessionHr("c"), TEST_ZONE) }
            assertEquals(HrFacts.Filled(filled), facts())
            repo.setSessionDate("c", tue)
            assertEquals(HrFacts.Filled(filled), facts())
            repo.setSessionDate("c", tue.plusDays(1))
            assertEquals(HrFacts.NoRange, facts())
        } finally { db.close() }
    }
}
