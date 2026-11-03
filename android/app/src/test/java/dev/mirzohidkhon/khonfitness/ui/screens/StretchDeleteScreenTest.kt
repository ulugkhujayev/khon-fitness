package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.data.StretchRoutine
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Delete in the stretch editor (contract row 12): off with the reason for a built-in stretch and for one a routine
 * row uses, counted once the row reaches the screen; a delete refused at confirm time keeps the stretch and shows why.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class StretchDeleteScreenTest {
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

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun stretches() = runBlocking(Dispatchers.IO) { dao.stretchesOnce() }

    /** Opens the editor with the clock held: the guide plays without end, so frames run only when the test asks. */
    private fun openEditor(id: String) {
        compose.mainClock.autoAdvance = false
        compose.setContent { KhonTheme { StretchEditorScreen(vm, rememberNavController(), id) } }
        frame()
    }

    private fun frame() { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }

    private fun until(shown: () -> Boolean) {
        repeat(250) { if (shown()) return; frame(); Thread.sleep(20) }
        throw AssertionError("the screen did not reach the expected state")
    }

    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /** Whether the Delete stretch action is there and can be tapped. */
    private fun deleteEnabled(): Boolean = compose.onAllNodesWithText("Delete stretch").fetchSemanticsNodes()
        .any { !it.config.contains(SemanticsProperties.Disabled) }

    private fun useIn(routine: String, stretch: String) = io {
        dao.upsert(StretchRoutine(routine, "Daily"))
        dao.upsertRoutineStretches(listOf(RoutineStretch("$routine-$stretch", routine, stretch, 0)))
    }

    @Test fun `the use count loads once the stretch reaches the screen, so Delete is off for a stretch in use`() {
        io { dao.upsertStretches(listOf(Stretch("fold", "Fold"))) }
        useIn("r", "fold")
        // The stretch list is still empty on the first frame: the count must wait for the row, not run once and miss it.
        openEditor("fold")
        until { shows("In use by 1 routine rows.") }
        assertFalse(deleteEnabled())
    }

    @Test fun `a built-in stretch cannot be deleted and says why`() {
        io { dao.upsertStretches(listOf(Stretch("plow", "Plow", builtin = true))) }
        openEditor("plow")
        until { shows("Built-in stretches cannot be deleted.") }
        assertFalse(deleteEnabled())
    }

    @Test fun `a delete refused at confirm time keeps the stretch and shows the reason`() {
        io { dao.upsertStretches(listOf(Stretch("fold", "Fold"))) }
        openEditor("fold")
        until { deleteEnabled() }
        compose.onNodeWithText("Delete stretch").performSemanticsAction(SemanticsActions.OnClick) // below the screen; a scroll would wait on the held clock
        until { shows("Delete this stretch?") }
        useIn("r", "fold") // a routine takes the stretch while the confirm is open
        compose.onNodeWithText("Delete").performSemanticsAction(SemanticsActions.OnClick) // in the sheet, which may still be sliding in
        until { shows("In use by 1 routine rows.") }
        assertEquals(listOf("fold"), stretches().map { it.id })
        assertFalse(deleteEnabled())
    }
}
