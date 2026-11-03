package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.material3.Text
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.Block
import dev.mirzohidkhon.khonfitness.data.BlockExercise
import dev.mirzohidkhon.khonfitness.data.DayOverride
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Program
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.WeekPlan
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Merge asks first, with what moves (row 9), and then merges once, with no Undo (owner): one transaction that refuses,
 * writing nothing, when the source or the target is gone or they are the same exercise (attack case 13).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MergeConfirmTest {
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
        io {
            dao.upsertExercises(listOf(Exercise("a", "Old Bench", Kind.STRENGTH), Exercise("b", "Bench Press", Kind.STRENGTH)))
            dao.upsertSetLogs((0 until 3).map { SetLog("l$it", "s${it / 2}", 0, false, 0, it, it, "a", "Old Bench", 6, 10, 2.5, 60.0, 8, SetStatus.DONE) })
        }
    }

    @After fun close() { app.scope.cancel(); app.db.close() }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun until(shown: () -> Boolean) {
        repeat(250) { if (shown()) return; compose.waitForIdle(); Thread.sleep(20) }
        throw AssertionError("the screen did not reach the expected state")
    }
    private fun click(text: String): () -> Boolean = compose.onNodeWithText(text).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
    private fun tap(text: String) { val c = click(text); compose.runOnUiThread { c() } }
    private fun source() = runBlocking(Dispatchers.IO) { dao.exercise("a") }
    private fun routes() = nav.currentBackStack.value.mapNotNull { it.destination.route }

    /** The editor on a real back stack over a home screen, the merge target picked: the confirm shows. */
    private fun pickTarget() {
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, startDestination = "home") {
                    composable("home") { Text("Home") }
                    composable("exercise/{id}") { e -> ExerciseEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                }
            }
        }
        compose.runOnIdle { nav.navigate("exercise/a") }
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
        assertEquals(listOf("home", "exercise/{id}"), routes())
    }

    @Test fun `a double tap on Merge merges once and leaves once, with no Undo`() {
        pickTarget()
        val merge = click("Merge")
        compose.runOnUiThread { merge(); merge() }
        until { routes() == listOf("home") }
        assertNull(source())
        assertEquals(setOf("b"), runBlocking(Dispatchers.IO) { dao.allSetLogsOnce() }.map { it.exerciseId }.toSet())
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithText("Undo").fetchSemanticsNodes().isEmpty())
    }

    @Test fun `a merge refused when it runs keeps the editor open with the reason`() {
        pickTarget()
        io { dao.deleteExercise("b") } // the target goes after the confirm opened
        tap("Merge")
        until { shows(MERGE_REFUSED) }
        assertEquals(listOf("home", "exercise/{id}"), routes())
        assertEquals(setOf("a"), runBlocking(Dispatchers.IO) { dao.allSetLogsOnce() }.map { it.exerciseId }.toSet())
        // The guard came back: Back still leaves.
        val back = compose.onNodeWithContentDescription("Library").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnUiThread { back() }
        until { routes() == listOf("home") }
    }

    @Test fun `case 13 - a missing source, a missing target, or one exercise into itself writes nothing`() = runBlocking(Dispatchers.IO) {
        dao.upsert(Session(id = "c1", date = "2026-11-03", startedAt = 1L, itemType = ItemType.CARDIO, exerciseId = "a"))
        dao.upsertPrograms(listOf(Program("p1", "Push")))
        dao.upsertBlocks(listOf(Block("k0", "p1", 0)))
        dao.upsertBlockExercises(listOf(BlockExercise("r0", "k0", 0, "a")))
        dao.upsertWeekPlan(listOf(WeekPlan(1, ItemType.CARDIO, "a")))
        dao.upsertOverrides(listOf(DayOverride("2026-11-04", ItemType.CARDIO, "a")))
        val before = dao.backupSnapshot()
        assertFalse(vm.repo.mergeExercise("gone", "b"))
        assertFalse(vm.repo.mergeExercise("a", "gone"))
        assertFalse(vm.repo.mergeExercise("a", "a"))
        assertEquals(before, dao.backupSnapshot())
        // A merge that runs moves every use and deletes the source, in one go.
        assertTrue(vm.repo.mergeExercise("a", "b"))
        val after = dao.backupSnapshot()
        assertNull(dao.exercise("a"))
        assertTrue(after.setLogs.all { it.exerciseId == "b" })
        assertEquals("b", after.sessions.single().exerciseId)
        assertEquals("b", after.blockExercises.single().exerciseId)
        assertEquals("b", after.weekPlan.single().itemId)
        assertEquals("b", after.overrides.single().itemId)
    }
}
