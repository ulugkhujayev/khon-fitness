package dev.mirzohidkhon.khonfitness.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
data class ChartPoint(val date: LocalDate, val value: Double, val mark: Boolean = false, val change: Boolean = false)

private val SMALL_STEPS = doubleArrayOf(0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 25.0, 50.0, 100.0, 200.0, 250.0, 500.0, 1000.0)
private val STEP_FACTORS = doubleArrayOf(1.0, 2.0, 2.5, 5.0)

/** The smallest step at or above `span / target`: [SMALL_STEPS], then 1, 2, 2.5 and 5 times each higher power of ten. */
internal fun niceStep(span: Double, target: Int): Double {
    val raw = span / target.coerceAtLeast(1)
    SMALL_STEPS.firstOrNull { it >= raw }?.let { return it }
    var power = SMALL_STEPS.last()
    while (power.isFinite()) {
        STEP_FACTORS.map { it * power }.firstOrNull { it >= raw && it.isFinite() }?.let { return it }
        power *= 10
    }
    return Double.MAX_VALUE
}

const val MAX_AXIS_TICKS = 5

data class ValueAxis(val low: Double, val high: Double, val step: Double) {
    val ticks: List<Double> get() {
        val last = floor((high - low) / step + 1.0 / 1000).takeIf { it.isFinite() }?.coerceIn(0.0, MAX_AXIS_TICKS - 1.0)?.toInt() ?: 0
        return (0..last).map { low + it * step }.filter { it.isFinite() }.distinct()
    }
}

fun valueAxis(values: List<Double>, domain: ClosedFloatingPointRange<Double>? = null): ValueAxis {
    var vmin = values.min(); var vmax = values.max()
    if (vmin == vmax) { vmin -= 1; vmax += 1 }
    if (domain != null) {
        if (vmin < domain.start) { vmax = minOf(domain.endInclusive, vmax + domain.start - vmin); vmin = domain.start }
        if (vmax > domain.endInclusive) { vmin = maxOf(domain.start, vmin - (vmax - domain.endInclusive)); vmax = domain.endInclusive }
    }
    val step = niceStep(vmax - vmin, 3)
    var yMin = (floor(vmin / step) * step).coerceAtLeast(-Double.MAX_VALUE); var yMax = (ceil(vmax / step) * step).coerceAtMost(Double.MAX_VALUE)
    if (domain != null) { yMin = yMin.coerceAtLeast(domain.start); yMax = yMax.coerceAtMost(domain.endInclusive) }
    return ValueAxis(yMin, yMax, step)
}

private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

/** Each [trend] point as the index of the point of its date and its value; a trend point without one is left out. */
internal fun trendAt(points: List<ChartPoint>, trend: List<ChartPoint>): List<Pair<Int, Double>> =
    trend.mapNotNull { t -> points.indexOfFirst { it.date == t.date }.takeIf { it >= 0 }?.let { it to t.value } }

/** The lowest and highest value the chart's scale covers: both series. */
internal fun valueBounds(points: List<ChartPoint>, trendAt: List<Pair<Int, Double>>): Pair<Double, Double> {
    val all = points.map { it.value } + trendAt.map { it.second }
    return all.min() to all.max()
}

/**
 * The box of the value label of point [i] of [n], [w] by [h] with its bottom at [bottom]: from the point [x] at the
 * first point, ending at it at the last, else centered on it; kept inside `0..`[chartWidth].
 */
internal fun pointLabelBox(i: Int, n: Int, x: Float, bottom: Float, w: Float, h: Float, chartWidth: Float): Rect {
    val left = when { i == 0 && n > 1 -> x; i == n - 1 && n > 1 -> x - w; else -> x - w / 2 }.coerceIn(0f, chartWidth - w)
    return Rect(left, bottom - h, left + w, bottom)
}

/**
 * The value labels that draw, from their [boxes] by point index: [last] first, then [highest], then the others from the
 * newest to the oldest. A label draws when its box grown by [margin] on each side meets no box that already draws.
 */
internal fun drawnLabels(boxes: Map<Int, Rect>, last: Int, highest: Int, margin: Float): Set<Int> {
    val drawn = linkedMapOf<Int, Rect>()
    for (i in (listOf(last, highest) + boxes.keys.sortedDescending()).distinct()) {
        val box = boxes[i] ?: continue
        val grown = box.inflate(margin)
        if (drawn.values.none { it.overlaps(grown) }) drawn[i] = box
    }
    return drawn.keys
}

/**
 * What one [LineChart] drew, in px of its canvas: the points in drawing order, the value axis label boxes, and the
 * drawn value label boxes by point index.
 */
internal class ChartDrawn(val width: Float, val height: Float, val points: List<ChartPoint>, val ticks: List<Rect>, val labels: Map<Int, Rect>)

internal val LocalChartDrawn = staticCompositionLocalOf<((ChartDrawn) -> Unit)?> { null }

/** Every chart is this tall, Today's bodyweight and History's alike. */
private const val CHART_HEIGHT = 190
/** Room between the value axis labels and the plot. */
private const val AXIS_GAP = 8

/**
 * Line chart: dashed grid at round values labelled on the left, a marker on every point, value labels that do not
 * meet ([drawnLabels]), the last point filled in accent with its unit, dates along the bottom. The value axis is as wide as its widest label plus
 * 8 dp and the right inset is 8 dp, so the chart sits even between the screen margins. A [trend] point sits at the
 * point of its date and draws as a thin muted line without markers, on the same scale. A point with [ChartPoint.change]
 * draws a thin muted vertical line through the plot.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>, unit: String, modifier: Modifier = Modifier, decimals: Int = 1, format: ((Double) -> String)? = null,
    trend: List<ChartPoint> = emptyList(), domain: ClosedFloatingPointRange<Double>? = null,
) {
    val measurer = rememberTextMeasurer()
    // Axis and date labels in secondary label at Caption 1: tertiary at 11 was too faint to read on black.
    val tick = TextStyle(fontFamily = Inter, color = K.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val valueStyle = TextStyle(fontFamily = Inter, color = K.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val lastStyle = TextStyle(fontFamily = Inter, color = K.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val emptyStyle = TextStyle(fontFamily = Inter, color = K.Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    val onDrawn = LocalChartDrawn.current
    val points = points.filter { it.value.isFinite() }
    val trend = trend.filter { it.value.isFinite() }
    Canvas(modifier.fillMaxWidth().height(CHART_HEIGHT.dp)) {
        if (points.isEmpty()) {
            val t = measurer.measure("No data yet", emptyStyle)
            drawText(t, topLeft = Offset((size.width - t.size.width) / 2, (size.height - t.size.height) / 2))
            return@Canvas
        }
        val trendAt = trendAt(points, trend)
        val axis = valueAxis(valueBounds(points, trendAt).toList(), domain)
        val yMin = axis.low; val yMax = axis.high; val step = axis.step; val ticks = axis.ticks
        val tickText = ticks.map { measurer.measure(format?.invoke(it) ?: fmt(it, if (step < 1) 1 else 0), tick) }
        val padL = axisWidth(tickText.map { it.size.width.toFloat() }, AXIS_GAP.dp.toPx()); val padR = 8.dp.toPx(); val padT = 22.dp.toPx(); val padB = 24.dp.toPx()
        val plotW = size.width - padL - padR; val plotH = size.height - padT - padB
        fun yFor(v: Double) = padT + ((yMax - v) / (yMax - yMin) * plotH).toFloat()
        val n = points.size
        fun xFor(i: Int) = if (n == 1) padL + plotW / 2 else padL + i * plotW / (n - 1)
        val tickBoxes = ticks.mapIndexed { k, v ->
            val y = yFor(v)
            drawLine(K.Divider, Offset(padL, y), Offset(size.width - padR, y), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
            val t = tickText[k]
            val topLeft = Offset(padL - AXIS_GAP.dp.toPx() - t.size.width, y - t.size.height / 2)
            drawText(t, topLeft = topLeft)
            Rect(topLeft, Size(t.size.width.toFloat(), t.size.height.toFloat()))
        }
        val xs = points.indices.map { xFor(it) }; val ys = points.map { yFor(it.value) }
        val line = Path().apply { moveTo(xs[0], ys[0]); for (i in 1 until n) lineTo(xs[i], ys[i]) }
        val area = Path().apply { addPath(line); lineTo(xs[n - 1], padT + plotH); lineTo(xs[0], padT + plotH); close() }
        drawPath(area, K.Accent.copy(alpha = 0.13f))
        points.indices.filter { points[it].change }.forEach { i -> drawLine(K.Muted, Offset(xFor(i), padT), Offset(xFor(i), padT + plotH), strokeWidth = 1.dp.toPx()) }
        if (trendAt.size > 1) {
            val t = Path().apply { trendAt.forEachIndexed { k, (i, v) -> if (k == 0) moveTo(xFor(i), yFor(v)) else lineTo(xFor(i), yFor(v)) } }
            drawPath(t, K.Muted, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawPath(line, K.Accent, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val labelAll = n <= 7
        val maxIdx = points.indices.maxByOrNull { points[it].value } ?: 0
        fun radius(i: Int) = (if (i == n - 1) 4.5f else 3.5f).dp.toPx()
        val labels = (0 until n).filter { labelAll || it == n - 1 || it == maxIdx }.associateWith { i ->
            val last = i == n - 1
            val label = (format?.invoke(points[i].value) ?: fmt(points[i].value, decimals)) + if (last && unit.isNotEmpty()) " $unit" else ""
            measurer.measure(label, if (last) lastStyle else valueStyle)
        }
        val boxes = labels.mapValues { (i, t) ->
            pointLabelBox(i, n, xs[i], ys[i] - radius(i) - 2.dp.toPx(), t.size.width.toFloat(), t.size.height.toFloat(), size.width)
        }
        val drawn = drawnLabels(boxes, n - 1, maxIdx, 2.dp.toPx())
        for (i in 0 until n) {
            val r = radius(i)
            drawCircle(K.Bg, r, Offset(xs[i], ys[i]))
            drawCircle(if (points[i].mark) K.Green else K.Accent, r, Offset(xs[i], ys[i]), style = if (i == n - 1) androidx.compose.ui.graphics.drawscope.Fill else Stroke(2.dp.toPx()))
            if (i in drawn) drawText(labels.getValue(i), topLeft = boxes.getValue(i).topLeft)
        }
        onDrawn?.invoke(ChartDrawn(size.width, size.height, points, tickBoxes, drawn.associateWith { boxes.getValue(it) }))
        val maxLabels = if (n == 1) 1 else (plotW / 70.dp.toPx()).toInt().coerceIn(2, 4)
        val idx = if (n == 1) listOf(0) else (0 until maxLabels).map { k -> (k * (n - 1).toDouble() / (maxLabels - 1)).roundToInt() }.distinct()
        idx.forEach { i ->
            val t = measurer.measure(points[i].date.format(dateFmt), tick)
            val x = when { i == 0 && n > 1 -> xs[i]; i == n - 1 && n > 1 -> xs[i] - t.size.width; else -> xs[i] - t.size.width / 2 }
            drawText(t, topLeft = Offset(x, size.height - t.size.height))
        }
    }
}
