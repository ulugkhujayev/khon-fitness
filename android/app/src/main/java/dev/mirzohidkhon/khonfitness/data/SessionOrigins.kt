package dev.mirzohidkhon.khonfitness.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Where a running program session came from: its program, its start time, and the program's [fingerprint] at the start
 * (or after its last "Update the program too"). A swap updates the program only while all three still hold
 * ([KhonDao.swapExercise]).
 */
@Serializable
data class SessionOrigin(val programId: String, val startedAt: Long, val fingerprint: String)

/**
 * The origin of each program session, in DataStore preferences (key `session_origin_<sessionId>`): no table, so no
 * schema change, and not in the backup. A missing or unreadable key means "no origin": the swap then changes the
 * session only. Every call runs under the repository's origin lock ([KhonRepository]); each awaits its edit.
 */
class SessionOrigins(private val store: DataStore<Preferences>) {
    /**
     * Any failure is "no origin": a store that cannot be read, a value of another type under the key (a typed get
     * would throw ClassCastException), or text that does not decode.
     */
    suspend fun read(sessionId: String): SessionOrigin? = try {
        (store.data.first().asMap()[key(sessionId)] as? String)?.let { Json.decodeFromString<SessionOrigin>(it) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /**
     * False when the store fails: the edit is atomic, so nothing is written. Callers run after their Room commit, so a
     * failure must not escape; every state it leaves is one the swap check refuses (no origin, an old fingerprint, or a
     * finished or missing session behind a stale key).
     */
    suspend fun write(sessionId: String, origin: SessionOrigin): Boolean = edit { it[key(sessionId)] = Json.encodeToString(origin) }

    /** False when the store fails, as [write]. */
    suspend fun remove(sessionId: String): Boolean = edit { it.remove(key(sessionId)) }

    private suspend fun edit(change: (MutablePreferences) -> Unit): Boolean = try {
        store.edit(change)
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
    }

    /**
     * Every session's origin goes, in one edit: an import or a reset makes none of them trustworthy. Unlike [write]
     * and [remove] it throws on failure, so an import or a reset stops before its Room writes.
     */
    suspend fun clearAll() {
        store.edit { p ->
            @Suppress("UNCHECKED_CAST")
            p.asMap().keys.filter { it.name.startsWith(PREFIX) }.forEach { p.remove(it as Preferences.Key<Any>) }
        }
    }

    companion object {
        const val PREFIX = "session_origin_"
        fun key(sessionId: String) = stringPreferencesKey(PREFIX + sessionId)
    }
}

/**
 * A program as a session starts from it, read in one transaction ([KhonDao.programSnapshot]): blocks by (sortOrder,
 * id), each block's rows by (slot, id), rows whose exercise is gone left out. A position in these lists is a set log's
 * `blockIndex` and `slot`. [fingerprint] covers the raw rows, the left-out ones too ([programFingerprint]).
 */
data class ProgramSnapshot(val detail: ProgramDetail, val fingerprint: String)

/**
 * A stable hash of a program's raw blocks and rows, before any filter: the program id, every Block column per block in
 * (sortOrder, id) order, every BlockExercise column per row in (slot, id) order. Any change to them changes it.
 */
fun programFingerprint(programId: String, blocks: List<Block>, rows: List<BlockExercise>): String {
    val text = StringBuilder("program\u0000").append(programId)
    val byBlock = rows.groupBy { it.blockId }
    for (b in blocks.sortedWith(compareBy({ it.sortOrder }, { it.id }))) {
        text.append("\u0001block\u0000").append(b.id).append('\u0000').append(b.programId).append('\u0000').append(b.sortOrder).append('\u0000').append(b.superset)
        for (r in byBlock[b.id].orEmpty().sortedWith(compareBy({ it.slot }, { it.id }))) {
            text.append("\u0001row\u0000").append(r.id).append('\u0000').append(r.blockId).append('\u0000').append(r.slot).append('\u0000').append(r.exerciseId)
                .append('\u0000').append(r.sets).append('\u0000').append(r.minReps).append('\u0000').append(r.maxReps)
        }
    }
    return MessageDigest.getInstance("SHA-256").digest(text.toString().toByteArray()).joinToString("") { "%02x".format(it) }
}

/** What a swap did: [ProgramUpdated] carries the program's new fingerprint; [SessionOnly] says whether it was refused. */
sealed interface SwapResult {
    data class SessionOnly(val refused: Boolean) : SwapResult
    data class ProgramUpdated(val fingerprint: String) : SwapResult
}

/** The line a refused "Update the program too" shows. */
const val SWAP_REFUSED = "The program changed after this workout started. Only this workout changed."

/** The cardio form's Save ([KhonDao.saveCardioEntry]): saved, or refused with the line the form shows. */
sealed interface CardioSaveResult {
    data object Saved : CardioSaveResult
    data class Refused(val message: String) : CardioSaveResult
}

/** What a new cardio entry needs before Save: a time, a distance, or laps above 0. Null when it has one. */
fun cardioMissing(s: Session): String? =
    if ((s.timeSec ?: 0) > 0 || (s.distanceM ?: 0) > 0 || (s.laps ?: 0) > 0) null else "Enter time or distance"
