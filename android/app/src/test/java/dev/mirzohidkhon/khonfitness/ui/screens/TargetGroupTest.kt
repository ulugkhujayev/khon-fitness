package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.BodyTarget
import dev.mirzohidkhon.khonfitness.data.Bodyweight
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.ItemType
import dev.mirzohidkhon.khonfitness.data.Kind
import dev.mirzohidkhon.khonfitness.data.LiftTarget
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.SetStatus
import dev.mirzohidkhon.khonfitness.data.iso
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h2400dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TargetGroupTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val dao get() = app.db.dao()
    private val today = LocalDate.now()
    private var k = 0

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() { vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }

    /** A bench session [back] days ago whose est. 1RM is [value]: one set of 30 reps at half of it. */
    private fun lift(back: Long, value: Double) = io {
        val date = today.minusDays(back)
        val at = date.toEpochDay() * 86_400_000 + 7 * 3_600_000
        dao.upsertSessions(listOf(Session("b$back", date.iso(), at, at + 3_000_000, ItemType.PROGRAM, programId = "p", programName = "P")))
        dao.upsertSetLogs(listOf(SetLog("l${k++}", "b$back", 0, false, 0, 0, 0, "bench", "Bench", 5, 8, 2.5, weightKg = value / 2, reps = 30, status = SetStatus.DONE)))
    }

    private fun exercises() = io {
        dao.upsertExercises(listOf(
            Exercise("bench", "Bench", Kind.STRENGTH, primaryMuscle = "chest"),
            Exercise("fresh", "Fresh lift", Kind.STRENGTH, primaryMuscle = "chest"),
            Exercise("run", "Run", Kind.CARDIO),
        ))
    }

    private fun detail(id: String) = compose.setContent { KhonTheme { ExerciseDetailScreen(vm, rememberNavController(), id) } }
    private fun said(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription(text).fetchSemanticsNodes().isNotEmpty() }
    private fun gone(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription(text, substring = true).fetchSemanticsNodes().isEmpty() }
    private fun field(spoken: String) = hasContentDescription(spoken).and(hasSetTextAction())

    @Test fun `G12 the rows follow a new target and a new session while the screen is open`() {
        exercises()
        listOf(42L to 90.0, 28L to 92.0, 14L to 94.0).forEach { (b, v) -> lift(b, v) }
        detail("bench")
        said("Target, not set")
        io { vm.targets.setLift("bench", BigDecimal(100), reps = false, date = today.minusDays(50)) }
        said("Target, 100 kilograms")
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("At plus 1.0 kilograms per week, ", substring = true).fetchSemanticsNodes().size == 1 }
        assertEquals(1, compose.onAllNodesWithText("At +1.0 kg\u2060/\u2060week", useUnmergedTree = true).fetchSemanticsNodes().size)
        lift(0, 100.0)
        said("Reached, ${spokenTargetDate(today, today)}")
        io { vm.targets.setLift("bench", BigDecimal(110), reps = false, date = today.minusDays(50)) }
        said("Target, 110 kilograms")
        gone("Reached")
    }

    @Test fun `G12 the result follows a new today on the next composition`() {
        val h = listOf(42L to 90.0, 28L to 92.0, 14L to 94.0, 0L to 96.0).map { (b, v) ->
            val d = today.minusDays(b)
            Session("s$b", d.iso(), b, b + 1, ItemType.PROGRAM, programId = "p", programName = "P") to SetLog("x$b", "s$b", 0, false, 0, 0, 0, "bench", "Bench", 5, 8, 2.5, weightKg = v / 2, reps = 30, status = SetStatus.DONE)
        }
        val history = liftHistory("bench", h.map { it.second }, h.map { it.first })
        io { vm.targets.setLift("bench", BigDecimal(100), reps = false, date = today.minusDays(50)) }
        var shown by mutableStateOf(today)
        compose.setContent { KhonTheme { Column { LiftTargetGroup(vm, "bench", history, shown) } } }
        said("At plus 1.0 kilograms per week, ${spokenTargetDate(today.plusDays(28), today)}")
        compose.runOnIdle { shown = today.plusDays(60) }
        said("Target, 100 kilograms")
        gone("At plus")
    }

    @Test fun `G11 G13 an exercise with no session has the target row only and the sheet refuses bad values`() {
        exercises()
        detail("fresh")
        said("Target, not set")
        val row = compose.onNodeWithContentDescription("Target, not set").fetchSemanticsNode()
        assertEquals(Role.Button, row.config[SemanticsProperties.Role])
        compose.onNodeWithContentDescription("Target, not set").performScrollTo().performClick()
        val input = field("Target repetitions, repetitions")
        compose.waitUntil(5_000) { compose.onAllNodes(input).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(compose.onAllNodesWithText("Target reps").fetchSemanticsNodes().isNotEmpty())
        assertEquals("", compose.onNode(input).fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        for (bad in listOf("0", "501", "7.5")) {
            compose.onNode(input).performTextReplacement(bad)
            compose.onNodeWithText("Save").assertIsNotEnabled().performClick()
            compose.waitForIdle()
            assertNull(bad, io2 { vm.targets.lift("fresh").first() })
        }
        compose.onNode(input).performTextReplacement("12")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        said("Target, 12 repetitions")
        assertEquals(LiftTarget(BigDecimal("12"), true, today), io2 { vm.targets.lift("fresh").first() })
        assertTrue(compose.onAllNodesWithContentDescription("At ", substring = true).fetchSemanticsNodes().isEmpty())
        compose.onNodeWithContentDescription("Target, 12 repetitions").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(input).fetchSemanticsNodes().isNotEmpty() }
        assertEquals("12", compose.onNode(input).fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        compose.onNode(input).performTextReplacement("")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        said("Target, not set")
        assertNull(io2 { vm.targets.lift("fresh").first() })
    }

    @Test fun `G11 the est 1RM sheet refuses values off its limits and steps`() {
        exercises()
        listOf(42L to 90.0, 28L to 92.0).forEach { (b, v) -> lift(b, v) }
        detail("bench")
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Sessions · newest first").fetchSemanticsNodes().isNotEmpty() }
        said("Target, not set")
        compose.onNodeWithContentDescription("Target, not set").performScrollTo().performClick()
        val input = field("Target estimated one rep max, kilograms")
        compose.waitUntil(5_000) { compose.onAllNodes(input).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(compose.onAllNodesWithText("Target est. 1RM").fetchSemanticsNodes().isNotEmpty())
        for (bad in listOf("0.5", "500.5", "100.3")) {
            compose.onNode(input).performTextReplacement(bad)
            compose.onNodeWithText("Save").assertIsNotEnabled().performClick()
            compose.waitForIdle()
            assertNull(bad, io2 { vm.targets.lift("bench").first() })
        }
        compose.onNode(input).performTextReplacement("abc")
        compose.waitForIdle()
        assertEquals("abc", compose.onNode(input).fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNode(input).performTextReplacement("100.5")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        said("Target, 100.5 kilograms")
    }

    @Test fun `G13 a cardio exercise has no target group`() {
        exercises()
        detail("run")
        compose.waitUntil(5_000) { compose.onAllNodesWithText("No sets yet").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithText("Target").fetchSemanticsNodes().isEmpty())
    }

    @Test fun `the bodyweight section has the group with and without entries and refuses bad values`() {
        compose.setContent { KhonTheme { Column(Modifier.verticalScroll(rememberScrollState())) { HistoryProgress(vm, rememberNavController(), today) } } }
        said("Target, not set")
        compose.onNodeWithContentDescription("Target, not set").performScrollTo().performClick()
        val input = field("Target bodyweight, kilograms")
        compose.waitUntil(5_000) { compose.onAllNodes(input).fetchSemanticsNodes().isNotEmpty() }
        for (bad in listOf("19.9", "300.1", "80.25")) {
            compose.onNode(input).performTextReplacement(bad)
            compose.onNodeWithText("Save").assertIsNotEnabled().performClick()
            compose.waitForIdle()
            assertNull(bad, io2 { vm.targets.body.first() })
        }
        compose.onNode(input).performTextReplacement("80")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        said("Target, 80.0 kilograms")
        assertEquals(BodyTarget(BigDecimal("80.0"), null, today), io2 { vm.targets.body.first() })

        io { dao.upsertBodyweights((0..2).map { Bodyweight(today.minusDays(it.toLong()).iso(), 82.0) } + (7..9).map { Bodyweight(today.minusDays(it.toLong()).iso(), 82.3) }) }
        said("At minus 0.3 kilograms per week, ${spokenTargetDate(today.plusDays(47), today)}")
    }

    private fun <T> io2(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }

    private fun weighted(): List<LiftSession> {
        val session = Session("w", today.iso(), 1, 2, ItemType.PROGRAM, programId = "p")
        val set = SetLog("wl", "w", 0, false, 0, 0, 0, "bench", "Bench", 5, 8, 2.5, weightKg = 50.0, reps = 5, status = SetStatus.DONE)
        return liftHistory("bench", listOf(set), listOf(session))
    }

    private fun openSheet(row: String, input: String): androidx.compose.ui.test.SemanticsMatcher {
        said(row)
        compose.onNodeWithContentDescription(row).performClick()
        val match = field(input)
        compose.waitUntil(5_000) { compose.onAllNodes(match).fetchSemanticsNodes().isNotEmpty() }
        return match
    }

    private fun text(match: androidx.compose.ui.test.SemanticsMatcher) = compose.onNode(match).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    private fun refusedKeeps(match: androidx.compose.ui.test.SemanticsMatcher, typed: String, stored: () -> Any?, expected: Any?) {
        compose.onNode(match).performTextReplacement(typed)
        compose.waitForIdle()
        assertEquals(typed, typed, text(match))
        compose.onNodeWithText("Save").assertIsNotEnabled().performClick()
        compose.waitForIdle()
        assertEquals(typed, expected, stored())
    }

    @Test fun `X1 text that is not a plain decimal on the step keeps the stored target and stays in the field`() {
        io { vm.targets.setLift("bench", BigDecimal(100), reps = false, date = today.minusDays(3)) }
        val history = weighted()
        compose.setContent { KhonTheme { Column { LiftTargetGroup(vm, "bench", history, today) } } }
        val input = openSheet("Target, 100 kilograms", "Target estimated one rep max, kilograms")
        assertEquals("100", text(input))
        val kept = LiftTarget(BigDecimal(100), false, today.minusDays(3))
        for (bad in listOf("abc", "8x0", "-80", "+80", " 80", "80.001", "80.25", "100.501", "100.3", "7.05", "1,000.5", "\u0668\u0660")) {
            refusedKeeps(input, bad, { io2 { vm.targets.lift("bench").first() } }, kept)
        }
    }

    @Test fun `X1 the reps and bodyweight sheets refuse text off their step`() {
        io { vm.targets.setLift("bench", BigDecimal(12), reps = true, date = today.minusDays(3)) }
        compose.setContent { KhonTheme { Column { LiftTargetGroup(vm, "bench", emptyList(), today); BodyTargetGroup(vm, emptyList(), today) } } }
        val reps = openSheet("Target, 12 repetitions", "Target repetitions, repetitions")
        val keptReps = LiftTarget(BigDecimal(12), true, today.minusDays(3))
        for (bad in listOf("12.0", "7.05", "12,", "-12")) refusedKeeps(reps, bad, { io2 { vm.targets.lift("bench").first() } }, keptReps)
        compose.onNode(reps).performTextReplacement("0000015")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        said("Target, 15 repetitions")
        assertEquals(LiftTarget(BigDecimal(15), true, today), io2 { vm.targets.lift("bench").first() })

        io { vm.targets.setBody(BigDecimal("80.0"), null, today.minusDays(3)) }
        val body = openSheet("Target, 80.0 kilograms", "Target bodyweight, kilograms")
        assertEquals("80", text(body))
        val keptBody = BodyTarget(BigDecimal("80.0"), null, today.minusDays(3))
        for (bad in listOf("80.25", "80.001", "8x0", "-80", "80..0", "\u0668\u0660")) refusedKeeps(body, bad, { io2 { vm.targets.body.first() } }, keptBody)
    }

    @Test fun `X2 leading zeros keep the value, a comma is the decimal sign, and an empty field removes the target`() {
        val history = weighted()
        var shown by mutableStateOf("lift")
        compose.setContent { KhonTheme { Column { if (shown == "lift") LiftTargetGroup(vm, "bench", history, today) else BodyTargetGroup(vm, emptyList(), today) } } }
        fun lift(row: String, typed: String, saved: String) {
            val input = openSheet(row, "Target estimated one rep max, kilograms")
            compose.onNode(input).performTextReplacement(typed)
            compose.onNodeWithText("Save").assertIsEnabled().performClick()
            said("Target, $saved kilograms")
            assertEquals(typed, 0, io2 { vm.targets.lift("bench").first() }!!.value.compareTo(BigDecimal(saved)))
        }
        lift("Target, not set", "100,0", "100")
        lift("Target, 100 kilograms", "100.50", "100.5")
        lift("Target, 100.5 kilograms", "0000015", "15")
        val input = openSheet("Target, 15 kilograms", "Target estimated one rep max, kilograms")
        compose.onNode(input).performTextReplacement("")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        said("Target, not set")
        assertNull(io2 { vm.targets.lift("bench").first() })

        compose.runOnIdle { shown = "body" }
        fun body(typed: String, saved: String) {
            compose.onNodeWithContentDescription("Target, ", substring = true).performClick()
            val b = field("Target bodyweight, kilograms")
            compose.waitUntil(5_000) { compose.onAllNodes(b).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(b).performTextReplacement(typed)
            compose.onNodeWithText("Save").assertIsEnabled().performClick()
            said("Target, $saved kilograms")
            assertEquals(BigDecimal(saved), io2 { vm.targets.body.first() }!!.kg)
        }
        said("Target, not set")
        body("0080", "80.0")
        body("80,5", "80.5")
        compose.onNodeWithContentDescription("Target, 80.5 kilograms").performClick()
        val b = field("Target bodyweight, kilograms")
        compose.waitUntil(5_000) { compose.onAllNodes(b).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(b).performTextReplacement("")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        said("Target, not set")
        assertNull(io2 { vm.targets.body.first() })
    }

    @Test fun `the step buttons give a valid text inside the limits`() {
        val history = weighted()
        compose.setContent { KhonTheme { Column { LiftTargetGroup(vm, "bench", history, today) } } }
        val input = openSheet("Target, not set", "Target estimated one rep max, kilograms")
        compose.onNodeWithContentDescription("Increase by 0.5 kg").performClick()
        compose.waitForIdle()
        assertEquals("1", text(input))
        compose.onNodeWithContentDescription("Increase by 0.5 kg").performClick()
        compose.waitForIdle()
        assertEquals("1.5", text(input))
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        said("Target, 1.5 kilograms")
        openSheet("Target, 1.5 kilograms", "Target estimated one rep max, kilograms")
        repeat(3) { compose.onNodeWithContentDescription("Decrease by 0.5 kg").performClick() }
        compose.waitForIdle()
        assertEquals("1", text(input))
        compose.onNode(input).performTextReplacement("500")
        compose.onNodeWithContentDescription("Increase by 0.5 kg").performClick()
        compose.waitForIdle()
        assertEquals("500", text(input))
        compose.onNodeWithText("Save").assertIsEnabled()
    }

    @Test fun `the bodyweight step buttons give a valid text`() {
        compose.setContent { KhonTheme { Column { BodyTargetGroup(vm, emptyList(), today) } } }
        val input = openSheet("Target, not set", "Target bodyweight, kilograms")
        compose.onNodeWithContentDescription("Increase by 0.1 kg").performClick()
        compose.waitForIdle()
        assertEquals("20", text(input))
        compose.onNode(input).performTextReplacement("80")
        repeat(3) { compose.onNodeWithContentDescription("Increase by 0.1 kg").performClick() }
        compose.waitForIdle()
        assertEquals("80.3", text(input))
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        said("Target, 80.3 kilograms")
    }

    @Test fun `X6 a new unit closes an open sheet and drops its draft`() {
        io { vm.targets.setLift("bench", BigDecimal(15), reps = true, date = today.minusDays(3)) }
        var history by mutableStateOf(emptyList<LiftSession>())
        compose.setContent { KhonTheme { Column { LiftTargetGroup(vm, "bench", history, today) } } }
        val reps = openSheet("Target, 15 repetitions", "Target repetitions, repetitions")
        compose.onNode(reps).performTextReplacement("16")
        compose.runOnIdle { history = weighted() }
        compose.waitForIdle()
        assertTrue(compose.onAllNodes(reps).fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodes(field("Target estimated one rep max, kilograms")).fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodesWithText("Save").fetchSemanticsNodes().isEmpty())
        said("Target, not set")
        assertEquals(LiftTarget(BigDecimal(15), true, today.minusDays(3)), io2 { vm.targets.lift("bench").first() })
        val kg = openSheet("Target, not set", "Target estimated one rep max, kilograms")
        assertEquals("", text(kg))
    }

    @Test fun `X6 a save on a stored target of the other unit replaces or removes it`() {
        io { vm.targets.setLift("bench", BigDecimal(15), reps = true, date = today.minusDays(3)) }
        val history = weighted()
        compose.setContent { KhonTheme { Column { LiftTargetGroup(vm, "bench", history, today) } } }
        openSheet("Target, not set", "Target estimated one rep max, kilograms")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        compose.waitForIdle()
        assertNull(io2 { vm.targets.lift("bench").first() })
        assertNull(io2 { app.settings.data.first()[dev.mirzohidkhon.khonfitness.data.Targets.liftKey("bench")] })

        io { vm.targets.setLift("bench", BigDecimal(15), reps = true, date = today.minusDays(3)) }
        val input = openSheet("Target, not set", "Target estimated one rep max, kilograms")
        compose.onNode(input).performTextReplacement("100")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        said("Target, 100 kilograms")
        assertEquals(LiftTarget(BigDecimal(100), false, today), io2 { vm.targets.lift("bench").first() })
    }

    @Test fun `G13 the stretch routes have no target group`() {
        io {
            dao.upsertStretches(listOf(dev.mirzohidkhon.khonfitness.data.Stretch("hip", "Hip flexor", sided = true, figure = "hipflexor")))
            dao.upsert(dev.mirzohidkhon.khonfitness.data.StretchRoutine("r", "Daily", active = true))
            dao.upsertRoutineStretches(listOf(dev.mirzohidkhon.khonfitness.data.RoutineStretch("r-hip", "r", "hip", 0)))
        }
        var route by mutableStateOf("stretch")
        compose.mainClock.autoAdvance = false
        compose.setContent {
            KhonTheme {
                when (route) {
                    "stretch" -> StretchScreen(vm, rememberNavController(), "r")
                    "editor" -> StretchEditorScreen(vm, rememberNavController(), "hip")
                    else -> ExerciseDetailScreen(vm, rememberNavController(), "hip")
                }
            }
        }
        for ((r, shown) in listOf("stretch" to "Daily", "editor" to "Hip flexor", "lift" to "No sets yet")) {
            compose.runOnIdle { route = r }
            var tries = 0
            while (compose.onAllNodesWithText(shown, substring = true).fetchSemanticsNodes().isEmpty()) {
                check(tries++ < 250) { "$r did not show $shown" }
                compose.mainClock.advanceTimeByFrame(); compose.waitForIdle(); Thread.sleep(20)
            }
            repeat(10) { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }
            assertTrue(r, compose.onAllNodesWithContentDescription("Target, ", substring = true).fetchSemanticsNodes().isEmpty())
            assertTrue(r, compose.onAllNodesWithText("Target").fetchSemanticsNodes().isEmpty())
            assertTrue(r, compose.onAllNodesWithText("Target est. 1RM").fetchSemanticsNodes().isEmpty())
        }
    }
}
