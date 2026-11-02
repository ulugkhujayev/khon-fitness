package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The exercise and modality editors' Done and Archive against a real in-memory database, while an import or a
 * session writes the same row: each writes only what the user changed, onto the row as it is then.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class EditorWritesTest {
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var repo: KhonRepository

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        repo = KhonRepository(dao)
    }

    @After fun close() = db.close()

    private val bench = Exercise("bench", "Bench press", Kind.STRENGTH, primaryMuscle = "chest", stepKg = 2.5)
    private val bike = Modality("bike", "Bike", 0xFFB39DFF)

    /** Merges a backup that holds only these rows, as Settings' Import does. */
    private suspend fun import(exercises: List<Exercise> = emptyList(), modalities: List<Modality> = emptyList()) = repo.importJson(
        Json.encodeToString(Backup(exercises = exercises, modalities = modalities, programs = emptyList(), blocks = emptyList(), blockExercises = emptyList(),
            sessions = emptyList(), setLogs = emptyList(), intervalLogs = emptyList(), bodyweights = emptyList(), weekPlan = emptyList(), overrides = emptyList())),
        replace = false,
    )

    // ---- exercise

    @Test fun `an import while the editor is open survives Done of an unrelated edit`() = runBlocking {
        dao.upsert(bench)
        val opened = dao.exercise("bench")!!
        import(exercises = listOf(bench.copy(notes = "Seat 4")))
        repo.saveExerciseEdits(opened, opened.copy(stepKg = 5.0))
        assertEquals(bench.copy(notes = "Seat 4", stepKg = 5.0), dao.exercise("bench"))
    }

    @Test fun `Done writes the field the user changed over the imported value`() = runBlocking {
        dao.upsert(bench)
        val opened = dao.exercise("bench")!!
        import(exercises = listOf(bench.copy(name = "Flat bench")))
        repo.saveExerciseEdits(opened, opened.copy(name = "Barbell bench"))
        assertEquals("Barbell bench", dao.exercise("bench")!!.name)
    }

    @Test fun `Archive keeps a rename that landed after the editor opened`() = runBlocking {
        dao.upsert(bench)
        dao.exercise("bench")!! // what the editor holds
        import(exercises = listOf(bench.copy(name = "Flat bench")))
        repo.setExerciseArchived("bench", true)
        assertEquals(bench.copy(name = "Flat bench", archived = true), dao.exercise("bench"))
        repo.setExerciseArchived("bench", false)
        assertEquals(bench.copy(name = "Flat bench"), dao.exercise("bench"))
    }

    @Test fun `a kind change is refused when a session started using the exercise after the editor opened`() = runBlocking {
        dao.upsert(bench)
        val opened = dao.exercise("bench")!! // unused: the Kind row is open
        dao.upsert(Session(id = "s1", date = "2026-11-02", startedAt = 1L, itemType = ItemType.PROGRAM, programId = "push", programName = "Push"))
        dao.upsert(SetLog(id = "l1", sessionId = "s1", blockIndex = 0, blockSuperset = false, slot = 0, setIndex = 0, order = 0, exerciseId = "bench", exerciseName = "Bench press", minReps = 8, maxReps = 12, stepKg = 2.5))
        repo.saveExerciseEdits(opened, opened.copy(kind = Kind.CARDIO, modalityId = "bike", notes = "Slow down"))
        val row = dao.exercise("bench")!!
        assertEquals(Kind.STRENGTH, row.kind)
        assertEquals("Slow down", row.notes) // the other edits still land
    }

    @Test fun `a kind change goes through while nothing uses the exercise`() = runBlocking {
        dao.upsert(bench)
        val opened = dao.exercise("bench")!!
        repo.saveExerciseEdits(opened, opened.copy(kind = Kind.CARDIO, modalityId = "bike"))
        assertEquals(Kind.CARDIO, dao.exercise("bench")!!.kind)
    }

    @Test fun `Done on an exercise deleted meanwhile writes nothing`() = runBlocking {
        dao.upsert(bench)
        val opened = dao.exercise("bench")!!
        repo.deleteExercise("bench")
        repo.saveExerciseEdits(opened, opened.copy(name = "Barbell bench"))
        assertNull(dao.exercise("bench"))
    }

    @Test fun `a new exercise is written whole`() = runBlocking {
        repo.saveExerciseEdits(null, bench)
        assertEquals(bench, dao.exercise("bench"))
    }

    // ---- modality

    @Test fun `an import while the modality editor is open survives Done of a rename`() = runBlocking {
        dao.upsert(bike)
        val opened = dao.modality("bike")!!
        import(modalities = listOf(bike.copy(color = 0xFF71B7FF)))
        repo.saveModalityEdits(opened, opened.copy(name = "Cycling"))
        assertEquals(Modality("bike", "Cycling", 0xFF71B7FF), dao.modality("bike"))
    }

    @Test fun `modality Archive keeps a rename that landed after the editor opened`() = runBlocking {
        dao.upsert(bike)
        dao.modality("bike")!!
        import(modalities = listOf(bike.copy(name = "Cycling")))
        repo.setModalityArchived("bike", true)
        assertEquals(Modality("bike", "Cycling", 0xFFB39DFF, archived = true), dao.modality("bike"))
    }

    @Test fun `Done on a modality deleted meanwhile writes nothing`() = runBlocking {
        dao.upsert(bike)
        val opened = dao.modality("bike")!!
        dao.clearModalities()
        repo.saveModalityEdits(opened, opened.copy(name = "Cycling"))
        assertNull(dao.modality("bike"))
    }
}
