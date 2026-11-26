package dev.mirzohidkhon.khonfitness.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

val MAX_HR_RANGE = 100..230

/**
 * The typed maximum heart rate, in DataStore preferences (key `max_heart_rate`): no table, so no schema change, and not
 * in the backup. A missing key, a value of another type, or a number outside [MAX_HR_RANGE] reads as none.
 */
class MaxHeartRate(private val store: DataStore<Preferences>) {
    val typed: Flow<Int?> = store.data.map { (it.asMap()[KEY] as? Int)?.takeIf { v -> v in MAX_HR_RANGE } }
        .catch { emit(null) }.distinctUntilChanged()

    /** Stores [bpm], or removes the typed value for null. A number outside [MAX_HR_RANGE] stores nothing. */
    suspend fun set(bpm: Int?): Boolean {
        if (bpm != null && bpm !in MAX_HR_RANGE) return false
        store.edit { if (bpm == null) it.remove(KEY) else it[KEY] = bpm }
        return true
    }

    companion object { val KEY = intPreferencesKey("max_heart_rate") }
}
