package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.MERGE_UNDO_MS
import dev.mirzohidkhon.khonfitness.ui.MergeUndoBar
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

/** Merge asks first, with what moves (row 9); after it, "Merged into <B>" with Undo for 10 s (row 10). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MergeConfirmTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val dao get() = app.db.dao()

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
        io {
            dao.upsertExercises(listOf(Exercise("a", "Old Bench", Kind.STRENGTH), Exercise("b", "Bench Press", Kind.STRENGTH)))
            dao.upsertSetLogs((0 until 3).map { SetLog("l$it", "s${it / 2}", 0, false, 0, it, it, "a", "Old Bench", 6, 10, 2.5, 60.0, 8, SetStatus.DONE) })
        }
    }

    // The view model's flows outlive the screen by 5 s (WhileSubscribed): stop them before the database closes, or one
    // queries the closed database and its exception fails the next test.
    @After fun close() { vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun until(shown: () -> Boolean) {
        repeat(250) { if (shown()) return; compose.waitForIdle(); Thread.sleep(20) }
        throw AssertionError("the screen did not reach the expected state")
    }
    private fun tap(text: String) {
        val click = compose.onNodeWithText(text).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread { click() }
    }
    private fun source() = runBlocking(Dispatchers.IO) { dao.exercise("a") }

    private fun pickTarget() {
        compose.setContent { KhonTheme { ExerciseEditorScreen(vm, rememberNavController(), "a") } }
        until { shows("Merge into…") }
        compose.onNodeWithText("Merge into…").performScrollTo().performClick()
        until { shows("Bench Press") }
        tap("Bench Press")
        until { shows("Merge Old Bench into Bench Press?") }
    }

    @Test fun `picking a target asks first, and Cancel merges nothing`() {
        pickTarget()
        assertNotNull(compose.onNodeWithText("3 sets and 2 sessions move to Bench Press. Old Bench is deleted.").fetchSemanticsNode())
        tap("Cancel")
        until { !shows("Merge Old Bench into Bench Press?") }
        assertNotNull(source())
        assertNull(vm.lastMerge.value)
    }

    @Test fun `Merge merges and keeps the snapshot for Undo`() {
        pickTarget()
        tap("Merge")
        until { vm.lastMerge.value != null }
        assertNull(source())
        assertEquals("Bench Press", vm.lastMerge.value!!.intoName)
    }

    @Test fun `the bar says where it merged, Undo puts it back, and it goes after 10 s`() {
        compose.mainClock.autoAdvance = false
        compose.setContent { KhonTheme { MergeUndoBar(vm) } }
        vm.mergeExercise("a", Exercise("b", "Bench Press", Kind.STRENGTH))
        until { compose.mainClock.advanceTimeByFrame(); shows("Merged into Bench Press") }
        tap("Undo")
        until { compose.mainClock.advanceTimeByFrame(); source() != null && !shows("Merged into Bench Press") }
        assertEquals(setOf("a"), runBlocking(Dispatchers.IO) { dao.allSetLogsOnce() }.map { it.exerciseId }.toSet())

        vm.mergeExercise("a", Exercise("b", "Bench Press", Kind.STRENGTH))
        until { compose.mainClock.advanceTimeByFrame(); shows("Merged into Bench Press") }
        compose.mainClock.advanceTimeBy(MERGE_UNDO_MS - 1_000)
        assertNotNull(compose.onNodeWithText("Undo").fetchSemanticsNode())
        compose.mainClock.advanceTimeBy(2_000)
        until { compose.mainClock.advanceTimeByFrame(); !shows("Merged into Bench Press") }
        assertNull(vm.lastMerge.value)
        assertNull(source())
    }
}
