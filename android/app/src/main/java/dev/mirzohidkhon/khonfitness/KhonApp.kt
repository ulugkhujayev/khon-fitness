package dev.mirzohidkhon.khonfitness

import android.app.Application
import dev.mirzohidkhon.khonfitness.data.KhonDatabase
import dev.mirzohidkhon.khonfitness.data.KhonRepository
import dev.mirzohidkhon.khonfitness.data.WindowSince
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.mirzohidkhon.khonfitness.widget.TodayWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class KhonApp : Application() {
    val db: KhonDatabase by lazy { KhonDatabase.build(this) }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** App settings outside the database and the backup ([dev.mirzohidkhon.khonfitness.data.WindowSince]); one store per process. */
    val settings: DataStore<Preferences> by lazy { PreferenceDataStoreFactory.create(scope = scope) { preferencesDataStoreFile("settings") } }
    val repo: KhonRepository by lazy { KhonRepository(db.dao(), WindowSince(settings)) }

    override fun onCreate() {
        super.onCreate()
        scope.launch { repo.seedIfEmpty() }
        scope.launch {
            // The widget must follow data changes even when Today navigates away immediately.
            combine(repo.planData, repo.unfinishedSession, repo.stretchSessions) { _, _, _ -> Unit }
                .collect { TodayWidget.refresh(this@KhonApp) }
        }
    }
}
