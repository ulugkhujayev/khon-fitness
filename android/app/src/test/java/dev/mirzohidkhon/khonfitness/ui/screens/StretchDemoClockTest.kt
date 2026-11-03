package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.data.StretchRoutine
import dev.mirzohidkhon.khonfitness.data.StretchSession
import dev.mirzohidkhon.khonfitness.timer.Phase
import dev.mirzohidkhon.khonfitness.timer.PhaseKind
import dev.mirzohidkhon.khonfitness.timer.TimerService
import dev.mirzohidkhon.khonfitness.timer.TimerState
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The demonstration on the player's ready screen (contract row 21): play, pause, change view and a step tap never
 * send the timer an action, so the run clock does not start, and nothing is written.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class StretchDemoClockTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val dao get() = app.db.dao()
    private val shadowApp get() = shadowOf(ApplicationProvider.getApplicationContext<Application>())

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    // The view model's flows outlive the screen by 5 s (WhileSubscribed): stop them before the database closes, or one
    // queries the closed database and its exception fails the next test.
    @After fun close() { TimerService.state.value = null; TimerService.finished.value = null; vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }

    private fun frame() { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }

    private fun until(shown: () -> Boolean) {
        repeat(250) { if (shown()) return; frame(); Thread.sleep(20) }
        throw AssertionError("the screen did not reach the expected state")
    }

    private fun named(label: String) = compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()

    /** The click action, not a touch: the guide's steps can sit below the screen, and a scroll would wait on the held clock. */
    private fun tap(label: String) { until { named(label) }; compose.onNodeWithContentDescription(label).performSemanticsAction(SemanticsActions.OnClick); frame() }

    /** Every service intent the screen sent since the last call. */
    private fun sent(): List<String?> = generateSequence { shadowApp.nextStartedService }.map { it.action }.toList()

    @Test fun `demo controls on the ready screen never start the run or write`() {
        io {
            dao.upsertStretches(listOf(Stretch("hip", "Hip flexor", sided = true, figure = "hipflexor")))
            dao.upsert(StretchRoutine("r", "Daily", active = true))
            dao.upsertRoutineStretches(listOf(RoutineStretch("r-hip", "r", "hip", 0)))
        }
        // The run waits on its ready screen, as the service leaves it after opening a routine.
        val ready = TimerState("r:run", "Daily", listOf(Phase(PhaseKind.WORK, 40, 0, label = "Hip flexor", side = "Left", figure = "hipflexor", stretchId = "hip")),
            remaining = 40.0, stretch = true)
        TimerService.state.value = ready
        compose.mainClock.autoAdvance = false // the guide plays without end
        compose.setContent { KhonTheme { StretchScreen(vm, rememberNavController(), "r") } }
        until { named("Pause demo") }
        sent()

        tap("Pause demo")
        tap("Step 2: Tuck")
        tap("Play demo")
        tap("Change camera angle. View 1 of 2")
        repeat(30) { frame() }

        assertEquals(emptyList<String?>(), sent())
        assertEquals(ready, TimerService.state.value)
        assertEquals(emptyList<StretchSession>(), runBlocking(Dispatchers.IO) { dao.stretchSessionsOnce() })

        // The check sees an action when there is one: Start sends Begin.
        compose.onNodeWithText("Start left side").performClick() // fixed under the scroll
        frame()
        assertEquals(listOf(TimerService.ACTION_BEGIN), sent())
        assertNull(TimerService.finished.value)
    }
}
