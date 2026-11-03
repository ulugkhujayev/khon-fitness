package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A new cardio entry saves only with a time or a distance (row 7); the form says so and Save is off. */
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

    private fun openForm(s: Session) {
        runBlocking(Dispatchers.IO) {
            dao.upsert(Exercise("run", "Run", Kind.CARDIO, intervals = true, rounds = 1, fields = "time,distance,avgHr"))
            dao.upsert(s)
            dao.upsertIntervalLogs(listOf(IntervalLog("i1", s.id, 0)))
        }
        compose.setContent { KhonTheme { CardioScreen(vm, rememberNavController(), s.id) } }
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
}
