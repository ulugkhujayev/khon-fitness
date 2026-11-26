package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import java.time.LocalDate
import kotlin.math.roundToInt
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
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h2400dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SetupGroupTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private val today = LocalDate.now()

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
    }

    @After fun close() { vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() }

    private fun detail(id: String) {
        runBlocking(Dispatchers.IO) { seedSetupChanges(app.db.dao(), today) }
        compose.setContent { KhonTheme { ExerciseDetailScreen(vm, rememberNavController(), id) } }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Sessions", substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun day(back: Long) = today.minusDays(back).let { if (it.year == today.year) monthDay(it) else "${monthDay(it)}, ${it.year}" }
    private fun bounds(text: String) = compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot

    @Test fun `three periods show three rows newest first, between the chart and the most reps, and open nothing`() {
        detail("bench")
        val rows = compose.onAllNodesWithContentDescription(" per week", substring = true).fetchSemanticsNodes()
            .plus(compose.onAllNodesWithContentDescription("no rate", substring = true).fetchSemanticsNodes())
            .sortedBy { it.boundsInRoot.top }
        val spoken = rows.map { it.config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].single() }
        assertEquals(3, spoken.size)
        assertTrue(spoken[0], spoken[0].startsWith("Since ${day(26)}, TP v4, 4 sets, 6 to 10 reps, plus "))
        assertTrue(spoken[0], spoken[0].endsWith(" kilograms per week"))
        assertTrue(spoken[1], spoken[1].startsWith("${day(54)} to ${day(27)}, TP v4, 4 sets, 5 to 8 reps, plus "))
        assertEquals("${day(70)} to ${day(55)}, TP v3, 3 sets, 8 to 12 reps, no rate", spoken[2])
        compose.onAllNodesWithContentDescription("TP v", substring = true).fetchSemanticsNodes().forEach {
            assertTrue("a rate row opens nothing", SemanticsActions.OnClick !in it.config)
        }
        val title = bounds("Est. 1RM per week")
        assertTrue("under the chart", bounds("Heaviest set per session").bottom < title.top)
        assertTrue("over the most reps", title.bottom < rows.first().boundsInRoot.top && rows.last().boundsInRoot.bottom < bounds("Most reps by weight").top)
        val rates = compose.onAllNodesWithTag(PERIOD_RATE, useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(3, rates.size)
    }

    @Test fun `one period shows no group`() {
        detail("curl")
        assertEquals(0, compose.onAllNodesWithText("Est. 1RM per week").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithTag(PERIOD_RATE, useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    @Test fun `an exercise without weight is titled reps per week`() {
        detail("pullup")
        compose.onNodeWithText("Reps per week").fetchSemanticsNode()
        assertEquals(2, compose.onAllNodesWithContentDescription(" repetitions per week", substring = true).fetchSemanticsNodes().size +
            compose.onAllNodesWithContentDescription("no rate", substring = true).fetchSemanticsNodes().size)
    }

    private val rows = listOf(
        PeriodRow("Since Nov 3", LONG_PROGRAM, "4 sets · 5–8 reps", "+12.5", ""),
        PeriodRow("Oct 2, 2025 – Nov 2, 2025", "TP v4", "10 sets · 12–15 reps", "−0.3", ""),
        PeriodRow("Sep 1 – Oct 1", "TP v3", "3 sets · 5 reps", null, ""),
    )

    private fun tags(tag: String): List<Rect> = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().map { it.boundsInRoot }

    private var nameRefWidth by mutableStateOf<Int?>(null)
    private var nameEllipsized: Boolean? = null
    private lateinit var view: android.view.View

    private fun layoutHolds(widthDp: Int, scale: Float) {
        var nameWidths = emptyList<Int>()
        var rateWidths = emptyList<Int>()
        compose.setContent {
            view = LocalView.current
            val d = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale; screenWidthDp = widthDp }
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
                KhonTheme {
                    val m = rememberTextMeasurer()
                    val small = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR)
                    val large = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR, fontWeight = FontWeight.Bold)
                    nameWidths = rows.map { m.measure(it.programName, small).size.width }
                    rateWidths = rows.map { m.measure(it.rate ?: "–", large).size.width }
                    Column(Modifier.width(widthDp.dp).background(K.Bg)) {
                        Column(Modifier.padding(horizontal = 16.dp)) { PeriodGroup("Est. 1RM per week", rows) }
                        Column(Modifier.background(K.Surface)) {
                            nameRefWidth?.let { w ->
                                Text(LONG_PROGRAM, style = small, color = K.Muted, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, onTextLayout = { nameEllipsized = it.isLineEllipsized(0) },
                                    modifier = Modifier.width(with(LocalDensity.current) { w.toDp() }).testTag(NAME_REF))
                            }
                            Text(rows[0].rate!!, style = large, maxLines = 1, softWrap = false, modifier = Modifier.testTag(RATE_REF))
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        val px = compose.density.density
        val dates = tags(PERIOD_DATES); val names = tags(PERIOD_PROGRAM); val tails = tags(PERIOD_TAIL); val rates = tags(PERIOD_RATE)
        assertEquals(3, rates.size)
        val right = (widthDp - 16) * px
        for (i in rows.indices) {
            val at = "row $i at $widthDp dp, scale $scale"
            assertTrue("$at: dates over the setup ${dates[i]} ${names[i]}", dates[i].bottom <= names[i].top + 0.5f && dates[i].bottom <= tails[i].top + 0.5f)
            assertTrue("$at: name before tail ${names[i]} ${tails[i]}", names[i].right <= tails[i].left + 0.5f)
            assertTrue("$at: setup before rate ${tails[i]} ${rates[i]}", tails[i].right <= rates[i].left + 0.5f && dates[i].right <= rates[i].left + 0.5f)
            assertTrue("$at: rate inside the row ${rates[i]} of $right", rates[i].right <= right - 16 * px + 0.5f)
            assertTrue("$at: rate whole ${rates[i].width} of ${rateWidths[i]}", rates[i].width + 0.5f >= rateWidths[i])
            assertTrue("$at: text inside the row", dates[i].left >= 16 * px - 0.5f)
        }
        assertTrue("the long name is cut: ${names[0].width} of ${nameWidths[0]}", names[0].width < nameWidths[0])
        val name = compose.onAllNodesWithTag(PERIOD_PROGRAM, useUnmergedTree = true)[0]
        val rate = compose.onAllNodesWithTag(PERIOD_RATE, useUnmergedTree = true)[0]
        compose.runOnIdle { nameRefWidth = (names[0].right.roundToInt() - names[0].left.roundToInt()) }
        compose.waitForIdle()
        assertEquals("the reference name at the row's width ends with an ellipsis", true, nameEllipsized)
        assertTrue("the long name is drawn with an ellipsis", compose.nodeImage(view, name).sameAs(compose.nodeImage(view, compose.onNodeWithTag(NAME_REF))))
        assertTrue("the rate is drawn whole", compose.nodeImage(view, rate).sameAs(compose.nodeImage(view, compose.onNodeWithTag(RATE_REF))))
        assertEquals(40, LONG_PROGRAM.length)
        for (i in 1..2) assertTrue("short name $i whole: ${names[i].width} of ${nameWidths[i]}", names[i].width + 0.5f >= nameWidths[i])
    }

    private companion object {
        const val NAME_REF = "name-ref"
        const val RATE_REF = "rate-ref"
    }

    @Test fun `the rows fit at 360 dp, font 1_0`() = layoutHolds(360, 1f)
    @Test fun `the rows fit at 360 dp, font 1_3`() = layoutHolds(360, 1.3f)
    @Test fun `the rows fit at 411 dp, font 1_0`() = layoutHolds(411, 1f)
    @Test fun `the rows fit at 411 dp, font 1_3`() = layoutHolds(411, 1.3f)
}
