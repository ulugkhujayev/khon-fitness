package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
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

/** The Review row with the longest heart rate line fits on one line next to its title, at 360 and 411 dp, font 1.0 and 1.3. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReviewEntryFitTest {
    @get:Rule val compose = createComposeRule()
    private val longest = "avg 188 · max 201 bpm"

    private fun layout(text: String): TextLayoutResult {
        val out = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(out) }
        return out.single()
    }

    private fun fits(scale: Float) {
        compose.setContent {
            val d = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
                KhonTheme { ReviewEntry(longest, "average 188, maximum 201 beats per minute", Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {} }
            }
        }
        compose.waitForIdle()
        val value = layout(longest)
        val title = layout("Review")
        val valueBox = compose.onNodeWithText(longest, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val titleBox = compose.onNodeWithText("Review", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val seen = "at $scale: title ${title.size} lines ${title.lineCount} $titleBox, value ${value.size} lines ${value.lineCount} cut ${value.isLineEllipsized(0)} $valueBox"
        assertEquals(seen, 1, title.lineCount)
        assertEquals(seen, 1, value.lineCount)
        assertFalse(seen, value.isLineEllipsized(0) || title.isLineEllipsized(0))
        assertFalse(seen, titleBox.overlaps(valueBox))
    }

    @Test fun at360() { fits(1f) }
    @Test fun at360Large() { fits(1.3f) }
    @Config(qualifiers = "w411dp-h900dp-xxhdpi") @Test fun at411() { fits(1f) }
    @Config(qualifiers = "w411dp-h900dp-xxhdpi") @Test fun at411Large() { fits(1.3f) }
}
