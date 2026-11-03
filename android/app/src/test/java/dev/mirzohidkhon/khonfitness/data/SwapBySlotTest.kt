package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A swap with "Update the program too" (rows 5, 6 and attack cases 1-12): the program row at the set's snapshot
 * position changes only while the session's origin holds, that is while the program is exactly as the session started
 * from it (or as its last update left it). Any change refuses, and then only the session changes. Real DAO in memory,
 * real DataStore on disk.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SwapBySlotTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var scope: CoroutineScope
    private lateinit var store: GatedStore
    private lateinit var origins: SessionOrigins
    private lateinit var repo: KhonRepository

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        openStore()
    }

    /** The app state as a fresh process builds it: a new DataStore on the same file and a new repository. */
    private fun openStore() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        store = GatedStore(PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "settings.preferences_pb") })
        origins = SessionOrigins(store)
        repo = KhonRepository(dao, null, origins)
    }

    private fun restart() { runBlocking { scope.coroutineContext.job.cancelAndJoin() }; openStore() }

    @After fun close() { scope.cancel(); db.close() }

    private fun strength(id: String) = Exercise(id, id.uppercase(), Kind.STRENGTH, stepKg = 2.5)

    /** Program p1: block 0 holds A then X, block 1 holds A again. */
    private suspend fun seed() {
        dao.upsertExercises(listOf("a", "b", "c", "d", "x").map(::strength))
        dao.upsertPrograms(listOf(Program("p1", "Push", active = true), Program("p2", "Pull", sortOrder = 1)))
        dao.upsertBlocks(listOf(Block("k0", "p1", 0), Block("k1", "p1", 1), Block("q0", "p2", 0)))
        dao.upsertBlockExercises(listOf(
            BlockExercise("r0a", "k0", 0, "a"), BlockExercise("r0b", "k0", 1, "x"), BlockExercise("r1a", "k1", 0, "a"), BlockExercise("q0a", "q0", 0, "a"),
        ))
    }

    private suspend fun start(): Session { seed(); return repo.startProgramSession("p1")!! }

    private suspend fun firstLog(s: Session, block: Int, slot: Int) = dao.setLogsOnce(s.id).first { it.blockIndex == block && it.slot == slot }
    private suspend fun row(id: String) = dao.blockExercisesOnce().first { it.id == id }
    private suspend fun sessionExercises(s: Session, block: Int, slot: Int) =
        dao.setLogsOnce(s.id).filter { it.blockIndex == block && it.slot == slot }.map { it.exerciseId }.toSet()
    private suspend fun swap(s: Session, block: Int, slot: Int, to: String, update: Boolean = true) =
        repo.swapExerciseInSession(firstLog(s, block, slot), strength(to), updateProgram = update)
    private val refused = SwapResult.SessionOnly(refused = true)

    @Test fun `row 5 - A to B for this session, then B to C for the program too, puts C in that program row only`() = runBlocking {
        val s = start()
        assertEquals(SwapResult.SessionOnly(refused = false), swap(s, 0, 0, "b", update = false))
        assertEquals("a", row("r0a").exerciseId)
        assertTrue(swap(s, 0, 0, "c") is SwapResult.ProgramUpdated)
        assertEquals("c", row("r0a").exerciseId)
        // The other rows with A, the other slot and the other program stay.
        assertEquals("a", row("r1a").exerciseId)
        assertEquals("x", row("r0b").exerciseId)
        assertEquals("a", row("q0a").exerciseId)
        assertEquals(setOf("c"), sessionExercises(s, 0, 0))
        assertEquals(setOf("a"), sessionExercises(s, 1, 0))
    }

    @Test fun `case 5 - after an update and a restart, the next update still lands in the same row`() = runBlocking {
        val s = start()
        swap(s, 0, 0, "b", update = false)
        swap(s, 0, 0, "c")
        restart()
        assertTrue(swap(s, 0, 0, "d") is SwapResult.ProgramUpdated)
        assertEquals("d", row("r0a").exerciseId)
    }

    @Test fun `row 6 - a block gone since the start refuses, the session still swaps, the program keeps what it has`() = runBlocking {
        val s = start()
        dao.deleteBlockFully("k1")
        assertEquals(refused, swap(s, 1, 0, "c"))
        assertEquals(setOf("c"), sessionExercises(s, 1, 0))
        assertEquals(listOf("a", "a", "x"), dao.blockExercisesOnce().sortedBy { it.id }.map { it.exerciseId })
    }

    @Test fun `row 6 - a deleted program refuses and does not crash`() = runBlocking {
        val s = start()
        dao.deleteProgramFully("p1")
        assertEquals(refused, swap(s, 0, 0, "c"))
        assertEquals("c", firstLog(s, 0, 0).exerciseId)
    }

    @Test fun `case 1 - blocks reordered since the start refuse`() = runBlocking {
        val s = start()
        dao.moveBlock("k1", -1)
        assertEquals(refused, swap(s, 0, 0, "c"))
        assertEquals(setOf("c"), sessionExercises(s, 0, 0))
        assertEquals(listOf("a", "x", "a"), listOf(row("r0a"), row("r0b"), row("r1a")).map { it.exerciseId })
    }

    @Test fun `case 2 - the row's exercise changed in the editor since the start refuses`() = runBlocking {
        val s = start()
        dao.setSlotExercise("k0", 0, "b", "new")
        assertEquals(refused, swap(s, 0, 0, "c"))
        assertEquals("b", row("r0a").exerciseId)
    }

    @Test fun `case 3 - a block or a row removed and added again at the same position refuses, the new row unchanged`() = runBlocking {
        val s = start()
        dao.deleteBlockFully("k1")
        dao.createBlock(Block("k1new", "p1", 1), BlockExercise("r1new", "k1new", 0, "a"))
        assertEquals(refused, swap(s, 1, 0, "c"))
        assertEquals("a", row("r1new").exerciseId)

        val s2 = repo.startProgramSession("p1")!!
        dao.deleteBlockExercise("r0b")
        dao.setSlotExercise("k0", 1, "x", "r0bnew")
        assertEquals(refused, swap(s2, 0, 1, "c"))
        assertEquals("x", row("r0bnew").exerciseId)
    }

    @Test fun `case 4 - duplicate exercises and a slot gap - exactly the row at the snapshot position changes`() = runBlocking {
        dao.upsertExercises(listOf("a", "c").map(::strength))
        dao.upsertPrograms(listOf(Program("p1", "Push", active = true)))
        dao.upsertBlocks(listOf(Block("k0", "p1", 0), Block("k1", "p1", 1)))
        // Block 1's only stored row has slot 1: its sets are slot 0 of the session.
        dao.upsertBlockExercises(listOf(BlockExercise("r0a", "k0", 0, "a"), BlockExercise("r0b", "k0", 1, "a"), BlockExercise("r1", "k1", 1, "a")))
        val s = repo.startProgramSession("p1")!!
        assertTrue(swap(s, 1, 0, "c") is SwapResult.ProgramUpdated)
        assertEquals(listOf("a", "a", "c"), listOf(row("r0a"), row("r0b"), row("r1")).map { it.exerciseId })
    }

    @Test fun `case 6 - two sessions from one program - after an update in the first, the second refuses`() = runBlocking {
        val first = start()
        val second = repo.startProgramSession("p1")!!
        assertTrue(swap(first, 0, 0, "c") is SwapResult.ProgramUpdated)
        assertEquals(refused, swap(second, 0, 0, "d"))
        assertEquals("c", row("r0a").exerciseId)
    }

    @Test fun `case 7 - an import in either mode, or a reset, while a session runs leaves no usable origin`() = runBlocking {
        val backup = run { seed(); repo.exportJson() }
        for (replace in listOf(false, true)) {
            val s = repo.startProgramSession("p1")!!
            repo.importJson(backup, replace)
            assertNull(origins.read(s.id))
            // A replace import removes the session itself; a merge import keeps it, and the swap changes it only.
            if (!replace) assertEquals(refused, swap(s, 0, 0, "c"))
            assertEquals("a", row("r0a").exerciseId)
        }
        val s = repo.startProgramSession("p1")!!
        repo.resetToSeed()
        assertNull(origins.read(s.id))
        assertTrue(store.data.first().asMap().keys.none { it.name.startsWith(SessionOrigins.PREFIX) })
    }

    @Test fun `case 7 - an import waits for a start whose origin write is pending, then removes that origin too`() = runBlocking {
        val backup = run { seed(); repo.exportJson() }
        val gate = store.holdNextEdit()
        val starting = async(Dispatchers.IO) { repo.startProgramSession("p1")!! }
        withTimeout(10_000) { gate.entered.await() } // the session is in Room, its origin write held
        val importing = async(Dispatchers.IO) { repo.importJson(backup, replace = false) }
        delay(300)
        assertFalse("the import must wait for the origin lock", importing.isCompleted)
        gate.release.complete(Unit)
        val s = starting.await()
        importing.await()
        assertNull(origins.read(s.id))
        assertEquals(refused, swap(s, 0, 0, "c"))
        assertEquals("a", row("r0a").exerciseId)
    }

    @Test fun `case 8 - a missing or unreadable origin refuses, and finish or discard removes only its own origin`() = runBlocking {
        val s = start()
        store.edit { it.remove(SessionOrigins.key(s.id)) }
        restart()
        assertEquals(refused, swap(s, 0, 0, "c"))
        store.edit { it[SessionOrigins.key(s.id)] = "{not json" }
        assertEquals(refused, swap(s, 0, 0, "c"))
        assertEquals("a", row("r0a").exerciseId)

        val kept = repo.startProgramSession("p1")!!
        val finished = repo.startProgramSession("p1")!!
        val discarded = repo.startProgramSession("p1")!!
        repo.finishSession(finished.id)
        repo.deleteSession(discarded.id)
        assertNull(origins.read(finished.id))
        assertNull(origins.read(discarded.id))
        assertNotNull(origins.read(kept.id))
        // A finished session refuses even with an origin put back.
        origins.write(finished.id, origins.read(kept.id)!!.copy(startedAt = finished.startedAt))
        assertEquals(refused, swap(finished, 0, 0, "c"))
    }

    @Test fun `case 9 - tied block order and slots, and a row whose exercise is gone - selection follows (sortOrder, id) and (slot, id)`() = runBlocking {
        dao.upsertExercises(listOf("a", "b", "c", "x").map(::strength))
        dao.upsertPrograms(listOf(Program("p1", "Push", active = true)))
        // Both blocks at sortOrder 0: "ka" comes first. In "ka", r0 (exercise gone) drops; r1 and r2 tie at slot 0.
        dao.upsertBlocks(listOf(Block("kb", "p1", 0), Block("ka", "p1", 0)))
        dao.upsertBlockExercises(listOf(
            BlockExercise("r2", "ka", 0, "b"), BlockExercise("r1", "ka", 0, "a"), BlockExercise("r0", "ka", 0, "gone"), BlockExercise("rb", "kb", 0, "x"),
        ))
        val s = repo.startProgramSession("p1")!!
        assertEquals(setOf("a"), sessionExercises(s, 0, 0))
        assertEquals(setOf("b"), sessionExercises(s, 0, 1))
        assertEquals(setOf("x"), sessionExercises(s, 1, 0))
        assertTrue(swap(s, 0, 1, "c") is SwapResult.ProgramUpdated)
        assertEquals(listOf("gone", "a", "c", "x"), listOf(row("r0"), row("r1"), row("r2"), row("rb")).map { it.exerciseId })
    }

    @Test fun `case 11 - only sets or rep limits changed since the start refuse`() = runBlocking {
        val s = start()
        dao.setBlockExerciseSets("r0b", 5)
        assertEquals(refused, swap(s, 0, 0, "c"))
        val s2 = repo.startProgramSession("p1")!!
        dao.setBlockExerciseMinReps("r1a", 6)
        assertEquals(refused, swap(s2, 0, 0, "c"))
        val s3 = repo.startProgramSession("p1")!!
        dao.setBlockExerciseMaxReps("r1a", 15)
        assertEquals(refused, swap(s3, 0, 0, "c"))
        assertEquals("a", row("r0a").exerciseId)
    }

    @Test fun `case 12 - a row whose exercise is gone still counts - a change to its sets refuses`() = runBlocking {
        seed()
        dao.upsertBlockExercises(listOf(BlockExercise("r1gone", "k1", 1, "gone")))
        val s = repo.startProgramSession("p1")!!
        dao.setBlockExerciseSets("r1gone", 5)
        assertEquals(refused, swap(s, 0, 0, "c"))
        assertEquals("a", row("r0a").exerciseId)
    }

    @Test fun `the origin holds the program, the start time and the fingerprint of the snapshot the logs came from`() = runBlocking {
        val s = start()
        val origin = origins.read(s.id)!!
        assertEquals(SessionOrigin("p1", s.startedAt, dao.programSnapshot("p1")!!.fingerprint), origin)
        assertEquals(programFingerprint("p1", dao.blocksOnce().filter { it.programId == "p1" }, dao.blockExercisesOnce().filter { it.blockId.startsWith("k") }), origin.fingerprint)
    }

    @Test fun `without an origin store, Update the program too changes the session only`() = runBlocking {
        val bare = KhonRepository(dao)
        seed()
        val s = bare.startProgramSession("p1")!!
        assertEquals(refused, bare.swapExerciseInSession(firstLog(s, 0, 0), strength("c"), updateProgram = true))
        assertEquals("a", row("r0a").exerciseId)
        assertEquals(setOf("c"), sessionExercises(s, 0, 0))
    }
}

/**
 * A DataStore whose next edit waits after [holdNextEdit] until its gate opens: an origin write held in flight. Every
 * other edit and every read go straight through.
 */
class GatedStore(private val inner: DataStore<Preferences>) : DataStore<Preferences> {
    class Gate { val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>() }
    @Volatile private var next: Gate? = null
    fun holdNextEdit(): Gate = Gate().also { next = it }
    override val data = inner.data
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val gate = synchronized(this) { next.also { next = null } }
        if (gate != null) { gate.entered.complete(Unit); gate.release.await() }
        return inner.updateData(transform)
    }
}
