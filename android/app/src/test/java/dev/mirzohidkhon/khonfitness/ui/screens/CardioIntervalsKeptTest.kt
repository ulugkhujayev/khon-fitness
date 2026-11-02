package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.IntervalLog
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The cardio form keeps an unsaved interval edit when an import adds or removes a round while it is open. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CardioIntervalsKeptTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val dao get() = app.db.dao()

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() { app.scope.cancel(); app.db.close() }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }

    /** Opens the form on rounds [rounds], types 150 over the first round's 100, then runs [meanwhile] and saves. */
    private fun editThenSave(rounds: List<IntervalLog>, meanwhile: suspend () -> Unit, settled: String) {
        io {
            dao.upsert(Exercise("bike", "Bike", Kind.CARDIO, intervals = true, rounds = rounds.size, fields = "time,watts"))
            dao.upsert(Session("c1", "2026-11-02", 1000L, itemType = ItemType.CARDIO, exerciseId = "bike", exerciseName = "Bike"))
            dao.upsertIntervalLogs(rounds)
        }
        compose.setContent { KhonTheme { CardioScreen(vm, rememberNavController(), "c1") } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("100").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("100").performTextReplacement("150")
        io { meanwhile() }
        compose.waitUntil(5_000) { compose.onAllNodesWithText(settled).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5_000) { runBlocking(Dispatchers.IO) { dao.session("c1")?.finishedAt != null } }
    }

    @Test fun `a round added by an import keeps the typed heart rate`() {
        editThenSave(listOf(IntervalLog("i1", "c1", 0, avgHr = 100, watts = 200)),
            { dao.upsertIntervalLogs(listOf(IntervalLog("i2", "c1", 1, avgHr = 120, watts = 220))) }, settled = "Round 2")
        val saved = runBlocking(Dispatchers.IO) { dao.intervalLogsFor("c1") }
        assertEquals(listOf(150, 120), saved.map { it.avgHr })
    }

    @Test fun `a round removed by an import keeps the typed heart rate and stays removed`() {
        editThenSave(listOf(IntervalLog("i1", "c1", 0, avgHr = 100, watts = 200), IntervalLog("i2", "c1", 1, avgHr = 120, watts = 220)),
            { dao.deleteIntervalLogsFor("c1"); dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c1", 0, avgHr = 100, watts = 200))) }, settled = "150")
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Round 2").fetchSemanticsNodes().isEmpty() }
        val saved = runBlocking(Dispatchers.IO) { dao.intervalLogsFor("c1") }
        assertEquals(listOf("i1"), saved.map { it.id })
        assertEquals(150, saved.single().avgHr)
    }
}
