package dev.mirzohidkhon.khonfitness.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

val BODY_TARGET_KG: ClosedRange<BigDecimal> = BigDecimal("20.0")..BigDecimal("300.0")
val LIFT_TARGET_KG: ClosedRange<BigDecimal> = BigDecimal("1.0")..BigDecimal("500.0")
val LIFT_TARGET_REPS: ClosedRange<BigDecimal> = BigDecimal.ONE..BigDecimal(500)

/** The exact mean of [count] entries that add up to [sum] kg. */
data class EntryAverage(val sum: BigDecimal, val count: Int)

/** [start] is the 7-day average (else the latest entry) when the target was typed, null without an entry then. */
data class BodyTarget(val kg: BigDecimal, val start: EntryAverage?, val date: LocalDate)

/** [value] is an estimated 1RM in kg, or reps when [reps]. */
data class LiftTarget(val value: BigDecimal, val reps: Boolean, val date: LocalDate)

private fun BigDecimal.scaleAtMost(n: Int) = stripTrailingZeros().scale() <= n

/** [v] without trailing zeros: 100.50 is "100.5", 100.0 is "100". */
fun plain(v: BigDecimal): String = v.stripTrailingZeros().let { if (it.scale() < 0) it.setScale(0) else it }.toPlainString()

fun bodyTargetValid(kg: BigDecimal): Boolean = kg in BODY_TARGET_KG && kg.scaleAtMost(1)

fun liftTargetValid(value: BigDecimal, reps: Boolean): Boolean =
    if (reps) value in LIFT_TARGET_REPS && value.scaleAtMost(0)
    else value in LIFT_TARGET_KG && (value * BigDecimal(2)).scaleAtMost(0)

const val MAX_ENTRY_COUNT = 1000

/** An entry average from 20 to 300 kg, of 1 to [MAX_ENTRY_COUNT] entries. */
fun entryAverageValid(a: EntryAverage): Boolean =
    a.count in 1..MAX_ENTRY_COUNT && a.sum.signum() > 0 && BigDecimal(a.count).let { n -> a.sum >= BODY_TARGET_KG.start * n && a.sum <= BODY_TARGET_KG.endInclusive * n }

private val STORED_NUMBER = Regex("""[0-9]+(\.[0-9]+)?""")
private val STORED_COUNT = Regex("""[0-9]+""")
private const val MAX_DATE_CHARS = 16
private const val MAX_STORED_CHARS = 12

private fun storedNumber(text: String): BigDecimal? = text.takeIf { it.length <= MAX_STORED_CHARS && STORED_NUMBER.matches(it) }?.let(::BigDecimal)
private fun storedCount(text: String): Int? = text.takeIf { it.length <= MAX_STORED_CHARS && STORED_COUNT.matches(it) }?.toBigInteger()?.takeIf { it <= MAX_ENTRY_COUNT.toBigInteger() }?.toInt()
private fun storedDate(text: String): LocalDate? = text.takeIf { it.length <= MAX_DATE_CHARS }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

/**
 * The typed targets, in the settings DataStore as `target_bodyweight` and `target_lift.<exercise id>`: no table, so
 * no schema change, and not in the backup. A missing key, a broken text, or a value outside its limits reads as none:
 * each number is a plain decimal of at most 12 characters, so no stored text can throw or make the arithmetic long.
 * A bodyweight target is `kg|sum|count|date`, or `kg|average|date` as first stored (its average reads as exact).
 */
class Targets(private val store: DataStore<Preferences>) {
    val body: Flow<BodyTarget?> = store.data.map { readBody(it.asMap()[BODY_KEY] as? String) }.catch { emit(null) }.distinctUntilChanged()

    fun lift(exerciseId: String): Flow<LiftTarget?> =
        store.data.map { readLift(it.asMap()[liftKey(exerciseId)] as? String) }.catch { emit(null) }.distinctUntilChanged()

    private val writes = Mutex()

    /** One write at a time, in the order of the calls, off the caller's thread. */
    private suspend fun write(change: (MutablePreferences) -> Unit) {
        writes.withLock { withContext(Dispatchers.IO) { store.edit { change(it) } } }
    }

    /**
     * Stores [kg], or removes the target for null. A value outside its limits stores nothing. With [keepEqual], a stored
     * target of the same value stays as it is, with its entry average and date.
     */
    suspend fun setBody(kg: BigDecimal?, start: EntryAverage?, date: LocalDate, keepEqual: Boolean = false): Boolean {
        if (kg != null && !bodyTargetValid(kg)) return false
        val kept = start?.takeIf { entryAverageValid(it) && storedNumber(it.sum.toPlainString()) != null }
        write {
            if (kg == null) it.remove(BODY_KEY)
            else if (!keepEqual || readBody(it[BODY_KEY])?.kg?.compareTo(kg) != 0)
                it[BODY_KEY] = listOf(kg.setScale(1, RoundingMode.UNNECESSARY).toPlainString(), kept?.sum?.toPlainString().orEmpty(), kept?.count?.toString().orEmpty(), date.toString()).joinToString(SEP)
        }
        return true
    }

    /** As [setBody]; with [keepEqual], a stored target of the same value and unit stays with its date. */
    suspend fun setLift(exerciseId: String, value: BigDecimal?, reps: Boolean, date: LocalDate, keepEqual: Boolean = false): Boolean {
        if (value != null && !liftTargetValid(value, reps)) return false
        val key = liftKey(exerciseId)
        write {
            val old = readLift(it[key])
            if (value == null) it.remove(key)
            else if (!keepEqual || old == null || old.reps != reps || old.value.compareTo(value) != 0)
                it[key] = listOf(if (reps) REPS else KG, plain(value), date.toString()).joinToString(SEP)
        }
        return true
    }

    suspend fun clearAll() {
        write { prefs -> prefs.asMap().keys.filter { it.name == BODY_KEY.name || it.name.startsWith(LIFT_PREFIX) }.forEach { prefs.remove(it) } }
    }

    companion object {
        private const val SEP = "|"
        private const val KG = "kg"
        private const val REPS = "reps"
        private const val LIFT_PREFIX = "target_lift."
        private const val MAX_BODY_CHARS = 64
        val BODY_KEY = stringPreferencesKey("target_bodyweight")
        fun liftKey(exerciseId: String) = stringPreferencesKey(LIFT_PREFIX + exerciseId)

        fun readBody(text: String?): BodyTarget? {
            val parts = text?.takeIf { it.length <= MAX_BODY_CHARS }?.split(SEP) ?: return null
            if (parts.size != 3 && parts.size != 4) return null
            val kg = storedNumber(parts[0])?.takeIf(::bodyTargetValid) ?: return null
            val start = when {
                parts.size == 3 && parts[1].isEmpty() -> null
                parts.size == 3 -> EntryAverage(storedNumber(parts[1]) ?: return null, 1)
                parts[1].isEmpty() && parts[2].isEmpty() -> null
                else -> EntryAverage(storedNumber(parts[1]) ?: return null, storedCount(parts[2]) ?: return null)
            }
            if (start != null && !entryAverageValid(start)) return null
            val date = storedDate(parts.last()) ?: return null
            return BodyTarget(kg, start, date)
        }

        fun readLift(text: String?): LiftTarget? {
            val parts = text?.takeIf { it.length <= MAX_BODY_CHARS }?.split(SEP)?.takeIf { it.size == 3 } ?: return null
            val reps = when (parts[0]) { KG -> false; REPS -> true; else -> return null }
            val value = storedNumber(parts[1])?.takeIf { liftTargetValid(it, reps) } ?: return null
            val date = storedDate(parts[2]) ?: return null
            return LiftTarget(value, reps, date)
        }
    }
}
