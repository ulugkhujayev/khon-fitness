package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.runtime.mutableStateOf
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

/**
 * The cardio form writes only the interval fields the user typed: an import while the form is open (a round added or
 * removed, the other field changed) stays, and a typed field wins even when typed back to its first value or cleared.
 */
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

    /** Seeds an interval exercise with session c1 and [rounds], then opens the form and waits for "100". */
    private fun openForm(rounds: List<IntervalLog>) {
        io {
            dao.upsert(Exercise("bike", "Bike", Kind.CARDIO, intervals = true, rounds = rounds.size, fields = "time,watts"))
            dao.upsert(Session("c1", "2026-11-02", 1000L, itemType = ItemType.CARDIO, exerciseId = "bike", exerciseName = "Bike"))
            dao.upsertIntervalLogs(rounds)
        }
        compose.setContent { KhonTheme { CardioScreen(vm, rememberNavController(), "c1") } }
        shows("100")
    }

    /**
     * Types [new] over the field showing [old], then lets a frame pass: the field's input session takes the typed text
     * only on the next frame, and a focused field that leaves the screen before it reports its old text once more.
     */
    private fun type(old: String, new: String) { compose.onNodeWithText(old).performTextReplacement(new); compose.waitForIdle() }

    private fun shows(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun save() {
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5_000) { runBlocking(Dispatchers.IO) { dao.session("c1")?.finishedAt != null } }
    }

    private fun saved(id: String) = runBlocking(Dispatchers.IO) { dao.intervalLogsFor("c1").first { it.id == id } }

    /** Opens the form on rounds [rounds], types 150 over the first round's 100, then runs [meanwhile] and saves. */
    private fun editThenSave(rounds: List<IntervalLog>, meanwhile: suspend () -> Unit, settled: String) {
        openForm(rounds)
        type("100", "150")
        io { meanwhile() }
        shows(settled)
        save()
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

    @Test fun `a heart rate edit keeps watts imports from before and after it`() {
        openForm(listOf(IntervalLog("i1", "c1", 0, avgHr = 100, watts = 200)))
        io { dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c1", 0, avgHr = 100, watts = 220))) }
        shows("220")
        type("100", "150")
        io { dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c1", 0, avgHr = 100, watts = 240), IntervalLog("i2", "c1", 1, avgHr = 120, watts = 260))) }
        shows("Round 2")
        save()
        assertEquals(IntervalLog("i1", "c1", 0, avgHr = 150, watts = 240), saved("i1"))
    }

    @Test fun `a watts edit keeps heart rate imports from before and after it`() {
        openForm(listOf(IntervalLog("i1", "c1", 0, avgHr = 100, watts = 200)))
        io { dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c1", 0, avgHr = 120, watts = 200))) }
        shows("120")
        type("200", "250")
        io { dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c1", 0, avgHr = 130, watts = 200), IntervalLog("i2", "c1", 1, avgHr = 140, watts = 260))) }
        shows("Round 2")
        save()
        assertEquals(IntervalLog("i1", "c1", 0, avgHr = 130, watts = 250), saved("i1"))
    }

    @Test fun `a field typed back to its first value is still written`() {
        openForm(listOf(IntervalLog("i1", "c1", 0, avgHr = 100, watts = 200)))
        type("100", "150")
        type("150", "100")
        io { dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c1", 0, avgHr = 130, watts = 210), IntervalLog("i2", "c1", 1, avgHr = 140, watts = 260))) }
        shows("Round 2")
        save()
        assertEquals(IntervalLog("i1", "c1", 0, avgHr = 100, watts = 210), saved("i1"))
    }

    @Test fun `a cleared field writes null and keeps the other field's imports`() {
        openForm(listOf(IntervalLog("i1", "c1", 0, avgHr = 100, watts = 200)))
        io { dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c1", 0, avgHr = 120, watts = 200))) }
        shows("120")
        type("200", "")
        io { dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c1", 0, avgHr = 130, watts = 200), IntervalLog("i2", "c1", 1, avgHr = 140, watts = 260))) }
        shows("Round 2")
        save()
        assertEquals(IntervalLog("i1", "c1", 0, avgHr = 130, watts = null), saved("i1"))
    }

    @Test fun `an edited round deleted before Save is not created again`() {
        openForm(listOf(IntervalLog("i1", "c1", 0, avgHr = 100, watts = 200)))
        type("100", "150")
        io { dao.deleteIntervalLogsFor("c1") }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Round 1").fetchSemanticsNodes().isEmpty() }
        save()
        assertEquals(emptyList<IntervalLog>(), runBlocking(Dispatchers.IO) { dao.intervalLogsFor("c1") })
    }

    @Test fun `another session id starts without the previous interval edits`() {
        io {
            dao.upsert(Exercise("bike", "Bike", Kind.CARDIO, intervals = true, rounds = 1, fields = "time,watts"))
            dao.upsert(Session("c1", "2026-11-02", 1000L, itemType = ItemType.CARDIO, exerciseId = "bike", exerciseName = "Bike"))
            dao.upsert(Session("c2", "2026-11-02", 2000L, itemType = ItemType.CARDIO, exerciseId = "bike", exerciseName = "Bike"))
            dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c1", 0, avgHr = 100, watts = 200), IntervalLog("j1", "c2", 0, avgHr = 120, watts = 220)))
        }
        val id = mutableStateOf("c1")
        compose.setContent { KhonTheme { CardioScreen(vm, rememberNavController(), id.value) } }
        shows("100")
        type("100", "150")
        compose.runOnIdle { id.value = "c2" }
        shows("120")
        type("120", "160")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(5_000) { runBlocking(Dispatchers.IO) { dao.session("c2")?.finishedAt != null } }
        io {
            assertEquals(IntervalLog("i1", "c1", 0, avgHr = 100, watts = 200), dao.intervalLogsFor("c1").single())
            assertEquals(IntervalLog("j1", "c2", 0, avgHr = 160, watts = 220), dao.intervalLogsFor("c2").single())
        }
    }
}
