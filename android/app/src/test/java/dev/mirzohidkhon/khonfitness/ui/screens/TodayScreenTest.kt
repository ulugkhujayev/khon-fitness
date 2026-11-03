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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
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
    @get:Rule val compose = createComposeRule()
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

    @After fun close() { app.scope.cancel(); app.db.close() }

    private fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }
    private fun shows(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    private fun named(label: String) = compose.onAllNodesWithContentDescription(label).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(shown: () -> Boolean) = compose.waitUntil(5_000) { shown() }

    /** Push day (2 exercises, 5 sets) on today's weekday, or rest today and Push day tomorrow; a Zone 2 run to change to. */
    private fun seed(restToday: Boolean = false) = io {
        dao.upsert(Modality("run", "Run", 0xFF30D158))
        dao.upsert(Exercise("bench", "Bench press", Kind.STRENGTH))
        dao.upsert(Exercise("fly", "Cable fly", Kind.STRENGTH))
        dao.upsert(Exercise("z2", "Zone 2 run", Kind.CARDIO, modalityId = "run"))
        dao.upsert(Program("push", "Push day", active = true))
        dao.upsert(Block("b1", "push", 0))
        dao.upsert(BlockExercise("be1", "b1", 0, "bench", sets = 3))
        dao.upsert(BlockExercise("be2", "b1", 1, "fly", sets = 2))
        val day = if (restToday) today.plusDays(1) else today
        dao.upsert(WeekPlan(day.dayOfWeek.value, ItemType.PROGRAM, "push"))
    }

    private fun openToday() {
        compose.setContent {
            KhonTheme {
                val nav = rememberNavController()
                NavHost(nav, Routes.TODAY) {
                    composable(Routes.TODAY) { TodayScreen(vm, nav) }
                    composable("review/{id}") { e -> ReviewScreen(vm, nav, e.arguments?.getString("id") ?: "") }
                    composable("session/{id}") { Text("Session screen") }
                    composable(Routes.HISTORY) { Text("History screen") }
                    composable(Routes.PROGRAMS) { Text("Plan screen") }
                    composable(Routes.SETTINGS) { Text("Settings screen") }
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
        assertFalse("no Edit from Today", shows("Edit"))
        assertFalse("no Delete from Today", shows("Delete"))
    }

    @Test fun `two saved sessions today show the last one and a line to History`() {
        seed()
        session(done = 5, finish = true)
        session(done = 3, finish = true)
        openToday()
        waitFor { shows("Done · 3 of 5 sets") }
        compose.onNodeWithContentDescription("+1 more today, open History").assertHeightIsAtLeast(48.dp).performClick()
        waitFor { shows("History screen") }
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
        for (bad in listOf("", "0", "19", "251", "abc")) {
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
