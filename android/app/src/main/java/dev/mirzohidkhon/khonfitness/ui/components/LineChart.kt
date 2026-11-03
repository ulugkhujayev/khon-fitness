package dev.mirzohidkhon.khonfitness.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.ui.theme.Inter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** A chart point; a [mark]ed one (a record) draws green. */
data class ChartPoint(val date: LocalDate, val value: Double, val mark: Boolean = false)

private fun niceStep(span: Double, target: Int): Double {
    val raw = span / target.coerceAtLeast(1)
    val steps = doubleArrayOf(0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 25.0, 50.0, 100.0)
    return steps.firstOrNull { it >= raw } ?: steps.last()
}

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

/** Every chart is this tall, Today's bodyweight and History's alike. */
private const val CHART_HEIGHT = 190
/** Room between the value axis labels and the plot. */
private const val AXIS_GAP = 8

/**
 * Line chart: dashed grid at round values labelled on the left, a marker on every point, value labels, the last
 * point filled in accent with its unit, dates along the bottom. The value axis is as wide as its widest label plus
 * 8 dp and the right inset is 8 dp, so the chart sits even between the screen margins.
 */
@Composable
fun LineChart(points: List<ChartPoint>, unit: String, modifier: Modifier = Modifier, decimals: Int = 1, format: ((Double) -> String)? = null) {
    val measurer = rememberTextMeasurer()
    // Axis and date labels in secondary label at Caption 1: tertiary at 11 was too faint to read on black.
    val tick = TextStyle(fontFamily = Inter, color = K.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val valueStyle = TextStyle(fontFamily = Inter, color = K.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val lastStyle = TextStyle(fontFamily = Inter, color = K.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val emptyStyle = TextStyle(fontFamily = Inter, color = K.Dim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    Canvas(modifier.fillMaxWidth().height(CHART_HEIGHT.dp)) {
        if (points.isEmpty()) {
            val t = measurer.measure("No data yet", emptyStyle)
            drawText(t, topLeft = Offset((size.width - t.size.width) / 2, (size.height - t.size.height) / 2))
            return@Canvas
        }
        var vmin = points.minOf { it.value }; var vmax = points.maxOf { it.value }
        if (vmin == vmax) { vmin -= 1; vmax += 1 }
        val step = niceStep(vmax - vmin, 3)
        val yMin = floor(vmin / step) * step; val yMax = ceil(vmax / step) * step
        val ticks = generateSequence(yMin) { it + step }.takeWhile { it <= yMax + step / 1000 }.toList()
        val tickText = ticks.map { measurer.measure(format?.invoke(it) ?: fmt(it, if (step < 1) 1 else 0), tick) }
        val padL = axisWidth(tickText.map { it.size.width.toFloat() }, AXIS_GAP.dp.toPx()); val padR = 8.dp.toPx(); val padT = 22.dp.toPx(); val padB = 24.dp.toPx()
        val plotW = size.width - padL - padR; val plotH = size.height - padT - padB
        fun yFor(v: Double) = padT + ((yMax - v) * plotH / (yMax - yMin)).toFloat()
        val n = points.size
        fun xFor(i: Int) = if (n == 1) padL + plotW / 2 else padL + i * plotW / (n - 1)
        ticks.forEachIndexed { k, v ->
            val y = yFor(v)
            drawLine(K.Divider, Offset(padL, y), Offset(size.width - padR, y), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
            val t = tickText[k]
            drawText(t, topLeft = Offset(padL - AXIS_GAP.dp.toPx() - t.size.width, y - t.size.height / 2))
        }
        val xs = points.indices.map { xFor(it) }; val ys = points.map { yFor(it.value) }
        val line = Path().apply { moveTo(xs[0], ys[0]); for (i in 1 until n) lineTo(xs[i], ys[i]) }
        val area = Path().apply { addPath(line); lineTo(xs[n - 1], padT + plotH); lineTo(xs[0], padT + plotH); close() }
        drawPath(area, K.Accent.copy(alpha = 0.13f))
        drawPath(line, K.Accent, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val labelAll = n <= 7
        val maxIdx = points.indices.maxByOrNull { points[it].value } ?: 0
        for (i in 0 until n) {
            val last = i == n - 1
            val r = (if (last) 4.5f else 3.5f).dp.toPx()
            drawCircle(K.Bg, r, Offset(xs[i], ys[i]))
            drawCircle(if (points[i].mark) K.Green else K.Accent, r, Offset(xs[i], ys[i]), style = if (last) androidx.compose.ui.graphics.drawscope.Fill else Stroke(2.dp.toPx()))
            if (labelAll || last || i == maxIdx) {
                val label = (format?.invoke(points[i].value) ?: fmt(points[i].value, decimals)) + if (last && unit.isNotEmpty()) " $unit" else ""
                val t = measurer.measure(label, if (last) lastStyle else valueStyle)
                val x = when { i == 0 && n > 1 -> xs[i]; last && n > 1 -> xs[i] - t.size.width; else -> xs[i] - t.size.width / 2 }
                drawText(t, topLeft = Offset(x.coerceIn(0f, size.width - t.size.width), ys[i] - r - t.size.height - 2.dp.toPx()))
            }
        }
        val maxLabels = if (n == 1) 1 else (plotW / 70.dp.toPx()).toInt().coerceIn(2, 4)
        val idx = if (n == 1) listOf(0) else (0 until maxLabels).map { k -> (k * (n - 1).toDouble() / (maxLabels - 1)).roundToInt() }.distinct()
        idx.forEach { i ->
            val t = measurer.measure(points[i].date.format(dateFmt), tick)
            val x = when { i == 0 && n > 1 -> xs[i]; i == n - 1 && n > 1 -> xs[i] - t.size.width; else -> xs[i] - t.size.width / 2 }
            drawText(t, topLeft = Offset(x, size.height - t.size.height))
        }
    }
}
