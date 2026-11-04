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
import androidx.compose.ui.unit.dp
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI07 verify 1: a tab screen's title bar takes the taps on its background, so a tap there never reaches a row that
 * scrolled under it. Both bars: the folding one (Today, Plan) and the pinned one (History). The bar's own controls
 * still take their taps.
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
}
