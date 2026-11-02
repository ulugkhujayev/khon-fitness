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
 * A new routine is a draft (attack cases E4 and E5): the "+" on Stretching writes nothing, leaving the draft writes
 * nothing, Done writes one routine with the typed name and Active, and the first stretch saves the routine once,
 * also when the screen comes back from saved state before Done.
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
}
