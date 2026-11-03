package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
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

/** The History tab (UI06 P3): Log and Progress pages, the exercise detail, first use and kept scroll positions. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HistoryScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var nav: NavHostController
    private val dao get() = app.db.dao()
    private val today = LocalDate.now()
    private val lastMonday = weekOf(today).minusWeeks(1)

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() { app.scope.cancel(); app.db.close() }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }

    /** History inside a nav host whose other routes are stubs that name themselves; Today is the start, as in the app. */
    private fun show() {
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, startDestination = Routes.TODAY) {
                    composable(Routes.TODAY) { Text("Today screen") }
                    composable(Routes.HISTORY) { HistoryScreen(vm, nav) }
                    composable("session/{id}") { e -> Text("Session ${e.arguments?.getString("id")}") }
                    composable("cardio/{id}") { e -> Text("Cardio ${e.arguments?.getString("id")}") }
                    composable("lift/{id}") { e -> ExerciseDetailScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("cardiotype/{id}") { e -> CardioTypeScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                }
            }
        }
        compose.runOnIdle { nav.navigate(Routes.HISTORY) }
        compose.waitForIdle()
    }

    private fun until(shown: () -> Boolean) = compose.waitUntil(5_000) { shown() }
    private fun shows(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(text: String) = until { shows(text) }

    private val push = Program("push", "Push", active = true)

    /** Mon-Sat push, Sunday rest; today is always push (a one-day change), so today's row is the same on any weekday. */
    private suspend fun plan() {
        dao.upsertExercises(listOf(
            Exercise("bench", "Barbell Bench Press", Kind.STRENGTH, primaryMuscle = "chest", secondaryMuscles = "shoulders,triceps"),
            Exercise("legpress", "Leg Press", Kind.STRENGTH, primaryMuscle = "quads", secondaryMuscles = "glutes"),
            Exercise("run", "Easy run", Kind.CARDIO, modalityId = "run"),
        ))
        dao.upsertModalities(listOf(Modality("run", "Run", 0xFF7CE0A8)))
        dao.upsert(push)
        dao.upsertWeekPlan((1..7).map { WeekPlan(it, if (it == 7) ItemType.REST else ItemType.PROGRAM, if (it == 7) null else "push") })
        dao.upsert(DayOverride(today.iso(), ItemType.PROGRAM, "push"))
    }

    private var n = 0
    private fun set(session: String, kg: Double, reps: Int, ex: String = "bench", name: String = "Barbell Bench Press", status: String = SetStatus.DONE) =
        SetLog("l${n++}", session, 0, false, 0, n, n, ex, name, 8, 12, 2.5, weightKg = kg, reps = reps, status = status)

    private fun gym(id: String, date: LocalDate, finished: Boolean = true) =
        Session(id, date.iso(), startedAt = date.toEpochDay() * 86_400_000, finishedAt = if (finished) date.toEpochDay() * 86_400_000 + 3_480_000 else null, itemType = ItemType.PROGRAM, programId = "push", programName = "Push")

    /** Last week: Monday done (a record on Wednesday), Tuesday skipped, Sunday rest with a stretch and a bodyweight. */
    private suspend fun lastWeek() {
        dao.upsertSessions(listOf(gym("mon", lastMonday), gym("wed", lastMonday.plusDays(2))))
        dao.upsertSetLogs(listOf(set("mon", 60.0, 8), set("mon", 60.0, 8), set("wed", 60.0, 8), set("wed", 62.5, 6), set("wed", 60.0, 5, status = SetStatus.SKIPPED)))
        dao.upsert(Bodyweight(lastMonday.plusDays(6).iso(), 83.0))
        dao.upsertStretchSessions(listOf(StretchSession("st", "daily", "Daily", lastMonday.plusDays(6).iso(), 1L, 600, 9, 0)))
    }

    @Test fun `log opens first with the week, the card and today's row`() {
        io { plan(); lastWeek() }
        show()
        waitFor("Today: Push")
        compose.onNodeWithText("Log").assertHeightIsAtLeast(48.dp)
        assertTrue(compose.onNodeWithText("Log").fetchSemanticsNode().config.getOrElse(SemanticsProperties.Selected) { false })
        compose.onNodeWithText(weekTitle(weekOf(today))).assertExists()
        compose.onNodeWithContentDescription("Previous week").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        assertTrue(shows("planned done"))
        assertTrue(shows("Not started"))
        // › stops at this week.
        assertTrue(compose.onNodeWithContentDescription("Next week").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled))
    }

    @Test fun `last week shows done, PR, skipped and rest rows, and stops at the first session's week`() {
        io { plan(); lastWeek() }
        show()
        waitFor("Today: Push")
        compose.onNodeWithContentDescription("Previous week").performClick()
        waitFor(weekTitle(lastMonday))
        assertTrue(shows("2 of 2 sets · 1 t · 58 min"))
        assertTrue(shows("2 of 3 sets · 0.9 t · 58 min · 1 PR"))
        assertTrue(shows("Skipped · Push"))
        assertTrue(shows("Stretch ✓ · 83.0 kg"))
        assertTrue(shows("2 of 6 planned done"))
        assertTrue(shows("4 missed"))
        assertTrue(compose.onNodeWithContentDescription("Previous week").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled))
        compose.onNodeWithText("2 of 2 sets · 1 t · 58 min", substring = true).performClick()
        waitFor("Session mon")
    }

    @Test fun `a running session today shows its progress and opens the session`() {
        io {
            plan()
            dao.upsert(gym("now", today, finished = false))
            dao.upsertSetLogs(listOf(set("now", 60.0, 8), set("now", 60.0, 8, status = SetStatus.PENDING), set("now", 60.0, 8, status = SetStatus.PENDING)))
        }
        show()
        waitFor("In progress · 1 of 3 sets")
        compose.onNodeWithText("In progress · 1 of 3 sets").performClick()
        waitFor("Session now")
    }

    @Test fun `an imported cardio session shows its source and counts in the week`() {
        io {
            plan()
            dao.upsert(Session("hc", today.iso(), 1L, 2L, ItemType.CARDIO, exerciseId = "run", exerciseName = "Easy run", timeSec = 1800, distanceM = 5000, sourceId = "rec1"))
        }
        show()
        waitFor("5 km · 30 min · 6:00\u00A0/\u2060km · Health Connect")
        assertTrue(shows("30 min cardio · 5 km"))
    }

    @Test fun `progress lists records and opens the exercise detail`() {
        io { plan(); lastWeek() }
        show()
        waitFor("Today: Push")
        compose.onNodeWithText("Progress").performClick()
        waitFor("Personal records · heaviest weight lifted")
        compose.onNodeWithText("Progress").assertHeightIsAtLeast(48.dp)
        assertTrue(shows("62.5 kg × 6"))
        assertTrue(shows("Sets per muscle · this week / last week"))
        assertTrue(shows("No cardio in the last 30 days."))
        compose.onNodeWithText("Barbell Bench Press").performClick()
        waitFor("Heaviest set per session")
        assertTrue(shows("Best"))
        assertTrue(shows("60×8 · 62.5×6"))
        compose.onNodeWithText("60×8 · 62.5×6").performClick()
        waitFor("Session wed")
    }

    @Test fun `more than five records fold under All n exercises`() {
        io {
            plan()
            dao.upsert(gym("g", lastMonday))
            dao.upsertSetLogs((1..7).map { set("g", 20.0 + it, 8, ex = "e$it", name = "Lift $it") })
        }
        show()
        waitFor("Today: Push")
        compose.onNodeWithText("Progress").performClick()
        waitFor("All 7 exercises")
        assertEquals(5, compose.onAllNodesWithText("Lift ", substring = true).fetchSemanticsNodes().size)
        compose.onNodeWithText("All 7 exercises").performScrollTo().performClick()
        waitFor("Show fewer")
        assertEquals(7, compose.onAllNodesWithText("Lift ", substring = true).fetchSemanticsNodes().size)
    }

    @Test fun `an exercise without sets opens with its muscle and No sets yet`() {
        io { plan() }
        show()
        waitFor("Today: Push")
        compose.runOnIdle { nav.navigate(Routes.exerciseDetail("legpress")) }
        waitFor("No sets yet")
        assertTrue(shows("Leg Press"))
        assertTrue(shows("Quads, Glutes"))
    }

    @Test fun `first use shows planned rows and one empty line per Progress section`() {
        io { plan() }
        show()
        waitFor("0 of ")
        assertTrue(shows("planned done"))
        assertTrue(shows("Planned: Push") || today.dayOfWeek.value >= 6)
        compose.onNodeWithText("Progress").performClick()
        waitFor("No records yet. Save a gym session to see them.")
        assertTrue(shows("No sets this week or last week."))
        assertTrue(shows("No entries yet. Add one on Today."))
        assertTrue(shows("No cardio in the last 30 days."))
    }

    @Test fun `bodyweight ranges show the point or say the range is empty`() {
        io {
            plan()
            dao.upsertBodyweights(listOf(Bodyweight(today.minusDays(60).iso(), 84.0), Bodyweight(today.minusDays(50).iso(), 83.5)))
        }
        show()
        waitFor("Today: Push")
        compose.onNodeWithText("Progress").performClick()
        waitFor("No entries in this range")
        compose.onNodeWithContentDescription("3 months").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
        waitFor("−0.5 kg since")
    }

    /**
     * Each page keeps its own scroll position. A tab switch saves History's state the way a restore does (the nav
     * host's saveState), so the page shown and both positions must come back from saved state.
     */
    @Test fun `each page keeps its scroll position, also across a saved state restore`() {
        io {
            plan(); lastWeek()
            // Two sessions on each day of this week, so the Log scrolls too.
            val days = (0L..6L).map { weekOf(today).plusDays(it) }
            dao.upsertSessions(days.flatMap { d -> listOf(gym("a$d", d).copy(startedAt = 1), gym("b$d", d).copy(startedAt = 2)) })
            dao.upsertSetLogs((1..7).map { set("mon", 20.0 + it, 8, ex = "e$it", name = "Lift $it") })
        }
        val tester = StateRestorationTester(compose)
        tester.setContent { KhonTheme { HistoryScreen(vm, rememberNavController()) } }
        waitFor("planned done")
        compose.onAllNodesWithText("Push", substring = true).fetchSemanticsNodes().size.let { assertTrue(it >= 14) }
        compose.onNodeWithText("Progress").performClick()
        waitFor("Personal records · heaviest weight lifted")
        compose.onNodeWithText("No cardio in the last 30 days.").performScrollTo()
        // The page switch scrolls with the page, as in the prototype: scroll up to it a little, and keep that place.
        compose.onNodeWithText("Log").performScrollTo()
        val progressY = top("No cardio in the last 30 days.")
        assertTrue(progressY > 0)
        compose.onNodeWithText("Log").performClick()
        waitFor("planned done")
        val logTop = top("planned done")
        // Scroll the Log to its end: its own position, apart from Progress.
        compose.onAllNodesWithText("Push", substring = true).let { it[it.fetchSemanticsNodes().size - 1].performScrollTo() }
        val logY = top("planned done")
        assertTrue(logY < logTop)
        tester.emulateSavedInstanceStateRestore()
        waitFor("planned done")
        compose.waitForIdle()
        assertEquals(logY, top("planned done"), 0.5f)
        compose.onNodeWithText("Log").performScrollTo()
        compose.onNodeWithText("Progress").performClick()
        waitFor("No cardio in the last 30 days.")
        compose.waitForIdle()
        assertEquals(progressY, top("No cardio in the last 30 days."), 0.5f)
    }

    private fun top(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().first().positionInRoot.y
}
