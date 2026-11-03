package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Looper
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.PlanItem
import dev.mirzohidkhon.khonfitness.timer.TimerService
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Attack case 21, the whole path of row 8: the real timer service runs and is stopped early; the app state is built
 * again from the database alone (a new process: no timer state left); Today offers Save and Discard, not Resume; Save
 * opens the form with the time the service stored and the session's intervals.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = KhonApp::class)
class TimerStopRestartTest {
    @get:Rule val compose = createComposeRule()
    private val running = ApplicationProvider.getApplicationContext<KhonApp>()
    private var restarted: KhonApp? = null
    private lateinit var nav: NavHostController
    private val tabata = Exercise("tabata", "Tabata bike", Kind.CARDIO, intervals = true, workSec = 20, restSec = 10, rounds = 8, fields = "time,avgHr,watts")

    @After fun close() {
        TimerService.state.value = null; TimerService.finished.value = null
        restarted?.let { it.scope.cancel(); it.db.close() }
        running.scope.cancel(); running.db.close()
    }

    private fun pump(ms: Long) = repeat((ms / 50).toInt()) { Thread.sleep(50); shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50)) }
    private fun until(shown: () -> Boolean) {
        repeat(250) { if (shown()) return; compose.waitForIdle(); Thread.sleep(20) }
        throw AssertionError("the screen did not reach the expected state")
    }
    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @Test fun `stopped timer, restart - Today offers Save and Discard, and Save opens the form with the stored time and intervals`() {
        val repo = running.repo
        val session = runBlocking(Dispatchers.IO) {
            running.db.dao().upsert(tabata)
            repo.startCardioSession(tabata)
        }
        val round1 = runBlocking(Dispatchers.IO) { running.db.dao().intervalLogsFor(session.id) }.first()
        runBlocking(Dispatchers.IO) { running.db.dao().setIntervalAvgHr(round1.id, 151) }

        // The service: start, begin, run (sped up), Stop.
        fun intent(action: String) = Intent(running, TimerService::class.java).setAction(action)
        val service = Robolectric.buildService(TimerService::class.java, intent(TimerService.ACTION_START)
            .putExtra("sessionId", session.id).putExtra("title", tabata.name).putExtra("work", 20).putExtra("rest", 10).putExtra("rounds", 8).putExtra("speed", 10.0))
            .create().startCommand(0, 1)
        service.withIntent(intent(TimerService.ACTION_BEGIN)).startCommand(0, 2)
        pump(4_000) // the 3 s get-ready, then about 1 s at 10 x
        service.withIntent(intent(TimerService.ACTION_STOP)).startCommand(0, 3)
        var stored: Int? = null
        repeat(200) { if (stored == null) { pump(50); stored = runBlocking(Dispatchers.IO) { running.db.dao().session(session.id) }?.timeSec } }
        val timeSec = stored ?: throw AssertionError("the service stored no time")
        assertTrue("some time ran: $timeSec", timeSec > 0)
        service.destroy()

        // A new process: no timer state, a new app over the same database file.
        TimerService.state.value = null; TimerService.finished.value = null
        val app = KhonApp().also { restarted = it }
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        val vm = AppViewModel(app)
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, startDestination = "today") {
                    composable("today") {
                        val active by vm.unfinished.collectAsStateWithLifecycle()
                        val timer by TimerService.state.collectAsStateWithLifecycle()
                        val s = active
                        if (s == null) Text("Loading") else TodayCard(
                            PlanItem(ItemType.CARDIO, cardio = tabata), "", s, null, null, emptyList(), listOf(tabata), now = System.currentTimeMillis(),
                            onStart = {}, onResume = { nav.navigate(Routes.cardio(it.id)) }, timerStopped = timerStopped(s, timer?.sessionId), onDiscard = {},
                        )
                    }
                    composable("cardio/{id}") { e -> CardioScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                }
            }
        }
        until { shows("Discard") }
        assertTrue(compose.onAllNodesWithText("Resume").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText("Save").performClick()
        until { shows("Round 8") }
        // The form holds the stored time (Save is on, so the time is above 0) and the stored round values.
        assertTrue(!compose.onNodeWithText("Save").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled))
        assertTrue(shows("151"))
        compose.onNodeWithText("Save").performClick()
        until { runBlocking(Dispatchers.IO) { app.db.dao().session(session.id) }?.finished == true }
        assertEquals(timeSec, runBlocking(Dispatchers.IO) { app.db.dao().session(session.id) }!!.timeSec)
        assertEquals(151, runBlocking(Dispatchers.IO) { app.db.dao().intervalLogsFor(session.id) }.first().avgHr)
        until { nav.currentDestination?.route == "today" }
    }
}
