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

/**
 * Each stretch, routine and window command with a competing write in the gap between its read and its write ([Race]):
 * the command is one transaction, so the other write lands before or after it, never inside, and nothing the command
 * checked can change under it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class StretchRacesTest {
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

    private val fold = Stretch("fold", "Fold", cue = "Hinge")
    private val cat = Stretch("cat", "Cat cow")

    private suspend fun seed() {
        dao.upsertStretches(listOf(fold, cat))
        dao.upsertStretchRoutines(listOf(StretchRoutine("am", "Morning", active = true, sortOrder = 0), StretchRoutine("pm", "Evening", sortOrder = 1)))
    }

    /** A raw write, as an import's, so it can land in the gap of a step that is not one transaction. */
    private fun importStretch(s: Stretch) = db.openHelper.writableDatabase.execSQL(
        "INSERT OR REPLACE INTO stretches (id, name, sided, mode, seconds, reps, figure, muscles, cue, link, builtin, archived) VALUES (?, ?, 0, ?, ?, 0, '', '', ?, '', 0, 0)",
        arrayOf(s.id, s.name, s.mode, s.seconds, s.cue),
    )

    private fun noOrphans() = runBlocking {
        val stretches = dao.stretchesOnce().map { it.id }.toSet(); val routines = dao.stretchRoutinesOnce().map { it.id }.toSet()
        dao.routineStretchesOnce().all { it.stretchId in stretches && it.routineId in routines }
    }

    @Test fun `an import rename during Done is never undone by the held copy`() = runBlocking {
        seed()
        val opened = dao.stretch("fold")!!
        race.step(firstWriteAfterRead("stretches"), { importStretch(fold.copy(name = "Forward fold")) }) { repo.saveStretchEdits(opened, opened.copy(cue = "Long spine")) }
        val row = dao.stretch("fold")
        // Either order is fine: the edit onto the import, or the import after the edit. The opened name never comes back.
        assertTrue("$row", row == fold.copy(name = "Forward fold", cue = "Long spine") || row == fold.copy(name = "Forward fold"))
    }

    @Test fun `a routine row added during a stretch delete leaves no orphan`() = runBlocking {
        seed()
        race.step(firstWriteAfterRead(), { repo.addRoutineStretch("am", "cat", "r1") }) { repo.deleteStretchIfUnused("cat") }
        assertTrue(noOrphans())
        assertTrue((dao.stretch("cat") == null) == dao.routineStretchesOnce().isEmpty())
    }

    @Test fun `a stretch delete during an add leaves no orphan`() = runBlocking {
        seed()
        race.step(firstWriteAfterRead(), { repo.deleteStretchIfUnused("cat") }) { repo.addRoutineStretch("am", "cat", "r1") }
        assertTrue(noOrphans())
        assertTrue((dao.stretch("cat") == null) == dao.routineStretchesOnce().isEmpty())
    }

    @Test fun `a routine delete during an add leaves no orphan`() = runBlocking {
        seed()
        race.step(firstWriteAfterRead(), { repo.deleteRoutine("am") }) { repo.addRoutineStretch("am", "cat", "r1") }
        assertTrue(noOrphans())
    }

    @Test fun `a stretch delete during a create with that first stretch leaves no orphan`() = runBlocking {
        seed()
        race.step(firstWriteAfterRead(), { repo.deleteStretchIfUnused("cat") }) { repo.createRoutine(StretchRoutine("new", "Night"), "cat", "r1") }
        assertTrue(noOrphans())
        assertTrue((dao.stretchRoutine("new") != null) == (dao.stretch("cat") != null))
    }

    @Test fun `a routine created during another create gets its own order`() = runBlocking {
        seed()
        race.step(firstWriteAfterRead("stretch_routines"), { repo.createRoutine(StretchRoutine("b", "B")) }) { repo.createRoutine(StretchRoutine("a", "A")) }
        val orders = dao.stretchRoutinesOnce().map { it.sortOrder }
        assertEquals(orders.distinct(), orders)
    }

    @Test fun `a routine delete during Active leaves at most one active and re-creates nothing`() = runBlocking {
        seed()
        race.step(firstWriteAfterRead("stretch_routines"), { repo.deleteRoutine("pm") }) { repo.setRoutineActive("pm", true) }
        assertTrue(dao.stretchRoutinesOnce().count { it.active } <= 1)
        assertTrue(dao.stretchRoutinesOnce().none { it.id == "pm" })
    }

    @Test fun `a seconds import and a remove during a move keep the order strict and the seconds`() = runBlocking {
        seed()
        dao.upsertRoutineStretches(listOf("A", "B", "C").mapIndexed { i, id -> RoutineStretch(id, "am", "fold", i) })
        race.step(firstWriteAfterRead("routine_stretches"), {
            db.openHelper.writableDatabase.execSQL("UPDATE routine_stretches SET seconds = 90 WHERE id = 'B'")
            repo.removeRoutineStretch("A")
        }) { repo.moveRoutineStretch("C", up = true) }
        val rows = dao.routineStretchesFor("am")
        assertEquals(90, rows.single { it.id == "B" }.seconds)
        assertEquals(rows.map { it.sortOrder }.distinct(), rows.map { it.sortOrder })
        assertEquals(listOf("C", "B"), rows.map { it.id })
    }

    @Test fun `an end edit during a start edit keeps the hours an hour apart`() = runBlocking {
        dao.upsert(EatingWindow())
        race.step(firstWriteAfterRead("eating_window"), { repo.setWindowEnd(13 * 60) }) { repo.setWindowStart(19 * 60) }
        val w = dao.eatingWindowOnce()!!
        assertTrue("$w", w.endMinute - w.startMinute >= 60)
    }

    @Test fun `a start edit during an end edit keeps the hours an hour apart`() = runBlocking {
        dao.upsert(EatingWindow())
        race.step(firstWriteAfterRead("eating_window"), { repo.setWindowStart(19 * 60) }) { repo.setWindowEnd(13 * 60) }
        val w = dao.eatingWindowOnce()!!
        assertTrue("$w", w.endMinute - w.startMinute >= 60)
    }

    @Test fun `D3 an export holds all of a write that landed while it read, or none of it`() = runBlocking {
        seed()
        // After the exercises are read and before the stretches are: an exercise and a stretch arrive together.
        race.step({ it.startsWith("SELECT * FROM stretches") }, {
            db.openHelper.writableDatabase.execSQL("INSERT INTO exercises (id, name, kind, notes, archived, builtin, secondaryMuscles, stepKg, intervals, warmupSec, workSec, restSec, rounds, fields) VALUES ('x', 'X', 'strength', '', 0, 0, '', 2.5, 0, 0, 0, 0, 0, '')")
            importStretch(Stretch("x", "X"))
        }) {
            val b = kotlinx.serialization.json.Json.decodeFromString<Backup>(repo.exportJson())
            assertEquals(b.exercises.any { it.id == "x" }, b.stretches.any { it.id == "x" })
        }
    }
}
