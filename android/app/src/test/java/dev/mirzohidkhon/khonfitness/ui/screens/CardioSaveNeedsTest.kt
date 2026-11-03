package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.CardioSaveResult
import dev.mirzohidkhon.khonfitness.data.Edit
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.IntervalEdit
import dev.mirzohidkhon.khonfitness.data.cardioMissing
import dev.mirzohidkhon.khonfitness.data.mergeCardioEdits
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A new cardio entry saves only with a time, a distance or laps (row 7, attack cases 17-20): the form says so and Save
 * is off, and the data layer checks the same rule again when Save runs ([dev.mirzohidkhon.khonfitness.data.KhonDao.saveCardioEdits]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CardioSaveNeedsTest {
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

    // The view model's flows outlive the screen by 5 s (WhileSubscribed): stop them before the database closes, or one
    // queries the closed database and its exception fails the next test.
    @After fun close() { vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() }

    private fun cardio(timeSec: Int? = null, distanceM: Int? = null, laps: Int? = null, finishedAt: Long? = null) =
        Session(id = "c1", date = "2026-11-03", startedAt = 1L, finishedAt = finishedAt, itemType = ItemType.CARDIO, exerciseId = "run", exerciseName = "Run", timeSec = timeSec, distanceM = distanceM, laps = laps)

    @Test fun `an entry needs a time or a distance, laps count as distance`() {
        assertEquals("Enter time or distance", cardioMissing(cardio()))
        assertEquals("Enter time or distance", cardioMissing(cardio(timeSec = 0, distanceM = 0, laps = 0)))
        assertNull(cardioMissing(cardio(timeSec = 1500)))
        assertNull(cardioMissing(cardio(distanceM = 5000)))
        assertNull(cardioMissing(cardio(laps = 40)))
    }

    private lateinit var nav: NavHostController

    /** The form on a real back stack, over Today, or over History on Today when [fromHistory]. */
    private fun openForm(s: Session, fields: String = "time,distance,avgHr", fromHistory: Boolean = false) {
        runBlocking(Dispatchers.IO) {
            dao.upsert(Exercise("run", "Run", Kind.CARDIO, intervals = true, rounds = 1, fields = fields))
            dao.upsert(s)
            dao.upsertIntervalLogs(listOf(IntervalLog("i1", s.id, 0)))
        }
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, startDestination = "today") {
                    composable("today") { Text("Home") }
                    composable("history") { Text("History") }
                    composable("cardio/{id}") { e -> CardioScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                }
            }
        }
        compose.runOnIdle { if (fromHistory) nav.navigate("history"); nav.navigate("cardio/${s.id}") }
        // The round shows once every query of the form has answered, so none is still running at teardown.
        repeat(250) { if (compose.onAllNodesWithText("Round 1").fetchSemanticsNodes().isNotEmpty()) return; compose.waitForIdle(); Thread.sleep(20) }
        throw AssertionError("the form did not load")
    }

    private fun saveEnabled() = !compose.onNodeWithText("Save").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)

    @Test fun `an empty new entry has Save off and says what to enter`() {
        openForm(cardio())
        assertFalse(saveEnabled())
        compose.onNodeWithText("Enter time or distance").fetchSemanticsNode()
    }

    @Test fun `an entry with the timer's time saves`() {
        openForm(cardio(timeSec = 960))
        assertTrue(saveEnabled())
        assertTrue(compose.onAllNodesWithText("Enter time or distance").fetchSemanticsNodes().isEmpty())
    }

    @Test fun `a saved session in History keeps Save on`() {
        openForm(cardio(finishedAt = 5L))
        assertTrue(saveEnabled())
    }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun stored() = runBlocking(Dispatchers.IO) { dao.session("c1") }
    private fun until(shown: () -> Boolean) {
        repeat(250) { if (shown()) return; compose.waitForIdle(); Thread.sleep(20) }
        throw AssertionError("the screen did not reach the expected state")
    }
    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun route() = nav.currentDestination?.route

    private fun routes() = nav.currentBackStack.value.mapNotNull { it.destination.route }

    /** The Save that finishes a workout lands on Today, also from History's "In progress" row; one write, one exit. */
    @Test fun `Save of an unfinished entry opened from History, tapped twice, finishes it once and lands on Today`() {
        openForm(cardio(timeSec = 960), fromHistory = true)
        val click = compose.onNodeWithText("Save").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread { click(); click() }
        until { routes() == listOf("today") }
        until { stored()?.finished == true }
        assertEquals(listOf("c1"), runBlocking(Dispatchers.IO) { dao.sessionsOnce() }.map { it.id })
    }

    /** An edit of a saved session goes back to History, where it was opened. */
    @Test fun `Save of a saved entry opened from History goes back to History`() {
        openForm(cardio(timeSec = 960, finishedAt = 5L), fromHistory = true)
        compose.onNodeWithText("Save").performClick()
        until { routes() == listOf("today", "history") }
    }

    @Test fun `case 17 - an exercise with only heart rate and watts shows Time - Save off while empty, on with a time`() {
        openForm(cardio(), fields = "avgHr,watts")
        compose.onNodeWithText("Time").fetchSemanticsNode()
        assertFalse(saveEnabled())
        compose.onNodeWithContentDescription("Increase by 1 min").performClick()
        until { saveEnabled() }
        compose.onNodeWithText("Save").performClick()
        until { stored()?.finished == true }
        assertEquals(60, stored()!!.timeSec)
        until { route() == "today" }
    }

    @Test fun `case 18 - the stored time cleared elsewhere before Save - refused, intervals unchanged, form open`() {
        openForm(cardio(timeSec = 960))
        compose.onNodeWithContentDescription("Round 1 heart rate").performTextInput("150")
        val click = compose.onNodeWithText("Save").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        // The timer's time goes in the same frame as the tap, before the form recomposes: only the data layer sees it.
        compose.runOnUiThread { io { dao.setSessionTime("c1", null) }; click() }
        until { shows("Enter time or distance") }
        assertEquals("cardio/{id}", route())
        assertFalse(stored()!!.finished)
        assertNull(runBlocking(Dispatchers.IO) { dao.intervalLogsFor("c1") }.single().avgHr)
        // The guard came back: a later valid Save leaves.
        io { dao.setSessionTime("c1", 300) }
        until { saveEnabled() }
        compose.onNodeWithText("Save").performClick()
        until { route() == "today" }
        assertEquals(150, runBlocking(Dispatchers.IO) { dao.intervalLogsFor("c1") }.single().avgHr)
    }

    @Test fun `case 19 - opened empty, the timer stores a time - Save turns on and a note saves with it`() {
        openForm(cardio())
        assertFalse(saveEnabled())
        io { dao.setSessionTime("c1", 960) }
        until { saveEnabled() }
        compose.onNodeWithContentDescription("Note").performTextInput("Felt good")
        compose.onNodeWithText("Save").performClick()
        until { stored()?.finished == true }
        assertEquals(960, stored()!!.timeSec)
        assertEquals("Felt good", stored()!!.note)
    }

    @Test fun `case 20 - zero, negative, laps only and finished entries follow one rule in the form and the data layer`() {
        val base = cardio()
        val cases = listOf(
            cardio(timeSec = 0) to false, cardio(timeSec = -60) to false, cardio(distanceM = -5) to false, cardio(laps = 0) to false,
            cardio(laps = 12) to true, cardio(distanceM = 1) to true, cardio(timeSec = 1) to true,
        )
        for ((edited, saves) in cases) {
            io { dao.deleteSessionWithLogs("c1"); dao.upsert(base) }
            // The form's rule: the stored row with the form's edits.
            assertEquals(edited.toString(), saves, cardioMissing(mergeCardioEdits(base, base, edited, 0L)) == null)
            val result = runBlocking(Dispatchers.IO) { vm.repo.saveCardioEdits(base, edited, emptyMap(), now = 5L) }
            assertEquals(edited.toString(), if (saves) CardioSaveResult.Saved else CardioSaveResult.Refused("Enter time or distance"), result)
            assertEquals(saves, stored()!!.finished)
        }
        // A finished entry from History saves with nothing in it, and its interval edits are written.
        val finished = cardio(finishedAt = 5L)
        io { dao.deleteSessionWithLogs("c1"); dao.upsert(finished); dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c1", 0))) }
        val result = runBlocking(Dispatchers.IO) { vm.repo.saveCardioEdits(finished, finished.copy(note = "x"), mapOf("i1" to IntervalEdit(avgHr = Edit.Set(140)))) }
        assertEquals(CardioSaveResult.Saved, result)
        assertEquals("x", stored()!!.note)
        assertEquals(140, runBlocking(Dispatchers.IO) { dao.intervalLogsFor("c1") }.single().avgHr)
    }

    @Test fun `a refused new entry writes nothing - no interval field and no finish`() {
        val s = cardio()
        io { dao.upsert(s); dao.upsertIntervalLogs(listOf(IntervalLog("i1", "c1", 0))) }
        val result = runBlocking(Dispatchers.IO) { vm.repo.saveCardioEdits(s, s.copy(note = "x"), mapOf("i1" to IntervalEdit(avgHr = Edit.Set(140)))) }
        assertEquals(CardioSaveResult.Refused("Enter time or distance"), result)
        assertEquals(s, stored())
        assertNull(runBlocking(Dispatchers.IO) { dao.intervalLogsFor("c1") }.single().avgHr)
    }
}
