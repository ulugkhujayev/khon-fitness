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
import java.io.File
import java.time.LocalDate
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LineChartChangesTest {
    @get:Rule val compose = createComposeRule()

    private val start = LocalDate.of(2026, 10, 1)
    private val points = listOf(80.0, 82.5, 82.5, 85.0, 85.0, 87.5, 85.0, 90.0, 90.0, 92.5).mapIndexed { i, v -> ChartPoint(start.plusDays(2L * i), v) }

    private var shown by mutableStateOf(points)
    private lateinit var view: android.view.View
    private var composed = false

    private fun draw(with: List<ChartPoint>): Bitmap {
        if (!composed) {
            composed = true
            compose.setContent {
                view = LocalView.current
                KhonTheme { Box(Modifier.width(360.dp).background(K.Bg).testTag("chart")) { LineChart(shown, "kg") } }
            }
        }
        compose.runOnIdle { shown = with }
        return compose.nodeImage(view, compose.onNodeWithTag("chart"))
    }

    private fun near(p: Int, c: Color): Boolean {
        val a = c.toArgb()
        return abs(android.graphics.Color.red(p) - android.graphics.Color.red(a)) <= 24 &&
            abs(android.graphics.Color.green(p) - android.graphics.Color.green(a)) <= 24 &&
            abs(android.graphics.Color.blue(p) - android.graphics.Color.blue(a)) <= 24
    }

    private val plotTopDp = 22
    private val plotHeightDp = 144

    private fun verticalLines(bmp: Bitmap): List<Float> {
        val px = compose.density.density
        val top = (plotTopDp * px).toInt(); val bottom = ((plotTopDp + plotHeightDp) * px).toInt()
        val columns = (0 until bmp.width).filter { x -> (top until bottom).count { y -> near(bmp.getPixel(x, y), K.Muted) } >= (bottom - top) * 6 / 10 }
        val runs = mutableListOf<MutableList<Int>>()
        for (x in columns) if (runs.lastOrNull()?.last() == x - 1) runs.last() += x else runs += mutableListOf(x)
        return runs.map { it.average().toFloat() }
    }

    private fun greenCenter(bmp: Bitmap): Float {
        val xs = (0 until bmp.width).flatMap { x -> (0 until bmp.height).filter { y -> near(bmp.getPixel(x, y), K.Green) }.map { x } }
        assertTrue("a green marker is drawn", xs.isNotEmpty())
        return xs.average().toFloat()
    }

    private fun column(i: Int): Float = greenCenter(draw(points.mapIndexed { k, p -> if (k == i) p.copy(mark = true) else p }))

    @Test fun `a chart without a flagged point draws no line, and each flagged point draws one line at its own column`() {
        val plain = draw(points)
        File("build/shots-setup").takeIf { it.isDirectory }?.let { File(it, "chart-plain.png").outputStream().use { o -> plain.compress(Bitmap.CompressFormat.PNG, 100, o) } }
        assertEquals("no vertical line without flags", emptyList<Float>(), verticalLines(plain))

        val at3 = column(3); val at7 = column(7)
        val lines = verticalLines(draw(points.mapIndexed { k, p -> if (k == 3 || k == 7) p.copy(change = true) else p }))
        assertEquals("one line per flagged point: $lines", 2, lines.size)
        assertEquals("the line of point 3 stands at its marker", at3, lines[0], 1f)
        assertEquals("the line of point 7 stands at its marker", at7, lines[1], 1f)
    }
}
