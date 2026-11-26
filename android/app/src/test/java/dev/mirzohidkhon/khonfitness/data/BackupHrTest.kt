package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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
 * Backup export and import of the clock columns and `session_hr`: what a file may bring, the anchor that never comes
 * from a file, and Merge where the local unit wins and what no longer fits is cleared.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BackupHrTest {
    /** One database with its repository, its fill over [heart], and the clocks of both. */
    inner class Phone {
        val db: KhonDatabase = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        val dao: KhonDao = db.dao()
        val clocks = FakeClocks(at(19, 0))
        val repo = KhonRepository(dao, clocks = clocks, zone = { TEST_ZONE })
        val heart = FakeHeart { everyMinute(it) }
        val fill = HeartFill(dao, heart, scope, { repo.generation }, clocks, { TEST_ZONE })
        init { phones += this }
    }

    private val phones = mutableListOf<Phone>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var local: Phone
    private val json = Json { ignoreUnknownKeys = true }
    private val gymRange = HrRange(at(18, 0), at(18, 40))

    @Before fun open() { local = Phone() }
    @After fun close() { phones.forEach { it.db.close() }; phones.clear() }
    @After fun stop() = scope.cancel()

    private fun gym(id: String = "g") = Session(id, "2026-11-20", at(18, 0), finishedAt = at(18, 40), itemType = ItemType.PROGRAM, programId = "p",
        timedFrom = gymRange.fromMs, timedTo = gymRange.toMs, startedElapsed = 4_000_000L, linkedIds = "hc1")
    private fun cardio(id: String = "c") = Session(id, "2026-11-20", at(17, 0), finishedAt = at(17, 31), itemType = ItemType.CARDIO, exerciseId = "nor",
        timeSec = 1800, distanceM = 5000, avgHr = 150, timedFrom = at(17, 0), timedTo = at(17, 30), timerRun = 2, timerDone = 2)
    private fun set(id: String, session: String = "g", status: String = SetStatus.DONE, checkedAt: Long? = at(18, 10)) =
        SetLog(id, session, 0, false, 0, 0, 0, "bench", "Bench", 6, 10, 2.5, 60.0, 8, status, checkedAt)
    private fun rounds(session: String = "c") = (0 until 4).map {
        IntervalLog("$session-i$it", session, it, avgHr = 160 + it, watts = 200, startedAt = at(17, 1 + it * 7), endedAt = at(17, 5 + it * 7))
    }
    private fun filled(id: String, r: HrRange = gymRange, avg: Int = 130) =
        SessionHr(id, r.fromMs, r.toMs, HrState.FILLED, "0:$avg,5:$avg", 2, 2, "mi", "mi", r.fromMs, r.fromMs + 5000, 5, avg, avg, fetchedAt = at(18, 50))
    private fun off(id: String, r: HrRange) = SessionHr(id, r.fromMs, r.toMs, HrState.OFF, fetchedAt = at(18, 55))
    private val cardioRange = HrRange(at(17, 0), at(17, 31))

    private suspend fun Phone.fill() {
        dao.insertSessionWithLogs(gym(), listOf(set("s1"), set("s2", status = SetStatus.PENDING, checkedAt = null)), emptyList())
        dao.insertSessionWithLogs(cardio(), emptyList(), rounds())
        dao.upsertSessionHrs(listOf(filled("g"), off("c", cardioRange)))
    }

    private suspend fun Phone.rows() = Triple(dao.sessionsOnce().sortedBy { it.id }, dao.allSetLogsOnce().sortedBy { it.id }, dao.intervalLogsOnce().sortedBy { it.id })
    private suspend fun Phone.hr() = dao.sessionHrOnce().sortedBy { it.sessionId }
    private fun decode(text: String) = json.decodeFromString<Backup>(text)
    private fun Backup.text() = json.encodeToString(this)

    private fun edit(text: String, block: MutableMap<String, JsonElement>.() -> Unit): String =
        JsonObject(Json.parseToJsonElement(text).jsonObject.toMutableMap().apply(block)).toString()
    private fun MutableMap<String, JsonElement>.without(list: String, vararg keys: String) {
        this[list] = JsonArray(getValue(list).jsonArray.map { e -> JsonObject(e.jsonObject - keys.toSet()) })
    }
    /** A file as the version before these columns wrote it. */
    private fun old(text: String) = edit(text) {
        without("sessions", "timedFrom", "timedTo", "startedElapsed", "timerRun", "timerDone", "linkedIds")
        without("setLogs", "checkedAt")
        without("intervalLogs", "startedAt", "endedAt")
        remove("sessionHr")
    }

    @Test fun `C33 export and both imports keep the new columns and valid rows, off too, and never the anchor`() = runBlocking {
        local.fill()
        val file = local.repo.exportJson()
        val (sessions, sets, logs) = local.rows()
        val hr = local.hr()
        assertEquals(2, decode(file).sessionHr.size)
        for (replace in listOf(true, false)) {
            val other = Phone()
            other.repo.importJson(file, replace)
            val (s, l, i) = other.rows()
            assertEquals(sessions.map { it.copy(startedElapsed = null) }, s)
            assertEquals(sets, l)
            assertEquals(logs, i)
            assertEquals(hr, other.hr())
        }
        local.repo.importJson(file, replace = true)
        assertNull(local.dao.session("g")!!.startedElapsed)
        assertEquals(hr, local.hr())
    }

    @Test fun `C33 a file of the version before imports as before and its sessions have no range`() = runBlocking {
        local.fill()
        val file = old(local.repo.exportJson())
        assertTrue(local.repo.describeBackup(file).startsWith("2 sessions"))
        val other = Phone()
        other.repo.importJson(file, replace = true)
        val c = other.dao.session("c")!!
        assertEquals(150, c.avgHr); assertEquals(5000, c.distanceM)
        assertNull(c.timedFrom); assertNull(hrWindow(c, TEST_ZONE)); assertNull(hrWindow(other.dao.session("g")!!, TEST_ZONE))
        assertTrue(other.hr().isEmpty())
        assertEquals(4, other.dao.intervalLogsOnce().count { it.avgHr != null && it.startedAt == null })
    }

    @Test fun `C33 AT6 AT24 bad, undecodable, orphan and duplicate rows never fail the preview or either import`() = runBlocking {
        local.fill()
        listOf("g2", "g3", "g4").forEach { local.dao.upsert(gym(it)) }
        val base = local.repo.exportJson()
        fun row(r: SessionHr) = Json.parseToJsonElement(json.encodeToString(r))
        val rows = listOf(
            row(filled("ghost")),
            JsonObject(row(filled("g")).jsonObject + ("fromMs" to JsonPrimitive("x"))),
            JsonObject(row(filled("g")).jsonObject + ("state" to JsonPrimitive(5))),
            row(filled("g", avg = 100)),
            row(filled("g", avg = 110)),
            row(off("c", cardioRange)),
            row(filled("g2").copy(samples = "0:999")),
            row(filled("g3").copy(samples = "0:130,2400:130")),
            row(filled("g4").copy(samples = "")),
            JsonPrimitive("not a row"),
        )
        val file = edit(base) { this["sessionHr"] = JsonArray(rows) }
        assertTrue(local.repo.describeBackup(file).startsWith("5 sessions"))
        for (replace in listOf(true, false)) {
            val other = Phone()
            other.repo.importJson(file, replace)
            assertEquals(listOf("c", "g", "g2", "g3", "g4"), other.dao.sessionsOnce().map { it.id }.sorted())
            assertEquals(150, other.dao.session("c")!!.avgHr)
            assertEquals(listOf(off("c", cardioRange), filled("g", avg = 110)), other.hr())
        }
    }

    @Test fun `AT6 a state that is a JSON number is dropped, Preview and Replace keep the session and its typed average`() = runBlocking {
        local.fill()
        val file = edit(local.repo.exportJson()) {
            this["sessionHr"] = JsonArray(getValue("sessionHr").jsonArray.map { JsonObject(it.jsonObject + ("state" to JsonPrimitive(1))) })
        }
        assertTrue(local.repo.describeBackup(file).startsWith("2 sessions"))
        local.repo.importJson(file, replace = true)
        assertEquals(150, local.dao.session("c")!!.avgHr)
        assertTrue(local.hr().isEmpty())
    }

    @Test fun `validFileHr checks the range, the state, the samples and the values`() {
        val ok = filled("g")
        assertTrue(validFileHr(ok))
        assertTrue(validFileHr(off("g", gymRange)))
        assertTrue(validFileHr(ok.copy(state = HrState.WAITING, avgHr = null, maxHr = null)))
        assertTrue(validFileHr(ok.copy(state = HrState.NONE, samples = "")))
        assertTrue(validFileHr(ok.copy(samples = "2395:30,0:230")))
        val bad = listOf(
            ok.copy(toMs = ok.fromMs), ok.copy(state = "full"), ok.copy(samples = "0:130,x"), ok.copy(samples = "2400:130"),
            ok.copy(samples = "-5:130"), ok.copy(samples = "0:29"), ok.copy(samples = "0:231"), ok.copy(samples = ""),
            ok.copy(avgHr = null), ok.copy(maxHr = 231), ok.copy(avgHr = 29), off("g", gymRange).copy(samples = "0:130"),
            off("g", gymRange).copy(avgHr = 120),
        )
        bad.forEach { assertFalse(it.toString(), validFileHr(it)) }
    }

    @Test fun `C34 Merge keeps every local clock value and the local row against an old file and another file`() = runBlocking {
        local.fill()
        val before = local.rows()
        val hr = local.hr()
        val file = decode(local.repo.exportJson())
        val other = file.copy(
            sessions = file.sessions.map { it.copy(timedFrom = it.timedFrom!! + MIN, timedTo = it.timedTo!! - MIN, startedElapsed = 9L, timerRun = 7, timerDone = 7, linkedIds = "hc9") },
            setLogs = file.setLogs.map { it.copy(checkedAt = at(18, 20)) },
            intervalLogs = file.intervalLogs.map { it.copy(startedAt = it.startedAt!! + 1000, endedAt = it.endedAt!! + 1000) },
            sessionHr = listOf(filled("g", avg = 99), filled("c", cardioRange, avg = 98)),
        )
        local.repo.importJson(old(local.repo.exportJson()), replace = false)
        assertEquals(before, local.rows())
        assertEquals(hr, local.hr())
        local.repo.importJson(other.text(), replace = false)
        assertEquals(before.first, local.rows().first)
        assertEquals(before.third, local.rows().third)
        assertEquals(listOf(at(18, 10), null), local.rows().second.map { it.checkedAt })
        assertEquals(hr, local.hr())

        val newcomer = other.copy(
            sessions = other.sessions.map { it.copy(id = "n" + it.id) }, setLogs = other.setLogs.map { it.copy(id = "n" + it.id, sessionId = "n" + it.sessionId) },
            intervalLogs = other.intervalLogs.map { it.copy(id = "n" + it.id, sessionId = "n" + it.sessionId) },
            sessionHr = other.sessionHr.map { it.copy(sessionId = "n" + it.sessionId) },
        )
        local.repo.importJson(newcomer.text(), replace = false)
        newcomer.sessions.forEach { assertEquals(it.copy(startedElapsed = null), local.dao.session(it.id)) }
        val sets = local.dao.allSetLogsOnce().associateBy { it.id }
        newcomer.setLogs.forEach { assertEquals(it.checkedAt.takeIf { _ -> it.status == SetStatus.DONE }, sets.getValue(it.id).checkedAt) }
        val rounds = local.dao.intervalLogsOnce().associateBy { it.id }
        newcomer.intervalLogs.forEach { assertEquals(it, rounds[it.id]) }
        assertEquals(newcomer.sessionHr.sortedBy { it.sessionId }, local.hr().filter { it.sessionId.startsWith("n") })

        local.repo.importJson(other.text(), replace = true)
        assertEquals(other.sessions.map { it.copy(startedElapsed = null) }.sortedBy { it.id }, local.rows().first)
        assertEquals(other.setLogs.sortedBy { it.id }, local.rows().second)
        assertEquals(other.intervalLogs.sortedBy { it.id }, local.rows().third)
        assertEquals(other.sessionHr.sortedBy { it.sessionId }, local.hr())
    }

    @Test fun `C34a a file row that moves the workout clears the range and round times, its row is stale and cleaned`() = runBlocking {
        val moves: List<Pair<String, (Session) -> Session>> = listOf(
            "startedAt" to { s -> s.copy(startedAt = s.startedAt + MIN) },
            "finishedAt" to { s -> s.copy(finishedAt = s.finishedAt!! + MIN) },
            "exerciseId" to { s -> s.copy(exerciseId = "z2run") },
        )
        for ((name, move) in moves) {
            close(); open()
            local.fill()
            local.dao.upsertSessionHr(filled("c", cardioRange))
            val file = decode(local.repo.exportJson())
            local.repo.importJson(file.copy(sessions = file.sessions.map { if (it.id == "c") move(it) else it }).text(), replace = false)
            val c = local.dao.session("c")!!
            assertNull(name, c.timedFrom); assertNull(name, c.timedTo)
            assertEquals(name, 2, c.timerRun)
            assertTrue(name, local.dao.intervalLogsFor("c").all { it.startedAt == null && it.endedAt == null && it.avgHr != null })
            assertEquals(name, gymRange.toMs, local.dao.session("g")!!.timedTo)
            val row = local.dao.sessionHr("c")!!
            assertEquals(name, HrFacts.NoRange, hrFacts(c, row, TEST_ZONE))
            local.heart.answer = { error("provider failed") }
            local.fill.pass()
            assertNull(name, local.dao.sessionHr("c"))
            assertEquals(name, filled("g"), local.dao.sessionHr("g"))
        }
    }

    @Test fun `C34a AT25 a set the file makes pending or skipped loses its stamp, a new check stamps the new time`() = runBlocking {
        for (status in listOf(SetStatus.PENDING, SetStatus.SKIPPED)) {
            close(); open()
            val clocks = local.clocks
            clocks.wall = at(18, 0)
            local.dao.insertSessionWithLogs(
                Session("live", "2026-11-20", at(18, 0), itemType = ItemType.PROGRAM, programId = "p", startedElapsed = clocks.elapsed),
                listOf(set("a", "live", SetStatus.PENDING, null)), emptyList(),
            )
            clocks.pass(8 * MIN)
            assertTrue(local.repo.checkSet("a"))
            assertEquals(at(18, 8), local.dao.allSetLogsOnce().single().checkedAt)
            val file = decode(local.repo.exportJson())
            local.repo.importJson(file.copy(setLogs = file.setLogs.map { it.copy(status = status, checkedAt = null) }).text(), replace = false)
            local.dao.allSetLogsOnce().single().let { assertEquals(status, it.status); assertNull(status, it.checkedAt) }
            clocks.pass(7 * MIN)
            assertTrue(local.repo.checkSet("a"))
            assertEquals(status, at(18, 15), local.dao.allSetLogsOnce().single().checkedAt)
            local.repo.importJson(file.copy(setLogs = file.setLogs.map { it.copy(status = status, checkedAt = at(18, 2)) }).text(), replace = false)
            assertNull(status, local.dao.allSetLogsOnce().single().checkedAt)
        }
    }

    @Test fun `AT17 an imported anchor is never kept, a check has no stamp, Finish no range, confirmRange works`() = runBlocking {
        val clocks = local.clocks
        clocks.wall = at(18, 0)
        local.dao.insertSessionWithLogs(
            Session("live", "2026-11-20", at(18, 0), itemType = ItemType.PROGRAM, programId = "p", startedElapsed = clocks.elapsed),
            listOf(set("a", "live", SetStatus.PENDING, null)), emptyList(),
        )
        val file = local.repo.exportJson()
        assertTrue(decode(file).sessions.single().startedElapsed != null)
        for (replace in listOf(true, false)) {
            val other = Phone()
            other.clocks.wall = at(18, 0); other.clocks.elapsed = clocks.elapsed
            other.repo.importJson(file, replace)
            assertNull(other.dao.session("live")!!.startedElapsed)
            other.clocks.pass(10 * MIN)
            assertTrue(other.repo.checkSet("a"))
            other.dao.allSetLogsOnce().single().let { assertEquals(SetStatus.DONE, it.status); assertNull(it.checkedAt) }
            other.clocks.pass(30 * MIN)
            other.repo.finishSession("live")
            val s = other.dao.session("live")!!
            assertEquals(at(18, 40), s.finishedAt)
            assertNull(s.timedFrom)
            assertEquals(ConfirmRangeResult.Confirmed, other.repo.confirmRange("live", at(18, 0), at(18, 40)))
            assertEquals(gymRange, hrWindow(other.dao.session("live")!!, TEST_ZONE))
        }
    }

    @Test fun `AT24 Remove without a range writes nothing, an off row survives both imports and blocks the pass`() = runBlocking {
        local.dao.upsert(gym("bare").copy(timedFrom = null, timedTo = null))
        assertFalse(local.fill.removeHeartRate("bare"))
        assertNull(local.dao.sessionHr("bare"))
        local.dao.upsert(gym("g"))
        assertTrue(local.fill.removeHeartRate("g"))
        val off = local.dao.sessionHr("g")!!
        val file = local.repo.exportJson()
        for (replace in listOf(true, false)) {
            val other = Phone()
            other.repo.importJson(file, replace)
            assertEquals(off, other.dao.sessionHr("g"))
            other.fill.pass()
            assertTrue(other.heart.calls.isEmpty())
            assertEquals(HrFacts.Removed, hrFacts(other.dao.session("g")!!, other.dao.sessionHr("g"), TEST_ZONE))
        }
    }
}
