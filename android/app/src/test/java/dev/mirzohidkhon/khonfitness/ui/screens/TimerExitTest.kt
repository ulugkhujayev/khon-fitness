package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.timer.FinishedTimer
import dev.mirzohidkhon.khonfitness.timer.TimerService
import dev.mirzohidkhon.khonfitness.timer.TimerState
import dev.mirzohidkhon.khonfitness.timer.buildPhases
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
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
import java.time.LocalDate

/**
 * The interval timer's exits (the run's end, the safety net, system Back) go through one [ExitGuard]: the real
 * [TimerScreen] over Today, with the run's end and Back in one frame, leaves once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TimerExitTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var nav: NavHostController

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
        runBlocking(Dispatchers.IO) {
            app.db.dao().upsert(Exercise("hiit", "Bike intervals", Kind.CARDIO, intervals = true, workSec = 60, restSec = 30, rounds = 4))
            app.db.dao().upsert(Session(id = "t1", date = LocalDate.now().toString(), startedAt = System.currentTimeMillis(), itemType = ItemType.CARDIO, exerciseId = "hiit"))
        }
        // The timer runs for this session, so the screen does not start the service.
        TimerService.state.value = TimerState("t1", "Bike intervals", buildPhases(0, 60, 30, 4), remaining = 60.0)
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, Routes.TODAY) {
                    composable(Routes.TODAY) { Text("Today") }
                    composable("timer/{id}") { e -> TimerScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("cardio/{id}") { Text("Cardio form") }
                }
            }
        }
        compose.runOnIdle { nav.navigate(Routes.timer("t1")) }
        compose.waitUntil(5_000) { compose.onAllNodes(androidx.compose.ui.test.hasText("Bike intervals")).fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun close() {
        TimerService.state.value = null; TimerService.finished.value = null
        compose.waitForIdle()
        vm.viewModelScope.cancel(); app.scope.cancel()
        app.db.close()
    }

    private fun routes() = nav.currentBackStack.value.mapNotNull { it.destination.route }
    private fun back() = compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
    private fun end() { TimerService.finished.value = FinishedTimer("t1", 240, early = false) }

    @Test fun `Back and then the run's end in one frame leave once, to Today`() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed(); end() }
        compose.waitForIdle()
        assertEquals(listOf(Routes.TODAY), routes())
    }

    @Test fun `the run's end and then Back in one frame leave once, to the form`() {
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { end() }
        // Frame by frame until the form is on top; the timer is still composed while it fades out.
        repeat(30) { if (nav.currentDestination?.route != "cardio/{id}") compose.mainClock.advanceTimeByFrame() }
        assertEquals("cardio/{id}", nav.currentDestination?.route)
        back()
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(listOf(Routes.TODAY, "cardio/{id}"), routes())
    }

    @Test fun `Back alone leaves the timer once`() {
        back()
        compose.waitForIdle()
        assertEquals(listOf(Routes.TODAY), routes())
    }
}
