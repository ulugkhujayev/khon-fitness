package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
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
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h2400dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScoreScreenTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val dao get() = app.db.dao()
    private val zone = ZoneId.systemDefault()
    private val day = LocalDate.now(zone).minusDays(1)

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
        io { vm.maxHeartRate.set(null); dao.upsertExercises(listOf(HIGH_EXERCISE, LOW_EXERCISE)) }
    }

    @After fun close() {
        if (!::app.isInitialized) return
        compose.runOnIdle { screen = "" }
        compose.waitForIdle()
        io { app.db.withTransaction {} }
        if (::vm.isInitialized) vm.viewModelScope.cancel()
        app.scope.cancel()
        compose.waitForIdle()
        app.db.close()
    }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun seed(f: PictureFixture, row: SessionHr? = f.row) = io {
        dao.upsert(f.session)
        dao.upsertIntervalLogs(f.rounds)
        row?.let { dao.upsertSessionHr(it) }
    }

    private var screen by mutableStateOf("")
    private fun show(what: String, scale: Float = 1f) {
        screen = what
        compose.setContent {
            val d = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
                KhonTheme {
                    Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) {
                        val nav = rememberNavController()
                        when {
                            screen == "settings" -> SettingsScreen(vm, nav)
                            screen.startsWith("type:") -> CardioTypeScreen(vm, nav, screen.removePrefix("type:"))
                            screen.isNotEmpty() -> ReviewScreen(vm, nav, screen)
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun shows(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(text: String) = compose.waitUntil(5_000) { shows(text) }
    private fun groupShown() = compose.onAllNodesWithTag(SCORE_GROUP_TAG).fetchSemanticsNodes().isNotEmpty()
    private fun spoken() = compose.onNodeWithTag(SCORE_GROUP_TAG).fetchSemanticsNode().children.map { it.config[SemanticsProperties.ContentDescription].joinToString() }

    @Test fun highIntensityWithFourRounds() {
        val f = intervalFixture(day, zone)
        seed(f)
        io { vm.maxHeartRate.set(186) }
        show(f.session.id)
        waitFor("At or above 159 bpm")
        waitFor("Round 4")
        val rows = spoken()
        assertTrue(rows.first(), rows.first().matches(Regex("At or above 159 beats per minute, \\d+ minutes( \\d+ seconds)?")))
        assertEquals((1..4).map { "Round $it" }, rows.drop(1).map { it.substringBefore(",") })
        assertTrue(rows.drop(1).all { it.matches(Regex("Round \\d, peak \\d+, drop \\d+ beats per minute")) })
        assertTrue("the Intervals group stays", shows("Intervals"))
        assertTrue("the chart stays", compose.onAllNodesWithTag(HEART_PICTURE_TAG).fetchSemanticsNodes().isNotEmpty())
    }

    @Test fun lowIntensitySteady() {
        val f = zoneFixture(day, zone)
        seed(f)
        io { vm.maxHeartRate.set(186) }
        show(f.session.id)
        waitFor("112 to 130 bpm")
        assertTrue(shows("Below 112 bpm") && shows("Above 130 bpm"))
        assertEquals(listOf("Below 112", "112 to 130", "Above 130"), spoken().map { it.substringBefore(" beats") })
        assertFalse(shows("At or above"))
    }

    @Test fun highIntensityWithoutRounds() {
        val f = intervalFixture(day, zone).let { it.copy(rounds = emptyList()) }
        seed(f)
        show(f.session.id)
        waitFor("At or above")
        assertEquals(1, spoken().size)
        assertFalse(shows("Round 1"))
    }

    @Test fun noMaximumShowsNothing() {
        val old = intervalFixture(LocalDate.now(zone).minusDays(400), zone)
        seed(old)
        show(old.session.id)
        waitFor("Intervals")
        compose.waitForIdle()
        assertFalse(groupShown())
        assertFalse(shows("At or above"))
    }

    @Test fun rowsFollowTheTypedMaximumAndTheFill() {
        val f = intervalFixture(day, zone)
        seed(f, row = f.row!!.copy(state = HrState.WAITING, samples = "", avgHr = null, maxHr = null))
        io { vm.maxHeartRate.set(186) }
        show(f.session.id)
        waitFor("Intervals")
        assertFalse(groupShown())
        io { dao.upsertSessionHr(f.row!!) }
        waitFor("At or above 159 bpm")
        io { vm.maxHeartRate.set(200) }
        waitFor("At or above 170 bpm")
        assertFalse(shows("At or above 159 bpm"))
        io { vm.maxHeartRate.set(null) }
        waitFor("At or above ${hardLine(f.row!!.maxHr!!)} bpm")
    }

    private fun rowSpoken(): String = compose.onAllNodes(SemanticsMatcher("max row") { n -> n.config.getOrNull(SemanticsProperties.ContentDescription)?.any { it.startsWith("Maximum heart rate,") } == true })
        .fetchSemanticsNodes().single().config[SemanticsProperties.ContentDescription].single()
    private fun waitSpoken(text: String) = compose.waitUntil(5_000) { runCatching { rowSpoken() == text }.getOrDefault(false) }

    @Test fun settingsRowShowsTypedMeasuredOrNotSet() {
        show("settings")
        waitSpoken("Maximum heart rate, not set")
        val f = intervalFixture(day, zone)
        seed(f)
        val measured = f.row!!.maxHr!!
        waitSpoken("Maximum heart rate, measured $measured beats per minute")
        io { vm.maxHeartRate.set(186) }
        waitSpoken("Maximum heart rate, 186 beats per minute")
    }

    private val field = hasContentDescription("Maximum heart rate, beats per minute").and(hasSetTextAction())
    private fun typedNow(): Int? = runBlocking(Dispatchers.IO) { vm.maxHeartRate.typed.first() }

    @Test fun sheetRefusesBadValuesAndClears() {
        show("settings")
        waitSpoken("Maximum heart rate, not set")
        compose.onNodeWithContentDescription("Maximum heart rate, not set").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(field).fetchSemanticsNodes().isNotEmpty() }
        for (bad in listOf("99", "231")) {
            compose.onNode(field).performTextReplacement(bad)
            compose.onNodeWithText("Save").assertIsNotEnabled().performClick()
            compose.waitForIdle()
            assertNull(bad, typedNow())
        }
        compose.onNode(field).performTextReplacement("186")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        waitSpoken("Maximum heart rate, 186 beats per minute")
        assertEquals(186, typedNow())
        compose.onNodeWithContentDescription("Maximum heart rate, 186 beats per minute").performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(field).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(field).performTextReplacement("")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        waitSpoken("Maximum heart rate, not set")
        assertNull(typedNow())
    }

    private fun fieldText() = compose.onNode(field).fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    @Test fun sheetJudgesTheTextAsTypedAndKeepsTheSavedValue() {
        io { vm.maxHeartRate.set(186) }
        show("settings")
        waitSpoken("Maximum heart rate, 186 beats per minute")
        compose.onNodeWithContentDescription("Maximum heart rate, 186 beats per minute").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(field).fetchSemanticsNodes().isNotEmpty() }
        val enabled = mutableListOf<String>()
        for (input in listOf("", "abc", "200.5", "-201", "2x02", " 150", "150", "99", "231")) {
            compose.onNode(field).performTextReplacement(input)
            compose.waitForIdle()
            assertEquals(input, fieldText())
            val save = compose.onNodeWithText("Save").fetchSemanticsNode()
            if (!save.config.contains(SemanticsProperties.Disabled)) enabled += input
            else { compose.onNodeWithText("Save").performClick(); compose.waitForIdle(); assertEquals(input, 186, typedNow()) }
        }
        assertEquals(listOf("", "150"), enabled)
        assertEquals(186, typedNow())
        compose.onNode(field).performTextReplacement("150")
        compose.onNodeWithText("Save").performClick()
        waitSpoken("Maximum heart rate, 150 beats per minute")
        assertEquals(150, typedNow())
    }

    @Test fun measuredMaximumLeavesOnResumeOnceItIsOlderThan365Days() {
        val f = intervalFixture(day, zone)
        seed(f)
        val measured = f.row!!.maxHr!!
        var now = f.session.finishedAt!! + 365 * 86_400_000L
        compose.setContent {
            CompositionLocalProvider(LocalWallClock provides { now }) {
                KhonTheme { Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { SettingsScreen(vm, rememberNavController()) } }
            }
        }
        waitSpoken("Maximum heart rate, measured $measured beats per minute")
        now += 1
        compose.waitForIdle()
        assertEquals("Maximum heart rate, measured $measured beats per minute", rowSpoken())
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        waitSpoken("Maximum heart rate, not set")
    }

    @Test fun aRowWithARepeatedOffsetCountsTheBucketOnce() {
        val f = intervalFixture(day, zone).copy(rounds = emptyList())
        seed(f, row = filledFor(f.session, zone, listOf(HrSample(0, 175), HrSample(0, 175), HrSample(5, 175), HrSample(10, 120))))
        io { vm.maxHeartRate.set(186) }
        show(f.session.id)
        waitFor("At or above 159 bpm")
        assertEquals(listOf("At or above 159 beats per minute, 10 seconds"), spoken())
    }

    private fun seedType(filled: Int, plain: Int) {
        (0 until filled + plain).forEach { i ->
            val f = intervalFixture(day.minusDays(i * 2L), zone, "t$i")
            seed(f, row = if (i < filled) f.row else null)
        }
        io { vm.maxHeartRate.set(186) }
    }

    private fun accentColumns(): Pair<Int, Int> {
        val bmp = compose.nodeImage(compose.activity.window.decorView, compose.onNodeWithTag(HARD_CHART_TAG))
        val accent = K.Accent.toArgb()
        fun near(p: Int) = listOf(16, 8, 0).all { sh -> abs((p shr sh and 255) - (accent shr sh and 255)) <= 24 }
        return (0 until bmp.width).count { x -> (0 until bmp.height).any { y -> near(bmp.getPixel(x, y)) } } to bmp.width
    }

    @Test fun typeScreenChartDrawsTheLine() {
        seedType(filled = 3, plain = 2)
        show("type:bike")
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(HARD_CHART_TAG).fetchSemanticsNodes().isNotEmpty() }
        assertTrue(shows("At or above 85 %"))
        compose.onNodeWithTag(HARD_CHART_TAG).performScrollTo()
        compose.waitForIdle()
        val (columns, width) = accentColumns()
        assertTrue("accent in $columns of $width columns", columns > width * 0.6)
    }

    @Test fun typeScreenWithOneFilledSessionHasNoChart() {
        seedType(filled = 1, plain = 2)
        show("type:bike")
        waitFor("Sessions")
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithTag(HARD_CHART_TAG).fetchSemanticsNodes().isEmpty())
        assertFalse(shows("At or above 85 %"))
    }

    private fun layout(n: SemanticsNode): TextLayoutResult {
        val out = mutableListOf<TextLayoutResult>()
        n.config[SemanticsActions.GetTextLayoutResult].action?.invoke(out)
        return out.single()
    }

    private fun tenRoundsFit(scale: Float) {
        val f = intervalFixture(day, zone, rounds = 10)
        seed(f)
        io { vm.maxHeartRate.set(186) }
        show(f.session.id, scale)
        waitFor("Round 10")
        val group = compose.onNodeWithTag(SCORE_GROUP_TAG).fetchSemanticsNode()
        val rows = group.children
        assertEquals(11, rows.size)
        rows.forEach { row ->
            val texts = compose.onAllNodes(SemanticsMatcher("in row") { n -> n.config.contains(SemanticsProperties.Text) && generateSequence(n.parent) { it.parent }.any { it.id == row.id } }, useUnmergedTree = true).fetchSemanticsNodes()
            assertEquals(2, texts.size)
            val (label, value) = texts.sortedBy { it.boundsInRoot.left }
            layout(label).let { assertFalse("label clipped", it.didOverflowHeight || it.isLineEllipsized(it.lineCount - 1)) }
            layout(value).let { assertFalse("value clipped: ${value.config[SemanticsProperties.Text]}", it.lineCount != 1 || it.multiParagraph.maxIntrinsicWidth > it.size.width + 0.5f || it.didOverflowHeight) }
            assertTrue("label runs into the value", label.boundsInRoot.right <= value.boundsInRoot.left)
            assertTrue(value.boundsInRoot.right <= row.boundsInRoot.right)
        }
    }

    @Test fun tenRoundsFitAtScale1() = tenRoundsFit(1f)
    @Test fun tenRoundsFitAtScale13() = tenRoundsFit(1.3f)

    @Config(qualifiers = "w411dp-h2400dp-xxhdpi")
    @Test fun tenRoundsFitAt411Scale1() = tenRoundsFit(1f)

    @Config(qualifiers = "w411dp-h2400dp-xxhdpi")
    @Test fun tenRoundsFitAt411Scale13() = tenRoundsFit(1.3f)
}
