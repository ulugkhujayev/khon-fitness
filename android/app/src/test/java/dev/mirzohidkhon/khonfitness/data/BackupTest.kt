package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Export, merge and replace against real in-memory databases: one active program after every import, and a full
 * round trip keeps every table.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class BackupTest {
    private val dbs = mutableListOf<KhonDatabase>()

    private fun newRepo(): Pair<KhonDao, KhonRepository> {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dbs += db
        return db.dao() to KhonRepository(db.dao())
    }

    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository

    @Before fun open() { newRepo().let { (d, r) -> dao = d; repo = r } }

    @After fun close() = dbs.forEach { it.close() }

    private fun backupOf(programs: List<Program>) = Json.encodeToString(Backup(
        exercises = emptyList(), modalities = emptyList(), programs = programs, blocks = emptyList(), blockExercises = emptyList(),
        sessions = emptyList(), setLogs = emptyList(), intervalLogs = emptyList(), bodyweights = emptyList(), weekPlan = emptyList(), overrides = emptyList(),
    ))

    private suspend fun active() = dao.programsOnce().filter { it.active }.map { it.id }.sorted()

    // ---- one active program after an import

    @Test fun `merging an export from before the active mark moved keeps this phone's active program only`() = runBlocking {
        dao.upsertPrograms(listOf(Program("a", "A", active = true, sortOrder = 0), Program("b", "B", sortOrder = 1)))
        val export = repo.exportJson() // A active
        repo.setActiveProgram("b")
        repo.importJson(export, replace = false)
        assertEquals(listOf("b"), active())
    }

    @Test fun `merge clears the mark on imported programs`() = runBlocking {
        dao.upsertPrograms(listOf(Program("a", "A", active = true, sortOrder = 0)))
        repo.importJson(backupOf(listOf(Program("c", "C", active = true, sortOrder = 0), Program("d", "D", active = true, sortOrder = 1))), replace = false)
        assertEquals(listOf("a"), active())
    }

    @Test fun `merge onto a phone with no programs takes the backup's first active program`() = runBlocking {
        repo.importJson(backupOf(listOf(Program("c", "C", active = true, sortOrder = 1), Program("d", "D", active = true, sortOrder = 0))), replace = false)
        assertEquals(listOf("d"), active())
    }

    @Test fun `replace with no active program in the backup makes the first program active`() = runBlocking {
        dao.upsertPrograms(listOf(Program("a", "A", active = true)))
        repo.importJson(backupOf(listOf(Program("p1", "One", sortOrder = 1), Program("p0", "Zero", sortOrder = 0))), replace = true)
        assertEquals(listOf("p0"), active())
    }

    @Test fun `replace with one active program in the backup keeps it`() = runBlocking {
        dao.upsertPrograms(listOf(Program("a", "A", active = true)))
        repo.importJson(backupOf(listOf(Program("p0", "Zero", sortOrder = 0), Program("p1", "One", active = true, sortOrder = 1))), replace = true)
        assertEquals(listOf("p1"), active())
    }

    @Test fun `replace with two active programs in the backup keeps the first in list order`() = runBlocking {
        dao.upsertPrograms(listOf(Program("a", "A", active = true)))
        repo.importJson(backupOf(listOf(
            Program("p2", "Two", active = true, sortOrder = 2), Program("p1b", "One b", active = true, sortOrder = 1),
            Program("p1a", "One a", active = true, sortOrder = 1), Program("p0", "Zero", sortOrder = 0),
        )), replace = true)
        assertEquals(listOf("p1a"), active())
    }

    @Test fun `replace with no programs in the backup leaves none`() = runBlocking {
        dao.upsertPrograms(listOf(Program("a", "A", active = true)))
        repo.importJson(backupOf(emptyList()), replace = true)
        assertEquals(emptyList<Program>(), dao.programsOnce())
    }

    // ---- round trip

    /** Every table in a fixed order, so two databases compare row for row. */
    private fun Backup.sorted() = copy(
        exercises = exercises.sortedBy { it.id }, modalities = modalities.sortedBy { it.id }, programs = programs.sortedBy { it.id },
        blocks = blocks.sortedBy { it.id }, blockExercises = blockExercises.sortedBy { it.id }, sessions = sessions.sortedBy { it.id },
        setLogs = setLogs.sortedBy { it.id }, intervalLogs = intervalLogs.sortedBy { it.id }, bodyweights = bodyweights.sortedBy { it.date },
        weekPlan = weekPlan.sortedBy { it.weekday }, overrides = overrides.sortedBy { it.date }, stretches = stretches.sortedBy { it.id },
        stretchRoutines = stretchRoutines.sortedBy { it.id }, routineStretches = routineStretches.sortedBy { it.id },
        stretchSessions = stretchSessions.sortedBy { it.id }, windowDays = windowDays.sortedBy { it.date },
    )

    private fun parse(json: String) = Json.decodeFromString<Backup>(json).sorted()

    @Test fun `export, merge into an empty database, then replace keeps every table equal`() = runBlocking {
        Seed.apply(dao); Seed.applyStretches(dao)
        dao.upsertPrograms(listOf(Program("legs", "Legs", sortOrder = 1)))
        dao.upsert(Session(id = "s1", date = "2026-11-01", startedAt = 1L, finishedAt = 2L, itemType = ItemType.PROGRAM, programId = "tpv3", programName = "TP v3", effort = 7, note = "Good"))
        dao.upsert(SetLog("l1", "s1", 0, true, 0, 0, 0, "bench", "Barbell Bench Press", 8, 12, 2.5, weightKg = 60.0, reps = 10, status = SetStatus.DONE))
        dao.upsert(Session(id = "c1", date = "2026-11-02", startedAt = 3L, finishedAt = 4L, itemType = ItemType.CARDIO, exerciseId = "nor4x4", exerciseName = "Norwegian 4x4", timeSec = 1800))
        dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c1", 0, avgHr = 160)))
        dao.upsert(Bodyweight("2026-11-01", 80.5))
        dao.upsert(DayOverride("2026-11-03", ItemType.REST))
        dao.recordStretchSession(StretchSession(id = "st1", routineId = "daily", routineName = "Daily mobility", date = "2026-11-01", startedAt = 5L, totalSec = 600, completed = 9, skipped = 0))
        dao.upsert(WindowDay("2026-11-01", openedAt = 6L, closedAt = 7L))
        val export = repo.exportJson()
        val original = parse(export)
        assertTrue(original.sessions.size == 2 && original.setLogs.size == 1 && original.stretchSessions.size == 1)
        assertTrue(original.stretchRoutines.single { it.id == "daily" }.active) // the empty phone takes the backup's active routine

        val (dao2, repo2) = newRepo()
        repo2.importJson(export, replace = false)
        assertEquals(original, parse(repo2.exportJson()))
        dao2.upsert(Bodyweight("2026-11-09", 79.0)) // replace must drop this
        repo2.importJson(export, replace = true)
        assertEquals(original, parse(repo2.exportJson()))
    }
}
