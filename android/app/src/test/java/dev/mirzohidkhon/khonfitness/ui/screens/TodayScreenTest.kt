package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.timer.Phase
import dev.mirzohidkhon.khonfitness.timer.PhaseKind
import dev.mirzohidkhon.khonfitness.timer.TimerService
import dev.mirzohidkhon.khonfitness.timer.TimerState
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.Block
import dev.mirzohidkhon.khonfitness.data.BlockExercise
import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.Modality
import dev.mirzohidkhon.khonfitness.data.Program
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.WeekPlan
import dev.mirzohidkhon.khonfitness.data.iso
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * UI06 P2, the Today tab on the S25 screen: the card's states with Change, Start, Resume and Review, the one-day
 * Change sheet, first use, and the bodyweight card's stepper, typing and save states.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TodayScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val dao get() = app.db.dao()
    private val today = LocalDate.now()

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    /** Off at the end: the screens leave composition and stop their queries before the database closes. */
    private var shown by androidx.compose.runtime.mutableStateOf(true)

    @After fun close() {
        TimerService.state.value = null
        shown = false
        compose.waitForIdle()
        vm.viewModelScope.cancel()
        app.scope.cancel()
        compose.waitForIdle()
        app.db.close()
    }

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }
    private fun shows(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    private fun named(label: String) = compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(shown: () -> Boolean) = compose.waitUntil(5_000) { shown() }

    /** Push day (2 exercises, 5 sets) on [day]'s weekday, or rest today and Push day tomorrow; a Zone 2 run to change to. */
    private fun seed(restToday: Boolean = false, day: LocalDate = if (restToday) today.plusDays(1) else today, weekPlan: Boolean = true) = io {
        dao.upsert(Modality("run", "Run", 0xFF30D158))
        dao.upsert(Exercise("bench", "Bench press", Kind.STRENGTH))
        dao.upsert(Exercise("fly", "Cable fly", Kind.STRENGTH))
        dao.upsert(Exercise("z2", "Zone 2 run", Kind.CARDIO, modalityId = "run"))
        dao.upsert(Program("push", "Push day", active = true))
        dao.upsert(Block("b1", "push", 0))
        dao.upsert(BlockExercise("be1", "b1", 0, "bench", sets = 3))
        dao.upsert(BlockExercise("be2", "b1", 1, "fly", sets = 2))
        if (weekPlan) dao.upsert(WeekPlan(day.dayOfWeek.value, ItemType.PROGRAM, "push"))
    }

    /** The wall clock Today reads: now, unless a test moves it. */
    private var wall = System.currentTimeMillis()
    private lateinit var nav: NavHostController

    /** Today in the app's graph: the real History and Review, stand-ins for the rest. [fontScale] as in Settings. */
    private fun openToday(fontScale: Float = 1f) {
        compose.setContent {
            val d = LocalDensity.current
            val config = android.content.res.Configuration(LocalConfiguration.current).apply { this.fontScale = fontScale }
            if (shown) CompositionLocalProvider(LocalWallClock provides { wall }, LocalDensity provides Density(d.density, fontScale), LocalConfiguration provides config) {
                KhonTheme {
                    nav = rememberNavController()
                    NavHost(nav, Routes.TODAY) {
                        composable(Routes.TODAY) { TodayScreen(vm, nav) }
                        composable("review/{id}") { e -> ReviewScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                        composable("session/{id}") { Text("Session screen") }
                        composable("cardio/{id}") { Text("Cardio screen") }
                        composable("timer/{id}") { Text("Timer screen") }
                        composable(Routes.HISTORY) { HistoryScreen(vm, nav) }
                        composable(Routes.PROGRAMS) { Text("Plan screen") }
                        composable(Routes.SETTINGS) { Text("Settings screen") }
                    }
                }
            }
        }
        waitFor { named("Bodyweight in kilograms") }
        compose.waitForIdle()
    }

    /** A Push day session today: started, then [done] sets checked and the rest skipped when [finish]. */
    private fun session(done: Int, finish: Boolean): String = io {
        val s = vm.repo.startProgramSession("push", today)!!
        dao.setLogsOnce(s.id).sortedBy { it.order }.take(done).forEach { dao.upsert(it.copy(weightKg = 80.0, reps = 10, status = SetStatus.DONE)) }
        if (finish) vm.repo.finishSession(s.id)
        s.id
    }

    /** A Zone 2 run today, started and saved at [finishedAt] when given. */
    private fun cardio(finishedAt: Long? = null): String = io {
        val s = vm.repo.startCardioSession(dao.exercisesOnce().first { it.id == "z2" }, today)
        if (finishedAt != null) vm.repo.finishSession(s.id, finishedAt)
        s.id
    }

    /** The bottom bar's tab switch: the tab left keeps its state, the one opened gets its own back. */
    private fun tab(route: String) = compose.runOnUiThread { nav.navigate(route) { popUpTo(Routes.TODAY) { saveState = true }; launchSingleTop = true; restoreState = true } }

    /** The app goes to the background and comes back: Today reads the clock again. */
    private fun pauseAndResume() {
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
    }

    private fun top(text: String) = compose.onNodeWithText(text, substring = true).getBoundsInRoot().top

    @Test fun `today has no week strip, and the card, the rows and the bodyweight card come in that order`() {
        seed()
        openToday()
        waitFor { shows("Push day") }
        // The week strip spoke each day as "<date>, <item>, today"; nothing of it is left.
        assertFalse(compose.onAllNodesWithContentDescription(", today", substring = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(shows("2 exercises · 5 sets"))
        assertTrue(top("Push day") < top("Eating window"))
        assertTrue(top("Eating window") < top("Bodyweight"))
        assertFalse("no chart on Today", named("Bodyweight chart"))
        compose.onNodeWithText("Start").assertIsEnabled()
    }

    @Test fun `change link is a 48 dp target and opens the sheet for today only`() {
        seed()
        openToday()
        waitFor { named("Change today's plan") }
        compose.onNodeWithContentDescription("Change today's plan").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
        waitFor { shows(changeTitle(today)) }
        assertTrue(shows("Only today changes. The week plan stays."))
        assertTrue(shows("Gym")); assertTrue(shows("Cardio")); assertTrue(shows("Other"))
        assertFalse("no Back to plan without a change", shows("Back to plan"))
        compose.onNodeWithText("Zone 2 run").performSemanticsAction(SemanticsActions.OnClick) // the sheet may still be sliding in
        waitFor { io { dao.overridesOnce() }.isNotEmpty() }
        val o = io { dao.overridesOnce() }.single()
        assertEquals(listOf(today.iso(), ItemType.CARDIO, "z2"), listOf(o.date, o.itemType, o.itemId))
        assertTrue("the week plan stays", io { dao.weekPlanOnce() }.any { it.weekday == today.dayOfWeek.value && it.itemId == "push" })
        waitFor { shows("changed for today") }
        compose.onNodeWithContentDescription("Change today's plan").performClick()
        waitFor { shows("Back to plan: Push day") }
        compose.onNodeWithText("Back to plan: Push day").performSemanticsAction(SemanticsActions.OnClick)
        waitFor { io { dao.overridesOnce() }.isEmpty() }
        waitFor { !shows("changed for today") }
    }

    @Test fun `rest day has Change and no Start`() {
        seed(restToday = true)
        openToday()
        waitFor { shows("Rest day") }
        assertTrue(named("Change today's plan"))
        assertFalse(shows("Start"))
    }

    @Test fun `a running session shows its sets with Resume and no Change`() {
        seed()
        session(done = 2, finish = false)
        openToday()
        waitFor { shows("In progress · 2 of 5 sets") }
        assertTrue(shows("Resume"))
        assertFalse(named("Change today's plan"))
        compose.onNodeWithText("Resume").performClick()
        waitFor { shows("Session screen") }
    }

    @Test fun `a saved session shows its done line with Review only, and Review is read-only with skipped sets`() {
        seed()
        session(done = 1, finish = true)
        openToday()
        waitFor { shows("Done · 1 of 5 sets · 0.8 t") }
        assertFalse(shows("Start again"))
        assertFalse(shows("Start"))
        assertFalse(named("Change today's plan"))
        compose.onNodeWithContentDescription("Review today's session").assertHeightIsAtLeast(48.dp).performClick()
        waitFor { shows("Bench press") }
        assertEquals(4, compose.onAllNodesWithText("skipped").fetchSemanticsNodes().size)
        assertTrue(shows("80 kg × 10"))
        compose.onNodeWithContentDescription("Today").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        assertFalse("no Edit from Today", shows("Edit"))
        assertFalse("no Delete from Today", shows("Delete"))
    }

    @Test fun `two saved sessions today show the last one, and the line opens History fresh on this week`() {
        seed()
        session(done = 5, finish = true)
        session(done = 3, finish = true)
        openToday()
        waitFor { shows("Done · 3 of 5 sets") }
        // History was left a month back; the line opens it fresh, not where it was left.
        val month = DateTimeFormatter.ofPattern("MMMM yyyy")
        tab(Routes.HISTORY)
        waitFor { named("Previous month") }
        compose.onNodeWithContentDescription("Previous month").performClick()
        waitFor { shows(YearMonth.from(today).minusMonths(1).format(month)) }
        tab(Routes.TODAY)
        waitFor { named("+1 more today, open History") }
        compose.onNodeWithContentDescription("+1 more today, open History").assertHeightIsAtLeast(48.dp).performClick()
        waitFor { named("Previous month") }
        assertTrue(shows(YearMonth.from(today).format(month)))
        assertFalse(shows(YearMonth.from(today).minusMonths(1).format(month)))
    }

    @Test fun `a run saved on a gym day shows on the card with its own name, and Change is gone`() {
        seed()
        cardio(finishedAt = System.currentTimeMillis())
        openToday()
        waitFor { shows("Zone 2 run") }
        assertTrue(shows("Done"))
        assertFalse(shows("Push day"))
        assertFalse(shows("Start"))
        assertFalse(named("Change today's plan"))
        // A gym session saved before it counts as one more.
        session(done = 5, finish = false).let { io { vm.repo.finishSession(it, System.currentTimeMillis() - 60_000) } }
        waitFor { named("+1 more today, open History") }
        assertTrue(shows("Zone 2 run"))
    }

    @Test fun `a week with only rest days is a rest day with Change, not first use`() {
        seed(weekPlan = false)
        openToday()
        waitFor { shows("Rest day") }
        assertFalse(shows("No plan for today"))
        assertTrue(named("Change today's plan"))
    }

    @Test fun `a running cardio session resumes on its screen, or on the timer while the timer runs it`() {
        seed()
        val id = cardio()
        openToday()
        waitFor { shows("Resume") }
        assertFalse(named("Change today's plan"))
        compose.onNodeWithText("Resume").performClick()
        waitFor { shows("Cardio screen") }
        compose.runOnUiThread { nav.popBackStack() }
        TimerService.state.value = TimerState(id, "Zone 2 run", listOf(Phase(PhaseKind.WORK, 60, 1)))
        waitFor { shows("Round 1 of 1 · Work") }
        compose.onNodeWithText("Resume").performClick()
        waitFor { shows("Timer screen") }
    }

    @Test fun `a session that starts while Change is open closes it, and the sheet stays closed after`() {
        seed()
        openToday()
        compose.onNodeWithContentDescription("Change today's plan").performClick()
        waitFor { shows(changeTitle(today)) }
        val id = io { vm.repo.startProgramSession("push", today)!!.id }
        waitFor { shows("Resume") }
        waitFor { !shows(changeTitle(today)) }
        // The session is thrown away: Change is back, but the sheet does not open on its own.
        io { vm.repo.deleteSession(id) }
        waitFor { named("Change today's plan") }
        compose.waitForIdle()
        assertFalse(shows(changeTitle(today)))
    }

    @Test fun `midnight with Today open turns it to the new day on resume`() {
        seed(day = today.minusDays(1))
        wall = today.minusDays(1).atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        openToday()
        waitFor { shows("Push day") }
        wall = System.currentTimeMillis()
        pauseAndResume()
        waitFor { shows("Rest day") }
        assertFalse(shows("Push day"))
    }

    @Test fun `the Change sheet closes when the date turns`() {
        seed(day = today.minusDays(1))
        wall = today.minusDays(1).atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        openToday()
        compose.onNodeWithContentDescription("Change today's plan").performClick()
        waitFor { shows(changeTitle(today.minusDays(1))) }
        wall = System.currentTimeMillis()
        pauseAndResume()
        waitFor { shows("Rest day") }
        assertFalse(shows(changeTitle(today.minusDays(1))))
        assertFalse(shows(changeTitle(today)))
    }

    @Test fun `the settings gear is a 48 dp target`() {
        openToday()
        // A touch 1 dp inside its top edge, past the large title's 44 dp row, still opens Settings.
        compose.onNodeWithContentDescription("Settings").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            .performTouchInput { click(androidx.compose.ui.geometry.Offset(centerX, 1.dp.toPx())) }
        waitFor { shows("Settings screen") }
    }

    @Test fun `at font scale 1_6 the card and the stepper fit the screen without overlap`() {
        seed()
        openToday(fontScale = 1.6f)
        waitFor { named("Change today's plan") }
        val screen = 360.dp
        val change = compose.onNodeWithContentDescription("Change today's plan").assertHeightIsAtLeast(48.dp).getBoundsInRoot()
        val title = compose.onNodeWithText("Push day").getBoundsInRoot()
        assertTrue("Change is on the screen: ${change.right}", change.right <= screen)
        assertTrue("the name ends before Change", title.right <= change.left)
        compose.onNodeWithText("Start").assertHeightIsAtLeast(48.dp)
        val minus = compose.onNodeWithContentDescription("Minus 0.1 kg").getBoundsInRoot()
        val plus = compose.onNodeWithContentDescription("Plus 0.1 kg").getBoundsInRoot()
        field.performTextReplacement("250.0")
        compose.waitForIdle()
        val box = field.getBoundsInRoot()
        assertTrue("− | value | + in order", minus.right <= box.left && box.right <= plus.left && plus.right <= screen)
        val layout = mutableListOf<TextLayoutResult>()
        field.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
        val textWidth = with(compose.density) { layout.single().size.width.toDp() }
        assertTrue("\"250.0\" fits: $textWidth in ${box.right - box.left}", textWidth <= box.right - box.left)
        compose.onNodeWithContentDescription("Save bodyweight").assertHeightIsAtLeast(48.dp)
    }

    @Test fun `first use offers to set up the week and starts the entry at 75 kg`() {
        openToday()
        waitFor { shows("No plan for today") }
        assertTrue(shows("No entry yet"))
        assertTrue(shows("75.0"))
        compose.onNodeWithText("Set up your week").performClick()
        waitFor { shows("Plan screen") }
    }

    private val field get() = compose.onNodeWithContentDescription("Bodyweight in kilograms")
    private fun saved() = io { dao.bodyweightsOnce() }.find { it.date == today.iso() }?.kg

    @Test fun `a typed weight with a comma saves with one tap`() {
        io { dao.upsert(Bodyweight(today.minusDays(4).iso(), 83.4)) }
        openToday()
        assertTrue(shows("Last: 83.4 kg"))
        field.performClick()
        field.performTextReplacement("83,5")
        compose.onNodeWithContentDescription("Save bodyweight").assertIsEnabled().performClick()
        waitFor { saved() == 83.5 }
        waitFor { named("Today's bodyweight is saved") }
        compose.onNodeWithContentDescription("Today's bodyweight is saved").assertIsNotEnabled()
        assertTrue(shows("Saved for today · +0.1 kg since"))
    }

    @Test fun `keyboard Done saves a typed weight`() {
        openToday()
        field.performClick()
        field.performTextReplacement("83.5")
        field.performImeAction()
        waitFor { saved() == 83.5 }
    }

    @Test fun `text that is not a weight stays, says the range, and turns Save off`() {
        openToday()
        for (bad in listOf("", "0", "19", "251", "abc", "83.55555", "1234567890")) {
            field.performTextReplacement(bad)
            compose.waitForIdle()
            assertTrue(bad, compose.onAllNodesWithText(bad).fetchSemanticsNodes().isNotEmpty() || bad.isEmpty())
            assertTrue(bad, shows(KG_ERROR))
            compose.onNodeWithContentDescription("Save bodyweight").assertIsNotEnabled().performClick()
            field.performImeAction()
        }
        compose.waitForIdle()
        assertNull(saved())
        field.performTextReplacement("80")
        compose.waitForIdle()
        assertFalse(shows(KG_ERROR))
    }

    @Test fun `save states are Save, Update for another value, and Saved when equal`() {
        io { dao.upsert(Bodyweight(today.iso(), 80.0)) }
        openToday()
        waitFor { named("Today's bodyweight is saved") }
        compose.onNodeWithContentDescription("Plus 0.1 kg").performClick()
        waitFor { named("Update today's bodyweight") }
        compose.onNodeWithContentDescription("Update today's bodyweight").performClick()
        waitFor { saved() == 80.1 }
    }

    @Test fun `minus and plus are 52 dp squares that step 0_1 kg, and a hold repeats`() {
        openToday()
        compose.onNodeWithContentDescription("Plus 0.1 kg").assertWidthIsEqualTo(52.dp).assertHeightIsEqualTo(52.dp).performClick()
        waitFor { shows("75.1") }
        compose.onNodeWithContentDescription("Minus 0.1 kg").assertWidthIsEqualTo(52.dp).assertHeightIsEqualTo(52.dp).performClick()
        compose.onNodeWithContentDescription("Minus 0.1 kg").performClick()
        waitFor { shows("74.9") }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Plus 0.1 kg").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithContentDescription("Plus 0.1 kg").performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        val shown = field.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text.toDouble()
        assertTrue("a hold steps more than once: $shown", shown > 75.1)
    }
}
