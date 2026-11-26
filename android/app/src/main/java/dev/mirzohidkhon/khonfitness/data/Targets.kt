package dev.mirzohidkhon.khonfitness.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate

val BODY_TARGET_KG: ClosedRange<BigDecimal> = BigDecimal("20.0")..BigDecimal("300.0")
val LIFT_TARGET_KG: ClosedRange<BigDecimal> = BigDecimal("1.0")..BigDecimal("500.0")
val LIFT_TARGET_REPS: ClosedRange<BigDecimal> = BigDecimal.ONE..BigDecimal(500)

/** [startAvg] is the 7-day average (else the latest entry) when the target was typed, null without an entry then. */
data class BodyTarget(val kg: BigDecimal, val startAvg: BigDecimal?, val date: LocalDate)

/** [value] is an estimated 1RM in kg, or reps when [reps]. */
data class LiftTarget(val value: BigDecimal, val reps: Boolean, val date: LocalDate)

private fun BigDecimal.scaleAtMost(n: Int) = stripTrailingZeros().scale() <= n

fun bodyTargetValid(kg: BigDecimal): Boolean = kg in BODY_TARGET_KG && kg.scaleAtMost(1)

fun liftTargetValid(value: BigDecimal, reps: Boolean): Boolean =
    if (reps) value in LIFT_TARGET_REPS && value.scaleAtMost(0)
    else value in LIFT_TARGET_KG && (value * BigDecimal(2)).scaleAtMost(0)

/**
 * The typed targets, in the settings DataStore as `target_bodyweight` and `target_lift.<exercise id>`: no table, so
 * no schema change, and not in the backup. A missing key, a broken text, or a value outside its limits reads as none.
 */
class Targets(private val store: DataStore<Preferences>) {
    val body: Flow<BodyTarget?> = store.data.map { readBody(it.asMap()[BODY_KEY] as? String) }.catch { emit(null) }.distinctUntilChanged()

    fun lift(exerciseId: String): Flow<LiftTarget?> =
        store.data.map { readLift(it.asMap()[liftKey(exerciseId)] as? String) }.catch { emit(null) }.distinctUntilChanged()

    /** Stores [kg], or removes the target for null. A value outside its limits stores nothing. */
    suspend fun setBody(kg: BigDecimal?, startAvg: BigDecimal?, date: LocalDate): Boolean {
        if (kg != null && !bodyTargetValid(kg)) return false
        store.edit { if (kg == null) it.remove(BODY_KEY) else it[BODY_KEY] = listOf(kg.toPlainString(), startAvg?.toPlainString().orEmpty(), date.toString()).joinToString(SEP) }
        return true
    }

    suspend fun setLift(exerciseId: String, value: BigDecimal?, reps: Boolean, date: LocalDate): Boolean {
        if (value != null && !liftTargetValid(value, reps)) return false
        val key = liftKey(exerciseId)
        store.edit { if (value == null) it.remove(key) else it[key] = listOf(if (reps) REPS else KG, value.toPlainString(), date.toString()).joinToString(SEP) }
        return true
    }

    suspend fun clearAll() {
        store.edit { prefs -> prefs.asMap().keys.filter { it.name == BODY_KEY.name || it.name.startsWith(LIFT_PREFIX) }.forEach { prefs.remove(it) } }
    }

    companion object {
        private const val SEP = "|"
        private const val KG = "kg"
        private const val REPS = "reps"
        private const val LIFT_PREFIX = "target_lift."
        val BODY_KEY = stringPreferencesKey("target_bodyweight")
        fun liftKey(exerciseId: String) = stringPreferencesKey(LIFT_PREFIX + exerciseId)

        fun readBody(text: String?): BodyTarget? {
            val parts = text?.split(SEP)?.takeIf { it.size == 3 } ?: return null
            val kg = parts[0].toBigDecimalOrNull()?.takeIf(::bodyTargetValid) ?: return null
            val avg = if (parts[1].isEmpty()) null else parts[1].toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return null
            val date = runCatching { LocalDate.parse(parts[2]) }.getOrNull() ?: return null
            return BodyTarget(kg, avg, date)
        }

        fun readLift(text: String?): LiftTarget? {
            val parts = text?.split(SEP)?.takeIf { it.size == 3 } ?: return null
            val reps = when (parts[0]) { KG -> false; REPS -> true; else -> return null }
            val value = parts[1].toBigDecimalOrNull()?.takeIf { liftTargetValid(it, reps) } ?: return null
            val date = runCatching { LocalDate.parse(parts[2]) }.getOrNull() ?: return null
            return LiftTarget(value, reps, date)
        }
    }
}
