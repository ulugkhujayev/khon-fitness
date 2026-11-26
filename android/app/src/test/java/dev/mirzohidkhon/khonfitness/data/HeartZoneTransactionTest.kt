package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
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

/** Every write that checks a range reads the zone inside its own transaction: the zone changes as Room starts it. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HeartZoneTransactionTest {
    private lateinit var transactions: ExecutorService
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var fill: HeartFill
    private lateinit var repo: KhonRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val heart = FakeHeart { error("provider failed") }
    @Volatile private var zone: ZoneId = TEST_ZONE
    @Volatile private var onTransaction: (() -> Unit)? = null
    private val midnight = HrRange(at(0, 15), at(0, 55))
    private val session = Session("m", localDate(midnight.fromMs, TEST_ZONE), midnight.fromMs, finishedAt = midnight.toMs,
        itemType = ItemType.PROGRAM, timedFrom = midnight.fromMs, timedTo = midnight.toMs)
    private val stored = SessionHr(session.id, midnight.fromMs, midnight.toMs, HrState.FILLED, "0:130", 1, 1, "mi", "mi",
        midnight.fromMs, midnight.fromMs, null, 130, 130, fetchedAt = at(18, 0))

    @Before fun open() {
        transactions = Executors.newSingleThreadExecutor()
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java)
            .setTransactionExecutor { command ->
                transactions.execute {
                    onTransaction?.let { onTransaction = null; it() }
                    command.run()
                }
            }.build()
        dao = db.dao()
        val clocks = FakeClocks(at(19, 0))
        repo = KhonRepository(dao, clocks = clocks, zone = { zone })
        fill = HeartFill(dao, heart, scope, { repo.generation }, clocks, { zone })
    }

    @After fun close() { scope.cancel(); db.close(); transactions.shutdown() }

    @Test fun `the cleanup delete keeps a row that became current as its transaction starts`() = runBlocking {
        dao.upsert(session); dao.upsertSessionHr(stored)
        zone = ZoneOffset.UTC
        assertNull(hrWindow(session, zone))
        var selected = false
        fill.beforeCleanup = { selected = true; onTransaction = { zone = TEST_ZONE } }
        fill.pass()
        assertTrue(selected)
        assertEquals(TEST_ZONE, zone)
        assertEquals(stored, dao.sessionHr(session.id))
        assertEquals(HrFacts.Filled(stored), hrFacts(session, dao.sessionHr(session.id), zone))
    }

    @Test fun `Remove with no row writes nothing when the range goes away as its transaction starts`() = runBlocking {
        dao.upsert(session)
        onTransaction = { zone = ZoneOffset.UTC }
        assertFalse(fill.removeHeartRate(session.id))
        assertEquals(ZoneOffset.UTC, zone)
        assertNull(dao.sessionHr(session.id))
    }

    @Test fun `Remove with no row writes the off row of the range that appears as its transaction starts`() = runBlocking {
        dao.upsert(session)
        zone = ZoneOffset.UTC
        onTransaction = { zone = TEST_ZONE }
        assertTrue(fill.removeHeartRate(session.id))
        assertEquals(SessionHr(session.id, midnight.fromMs, midnight.toMs, HrState.OFF, fetchedAt = at(19, 0)), dao.sessionHr(session.id))
    }

    @Test fun `confirmRange refuses a moved date when the zone changes as its transaction starts`() = runBlocking {
        val bare = session.copy(id = "b", timedFrom = null, timedTo = null)
        dao.upsert(bare)
        assertEquals(null, confirmableRange(bare, TEST_ZONE))
        onTransaction = { zone = ZoneOffset.UTC }
        assertEquals(ConfirmRangeResult.Refused(RangeRefusal.DATE_MOVED), repo.confirmRange(bare.id, bare.startedAt, bare.finishedAt!!))
        assertEquals(bare, dao.session(bare.id))
        zone = ZoneOffset.UTC
        onTransaction = { zone = TEST_ZONE }
        assertEquals(ConfirmRangeResult.Confirmed, repo.confirmRange(bare.id, bare.startedAt, bare.finishedAt!!))
    }

    @Test fun `a fill commit drops the result when the range goes away as its transaction starts`() = runBlocking {
        dao.upsert(session)
        heart.answer = { range -> onTransaction = { zone = ZoneOffset.UTC }; everyMinute(range) }
        assertEquals(HrReread.DROPPED, fill.rereadHeartRate(session.id))
        assertEquals(ZoneOffset.UTC, zone)
        assertNull(dao.sessionHr(session.id))
    }
}
