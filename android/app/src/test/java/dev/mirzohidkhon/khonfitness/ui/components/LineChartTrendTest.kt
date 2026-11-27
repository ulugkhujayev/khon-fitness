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
import androidx.compose.ui.graphics.Color
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
import java.time.LocalDate
import kotlin.math.abs

/**
 * The bodyweight chart drawn with and without its trend line: the main line runs through every column between its
 * markers, the trend draws as a muted line in the plot, and the value scale covers both series.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LineChartTrendTest {
    @get:Rule val compose = createComposeRule()

    private val start = LocalDate.of(2026, 10, 1)
    private val main = listOf(83.0, 83.6, 83.2, 83.8, 83.4, 84.0, 83.3, 83.7, 83.1, 83.5).mapIndexed { i, v -> ChartPoint(start.plusDays(i.toLong()), v) }
    private fun flat(value: Double) = main.map { ChartPoint(it.date, value) }

    private var trend by mutableStateOf<List<ChartPoint>?>(null)
    private lateinit var view: android.view.View
    private var shown = false

    /** The chart called without a trend ([with] null) or with [with]; one content, its trend swapped per image. */
    private fun draw(with: List<ChartPoint>?): Bitmap {
        if (!shown) {
            shown = true
            compose.setContent {
                view = LocalView.current
                KhonTheme {
                    Box(Modifier.width(360.dp).background(K.Bg).testTag("chart")) {
                        val t = trend
                        if (t == null) LineChart(main, "kg") else LineChart(main, "kg", trend = t)
                    }
                }
            }
        }
        compose.runOnIdle { trend = with }
        return compose.nodeImage(view, compose.onNodeWithTag("chart"))
    }

    private fun near(p: Int, c: Color, tol: Int = 24): Boolean {
        val a = c.toArgb()
        return abs(android.graphics.Color.red(p) - android.graphics.Color.red(a)) <= tol &&
            abs(android.graphics.Color.green(p) - android.graphics.Color.green(a)) <= tol &&
            abs(android.graphics.Color.blue(p) - android.graphics.Color.blue(a)) <= tol
    }

    /** The middle columns of the plot, clear of the axis labels on the left and the last value label on the right. */
    private fun band(bmp: Bitmap) = (bmp.width * 35 / 100) until (bmp.width * 60 / 100)

    private fun count(bmp: Bitmap, c: Color, columns: IntRange = band(bmp)): Int {
        var n = 0
        for (y in 0 until bmp.height) for (x in columns) if (near(bmp.getPixel(x, y), c)) n++
        return n
    }

    /**
     * The fewest accent pixels in one column of the band. The band holds some three marker spans of 35 dp, and a marker
     * is 9 dp wide at most, so most of its columns cross only the line between two markers.
     */
    private fun thinnestLine(bmp: Bitmap): Int = band(bmp).minOf { x -> (0 until bmp.height).count { y -> near(bmp.getPixel(x, y), K.Accent) } }

    private fun assertMainLine(name: String, bmp: Bitmap) {
        val floor = compose.density.density.toInt()
        assertTrue("$name: a band column with ${thinnestLine(bmp)} accent pixels, under the main line's $floor", thinnestLine(bmp) >= floor)
    }

    /** The top row of [c] pixels in the band; the bitmap height when there is none. */
    private fun top(bmp: Bitmap, c: Color): Int {
        for (y in 0 until bmp.height) for (x in band(bmp)) if (near(bmp.getPixel(x, y), c)) return y
        return bmp.height
    }

    @Test fun `the trend draws as a muted line on the same scale, and a trend outside the points widens the scale`() {
        val plain = draw(null)
        val inside = draw(flat(83.5))
        val above = draw(flat(90.0))
        val bandWidth = band(plain).count()
        assertMainLine("without a trend", plain)
        assertMainLine("with a trend inside the points", inside)
        assertMainLine("with a trend above the points", above)
        // A 1.5 dp line across the band, at 3 px a dp, is some 4 px tall in every column; half of that is a safe floor.
        assertTrue("trend inside: ${count(inside, K.Muted)} muted vs ${count(plain, K.Muted)} without", count(inside, K.Muted) - count(plain, K.Muted) >= bandWidth * 2)
        assertEquals("a trend inside the points keeps the scale", top(plain, K.Accent), top(inside, K.Accent))
        val px = compose.density.density
        assertTrue("a trend above the points pushes them down: ${top(plain, K.Accent)} -> ${top(above, K.Accent)}", top(above, K.Accent) - top(plain, K.Accent) > 40 * px)
        assertTrue("the trend above is drawn above the points", top(above, K.Muted) < top(above, K.Accent))
        assertTrue("the trend above is drawn: ${count(above, K.Muted)} muted", count(above, K.Muted) - count(plain, K.Muted) >= bandWidth * 2)
        val empty = draw(emptyList())
        assertMainLine("with an empty trend", empty)
        val all = 0 until plain.width
        assertEquals("an empty trend draws the accent of a chart called without one", count(plain, K.Accent, all), count(empty, K.Accent, all))
        assertEquals("an empty trend draws the muted of a chart called without one", count(plain, K.Muted, all), count(empty, K.Muted, all))
        assertTrue("the accent count compares a drawn line: ${count(plain, K.Accent, all)}", count(plain, K.Accent, all) >= bandWidth * 2)
    }
}
