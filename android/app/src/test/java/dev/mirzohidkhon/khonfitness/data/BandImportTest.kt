package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.health.HealthImport
import dev.mirzohidkhon.khonfitness.health.ImportDraft
import java.time.Instant
import java.time.LocalDate
import java.util.TimeZone
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The band import against Room: the covered rule on each original record, the grouping after it, claimed ids, the one
 * read transaction of the list and the count, and the write that checks again, rolls back, and asks for a fill.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BandImportTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository
    private lateinit var clocks: FakeClocks
    private var fills = 0
    private val oldZone = TimeZone.getDefault()
    private val day = LocalDate.of(2026, 11, 20)
    private val run = Exercise("z2", "Zone 2 run", Kind.CARDIO, modalityId = "run")
    private val bike = Exercise("ride", "Easy ride", Kind.CARDIO, modalityId = "bike")
    private val plain = Exercise("open", "Open cardio", Kind.CARDIO)
    private val intervals = Exercise("hill", "Hill repeats", Kind.CARDIO, modalityId = "run", intervals = true, workSec = 240, restSec = 180, rounds = 4)

    @Before fun open() {
        TimeZone.setDefault(TimeZone.getTimeZone(TEST_ZONE))
        clocks = FakeClocks(at(19, 0))
        fills = 0
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        repo = newRepo()
        runBlocking { listOf(run, bike, plain, intervals).forEach { dao.upsert(it) } }
    }

    @After fun close() { db.close(); TimeZone.setDefault(oldZone) }

    private fun fresh() { close(); open() }

    private fun newRepo(fill: () -> Unit = { fills++ }) = KhonRepository(dao, clocks = clocks, zone = { TEST_ZONE }, requestFill = fill)

    private fun band(
        id: String, from: Long, to: Long, modality: String? = "run", type: String = "Run", strength: Boolean = false,
        samples: Int = 0, avg: Int? = null, origin: String = "com.mi.health",
    ) = ImportDraft(
        id, Instant.ofEpochMilli(from), Instant.ofEpochMilli(to), type, modality, null, avg,
        List(samples) { Instant.ofEpochMilli(from + it * 60_000L) to (avg ?: 120) }, listOf(origin), strength = strength,
    )

    /** An app cardio session with the run range `[from, to)`, finished a little after its timer unless [finished] is false. */
    private suspend fun appRun(
        id: String, from: Long?, to: Long?, ex: Exercise = run, finished: Boolean = true, startedAt: Long = from ?: at(18, 0),
        date: String = localDate(startedAt, TEST_ZONE), itemType: String = ItemType.CARDIO, sourceId: String? = null, linkedIds: String? = null,
    ) = dao.insertSessionWithLogs(
        Session(
            id, date, startedAt, finishedAt = if (finished) (to ?: startedAt) + 30_000L else null, itemType = itemType,
            exerciseId = ex.id, exerciseName = ex.name, timedFrom = from, timedTo = to, sourceId = sourceId, linkedIds = linkedIds,
        ),
        emptyList(), emptyList(),
    )

    private suspend fun list(records: List<ImportDraft>) = HealthImport.offered(records, repo.importLocal()).filter { !it.strength }
    private suspend fun count(records: List<ImportDraft>) = HealthImport.countOffered(records, repo.importLocal())
    private suspend fun ids(records: List<ImportDraft>) = list(records).map { it.mergedIds.sorted() }.toSet()
    private suspend fun imported() = dao.sessionsOnce().filter { it.sourceId != null }

    private fun <T> permutations(items: List<T>): List<List<T>> =
        if (items.size <= 1) listOf(items) else items.flatMap { x -> permutations(items - x).map { listOf(x) + it } }

    private val record = band("b1", at(18, 0), at(18, 40))

    @Test fun `D1 a band run that an app run overlaps by 36 of its 40 minutes is covered, finished or not`() = runBlocking {
        for (finished in listOf(true, false)) {
            fresh()
            appRun("app", at(18, 4), at(18, 44), finished = finished)
            assertEquals(emptyList<ImportDraft>(), list(listOf(record)))
            assertEquals(0, count(listOf(record)))
            assertEquals(ImportResult.COVERED, repo.importDraft(record, run))
            assertEquals(emptyList<Session>(), imported())
            assertEquals(0, fills)
        }
    }

    @Test fun `D1a a run made at 23 59 whose timer runs after midnight keeps its range and covers the band run`() = runBlocking {
        val next = day.plusDays(1)
        appRun("app", at(0, 1, next), at(0, 21, next), startedAt = at(23, 59), date = day.iso())
        val late = band("b1", at(0, 0, next), at(0, 22, next))
        assertEquals(HrRange(at(0, 1, next), at(0, 21, next)), runRange(dao.session("app")!!, TEST_ZONE))
        assertEquals(emptyList<ImportDraft>(), list(listOf(late)))
        assertEquals(0, count(listOf(late)))
        assertEquals(ImportResult.COVERED, repo.importDraft(late, run))
    }

    @Test fun `D2 a gym session, a bike session, a run without a range and an imported run never cover a band run`() = runBlocking {
        val fixtures: List<suspend () -> Unit> = listOf(
            { appRun("gym", at(17, 50), at(18, 50), itemType = ItemType.PROGRAM) },
            { appRun("ride", at(18, 0), at(18, 40), ex = bike) },
            { appRun("plain", null, null) },
            { appRun("old", at(18, 0), at(18, 40), sourceId = "elsewhere") },
        )
        for (fixture in fixtures) {
            fresh()
            fixture()
            assertEquals(setOf(listOf("b1")), ids(listOf(record)))
            assertEquals(1, count(listOf(record)))
            assertEquals(ImportResult.SAVED, repo.importDraft(record, run))
        }
    }

    @Test fun `D3 AT08 AT08a one eligible app run must overlap at least half of both ranges`() = runBlocking {
        fun r(id: String, from: Long, to: Long, m: String? = "run") = BandRecord(id, from, to, m)
        fun span(from: Long, to: Long, m: String = "run") = RunSpan(HrRange(from, to), m)
        val b = r("b", at(18, 0), at(18, 40))
        assertTrue(covered(b, listOf(span(at(18, 20), at(19, 0)))))
        assertFalse(covered(b, listOf(span(at(18, 20), at(19, 10)))))
        assertFalse(covered(b, listOf(span(at(18, 21), at(19, 1)))))
        assertFalse(covered(r("short", at(18, 30), at(18, 32)), listOf(span(at(18, 0), at(19, 0)))))
        assertFalse(covered(b, listOf(span(at(18, 40), at(19, 20)))))
        assertFalse(covered(b, listOf(span(at(17, 20), at(18, 0)))))
        assertFalse(covered(b, listOf(span(at(18, 0), at(18, 15)), span(at(18, 20), at(18, 35)))))
        assertFalse(covered(b, listOf(span(at(18, 0), at(18, 40), "bike"))))
        assertFalse(covered(r("u", at(18, 0), at(18, 40), null), listOf(span(at(18, 0), at(18, 40)))))
        assertTrue(covered(r("m", at(23, 50), at(0, 20, day.plusDays(1))), listOf(span(at(23, 52), at(0, 22, day.plusDays(1))))))

        data class Case(val name: String, val record: ImportDraft, val covered: Boolean, val runs: List<Pair<Long, Long>>)
        val cases = listOf(
            Case("half of both", record, true, listOf(at(18, 20) to at(19, 0))),
            Case("half of the record, 40 % of the run", record, false, listOf(at(18, 20) to at(19, 10))),
            Case("one side below half", record, false, listOf(at(18, 21) to at(19, 1))),
            Case("2 minutes inside 60", band("b1", at(18, 30), at(18, 32)), false, listOf(at(18, 0) to at(19, 0))),
            Case("touching endpoints", record, false, listOf(at(18, 40) to at(19, 20))),
            Case("two runs together", record, false, listOf(at(18, 0) to at(18, 15), at(18, 20) to at(18, 35))),
            Case("across midnight", band("b1", at(23, 50), at(0, 20, day.plusDays(1))), true, listOf(at(23, 52) to at(0, 22, day.plusDays(1)))),
        )
        for (c in cases) {
            fresh()
            c.runs.forEachIndexed { i, (from, to) -> appRun("app$i", from, to) }
            val offered = if (c.covered) 0 else 1
            assertEquals(c.name, offered, list(listOf(c.record)).size)
            assertEquals(c.name, offered, count(listOf(c.record)))
            assertEquals(c.name, if (c.covered) ImportResult.COVERED else ImportResult.SAVED, repo.importDraft(c.record, run))
        }

        fresh()
        appRun("open", at(18, 0), at(18, 40), ex = plain)
        val unknown = band("w1", at(18, 0), at(18, 40), modality = null, type = "Workout")
        assertEquals(setOf(listOf("w1")), ids(listOf(unknown)))
        assertEquals(1, count(listOf(unknown)))
        val strength = band("s1", at(18, 0), at(18, 40), modality = null, type = "Strength", strength = true)
        assertEquals(emptyList<ImportDraft>(), list(listOf(strength)))
        assertEquals(0, count(listOf(strength)))
    }

    @Test fun `D5 covered records leave before grouping and the union never hides a group`() = runBlocking {
        appRun("app", at(18, 0), at(18, 30))
        val watch = band("a", at(18, 0), at(18, 40), origin = "com.mi.health")
        val phone = band("b", at(18, 10), at(19, 10), origin = "com.garmin.android.apps.connectmobile")
        for (order in permutations(listOf(watch, phone))) {
            assertEquals(setOf(listOf("b")), ids(order))
            assertEquals(1, count(order))
        }

        fresh()
        appRun("app", at(18, 0), at(18, 40))
        val first = band("u1", at(18, 0), at(18, 15))
        val second = band("u2", at(18, 10), at(18, 26))
        for (order in permutations(listOf(first, second))) {
            assertEquals(setOf(listOf("u1", "u2")), ids(order))
            assertEquals(1, count(order))
        }
    }

    @Test fun `AT03 a chain groups the same in every input order, and a grouped import stores its outer bounds`() = runBlocking {
        val chain = listOf(band("c1", at(18, 10), at(18, 35)), band("c2", at(18, 20), at(18, 25)), band("c3", at(18, 30), at(18, 40)))
        for (order in permutations(chain)) {
            assertEquals(setOf(listOf("c1", "c2", "c3")), ids(order))
            assertEquals(list(order).size, count(order))
        }
        val tied = listOf(band("t1", at(18, 10), at(18, 20)), band("t2", at(18, 10), at(18, 30)))
        for (order in permutations(tied)) {
            assertEquals(setOf(listOf("t1", "t2")), ids(order))
            assertEquals(1, count(order))
        }

        appRun("app", at(18, 0), at(18, 40))
        val union = listOf(band("u1", at(18, 0), at(18, 15)), band("u2", at(18, 10), at(18, 26)))
        val draft = list(union).single()
        assertEquals(ImportResult.SAVED, repo.importDraft(draft, run))
        val s = imported().single()
        assertEquals(at(18, 0) to at(18, 26), s.timedFrom to s.timedTo)
        assertEquals(at(18, 0) to at(18, 26), s.startedAt to s.finishedAt)
        assertEquals(setOf("u1", "u2"), s.sourceId!!.split(",").toSet())
    }

    @Test fun `AT03a a tied-start bridge and a tied pair give the same group and the same average in both orders`() = runBlocking {
        val bridge = listOf(
            band("r1", at(18, 30), at(18, 40)), band("r2", at(18, 20), at(18, 25)),
            band("t1", at(18, 10), at(18, 22)), band("t2", at(18, 10), at(18, 35)),
        )
        val swapped = listOf(bridge[0], bridge[1], bridge[3], bridge[2])
        for (order in listOf(bridge, swapped, bridge.reversed(), swapped.reversed())) {
            assertEquals(setOf(listOf("r1", "r2", "t1", "t2")), ids(order))
            assertEquals(list(order).size, count(order))
        }
        for (pair in listOf(
            listOf(band("p1", at(18, 10), at(18, 30), samples = 5, avg = 120), band("p2", at(18, 10), at(18, 40), samples = 5, avg = 140)),
            listOf(band("q1", at(18, 10), at(18, 30), samples = 5, avg = 120), band("q2", at(18, 10), at(18, 30), samples = 5, avg = 140)),
        )) {
            val averages = permutations(pair).map { list(it).single().avgHr }.toSet()
            assertEquals(1, averages.size)
            assertEquals(120, averages.single())
        }
    }

    @Test fun `D6 AT04 a linked id counts as imported, as a whole id, and its claim stays`() = runBlocking {
        val group = listOf(band("id1", at(18, 0), at(18, 40)), band("id10", at(18, 5), at(18, 45)))
        for (claim in listOf<suspend () -> Unit>(
            { appRun("other", at(9, 0), at(9, 30), linkedIds = "x,id1") },
            { appRun("old", at(9, 0), at(9, 30), sourceId = "id1") },
        )) {
            fresh()
            val old = list(group).single()
            assertEquals(listOf("id1", "id10"), old.mergedIds.sorted())
            claim()
            val before = dao.sessionsOnce()
            assertEquals(ImportResult.ALREADY_IMPORTED, repo.importDraft(old, run))
            assertEquals(before, dao.sessionsOnce())
            assertEquals(0, fills)
            val offered = list(group).single()
            assertEquals(listOf("id10"), offered.mergedIds)
            assertEquals(1, count(group))
            assertEquals(ImportResult.SAVED, repo.importDraft(offered, run))
            assertEquals(ImportResult.ALREADY_IMPORTED, repo.importDraft(offered, run))
            assertEquals(before.toSet(), dao.sessionsOnce().filter { it.sourceId != "id10" }.toSet())
            assertEquals(1, dao.sessionsOnce().count { it.sourceId == "id10" })
            assertEquals(emptyList<ImportDraft>(), list(group))
        }
        fresh()
        appRun("spaced", at(9, 0), at(9, 30), linkedIds = " id100 , id1 ")
        assertEquals(setOf(listOf("id10")), ids(group))
    }

    /**
     * The list read held between its two queries while [write] commits one Room transaction; the read is let go
     * without waiting for the write, which Room queues behind it.
     */
    private suspend fun heldRead(records: List<ImportDraft>, write: suspend () -> Unit, read: suspend (List<ImportDraft>) -> Int): Pair<Int, Int> {
        val reached = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        repo.betweenImportReads = { reached.complete(Unit); release.await() }
        val reader = CoroutineScope(Dispatchers.IO).async { read(records) }
        withTimeout(5_000) { reached.await() }
        repo.betweenImportReads = {}
        val writer = CoroutineScope(Dispatchers.IO).async { db.withTransaction { write() } }
        withTimeoutOrNull(500) { writer.await() }
        release.complete(Unit)
        val during = withTimeout(5_000) { reader.await() }
        withTimeout(5_000) { writer.await() }
        return during to read(records)
    }

    @Test fun `D6a AT12 a read held between its inputs gives one committed state, never a mix`() = runBlocking {
        val readers: List<suspend (List<ImportDraft>) -> Int> = listOf({ list(it).size }, { count(it) })
        for (read in readers) {
            fresh()
            appRun("app", at(18, 0), at(18, 40))
            val replace: suspend () -> Unit = {
                dao.deleteSessionWithLogs("app")
                appRun("imp", at(18, 0), at(18, 40), sourceId = "b1")
            }
            assertEquals(0 to 0, heldRead(listOf(record), replace, read))
        }
    }

    @Test fun `AT12a a session and its exercise changed in one transaction never mix an old modality with a new range`() = runBlocking {
        val readers: List<suspend (List<ImportDraft>) -> Int> = listOf({ list(it).size }, { count(it) })
        for (read in readers) {
            fresh()
            val shared = Exercise("x", "Morning cardio", Kind.CARDIO, modalityId = "run")
            dao.upsert(shared)
            appRun("x", at(19, 0), at(19, 40), ex = shared)
            val change: suspend () -> Unit = {
                dao.upsert(shared.copy(modalityId = "bike"))
                dao.upsert(dao.session("x")!!.copy(timedFrom = at(18, 0), timedTo = at(18, 40)))
            }
            assertEquals(1 to 1, heldRead(listOf(record), change, read))
            assertEquals(0, fills)
            assertEquals(emptyList<Session>(), imported())
        }
    }

    private fun failRoundsOf(sourceId: String) = db.openHelper.writableDatabase.execSQL(
        "CREATE TRIGGER fail_rounds BEFORE INSERT ON interval_logs WHEN (SELECT sourceId FROM sessions WHERE id = NEW.sessionId) = '$sourceId' " +
            "BEGIN SELECT RAISE(ABORT, 'held'); END",
    )

    @Test fun `D7a AT09 a failed round insert leaves nothing of its draft and the batch goes on`() = runBlocking {
        val bad = band("bad", at(10, 0), at(10, 30))
        val one = band("ok1", at(12, 0), at(12, 30))
        val two = band("ok2", at(14, 0), at(14, 30))
        val all = listOf(bad, one, two)
        for (batch in listOf(listOf(bad), all)) {
            fresh()
            failRoundsOf("bad")
            val results = HealthImport.importEach(list(all).filter { it.sourceId in batch.map { b -> b.sourceId } }.sortedBy { it.start }.map { it to intervals }, repo::importDraft)
            assertEquals(listOf(null) + List(batch.size - 1) { ImportResult.SAVED }, results)
            assertEquals(batch.size - 1, fills)
            assertTrue(dao.sessionsOnce().none { it.sourceId == "bad" })
            assertEquals((batch.size - 1) * 4, dao.intervalLogsOnce().size)
            val again = list(all)
            assertTrue(again.any { it.sourceId == "bad" })
            assertEquals(again.size, count(all))
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_rounds")
            assertEquals(listOf<ImportResult?>(ImportResult.SAVED), HealthImport.importEach(listOf(again.single { it.sourceId == "bad" } to intervals), repo::importDraft))
            assertEquals(1, dao.sessionsOnce().count { it.sourceId == "bad" })
            assertEquals(batch.size * 4, dao.intervalLogsOnce().size)
            assertEquals(batch.size, fills)
        }
    }

    @Test fun `AT02 an old group with a member covered since returns covered, and the refresh offers the other member`() = runBlocking {
        val group = listOf(band("g1", at(18, 0), at(18, 20)), band("g2", at(18, 15), at(18, 45)))
        val old = list(group).single()
        assertEquals(listOf("g1", "g2"), old.mergedIds.sorted())
        appRun("app", at(18, 0), at(18, 20))
        assertEquals(ImportResult.COVERED, repo.importDraft(old, run))
        assertEquals(emptyList<Session>(), imported())
        val fresh = list(group).single()
        assertEquals(listOf("g2"), fresh.mergedIds)
        assertEquals(1, count(group))
        assertEquals(ImportResult.SAVED, repo.importDraft(fresh, run))
        val s = imported().single()
        assertEquals("g2", s.sourceId)
        assertEquals(at(18, 15) to at(18, 45), s.timedFrom to s.timedTo)
        assertEquals(1, fills)
    }

    @Test fun `AT05 Import all writes nothing for a stale draft, replays are already imported, and two submits save once`() = runBlocking {
        val records = listOf(band("m", at(10, 0), at(10, 30)), band("n", at(12, 0), at(12, 30)), band("e", at(14, 0), at(14, 30)))
        val shown = list(records)
        assertEquals(3, shown.size)
        appRun("app", at(12, 0), at(12, 30))
        val items = shown.sortedBy { it.start }.map { it to run }
        assertEquals(listOf(ImportResult.SAVED, ImportResult.COVERED, ImportResult.SAVED), HealthImport.importEach(items, repo::importDraft))
        assertEquals(setOf("m" to (at(10, 0) to at(10, 30)), "e" to (at(14, 0) to at(14, 30))), imported().map { it.sourceId to (it.timedFrom to it.timedTo) }.toSet())
        assertEquals(emptyList<ImportDraft>(), list(records))
        assertEquals(0, count(records))
        val saved = listOf(items[0], items[2])
        assertEquals(listOf(ImportResult.ALREADY_IMPORTED, ImportResult.ALREADY_IMPORTED), HealthImport.importEach(saved, repo::importDraft))
        assertEquals(2, fills)

        fresh()
        val d = list(records).first()
        val both = listOf(async(Dispatchers.IO) { repo.importDraft(d, run) }, async(Dispatchers.IO) { repo.importDraft(d, run) }).map { it.await() }
        assertEquals(listOf(ImportResult.ALREADY_IMPORTED, ImportResult.SAVED), both.sortedBy { it.name })
        assertEquals(1, imported().size)
        assertEquals(1, fills)
    }

    @Test fun `AT10 an import whose fill never ran keeps its id and bounds, a later fill reads them, and a replay asks nothing`() = runBlocking {
        val dead = newRepo(fill = {})
        assertEquals(ImportResult.SAVED, dead.importDraft(record, run))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            repo = newRepo()
            val heart = FakeHeart { range -> everyMinute(range) }
            val fill = repo.heartFill(heart, scope)
            fill.fillHeartRate()
            withTimeout(5_000) { fill.passes.first { it >= 1 } }
            val s = imported().single()
            assertEquals("b1", s.sourceId)
            assertEquals(HrRange(at(18, 0), at(18, 40)), hrWindow(s, TEST_ZONE))
            val row = dao.sessionHr(s.id)!!
            assertEquals(HrState.FILLED to (at(18, 0) to at(18, 40)), row.state to (row.fromMs to row.toMs))
            assertEquals(ImportResult.ALREADY_IMPORTED, repo.importDraft(record, run))
            assertEquals(1, imported().size)
            assertEquals(0, fills)
        } finally {
            scope.cancel()
        }
    }

    @Test fun `AT11 an archived exercise still gives its run a modality, and a date move takes the range away`() = runBlocking {
        appRun("app", at(18, 0), at(18, 40))
        assertEquals(0, list(listOf(record)).size + count(listOf(record)))
        dao.upsert(run.copy(archived = true))
        assertEquals(0, list(listOf(record)).size + count(listOf(record)))
        dao.upsert(dao.session("app")!!.copy(date = "2026-11-25"))
        assertNull(runRange(dao.session("app")!!, TEST_ZONE))
        assertEquals(setOf(listOf("b1")), ids(listOf(record)))
        assertEquals(1, count(listOf(record)))
    }

    @Test fun `AT24 a saved import asks one fill after its commit and a refused repeat asks none`() = runBlocking {
        assertEquals(ImportResult.SAVED, repo.importDraft(record, run))
        assertEquals(1, fills)
        assertEquals(ImportResult.ALREADY_IMPORTED, repo.importDraft(record, run))
        assertEquals(1, fills)
        repo.fillHeartRate()
        assertEquals(2, fills)
    }
}
