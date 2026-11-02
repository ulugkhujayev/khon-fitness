package dev.mirzohidkhon.khonfitness.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.fail
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Runs a competing write at the first statement of a step that a test matches, as another writer on the phone would.
 * [step] marks where the step starts and ends; only statements inside it are checked. The helper reads only the
 * statements the query callback reports ([onQuery]) and counts the transaction depth from them: Room reports
 * `BEGIN EXCLUSIVE TRANSACTION`, `BEGIN DEFERRED TRANSACTION` and `END TRANSACTION` before each takes effect.
 *
 * The step is one transaction while its first statement is a BEGIN at depth 0 and the depth has not come back to 0
 * since. At the matched statement:
 * - depth 0 (a BEGIN being reported is not open yet): nothing holds the other writer back, so the write runs now, to
 *   the end, on another thread, before the matched statement;
 * - depth 1 or more and one transaction: the other writer waits for the commit, so the write runs when the step ends;
 * - depth 1 or more and not one transaction: it throws "the step is not one transaction".
 * After a deferred match, any later statement of the step outside its transaction throws the same. Room's own tracker
 * statements ([isTracker], matched exactly) and the transaction boundaries are not statements of the step.
 *
 * No timing decides the outcome. Build the database with [raceDatabase], so Room's own statements run in order.
 */
internal class Race {
    private var depth = 0
    private var active = false
    private var at: ((String) -> Boolean)? = null
    private var write: (suspend () -> Unit)? = null
    private var first = false
    private var oneTransaction = false
    private var deferred: (suspend () -> Unit)? = null
    private var ran = false
    private var ended = false

    /**
     * Runs [body] as the step: [write] lands at the first statement [at] matches, as described on [Race]. Statements
     * after [body] returns (the test's own reads) are not checked. Fails when the matched statement never ran.
     */
    fun step(at: (String) -> Boolean, write: suspend () -> Unit, body: suspend () -> Unit) {
        synchronized(this) {
            this.at = at; this.write = write; first = true; oneTransaction = false; deferred = null; ran = false; ended = false
            active = true
        }
        val held = try { runBlocking { body() }; synchronized(this) { deferred } } finally { synchronized(this) { active = false; this.at = null } }
        if (!ran) fail("the competing write never ran")
        held?.let { runBlocking { it() } }
    }

    fun onQuery(sql: String) {
        val w = synchronized(this) {
            val before = depth
            if (sql.startsWith("BEGIN")) depth++ else if (sql.startsWith("END TRANSACTION")) depth--
            if (!active || isTracker(sql)) return
            if (deferred != null) {
                if (depth == 0) ended = true
                check(!(ended && !isBoundary(sql))) { "the step is not one transaction: \"$sql\" ran after the step's transaction ended" }
                return
            }
            val match = at ?: return
            if (first) { first = false; oneTransaction = before == 0 && sql.startsWith("BEGIN") } else if (depth == 0) oneTransaction = false
            if (!match(sql)) return
            at = null
            ran = true
            when {
                before == 0 -> write!!
                oneTransaction -> { deferred = write; return }
                else -> throw IllegalStateException("the step is not one transaction: \"$sql\" ran at depth $before after the step's first transaction ended")
            }
        }
        var failure: Throwable? = null
        val t = Thread { runCatching { runBlocking { w() } }.onFailure { failure = it } }
        t.start(); t.join(HANG_GUARD_MS)
        check(!t.isAlive) { "the competing write hung" }
        failure?.let { throw it }
    }

    private companion object { const val HANG_GUARD_MS = 60_000L }
}

/** Room 2.6.1's invalidation tracker statements, exactly as `InvalidationTracker` writes them (prefixes). */
private val TRACKER = listOf(
    "SELECT * FROM room_table_modification_log WHERE invalidated = 1",
    "UPDATE room_table_modification_log SET invalidated = 0 WHERE invalidated = 1",
    "INSERT OR IGNORE INTO room_table_modification_log VALUES(",
    "CREATE TEMP TABLE room_table_modification_log ",
    "CREATE TEMP TRIGGER IF NOT EXISTS `room_table_modification_trigger_",
    "DROP TRIGGER IF EXISTS `room_table_modification_trigger_",
    "PRAGMA recursive_triggers='ON'",
    "PRAGMA temp_store = MEMORY",
)

internal fun isTracker(sql: String) = TRACKER.any { sql.startsWith(it) }

private fun isBoundary(sql: String) = listOf("BEGIN", "END TRANSACTION", "TRANSACTION SUCCESSFUL").any { sql.startsWith(it) }

private val WRITES = listOf("BEGIN", "INSERT", "UPDATE", "DELETE")

/**
 * The first BEGIN, INSERT, UPDATE or DELETE after a SELECT (of [table] when given): the gap between a step's read and
 * its write. It matches with or without nested BEGIN statements. With a [table], the first INSERT, UPDATE or DELETE of
 * it also matches without a read before it: a step that only writes the table has no gap, and the write then shows
 * whether it runs in one transaction. Room's tracker statements are never matched (the [Race] skips them).
 */
internal fun firstWriteAfterRead(table: String? = null): (String) -> Boolean {
    var read = false
    return { sql ->
        when {
            sql.startsWith("SELECT") -> { if (table == null || table in sql) read = true; false }
            WRITES.none { sql.startsWith(it) } -> false
            else -> read || (table != null && !sql.startsWith("BEGIN") && table in sql)
        }
    }
}

/**
 * An in-memory database whose statements feed [race]. Queries run on the calling thread, so Room's invalidation
 * tracker runs its own transaction right after each commit, on that thread, and never in the middle of a step.
 */
internal fun raceDatabase(race: Race): KhonDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), KhonDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor(Executor { it.run() })
        .setTransactionExecutor(Executors.newCachedThreadPool { r -> Thread(r, "race-transaction").apply { isDaemon = true } })
        .setQueryCallback({ sql, _ -> race.onQuery(sql) }, { it.run() })
        .build()
