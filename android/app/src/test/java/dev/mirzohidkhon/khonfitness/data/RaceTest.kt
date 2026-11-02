package dev.mirzohidkhon.khonfitness.data

import android.app.Application
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The [Race] helper against raw SQL on a real in-memory database: it tells one transaction from two by the statements
 * alone, defers the competing write only inside one transaction, checks only the step, and fails clearly otherwise.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RaceTest {
    private val race = Race()
    private lateinit var db: KhonDatabase
    private val raw: SupportSQLiteDatabase get() = db.openHelper.writableDatabase

    // Where the step is when the competing write runs.
    private var stage = "before the step"
    private var wroteAt = "never"
    private val write: suspend () -> Unit = { wroteAt = stage }

    // Opened at once: Room syncs its tracker triggers on an observer change only while the database is open.
    @Before fun open() { db = raceDatabase(race); read("day_overrides") }

    @After fun close() = db.close()

    private fun read(table: String) = raw.query("SELECT * FROM $table").close()
    private fun insertOverride(note: String = "rest") =
        raw.execSQL("INSERT OR REPLACE INTO day_overrides (date, itemType, itemId, auto) VALUES ('2026-11-03', ?, NULL, 0)", arrayOf(note))

    /** Runs [body] in one raw transaction, by [exclusive] BEGIN or not, and commits it. */
    private fun transaction(exclusive: Boolean = false, body: () -> Unit) {
        if (exclusive) raw.beginTransaction() else raw.beginTransactionNonExclusive()
        try { body(); raw.setTransactionSuccessful() } finally { raw.endTransaction() }
    }

    private fun assertNotOneTransaction(block: () -> Unit) {
        val e = assertThrows(IllegalStateException::class.java) { block() }
        assertTrue(e.message!!, e.message!!.startsWith("the step is not one transaction"))
    }

    @Test fun `two transactions fail at a read in the second`() {
        assertNotOneTransaction {
            race.step({ it == "SELECT * FROM sessions" }, write) {
                transaction { read("day_overrides") }
                stage = "between transactions"
                transaction(exclusive = true) { read("sessions") }
            }
        }
        assertEquals("never", wroteAt)
    }

    @Test fun `two transactions let the write land between them at the second BEGIN`() {
        race.step(firstWriteAfterRead(), write) {
            transaction { read("day_overrides") }
            stage = "between transactions"
            transaction { insertOverride() }
            stage = "after the step"
        }
        assertEquals("between transactions", wroteAt)
    }

    @Test fun `a second transaction after a deferred write match fails`() {
        assertNotOneTransaction {
            race.step(firstWriteAfterRead("day_overrides"), write) {
                transaction { read("day_overrides"); insertOverride() }
                transaction { insertOverride() }
            }
        }
    }

    @Test fun `a second transaction after a deferred read match fails`() {
        assertNotOneTransaction {
            race.step({ it == "SELECT * FROM sessions" }, write) {
                transaction { read("day_overrides"); read("sessions") }
                transaction { insertOverride() }
            }
        }
    }

    @Test fun `a second transaction whose values hold the tracker table name still fails`() {
        assertNotOneTransaction {
            race.step(firstWriteAfterRead("day_overrides"), write) {
                transaction { read("day_overrides"); insertOverride() }
                // The name in the SQL text itself, where the helper looks; a bound value would never reach it.
                transaction { raw.execSQL("INSERT OR REPLACE INTO day_overrides (date, itemType, itemId, auto) VALUES ('2026-11-03', 'room_table_modification_log', NULL, 0)") }
            }
        }
    }

    private val observer = object : InvalidationTracker.Observer("day_overrides") { override fun onInvalidated(tables: Set<String>) {} }

    /** One app transaction next to a transaction of Room's tracker alone ([change] adds or removes an observer). */
    private fun trackerThenStep(at: (String) -> Boolean, change: () -> Unit) {
        race.step(at, write) {
            change()
            transaction { read("day_overrides"); read("sessions"); insertOverride() }
            stage = "after the step"
        }
        assertEquals("after the step", wroteAt)
    }

    @Test fun `an observer added in the step does not split it at a read match`() =
        trackerThenStep({ it == "SELECT * FROM sessions" }) { db.invalidationTracker.addObserver(observer) }

    @Test fun `an observer added in the step does not split it at a write match`() =
        trackerThenStep(firstWriteAfterRead("day_overrides")) { db.invalidationTracker.addObserver(observer) }

    @Test fun `an observer removed in the step does not split it at a read match`() {
        db.invalidationTracker.addObserver(observer)
        trackerThenStep({ it == "SELECT * FROM sessions" }) { db.invalidationTracker.removeObserver(observer) }
    }

    @Test fun `an observer removed in the step does not split it at a write match`() {
        db.invalidationTracker.addObserver(observer)
        trackerThenStep(firstWriteAfterRead("day_overrides")) { db.invalidationTracker.removeObserver(observer) }
    }

    @Test fun `nested transactions inside one outer transaction after the match still defer`() {
        race.step(firstWriteAfterRead("day_overrides"), write) {
            transaction {
                read("day_overrides")
                insertOverride()
                transaction { insertOverride() }
                transaction { insertOverride() }
            }
            stage = "after the step"
        }
        assertEquals("after the step", wroteAt)
    }

    @Test fun `one transaction with nested BEGIN statements defers the write`() {
        race.step(firstWriteAfterRead(), write) {
            transaction(exclusive = true) {
                read("day_overrides")
                transaction { insertOverride() }
                stage = "in the outer transaction"
            }
            assertEquals("never", wroteAt)
            stage = "after the step"
        }
        assertEquals("after the step", wroteAt)
    }

    @Test fun `one transaction of plain statements matches the write and defers it`() {
        race.step(firstWriteAfterRead("day_overrides"), write) {
            transaction {
                read("day_overrides")
                stage = "between read and write"
                insertOverride()
            }
            assertEquals("never", wroteAt)
            stage = "after the step"
        }
        assertEquals("after the step", wroteAt)
    }

    @Test fun `reads after the step are not checked`() {
        race.step(firstWriteAfterRead("day_overrides"), write) {
            transaction { read("day_overrides"); insertOverride() }
            stage = "after the step"
        }
        read("day_overrides")
        insertOverride()
        assertEquals("after the step", wroteAt)
    }

    @Test fun `no transaction runs the write at the match`() {
        race.step(firstWriteAfterRead("day_overrides"), write) {
            read("day_overrides")
            stage = "between read and write"
            insertOverride()
            stage = "after the step"
        }
        assertEquals("between read and write", wroteAt)
    }

    @Test fun `a write to the given table matches without a read and a write to another table does not`() {
        race.step(firstWriteAfterRead("window_days"), write) {
            transaction {
                insertOverride()
                stage = "before the window write"
                raw.execSQL("INSERT INTO window_days (date, openedAt, closedAt, note) VALUES ('2026-11-03', 1, NULL, '')")
            }
            assertEquals("never", wroteAt)
            stage = "after the step"
        }
        assertEquals("after the step", wroteAt)
    }

    @Test fun `a match that never comes fails the test`() {
        assertThrows(AssertionError::class.java) {
            race.step({ it == "SELECT * FROM sessions" }, write) { transaction { read("day_overrides") } }
        }
        assertEquals("never", wroteAt)
    }
}
