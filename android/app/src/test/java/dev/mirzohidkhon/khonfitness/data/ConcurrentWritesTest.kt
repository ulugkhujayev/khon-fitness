package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/**
 * Multi-step writes against a real in-memory database while another write lands in the middle, or while a later step
 * fails: each step's reads and writes run in one transaction, so the other write is never lost and a failed step
 * leaves nothing behind.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ConcurrentWritesTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository
    private val race = Race()

    @Before fun open() {
        db = raceDatabase(race)
        dao = db.dao()
        repo = KhonRepository(dao)
    }

    @After fun close() = db.close()

    /**
     * A raw write, as an import's: a Room suspend write would queue behind the step's own write on Room's serial
     * transaction executor, so it could never land in the gap on the old code.
     */
    private fun importExercise(id: String, name: String) = db.openHelper.writableDatabase.execSQL(
        "INSERT OR REPLACE INTO exercises (id, name, kind, notes, archived, builtin, secondaryMuscles, stepKg, intervals, warmupSec, workSec, restSec, rounds, fields) " +
            "VALUES (?, ?, ?, '', 0, 0, '', 2.5, 0, 0, 0, 0, 0, '')", arrayOf(id, name, Kind.STRENGTH),
    )

    private fun failInserts(table: String) =
        db.openHelper.writableDatabase.execSQL("CREATE TEMP TRIGGER fail_$table BEFORE INSERT ON $table BEGIN SELECT RAISE(ABORT, 'test failure'); END")

    private suspend fun assertFails(block: suspend () -> Unit) {
        val thrown = try { block(); null } catch (e: Exception) { e }
        assertTrue("the write should fail", thrown != null)
    }

    private val today = LocalDate.of(2026, 11, 2) // a Monday

    // ---- week plan and dated changes

    @Test fun `a dated change saved while the week plan reads stays manual`() = runBlocking {
        dao.upsert(WeekPlan(1, ItemType.PROGRAM, "push"))
        // After the plan's overrides are read, and before it writes, a dated change lands for last Monday.
        race.arm({ it == "SELECT * FROM sessions" }) { dao.upsert(DayOverride("2026-10-26", ItemType.CARDIO, "z2run")) }
        repo.saveWeekPlan(1, ItemType.REST, null, today)
        race.finish()
        assertEquals(DayOverride("2026-10-26", ItemType.CARDIO, "z2run", auto = false), dao.overridesOnce().find { it.date == "2026-10-26" })
        assertEquals(DayOverride("2026-10-19", ItemType.PROGRAM, "push", auto = true), dao.overridesOnce().find { it.date == "2026-10-19" })
        assertEquals(WeekPlan(1, ItemType.REST), dao.weekPlanOnce().single())
    }

    @Test fun `a dated change saved during a swap is not lost`() = runBlocking {
        dao.upsertPrograms(listOf(Program("push", "Push", active = true), Program("pull", "Pull", sortOrder = 1)))
        dao.upsert(Exercise("z2run", "Zone 2 run", Kind.CARDIO))
        dao.upsertWeekPlan(listOf(WeekPlan(2, ItemType.PROGRAM, "push"), WeekPlan(3, ItemType.CARDIO, "z2run")))
        val tue = "2026-11-03"; val wed = "2026-11-04"
        // Wednesday gets Pull from the Today sheet after the swap has read the plan, before it writes.
        race.arm(firstWriteAfterRead()) { repo.saveOverride(wed, ItemType.PROGRAM, "pull") }
        repo.swapDays(LocalDate.parse(tue), LocalDate.parse(wed))
        race.finish()
        // The swap, then the change: Tuesday holds the run, Wednesday holds Pull.
        assertEquals(DayOverride(tue, ItemType.CARDIO, "z2run"), dao.overridesOnce().find { it.date == tue })
        assertEquals(DayOverride(wed, ItemType.PROGRAM, "pull"), dao.overridesOnce().find { it.date == wed })
    }

    // ---- eating window

    private fun importDay(date: String, openedAt: Long?, closedAt: Long?, note: String) = db.openHelper.writableDatabase.execSQL(
        "INSERT OR REPLACE INTO window_days (date, openedAt, closedAt, note) VALUES (?, ?, ?, ?)", arrayOf(date, openedAt, closedAt, note),
    )

    @Test fun `open now keeps a day row an import wrote meanwhile`() = runBlocking {
        val date = today.iso()
        dao.upsert(WindowDay(date, note = "old"))
        race.arm(firstWriteAfterRead("window_days")) { importDay(date, null, 7L, "imported") }
        repo.openWindowNow(date, 100L)
        race.finish()
        // Either order is fine; the held copy of the row is never written back.
        val row = dao.windowDay(date)
        assertTrue("$row", row == WindowDay(date, openedAt = 100L, note = "imported") || row == WindowDay(date, closedAt = 7L, note = "imported"))
    }

    @Test fun `close now keeps a day row an import wrote meanwhile`() = runBlocking {
        val date = today.iso()
        dao.upsert(WindowDay(date, openedAt = 50L, note = "old"))
        race.arm(firstWriteAfterRead("window_days")) { importDay(date, 60L, null, "imported") }
        repo.closeWindowNow(date, 100L)
        race.finish()
        val row = dao.windowDay(date)
        assertTrue("$row", row == WindowDay(date, openedAt = 60L, closedAt = 100L, note = "imported") || row == WindowDay(date, openedAt = 60L, note = "imported"))
    }

    @Test fun `open and close write only their own columns`() = runBlocking {
        val date = today.iso()
        repo.openWindowNow(date, 100L)
        assertEquals(WindowDay(date, openedAt = 100L), dao.windowDay(date))
        repo.closeWindowNow(date, 200L)
        assertEquals(WindowDay(date, openedAt = 100L, closedAt = 200L), dao.windowDay(date))
        dao.upsert(WindowDay(date, openedAt = 100L, closedAt = 200L, note = "Late dinner"))
        repo.openWindowNow(date, 300L)
        assertEquals(WindowDay(date, openedAt = 300L, note = "Late dinner"), dao.windowDay(date))
        repo.closeWindowNow("2026-11-03", 400L)
        assertEquals(WindowDay("2026-11-03", closedAt = 400L), dao.windowDay("2026-11-03"))
    }

    // ---- new sessions

    private suspend fun seedProgram() {
        dao.upsert(Program("push", "Push", active = true))
        dao.upsert(Block("b1", "push", 0))
        dao.upsert(Exercise("bench", "Bench", Kind.STRENGTH))
        dao.upsert(BlockExercise("e1", "b1", 0, "bench"))
    }

    private val intervalBike = Exercise("nor4x4", "Norwegian 4x4", Kind.CARDIO, intervals = true, workSec = 240, restSec = 180, rounds = 4)

    private fun draft(id: String) = dev.mirzohidkhon.khonfitness.health.ImportDraft(
        id, Instant.parse("2026-11-01T07:00:00Z"), Instant.parse("2026-11-01T07:30:00Z"), "Biking", "bike", null, 140, emptyList(), listOf("Watch"),
    )

    @Test fun `a program session whose sets fail to save leaves no session`() = runBlocking {
        seedProgram()
        failInserts("set_logs")
        assertFails { repo.startProgramSession("push", today) }
        assertEquals(emptyList<Session>(), dao.sessionsOnce())
    }

    @Test fun `a cardio session whose intervals fail to save leaves no session`() = runBlocking {
        failInserts("interval_logs")
        assertFails { repo.startCardioSession(intervalBike, today) }
        assertEquals(emptyList<Session>(), dao.sessionsOnce())
    }

    @Test fun `an import whose intervals fail to save leaves no session`() = runBlocking {
        failInserts("interval_logs")
        assertFails { repo.importDraft(draft("hc1"), intervalBike) }
        assertEquals(emptyList<Session>(), dao.sessionsOnce())
    }

    @Test fun `a draft imported twice is one session`() = runBlocking {
        repo.importDraft(draft("hc1"), intervalBike)
        repo.importDraft(draft("hc1"), intervalBike)
        assertEquals(1, dao.sessionsOnce().size)
        assertEquals(4, dao.intervalLogsOnce().size)
    }

    @Test fun `new sessions keep their logs`() = runBlocking {
        seedProgram()
        val program = repo.startProgramSession("push", today)!!
        assertEquals(3, dao.setLogsOnce(program.id).size)
        val cardio = repo.startCardioSession(intervalBike, today)
        assertEquals(4, dao.intervalLogsOnce().count { it.sessionId == cardio.id })
    }

    // ---- seeds

    @Test fun `an exercise saved while the seed writes is not seeded over`() = runBlocking {
        race.arm(firstWriteAfterRead()) { importExercise("bench", "My bench") }
        repo.seedIfEmpty()
        race.finish()
        assertEquals("My bench", dao.exercise("bench")?.name)
    }

    @Test fun `a stretch seed that fails halfway leaves no stretches`() = runBlocking {
        failInserts("routine_stretches")
        assertFails { repo.seedStretchesIfEmpty() }
        assertEquals(emptyList<Stretch>(), dao.stretchesOnce())
        assertEquals(emptyList<StretchRoutine>(), dao.stretchRoutinesOnce())
    }

    @Test fun `a reset that fails halfway keeps the old data`() = runBlocking {
        dao.upsert(Exercise("mine", "My exercise", Kind.STRENGTH))
        failInserts("stretches")
        assertFails { repo.resetToSeed() }
        assertEquals(listOf("mine"), dao.exercisesOnce().map { it.id })
    }

    @Test fun `the seeds write once`() = runBlocking {
        repo.seedIfEmpty()
        val counts = listOf(dao.exercisesOnce().size, dao.programsOnce().size, dao.stretchesOnce().size, dao.routineStretchesOnce().size)
        dao.upsert(dao.exercise("bench")!!.copy(name = "My bench"))
        repo.seedIfEmpty()
        assertEquals(counts, listOf(dao.exercisesOnce().size, dao.programsOnce().size, dao.stretchesOnce().size, dao.routineStretchesOnce().size))
        assertEquals("My bench", dao.exercise("bench")?.name)
        assertTrue(dao.eatingWindowOnce() != null)
    }
}
