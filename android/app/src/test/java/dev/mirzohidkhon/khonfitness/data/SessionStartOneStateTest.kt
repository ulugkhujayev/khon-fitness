package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Attack case 10: a block edit that lands while a session start reads the program, between its blocks and its rows,
 * waits for the start's transaction ([Race]). The logs and the origin's fingerprint come from one database state, so
 * the edit then makes "Update the program too" refuse.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SessionStartOneStateTest {
    @get:Rule val tmp = TemporaryFolder()
    private val race = Race()
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var scope: CoroutineScope
    private lateinit var origins: SessionOrigins
    private lateinit var repo: KhonRepository

    @Before fun open() {
        db = raceDatabase(race)
        dao = db.dao()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        origins = SessionOrigins(PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "settings.preferences_pb") })
        repo = KhonRepository(dao, null, origins)
    }

    @After fun close() { scope.cancel(); db.close() }

    @Test fun `a block edit between the start's program reads waits, and the logs and fingerprint are from before it`() = runBlocking {
        dao.upsertExercises(listOf(Exercise("a", "A", Kind.STRENGTH), Exercise("c", "C", Kind.STRENGTH)))
        dao.upsertPrograms(listOf(Program("p1", "Push", active = true)))
        dao.upsertBlocks(listOf(Block("k0", "p1", 0)))
        dao.upsertBlockExercises(listOf(BlockExercise("r0", "k0", 0, "a", sets = 3)))
        val before = dao.programSnapshot("p1")!!.fingerprint
        var s: Session? = null
        // A raw write, as another writer's: it lands at once unless a transaction holds the database.
        race.step({ it.startsWith("SELECT * FROM block_exercises WHERE blockId IN") }, { db.openHelper.writableDatabase.execSQL("UPDATE block_exercises SET sets = 5 WHERE id = 'r0'") }) {
            s = repo.startProgramSession("p1")
        }
        val session = s!!
        assertEquals(3, dao.setLogsOnce(session.id).size)
        assertEquals(before, origins.read(session.id)!!.fingerprint)
        assertEquals(5, dao.blockExercisesOnce().single().sets)
        val log = dao.setLogsOnce(session.id).first()
        assertEquals(SwapResult.SessionOnly(refused = true), repo.swapExerciseInSession(log, Exercise("c", "C", Kind.STRENGTH), updateProgram = true))
        assertEquals("a", dao.blockExercisesOnce().single().exerciseId)
    }
}
