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

data class ChartPoint(val date: LocalDate, val value: Double)

private fun niceStep(span: Double, target: Int): Double {
    val raw = span / target.coerceAtLeast(1)
    val steps = doubleArrayOf(0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 25.0, 50.0, 100.0)
    return steps.firstOrNull { it >= raw } ?: steps.last()
}

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

/** Line chart: dashed grid at round values, marker on every point, value labels, dates along the bottom. */
@Composable
fun LineChart(points: List<ChartPoint>, unit: String, modifier: Modifier = Modifier, height: Int = 180, decimals: Int = 1, format: ((Double) -> String)? = null) {
    val measurer = rememberTextMeasurer()
    val tick = TextStyle(fontFamily = Inter, color = K.Dim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    val valueStyle = TextStyle(fontFamily = Inter, color = K.Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    val lastStyle = TextStyle(fontFamily = Inter, color = K.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Canvas(modifier.fillMaxWidth().height(height.dp)) {
        val padL = 40.dp.toPx(); val padR = 14.dp.toPx(); val padT = 22.dp.toPx(); val padB = 24.dp.toPx()
        val plotW = size.width - padL - padR; val plotH = size.height - padT - padB
        if (points.isEmpty()) {
            val t = measurer.measure("No data yet", tick)
            drawText(t, topLeft = Offset((size.width - t.size.width) / 2, (size.height - t.size.height) / 2))
            return@Canvas
        }
        var vmin = points.minOf { it.value }; var vmax = points.maxOf { it.value }
        if (vmin == vmax) { vmin -= 1; vmax += 1 }
        val step = niceStep(vmax - vmin, 3)
        val yMin = floor(vmin / step) * step; val yMax = ceil(vmax / step) * step
        fun yFor(v: Double) = padT + ((yMax - v) * plotH / (yMax - yMin)).toFloat()
        val n = points.size
        fun xFor(i: Int) = if (n == 1) padL + plotW / 2 else padL + i * plotW / (n - 1)
        var v = yMin
        while (v <= yMax + step / 1000) {
            val y = yFor(v)
            drawLine(K.Divider, Offset(padL, y), Offset(size.width - padR, y), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
            val t = measurer.measure(format?.invoke(v) ?: fmt(v, if (step < 1) 1 else 0), tick)
            drawText(t, topLeft = Offset(padL - 8.dp.toPx() - t.size.width, y - t.size.height / 2))
            v += step
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
            drawCircle(K.Accent, r, Offset(xs[i], ys[i]), style = if (last) androidx.compose.ui.graphics.drawscope.Fill else Stroke(2.dp.toPx()))
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
