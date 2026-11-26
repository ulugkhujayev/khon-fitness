package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.viewModelScope
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

/** The compare screen: its entry on Progress, the period sheet, the fact groups, an empty database and saved state. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class CompareScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var nav: NavHostController
    private val dao get() = app.db.dao()
    private val today = LocalDate.of(2026, 11, 27)
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.systemDefault()
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = LocalDateTime.of(2026, 11, 27, 10, 0).atZone(zone).toInstant()
    }

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() { vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun shows(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    private fun spoken(text: String) = compose.onAllNodesWithContentDescription(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(text: String) = compose.waitUntil(5_000) { shows(text) || spoken(text) }
    private fun tap(text: String) = compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick)

    private fun gym(id: String, date: LocalDate, program: String = "push") =
        Session(id, date.iso(), startedAt = date.toEpochDay(), finishedAt = date.toEpochDay() + 1, itemType = ItemType.PROGRAM, programId = program, programName = program)
    private var n = 0
    private fun set(s: String, ex: String, name: String, kg: Double, reps: Int) = SetLog("l${n++}", s, 0, false, 0, n, n, ex, name, 8, 12, 2.5, weightKg = kg, reps = reps, status = SetStatus.DONE)

    private suspend fun seed() {
        dao.upsertModalities(listOf(Modality("run", "Run", 0xFF7CE0A8)))
        dao.upsertExercises(listOf(
            Exercise("bench", "Bench press", Kind.STRENGTH, primaryMuscle = "chest"),
            Exercise("z2", "Zone 2 run", Kind.CARDIO, modalityId = "run"),
        ))
        dao.upsertPrograms(listOf(Program("push", "Push day", active = true), Program("legs", "Legs")))
        dao.upsertSessions(listOf(
            gym("a", LocalDate.of(2026, 10, 10)), gym("b", LocalDate.of(2026, 11, 20)),
            Session("r", LocalDate.of(2026, 11, 21).iso(), 1L, 2L, ItemType.CARDIO, exerciseId = "z2", exerciseName = "Zone 2 run", timeSec = 1800, distanceM = 6000),
        ))
        dao.upsertSetLogs(listOf(set("a", "bench", "Bench press", 60.0, 8), set("b", "bench", "Bench press", 65.0, 6)))
        dao.upsert(Bodyweight(LocalDate.of(2026, 11, 22).iso(), 83.0))
    }

    private fun show() {
        compose.setContent {
            KhonTheme {
                nav = rememberNavController()
                NavHost(nav, startDestination = Routes.HISTORY) {
                    composable(Routes.HISTORY) { HistoryScreen(vm, nav, clock) }
                    composable(Routes.COMPARE) { CompareScreen(vm, nav, clock) }
                    composable(Routes.TODAY) { Text("Today screen") }
                }
            }
        }
    }

    @Test fun `the Progress row opens the compare screen with the two default periods and every group`() {
        io { seed() }
        show()
        tap("Progress")
        waitFor("Compare two periods")
        compose.onNodeWithText("Compare two periods").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        waitFor("The 4 weeks before")
        assertEquals(Routes.COMPARE, nav.currentDestination?.route)
        assertTrue(shows("Oct 3 – Oct 30") && shows("Last 4 weeks") && shows("Oct 31 – Nov 27"))
        waitFor("Bench press, 60 kg × 8 in Oct 3 – Oct 30, 65 kg × 6 in Oct 31 – Nov 27")
        for (s in listOf("Days, 28 in Oct 3 – Oct 30, 28 in Oct 31 – Nov 27", "Gym sessions, 1 in Oct 3", "Average, none in Oct 3 – Oct 30, 83.0 kg in Oct 31 – Nov 27",
            "Chest, 0.3 in Oct 3 – Oct 30, 0.3 in Oct 31 – Nov 27", "Run · sessions, 0 in Oct 3 – Oct 30, 1 in Oct 31 – Nov 27", "Run · pace, none in Oct 3 – Oct 30, 5:00"))
            assertTrue(s, spoken(s))
        for (t in listOf("Bodyweight", "Sets per muscle · per week", "Best set", "Cardio")) assertTrue(t, shows(t))
    }

    @Test fun `the sheet lists the presets, the programs with sessions and custom dates, and a pick updates the column`() {
        io { seed() }
        show()
        compose.runOnIdle { nav.navigate(Routes.COMPARE) }
        waitFor("Oct 31 – Nov 27")
        tap("Oct 31 – Nov 27")
        waitFor("Right column")
        for (p in Preset.entries) assertTrue(p.label, shows(p.label))
        assertTrue(shows("Push day") && shows("Oct 10 – Nov 20"))
        assertFalse(shows("Legs"))
        assertTrue(shows("Custom dates"))
        tap("Push day")
        compose.waitUntil(5_000) { !shows("Right column") }
        assertTrue(shows("Oct 10 – Nov 20"))
        assertTrue(spoken("Days, 28 in Oct 3 – Oct 30, 42 in Oct 10 – Nov 20"))
        assertTrue(spoken("Gym sessions, 1 in Oct 3 – Oct 30, 2 in Oct 10 – Nov 20"))
    }

    @Test fun `custom dates show From and To, and a From after the To moves the To`() {
        io { seed() }
        show()
        compose.runOnIdle { nav.navigate(Routes.COMPARE) }
        waitFor("Oct 3 – Oct 30")
        tap("Oct 3 – Oct 30")
        waitFor("Left column")
        assertFalse(shows("From"))
        tap("Custom dates")
        waitFor("From")
        assertTrue(compose.onAllNodesWithText("To").fetchSemanticsNodes().isNotEmpty())
        assertTrue(shows("Oct 3 – Oct 30"))
        tap("From")
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("October 31", substring = true) or hasContentDescription("October 31", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText("October 31", substring = true) or hasContentDescription("October 31", substring = true))[0].performSemanticsAction(SemanticsActions.OnClick)
        tap("Done")
        compose.waitUntil(5_000) { spoken("Days, 1 in Oct 31, 28 in Oct 31 – Nov 27") }
    }

    @Test fun `with no data only the first group shows, with zeros`() {
        show()
        compose.runOnIdle { nav.navigate(Routes.COMPARE) }
        waitFor("Days, 28 in Oct 3 – Oct 30, 28 in Oct 31 – Nov 27")
        assertTrue(spoken("Distance, 0.0 km in Oct 3 – Oct 30, 0.0 km in Oct 31 – Nov 27"))
        for (t in listOf("Bodyweight", "Sets per muscle · per week", "Best set", "Cardio")) assertFalse(t, shows(t))
        assertTrue(shows("Oct 3 – Oct 30"))
    }

    @Test fun `the choices come back after a saved state restore`() {
        io { seed() }
        val tester = StateRestorationTester(compose)
        tester.setContent { KhonTheme { CompareScreen(vm, rememberNavController(), clock) } }
        waitFor("Oct 31 – Nov 27")
        tap("Oct 31 – Nov 27")
        waitFor("Right column")
        tap("Last 12 weeks")
        compose.waitUntil(5_000) { shows("Sep 5 – Nov 27") && !shows("Right column") }
        tester.emulateSavedInstanceStateRestore()
        waitFor("Sep 5 – Nov 27")
        assertTrue(shows("Last 12 weeks"))
        assertTrue(shows("The 4 weeks before"))
    }
}
