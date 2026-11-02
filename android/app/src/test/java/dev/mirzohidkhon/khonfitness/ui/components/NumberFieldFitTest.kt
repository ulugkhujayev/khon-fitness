package dev.mirzohidkhon.khonfitness.ui.components

import android.app.Application
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.mirzohidkhon.khonfitness.ui.screens.NumberRow
import dev.mirzohidkhon.khonfitness.ui.screens.SetFields
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every [NumberField] caller on a 360 dp phone at font scales 1.0, 1.3 and 1.6, filled with the longest value the
 * caller allows and once empty: the value, unit and placeholder fit on one line inside their slot, and each step
 * button keeps its width (44 dp; 36 dp in the compact two-per-row mode of a session set).
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NumberFieldFitTest {
    @get:Rule val compose = createComposeRule()

    /** One field of a caller: what it shows, and which of the rendered fields it is ([index], top to bottom). */
    private class Case(
        val name: String, val value: Double, val unit: String, val step: String, val placeholder: String? = null,
        val index: Int = 0, val compactAllowed: Boolean = false, val content: @Composable (Double?) -> Unit,
    )

    private fun row(name: String, value: Double, unit: String, step: Double, decimals: Int, stepText: String, min: Double = 0.0, max: Double = Double.POSITIVE_INFINITY) =
        Case(name, value, unit, stepText) { v -> NumberRow(name, v, unit, step, decimals, true, min, max) {} }

    private fun inRow(label: String, value: Double, unit: String, step: Double, stepText: String, placeholder: String? = null, note: String? = null, min: Double = 0.0, max: Double = Double.POSITIVE_INFINITY, elevated: Boolean = false) =
        Case(label, value, unit, stepText, placeholder) { v -> FieldRow(label, note = note) { Field(v, step, unit, 176.dp, 0, 38, placeholder, min, max, elevated) } }

    private fun interval(index: Int, value: Double, unit: String, stepText: String) = Case("Round 12 $unit", value, unit, stepText, index = index) { v ->
        FieldRow("Round 12") {
            Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.End) {
                Field(if (index == 0) v else 999.0, 1.0, "bpm", 176.dp, 0, 38)
                Spacer(Modifier.height(6.dp))
                Field(if (index == 1) v else 9999.0, 5.0, "W", 176.dp, 0, 38)
            }
        }
    }

    /** A session set (SetRow): weight and reps in the set's padding, inside the list's 16 dp. */
    private fun set(index: Int, value: Double, unit: String, stepText: String, placeholder: String) = Case("Set $unit", value, unit, stepText, placeholder, index, compactAllowed = true) { v ->
        Column(Modifier.padding(start = 10.dp, end = 16.dp)) {
            SetFields(
                weight = { m -> Column(m) { Field(if (index == 0) v else 102.25, 1.25, "kg", null, 2, 44, if (index == 0) placeholder else "102.25") } },
                reps = { m -> Column(m) { Field(if (index == 1) v else 100.0, 1.0, "reps", null, 0, 44, if (index == 1) placeholder else "12") } },
            )
        }
    }

    private val cases = listOf(
        // Cardio (CardioScreen.kt): top fields, intervals, effort.
        row("Time", 999.9, "min", 1.0, 1, "1 min"),
        row("Distance", 999.9, "km", 0.1, 1, "0.1 km"),
        row("Avg heart rate", 999.0, "bpm", 1.0, 0, "1 bpm"),
        row("Avg watts", 9999.0, "W", 5.0, 0, "5 W"),
        row("Laps", 999.0, "", 1.0, 0, "1"),
        row("Pool length", 999.0, "m", 5.0, 0, "5 m"),
        interval(0, 999.0, "bpm", "1 bpm"),
        interval(1, 9999.0, "W", "5 W"),
        inRow("Effort", 10.0, "/10", 1.0, "1 /10", placeholder = "1–10", min = 1.0, max = 10.0),
        // Session (SessionScreen.kt): the summary sheet's effort, a set's weight and reps.
        inRow("Effort", 10.0, "/10", 1.0, "1 /10", placeholder = "1–10", min = 1.0, max = 10.0, elevated = true),
        set(0, 102.25, "kg", "1.25 kg", "102.25"),
        set(1, 100.0, "reps", "1 reps", "12"),
        // Eating window (WindowSheet.kt).
        inRow("Remind before", 120.0, "min", 5.0, "5 min", max = 120.0, elevated = true),
        // Stretching (StretchEditors.kt): a routine's seconds, a stretch's rep count and hold.
        inRow("Hip flexor stretch", 600.0, "s", 5.0, "5 s", note = "per side", min = 5.0, max = 600.0),
        row("Rep count", 50.0, "", 1.0, 0, "1", 1.0, 50.0),
        row("Hold", 600.0, "s", 5.0, 0, "5 s", 5.0, 600.0),
        // Exercise and program editors (Editors.kt).
        row("Max reps", 50.0, "", 1.0, 0, "1", 1.0, 50.0),
        row("Weight step", 99.5, "kg", 0.5, 1, "0.5 kg"),
        row("Warm-up", 999.0, "min", 1.0, 0, "1 min"),
        row("Rest", 9999.0, "s", 10.0, 0, "10 s"),
    )

    @Test fun fitsAtScale1_0() = check(1.0f)
    @Test fun fitsAtScale1_3() = check(1.3f)
    @Test fun fitsAtScale1_6() = check(1.6f)

    private fun check(scale: Float) {
        var shown by mutableStateOf(cases[0] to cases[0].value as Double?)
        compose.setContent {
            val d = LocalDensity.current
            val config = Configuration(LocalConfiguration.current).apply { fontScale = scale; screenWidthDp = 360 }
            CompositionLocalProvider(LocalDensity provides Density(d.density, scale), LocalConfiguration provides config) {
                KhonTheme {
                    Column(Modifier.width(360.dp).padding(horizontal = 16.dp)) {
                        val (case, value) = shown
                        key(case, value) { GroupedList { case.content(value) } }
                    }
                }
            }
        }
        val problems = mutableListOf<String>()
        for (case in cases) for (value in listOf(case.value, null)) {
            compose.runOnIdle { shown = case to value }
            compose.waitForIdle()
            problems += measure(case, value).map { "x$scale ${case.name} ${if (value == null) "empty" else "full"}: $it" }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private fun measure(case: Case, value: Double?): List<String> {
        val out = mutableListOf<String>()
        val px = compose.density.density
        val field = compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes()[case.index]
        if (value != null) out += fits("value", field)
        val buttons = listOf("Decrease", "Increase").map { compose.onNodeWithContentDescription("$it by ${case.step}").fetchSemanticsNode() }
        val compact = buttons[0].size.width < 44 * px - 0.5f
        if (compact && !case.compactAllowed) out += "unit dropped: the field went compact"
        val minButton = if (compact) 36 else 44
        buttons.forEach { if (it.size.width < minButton * px - 0.5f) out += "step button ${it.size.width / px} dp wide" }
        if (case.unit.isNotEmpty() && !compact) out += fits("unit", text(case.unit))
        if (value == null && case.placeholder != null) out += fits("placeholder", text(case.placeholder))
        return out
    }

    private fun text(s: String): SemanticsNode = compose.onAllNodesWithText(s, useUnmergedTree = true).fetchSemanticsNodes().first()

    /**
     * The text fits its node on one line: its measured width is no wider than the node, and laid out again inside the
     * node's width, one line at most, it reports no visual overflow. (The layout that semantics hands back is not the
     * drawn one: a text field's lies in an endless scroller, a Text's spans the parent's room.)
     */
    private fun fits(what: String, node: SemanticsNode): List<String> {
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        val input = results.single().layoutInput
        val measurer = TextMeasurer(input.fontFamilyResolver, input.density, input.layoutDirection)
        val style = input.style.copy(textAlign = TextAlign.Start)
        val width = measurer.measure(input.text, style).size.width
        val inSlot = measurer.measure(input.text, style, maxLines = 1, constraints = Constraints(maxWidth = node.size.width))
        return listOfNotNull(
            "$what \"${input.text}\" is $width px in a ${node.size.width} px slot".takeIf { width > node.size.width },
            "$what \"${input.text}\" overflows its slot".takeIf { inSlot.hasVisualOverflow },
        )
    }

    /** The field as the callers build it; [width] null fills the slot (session sets). */
    @Composable
    private fun Field(value: Double?, step: Double, unit: String, width: Dp?, decimals: Int, height: Int, placeholder: String? = null, min: Double = 0.0, max: Double = Double.POSITIVE_INFINITY, elevated: Boolean = false) =
        NumberField(value, {}, step, unit, width = width, decimals = decimals, height = height, placeholder = placeholder, min = min, max = max, elevated = elevated)
}
