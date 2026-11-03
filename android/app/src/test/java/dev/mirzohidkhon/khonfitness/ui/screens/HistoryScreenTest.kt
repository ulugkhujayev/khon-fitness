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
import androidx.compose.ui.test.performTextInput
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

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

    /** A clock the test moves by hand. */
    private class TestClock(var now: LocalDateTime) : Clock() {
        override fun getZone(): ZoneId = ZoneId.systemDefault()
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now.atZone(zone).toInstant()
    }

    /** History inside a nav host whose other routes are stubs that name themselves; Today is the start, as in the app. */
    private fun show(clock: Clock = Clock.systemDefaultZone()) {
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, startDestination = Routes.TODAY) {
                    composable(Routes.TODAY) { Text("Today screen") }
                    composable(Routes.PROGRAMS) { Text("Programs screen") }
                    composable(Routes.HISTORY) { HistoryScreen(vm, nav, clock) }
                    composable("session/{id}") { e -> Text("Session ${e.arguments?.getString("id")}") }
                    composable("cardio/{id}") { e -> Text("Cardio ${e.arguments?.getString("id")}") }
                    composable("lift/{id}") { e -> ExerciseDetailScreen(vm, nav, e.arguments?.getString("id") ?: "", clock) }
                    composable("cardiotype/{id}") { e -> CardioTypeScreen(vm, nav, e.arguments?.getString("id") ?: "", clock) }
                    composable(Routes.LIFTS) { LiftListScreen(vm, nav, clock) }
                }
            }
        }
        compose.runOnIdle { nav.navigate(Routes.HISTORY) }
        compose.waitForIdle()
    }

    /** A bottom-bar tab tap, with the options KhonNav's bar uses. */
    private fun tab(route: String) {
        compose.runOnIdle { nav.navigate(route) { popUpTo(Routes.TODAY) { saveState = true }; launchSingleTop = true; restoreState = true } }
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
        // Today's row opens Today.
        compose.onNodeWithText("Today: Push").performClick()
        waitFor("Today screen")
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
        assertTrue(shows("83.0 kg"))
        assertTrue(shows("Stretch: Daily · 10 min · 9 of 9"))
        assertTrue(shows(" · 1 stretch"))
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
        // Every saved set shows, the skipped one marked.
        assertTrue(shows("60×8 · 62.5×6 · 60×5 skipped"))
        compose.onNodeWithText("60×8 · 62.5×6", substring = true).performClick()
        waitFor("Session wed")
    }

    @Test fun `five records show, All n exercises lists every strength exercise and reaches one without sets`() {
        io {
            plan()
            dao.upsert(gym("g", lastMonday))
            dao.upsertSetLogs((1..7).map { set("g", 20.0 + it, 8, ex = "e$it", name = "Lift $it") })
        }
        show()
        waitFor("Today: Push")
        compose.onNodeWithText("Progress").performClick()
        // 7 lifted (gone from the library) + Barbell Bench Press and Leg Press without sets.
        waitFor("All 9 exercises")
        assertEquals(5, compose.onAllNodesWithText("Lift ", substring = true).fetchSemanticsNodes().size)
        compose.onNodeWithText("All 9 exercises").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        waitFor("Lift 7")
        assertEquals(7, compose.onAllNodesWithText("Lift ", substring = true).fetchSemanticsNodes().size)
        assertTrue(shows("27 kg × 8"))
        compose.onNodeWithContentDescription("Search").performTextInput("leg")
        until { !shows("Lift 1") }
        compose.onNodeWithText("Leg Press").performClick()
        waitFor("No sets yet")
        assertTrue(shows("Quads, Glutes"))
    }

    @Test fun `the exercise list marks archived exercises and reaches a lost one with only skipped sets`() {
        io {
            plan()
            dao.upsertExercises(listOf(Exercise("fly", "Cable fly", Kind.STRENGTH, primaryMuscle = "chest", archived = true)))
            dao.upsert(gym("g", lastMonday))
            dao.upsertSetLogs(listOf(set("g", 50.0, 8, ex = "row", name = "Old row", status = SetStatus.SKIPPED)))
        }
        show()
        waitFor("Today: Push")
        compose.runOnIdle { nav.navigate(Routes.LIFTS) }
        waitFor("Cable fly")
        assertTrue(shows("Archived · No sets yet"))
        compose.onNodeWithText("Old row").performClick()
        waitFor("50×8 skipped")
    }

    @Test fun `records dated after today count nowhere in the Log, Progress, the list or a detail`() {
        // Today is this week's Wednesday; Thursday holds a gym session (also a lost exercise's set), a stretch run and
        // a bodyweight, Friday a run.
        val wed = weekOf(today).plusDays(2); val thu = wed.plusDays(1); val fri = wed.plusDays(2)
        io {
            plan()
            dao.upsertSessions(listOf(gym("g", thu), Session("c", fri.iso(), startedAt = 1, finishedAt = 2, itemType = ItemType.CARDIO, exerciseId = "run", exerciseName = "Easy run", timeSec = 1800, distanceM = 5000)))
            dao.upsertSetLogs(listOf(set("g", 60.0, 8), set("g", 40.0, 8, ex = "gone", name = "Old curl")))
            dao.upsertStretchSessions(listOf(StretchSession("st", "daily", "Daily", thu.iso(), 1L, 600, 9, 0)))
            dao.upsert(Bodyweight(thu.iso(), 80.0))
        }
        show(TestClock(wed.atTime(12, 0)))
        waitFor("0 of ${if (today.dayOfWeek.value == 7) 7 else 6} planned done")
        assertTrue(shows("0 sets · 0 t lifted · 0 min cardio · 0 km · 0 stretches"))
        assertTrue(shows("Planned: Push"))
        assertFalse(shows("Stretch:") || shows("80.0 kg") || shows("58 min") || shows("Easy run"))
        compose.onNodeWithText("Progress").performClick()
        waitFor("No records yet. Save a gym session to see them.")
        assertTrue(shows("No sets this week or last week."))
        assertTrue(shows("No entries yet. Add one on Today."))
        assertTrue(shows("No cardio in the last 30 days."))
        // Bench Press and Leg Press, no lost exercise.
        compose.onNodeWithText("All 2 exercises").performScrollTo().performClick()
        waitFor("Leg Press")
        assertFalse(shows("Old curl"))
        assertEquals(2, compose.onAllNodesWithText("No sets yet").fetchSemanticsNodes().size)
        compose.runOnIdle { nav.navigate(Routes.exerciseDetail("bench")) }
        waitFor("Chest, Shoulders, Triceps")
        assertTrue(shows("No sets yet"))
        compose.runOnIdle { nav.navigate(Routes.cardioType("run")) }
        waitFor("No sessions yet")
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
        // Today's row waits for the plan; before it loads every day reads rest.
        waitFor("Today: Push")
        assertTrue(shows("0 of "))
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
            // Four sessions on each day of this week up to today (later ones count nowhere), so the Log scrolls too.
            val days = (0L..6L).map { weekOf(today).plusDays(it) }.filter { it <= today }
            dao.upsertSessions(days.flatMap { d -> (1L..4L).map { gym("$it$d", d).copy(startedAt = it) } })
            dao.upsertSetLogs((1..7).map { set("mon", 20.0 + it, 8, ex = "e$it", name = "Lift $it") })
        }
        val tester = StateRestorationTester(compose)
        tester.setContent { KhonTheme { HistoryScreen(vm, rememberNavController()) } }
        waitFor("planned done")
        compose.onAllNodesWithText("Push", substring = true).fetchSemanticsNodes().size.let { assertTrue(it >= 9) }
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

    /**
     * A real tab switch (the bottom bar's navigate with saveState / restoreState, to Programs and back) keeps the page,
     * its scroll position, and each page's state: the week shown on Log, the bodyweight range on Progress. A page
     * switch keeps them too.
     */
    @Test fun `a bottom tab switch and a page switch keep the page, scroll, week and range`() {
        io {
            plan(); lastWeek()
            dao.upsertBodyweights(listOf(Bodyweight(today.minusDays(60).iso(), 84.0), Bodyweight(today.minusDays(50).iso(), 83.5)))
            dao.upsertSetLogs((1..7).map { set("mon", 20.0 + it, 8, ex = "e$it", name = "Lift $it") })
        }
        show()
        waitFor("Today: Push")
        // Log: last week. A page switch there and back keeps it.
        compose.onNodeWithContentDescription("Previous week").performClick()
        waitFor(weekTitle(lastMonday))
        compose.onNodeWithText("Progress").performClick()
        waitFor("Personal records · heaviest weight lifted")
        compose.onNodeWithText("Log").performClick()
        waitFor(weekTitle(lastMonday))
        // Progress: 3 months, then scroll to the end.
        compose.onNodeWithText("Progress").performClick()
        compose.onNodeWithContentDescription("3 months").performScrollTo().performClick()
        waitFor("−1.0 kg since")
        compose.onNodeWithText("No cardio in the last 30 days.").performScrollTo()
        val y = top("No cardio in the last 30 days.")
        tab(Routes.PROGRAMS)
        waitFor("Programs screen")
        tab(Routes.HISTORY)
        waitFor("No cardio in the last 30 days.")
        compose.waitForIdle()
        assertTrue(compose.onNodeWithText("Progress").fetchSemanticsNode().config.getOrElse(SemanticsProperties.Selected) { false })
        assertEquals(y, top("No cardio in the last 30 days."), 0.5f)
        assertTrue(shows("−1.0 kg since"))
        compose.onNodeWithText("Log").performScrollTo().performClick()
        waitFor(weekTitle(lastMonday))
    }

    /**
     * Midnight while History is open: a session that started on Sunday evening and still runs stays on Sunday (its
     * start date); Monday becomes today with its normal row, and the Log moves to the new week.
     */
    @Test fun `at midnight today moves on, a running session stays on its start day`() {
        val sunday = LocalDate.of(2026, 11, 8)
        val clock = TestClock(sunday.atTime(23, 59, 50))
        io {
            plan()
            dao.upsert(gym("late", sunday, finished = false).copy(startedAt = sunday.atTime(23, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
            dao.upsertSetLogs(listOf(set("late", 60.0, 8), set("late", 60.0, 8, status = SetStatus.PENDING)))
        }
        show(clock)
        waitFor(weekTitle(LocalDate.of(2026, 11, 2)))
        assertTrue(shows("In progress · 1 of 2 sets"))
        clock.now = sunday.plusDays(1).atTime(0, 0, 5)
        compose.mainClock.advanceTimeBy(15_000)
        waitFor(weekTitle(LocalDate.of(2026, 11, 9)))
        assertTrue(shows("Today: Push"))
        assertTrue(shows("Not started"))
        assertTrue(!shows("In progress"))
        compose.onNodeWithContentDescription("Previous week").performClick()
        waitFor(weekTitle(LocalDate.of(2026, 11, 2)))
        assertTrue(shows("In progress · 1 of 2 sets"))
    }

    /**
     * Cardio by type: the row's sparkline label with first and last values, a type used only before the 30 days says
     * so and still opens; the type detail shows pace, heart rate and interval charts with their titles.
     */
    @Test fun `cardio rows and the type detail show their charts and labels`() {
        io {
            plan()
            dao.upsertExercises(listOf(Exercise("hiit", "Bike intervals", Kind.CARDIO, modalityId = "bike", intervals = true)))
            dao.upsertModalities(listOf(Modality("bike", "Bike", 0xFFE0A87C)))
            dao.upsertSessions(listOf(
                Session("r1", today.minusDays(10).iso(), 1L, 2L, ItemType.CARDIO, exerciseId = "run", exerciseName = "Easy run", timeSec = 1800, distanceM = 5000, avgHr = 140),
                Session("r2", today.minusDays(3).iso(), 3L, 4L, ItemType.CARDIO, exerciseId = "run", exerciseName = "Easy run", timeSec = 3000, distanceM = 8000, avgHr = 150),
                Session("b1", today.minusDays(45).iso(), 5L, 6L, ItemType.CARDIO, exerciseId = "hiit", exerciseName = "Bike intervals", timeSec = 1200, avgHr = 160),
            ))
            dao.upsertIntervalLogs(listOf(IntervalLog("i1", "b1", 0, avgHr = 170), IntervalLog("i2", "b1", 1, avgHr = 174)))
        }
        show()
        waitFor("Today: Push")
        compose.onNodeWithText("Progress").performClick()
        waitFor("Cardio · last 30 days")
        compose.onNodeWithText("Distance per session").performScrollTo()
        assertTrue(shows("2 sessions · 13 km · 1 h 20 m · 6:09\u00A0/\u2060km"))
        assertTrue(shows("5 km · ${monthDay(today.minusDays(10))}"))
        assertTrue(shows("8 km · ${monthDay(today.minusDays(3))}"))
        compose.onNodeWithText("No sessions in 30 days").performScrollTo().performClick()
        waitFor("Heart rate in work intervals")
        assertTrue(shows("Average heart rate"))
        assertTrue(shows("Bike intervals · 20 min"))
        compose.runOnIdle { nav.popBackStack() }
        waitFor("Distance per session")
        compose.onNodeWithText("Run").performScrollTo().performClick()
        waitFor("Pace /km")
        assertTrue(shows("Average heart rate"))
        assertTrue(!shows("Heart rate in work intervals"))
        assertTrue(shows("Easy run · 8 km · 50 min · 6:15\u00A0/\u2060km"))
        compose.onNodeWithText("Easy run · 8 km", substring = true).performClick()
        waitFor("Cardio r2")
    }

    private fun top(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().first().positionInRoot.y
}
