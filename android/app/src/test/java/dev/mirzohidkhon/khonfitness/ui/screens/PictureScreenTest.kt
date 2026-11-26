package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

private const val PICTURE_PAD_RIGHT_DP = 8
private const val TICK_HEIGHT_DP = 8.0

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h2400dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PictureScreenTest {
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
    }

    @After fun close() {
        if (!::app.isInitialized) return
        compose.runOnIdle { reviewId = ""; shown = null }
        compose.waitForIdle()
        io { app.db.withTransaction {} }
        if (::vm.isInitialized) vm.viewModelScope.cancel()
        app.scope.cancel()
        compose.waitForIdle()
        app.db.close()
    }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun seed(f: PictureFixture) = io {
        dao.upsert(f.session)
        dao.upsertSetLogs(f.logs)
        dao.upsertIntervalLogs(f.rounds)
        f.row?.let { dao.upsertSessionHr(it) }
    }
    private fun picture(f: PictureFixture) = workoutPicture(f.session, hrFacts(f.session, f.row, zone), f.logs, f.rounds, zone)!!

    private var reviewId by mutableStateOf("")
    private fun review(id: String, scale: Float = 1f) {
        reviewId = id
        compose.setContent {
            val d = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
                KhonTheme { Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { if (reviewId.isNotEmpty()) ReviewScreen(vm, rememberNavController(), reviewId) } }
            }
        }
    }
    private fun shows(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(text: String) = compose.waitUntil(5_000) { shows(text) }
    private fun chartShown() = compose.onAllNodesWithTag(HEART_PICTURE_TAG).fetchSemanticsNodes().isNotEmpty()

    private var shown by mutableStateOf<WorkoutPicture?>(null)
    private fun chart(p: WorkoutPicture) {
        shown = p
        compose.setContent { KhonTheme { Box(Modifier.background(K.Bg).padding(16.dp)) { shown?.let { HeartPicture(it) } } } }
    }
    private fun image(): Bitmap = compose.nodeImage(compose.activity.window.decorView, compose.onNodeWithTag(HEART_PICTURE_TAG))
    private val pxPerDp: Double get() = compose.density.density.toDouble()

    private fun near(argb: Int, c: Color, tol: Int): Boolean {
        val t = c.toArgb()
        return abs((argb shr 16 and 255) - (t shr 16 and 255)) <= tol && abs((argb shr 8 and 255) - (t shr 8 and 255)) <= tol && abs((argb and 255) - (t and 255)) <= tol
    }
    private fun Bitmap.at(x: Double, y: Double): Int = getPixel(floor(x).toInt().coerceIn(0, width - 1), floor(y).toInt().coerceIn(0, height - 1))
    private fun Bitmap.accentNear(x: Double, y: Double, radiusPx: Int) =
        (-radiusPx..radiusPx).any { dx -> (-radiusPx..radiusPx).any { dy -> near(at(x + dx, y + dy), K.Accent, 30) } }

    private data class Expected(val lengthMs: Long, val buckets: List<HrSample>, val marks: List<Long>, val bands: List<Pair<Long, Long>>) {
        val gaps = buckets.zipWithNext().filter { (a, b) -> b.offsetSec - a.offsetSec > CURVE_MAX_JOINED_GAP_SEC }
        val lone = buckets.filterIndexed { i, b ->
            (i == 0 || b.offsetSec - buckets[i - 1].offsetSec > CURVE_MAX_JOINED_GAP_SEC) && (i == buckets.lastIndex || buckets[i + 1].offsetSec - b.offsetSec > CURVE_MAX_JOINED_GAP_SEC)
        }
        val axis = bpmAxis(buckets)!!
    }
    private fun expected(f: PictureFixture): Expected {
        val range = hrWindow(f.session, zone)!!
        val buckets = parseSamples(f.row!!.samples)!!.filter { it.offsetSec >= 0 && range.fromMs + it.offsetSec * 1000L < range.toMs }.sortedBy { it.offsetSec }
        val marks = f.logs.filter { it.status == SetStatus.DONE }.mapNotNull { it.checkedAt }.filter { it >= range.fromMs && it < range.toMs }.map { it - range.fromMs }.sorted()
        val bands = f.rounds.mapNotNull { r -> r.startedAt?.let { s -> r.endedAt?.let { e -> (s - range.fromMs) to (minOf(e, range.toMs) - range.fromMs) } } }
        return Expected(range.lengthMs, buckets, marks, bands)
    }

    private data class Plot(val left: Double, val right: Double, val lines: List<Double>, val e: Expected) {
        val top get() = lines.first()
        val bottom get() = lines.last()
        fun x(ms: Long) = left + ms * (right - left) / e.lengthMs
        fun y(bpm: Int) = top + (e.axis.high - bpm) * (bottom - top) / (e.axis.high - e.axis.low)
    }
    private fun plot(img: Bitmap, e: Expected): Plot {
        val gridRows = (0 until img.height).filter { y -> (0 until img.width).count { x -> near(img.getPixel(x, y), K.Divider, 6) } > img.width / 5 }
        val lineRows = mutableListOf<MutableList<Int>>()
        gridRows.forEach { r -> if (lineRows.lastOrNull()?.last() == r - 1) lineRows.last() += r else lineRows += mutableListOf(r) }
        val bottomRow = lineRows.last()[lineRows.last().size / 2]
        val left = (0 until img.width).first { near(img.getPixel(it, bottomRow), K.Divider, 6) }.toDouble()
        return Plot(left, img.width - PICTURE_PAD_RIGHT_DP * pxPerDp, lineRows.map { (it.first() + it.last() + 1) / 2.0 }, e)
    }

    private fun runs(img: Bitmap, y: Int, c: Color, tol: Int): List<IntRange> {
        val out = mutableListOf<IntRange>()
        var start = -1
        for (x in 0..img.width) {
            val on = x < img.width && near(img.getPixel(x, y), c, tol)
            if (on && start < 0) start = x
            if (!on && start >= 0) { out += start until x; start = -1 }
        }
        return out
    }

    private fun curveMisses(img: Bitmap, plot: Plot): List<String> {
        val e = plot.e
        val misses = mutableListOf<String>()
        e.buckets.forEach { b ->
            if (!img.accentNear(plot.x(b.offsetSec * 1000L), plot.y(b.bpm), 2)) misses += "no curve at ${b.offsetSec} s, ${b.bpm} bpm"
        }
        e.lone.forEach { b ->
            if (!img.accentNear(plot.x(b.offsetSec * 1000L), plot.y(b.bpm), 1)) misses += "no lone dot at ${b.offsetSec} s"
        }
        val margin = 2 * pxPerDp
        val empty = e.gaps.map { (a, b) -> plot.x(a.offsetSec * 1000L) + margin to plot.x(b.offsetSec * 1000L) - margin } +
            listOf(0.0 to plot.x(e.buckets.first().offsetSec * 1000L) - margin, plot.x(e.buckets.last().offsetSec * 1000L) + margin to img.width.toDouble())
        empty.forEach { (from, to) ->
            val hit = (ceil(from).toInt().coerceAtLeast(0) until floor(to).toInt().coerceAtMost(img.width)).firstOrNull { x -> (0 until img.height).any { y -> near(img.getPixel(x, y), K.Accent, 30) } }
            if (hit != null) misses += "curve pixel in the empty columns ${from.toInt()}..${to.toInt()} at x $hit"
        }
        return misses
    }

    private fun tickMisses(img: Bitmap, plot: Plot, marks: List<Long>): List<String> {
        val misses = mutableListOf<String>()
        fun muted(x: Double, dpAboveBottom: Double) = near(img.at(x, plot.bottom - dpAboveBottom * pxPerDp), K.Muted, 40)
        marks.forEach { ms ->
            val x = plot.x(ms)
            if (!muted(x, 1.0)) misses += "no tick foot on the plot bottom at $ms ms"
            if (!muted(x, TICK_HEIGHT_DP - 1.0)) misses += "no tick top at $ms ms"
            if (muted(x, TICK_HEIGHT_DP + 1.5)) misses += "tick higher than $TICK_HEIGHT_DP dp at $ms ms"
            if (muted(x, -1.5)) misses += "tick under the plot bottom at $ms ms"
        }
        val found = runs(img, floor(plot.bottom - TICK_HEIGHT_DP / 2 * pxPerDp).toInt(), K.Muted, 40).filter { it.first >= plot.left - 1 }.map { (it.first + it.last + 1) / 2.0 }
        if (found.size != marks.size || found.zip(marks).any { (fx, ms) -> abs(fx - plot.x(ms)) > 2.5 }) misses += "tick columns $found, expected ${marks.map { plot.x(it) }}"
        return misses
    }

    private fun Plot.bandHeights() = listOf(top + 2 * pxPerDp, lines.let { (it[(it.size - 1) / 2] + it[(it.size - 1) / 2 + 1]) / 2 }, bottom - 2 * pxPerDp)

    private fun bandMisses(img: Bitmap, plot: Plot, heights: List<Double>): List<String> {
        val misses = mutableListOf<String>()
        val edge = pxPerDp
        for (y in heights) for ((fromMs, toMs) in plot.e.bands) {
            val x0 = plot.x(fromMs)
            val x1 = plot.x(toMs)
            listOf(x0 + edge, (x0 + x1) / 2, x1 - edge).forEach { x -> if (!near(img.at(x, y), K.Surface2, 3)) misses += "no band at ($x, $y) for $fromMs..$toMs ms" }
            listOf(x0 - edge, x1 + edge).forEach { x -> if (!near(img.at(x, y), K.Bg, 3)) misses += "band beyond its edge at ($x, $y) for $fromMs..$toMs ms" }
        }
        val runsAtBottom = runs(img, floor(plot.bottom - 2 * pxPerDp).toInt(), K.Surface2, 3).filter { it.first >= plot.left }
        if (runsAtBottom.size != plot.e.bands.size) misses += "${runsAtBottom.size} bands along the bottom, expected ${plot.e.bands.size}"
        return misses
    }

    @Test fun `the gym chart draws the curve and a tick at each set mark only`() {
        val f = gymFixture(day, zone)
        val p = picture(f)
        val e = expected(f)
        assertEquals(18, e.marks.size)
        assertEquals(listOf(GYM_LONE_BUCKET_SEC), e.lone.map { it.offsetSec })
        chart(p)
        var img = image()
        val plot = plot(img, e)
        assertEquals(e.axis.lines.size, plot.lines.size)
        curveMisses(img, plot).let { assertTrue("$it", it.isEmpty()) }
        tickMisses(img, plot, e.marks).let { assertTrue("$it", it.isEmpty()) }
        assertTrue("a tick at no expected column passes", tickMisses(img, plot, e.marks.drop(1)).isNotEmpty())
        compose.runOnIdle { shown = p.copy(markOffsetsMs = emptyList()) }
        img = image()
        assertTrue("marks removed: the tick check still passes", tickMisses(img, plot, e.marks).isNotEmpty())
        compose.runOnIdle { shown = p.copy(curve = emptyList()) }
        img = image()
        assertTrue("curve removed: the curve check still passes", curveMisses(img, plot).isNotEmpty())
    }

    @Test fun `the cardio chart draws the curve over a band at each round`() {
        val f = cardioFixture(day, zone)
        val p = picture(f)
        val e = expected(f)
        assertEquals(4, e.bands.size)
        chart(p)
        var img = image()
        val plot = plot(img, e)
        assertEquals(e.axis.lines.size, plot.lines.size)
        curveMisses(img, plot).let { assertTrue("$it", it.isEmpty()) }
        bandMisses(img, plot, listOf(plot.bottom - 2 * pxPerDp)).let { assertTrue("$it", it.isEmpty()) }
        tickMisses(img, plot, emptyList()).let { assertTrue("$it", it.isEmpty()) }
        compose.runOnIdle { shown = p.copy(curve = emptyList()) }
        img = image()
        bandMisses(img, plot, plot.bandHeights()).let { assertTrue("$it", it.isEmpty()) }
        compose.runOnIdle { shown = p.copy(bands = emptyList()) }
        img = image()
        assertTrue("bands removed: the band check still passes", bandMisses(img, plot, plot.bandHeights()).isNotEmpty())
    }

    private fun assertExistingGym() {
        assertTrue(shows("Barbell Bench Press") && shows("Set 1") && shows("Pull-up"))
    }

    @Test fun `a gym session with a filled fact shows the chart and the group`() {
        seed(gymFixture(day, zone))
        review("gym")
        waitFor("Sets in order")
        compose.waitUntil(5_000) { chartShown() }
        assertExistingGym()
        val spoken = compose.onNodeWithTag(HEART_PICTURE_TAG).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)
        assertEquals(listOf("Heart rate from 18:02 to 19:14, lowest 92 beats per minute, highest 164 beats per minute, 18 sets marked"), spoken)
        val rows = compose.onAllNodes(hasContentDescription("since the set before", substring = true)).fetchSemanticsNodes()
        assertEquals(17, rows.size)
        assertTrue(rows.none { it.config.contains(SemanticsActions.OnClick) })
    }

    @Test fun `a gym session with stamps and no heart rate shows the group only`() {
        seed(gymFixture(day, zone, heart = false, id = "plain"))
        review("plain")
        waitFor("Sets in order")
        assertFalse(chartShown())
        assertFalse(shows("bpm"))
        assertExistingGym()
    }

    @Test fun `a cardio session with rounds shows the chart only`() {
        seed(cardioFixture(day, zone))
        review("cardio")
        compose.waitUntil(5_000) { chartShown() }
        assertFalse(shows("Sets in order"))
        assertTrue(shows("Time") && shows("37 min"))
    }

    @Test fun `a session without a range and an unfinished one show nothing new`() {
        val f = gymFixture(day, zone)
        seed(f.copy(session = f.session.copy(id = "norange", timedFrom = null, timedTo = null), logs = f.logs.map { it.copy(id = "n" + it.id, sessionId = "norange") }, row = f.row!!.copy(sessionId = "norange")))
        seed(f.copy(session = f.session.copy(id = "open", finishedAt = null), logs = f.logs.map { it.copy(id = "o" + it.id, sessionId = "open") }, row = f.row.copy(sessionId = "open")))
        review("norange")
        waitFor("Barbell Bench Press")
        assertExistingGym()
        assertFalse(chartShown()); assertFalse(shows("Sets in order"))
        compose.runOnIdle { reviewId = "open" }
        compose.waitForIdle()
        waitFor("Barbell Bench Press")
        assertFalse(chartShown()); assertFalse(shows("Sets in order"))
    }

    @Test fun `the chart appears when the fill commits and goes on Remove, without leaving Review`() {
        val f = gymFixture(day, zone, heart = false, id = "live")
        seed(f)
        val range = HrRange(f.session.timedFrom!!, f.session.timedTo!!)
        val heart = FakeHeart { r -> listOf(HeartRecord("mi", (r.fromMs until r.toMs step 5_000).mapIndexed { i, t -> HeartPoint(t, 100 + i % 60) })) }
        val fill = app.repo.heartFill(heart, app.scope)
        review("live")
        waitFor("Sets in order")
        assertFalse(chartShown())
        assertEquals(HrReread.FILLED, runBlocking { fill.rereadHeartRate("live") })
        assertEquals(listOf(range), heart.calls)
        compose.waitUntil(5_000) { chartShown() }
        assertTrue(shows("bpm"))
        assertTrue(runBlocking { fill.removeHeartRate("live") })
        compose.waitUntil(5_000) { !chartShown() }
        assertFalse(shows("bpm"))
        assertTrue(shows("Sets in order"))
    }

    private val longName = "Single-Arm Cable Rear Delt Fly to Raise X"

    private fun thirtySets(): PictureFixture {
        val f = gymFixture(day, zone, id = "thirty", names = listOf(longName) + GYM_LIFTS.drop(1).map { it.first })
        val start = f.session.timedFrom!!
        val logs = (0 until 30).map { i ->
            SetLog("t$i", "thirty", i / 5, false, 0, i % 5, i, "ex${i / 5}", if (i < 5) longName else GYM_LIFTS[i / 5].first, 8, 12, 2.5,
                weightKg = 102.5, reps = 12, status = SetStatus.DONE, checkedAt = start + (3 * 60 + i * 130) * 1000L)
        }
        return f.copy(logs = logs)
    }

    private fun layout(n: SemanticsNode): TextLayoutResult {
        val out = mutableListOf<TextLayoutResult>()
        n.config[SemanticsActions.GetTextLayoutResult].action?.invoke(out)
        return out.single()
    }

    private fun checkLayout(scale: Float) {
        seed(thirtySets())
        review("thirty", scale)
        waitFor("Sets in order")
        val rows = compose.onAllNodes(hasContentDescription("checked at", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(30, rows.size)
        compose.onAllNodes(hasContentDescription("checked at", substring = true))[29].performScrollTo()
        for (row in rows) {
            val texts = row.children.filter { it.config.contains(SemanticsProperties.Text) }
            assertTrue(texts.size >= 4)
            val (name, clock, set) = texts
            val values = texts.drop(3)
            fun clipped(n: SemanticsNode) = layout(n).let { it.lineCount != 1 || it.multiParagraph.maxIntrinsicWidth > it.size.width + 0.5f || it.didOverflowHeight }
            assertFalse("name", layout(name).lineCount != 1)
            assertFalse("clock clipped", clipped(clock))
            assertFalse("set text clipped: ${set.config[SemanticsProperties.Text]}", layout(set).let { it.didOverflowHeight || it.isLineEllipsized(it.lineCount - 1) })
            assertEquals("set text wraps", 1, layout(set).lineCount)
            values.forEach { assertFalse("value clipped", clipped(it)) }
            assertTrue("name runs into the clock", name.boundsInRoot.right <= clock.boundsInRoot.left)
            assertTrue("set text runs into the values", set.boundsInRoot.right <= values.minOf { it.boundsInRoot.left })
            values.zipWithNext().forEach { (a, b) -> assertTrue("values overlap", a.boundsInRoot.right <= b.boundsInRoot.left) }
            assertTrue((values + clock).all { it.boundsInRoot.right <= row.boundsInRoot.right })
            assertTrue("set line under the name line", set.boundsInRoot.top >= name.boundsInRoot.bottom - 1)
        }
        assertTrue(layout(rows[0].children[0]).isLineEllipsized(0))
        assertTrue(compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().none { it.config.getOrNull(SemanticsProperties.ContentDescription)?.any { d -> "checked at" in d } == true })
    }

    @Test fun `30 sets and a 40-character name fit at 360 dp and font scale 1_3`() = checkLayout(1.3f)

    @Test fun `30 sets and a 40-character name fit at 360 dp and font scale 1_0`() = checkLayout(1f)

    @Config(qualifiers = "w411dp-h2400dp-xxhdpi")
    @Test fun `30 sets and a 40-character name fit at 411 dp and font scale 1_3`() = checkLayout(1.3f)
}
