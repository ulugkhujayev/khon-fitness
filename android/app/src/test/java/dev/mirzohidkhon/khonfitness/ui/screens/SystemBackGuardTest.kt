package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.Block
import dev.mirzohidkhon.khonfitness.data.BlockExercise
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.Modality
import dev.mirzohidkhon.khonfitness.data.Program
import dev.mirzohidkhon.khonfitness.data.RoutineStretch
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.Stretch
import dev.mirzohidkhon.khonfitness.data.StretchRoutine
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
 * System Back goes through each screen's exit guard ([ExitGuard]): every screen that writes and then leaves, over a
 * parent screen. A closing action and system Back in one frame pop once; system Back while a Save, Merge or Delete is
 * pending pops nothing, and the action then pops once; an action that completes after its screen left by another way
 * pops nothing. System Back alone still leaves, also after the app went to the background and came back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SystemBackGuardTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
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
            dao.upsertExercises(listOf(
                Exercise("a", "Bench", Kind.STRENGTH), Exercise("b", "Target", Kind.STRENGTH), Exercise("unused", "Unused", Kind.STRENGTH),
                Exercise("run", "Run", Kind.CARDIO, fields = "time"),
            ))
            dao.upsert(Program("p", "Program", active = true))
            dao.upsert(Block("k", "p", 0))
            dao.upsert(BlockExercise("r", "k", 0, "a"))
            dao.upsert(Modality("m", "Bike", 0xFF123456))
            dao.upsert(StretchRoutine("ro", "Routine"))
            dao.upsert(RoutineStretch("ro-1", "ro", "st", 0))
            dao.upsertStretches(listOf(Stretch("st", "Calf stretch"), Stretch("st2", "Neck stretch")))
            dao.upsert(Session("c", "2026-11-03", 1L, itemType = ItemType.CARDIO, exerciseId = "run", exerciseName = "Run", timeSec = 600))
            dao.upsert(Session("s", "2026-11-03", 1L, itemType = ItemType.PROGRAM, programId = "p", programName = "Program"))
            dao.upsertSetLogs(listOf(SetLog("l", "s", 0, false, 0, 0, 0, "a", "Bench", 6, 10, 2.5, 60.0, 8)))
            dao.upsert(Session("h", "2026-11-02", 1L, finishedAt = 2L, itemType = ItemType.PROGRAM, programId = "p", programName = "Program"))
        }
    }

    @After fun close() { compose.waitForIdle(); io { app.db.withTransaction {} }; compose.waitForIdle(); app.scope.cancel(); app.db.close() }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun enabled(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().firstOrNull()?.config?.getOrNull(SemanticsProperties.Disabled) == null
    private fun until(condition: () -> Boolean) {
        repeat(250) { if (condition()) return; compose.waitForIdle(); Thread.sleep(20) }
        throw AssertionError("the screen did not reach the expected state")
    }
    private fun settle() = repeat(5) { compose.waitForIdle(); Thread.sleep(20) }
    private fun routes() = nav.currentBackStack.value.mapNotNull { it.destination.route }
    private fun systemBack() = compose.activity.onBackPressedDispatcher.onBackPressed()
    private fun clickByText(text: String): () -> Boolean = compose.onNodeWithText(text).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
    private fun clickByLabel(label: String): () -> Boolean = compose.onNodeWithContentDescription(label).fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
    private fun lastByText(text: String): () -> Boolean = compose.onAllNodesWithText(text).fetchSemanticsNodes().last().config[SemanticsActions.OnClick].action!!

    private var hosted = false

    /** The screen at [route] over home and parent, a new back stack entry each time; returns once [ready] shows. */
    private fun open(route: String, ready: String) {
        if (!hosted) host()
        compose.runOnIdle { nav.popBackStack("home", inclusive = false); nav.navigate("parent"); nav.navigate(route) }
        until { shows(ready) }
        compose.waitForIdle()
    }

    private fun host() {
        hosted = true
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, startDestination = "home", enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None }, popExitTransition = { ExitTransition.None }) {
                    composable("home") { Text("Home") }
                    composable("parent") { Text("Parent") }
                    composable("other") { Text("Other") }
                    composable("program/{id}") { e -> ProgramEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("block/{programId}/{blockId}") { e -> BlockEditorScreen(vm, nav, e.arguments?.getString("programId") ?: "", e.arguments?.getString("blockId") ?: "") }
                    composable("exercise/{id}") { e -> ExerciseEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("routine/{id}") { e -> RoutineEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("stretcheditor/{id}") { e -> StretchEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("cardio/{id}") { e -> CardioScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("session/{id}") { e -> SessionScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                }
            }
        }
        compose.waitForIdle()
    }

    /** The screen left once: parent on top. */
    private fun leftOnce() {
        until { routes() == listOf("home", "parent") }
        settle()
        assertEquals(listOf("home", "parent"), routes())
    }

    /** Every screen with a guard, its ready text, and its closing action that runs in one frame. */
    private val screens = listOf(
        Triple("program/p", "Blocks") { clickByText("Done") },
        Triple("block/p/k", "Superset") { clickByText("Done") },
        Triple("exercise/a", "Merge into…") { clickByText("Done") },
        Triple("routine/ro", "Add stretch") { clickByText("Done") },
        Triple("stretcheditor/st", "Timing") { clickByText("Done") },
        Triple("cardio/c", "Notes") { clickByText("Save") },
        Triple("session/s", "Finish") { clickByLabel("Today") },
        Triple("session/h", "Edit") { clickByLabel("History") },
    )

    @Test fun `system Back alone leaves each screen once`() {
        for ((route, ready) in screens) {
            open(route, ready)
            compose.runOnUiThread { systemBack() }
            leftOnce()
        }
    }

    @Test fun `the closing action and system Back in one frame leave each screen once`() {
        for ((route, ready, action) in screens) {
            open(route, ready)
            val close = action()
            compose.runOnUiThread { close(); systemBack() }
            leftOnce()
        }
    }

    @Test fun `system Back first and the closing action in the same frame leave each screen once`() {
        for ((route, ready, action) in screens) {
            open(route, ready)
            val close = action()
            compose.runOnUiThread { systemBack(); close() }
            leftOnce()
        }
    }

    @Test fun `after the app was in the background, Done and system Back in one frame still leave once`() {
        open("exercise/a", "Merge into…")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        until { shows("Merge into…") }
        val done = clickByText("Done")
        compose.runOnUiThread { done(); systemBack() }
        leftOnce()
    }

    @Test fun `session Save and Discard with system Back in one frame leave once`() {
        open("session/s", "Finish")
        compose.onNodeWithText("Finish").performClick()
        until { shows("Save") }
        val save = clickByText("Save")
        compose.runOnUiThread { save(); systemBack() }
        leftOnce()
        until { runBlocking(Dispatchers.IO) { dao.session("s")!!.finished } }

        io { dao.upsert(Session("s2", "2026-11-03", 1L, itemType = ItemType.PROGRAM, programId = "p", programName = "Program")) }
        open("session/s2", "Finish")
        compose.onNodeWithText("Discard workout").performScrollTo().performClick()
        until { shows("Discard this workout?") }
        val discard = lastByText("Discard")
        compose.runOnUiThread { discard(); systemBack() }
        leftOnce()
    }

    /** Holds the database until the action's write is queued, presses system Back, then lets the write run. */
    private fun pendingThenBack(confirm: () -> Boolean, written: () -> Boolean) {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val blocker = app.scope.async { app.db.withTransaction { entered.complete(Unit); release.await() } }
        runBlocking { entered.await() }
        try {
            compose.runOnUiThread { confirm() }
            compose.waitForIdle()
            compose.runOnUiThread { systemBack() }
            compose.waitForIdle()
            // Back met the taken guard: the screen is still there while its write waits.
            assertEquals(3, routes().size)
        } finally { release.complete(Unit) }
        runBlocking { blocker.await() }
        until(written)
        leftOnce()
    }

    @Test fun `cardio Save pending, then system Back - one pop`() {
        open("cardio/c", "Notes")
        pendingThenBack(clickByText("Save")) { runBlocking(Dispatchers.IO) { dao.session("c")!!.finished } }
    }

    @Test fun `exercise Merge pending, then system Back - one pop`() {
        open("exercise/a", "Merge into…")
        compose.onNodeWithText("Merge into…").performScrollTo().performClick()
        until { shows("Target") }
        compose.runOnUiThread { clickByText("Target")() }
        until { shows("Merge Bench into Target?") }
        pendingThenBack(clickByText("Merge")) { runBlocking(Dispatchers.IO) { dao.exercise("a") == null } }
    }

    @Test fun `exercise Delete pending, then system Back - one pop`() {
        open("exercise/unused", "Merge into…")
        until { enabled("Delete") }
        compose.onNodeWithText("Delete").performScrollTo().performClick()
        until { shows("Delete Unused?") }
        pendingThenBack(lastByText("Delete")) { runBlocking(Dispatchers.IO) { dao.exercise("unused") == null } }
    }

    @Test fun `stretch Delete pending, then system Back - one pop`() {
        open("stretcheditor/st2", "Timing")
        until { shows("Delete stretch") && enabled("Delete stretch") }
        compose.onAllNodesWithText("Delete stretch").onFirst().performScrollTo().performClick()
        until { shows("Delete this stretch?") }
        pendingThenBack(lastByText("Delete")) { runBlocking(Dispatchers.IO) { dao.stretch("st2") == null } }
    }

    @Test fun `a Merge that completes after its screen left by another way pops nothing`() {
        open("exercise/a", "Merge into…")
        compose.onNodeWithText("Merge into…").performScrollTo().performClick()
        until { shows("Target") }
        compose.runOnUiThread { clickByText("Target")() }
        until { shows("Merge Bench into Target?") }
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val blocker = app.scope.async { app.db.withTransaction { entered.complete(Unit); release.await() } }
        runBlocking { entered.await() }
        val merge = clickByText("Merge")
        try {
            compose.runOnUiThread { merge() }
            compose.waitForIdle()
            // A widget request, say, moves on while the merge waits.
            compose.runOnUiThread { nav.navigate("other") }
            compose.waitForIdle()
        } finally { release.complete(Unit) }
        runBlocking { blocker.await() }
        until { runBlocking(Dispatchers.IO) { dao.exercise("a") == null } }
        settle()
        assertEquals(listOf("home", "parent", "exercise/{id}", "other"), routes())
    }
}
