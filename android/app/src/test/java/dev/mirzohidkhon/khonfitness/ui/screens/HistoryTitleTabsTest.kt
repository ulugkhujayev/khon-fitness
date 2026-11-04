package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.mirzohidkhon.khonfitness.KhonApp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.components.TITLE_TABS_MIN_SP
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

/** UI07 rows 1 to 5: History's large title is the Log / Progress switch. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class HistoryTitleTabsTest {
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

    @After fun close() { vm.viewModelScope.cancel(); app.scope.cancel(); app.db.close() }

    private fun io(block: suspend () -> Unit) = runBlocking(Dispatchers.IO) { block() }
    private fun waitFor(text: String) = compose.waitUntil(5_000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }

    /** History alone at font [scale], once its Log has loaded. */
    private fun show(scale: Float = 1f) {
        compose.setContent {
            val d = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
                KhonTheme { HistoryScreen(vm, rememberNavController()) }
            }
        }
        waitFor("planned")
    }

    /** Ten bodyweights and five lifts, so the Progress page is long enough to fold the bar. */
    private fun seed() = io {
        dao.upsertExercises((1..5).map { Exercise("e$it", "Lift $it", Kind.STRENGTH, primaryMuscle = "muscle $it") })
        val d = today.minusDays(1)
        val at = d.toEpochDay() * 86_400_000 + 7 * 3_600_000
        dao.upsert(Session("s1", d.iso(), at, at + 3_600_000, ItemType.PROGRAM, programId = "push", programName = "Push"))
        dao.upsertSetLogs((1..5).map { SetLog("l$it", "s1", 0, false, 0, it, it, "e$it", "Lift $it", 8, 12, 2.5, weightKg = 60.0, reps = 8, status = SetStatus.DONE) })
        dao.upsertBodyweights((0 until 10).map { Bodyweight(today.minusDays(3L * it + 1).iso(), 83.4 + it * 0.13) })
    }

    private fun layout(text: String): TextLayoutResult {
        val out = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(out) }
        return out.single()
    }

    /** Rows 1 and 3: one heading spoken as History over two tabs; no History word and no segmented row on screen. */
    @Test fun `the title row is a heading spoken as History with Log and Progress as tabs`() {
        show()
        val heading = compose.onNode(isHeading() and hasContentDescription("History")).fetchSemanticsNode()
        assertTrue(compose.onAllNodesWithText("History").fetchSemanticsNodes().isEmpty())
        for (word in listOf("Log", "Progress")) {
            val tab = compose.onNodeWithText(word).fetchSemanticsNode()
            assertEquals(Role.Tab, tab.config[SemanticsProperties.Role])
            assertEquals(heading.id, tab.parent?.id)
        }
        compose.onNodeWithText("Log").assertIsSelected()
        compose.onNodeWithText("Progress").assertIsNotSelected()
        // The words are in the large title's size, the import button at the end of their row.
        assertEquals(34f, layout("Progress").layoutInput.style.fontSize.value, 0.01f)
        val progress = compose.onNodeWithText("Progress").fetchSemanticsNode().boundsInRoot
        val import = compose.onNodeWithContentDescription("Import workouts").fetchSemanticsNode().boundsInRoot
        assertTrue(progress.right <= import.left)
        assertEquals(progress.center.y, import.center.y, 6 * compose.density.density)
    }

    /** Row 2: a tap opens that page, and the open word changes. */
    @Test fun `a tap on a word opens its page`() {
        seed()
        show()
        compose.onNodeWithText("Progress").performClick()
        waitFor("Personal records")
        compose.onNodeWithText("Progress").assertIsSelected()
        compose.onNodeWithText("Log").assertIsNotSelected()
        compose.onNodeWithText("Log").performClick()
        waitFor("planned")
        compose.onNodeWithText("Log").assertIsSelected()
    }

    /**
     * Row 4: at font scale 1.6 both words and the import button stay on one row inside the screen with nothing cut, at
     * 22 to 34 sp. Android scales large text less than small text (non-linear font scaling), so at 1.6 the words fit at
     * 34 sp on the S25 width. Native graphics, so text has its real width.
     */
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test fun `at font scale 1_6 the words stay on one row with the import button`() = fits(1.6f)

    /** Row 4 on a narrow 280 dp screen at font scale 1.6, where the words at 34 sp are too wide: they step down to fit. */
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w280dp-h780dp-xxhdpi")
    @Test fun `on a narrow screen at font scale 1_6 the words step down to fit`() {
        fits(1.6f)
        assertTrue(layout("Progress").layoutInput.style.fontSize.value < 34f)
    }

    private fun fits(scale: Float) {
        show(scale)
        val px = compose.density.density
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val log = compose.onNodeWithText("Log").fetchSemanticsNode().boundsInRoot
        val progress = compose.onNodeWithText("Progress").fetchSemanticsNode().boundsInRoot
        val import = compose.onNodeWithContentDescription("Import workouts").fetchSemanticsNode().boundsInRoot
        assertTrue("$log $progress", log.right <= progress.left)
        assertTrue("$progress $import", progress.right <= import.left)
        assertTrue(import.right <= root.right && log.left >= 0f)
        assertEquals(log.center.y, progress.center.y, 1f)
        assertEquals(progress.center.y, import.center.y, 10 * px)
        for (word in listOf("Log", "Progress")) {
            val text = layout(word)
            // Every letter on the one line, in the room the word takes: nothing clipped or ellipsized.
            assertEquals("$word on one line", word.length, text.getLineEnd(0))
            assertTrue("$word is cut", text.multiParagraph.intrinsics.maxIntrinsicWidth <= text.size.width + 0.5f && !text.didOverflowHeight)
            assertEquals("$word on one line", 1, text.lineCount)
            val size = text.layoutInput.style.fontSize.value
            assertTrue("$word at $size sp", size >= TITLE_TABS_MIN_SP && size <= 34f)
            assertTrue("$word ${text.size.width} px in ${compose.onNodeWithText(word).fetchSemanticsNode().size.width} px",
                text.size.width <= compose.onNodeWithText(word).fetchSemanticsNode().size.width)
        }
    }

    /** Row 4: the style steps down a whole size at a time until the words fit, and stops at 22 sp. */
    @Test fun `title tabs step down until they fit, not below 22 sp`() {
        val base = androidx.compose.ui.text.TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(34f, androidx.compose.ui.unit.TextUnitType.Sp), lineHeight = androidx.compose.ui.unit.TextUnit(41f, androidx.compose.ui.unit.TextUnitType.Sp))
        val width = { st: androidx.compose.ui.text.TextStyle -> (st.fontSize.value * 10).toInt() }
        assertEquals(34f, dev.mirzohidkhon.khonfitness.ui.components.titleTabsStyle(base, 400, width).fontSize.value)
        val fit = dev.mirzohidkhon.khonfitness.ui.components.titleTabsStyle(base, 285, width)
        assertEquals(28f, fit.fontSize.value)
        assertEquals(41f * 28 / 34, fit.lineHeight.value, 0.01f)
        assertEquals(TITLE_TABS_MIN_SP, dev.mirzohidkhon.khonfitness.ui.components.titleTabsStyle(base, 50, width).fontSize.value)
    }

    /** Row 4: at the default size the words keep the large title's size. */
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test fun `at font scale 1_0 the words keep the large title size`() {
        show()
        assertEquals(34f, layout("Log").layoutInput.style.fontSize.value, 0.01f)
        assertEquals(34f, layout("Progress").layoutInput.style.fontSize.value, 0.01f)
    }

    /**
     * Row 5: once the bar folds the small bar title is the open page's word: the closed word is gone from the screen's
     * semantics and takes no taps, the open one stays. Scrolled back up, both words show again.
     */
    @Test fun `the folded bar shows only the open page's word`() {
        seed()
        show()
        compose.onNodeWithText("Progress").performClick()
        waitFor("Personal records")
        val page = compose.onNode(hasScrollAction())
        val before = compose.onNodeWithText("Personal records", substring = true).fetchSemanticsNode().positionInRoot.y
        page.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 200 * compose.density.density) }
        compose.waitForIdle()
        val after = compose.onNodeWithText("Personal records", substring = true).fetchSemanticsNode().positionInRoot.y
        assertTrue("scrolled ${(before - after) / compose.density.density} dp", before - after >= 40 * compose.density.density)
        assertTrue(compose.onAllNodesWithText("Log").fetchSemanticsNodes().isEmpty())
        compose.onNodeWithText("Progress").assertIsSelected()
        compose.onNode(isHeading() and hasContentDescription("History")).assertExists()
        page.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -100_000f) }
        compose.waitForIdle()
        compose.onNodeWithText("Log").assertIsNotSelected()
    }
}
