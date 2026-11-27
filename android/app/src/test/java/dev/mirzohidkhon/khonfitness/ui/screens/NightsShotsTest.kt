package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.room.withTransaction
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.NightStore
import dev.mirzohidkhon.khonfitness.data.NightsState
import dev.mirzohidkhon.khonfitness.health.HcStatus
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h3200dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NightsShotsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val dir = File("build/shots-nights")
    private lateinit var app: KhonApp
    private lateinit var vm: AppViewModel
    private lateinit var scope: CoroutineScope
    private lateinit var store: NightStore
    private val source = FakeSleepSource()
    private val zone = ZoneId.systemDefault()
    private val clock = nightTestClock(zone)

    @Before fun open() {
        assumeTrue(dir.isDirectory)
        app = newApp()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        store = installNights(app, source, scope, zone)
        vm = AppViewModel(app)
        runBlocking(Dispatchers.IO) { vm.maxHeartRate.set(LOAD_FIXTURE_MAX) }
    }

    @After fun close() {
        if (!::app.isInitialized) return
        runBlocking(Dispatchers.IO) { app.db.withTransaction {} }
        if (::vm.isInitialized) vm.viewModelScope.cancel()
        scope.cancel(); app.scope.cancel()
        compose.waitForIdle()
        app.db.close()
    }

    @Suppress("UNCHECKED_CAST")
    private fun shot(name: String): Bitmap {
        compose.waitForIdle()
        return compose.runOnIdle {
            val global = Class.forName("android.view.WindowManagerGlobal").getMethod("getInstance").invoke(null)
            val views = (global.javaClass.getDeclaredField("mViews").apply { isAccessible = true }.get(global) as List<View>).filter { it.isShown }
            val base = compose.activity.window.decorView
            val bmp = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            for (v in views) {
                val at = IntArray(2).also { v.getLocationOnScreen(it) }
                canvas.save(); canvas.translate(at[0].toFloat(), at[1].toFloat()); v.draw(canvas); canvas.restore()
            }
            File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp
        }
    }

    private fun show(scale: Float, content: @Composable () -> Unit) = compose.setContent {
        val d = LocalDensity.current
        val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
        CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
            KhonTheme { Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { content() } }
        }
    }

    private fun tag(scale: Float) = if (scale == 1f) "1_0" else "1_3"
    private fun waitText(text: String) = compose.waitUntil(15_000) { compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    private fun waitSaid(text: String) = compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription(text, substring = true).fetchSemanticsNodes().isNotEmpty() }

    private fun layout(n: SemanticsNode): TextLayoutResult = mutableListOf<TextLayoutResult>().also { n.config[SemanticsActions.GetTextLayoutResult].action?.invoke(it) }.single()

    /** Every text on screen fits its box: no cut line, no ellipsis, no line wider than the text. */
    private fun noClippedText() {
        val nodes = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue(nodes.isNotEmpty())
        val clipped = nodes.map(::layout).filter { t ->
            t.didOverflowHeight || t.isLineEllipsized(t.lineCount - 1) || (0 until t.lineCount).any { t.getLineRight(it) - t.getLineLeft(it) > t.size.width + 0.5f }
        }
        assertEquals(clipped.map { it.layoutInput.text.text }.toString(), 0, clipped.size)
    }

    private fun nights(scale: Float, status: HcStatus = HcStatus.AVAILABLE) =
        show(scale) { NightsScreen(vm, rememberNavController(), clock, hcStatus = { status }) }

    private fun loaded(scale: Float) {
        source.spans = chartSpans(zone)
        runBlocking(Dispatchers.IO) { seedLoad(app, chartFixtures(zone)) }
        nights(scale)
        waitText(NIGHT_BEFORE_TITLE)
        waitSaid("$LONG_TITLE, 1 night")
        noClippedText()
        val bmp = shot("nights-loaded-${tag(scale)}")
        val bars = (0L until 14L).map { NIGHT_TODAY.minusDays(it) }.filter { d -> compose.onAllNodes(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.TestTag, nightFillTag(d)), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        assertEquals(11, bars.size)
        val colors = bars.map { d ->
            val r = compose.onNodeWithTag(nightFillTag(d), useUnmergedTree = true).fetchSemanticsNode().boundsInWindow
            bmp.getPixel(r.center.x.toInt(), (r.top + r.height * 0.75f).toInt())
        }.toSet()
        assertEquals(setOf(K.Night.toArgb()), colors)
    }

    @Test fun loaded10() = loaded(1f)
    @Test fun loaded13() = loaded(1.3f)

    private fun noAccess(scale: Float) {
        source.mode = SleepMode.NO_ACCESS
        nights(scale)
        waitText(NIGHTS_ALLOW_LINE)
        noClippedText()
        shot("nights-noaccess-${tag(scale)}")
    }

    @Test fun noAccess10() = noAccess(1f)
    @Test fun noAccess13() = noAccess(1.3f)

    private fun empty(scale: Float) {
        nights(scale)
        waitText(NIGHTS_EMPTY)
        noClippedText()
        shot("nights-empty-${tag(scale)}")
    }

    @Test fun empty10() = empty(1f)
    @Test fun empty13() = empty(1.3f)

    private fun logWeek(scale: Float) {
        source.spans = logWeekSpans(zone)
        runBlocking(Dispatchers.IO) { seedLoad(app, logWeekFixtures(zone)); app.db.dao().upsertStretchSessions(listOf(stretchRun(NIGHT_TODAY.minusDays(2), zone))) }
        show(scale) { HistoryScreen(vm, rememberNavController(), clock) }
        waitText("Night 10 h 05 m · 23:10 – 09:15")
        waitText(NO_NIGHT_DATA)
        noClippedText()
        shot("log-week-${tag(scale)}")
    }

    @Test fun logWeek10() = logWeek(1f)
    @Test fun logWeek13() = logWeek(1.3f)

    private fun logIn(mode: SleepMode, reached: (NightsState) -> Boolean): Bitmap {
        source.spans = logWeekSpans(zone)
        source.mode = mode
        runBlocking(Dispatchers.IO) { seedLoad(app, logWeekFixtures(zone)); app.db.dao().upsertStretchSessions(listOf(stretchRun(NIGHT_TODAY.minusDays(2), zone))) }
        show(1f) { HistoryScreen(vm, rememberNavController(), clock) }
        waitText("Stretch: Morning")
        compose.waitUntil(15_000) { source.reads.get() >= 1 && reached(store.state.value) }
        return shot("log-${mode.name.lowercase()}")
    }

    @Test fun logNotRead() { logIn(SleepMode.HANG) { it == NightsState.NotRead } }
    @Test fun logNoAccess() { logIn(SleepMode.NO_ACCESS) { it == NightsState.NoAccess } }
    @Test fun logFailed() { logIn(SleepMode.FAIL) { it == NightsState.Failed } }

    @Test fun review() {
        source.spans = logWeekSpans(zone)
        runBlocking(Dispatchers.IO) { seedLoad(app, logWeekFixtures(zone)) }
        show(1f) { ReviewScreen(vm, rememberNavController(), "log-g") }
        waitSaid("Night before, 7 hours 5 minutes")
        waitText(GYM_LIFTS.first().first)
        compose.waitUntil(15_000) { compose.onAllNodes(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.TestTag, HEART_PICTURE_TAG)).fetchSemanticsNodes().isNotEmpty() }
        shot("review-gym-1_0")
    }

    @Test fun compare() {
        source.spans = logWeekSpans(zone) + nightSpan(LocalDate.of(2026, 10, 20), 400, zone)
        runBlocking(Dispatchers.IO) { seedLoad(app, logWeekFixtures(zone)) }
        show(1f) { CompareScreen(vm, rememberNavController(), clock) }
        waitSaid("Sleep per night, ")
        shot("compare-1_0")
    }
}
