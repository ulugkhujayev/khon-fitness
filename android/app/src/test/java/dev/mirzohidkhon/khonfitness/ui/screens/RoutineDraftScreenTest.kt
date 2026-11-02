package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.data.StretchRoutine
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import android.os.Looper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * A new routine is a draft (attack cases E4 and E5): the "+" on Stretching writes nothing, leaving the draft writes
 * nothing, Done writes one routine with the typed name and Active, and the first stretch saves the routine once,
 * also when the screen comes back from saved state before Done. While that first create is still pending ([Hold]),
 * the editor stays a draft under the same id: a refused create leaves a draft that Done saves, and Done after a create
 * that succeeds saves no second routine.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RoutineDraftScreenTest {
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
    private fun routines() = runBlocking(Dispatchers.IO) { dao.stretchRoutinesOnce() }
    private fun rows() = runBlocking(Dispatchers.IO) { dao.routineStretchesOnce() }

    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun named(label: String) = compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(shown: () -> Boolean) = compose.waitUntil(5_000) { shown() }

    /** Stretching and the routine editor in one nav graph, as the app has them. */
    private fun openStretching() {
        compose.setContent {
            KhonTheme {
                val nav = rememberNavController()
                NavHost(nav, Routes.ROUTINES) {
                    composable(Routes.ROUTINES) { RoutinesScreen(vm, nav) }
                    composable("routine/{id}") { e -> RoutineEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                }
            }
        }
        waitFor { shows("Daily") }
    }

    /**
     * Holds the database's write lock in a transaction on its own thread, so every write the screen asks for waits
     * (pending) until [release].
     */
    private inner class Hold {
        private val held = CountDownLatch(1)
        private val ended = CountDownLatch(1)
        @Volatile private var last: String? = null
        private val holder = thread { app.db.runInTransaction { held.countDown(); ended.await(); last?.let { app.db.openHelper.writableDatabase.execSQL(it) } } }
        init { assertTrue("the hold started", held.await(10, TimeUnit.SECONDS)) }

        /** Ends the hold; [sql] runs in it first (for example deleting the first stretch, so the pending create is refused). */
        fun release(sql: String? = null) { last = sql; ended.countDown(); holder.join(10_000); compose.waitForIdle() }
    }

    /** A new routine's editor, standalone, with its stretch list loaded. */
    private fun openNewRoutine(restore: StateRestorationTester) {
        restore.setContent { KhonTheme { RoutineEditorScreen(vm, rememberNavController(), "new") } }
        waitFor { shows("Add stretch") && vm.stretches.value.isNotEmpty() }
    }

    /** Add stretch, then [name] in the sheet, which may still be sliding in. */
    private fun pick(name: String) {
        compose.onNodeWithText("Add stretch").performClick()
        waitFor { shows(name) }
        compose.onNodeWithText(name).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    /**
     * The queued writes have run: [done] holds, then a short wait shows no later write changes it. It idles the main
     * thread while it waits, where a queued write resumes after the one before it.
     */
    private fun settled(done: () -> Boolean) {
        val end = System.currentTimeMillis() + 5_000
        while (!done()) { check(System.currentTimeMillis() < end) { "the writes did not settle in 5 s" }; Thread.sleep(50); compose.waitForIdle() }
        Thread.sleep(300); compose.waitForIdle()
    }

    /** "+" on Stretching, then the name and the Active switch on the new routine. */
    private fun fillNewRoutine(name: String) {
        compose.onNodeWithContentDescription("New routine").performClick()
        waitFor { named("Routine name") }
        compose.onNodeWithContentDescription("Routine name").performTextReplacement(name)
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Active").performClick()
        compose.waitForIdle()
    }

    @Test fun `a new routine left without Done writes nothing, and Done writes it once with its name and Active`() {
        io { dao.upsert(StretchRoutine("daily", "Daily", active = true, sortOrder = 0)) }
        openStretching()

        fillNewRoutine("Evening")
        compose.onNodeWithContentDescription("Stretching").performClick() // Back
        waitFor { shows("Routines") }
        compose.waitForIdle()
        assertEquals(listOf(StretchRoutine("daily", "Daily", active = true, sortOrder = 0)), routines())

        fillNewRoutine("Evening")
        compose.onNodeWithText("Done").performClick()
        waitFor { routines().size == 2 }
        val saved = routines().associateBy { it.name }
        assertEquals(setOf("Daily", "Evening"), saved.keys)
        assertEquals(true, saved.getValue("Evening").active)
        assertEquals(false, saved.getValue("Daily").active) // the only active one
        assertEquals(1, saved.getValue("Evening").sortOrder)
        assertEquals(emptyList<RoutineStretch>(), rows())
    }

    @Test fun `the first stretch saves the routine once, and Done after a restore keeps that one routine`() {
        io { dao.upsertStretches(listOf(Stretch("fold", "Fold"))) }
        val restore = StateRestorationTester(compose)
        restore.setContent { KhonTheme { RoutineEditorScreen(vm, rememberNavController(), "new") } }
        waitFor { shows("Add stretch") }
        compose.onNodeWithText("Add stretch").performClick()
        waitFor { shows("Fold") }
        compose.onNodeWithText("Fold").performSemanticsAction(SemanticsActions.OnClick) // in the sheet, which may still be sliding in
        waitFor { routines().size == 1 && rows().size == 1 }
        val id = routines().single().id

        restore.emulateSavedInstanceStateRestore()
        waitFor { named("Move Fold up") }
        compose.onAllNodesWithText("Done").onFirst().performClick()
        compose.waitForIdle()
        Thread.sleep(300)
        assertEquals(listOf(id), routines().map { it.id })
        assertEquals(listOf(RoutineStretch(rows().single().id, id, "fold", 0)), rows())
    }

    @Test fun `a restore while the first stretch's create is pending, then a refusal, leaves a draft that Done saves without stretches`() {
        io { dao.upsertStretches(listOf(Stretch("fold", "Fold"))) }
        val restore = StateRestorationTester(compose)
        openNewRoutine(restore)
        compose.onNodeWithContentDescription("Routine name").performTextReplacement("Evening")
        val hold = Hold()
        pick("Fold")

        restore.emulateSavedInstanceStateRestore()
        hold.release("DELETE FROM stretches WHERE id = 'fold'")
        settled { runBlocking(Dispatchers.IO) { dao.stretchesOnce() }.isEmpty() }
        assertTrue("Add stretch shown", shows("Add stretch"))
        assertFalse("no Delete before the routine is saved", shows("Delete routine"))
        assertEquals(emptyList<StretchRoutine>(), routines())

        compose.onAllNodesWithText("Done").onFirst().performClick()
        settled { routines().isNotEmpty() }
        assertEquals(listOf("Evening"), routines().map { it.name })
        assertEquals(emptyList<RoutineStretch>(), rows())
    }

    @Test fun `a pick after a restore while the first create is pending saves under the same id, so one routine`() {
        io { dao.upsertStretches(listOf(Stretch("fold", "Fold"), Stretch("reach", "Reach"))) }
        val restore = StateRestorationTester(compose)
        openNewRoutine(restore)
        val hold = Hold()
        pick("Fold")
        restore.emulateSavedInstanceStateRestore()
        pick("Reach") // the restored editor is still a draft: the list does not hold its id yet

        hold.release()
        settled { routines().isNotEmpty() }
        val id = routines().single().id
        assertEquals(listOf(RoutineStretch(rows().single().id, id, "fold", 0)), rows())
    }

    @Test fun `Done while the first stretch's create is pending, then a refusal, saves the routine without stretches`() {
        io { dao.upsertStretches(listOf(Stretch("fold", "Fold"))) }
        openNewRoutine(StateRestorationTester(compose))
        compose.onNodeWithContentDescription("Routine name").performTextReplacement("Evening")
        val hold = Hold()
        pick("Fold")
        compose.onAllNodesWithText("Done").onFirst().performClick()

        hold.release("DELETE FROM stretches WHERE id = 'fold'")
        settled { routines().isNotEmpty() }
        assertEquals(listOf("Evening"), routines().map { it.name })
        assertEquals(emptyList<RoutineStretch>(), rows())
    }

    @Test fun `Done while the first stretch's create is pending, then a success, saves one routine with that stretch`() {
        io { dao.upsertStretches(listOf(Stretch("fold", "Fold"))) }
        openNewRoutine(StateRestorationTester(compose))
        val hold = Hold()
        pick("Fold")
        compose.onAllNodesWithText("Done").onFirst().performClick()

        hold.release()
        settled { routines().isNotEmpty() && rows().isNotEmpty() }
        val id = routines().single().id
        assertEquals(listOf(RoutineStretch(rows().single().id, id, "fold", 0)), rows())
    }

    @Test fun `the draft's writes run in the order asked, each after the one before it ends`() {
        val first = CompletableDeferred<Unit>()
        val order = mutableListOf<String>()
        vm.runSerial { first.await(); order += "first stretch" }
        vm.runSerial { order += "Done" }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(emptyList<String>(), order)
        first.complete(Unit)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("first stretch", "Done"), order)
    }
}
