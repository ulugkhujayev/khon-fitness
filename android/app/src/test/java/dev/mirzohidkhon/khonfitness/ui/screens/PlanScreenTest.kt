package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import android.content.res.Configuration
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle as TextStyleC
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.BottomBar
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.openTab
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.format.TextStyle
import java.util.Locale

/** The Plan tab (UI06 P4): its sections, "+", the week grid, the rows' lines, first use and its own scroll position. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PlanScreenTest {
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
    }

    // The view model's flows outlive the screen by 5 s (WhileSubscribed): stop them before the database closes, or one
    // queries the closed database and its exception fails the next test.
    @After fun close() { vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(shown: () -> Boolean) = compose.waitUntil(5_000) { shown() }
    private fun top(text: String) = compose.onNodeWithText(text).fetchSemanticsNode().positionInRoot.y
    private fun tap(node: SemanticsNodeInteraction) { node.performSemanticsAction(SemanticsActions.OnClick); compose.waitForIdle() }

    /** Today (a stand-in) and Plan as tabs, with the screens Plan opens, opened on Plan once [shown] text is there. */
    private fun openPlan(shown: String = "Library", scale: Float = 1f) {
        compose.setContent {
            val d = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
            KhonTheme {
                measurer = rememberTextMeasurer()
                textStyle = LocalTextStyle.current
                val nav = rememberNavController().also { nav = it }
                NavHost(nav, Routes.TODAY) {
                    composable(Routes.TODAY) { Text("Today screen") }
                    composable(Routes.PROGRAMS) { PlanScreen(vm, nav) }
                    composable("program/{id}") { e -> ProgramEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable(Routes.NEW_PROGRAM) { ProgramEditorScreen(vm, nav, null) }
                    composable("exercise/{id}") { e -> ExerciseEditorScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("exercise-new/{kind}") { e -> ExerciseEditorScreen(vm, nav, null, newKind = e.arguments?.getString("kind") ?: Kind.STRENGTH) }
                    composable("routine/{id}") { e -> Text("Routine ${e.arguments?.getString("id")}") }
                    composable(Routes.WEEK_PLAN) { WeekPlanScreen(vm, nav) }
                    composable(Routes.LIBRARY) { LibraryScreen(vm, nav) }
                    composable(Routes.STRETCHES) { StretchLibraryScreen(vm, nav) }
                }
                androidx.compose.runtime.LaunchedEffect(Unit) { if (nav.currentDestination?.route == Routes.TODAY) nav.openTab(Routes.PROGRAMS) }
            }
            }
        }
        waitFor { shows(shown) }
    }
    private lateinit var measurer: TextMeasurer
    private lateinit var textStyle: TextStyleC

    private fun seed() = io {
        dao.upsertPrograms(listOf(Program("push", "Push", active = true), Program("pull", "Pull", sortOrder = 1)))
        dao.upsert(Modality("run", "Run", 0xFF7CE0A8))
        dao.upsert(Exercise("bench", "Bench press", Kind.STRENGTH, primaryMuscle = "chest"))
        dao.upsert(Block("b1", "push", 0))
        dao.upsert(BlockExercise("be1", "b1", 0, "bench", sets = 3))
        dao.upsert(Exercise("dip", "Dip", Kind.STRENGTH, primaryMuscle = "chest", archived = true))
        dao.upsert(Exercise("z2run", "Zone 2 run", Kind.CARDIO, modalityId = "run"))
        dao.upsert(Exercise("oldrun", "Old run", Kind.CARDIO, modalityId = "run", archived = true))
        dao.upsertWeekPlan(listOf(WeekPlan(1, ItemType.PROGRAM, "push"), WeekPlan(4, ItemType.PROGRAM, "push")))
        dao.upsert(StretchRoutine("daily", "Daily mobility", active = true))
    }

    @Test fun `the third tab reads Plan`() {
        compose.setContent { KhonTheme { BottomBar(2) {} } }
        compose.onNodeWithText("Plan").assertIsSelected()
        assertFalse(shows("Programs"))
    }

    @Test fun `Plan shows its sections in order and no Settings`() {
        seed()
        openPlan()
        val titles = listOf("Plan", "This week", "Gym programs", "Cardio workouts", "Stretch routines")
        titles.zipWithNext().forEach { (a, b) -> assertTrue("$a above $b", top(a) < top(b)) }
        compose.onNodeWithText("Library").performScrollTo()
        assertTrue(top("Stretch routines") < top("Library"))
        assertFalse(shows("Settings"))
    }

    @Test fun `rows say what they hold and open their editors`() {
        seed()
        openPlan()
        // The rows show once the plan's queries answer.
        waitFor { compose.onAllNodes(hasText("Push") and hasText("1 block · 3 sets · Every Mon, Thu")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasText("Pull") and hasText("0 blocks · 0 sets · Not in the week")).assertExists()
        compose.onNode(hasText("Run") and hasText("Zone 2 run") and hasText("Steady · Not in the week")).assertExists()
        // The archived workout sits under Archived, after the live one.
        assertTrue(top("Zone 2 run") < top("Archived") && top("Archived") < top("Old run"))
        compose.onNode(hasText("Daily mobility") and hasText("0 min · 0 stretches")).assertExists()
        compose.onNodeWithText("Exercises").performScrollTo()
        compose.onNode(hasText("Exercises") and hasText("1")).assertExists() // live strength only
        compose.onNode(hasText("Stretches") and hasText("0")).assertExists()

        tap(compose.onNodeWithText("Zone 2 run"))
        waitFor { shows("Type") }
        assertTrue(shows(TYPE_NOTE))
    }

    @Test fun `plus opens each create flow`() {
        seed()
        openPlan()
        fun add(choice: String, opens: String) {
            compose.onNodeWithContentDescription("New").performClick()
            waitFor { shows(choice) }
            listOf("Gym program", "Cardio workout", "Stretch routine", "Exercise").forEach { assertTrue(it, shows(it)) }
            tap(compose.onNodeWithText(choice))
            waitFor { shows(opens) }
            compose.runOnUiThread { nav.popBackStack() }
            waitFor { shows("Gym programs") }
        }
        add("Gym program", "New program")
        add("Cardio workout", "New cardio workout")
        assertFalse("a new workout warns about no past sessions", shows(TYPE_NOTE))
        add("Exercise", "New exercise")
        compose.onNodeWithContentDescription("New").performClick()
        waitFor { shows("Stretch routine") }
        tap(compose.onNodeWithText("Stretch routine"))
        waitFor { compose.onAllNodesWithText("Routine", substring = true).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(2, runBlocking(Dispatchers.IO) { dao.stretchRoutinesOnce() }.size)
    }

    @Test fun `This week shows today's one-day change with a mark, and Edit opens the week plan`() {
        seed()
        val today = vm.today()
        io { dao.upsert(DayOverride(today.toString(), ItemType.CARDIO, "z2run")) }
        openPlan()
        val day = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        // The week shows the change once the plan's queries answer.
        waitFor { compose.onAllNodes(hasContentDescription("$day, Zone 2 run. Today. Changed for this day only.")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Edit week plan").performClick()
        waitFor { shows(WEEK_PLAN_NOTE) }
    }

    /**
     * At font scales 1.0, 1.3 and 1.6 each week cell holds its name at the full scaled size in two lines at most, with
     * no "…": above 1.15 the week wraps to rows of four cells. Native graphics, so text has its real width.
     */
    @GraphicsMode(GraphicsMode.Mode.NATIVE) @Test fun `the week grid names fit their cells at font scale 1_0`() = gridFits(1f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE) @Test fun `the week grid names fit their cells at font scale 1_3`() = gridFits(1.3f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE) @Test fun `the week grid names fit their cells at font scale 1_6`() = gridFits(1.6f)

    private fun gridFits(scale: Float) {
        seed()
        io {
            dao.upsert(Exercise("tempo", "Tempo run", Kind.CARDIO, modalityId = "run"))
            dao.upsertWeekPlan(listOf(WeekPlan(2, ItemType.CARDIO, "z2run"), WeekPlan(3, ItemType.PROGRAM, "pull"), WeekPlan(5, ItemType.CARDIO, "tempo")))
        }
        run {
            openPlan(scale = scale)
            val px = compose.density.density
            val names = mapOf("Monday" to "Push", "Tuesday" to "Zone 2 run", "Wednesday" to "Pull", "Thursday" to "Push", "Friday" to "Tempo run", "Saturday" to "Rest")
            val rows = names.map { (day, name) ->
                val cell = compose.onNode(hasContentDescription("$day, $name.", substring = true)).fetchSemanticsNode()
                val style = textStyle.merge(TextStyleC(fontSize = WEEK_CELL_NAME_SP.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center))
                val text = measurer.measure(name, style, overflow = TextOverflow.Ellipsis, maxLines = 2, constraints = Constraints(maxWidth = cell.size.width - (4 * px).toInt()))
                assertFalse("x$scale $day \"$name\" does not fit a ${cell.size.width / px} dp cell", text.hasVisualOverflow)
                cell.positionInRoot.y
            }
            assertEquals("x$scale rows of the week", if (scale > WEEK_GRID_ONE_ROW_MAX_SCALE) 2 else 1, rows.distinct().size)
        }
    }

    @Test fun `first use shows no programs, Create program and a week of rest`() {
        openPlan()
        assertTrue(shows("No programs yet"))
        assertEquals(7, compose.onAllNodes(hasContentDescription(", Rest.", substring = true)).fetchSemanticsNodes().size)
        tap(compose.onNodeWithText("Create program"))
        waitFor { shows("New program") }
    }

    @Test fun `Plan keeps its own scroll position across a tab switch`() {
        seed()
        io { dao.upsertPrograms((1..12).map { Program("p$it", "Program $it", sortOrder = 10 + it) }) }
        openPlan()
        // The twelve programs make the page scroll: wait for them, or the scroll below may find a short page.
        waitFor { shows("Program 12") }
        val unscrolled = top("Gym programs")
        compose.onNodeWithText("Stretches").performScrollTo()
        val scrolled = top("Stretch routines")
        assertNotEquals(unscrolled, top("Gym programs"))

        compose.runOnUiThread { nav.openTab(Routes.TODAY) }
        waitFor { shows("Today screen") }
        compose.runOnUiThread { nav.openTab(Routes.PROGRAMS) }
        waitFor { shows("Stretch routines") }
        assertEquals(scrolled, top("Stretch routines"))
    }
}
