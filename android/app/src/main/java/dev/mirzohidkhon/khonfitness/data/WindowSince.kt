package dev.mirzohidkhon.khonfitness.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The day the eating window was first turned on, in DataStore preferences (key `window_since`): no table, so no schema
 * change, and not in the backup. The window's history counts only days from it. Written once ([keepFirst]); turning
 * the window off and on keeps it. An import or a reset clears it, and the fallback ([windowSinceFallback]) applies again.
 */
class WindowSince(private val store: DataStore<Preferences>) {
    val date: Flow<LocalDate?> = store.data.map { read(it[KEY]) }

    /** Stores [date] unless a date is stored already; returns the stored date. One DataStore edit, so two calls keep one. */
    suspend fun keepFirst(date: LocalDate): LocalDate {
        var kept = date
        store.edit { p -> val stored = read(p[KEY]); if (stored == null) p[KEY] = date.iso() else kept = stored }
        return kept
    }

    suspend fun clear() { store.edit { it.remove(KEY) } }

    private fun read(text: String?): LocalDate? = text?.let { runCatching { it.toDate() }.getOrNull() }

    companion object { val KEY = stringPreferencesKey("window_since") }
}

/** The start date when none is stored (an existing user, or after an import): the earliest window day, else [today]. */
fun windowSinceFallback(days: List<WindowDay>, today: LocalDate): LocalDate = days.minOfOrNull { it.date }?.toDate() ?: today
