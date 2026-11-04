package dev.mirzohidkhon.khonfitness.ui.components

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI07 verify 1: a tab screen's title bar takes the taps on its background, so a tap there never reaches a row that
 * scrolled under it. Both bars: the folding one (Today, Plan) and the pinned one (History). The bar's own controls
 * still take their taps, and a drag that starts on a bar scrolls the content as a drag on the content does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TitleBarTouchTest {
    @get:Rule val compose = createComposeRule()

    private var rowTaps = 0
    private var buttonTaps = 0
    private lateinit var scroll: ScrollState

    /** Thirty tappable rows, scrolled so that rows lie under the whole bar. */
    private fun show(pinned: Boolean) {
        compose.setContent {
            KhonTheme {
                scroll = rememberScrollState()
                val button: @Composable () -> Unit = { Box(Modifier.size(48.dp).clickable { buttonTaps++ }) { Text("Bar button") } }
                val rows: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
                    repeat(30) { Box(Modifier.fillMaxWidth().height(60.dp).clickable { rowTaps++ }) }
                }
                if (pinned) TabScreen(trailing = button, scroll = scroll, title = { Text("Pinned") }, content = rows)
                else TabScreen("Folding", trailing = button, scroll = scroll, content = rows)
            }
        }
        compose.waitForIdle()
        runBlocking { scroll.scrollTo(600 * compose.density.density.toInt()) }
        compose.waitForIdle()
    }

    private fun tapBarBackground(xDp: Float, yDp: Float) {
        val px = compose.density.density
        compose.onRoot().performTouchInput { click(Offset(xDp * px, yDp * px)) }
        compose.waitForIdle()
    }

    @Test fun `a tap on the folding bar's background does not open the row under it`() {
        show(pinned = false)
        tapBarBackground(180f, 35f)
        assertEquals("rows tapped through the folding bar", 0, rowTaps)
        compose.onNodeWithText("Bar button").performTouchInput { click() }
        assertEquals("the bar's button", 1, buttonTaps)
    }

    @Test fun `a tap on the pinned bar's background does not open the row under it`() {
        show(pinned = true)
        tapBarBackground(275f, 58f)
        tapBarBackground(180f, 35f)
        assertEquals("rows tapped through the pinned bar", 0, rowTaps)
        compose.onNodeWithText("Bar button").performTouchInput { click() }
        assertEquals("the bar's button", 1, buttonTaps)
    }

    /** A real finger drag that starts on the bar background at [yDp] and moves [byDp] down. */
    private fun dragFromBar(yDp: Float, byDp: Float) {
        val px = compose.density.density
        compose.onRoot().performTouchInput { swipe(Offset(180 * px, yDp * px), Offset(180 * px, (yDp + byDp) * px), durationMillis = 300) }
        compose.waitForIdle()
    }

    @Test fun `a drag that starts on the folding bar scrolls the content`() {
        show(pinned = false)
        val before = scroll.value
        dragFromBar(35f, 300f)
        assertTrue("scroll ${scroll.value} after a drag down from $before", scroll.value < before - 200 * compose.density.density)
        assertEquals("rows tapped by the drag", 0, rowTaps)
    }

    @Test fun `a drag that starts on the pinned bar scrolls the content`() {
        show(pinned = true)
        val before = scroll.value
        dragFromBar(35f, 300f)
        assertTrue("scroll ${scroll.value} after a drag down from $before", scroll.value < before - 200 * compose.density.density)
        assertEquals("rows tapped by the drag", 0, rowTaps)
    }

    /**
     * A flick on the content starts a fling; a finger then lands on the bar at [yDp] (or on the content, as a
     * control) and holds still. The content stops as it does under a finger on the content.
     */
    private fun holdStopsFling(pinned: Boolean, yDp: Float) {
        compose.setContent {
            KhonTheme {
                scroll = rememberScrollState()
                val rows: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = { repeat(60) { Box(Modifier.fillMaxWidth().height(60.dp)) } }
                if (pinned) TabScreen(trailing = null, scroll = scroll, title = { Text("Pinned") }, content = rows)
                else TabScreen("Folding", scroll = scroll, content = rows)
            }
        }
        compose.waitForIdle()
        val px = compose.density.density
        runBlocking { scroll.scrollTo((600 * px).toInt()) }
        compose.mainClock.autoAdvance = false
        compose.onRoot().performTouchInput { swipe(Offset(180 * px, 600 * px), Offset(180 * px, 450 * px), durationMillis = 200) }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { assertTrue("the fling started", scroll.isScrollInProgress) }
        compose.onRoot().performTouchInput { down(Offset(180 * px, yDp * px)); moveTo(Offset(180 * px, (yDp + 60) * px), delayMillis = 16) }
        compose.mainClock.advanceTimeBy(16)
        val held = scroll.value
        compose.mainClock.advanceTimeBy(128)
        val after = scroll.value
        compose.onRoot().performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals("the content moved under a finger that holds still at $yDp dp", held, after)
    }

    @Test fun `a finger on the content stops a fling (control)`() = holdStopsFling(pinned = false, yDp = 400f)
    @Test fun `a finger on the folding bar stops a fling`() = holdStopsFling(pinned = false, yDp = 35f)
    @Test fun `a finger on the pinned bar stops a fling`() = holdStopsFling(pinned = true, yDp = 35f)
}
