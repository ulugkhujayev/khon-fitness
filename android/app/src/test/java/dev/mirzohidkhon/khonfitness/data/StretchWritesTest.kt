package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.timer.Phase
import dev.mirzohidkhon.khonfitness.timer.PhaseKind
import dev.mirzohidkhon.khonfitness.timer.TimerState
import dev.mirzohidkhon.khonfitness.timer.stillOpen
import dev.mirzohidkhon.khonfitness.timer.stretchResult
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The stretch, routine, eating-window and run-result commands against a real in-memory database, in the orders the
 * P5 attack cases name (E, D, P, W): a screen's held copy is never written back, a gone row stays gone, and an
 * import's values stay unless the user changed that field.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class StretchWritesTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        repo = KhonRepository(dao)
    }

    @After fun close() = db.close()

    private val fold = Stretch("fold", "Fold", cue = "Hinge")
    private val cat = Stretch("cat", "Cat cow", mode = StretchMode.REPS, reps = 10)
    private val needle = Stretch("needle", "Thread the needle", sided = true, builtin = true)

    // Stretches fold, cat (custom) and needle (built-in); routine "am" (active, order 0) and "pm" (order 1).
    private suspend fun seed() {
        dao.upsertStretches(listOf(fold, cat, needle))
        dao.upsertStretchRoutines(listOf(StretchRoutine("am", "Morning", active = true, sortOrder = 0), StretchRoutine("pm", "Evening", sortOrder = 1)))
    }

    /** Routine "am" holds rows A, B, C, D in that order (sortOrder 0..3). */
    private suspend fun seedRows(): List<RoutineStretch> {
        seed()
        val rows = listOf("A" to "fold", "B" to "cat", "C" to "needle", "D" to "fold").mapIndexed { i, (id, st) -> RoutineStretch(id, "am", st, i) }
        dao.upsertRoutineStretches(rows)
        return rows
    }

    private fun backup(
        stretches: List<Stretch> = emptyList(), routines: List<StretchRoutine> = emptyList(), rows: List<RoutineStretch> = emptyList(),
        sessions: List<StretchSession> = emptyList(), window: EatingWindow? = null,
    ) = Json.encodeToString(Backup(
        exercises = emptyList(), modalities = emptyList(), programs = emptyList(), blocks = emptyList(), blockExercises = emptyList(),
        sessions = emptyList(), setLogs = emptyList(), intervalLogs = emptyList(), bodyweights = emptyList(), weekPlan = emptyList(), overrides = emptyList(),
        stretches = stretches, stretchRoutines = routines, routineStretches = rows, stretchSessions = sessions, eatingWindow = window,
    ))

    private suspend fun merge(json: String) = repo.importJson(json, replace = false)

    private suspend fun rowsOf(routineId: String) = dao.routineStretchesFor(routineId)
    private suspend fun activeIds() = dao.stretchRoutinesOnce().filter { it.active }.map { it.id }.sorted()

    // ---- E1, row 14: the stretch editor's Done

    @Test fun `E1 an import rename survives Done of a cue edit`() = runBlocking {
        seed()
        val opened = dao.stretch("fold")!!
        merge(backup(stretches = listOf(fold.copy(name = "Forward fold"))))
        repo.saveStretchEdits(opened, opened.copy(cue = "Long spine"))
        assertEquals(fold.copy(name = "Forward fold", cue = "Long spine"), dao.stretch("fold"))
    }

    @Test fun `E1 Done on a stretch deleted meanwhile writes nothing`() = runBlocking {
        seed()
        val opened = dao.stretch("fold")!!
        dao.deleteStretch("fold")
        repo.saveStretchEdits(opened, opened.copy(cue = "Long spine"))
        assertNull(dao.stretch("fold"))
    }

    @Test fun `row 14 a new stretch with a blank name saves nothing, a named one is saved`() = runBlocking {
        assertFalse(repo.createStretch(Stretch("new", "  ")))
        assertNull(dao.stretch("new"))
        assertTrue(repo.createStretch(Stretch("new", "Pigeon")))
        assertEquals("Pigeon", dao.stretch("new")!!.name)
    }

    // ---- E2, E3, rows 12 and 13: deletes

    @Test fun `E2 a stretch a routine started using after the confirm opened is not deleted`() = runBlocking {
        seed()
        assertEquals(0, repo.stretchUseCount("cat")) // the editor shows Delete enabled
        repo.addRoutineStretch("am", "cat", "r1") // another write adds it to a routine
        assertFalse(repo.deleteStretchIfUnused("cat")) // confirm
        assertEquals(cat, dao.stretch("cat"))
        assertEquals(listOf("cat"), rowsOf("am").map { it.stretchId })
    }

    @Test fun `E2 adding a stretch deleted meanwhile writes no routine row`() = runBlocking {
        seed()
        assertTrue(repo.deleteStretchIfUnused("cat"))
        repo.addRoutineStretch("am", "cat", "r1")
        assertNull(dao.stretch("cat"))
        assertTrue(dao.routineStretchesOnce().isEmpty())
    }

    @Test fun `E3 a used or built-in stretch is refused, and after the row goes the stretch and routine delete and the session stays`() = runBlocking {
        seed()
        repo.addRoutineStretch("am", "fold", "r1")
        val session = StretchSession("am:s1", "am", "Morning", "2026-11-01", 5L, 600, 1, 0)
        repo.recordStretchSession(session)
        assertFalse(repo.deleteStretchIfUnused("fold"))
        assertFalse(repo.deleteStretchIfUnused("needle"))
        assertNotNull(dao.stretch("fold")); assertNotNull(dao.stretch("needle"))
        repo.removeRoutineStretch("r1")
        assertTrue(repo.deleteStretchIfUnused("fold"))
        repo.deleteRoutine("am")
        assertNull(dao.stretch("fold"))
        assertNull(dao.stretchRoutine("am"))
        assertEquals(listOf(session), repo.stretchSessions.first()) // History lists it
    }

    // ---- E4, E5, E10, E11, row 16: creating routines

    @Test fun `E4 and row 16 Done on a new routine saves its name and Active, the only active one, with no stretches`() = runBlocking {
        seed()
        assertTrue(repo.createRoutine(StretchRoutine("new", "Night", active = true)))
        assertEquals(StretchRoutine("new", "Night", active = true, sortOrder = 2), dao.stretchRoutine("new"))
        assertEquals(listOf("new"), activeIds())
        assertTrue(rowsOf("new").isEmpty())
        assertTrue(repo.createRoutine(StretchRoutine("off", "Off", active = false)))
        assertEquals(listOf("new"), activeIds())
    }

    @Test fun `E5 a new routine with its first stretch, created again after a restore, keeps one routine and one row`() = runBlocking {
        seed()
        val draft = StretchRoutine("new", "Night")
        assertTrue(repo.createRoutine(draft, "fold", "r1"))
        assertTrue(repo.createRoutine(draft, "fold", "r1")) // the restored editor calls it again
        assertEquals(1, dao.stretchRoutinesOnce().count { it.id == "new" })
        assertEquals(listOf(RoutineStretch("r1", "new", "fold", 0)), rowsOf("new"))
    }

    @Test fun `E10 a routine created after a delete goes after the highest order`() = runBlocking {
        seed()
        dao.upsert(StretchRoutine("x", "Third", sortOrder = 2))
        repo.deleteRoutine("pm") // orders 0 and 2 are left
        repo.createRoutine(StretchRoutine("new", "Night"))
        assertEquals(3, dao.stretchRoutine("new")!!.sortOrder)
    }

    @Test fun `E11 a new routine whose first stretch was deleted meanwhile writes nothing`() = runBlocking {
        seed()
        dao.deleteStretch("cat")
        assertFalse(repo.createRoutine(StretchRoutine("new", "Night", active = true), "cat", "r1"))
        assertNull(dao.stretchRoutine("new"))
        assertTrue(dao.routineStretchesOnce().isEmpty())
        assertEquals(listOf("am"), activeIds())
    }

    // ---- "+" on Stretching (P5B-FIX4): a saved, empty routine at once

    @Test fun `a new routine on an empty table is the one active routine, named New routine`() = runBlocking {
        repo.createNewRoutine("n1")
        assertEquals(listOf(StretchRoutine("n1", NEW_ROUTINE_NAME, active = true, sortOrder = 0)), dao.stretchRoutinesOnce())
        assertTrue(rowsOf("n1").isEmpty())
    }

    @Test fun `a new routine next to others is not active, the old active one keeps the mark, and it goes last`() = runBlocking {
        seed()
        dao.upsert(StretchRoutine("x", "Third", sortOrder = 5))
        repo.createNewRoutine("n1")
        assertEquals(StretchRoutine("n1", NEW_ROUTINE_NAME, active = false, sortOrder = 6), dao.stretchRoutine("n1"))
        assertEquals(listOf("am"), activeIds())
    }

    @Test fun `a second new routine with the same id writes nothing`() = runBlocking {
        seed()
        repo.createNewRoutine("n1")
        repo.setRoutineName("n1", "Night")
        repo.setRoutineActive("n1", true)
        val before = dao.stretchRoutinesOnce().sortedBy { it.id }
        repo.createNewRoutine("n1")
        assertEquals(before, dao.stretchRoutinesOnce().sortedBy { it.id })
        assertEquals(StretchRoutine("n1", "Night", active = true, sortOrder = 2), dao.stretchRoutine("n1"))
    }

    // ---- E6: edits of a routine deleted while its editor is open

    @Test fun `E6 add, seconds, order, name and Active of a deleted routine write nothing`() = runBlocking {
        val rows = seedRows()
        repo.deleteRoutine("am")
        repo.addRoutineStretch("am", "cat", "r9")
        repo.setRoutineStretchSeconds(rows[1].id, 60)
        repo.moveRoutineStretch(rows[1].id, up = true)
        repo.setRoutineName("am", "Renamed")
        repo.setRoutineActive("am", true)
        assertNull(dao.stretchRoutine("am"))
        assertTrue(dao.routineStretchesOnce().isEmpty())
        assertTrue(activeIds().isEmpty())
    }

    // ---- E7, E8, E9: order

    @Test fun `E7 an imported seconds value stays when another row moves`() = runBlocking {
        val rows = seedRows()
        merge(backup(rows = listOf(rows[1].copy(seconds = 90)))) // B
        repo.moveRoutineStretch("C", up = true) // the screen still holds B without seconds
        assertEquals(90, dao.routineStretch("B")!!.seconds)
        assertEquals(listOf("A", "C", "B", "D"), rowsOf("am").map { it.id })
    }

    @Test fun `E7 a move swaps with the neighbour now, not the removed row the screen showed`() = runBlocking {
        seedRows()
        repo.removeRoutineStretch("B")
        repo.moveRoutineStretch("C", up = true)
        assertNull(dao.routineStretch("B"))
        assertEquals(listOf("C", "A", "D"), rowsOf("am").map { it.id })
    }

    @Test fun `E8 move then remove of the same row leaves the others whole`() = runBlocking {
        val rows = seedRows()
        dao.upsert(rows[0].copy(seconds = 30))
        repo.moveRoutineStretch("C", up = true)
        repo.removeRoutineStretch("C")
        assertEquals(listOf("A", "B", "D"), rowsOf("am").map { it.id })
        assertEquals(listOf(30, null, null), rowsOf("am").map { it.seconds })
    }

    @Test fun `E8 remove then move of the same row re-creates nothing and swaps nothing`() = runBlocking {
        seedRows()
        repo.removeRoutineStretch("C")
        repo.moveRoutineStretch("C", up = true)
        assertNull(dao.routineStretch("C"))
        assertEquals(listOf("A" to 0, "B" to 1, "D" to 3), rowsOf("am").map { it.id to it.sortOrder })
    }

    @Test fun `E9 a move in a routine with tied orders renumbers it first, so the order is strict`() = runBlocking {
        seed()
        dao.upsertRoutineStretches(listOf(RoutineStretch("A", "am", "fold", 0), RoutineStretch("B", "am", "cat", 1), RoutineStretch("C", "am", "needle", 1)))
        repo.moveRoutineStretch("A", up = false)
        assertEquals(listOf("B" to 0, "A" to 1, "C" to 2), rowsOf("am").map { it.id to it.sortOrder })
    }

    @Test fun `ordering ties sort by id in every list`() = runBlocking {
        seed()
        dao.upsertRoutineStretches(listOf(RoutineStretch("z", "am", "fold", 0), RoutineStretch("a", "am", "cat", 0)))
        dao.upsert(StretchRoutine("aa", "Zed", sortOrder = 0))
        assertEquals(listOf("a", "z"), repo.routineStretches.first().map { it.id })
        assertEquals(listOf("aa", "am", "pm"), repo.stretchRoutines.first().map { it.id })
    }

    // ---- D1, row 17: activation

    @Test fun `D1 a rename from an editor opened while its routine was active keeps the newer active routine`() = runBlocking {
        seed()
        repo.setRoutineActive("pm", true) // while am's editor shows am active
        repo.setRoutineName("am", "Morning flow")
        assertEquals(listOf("pm"), activeIds())
        assertEquals("Morning flow", dao.stretchRoutine("am")!!.name)
    }

    @Test fun `D1 Active on a deleted routine changes no flag and re-creates nothing`() = runBlocking {
        seed()
        repo.deleteRoutine("pm")
        repo.setRoutineActive("pm", true)
        assertNull(dao.stretchRoutine("pm"))
        assertEquals(listOf("am"), activeIds())
    }

    @Test fun `D1 the last activation wins in either order`() = runBlocking {
        seed()
        repo.setRoutineActive("pm", true)
        repo.createRoutine(StretchRoutine("new", "Night", active = true))
        assertEquals(listOf("new"), activeIds())
        repo.createRoutine(StretchRoutine("new2", "Late", active = true))
        repo.setRoutineActive("pm", true)
        assertEquals(listOf("pm"), activeIds())
    }

    @Test fun `row 17 Active off leaves no routine active`() = runBlocking {
        seed()
        repo.setRoutineActive("am", false)
        assertTrue(activeIds().isEmpty())
    }

    // ---- D2, D4, D5, D6, row 18: import repairs

    @Test fun `D2 merging an old export keeps this phone's active routine`() = runBlocking {
        seed()
        val export = repo.exportJson() // am active
        repo.setRoutineActive("pm", true)
        merge(export)
        assertEquals(listOf("pm"), activeIds())
    }

    @Test fun `D2 a merge clears the mark on imported routines`() = runBlocking {
        seed()
        merge(backup(routines = listOf(StretchRoutine("x", "Imported", active = true, sortOrder = 5))))
        assertEquals(listOf("am"), activeIds())
    }

    @Test fun `D2 replace keeps none, the one, or the first active routine by sortOrder`() = runBlocking {
        fun routines(vararg active: Boolean) = active.mapIndexed { i, on -> StretchRoutine("r$i", "R$i", active = on, sortOrder = 10 - i) }
        repo.importJson(backup(stretches = listOf(fold), routines = routines(false, false)), replace = true)
        assertTrue(activeIds().isEmpty())
        repo.importJson(backup(stretches = listOf(fold), routines = routines(false, true)), replace = true)
        assertEquals(listOf("r1"), activeIds())
        repo.importJson(backup(stretches = listOf(fold), routines = routines(true, false, true)), replace = true)
        assertEquals(listOf("r2"), activeIds()) // sortOrder 8 comes before 10
    }

    @Test fun `row 18 merging an export after the active routine was turned off keeps none active`() = runBlocking {
        seed()
        val export = repo.exportJson() // am active
        repo.setRoutineActive("am", false)
        merge(export)
        assertTrue(activeIds().isEmpty())
    }

    @Test fun `row 18 replace with a backup without routines leaves the seeded routine off`() = runBlocking {
        seed()
        repo.importJson(backup(), replace = true)
        assertTrue(dao.stretchRoutinesOnce().any { it.id == "daily" }) // the seed ran
        assertTrue(activeIds().isEmpty())
    }

    @Test fun `row 18 a merge onto a phone with B active keeps B over the backup's active routine`() = runBlocking {
        seed()
        repo.setRoutineActive("pm", true)
        merge(backup(routines = listOf(StretchRoutine("x", "Imported", active = true, sortOrder = 0), StretchRoutine("am", "Morning", active = true, sortOrder = 0))))
        assertEquals(listOf("pm"), activeIds())
    }

    @Test fun `row 18 a merge onto a phone with no routine takes the backup's first active by sortOrder, then id`() = runBlocking {
        merge(backup(stretches = listOf(fold), routines = listOf(
            StretchRoutine("r0", "R0", sortOrder = 0), StretchRoutine("rb", "Rb", active = true, sortOrder = 1),
            StretchRoutine("ra", "Ra", active = true, sortOrder = 1), StretchRoutine("rc", "Rc", active = true, sortOrder = 2),
        )))
        assertEquals(listOf("ra"), activeIds())
    }

    @Test fun `D3 a round trip through merge and replace keeps the stretch tables and the settings equal`() = runBlocking {
        seedRows()
        repo.recordStretchSession(StretchSession("am:s1", "am", "Morning", "2026-11-01", 5L, 600, 1, 0))
        repo.setWindowEnabled(true); repo.setWindowStart(10 * 60)
        val export = Json.decodeFromString<Backup>(repo.exportJson())
        val db2 = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        try {
            val repo2 = KhonRepository(db2.dao())
            val json = Json.encodeToString(export)
            repo2.importJson(json, replace = false)
            assertEquals(export, Json.decodeFromString<Backup>(repo2.exportJson()))
            repo2.importJson(json, replace = true)
            assertEquals(export, Json.decodeFromString<Backup>(repo2.exportJson()))
        } finally { db2.close() }
    }

    @Test fun `D4 an import drops routine rows whose routine or stretch is missing and keeps the rest`() = runBlocking {
        seed()
        merge(backup(
            routines = listOf(StretchRoutine("x", "Imported", sortOrder = 2)),
            rows = listOf(RoutineStretch("ok", "x", "fold", 0), RoutineStretch("noRoutine", "gone", "fold", 0), RoutineStretch("noStretch", "x", "gone", 1)),
        ))
        assertEquals(listOf("ok"), dao.routineStretchesOnce().map { it.id })
        assertNotNull(dao.stretchRoutine("x"))
    }

    @Test fun `D5 a replace from a backup without settings leaves a default row that On turns on`() = runBlocking {
        repo.importJson(backup(stretches = listOf(fold)), replace = true)
        assertEquals(EatingWindow(), dao.eatingWindowOnce())
        repo.setWindowEnabled(true)
        assertEquals(EatingWindow(enabled = true), dao.eatingWindowOnce())
    }

    @Test fun `D6 a recorded session changed by an import stays when the run records again`() = runBlocking {
        seed()
        val s = StretchSession("am:s1", "am", "Morning", "2026-11-01", 5L, 600, 1, 0)
        repo.recordStretchSession(s)
        merge(backup(sessions = listOf(s.copy(totalSec = 900))))
        repo.recordStretchSession(s)
        assertEquals(listOf(s.copy(totalSec = 900)), dao.stretchSessionsOnce())
    }

    // ---- P1 to P4, rows 19 and 20: the run result

    private fun run(routineId: String, completed: Int, elapsed: Double) = TimerState(
        "$routineId:run1", "Morning", listOf(Phase(PhaseKind.WORK, 45, 1, label = "Fold", stretchId = "fold")), stretch = true,
        completed = completed, elapsed = elapsed, startedAt = 5L,
    )
    private val day = LocalDate.of(2026, 11, 2)

    @Test fun `P1 and row 20 a result recorded after its routine was deleted keeps the captured name and re-creates nothing`() = runBlocking {
        seedRows()
        repo.deleteRoutine("am")
        repo.recordStretchSession(stretchResult(run("am", 1, 50.0), day)!!)
        assertEquals(listOf(StretchSession("am:run1", "am", "Morning", "2026-11-02", 5L, 50, 1, 0)), dao.stretchSessionsOnce())
        assertNull(dao.stretchRoutine("am"))
        assertTrue(dao.routineStretchesOnce().isEmpty())
    }

    @Test fun `P2 and row 19 the same result twice is one unchanged session, and after a delete it comes back once`() = runBlocking {
        seed()
        val result = stretchResult(run("am", 1, 50.0), day)!!
        repo.recordStretchSession(result); repo.recordStretchSession(result)
        assertEquals(listOf(result), dao.stretchSessionsOnce())
        repo.deleteStretchSession(result.id) // History's delete
        repo.recordStretchSession(result); repo.recordStretchSession(result)
        assertEquals(listOf(result), dao.stretchSessionsOnce())
        repo.importJson(backup(stretches = listOf(fold)), replace = true)
        repo.recordStretchSession(result); repo.recordStretchSession(result)
        assertEquals(listOf(result), dao.stretchSessionsOnce())
        repo.resetToSeed()
        repo.recordStretchSession(result); repo.recordStretchSession(result)
        assertEquals(listOf(result), dao.stretchSessionsOnce())
    }

    @Test fun `P3 a run skipped through before 30 s is no session`() {
        assertNull(stretchResult(run("am", 0, 29.9), day))
        assertNotNull(stretchResult(run("am", 0, 30.0), day))
        assertNotNull(stretchResult(run("am", 1, 3.0), day))
    }

    @Test fun `P4 a restored snapshot whose routine was deleted is dropped`() = runBlocking {
        seed()
        val snapshot = run("am", 1, 50.0)
        assertTrue(stillOpen(repo, snapshot))
        repo.deleteRoutine("am")
        assertFalse(stillOpen(repo, snapshot)) // the service drops it and records nothing
        assertTrue(stillOpen(repo, run("pm", 1, 50.0)))
        repo.recordStretchSession(stretchResult(run("pm", 1, 50.0), day)!!)
        assertFalse(stillOpen(repo, run("pm", 1, 50.0))) // already recorded
    }

    // ---- W1, W4: eating window settings

    @Test fun `W1 On from a sheet opened before an import changes only the switch`() = runBlocking {
        dao.upsert(EatingWindow()) // the sheet holds 12:00-20:00, off
        merge(backup(window = EatingWindow(startMinute = 10 * 60, endMinute = 18 * 60, remindBeforeMin = 30)))
        repo.setWindowEnabled(true)
        assertEquals(EatingWindow(enabled = true, startMinute = 10 * 60, endMinute = 18 * 60, remindBeforeMin = 30), dao.eatingWindowOnce())
        repo.setWindowRemind(5)
        assertEquals(5, dao.eatingWindowOnce()!!.remindBeforeMin)
    }

    @Test fun `W1 of two crossing hour edits the first is written and the second refused, in both orders`() = runBlocking {
        dao.upsert(EatingWindow())
        assertTrue(repo.setWindowStart(19 * 60))
        assertFalse(repo.setWindowEnd(13 * 60))
        assertEquals(19 * 60 to 20 * 60, dao.eatingWindowOnce()!!.let { it.startMinute to it.endMinute })
        dao.upsert(EatingWindow())
        assertTrue(repo.setWindowEnd(13 * 60))
        assertFalse(repo.setWindowStart(19 * 60))
        assertEquals(12 * 60 to 13 * 60, dao.eatingWindowOnce()!!.let { it.startMinute to it.endMinute })
    }

    @Test fun `W4 the fixed hours never cross midnight`() = runBlocking {
        assertEquals(0, EatingWindowRules.startRange(20 * 60).first)
        assertEquals(24 * 60 - 1, EatingWindowRules.endRange(12 * 60).last)
        assertFalse(repo.setWindowEnd(24 * 60))
        assertFalse(repo.setWindowStart(-15))
        assertEquals(EatingWindow(), dao.eatingWindowOnce()) // the default row was created, the hours kept
    }
}
