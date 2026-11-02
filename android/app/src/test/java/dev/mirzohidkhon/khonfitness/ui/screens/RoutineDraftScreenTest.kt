package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
 * that succeeds saves no second routine. Meanwhile Add is off, also after a restore, and Name and Active edits land
 * after that create, with Done or Back.
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

    /** Add stretch, then [name] when the sheet opens: it stays shut while Add is off, and then nothing is picked. */
    private fun tryPick(name: String) {
        compose.onNodeWithText("Add stretch").performClick()
        compose.waitForIdle()
        Thread.sleep(300); compose.waitForIdle()
        if (shows(name)) compose.onNodeWithText(name).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    /** Every draft write queued so far has ended: a marker queued after them has run. */
    private fun drained() {
        val marker = CompletableDeferred<Unit>()
        vm.runSerial { marker.complete(Unit) }
        settled { marker.isCompleted }
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

    @Test fun `a configuration change while the first create is pending keeps Add off until the create ends`() {
        io { dao.upsertStretches(listOf(Stretch("fold", "Fold"), Stretch("reach", "Reach"))) }
        val restore = StateRestorationTester(compose)
        openNewRoutine(restore)
        val hold = Hold()
        pick("Fold")
        compose.onNodeWithText("Add stretch").assertIsNotEnabled()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Add stretch").assertIsNotEnabled()

        hold.release()
        settled { routines().isNotEmpty() && rows().isNotEmpty() }
        compose.onNodeWithText("Add stretch").assertIsEnabled()
        val id = routines().single().id
        assertEquals(listOf(RoutineStretch(rows().single().id, id, "fold", 0)), rows())
    }

    @Test fun `a pick while the first create is pending is ignored, so a refused create leaves no routine`() {
        io { dao.upsertStretches(listOf(Stretch("fold", "Fold"), Stretch("reach", "Reach"))) }
        openNewRoutine(StateRestorationTester(compose))
        val hold = Hold()
        pick("Fold")
        tryPick("Reach")

        hold.release("DELETE FROM stretches WHERE id = 'fold'")
        drained()
        assertEquals(emptyList<StretchRoutine>(), routines())
        assertEquals(emptyList<RoutineStretch>(), rows())
        compose.onNodeWithText("Add stretch").assertIsEnabled() // a draft again
    }

    @Test fun `Name and Active changed while the first create is pending, then Done, then a success, save the changes`() {
        io { dao.upsertStretches(listOf(Stretch("fold", "Fold"))) }
        openNewRoutine(StateRestorationTester(compose))
        val hold = Hold()
        pick("Fold") // creates "New routine", Active on (no routine yet)
        compose.onNodeWithContentDescription("Routine name").performTextReplacement("Evening")
        compose.onNodeWithContentDescription("Active").performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText("Done").onFirst().performClick()

        hold.release()
        settled { routines().isNotEmpty() && rows().isNotEmpty() }
        assertEquals(listOf("Evening" to false), routines().map { it.name to it.active })
        assertEquals(listOf("fold"), rows().map { it.stretchId })
    }

    @Test fun `Name and Active changed while the first create is pending, then Back, then a success, save the changes`() {
        io {
            dao.upsert(StretchRoutine("daily", "Daily", active = true, sortOrder = 0))
            dao.upsertStretches(listOf(Stretch("fold", "Fold")))
        }
        openStretching()
        compose.onNodeWithContentDescription("New routine").performClick()
        waitFor { shows("Add stretch") && vm.stretches.value.isNotEmpty() }
        val hold = Hold()
        pick("Fold") // creates "New routine", Active off (Daily is active)
        compose.onNodeWithContentDescription("Routine name").performTextReplacement("Evening")
        compose.onNodeWithContentDescription("Active").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Stretching").performClick() // Back, not Done
        waitFor { shows("Routines") }

        hold.release()
        settled { routines().size == 2 && rows().isNotEmpty() }
        val saved = routines().associateBy { it.id }
        assertEquals(StretchRoutine("daily", "Daily", active = false, sortOrder = 0), saved.getValue("daily"))
        val evening = routines().single { it.id != "daily" }
        assertEquals("Evening" to true, evening.name to evening.active)
        assertEquals(listOf(evening.id to "fold"), rows().map { it.routineId to it.stretchId })
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
