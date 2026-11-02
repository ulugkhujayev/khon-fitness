package dev.mirzohidkhon.khonfitness.data

import android.app.Application
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
 * alone, defers the competing write only inside one transaction, and fails clearly otherwise.
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

    @Before fun open() { db = raceDatabase(race) }

    @After fun close() = db.close()

    private fun read(table: String) = raw.query("SELECT * FROM $table").close()
    private fun insertOverride() = raw.execSQL("INSERT OR REPLACE INTO day_overrides (date, itemType, itemId, auto) VALUES ('2026-11-03', 'rest', NULL, 0)")

    /** Runs [body] in one raw transaction, by [exclusive] BEGIN or not, and commits it. */
    private fun transaction(exclusive: Boolean = false, body: () -> Unit) {
        if (exclusive) raw.beginTransaction() else raw.beginTransactionNonExclusive()
        try { body(); raw.setTransactionSuccessful() } finally { raw.endTransaction() }
    }

    @Test fun `two transactions fail at a read in the second`() {
        race.arm({ it == "SELECT * FROM sessions" }, write)
        transaction { read("day_overrides") }
        stage = "between transactions"
        val e = assertThrows(IllegalStateException::class.java) { transaction(exclusive = true) { read("sessions") } }
        assertTrue(e.message!!, e.message!!.startsWith("the step is not one transaction"))
        race.finish()
        assertEquals("never", wroteAt)
    }

    @Test fun `two transactions let the write land between them at the second BEGIN`() {
        race.arm(firstWriteAfterRead(), write)
        transaction { read("day_overrides") }
        stage = "between transactions"
        transaction { insertOverride() }
        stage = "after the step"
        race.finish()
        assertEquals("between transactions", wroteAt)
    }

    @Test fun `one transaction with nested BEGIN statements defers the write`() {
        race.arm(firstWriteAfterRead(), write)
        transaction(exclusive = true) {
            read("day_overrides")
            transaction { insertOverride() }
            stage = "in the outer transaction"
        }
        stage = "after the step"
        assertEquals("never", wroteAt)
        race.finish()
        assertEquals("after the step", wroteAt)
    }

    @Test fun `one transaction of plain statements matches the write and defers it`() {
        race.arm(firstWriteAfterRead("day_overrides"), write)
        transaction {
            read("day_overrides")
            stage = "between read and write"
            insertOverride()
        }
        stage = "after the step"
        assertEquals("never", wroteAt)
        race.finish()
        assertEquals("after the step", wroteAt)
    }

    @Test fun `no transaction runs the write at the match`() {
        race.arm(firstWriteAfterRead("day_overrides"), write)
        read("day_overrides")
        stage = "between read and write"
        insertOverride()
        stage = "after the step"
        race.finish()
        assertEquals("between read and write", wroteAt)
    }

    @Test fun `a write to the given table matches without a read and a write to another table does not`() {
        race.arm(firstWriteAfterRead("window_days"), write)
        transaction {
            insertOverride()
            stage = "before the window write"
            raw.execSQL("INSERT INTO window_days (date, openedAt, closedAt, note) VALUES ('2026-11-03', 1, NULL, '')")
        }
        stage = "after the step"
        assertEquals("never", wroteAt)
        race.finish()
        assertEquals("after the step", wroteAt)
    }

    @Test fun `a match that never comes fails the test`() {
        race.arm({ it == "SELECT * FROM sessions" }, write)
        transaction { read("day_overrides") }
        assertThrows(AssertionError::class.java) { race.finish() }
        assertEquals("never", wroteAt)
    }
}
