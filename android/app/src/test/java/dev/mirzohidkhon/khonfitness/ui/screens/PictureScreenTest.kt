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

/** The heart rate picture on Review: its pixels, which sessions get it, a live fill and remove, and its layout. */
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

    @After fun close() { if (::app.isInitialized) { if (::vm.isInitialized) vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() } }

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
                KhonTheme { Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) { ReviewScreen(vm, rememberNavController(), reviewId) } }
            }
        }
    }
    private fun shows(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    private fun waitFor(text: String) = compose.waitUntil(5_000) { shows(text) }
    private fun chartShown() = compose.onAllNodesWithTag(HEART_PICTURE_TAG).fetchSemanticsNodes().isNotEmpty()

    // Pixels ------------------------------------------------------------------------------------------------------

    private var shown by mutableStateOf<WorkoutPicture?>(null)
    private fun chart(p: WorkoutPicture) {
        shown = p
        compose.setContent { KhonTheme { Box(Modifier.background(K.Bg).padding(16.dp)) { shown?.let { HeartPicture(it) } } } }
    }
    private fun image(): Bitmap = compose.nodeImage(compose.activity.window.decorView, compose.onNodeWithTag(HEART_PICTURE_TAG))

    private fun near(argb: Int, c: Color, tol: Int): Boolean {
        val t = c.toArgb()
        return abs((argb shr 16 and 255) - (t shr 16 and 255)) <= tol && abs((argb shr 8 and 255) - (t shr 8 and 255)) <= tol && abs((argb and 255) - (t and 255)) <= tol
    }

    /** The plot as drawn: the bottom grid line's center row and first pixel, and the right inset. */
    private data class Plot(val left: Int, val bottom: Int, val right: Int)
    private fun plot(img: Bitmap): Plot {
        val gridRows = (0 until img.height).filter { y -> (0 until img.width).count { x -> near(img.getPixel(x, y), K.Divider, 6) } > img.width / 5 }
        val last = gridRows.max()
        val run = gridRows.filter { it > last - 6 }
        val bottom = (run.min() + run.max()) / 2
        val left = (0 until img.width).first { near(img.getPixel(it, bottom), K.Divider, 6) }
        return Plot(left, bottom, img.width - (8 * compose.density.density).toInt())
    }
    private fun Plot.x(ms: Long, p: WorkoutPicture) = left + ms.toDouble() * (right - left) / p.range.lengthMs

    /** Runs of [c] along row [y]: first and last column of each. */
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

    private fun curveDrawn(img: Bitmap): Boolean = (0 until img.height).sumOf { y -> (0 until img.width).count { x -> near(img.getPixel(x, y), K.Accent, 30) } } > 300

    /** A tick stands at each of [marks]' columns and at no other. */
    private fun ticksAt(img: Bitmap, p: WorkoutPicture, marks: List<Long>): Boolean {
        val plot = plot(img)
        val y = plot.bottom - (4 * compose.density.density).toInt()
        val found = runs(img, y, K.Muted, 40).filter { it.first >= plot.left }.map { (it.first + it.last) / 2.0 }
        return found.size == marks.size && found.zip(marks).all { (fx, ms) -> abs(fx - plot.x(ms, p)) <= 2.5 }
    }

    /** A band covers each of [bands]' columns and no other along the plot's bottom. */
    private fun bandsAt(img: Bitmap, p: WorkoutPicture, bands: List<RoundBand>): Boolean {
        val plot = plot(img)
        val y = plot.bottom - (3 * compose.density.density).toInt()
        val found = runs(img, y, K.Surface2, 3).filter { it.first >= plot.left }
        return found.size == bands.size && found.zip(bands).all { (r, b) -> abs(r.first - plot.x(b.fromMs, p)) <= 2.5 && abs(r.last + 1 - plot.x(b.toMs, p)) <= 2.5 }
    }

    @Test fun `the gym chart draws the curve and a tick at each set mark only`() {
        val p = picture(gymFixture(day, zone))
        assertEquals(18, p.marks.size)
        chart(p)
        var img = image()
        assertTrue("curve", curveDrawn(img))
        assertTrue("ticks", ticksAt(img, p, p.marks))
        assertFalse("ticks at other columns pass", ticksAt(img, p, p.marks.drop(1)))
        compose.runOnIdle { shown = p.copy(marks = emptyList()) }
        img = image()
        assertFalse("marks removed: the tick check still passes", ticksAt(img, p, p.marks))
        compose.runOnIdle { shown = p.copy(curve = emptyList()) }
        img = image()
        assertFalse("curve removed: the curve check still passes", curveDrawn(img))
    }

    @Test fun `the cardio chart draws the curve over a band at each round`() {
        val p = picture(cardioFixture(day, zone))
        assertEquals(4, p.bands.size)
        chart(p)
        var img = image()
        assertTrue("curve", curveDrawn(img))
        assertTrue("bands", bandsAt(img, p, p.bands))
        assertTrue("no ticks", ticksAt(img, p, emptyList()))
        compose.runOnIdle { shown = p.copy(bands = emptyList()) }
        img = image()
        assertFalse("bands removed: the band check still passes", bandsAt(img, p, p.bands))
    }

    // Screen ------------------------------------------------------------------------------------------------------

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

    // Layout ------------------------------------------------------------------------------------------------------

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
