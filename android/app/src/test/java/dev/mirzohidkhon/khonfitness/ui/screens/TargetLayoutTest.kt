package dev.mirzohidkhon.khonfitness.ui.screens

import android.app.Application
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.mirzohidkhon.khonfitness.ui.theme.K
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
import java.math.BigDecimal
import java.time.LocalDate

/** The longest result texts next to "At +12.5 kg/week" fit their row with no clipped or overlapping text. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h1000dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TargetLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    private val today = LocalDate.of(2026, 11, 27)
    private val label = "At +12.5 kg⁠/⁠week"

    private fun layout(n: SemanticsNode): TextLayoutResult {
        val out = mutableListOf<TextLayoutResult>()
        n.config[SemanticsActions.GetTextLayoutResult].action?.invoke(out)
        return out.single()
    }

    private fun clipped(t: TextLayoutResult) = t.didOverflowHeight || t.isLineEllipsized(t.lineCount - 1) ||
        (0 until t.lineCount).any { t.getLineRight(it) > t.size.width + 0.5f }

    private fun inside(inner: Rect, outer: Rect) =
        inner.left >= outer.left - 0.5f && inner.right <= outer.right + 0.5f && inner.top >= outer.top - 0.5f && inner.bottom <= outer.bottom + 0.5f

    private fun check(scale: Float) {
        var forecast by mutableStateOf<Forecast>(Forecast.Beyond("+12.5"))
        compose.setContent {
            val d = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale }
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
                KhonTheme {
                    Surface(Modifier.fillMaxSize(), color = K.Bg, contentColor = K.Text) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            TargetRowsGroup(targetRows(BigDecimal("400"), TargetKind.LIFT_KG, forecast, today), TargetKind.LIFT_KG) {}
                        }
                    }
                }
            }
        }
        for ((f, value) in listOf(Forecast.Beyond("+12.5") to "More than 2 years", Forecast.OnDate("+12.5", LocalDate.of(2027, 3, 14)) to "Mar 14, 2027")) {
            compose.runOnIdle { forecast = f }
            compose.waitForIdle()
            val row = compose.onAllNodesWithContentDescription("At plus 12.5 kilograms per week, ", substring = true).fetchSemanticsNodes().single()
            val labels = compose.onAllNodesWithText(label, useUnmergedTree = true).fetchSemanticsNodes()
            val values = compose.onAllNodesWithText(value, useUnmergedTree = true).fetchSemanticsNodes()
            assertEquals(value, 1, labels.size)
            assertEquals(value, 1, values.size)
            val l = labels.single()
            val v = values.single()
            val lt = layout(l)
            val vt = layout(v)
            assertFalse("$value: label clipped", clipped(lt))
            assertTrue("$value: label wraps more than once", lt.lineCount <= 2)
            for (line in 0 until lt.lineCount) {
                val text = lt.layoutInput.text.text.substring(lt.getLineStart(line), lt.getLineEnd(line)).trim().trimStart('⁠')
                assertFalse("$value: a line breaks inside kg/week: $text", text.startsWith("/") || text.startsWith("week"))
            }
            assertFalse("$value: value clipped", clipped(vt))
            assertEquals("$value: value wraps", 1, vt.lineCount)
            assertTrue("$value: value is cut", vt.multiParagraph.maxIntrinsicWidth <= vt.size.width + 0.5f)
            assertTrue("$value: label runs into the value", l.boundsInRoot.right <= v.boundsInRoot.left + 0.5f)
            assertTrue("$value: label outside its row", inside(l.boundsInRoot, row.boundsInRoot))
            assertTrue("$value: value outside its row", inside(v.boundsInRoot, row.boundsInRoot))
            assertTrue("$value: the row is off the screen", row.boundsInRoot.right <= compose.activity.window.decorView.width + 0.5f)
        }
        val target = compose.onAllNodesWithText("400 kg", useUnmergedTree = true).fetchSemanticsNodes().single()
        val name = compose.onAllNodesWithText("Target", useUnmergedTree = true).fetchSemanticsNodes().single()
        assertFalse(clipped(layout(target)))
        assertEquals(1, layout(target).lineCount)
        assertTrue(layout(target).multiParagraph.maxIntrinsicWidth <= layout(target).size.width + 0.5f)
        assertTrue(name.boundsInRoot.right <= target.boundsInRoot.left + 0.5f)
    }

    @Test fun at360() = check(1f)
    @Test fun at360Large() = check(1.3f)
    @Test @Config(qualifiers = "w411dp-h1000dp-xxhdpi") fun at411() = check(1f)
    @Test @Config(qualifiers = "w411dp-h1000dp-xxhdpi") fun at411Large() = check(1.3f)
}
