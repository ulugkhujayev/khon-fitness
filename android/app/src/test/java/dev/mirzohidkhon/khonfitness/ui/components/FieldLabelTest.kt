package dev.mirzohidkhon.khonfitness.ui.components

import android.app.Application
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * UI06 P5 row 6: a [FieldRow] label wraps onto more lines between words and is never cut or ended with an ellipsis, at
 * 1.0 (the 104 dp label column) and 1.6 (the label on its own line). Native graphics: text is measured as on a phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FieldLabelTest {
    @get:Rule val compose = createComposeRule()
    private val labels = listOf("Avg heart rate", "Pool length", "Preparation before each round")

    private fun open(scale: Float) {
        compose.setContent {
            val d = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale; screenWidthDp = 360 }
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
                KhonTheme {
                    GroupedList {
                        labels.forEachIndexed { i, l -> FieldRow(l, divider = i > 0) { NumberField(150.0, {}, 1.0, "bpm", width = 176.dp, decimals = 0, height = 38) } }
                    }
                }
            }
        }
    }

    private fun layout(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text).assertIsDisplayed().fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!(results)
        return results.single()
    }

    private fun assertWhole(scale: Float) {
        open(scale)
        for (l in labels) {
            val r = layout(l)
            assertEquals(l, r.layoutInput.text.text)
            // Cut: lines beyond the height it got, or a line wider than its box. (hasVisualOverflow also flags a label
            // narrower than its column, as its paragraph takes the column's width.)
            assertFalse("$l is cut at $scale", r.didOverflowHeight)
            for (line in 0 until r.lineCount) assertTrue("$l line ${line + 1} is wider than its box at $scale", r.getLineRight(line) <= r.size.width + 0.5f)
            assertFalse("$l ends in an ellipsis at $scale", (0 until r.lineCount).any { r.isLineEllipsized(it) })
            // A line breaks only at a space, never inside a word.
            for (line in 0 until r.lineCount - 1) assertEquals("$l line ${line + 1} at $scale", ' ', l[r.getLineEnd(line) - 1])
        }
        // The longest label needs a second line in the 104 dp column, and gets it.
        if (scale == 1f) assertTrue(layout(labels.last()).lineCount >= 2)
    }

    @Test fun `labels are whole at font scale 1_0`() = assertWhole(1f)

    @Test fun `labels are whole at font scale 1_6`() = assertWhole(1.6f)
}
