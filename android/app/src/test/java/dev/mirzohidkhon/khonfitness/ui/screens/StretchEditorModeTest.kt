package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
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
 * The stretch editor's mode comes from the route alone: an edit of a stretch that is gone, when the editor opens or
 * while it is open, writes nothing on Done, and only the new-stretch route (no id) creates a stretch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class StretchEditorModeTest {
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

    private fun stretches() = runBlocking(Dispatchers.IO) { dao.stretchesOnce() }

    /**
     * Opens the editor with the clock held: a blank editor shows the hip flexor guide, which plays without end, so
     * frames run only when the test asks for them ([frame], [until]).
     */
    private fun openEditor(id: String?) {
        compose.mainClock.autoAdvance = false
        compose.setContent { KhonTheme { StretchEditorScreen(vm, rememberNavController(), id) } }
        frame()
    }

    private fun frame() { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }

    /** Runs frames until [shown] holds; a database change reaches the screen within a few of them. */
    private fun until(shown: () -> Boolean) {
        repeat(250) { if (shown()) return; frame(); Thread.sleep(20) }
        throw AssertionError("the screen did not reach the expected state")
    }

    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /** Types [name] into the Name field (the first text field), then lets a frame pass so the field reports it. */
    private fun typeName(name: String) {
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement(name)
        frame()
    }

    /** Done, then time for a write it may start: a create lands well inside the wait. */
    private fun done() {
        compose.onNodeWithText("Done").performClick()
        runCatching { until { stretches().any { it.name == "Typed" } } }
    }

    @Test fun `a stretch deleted while its editor is open is not created again on Done`() {
        io { dao.upsertStretches(listOf(Stretch("fold", "Fold", cue = "Hinge"))) }
        openEditor("fold")
        until { shows("Delete stretch") }
        io { dao.deleteStretch("fold") }
        until { !shows("Delete stretch") }
        typeName("Typed")
        done()
        assertEquals(emptyList<Stretch>(), stretches())
    }

    @Test fun `an editor opened on a stretch that is gone writes nothing on Done`() {
        openEditor("ghost")
        typeName("Typed")
        done()
        assertEquals(emptyList<Stretch>(), stretches())
    }

    @Test fun `the new route creates the stretch on Done`() {
        openEditor(null)
        typeName("Typed")
        done()
        assertEquals(listOf("Typed"), stretches().map { it.name })
    }

    @Test fun `an edit keeps the fields an import changed meanwhile`() {
        io { dao.upsertStretches(listOf(Stretch("fold", "Fold", cue = "Hinge"))) }
        openEditor("fold")
        until { shows("Hinge") }
        io { dao.upsertStretches(listOf(Stretch("fold", "Fold", cue = "Imported cue"))) }
        typeName("Typed")
        done()
        assertEquals(listOf(Stretch("fold", "Typed", cue = "Imported cue")), stretches())
    }
}
