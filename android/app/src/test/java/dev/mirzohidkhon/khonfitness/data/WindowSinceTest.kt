package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.ui.components.StripCell
import dev.mirzohidkhon.khonfitness.ui.components.stripSummary
import dev.mirzohidkhon.khonfitness.ui.components.windowStrip
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * The eating window counts its history only from the day it was first turned on (rows 11-13): stored once in DataStore
 * (`window_since`), with the earliest window day (else today) for a user who has none, outside the backup.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class WindowSinceTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var db: KhonDatabase
    private lateinit var dao: KhonDao
    private lateinit var since: WindowSince
    private lateinit var repo: KhonRepository
    private lateinit var scope: CoroutineScope
    private val today = LocalDate.parse("2026-11-03")

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java).build()
        dao = db.dao()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        since = WindowSince(PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "settings.preferences_pb") })
        repo = KhonRepository(dao, since)
    }

    @After fun close() { scope.cancel(); db.close() }

    private fun stored() = runBlocking { repo.windowSince.first() }

    @Test fun `turned on today for the first time, the strip counts no day and says since when`() {
        val strip = windowStrip(emptyList(), today, since = today)
        assertEquals(List(27) { StripCell.BEFORE } + StripCell.TODAY, strip)
        assertEquals("Tracking since Nov 3.", stripSummary(strip, today))
    }

    @Test fun `days before the enable date are not counted`() {
        val start = today.minusDays(5)
        val days = listOf(WindowDay(today.minusDays(2).iso(), openedAt = 1L), WindowDay(today.minusDays(9).iso(), openedAt = 1L))
        val strip = windowStrip(days, today, since = start)
        assertEquals(22, strip.count { it == StripCell.BEFORE })
        assertEquals("Kept the hours on 4 of 5 days.", stripSummary(strip, start))
    }

    @Test fun `the summary never reads 0 of 0 days`() {
        for (s in listOf(null, today, today.minusDays(40))) {
            for (cells in listOf(listOf(StripCell.TODAY), List(27) { StripCell.BEFORE } + StripCell.TODAY)) {
                assertFalse(stripSummary(cells, s).contains("0 of 0"))
            }
        }
    }

    @Test fun `without a stored date the start is the earliest window day, else today`() {
        assertEquals(LocalDate.parse("2026-09-30"), windowSinceFallback(listOf(WindowDay("2026-10-04"), WindowDay("2026-09-30")), today))
        assertEquals(today, windowSinceFallback(emptyList(), today))
    }

    @Test fun `turning the window on stores today once, and off and on again keeps the first date`() = runBlocking {
        assertNull(stored())
        repo.setWindowEnabled(true, today)
        assertEquals(today, stored())
        repo.setWindowEnabled(false, today.plusDays(3))
        repo.setWindowEnabled(true, today.plusDays(5))
        assertEquals(today, stored())
        assertTrue(dao.eatingWindowOnce()!!.enabled)
    }

    @Test fun `an existing user with the window on gets the earliest window day, stored once`() = runBlocking {
        dao.setWindowEnabled(true)
        dao.upsertWindowDays(listOf(WindowDay("2026-10-12", openedAt = 1L), WindowDay("2026-10-01", closedAt = 1L)))
        assertEquals(LocalDate.parse("2026-10-01"), repo.keepWindowSince(today))
        dao.upsertWindowDays(listOf(WindowDay("2026-09-01", openedAt = 1L)))
        assertEquals(LocalDate.parse("2026-10-01"), repo.keepWindowSince(today))
        assertEquals(LocalDate.parse("2026-10-01"), stored())
    }

    @Test fun `the date is not in the backup, and an import brings the fallback back`() = runBlocking {
        repo.setWindowEnabled(true, today)
        val exported = repo.exportJson()
        assertFalse(exported.contains("window_since"))
        assertFalse(exported.contains("2026-11-03"))
        val backup = Json.decodeFromString<Backup>(exported).copy(windowDays = listOf(WindowDay("2026-08-20", openedAt = 1L)))
        repo.importJson(Json.encodeToString(backup), replace = true)
        assertNull(stored())
        assertEquals(LocalDate.parse("2026-08-20"), repo.keepWindowSince(today))
    }

    @Test fun `a repository without the store still turns the window on`() = runBlocking {
        val plain = KhonRepository(dao)
        plain.setWindowEnabled(true, today)
        assertTrue(dao.eatingWindowOnce()!!.enabled)
        assertNull(plain.keepWindowSince(today))
    }
}
