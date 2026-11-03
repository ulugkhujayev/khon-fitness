package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A running gym session on a real database and a real back stack (Today under the session): Save in the finish sheet
 * (row 1) and Discard (row 4) leave once, even for two taps in one frame; a check needs weight and reps (row 2).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FinishSaveOnceTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var nav: NavHostController
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

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }

    private fun log(id: String, order: Int, status: String, weight: Double? = 60.0, reps: Int? = 8, step: Double = 2.5) =
        SetLog(id, "s1", 0, false, 0, order, order, "bench", "Bench Press", 6, 10, step, weight, reps, status)

    /** Today with the session pushed on it. */
    private fun openSession(logs: List<SetLog>) {
        io {
            dao.upsert(Session(id = "s1", date = "2026-11-03", startedAt = System.currentTimeMillis(), itemType = ItemType.PROGRAM, programId = "p1", programName = "Push"))
            dao.upsertSetLogs(logs)
        }
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, startDestination = "today") {
                    composable("today") { Text("Today home") }
                    composable("session/{id}") { e -> SessionScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                }
            }
        }
        compose.runOnIdle { nav.navigate("session/s1") }
        until { shows("Bench Press") }
    }

    private fun until(shown: () -> Boolean) {
        repeat(250) { if (shown()) return; compose.waitForIdle(); Thread.sleep(20) }
        throw AssertionError("the screen did not reach the expected state")
    }

    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /** Two taps on [text] in one frame: the button's click runs twice before anything recomposes. */
    private fun doubleTap(text: String) {
        val click = compose.onNodeWithText(text).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread { click(); click() }
    }

    private fun session() = runBlocking(Dispatchers.IO) { dao.session("s1") }

    @Test fun `a double tap on Save finishes once and leaves Today showing`() {
        openSession(listOf(log("a", 0, SetStatus.DONE), log("b", 1, SetStatus.PENDING)))
        compose.onNodeWithText("Finish").performClick()
        until { shows("Workout done") }
        doubleTap("Save")
        compose.waitForIdle()
        // One back navigation: Today is on top and still on the stack, never an empty screen.
        assertEquals("today", nav.currentDestination?.route)
        assertEquals(listOf("today"), nav.currentBackStack.value.mapNotNull { it.destination.route })
        until { shows("Today home") }
        until { session()?.finished == true }
    }

    @Test fun `Discard asks with the count of checked sets, and Cancel keeps everything`() {
        openSession(listOf(log("a", 0, SetStatus.DONE), log("b", 1, SetStatus.DONE), log("c", 2, SetStatus.DONE), log("d", 3, SetStatus.PENDING)))
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Discard workout"))
        compose.onNodeWithText("Discard workout").performClick()
        until { shows("Discard this workout?") }
        assertNotNull(compose.onNodeWithText("3 checked sets are lost.").fetchSemanticsNode())
        val cancel = compose.onNodeWithText("Cancel").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread { cancel() }
        until { !shows("3 checked sets are lost.") }
        assertNotNull(session())
        assertEquals(4, runBlocking(Dispatchers.IO) { dao.setLogsOnce("s1") }.size)
        assertEquals("session/{id}", nav.currentDestination?.route)
    }

    @Test fun `a double tap on Discard deletes the session and leaves once`() {
        openSession(listOf(log("a", 0, SetStatus.DONE)))
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Discard workout"))
        compose.onNodeWithText("Discard workout").performClick()
        until { shows("Discard this workout?") }
        doubleTap("Discard")
        compose.waitForIdle()
        assertEquals(listOf("today"), nav.currentBackStack.value.mapNotNull { it.destination.route })
        until { session() == null }
    }

    @Test fun `a double tap on Back leaves once`() {
        openSession(listOf(log("a", 0, SetStatus.DONE)))
        val back = compose.onNodeWithContentDescription("Today").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread { back(); back() }
        compose.waitForIdle()
        assertEquals(listOf("today"), nav.currentBackStack.value.mapNotNull { it.destination.route })
        assertNotNull(session())
    }

    @Test fun `a check without reps stays pending and the row says Enter reps`() {
        openSession(listOf(log("a", 0, SetStatus.PENDING, reps = null)))
        compose.onNodeWithContentDescription("A1 Bench Press").performClick()
        until { shows("Enter reps") }
        assertEquals(SetStatus.PENDING, runBlocking(Dispatchers.IO) { dao.setLogsOnce("s1") }.single().status)
        assertNull(compose.onAllNodesWithText("Enter weight").fetchSemanticsNodes().firstOrNull())
    }

    @Test fun `a check with 0 kg on a weighted exercise stays pending and the row says Enter weight`() {
        openSession(listOf(log("a", 0, SetStatus.PENDING, weight = 0.0)))
        compose.onNodeWithContentDescription("A1 Bench Press").performClick()
        until { shows("Enter weight") }
        assertEquals(SetStatus.PENDING, runBlocking(Dispatchers.IO) { dao.setLogsOnce("s1") }.single().status)
    }

    @Test fun `a filled set checks, and a bodyweight set needs reps only`() {
        openSession(listOf(log("a", 0, SetStatus.PENDING), log("b", 1, SetStatus.PENDING, weight = null, step = 0.0)))
        compose.onNodeWithContentDescription("A1 Bench Press").performClick()
        compose.onNodeWithContentDescription("A2 Bench Press").performClick()
        until { runBlocking(Dispatchers.IO) { dao.setLogsOnce("s1") }.all { it.status == SetStatus.DONE } }
        assertNull(compose.onAllNodesWithText("Enter weight").fetchSemanticsNodes().firstOrNull())
    }
}
