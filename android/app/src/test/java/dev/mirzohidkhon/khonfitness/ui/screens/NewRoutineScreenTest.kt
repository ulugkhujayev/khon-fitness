package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.NEW_ROUTINE_NAME
import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.data.StretchRoutine
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.openTab
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * "Stretch routine" under "+" on Plan saves a new routine at once and opens it (P5B-FIX4): one routine per double tap,
 * no editor when the user left Plan meanwhile, and the editor edits the saved row, which Back keeps and Delete removes. Each
 * editor row is keyed by its id, so a held stepper never carries over to the row that takes its place.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class NewRoutineScreenTest {
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

    private val daily = StretchRoutine("daily", "Daily", active = true, sortOrder = 0)

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun routines() = runBlocking(Dispatchers.IO) { dao.stretchRoutinesOnce() }.sortedBy { it.sortOrder }
    private fun added() = routines().filter { it.id != "daily" }

    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun named(label: String) = compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(shown: () -> Boolean) = compose.waitUntil(5_000) { shown() }

    private lateinit var nav: NavHostController

    /** Today (a stand-in), Plan and the routine editor in one nav graph, opened on Plan with "Daily" saved. */
    private fun openStretching(restore: StateRestorationTester? = null) {
        io { dao.upsert(daily) }
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            KhonTheme {
                val nav = rememberNavController().also { nav = it }
                NavHost(nav, Routes.TODAY) {
                    composable(Routes.TODAY) { Text("Today screen") }
                    composable(Routes.PROGRAMS) { PlanScreen(vm, nav) }
                    composable("routine/{id}") { e -> RoutineEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                }
                androidx.compose.runtime.LaunchedEffect(Unit) { if (nav.currentDestination?.route == Routes.TODAY) nav.openTab(Routes.PROGRAMS) }
            }
        }
        if (restore != null) restore.setContent(content) else compose.setContent(content)
        waitFor { shows("Daily") }
    }

    /** Holds the database's write lock on its own thread, so every write the screen asks for waits until [release]. */
    private inner class Hold {
        private val held = CountDownLatch(1)
        private val ended = CountDownLatch(1)
        private val holder = thread { app.db.runInTransaction { held.countDown(); ended.await() } }
        init { assertTrue("the hold started", held.await(10, TimeUnit.SECONDS)) }
        fun release() { ended.countDown(); holder.join(10_000); compose.waitForIdle() }
    }

    /** [done] holds, then a short wait shows that no later write changes it. */
    private fun settled(done: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (!done()) { check(System.currentTimeMillis() < end) { "the writes did not settle in 5 s" }; Thread.sleep(50); compose.waitForIdle() }
        Thread.sleep(300); compose.waitForIdle()
    }

    /** "+" on Plan, then "Stretch routine" in its sheet. */
    private fun tapPlus() {
        compose.onNodeWithContentDescription("New").performClick()
        waitFor { shows("Stretch routine") }
        compose.onNodeWithText("Stretch routine").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    @Test fun `plus saves one new routine at once and opens its editor`() {
        openStretching()
        tapPlus()
        waitFor { named("Routine name") }
        val made = added().single()
        assertEquals(StretchRoutine(made.id, NEW_ROUTINE_NAME, active = false, sortOrder = 1), made)
        assertEquals(listOf("daily"), routines().filter { it.active }.map { it.id }) // Daily keeps the mark
        assertTrue("the editor shows Delete", shows("Delete routine"))
    }

    @Test fun `a double tap while the save runs makes one routine`() {
        openStretching()
        val hold = Hold()
        tapPlus()
        tapPlus()
        hold.release()
        settled { added().isNotEmpty() }
        waitFor { named("Routine name") }
        assertEquals(1, added().size)
    }

    @Test fun `a second tap after Plan was rebuilt while the save runs makes no second routine`() {
        val restore = StateRestorationTester(compose)
        openStretching(restore)
        val hold = Hold()
        tapPlus()
        restore.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        tapPlus()
        hold.release()
        settled { added().isNotEmpty() }
        assertEquals(1, added().size)
    }

    @Test fun `an editor with no routine row shows Back, also for a back stack saved as routine new`() {
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            KhonTheme {
                val nav = rememberNavController()
                NavHost(nav, "programs") {
                    composable("programs") { Text("Plan screen") }
                    composable("routine/{id}") { e -> RoutineEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                }
                androidx.compose.runtime.LaunchedEffect(Unit) { if (nav.currentDestination?.route == "programs") nav.navigate(Routes.routine("new")) }
            }
        }
        compose.setContent(content)
        waitFor { named("Plan") }
        compose.onNodeWithContentDescription("Plan").performClick()
        waitFor { shows("Plan screen") }
        assertTrue(routines().isEmpty())
    }

    @Test fun `leaving Plan before the save ends keeps the routine and opens no editor`() {
        openStretching()
        val hold = Hold()
        tapPlus()
        compose.runOnUiThread { nav.openTab(Routes.TODAY) } // the Today tab
        compose.waitForIdle()
        hold.release()
        settled { added().isNotEmpty() }
        assertTrue("on Today", shows("Today screen"))
        assertFalse("no editor", named("Routine name"))
        assertEquals(1, added().size)
    }

    @Test fun `Back from the editor keeps the new routine`() {
        openStretching()
        tapPlus()
        waitFor { named("Routine name") }
        compose.onNodeWithContentDescription("Plan").performClick() // Back
        waitFor { shows("Stretch routines") && shows(NEW_ROUTINE_NAME) }
        assertEquals(listOf(NEW_ROUTINE_NAME), added().map { it.name })
    }

    @Test fun `Name and Active edits on the new routine are stored at once`() {
        openStretching()
        tapPlus()
        waitFor { named("Routine name") }
        compose.onNodeWithContentDescription("Routine name").performTextReplacement("Evening")
        compose.onNodeWithContentDescription("Active").performClick()
        settled { added().single().let { it.name == "Evening" && it.active } }
        assertEquals(listOf(added().single().id), routines().filter { it.active }.map { it.id }) // the only active one
    }

    @Test fun `Delete removes the new routine`() {
        openStretching()
        tapPlus()
        waitFor { named("Routine name") }
        compose.onNodeWithText("Delete routine").performSemanticsAction(SemanticsActions.OnClick) // below the fold
        waitFor { shows("Delete this routine?") }
        compose.onNodeWithText("Delete").performSemanticsAction(SemanticsActions.OnClick) // in the sheet, which may still be sliding in
        settled { added().isEmpty() }
        assertEquals(listOf(daily), routines())
        waitFor { shows("Stretch routines") }
    }

    @Test fun `a rotation in the editor keeps the same routine`() {
        val restore = StateRestorationTester(compose)
        openStretching(restore)
        tapPlus()
        waitFor { named("Routine name") }
        val id = added().single().id

        restore.emulateSavedInstanceStateRestore()
        waitFor { named("Routine name") }
        compose.onNodeWithContentDescription("Routine name").performTextReplacement("Evening")
        settled { added().single().name == "Evening" }
        assertEquals(listOf(id), added().map { it.id })
    }

    @Test fun `a held plus on a row that goes leaves the next row's seconds alone`() {
        io {
            dao.upsertStretches(listOf(Stretch("fold", "Fold", seconds = 45), Stretch("reach", "Reach", seconds = 30)))
            dao.upsert(StretchRoutine("r", "Daily", active = true))
            dao.upsertRoutineStretches(listOf(RoutineStretch("a", "r", "fold", 0), RoutineStretch("b", "r", "reach", 1)))
        }
        compose.mainClock.autoAdvance = false
        compose.setContent { KhonTheme { RoutineEditorScreen(vm, rememberNavController(), "r") } }
        until { named("Move Reach up") }

        // Row A's plus held past the repeat delay: its field holds a number it saves only on release.
        compose.onAllNodesWithContentDescription("Increase by 5 s")[0].performTouchInput { down(center) }
        repeat(60) { frame() } // about one second
        io { dao.removeRoutineStretch("a") }
        until { !named("Move Fold up") }
        assertEquals(listOf("Daily", "30"), fieldTexts())
        compose.onAllNodesWithContentDescription("Increase by 5 s")[0].performTouchInput { up() }
        repeat(10) { frame() }
        compose.mainClock.autoAdvance = true
        settled { true }

        assertNull(runBlocking(Dispatchers.IO) { dao.routineStretch("b") }!!.seconds)
        assertEquals(listOf("Daily", "30"), fieldTexts()) // row B shows its own seconds, not row A's held number
    }

    /** The text of every editable field on screen, top to bottom. */
    private fun fieldTexts() = compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        .map { it.config.getOrNull(SemanticsProperties.EditableText)?.text }

    private fun frame() { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }

    private fun until(shown: () -> Boolean) {
        repeat(250) { if (shown()) return; frame(); Thread.sleep(20) }
        throw AssertionError("the screen did not reach the expected state")
    }
}
