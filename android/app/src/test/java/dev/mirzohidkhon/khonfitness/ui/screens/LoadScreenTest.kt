package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.SessionHr
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h3200dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoadScreenTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var nav: NavHostController
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.of(2026, 11, 25)
    private val clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.systemDefault()
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = LocalDateTime.of(2026, 11, 25, 21, 0).atZone(zone).toInstant()
    }

    @Before fun open() {
        app = KhonApp()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply { isAccessible = true }
            .invoke(app, ApplicationProvider.getApplicationContext<Application>())
        vm = AppViewModel(app)
        io { vm.maxHeartRate.set(LOAD_FIXTURE_MAX) }
    }

    @After fun close() {
        if (!::app.isInitialized) return
        io { app.db.withTransaction {} }
        if (::vm.isInitialized) vm.viewModelScope.cancel()
        app.scope.cancel()
        compose.waitForIdle()
        app.db.close()
    }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun spoken(text: String) = compose.onAllNodesWithContentDescription(text).fetchSemanticsNodes().isNotEmpty()
    private fun waitTag(tag: String) = compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun waitRow(week: LoadWeek) {
        val want = loadWeekSpoken(week)
        runCatching { compose.waitUntil(15_000) { spoken(want) } }.onFailure {
            val got = compose.onAllNodesWithTag(loadWeekTag(week.monday)).fetchSemanticsNodes().map { it.config.getOrNull(SemanticsProperties.ContentDescription) }
            throw AssertionError("want $want, got $got", it)
        }
    }

    private fun show(scale: Float = 1f, start: String = Routes.LOAD) {
        compose.setContent {
            val d = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
                KhonTheme {
                    Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) {
                        nav = rememberNavController()
                        NavHost(nav, startDestination = start) {
                            composable(Routes.HISTORY) { HistoryScreen(vm, nav, clock) }
                            composable(Routes.LOAD) { LoadScreen(vm, nav, clock) }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun expected(fixtures: List<PictureFixture>, max: Int = LOAD_FIXTURE_MAX): LoadYear {
        val sessions = fixtures.map { it.session }
        return loadYear(sessions, factsById(sessions, fixtures.mapNotNull { it.row }, zone), max, today)
    }

    @Test fun entryIsAbsentWithoutALoadAndOpensTheScreen() {
        val plain = loadFewFixtures(today, zone).map { it.copy(row = null) }
        io { seedLoad(app, plain) }
        show(start = Routes.HISTORY)
        compose.onNodeWithText("Progress").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil(15_000) { shows("Compare two periods") }
        compose.waitForIdle()
        assertFalse(shows("Load"))
        io { seedLoad(app, loadFewFixtures(today, zone)) }
        compose.waitUntil(15_000) { shows("Load") }
        compose.onNodeWithText("Load").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        waitTag(LOAD_GRID_TAG)
        assertEquals(Routes.LOAD, nav.currentDestination?.route)
        compose.onNodeWithContentDescription("Back").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil(15_000) { shows("Compare two periods") }
        assertEquals(Routes.HISTORY, nav.currentDestination?.route)
    }

    private fun accentColumns(): Pair<Int, Int> {
        val bmp = compose.nodeImage(compose.activity.window.decorView, compose.onNodeWithTag(LOAD_CHART_TAG))
        val accent = K.Accent.toArgb()
        fun near(p: Int) = listOf(16, 8, 0).all { sh -> abs((p shr sh and 255) - (accent shr sh and 255)) <= 24 }
        return (0 until bmp.width).count { x -> (0 until bmp.height).any { y -> near(bmp.getPixel(x, y)) } } to bmp.width
    }

    @Test fun chartDrawsTheLineOfTheWeeksWithAValue() {
        val f = loadYearFixtures(today, zone)
        io { seedLoad(app, f) }
        show()
        waitTag(LOAD_CHART_TAG)
        assertTrue(shows("Load per week"))
        val points = loadChart(expected(f))
        assertEquals(10, points.size)
        assertEquals(points.sortedBy { it.date }, points)
        val (columns, width) = accentColumns()
        assertTrue("accent in $columns of $width columns", columns > width * 0.6)
    }

    @Test fun oneWeekWithAValueHasNoChart() {
        val one = loadFewFixtures(today, zone).take(1)
        io { seedLoad(app, one) }
        show()
        waitTag(LOAD_GRID_TAG)
        waitRow(expected(one).weeks[2])
        assertTrue(compose.onAllNodesWithTag(LOAD_CHART_TAG).fetchSemanticsNodes().isEmpty())
        assertFalse(shows("Load per week"))
    }

    private fun cell(date: LocalDate) = compose.onNodeWithTag(loadCellTag(date), useUnmergedTree = true)
    private fun image(node: SemanticsNodeInteraction): Bitmap = compose.nodeImage(compose.activity.window.decorView, node)
    private fun near(p: Int, c: Color, tol: Int = 6) = listOf(16, 8, 0).all { sh -> abs((p shr sh and 255) - (c.toArgb() shr sh and 255)) <= tol }

    @Test fun cellsFillByStepAndSumsStandOnTheirRows() {
        val f = loadYearFixtures(today, zone)
        io { seedLoad(app, f) }
        show()
        waitTag(LOAD_GRID_TAG)
        val year = expected(f)
        waitRow(year.weeks[0])
        val highest = year.highest!!
        val days = year.weeks.flatMap { it.days }
        val byStep = (1..4).associateWith { s -> days.first { it.load != null && loadStep(it.load!!, highest) == s } }
        for ((step, day) in byStep) {
            val bmp = image(cell(day.date))
            val want = K.Accent.copy(alpha = LOAD_STEP_ALPHAS[step - 1]).compositeOver(K.Bg)
            val center = bmp.getPixel(bmp.width / 2, bmp.height / 2)
            assertTrue("step $step on ${day.date}: ${Integer.toHexString(center)}", near(center, want))
        }
        val trained = days.first { !it.empty && it.load == null }
        image(cell(trained.date)).let { bmp ->
            assertTrue("no fill", near(bmp.getPixel(bmp.width / 2, bmp.height / 2), K.Surface))
            assertTrue("border", near(bmp.getPixel(bmp.width / 2, 1), K.Muted, 40))
        }
        val empty = days.first { it.empty }
        image(cell(empty.date)).let { bmp ->
            assertTrue("no fill", near(bmp.getPixel(bmp.width / 2, bmp.height / 2), K.Surface))
            assertTrue("no border", near(bmp.getPixel(bmp.width / 2, 1), K.Surface))
        }
        for (w in year.weeks.take(8)) {
            val sum = w.load?.toString() ?: continue
            assertTrue("${w.monday} $sum", compose.onAllNodes(hasText(sum) and hasAnyAncestor(hasTestTag(loadWeekTag(w.monday))), useUnmergedTree = true).fetchSemanticsNodes().size == 1)
        }
        assertEquals(52, compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("week row") { n -> n.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("load-week-") == true }).fetchSemanticsNodes().size)
        assertTrue(compose.onAllNodesWithTag(loadCellTag(today.plusDays(1)), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
        assertTrue(compose.onAllNodesWithTag(loadCellTag(today), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test fun gridAndChartFollowTheMaximumAndAFill() {
        val few = loadFewFixtures(today, zone)
        io { seedLoad(app, few) }
        show()
        waitTag(LOAD_CHART_TAG)
        val at186 = expected(few)
        waitRow(at186.weeks[2])
        io { vm.maxHeartRate.set(150) }
        val at150 = expected(few, 150)
        assertTrue(at150.weeks[2].load!! > at186.weeks[2].load!!)
        waitRow(at150.weeks[2])
        val interval = few[2]
        val filled = intervalFixture(interval.session.date.let { LocalDate.parse(it) }, zone, "few-i").row!!
        val week = 20
        assertTrue(spoken(loadWeekSpoken(at150.weeks[week])))
        io { app.db.dao().upsertSessionHr(filled) }
        val after = expected(few.dropLast(1) + interval.copy(row = filled), 150)
        assertNotNull(after.weeks[week].load)
        waitRow(after.weeks[week])
    }

    private fun layout(n: SemanticsNode): TextLayoutResult {
        val out = mutableListOf<TextLayoutResult>()
        n.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(out)
        return out.single()
    }

    private fun fits(scale: Float) {
        val f = loadYearFixtures(today, zone)
        io { seedLoad(app, f) }
        show(scale)
        waitTag(LOAD_GRID_TAG)
        val year = expected(f)
        val heavy = year.weeks[HEAVY_WEEK]
        assertTrue("4 digits: ${heavy.load}", heavy.load!! in 1000..9999)
        waitRow(heavy)
        val texts = compose.onAllNodes(hasAnyAncestor(hasTestTag(LOAD_GRID_TAG)) and androidx.compose.ui.test.SemanticsMatcher("text") { it.config.getOrNull(SemanticsProperties.Text) != null }, useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue(texts.size > 52)
        for (t in texts) {
            val r = layout(t)
            assertFalse("clipped: ${t.config[SemanticsProperties.Text]}", r.lineCount != 1 || r.multiParagraph.maxIntrinsicWidth > r.size.width + 0.5f || r.didOverflowHeight)
        }
        val widths = year.weeks.flatMap { it.days }.map { compose.onNodeWithTag(loadCellTag(it.date), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width }
        assertTrue("equal cells ${widths.min()} ${widths.max()}", widths.max() - widths.min() <= 1f)
        val row = compose.onNodeWithTag(loadWeekTag(heavy.monday), useUnmergedTree = true).fetchSemanticsNode()
        val sum = compose.onAllNodes(hasText(heavy.load.toString()) and hasAnyAncestor(hasTestTag(loadWeekTag(heavy.monday))), useUnmergedTree = true).fetchSemanticsNodes().single().boundsInRoot
        val sunday = compose.onNodeWithTag(loadCellTag(heavy.monday.plusDays(6)), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("sum after the cells", sum.left >= sunday.right)
        assertTrue("sum inside its row", sum.right <= row.boundsInRoot.right + 0.5f)
        val rows = year.weeks.map { compose.onNodeWithTag(loadWeekTag(it.monday), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot }
        rows.zipWithNext().forEach { (a, b) -> assertFalse("rows overlap", a.overlaps(b) && a.intersect(b).height > 0.5f) }
    }

    @Test fun fitsAt360() = fits(1f)
    @Test fun fitsAt360Scale13() = fits(1.3f)
    @Config(qualifiers = "w411dp-h3200dp-xxhdpi")
    @Test fun fitsAt411() = fits(1f)
    @Config(qualifiers = "w411dp-h3200dp-xxhdpi")
    @Test fun fitsAt411Scale13() = fits(1.3f)
}
