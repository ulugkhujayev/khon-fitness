package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
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
import java.time.format.TextStyle
import java.util.Locale

/** The Plan tab (UI06 P4, UI07): its sections, "+", the week strip, the rows' lines, first use and its own scroll position. */
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
    /** [text] shows once its data has loaded: a screen fills in as each of its flows emits. */
    private fun assertShows(text: String) = compose.waitUntil(5_000) { shows(text) }
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
        compose.onNode(hasText("Daily mobility") and hasText("0 stretches · 0 min")).assertExists()
        compose.onNodeWithText("Exercises").performScrollTo()
        compose.onNode(hasText("Exercises") and hasText("1")).assertExists() // live strength only
        compose.onNode(hasText("Stretches") and hasText("0")).assertExists()

        tap(compose.onNodeWithText("Zone 2 run"))
        waitFor { shows("Type") }
        assertShows(TYPE_NOTE)
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

    /** One week column by its spoken line ("Thursday, Push."). */
    private fun day(spoken: String) = compose.onNode(hasContentDescription(spoken, substring = true))

    /**
     * UI07 rows 9 and 11: at font scales 1.0 and 1.6 the week is one row of seven columns that spans the whole screen
     * width, each 1/7 of it (51 dp at 360) and a full tap target.
     */
    @Test fun `the week strip is one row of seven full width columns at font scale 1_0`() = stripFits(1f)
    @Test fun `the week strip is one row of seven full width columns at font scale 1_6`() = stripFits(1.6f)

    private fun stripFits(scale: Float) {
        seed()
        openPlan(scale = scale)
        val px = compose.density.density
        val root = compose.onRoot().fetchSemanticsNode().size.width
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").map { day("$it,").fetchSemanticsNode() }
        assertEquals("x$scale rows of the week", 1, days.map { it.positionInRoot.y }.distinct().size)
        assertEquals("x$scale first column at the screen edge", 0f, days.first().positionInRoot.x, 1f)
        assertEquals("x$scale last column at the screen edge", root.toFloat(), days.last().positionInRoot.x + days.last().size.width, 1f)
        days.forEach {
            assertEquals("x$scale column width", root / 7f, it.size.width.toFloat(), 1f)
            assertTrue("x$scale column height ${it.size.height / px}", it.size.height >= 48 * px)
        }
    }

    /** UI07 rows 9 and 10: today is selected first; a tap selects a day and names it in full under the strip. */
    @Test fun `a tap on a day selects it and names it under the strip`() {
        seed()
        io { dao.upsert(Exercise("nor", "Norwegian 4×4 on the bike, long name that wraps", Kind.CARDIO, modalityId = "bike")); dao.upsert(WeekPlan(4, ItemType.CARDIO, "nor")) }
        openPlan()
        val today = vm.today()
        val todayName = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        day("$todayName,").assertIsSelected()
        val long = "Norwegian 4×4 on the bike, long name that wraps"
        val todayItem = when (today.dayOfWeek.value) { 1 -> "Push"; 4 -> long; else -> "Rest" }
        assertShows(weekdayShort(today.dayOfWeek.value) + " · " + todayItem)
        waitFor { compose.onAllNodes(hasContentDescription("Thursday, Norwegian", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        tap(day("Thursday,"))
        day("Thursday,").assertIsSelected()
        assertShows("Thu · $long")
        if (todayName != "Thursday") day("$todayName,").assertIsNotSelected()
        // Monday is a program day.
        tap(day("Monday,"))
        assertShows("Mon · Push")
        assertFalse(shows("Thu · $long"))
    }

    /** UI07 row 10: each column is a tab with its selected state, spoken as its day's line. */
    @Test fun `each week column is a tab spoken as its line`() {
        seed()
        openPlan()
        val node = day("Monday, Push.").fetchSemanticsNode()
        assertEquals(Role.Tab, node.config[SemanticsProperties.Role])
        assertTrue(node.config.contains(SemanticsProperties.Selected))
        assertTrue(node.config.contains(SemanticsActions.OnClick))
        assertEquals(1, compose.onAllNodes(hasContentDescription("Monday, Push.", substring = true)).fetchSemanticsNodes().size)
        // The letter and the symbol add nothing to the spoken line.
        assertEquals(listOf("Monday, Push."), node.config[SemanticsProperties.ContentDescription].map { it.substringBefore(" Today") })
        assertFalse(node.config.contains(SemanticsProperties.Text))
    }

    @Test fun `first use shows no programs, Create program and a week of rest`() {
        openPlan()
        assertShows("No programs yet")
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
