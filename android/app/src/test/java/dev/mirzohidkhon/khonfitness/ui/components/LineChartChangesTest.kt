package dev.mirzohidkhon.khonfitness.ui.components

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import dev.mirzohidkhon.khonfitness.ui.screens.nodeImage
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.ui.theme.KhonTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import kotlin.math.abs

/** The exercise chart's setup changes: a thin muted vertical line at the point of each change, and nothing without them. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LineChartChangesTest {
    @get:Rule val compose = createComposeRule()

    private val start = LocalDate.of(2026, 10, 1)
    private val points = listOf(80.0, 82.5, 82.5, 85.0, 85.0, 87.5, 85.0, 90.0, 90.0, 92.5).mapIndexed { i, v -> ChartPoint(start.plusDays(2L * i), v, mark = i == 9) }

    private var changes by mutableStateOf<List<LocalDate>?>(null)
    private lateinit var view: android.view.View
    private var shown = false

    /** The chart called without the list ([with] null) or with [with]. */
    private fun draw(with: List<LocalDate>?): Bitmap {
        if (!shown) {
            shown = true
            compose.setContent {
                view = LocalView.current
                KhonTheme {
                    Box(Modifier.width(360.dp).background(K.Bg).testTag("chart")) {
                        val c = changes
                        if (c == null) LineChart(points, "kg") else LineChart(points, "kg", changes = c)
                    }
                }
            }
        }
        compose.runOnIdle { changes = with }
        return compose.nodeImage(view, compose.onNodeWithTag("chart"))
    }

    private fun muted(p: Int): Boolean {
        val a = K.Muted.toArgb()
        return abs(android.graphics.Color.red(p) - android.graphics.Color.red(a)) <= 24 &&
            abs(android.graphics.Color.green(p) - android.graphics.Color.green(a)) <= 24 &&
            abs(android.graphics.Color.blue(p) - android.graphics.Color.blue(a)) <= 24
    }

    /** The centers of the column runs that are muted over most of the plot's height (22 dp from the top, 144 dp tall). */
    private fun verticalLines(bmp: Bitmap): List<Float> {
        val px = compose.density.density
        val top = (22 * px).toInt(); val bottom = ((22 + 144) * px).toInt()
        val columns = (0 until bmp.width).filter { x -> (top until bottom).count { y -> muted(bmp.getPixel(x, y)) } >= (bottom - top) * 6 / 10 }
        val runs = mutableListOf<MutableList<Int>>()
        for (x in columns) if (runs.lastOrNull()?.last() == x - 1) runs.last() += x else runs += mutableListOf(x)
        return runs.map { it.average().toFloat() }
    }

    @Test fun `a chart without the list draws as before, and each change draws one line at its point`() {
        val plain = draw(null)
        File("build/shots-setup").takeIf { it.isDirectory }?.let { File(it, "chart-plain.png").outputStream().use { o -> plain.compress(Bitmap.CompressFormat.PNG, 100, o) } }
        assertTrue("an empty list draws as no list", plain.sameAs(draw(emptyList())))
        assertEquals("no vertical line without changes", emptyList<Float>(), verticalLines(plain))

        val two = draw(listOf(points[3].date, points[7].date))
        val lines = verticalLines(two)
        assertEquals("one line per change: $lines", 2, lines.size)
        // Points sit evenly from the axis to 8 dp before the right edge: the two lines must fit points 3 and 7 of 10.
        val step = (lines[1] - lines[0]) / 4
        val right = two.width - 8 * compose.density.density
        assertEquals("the line at point 7 is two steps before the last point", right, lines[1] + 2 * step, 3f)
        assertTrue("a step is a ninth of a plot wider than half the chart: $step", step * 9 > two.width / 2)
    }

    @Test fun `a change lands on the first point on or after its date, once`() {
        assertEquals(listOf(2), changeAt(points, listOf(points[2].date)))
        assertEquals(listOf(3), changeAt(points, listOf(points[2].date.plusDays(1))))
        assertEquals(emptyList<Int>(), changeAt(points, listOf(points.last().date.plusDays(1))))
        assertEquals(listOf(3), changeAt(points, listOf(points[3].date, points[2].date.plusDays(1))))
    }
}
