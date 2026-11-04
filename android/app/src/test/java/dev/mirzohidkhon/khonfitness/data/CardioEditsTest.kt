package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The cardio form's Save against a real in-memory database, while the timer writes the session's time: the form
 * writes only the fields the user changed since it opened, so neither undoes the other.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CardioEditsTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        repo = KhonRepository(dao)
    }

    @After fun close() = db.close()

    private val round = IntervalLog("i1", "c1", 0)

    // An unfinished cardio session as the form opens it while its timer runs.
    private suspend fun seed(): Session {
        val s = Session(id = "c1", date = "2026-11-02", startedAt = 1_000L, itemType = ItemType.CARDIO, exerciseId = "row", exerciseName = "Rower", timeSec = 600, distanceM = 2_000)
        dao.upsert(s)
        dao.upsertIntervalLogs(listOf(round))
        return s
    }

    private suspend fun row() = dao.session("c1")!!

    @Test fun `timer writes the time then the form saves a note edit and both stay`() = runBlocking {
        val opened = seed()
        repo.setSessionTime(opened.id, 1_800) // the timer finishes while the form is open
        repo.saveCardioEdits(opened, opened.copy(note = "Easy pace"), mapOf("i1" to IntervalEdit(avgHr = Edit.Set(140))), now = 5_000L)
        assertEquals(opened.copy(timeSec = 1_800, note = "Easy pace", finishedAt = 5_000L), row())
        assertEquals(listOf(round.copy(avgHr = 140)), dao.intervalLogsOnce())
    }

    @Test fun `the form's own time edit wins over the timer's earlier write`() = runBlocking {
        val opened = seed()
        repo.setSessionTime(opened.id, 1_800)
        repo.saveCardioEdits(opened, opened.copy(timeSec = 1_500), emptyMap(), now = 5_000L)
        assertEquals(opened.copy(timeSec = 1_500, finishedAt = 5_000L), row())
    }

    @Test fun `a timer write after the form saved wins`() = runBlocking {
        val opened = seed()
        repo.saveCardioEdits(opened, opened.copy(timeSec = 1_500, effort = 6), emptyMap(), now = 5_000L)
        repo.setSessionTime(opened.id, 1_800)
        assertEquals(opened.copy(timeSec = 1_800, effort = 6, finishedAt = 5_000L), row())
    }

    @Test fun `an untouched form save changes nothing but finishedAt`() = runBlocking {
        val opened = seed()
        repo.setSessionTime(opened.id, 1_800)
        repo.setSessionNote(opened.id, "From the session page")
        val before = row()
        repo.saveCardioEdits(opened, opened, emptyMap(), now = 5_000L)
        assertEquals(before.copy(finishedAt = 5_000L), row())
        // A finished session keeps its finish time.
        repo.saveCardioEdits(opened, opened, emptyMap(), now = 9_000L)
        assertEquals(before.copy(finishedAt = 5_000L), row())
    }

    @Test fun `every field the form edits is written`() = runBlocking {
        val opened = seed()
        val edited = opened.copy(date = "2026-11-01", timeSec = 900, distanceM = 2_500, avgHr = 150, watts = 210, laps = 40,
            poolLength = 50, stroke = "Backstroke", effort = 8, note = "Hard")
        repo.saveCardioEdits(opened, edited, emptyMap(), now = 5_000L)
        assertEquals(edited.copy(finishedAt = 5_000L), row())
    }

    @Test fun `a deleted session is not written back`() = runBlocking {
        val opened = seed()
        repo.deleteSession(opened.id)
        repo.saveCardioEdits(opened, opened.copy(note = "Late save"), emptyMap(), now = 5_000L)
        assertEquals(null, dao.session("c1"))
        assertEquals(emptyList<IntervalLog>(), dao.intervalLogsOnce())
    }

    // ---- intervals

    @Test fun `an import that changed watts stays after a note Save`() = runBlocking {
        val opened = seed()
        dao.upsertIntervalLogs(listOf(round.copy(watts = 250))) // an import while the form is open
        repo.saveCardioEdits(opened, opened.copy(note = "Easy pace"), emptyMap(), now = 5_000L)
        assertEquals(listOf(round.copy(watts = 250)), dao.intervalLogsOnce())
    }

    @Test fun `an interval edit writes only its field`() = runBlocking {
        val opened = seed()
        dao.upsertIntervalLogs(listOf(round.copy(watts = 250)))
        repo.saveCardioEdits(opened, opened, mapOf("i1" to IntervalEdit(avgHr = Edit.Set(150))), now = 5_000L)
        assertEquals(listOf(round.copy(avgHr = 150, watts = 250)), dao.intervalLogsOnce())
        repo.saveCardioEdits(opened, opened, mapOf("i1" to IntervalEdit(watts = Edit.Set(260))), now = 6_000L)
        assertEquals(listOf(round.copy(avgHr = 150, watts = 260)), dao.intervalLogsOnce())
    }

    @Test fun `a touched field equal to its first value is written over an import`() = runBlocking {
        val opened = seed()
        dao.upsertIntervalLogs(listOf(round.copy(avgHr = 130)))
        repo.saveCardioEdits(opened, opened, mapOf("i1" to IntervalEdit(avgHr = Edit.Set(null))), now = 5_000L)
        assertEquals(listOf(round), dao.intervalLogsOnce())
    }

    @Test fun `a cleared field writes null`() = runBlocking {
        val opened = seed()
        dao.upsertIntervalLogs(listOf(round.copy(avgHr = 130, watts = 250)))
        repo.saveCardioEdits(opened, opened, mapOf("i1" to IntervalEdit(watts = Edit.Set(null))), now = 5_000L)
        assertEquals(listOf(round.copy(avgHr = 130)), dao.intervalLogsOnce())
    }

    @Test fun `an interval deleted meanwhile is not written back`() = runBlocking {
        val opened = seed()
        dao.deleteIntervalLogsFor(opened.id)
        repo.saveCardioEdits(opened, opened, mapOf("i1" to IntervalEdit(avgHr = Edit.Set(150), watts = Edit.Set(200))), now = 5_000L)
        assertEquals(emptyList<IntervalLog>(), dao.intervalLogsOnce())
    }

    @Test fun `interval edits on a deleted session write nothing`() = runBlocking {
        val opened = seed()
        dao.deleteSession(opened.id)
        dao.upsertIntervalLogs(listOf(round))
        repo.saveCardioEdits(opened, opened, mapOf("i1" to IntervalEdit(avgHr = Edit.Set(150))), now = 5_000L)
        assertEquals(listOf(round), dao.intervalLogsOnce())
    }

    @Test fun `an edit shows the typed value when touched and the current one when not`() {
        assertEquals(140, Edit.Untouched.or(140))
        assertEquals(150, Edit.Set(150).or(140))
        assertEquals(null, Edit.Set<Int?>(null).or(140))
    }
}
