package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.timer.TimerService
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.openTab
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * UI06 P5 rows 8 and 11: every tap target on the tabs, the bottom bar and every screen they open (editors, the cardio
 * form, the timer, the stretch player, settings, review, the lists) is 48 x 48 dp or more, at the S25 size. Each test
 * walks every node with a click action on one screen, icon buttons and text fields included, and lists the small ones.
 * Row 3: the session's swap symbol, spoken "Swap <name>", opens the swap sheet; the name's own tap stays.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TapTargetTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var nav: NavHostController
    private val dao get() = app.db.dao()
    private val today = LocalDate.now()
    private var shown by androidx.compose.runtime.mutableStateOf(true)

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() {
        TimerService.state.value = null
        shown = false
        compose.waitForIdle()
        vm.viewModelScope.cancel(); app.scope.cancel()
        compose.waitForIdle()
        app.db.close()
    }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun shows(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    /** [text] shows once its data has loaded: a screen fills in as each of its flows emits. */
    private fun assertShows(text: String) = compose.waitUntil(5_000) { shows(text) }
    private fun named(label: String) = compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(shown: () -> Boolean) = compose.waitUntil(5_000) { shown() }

    private fun seed() = io {
        dao.upsert(Modality("run", "Run", 0xFF30D158))
        dao.upsert(Exercise("bench", "Bench press", Kind.STRENGTH, primaryMuscle = "chest"))
        dao.upsert(Exercise("fly", "Cable fly", Kind.STRENGTH, primaryMuscle = "chest"))
        dao.upsert(Exercise("z2", "Zone 2 run", Kind.CARDIO, modalityId = "run"))
        dao.upsert(Program("push", "Push day", active = true))
        dao.upsert(Block("b1", "push", 0))
        dao.upsert(BlockExercise("be1", "b1", 0, "bench", sets = 2))
        dao.upsert(WeekPlan(today.dayOfWeek.value, ItemType.PROGRAM, "push"))
        dao.upsert(Exercise("hiit", "Bike intervals", Kind.CARDIO, modalityId = "run", intervals = true, workSec = 60, restSec = 30, rounds = 4))
        dao.upsert(StretchRoutine("daily", "Daily mobility", active = true))
        dao.upsert(Stretch("fold", "Forward fold", seconds = 40))
        dao.upsertRoutineStretches(listOf(RoutineStretch("r1", "daily", "fold", 0)))
        dao.upsert(Session(id = "old", date = today.minusDays(2).toString(), startedAt = System.currentTimeMillis() - 2 * 86_400_000L,
            finishedAt = System.currentTimeMillis() - 2 * 86_400_000L + 3_600_000L, itemType = ItemType.PROGRAM, programId = "push", programName = "Push day"))
        dao.upsertSetLogs(listOf(SetLog("o1", "old", 0, false, 0, 0, 0, "bench", "Bench press", 6, 10, 2.5, 60.0, 8, SetStatus.DONE)))
    }

    /** The three tabs and the screens they push, as in the app's graph. */
    private fun openApp() {
        compose.setContent {
            if (shown) KhonTheme {
                nav = rememberNavController()
                NavHost(nav, Routes.TODAY) {
                    composable(Routes.TODAY) { TodayScreen(vm, nav) }
                    composable(Routes.HISTORY) { HistoryScreen(vm, nav) }
                    composable(Routes.PROGRAMS) { PlanScreen(vm, nav) }
                    composable("session/{id}") { e -> SessionScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("lift/{id}") { e -> ExerciseDetailScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable(Routes.LIBRARY) { LibraryScreen(vm, nav) }
                    composable(Routes.WEEK_PLAN) { WeekPlanScreen(vm, nav) }
                    composable("exercise/{id}") { e -> ExerciseEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
                    composable("review/{id}") { e -> ReviewScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("cardio/{id}") { e -> CardioScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("program/{id}") { e -> ProgramEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("block/{programId}/{blockId}") { e -> BlockEditorScreen(vm, nav, e.arguments?.getString("programId") ?: "", e.arguments?.getString("blockId") ?: "") }
                    composable("exercise-new/{kind}") { e -> ExerciseEditorScreen(vm, nav, null, newKind = e.arguments?.getString("kind") ?: Kind.STRENGTH) }
                    composable("timer/{id}") { e -> TimerScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("stretch/{id}") { e -> StretchScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("routine/{id}") { e -> RoutineEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("stretcheditor/{id}") { e -> StretchEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("cardiotype/{id}") { e -> CardioTypeScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable(Routes.LIFTS) { LiftListScreen(vm, nav) }
                    composable(Routes.STRETCHES) { StretchLibraryScreen(vm, nav) }
                    composable(Routes.IMPORT) { ImportScreen(vm, nav) }
                }
            }
        }
        waitFor { named("Bodyweight in kilograms") }
        compose.waitForIdle()
    }

    private fun describe(n: SemanticsNode): String =
        n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
            ?: n.config.getOrNull(SemanticsProperties.Text)?.joinToString()
            ?: n.children.firstNotNullOfOrNull { c -> c.config.getOrNull(SemanticsProperties.Text)?.joinToString() } ?: "node ${n.id}"

    /**
     * Every clickable node on screen laid out under 48 dp on a side, with its size. The laid-out size, not
     * touchBoundsInRoot: Compose pads that to 48 dp only where no neighbor overlaps, so it passes a 34 dp capsule too.
     * Text fields count too: a set's kg and reps fields share a row on a 360 dp phone, each between its two steppers.
     */
    private fun small(): List<String> = compose.onAllNodes(hasClickAction(), useUnmergedTree = false).fetchSemanticsNodes().mapNotNull { n ->
        val w = with(compose.density) { n.size.width.toDp().value }; val h = with(compose.density) { n.size.height.toDp().value }
        if (w < 47.5f || h < 47.5f) "${describe(n)} ${w.toInt()}x${h.toInt()}" else null
    }

    private fun assertTargets(screen: String) {
        compose.waitForIdle()
        val s = small()
        assertTrue("$screen: ${s.joinToString("; ")}", s.isEmpty())
    }

    private fun tab(route: String) { compose.runOnIdle { nav.openTab(route) }; compose.waitForIdle() }

    /** Opens [route] over Today, checks its targets once [marker] shows, and goes back. */
    private fun visit(route: String, marker: String) {
        compose.runOnIdle { nav.navigate(route) }
        compose.waitForIdle() // Today has faded out: what shows now is the screen's own
        waitFor { shows(marker) }
        assertTargets(route)
        compose.runOnIdle { nav.popBackStack() }
        compose.waitForIdle()
    }

    @Test fun `Today and its sheets have 48 dp targets`() {
        seed(); openApp()
        assertTargets("Today")
        compose.onNodeWithText("Eating window").performSemanticsAction(SemanticsActions.OnClick)
        waitFor { shows("Eating window") && named("Opens 15 minutes earlier") || named("Opens 15 minutes later") || shows("Turn on") }
        assertTargets("Eating window sheet")
    }

    @Test fun `History and an exercise opened from it have 48 dp targets`() {
        seed(); openApp(); tab(Routes.HISTORY)
        waitFor { shows("Push day") }
        assertTargets("History log")
        compose.onNodeWithText("Progress").performSemanticsAction(SemanticsActions.OnClick)
        waitFor { shows("Personal records") }
        assertTargets("History progress")
        compose.runOnIdle { nav.navigate(Routes.exerciseDetail("bench")) }
        waitFor { shows("Bench press") }
        assertTargets("Exercise detail")
    }

    @Test fun `Plan, its library and search have 48 dp targets`() {
        seed(); openApp(); tab(Routes.PROGRAMS)
        waitFor { shows("Library") }
        assertTargets("Plan")
        compose.runOnIdle { nav.navigate(Routes.LIBRARY) }
        waitFor { shows("Bench press") }
        compose.onNodeWithContentDescription("Search").performTextInput("ben")
        waitFor { named("Clear search") }
        assertTargets("Library with a search")
    }

    @Test fun `a running session has 48 dp targets and a swap symbol that opens the swap sheet`() {
        seed()
        io {
            dao.upsert(Session(id = "s1", date = today.toString(), startedAt = System.currentTimeMillis(), itemType = ItemType.PROGRAM, programId = "push", programName = "Push day"))
            dao.upsertSetLogs(listOf(SetLog("a", "s1", 0, false, 0, 0, 0, "bench", "Bench press", 6, 10, 2.5, 60.0, 8, SetStatus.PENDING)))
        }
        openApp()
        compose.runOnIdle { nav.navigate(Routes.session("s1")) }
        waitFor { named("Swap Bench press") }
        assertTargets("Session")
        compose.onNodeWithContentDescription("Swap Bench press").performSemanticsAction(SemanticsActions.OnClick)
        waitFor { shows("Swap Bench press") }
        assertShows("Cable fly")
        assertEquals("session/{id}", nav.currentDestination?.route)
    }

    /** Row 11: the shared controls the reviews found under 48 dp, each on its own. */
    @Test fun `the controls found small in review are 48 dp`() {
        compose.setContent {
            KhonTheme {
                androidx.compose.foundation.layout.Column {
                    dev.mirzohidkhon.khonfitness.ui.components.PlusButton("Add") {}
                    dev.mirzohidkhon.khonfitness.ui.components.SymbolButton(dev.mirzohidkhon.khonfitness.ui.Icons.Trash, "Remove") {}
                    dev.mirzohidkhon.khonfitness.ui.components.DemoControl(dev.mirzohidkhon.khonfitness.ui.Icons.Play, "Play demo") {}
                    dev.mirzohidkhon.khonfitness.ui.components.PlayerButton(dev.mirzohidkhon.khonfitness.ui.Icons.SkipBack, "Back to the previous stretch") {}
                    dev.mirzohidkhon.khonfitness.ui.components.Capsule("Done", {})
                    dev.mirzohidkhon.khonfitness.ui.components.TextButton("Edit") {}
                    TimeStepper(600, "Opens", 0..1439) {}
                    dev.mirzohidkhon.khonfitness.ui.components.SearchField("bench", {})
                }
            }
        }
        assertTargets("Shared controls")
        for (label in listOf("Add", "Remove", "Play demo", "Back to the previous stretch", "Opens 15 minutes earlier", "Clear search"))
            assertTrue(label, named(label))
    }

    @Test fun `the editors have 48 dp targets`() {
        seed(); openApp()
        visit(Routes.program("push"), "Bench press")
        visit(Routes.block("push", "b1"), "Bench press")
        visit(Routes.exercise("bench"), "Bench press")
        visit(Routes.exercise("z2"), "Zone 2 run")
        visit(Routes.exercise("hiit"), "Bike intervals")
        visit(Routes.newExercise(Kind.CARDIO), "Intensity")
        visit(Routes.routine("daily"), "Forward fold")
        visit(Routes.stretchEditor("fold"), "Forward fold")
    }

    @Test fun `the cardio form and the timer have 48 dp targets`() {
        seed()
        io {
            dao.upsert(Session(id = "c1", date = today.toString(), startedAt = System.currentTimeMillis(), itemType = ItemType.CARDIO, exerciseId = "z2", exerciseName = "Zone 2 run"))
            dao.upsert(Session(id = "t1", date = today.toString(), startedAt = System.currentTimeMillis(), itemType = ItemType.CARDIO, exerciseId = "hiit", exerciseName = "Bike intervals"))
        }
        openApp()
        visit(Routes.cardio("c1"), "Zone 2 run")
        // The timer runs for t1, so the screen does not start the service.
        TimerService.state.value = dev.mirzohidkhon.khonfitness.timer.TimerState("t1", "Bike intervals", dev.mirzohidkhon.khonfitness.timer.buildPhases(0, 60, 30, 4), remaining = 60.0)
        compose.runOnIdle { nav.navigate(Routes.timer("t1")) }
        compose.waitForIdle()
        waitFor { shows("Round 1 of 4") }
        assertTargets("Timer")
    }

    @Test fun `settings, review, the week plan and the lists have 48 dp targets`() {
        seed(); openApp()
        visit(Routes.SETTINGS, "Version")
        visit(Routes.review("old"), "Bench press")
        visit(Routes.WEEK_PLAN, "Monday")
        visit(Routes.LIFTS, "Cable fly")
        visit(Routes.STRETCHES, "Forward fold")
        visit(Routes.cardioType("run"), "No sessions yet")
        visit(Routes.IMPORT, "Import")
    }

    @Test fun `the stretch player has 48 dp targets`() {
        seed(); openApp()
        val phases = listOf(dev.mirzohidkhon.khonfitness.timer.Phase(dev.mirzohidkhon.khonfitness.timer.PhaseKind.WORK, 40, 0, label = "Forward fold", figure = "hipflexor"))
        TimerService.state.value = dev.mirzohidkhon.khonfitness.timer.TimerState("daily:x", "Daily mobility", phases, remaining = 40.0, stretch = true)
        // The demonstration plays forever: drive the clock by hand.
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { nav.navigate(Routes.stretch("daily")) }
        repeat(40) { compose.mainClock.advanceTimeByFrame() }
        assertTargets("Stretch player")
        assertTrue(named("Back to the previous stretch") && named("Skip") && named("Stop"))
        TimerService.state.value = null; shown = false
        repeat(5) { compose.mainClock.advanceTimeByFrame() }
        compose.mainClock.autoAdvance = true
    }

    @Test fun `the bottom bar has 48 dp targets`() {
        compose.setContent { KhonTheme { dev.mirzohidkhon.khonfitness.ui.BottomBar(0) {} } }
        assertTargets("Bottom bar")
        assertEquals(3, compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }
}
