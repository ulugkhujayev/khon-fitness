package dev.mirzohidkhon.khonfitness.data

import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What [HeartFill.rereadHeartRate] did. [DROPPED]: the read ended, but its session, range, row, or data changed meanwhile. */
enum class HrReread { FILLED, WAITING, NONE, FAILED, NO_RANGE, DROPPED }

const val FOREGROUND_GAP_MS = 10 * 60_000L

/**
 * The heart rate reader of the app: it writes only `session_hr`. One runner: a request while a pass runs makes it run
 * once more after it ends, and no caller waits for it. One provider read at a time, for the runner and the explicit
 * calls alike. [generation] is the repository's ([KhonRepository.generation]).
 */
class HeartFill(
    private val dao: KhonDao,
    private val source: HeartSource,
    private val scope: CoroutineScope,
    private val generation: () -> Long,
    private val clocks: Clocks = SystemClocks,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val runner by lazy {
        scope.launch {
            for (r in requests) {
                try {
                    pass()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Unit
                }
                passCount.value++
            }
        }
    }
    private val readLock = Mutex()
    private val rereads = mutableMapOf<String, CompletableDeferred<HrReread>>()
    private var lastForeground: Long? = null

    private val askedCount = MutableStateFlow(0)
    val asked: StateFlow<Int> = askedCount

    private val passCount = MutableStateFlow(0)
    /** The number of passes ended since the start. */
    val passes: StateFlow<Int> = passCount

    /** Runs after a pass chose a stale row and before it deletes it; a test holds the cleanup here. */
    internal var beforeCleanup: suspend (SessionHr) -> Unit = {}

    /** Asks for a pass and returns at once. */
    fun fillHeartRate() {
        askedCount.update { it + 1 }
        runner
        requests.trySend(Unit)
    }

    /** The app came to the foreground: a pass, at most once per [FOREGROUND_GAP_MS] of elapsed real time. */
    fun foreground() {
        val now = clocks.elapsedMs()
        synchronized(this) {
            val last = lastForeground
            if (last != null && now - last in 0 until FOREGROUND_GAP_MS) return
            lastForeground = now
        }
        fillHeartRate()
    }

    /**
     * Reads [sessionId] now, at any age and from any row, `off` and `none` too; the old row stays until a read commits.
     * Two calls for one session at once share one read.
     */
    suspend fun rereadHeartRate(sessionId: String): HrReread {
        var mine = false
        val shared = synchronized(rereads) {
            rereads.getOrPut(sessionId) { mine = true; CompletableDeferred() }
        }
        if (!mine) return shared.await()
        try {
            val result = read(sessionId, auto = false)
            shared.complete(result)
            return result
        } catch (e: Throwable) {
            shared.completeExceptionally(e)
            throw e
        } finally {
            synchronized(rereads) { rereads.remove(sessionId) }
        }
    }

    /** "Remove" ([KhonDao.removeSessionHr]): the row becomes `off`, so no pass reads it and a running read cannot commit. */
    suspend fun removeHeartRate(sessionId: String): Boolean = dao.removeSessionHr(sessionId, zone, clocks.wallMs())

    /**
     * One pass over finished sessions: first every row that is not `off` and not for its session's range goes, checked
     * only in its delete with the zone read there; then each session whose range ended in the last 30 days and that
     * has no row or a `waiting` row is read.
     */
    internal suspend fun pass() {
        val sessions = dao.sessionsOnce().associateBy { it.id }
        for (row in dao.sessionHrOnce()) {
            if (row.state == HrState.OFF) continue
            beforeCleanup(row)
            dao.deleteStaleSessionHr(row, zone)
        }
        val rows = dao.sessionHrOnce().associateBy { it.sessionId }
        val now = clocks.wallMs()
        val zone = zone()
        sessions.values
            .filter { s -> hrWindow(s, zone)?.let { fillDue(it.toMs, now) } == true && rows[s.id].let { it == null || it.state == HrState.WAITING } }
            .sortedBy { it.startedAt }
            .forEach { read(it.id, auto = true) }
    }

    /** One read of [sessionId] under the read lock and its commit ([KhonDao.commitSessionHr]); a failed read writes nothing. */
    private suspend fun read(sessionId: String, auto: Boolean): HrReread = readLock.withLock {
        val began = generation()
        val session = dao.session(sessionId) ?: return@withLock HrReread.NO_RANGE
        val range = hrWindow(session, zone()) ?: return@withLock HrReread.NO_RANGE
        val from = dao.sessionHr(sessionId)
        if (auto && (from != null && from.state != HrState.WAITING || !fillDue(range.toMs, clocks.wallMs()))) return@withLock HrReread.DROPPED
        val records = try {
            source.records(range)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withLock HrReread.FAILED
        }
        val row = hrRow(sessionId, range, readHeart(records, range), clocks.wallMs(), from)
        val wrote = try {
            dao.commitSessionHr(row, from, zone) { generation() == began }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withLock HrReread.FAILED
        }
        when {
            !wrote -> HrReread.DROPPED
            row.state == HrState.FILLED -> HrReread.FILLED
            row.state == HrState.WAITING -> HrReread.WAITING
            else -> HrReread.NONE
        }
    }
}
