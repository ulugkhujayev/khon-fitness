package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mirzohidkhon.khonfitness.data.IntervalLog
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.data.SetLog
import dev.mirzohidkhon.khonfitness.data.hrFacts
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.components.GroupedList
import dev.mirzohidkhon.khonfitness.ui.components.SectionTitle
import dev.mirzohidkhon.khonfitness.ui.components.axisWidth
import dev.mirzohidkhon.khonfitness.ui.theme.Inter
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.ZoneId

const val HEART_PICTURE_TAG = "heart-picture"
private const val PICTURE_HEIGHT = 190
private const val AXIS_GAP = 8

@Composable
fun rememberWorkoutPicture(vm: AppViewModel, s: Session, logs: List<SetLog>, rounds: List<IntervalLog>): WorkoutPicture? {
    val row by remember(s.id) { vm.repo.sessionHr(s.id) }.collectAsStateWithLifecycle(null)
    val zone = ZoneId.systemDefault()
    return remember(s, row, logs, rounds, zone) { workoutPicture(s, hrFacts(s, row, zone), logs, rounds, zone) }
}

@Composable
fun HeartPicture(p: WorkoutPicture, modifier: Modifier = Modifier) {
    val axis = p.axis ?: return
    val measurer = rememberTextMeasurer()
    val tick = TextStyle(fontFamily = Inter, color = K.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    Canvas(modifier.fillMaxWidth().height(PICTURE_HEIGHT.dp).testTag(HEART_PICTURE_TAG).clearAndSetSemantics { contentDescription = p.spoken }) {
        val lines = axis.lines
        val labels = lines.map { measurer.measure(it.toString(), tick) }
        val start = measurer.measure(p.startClock, tick)
        val end = measurer.measure(p.endClock, tick)
        val padL = axisWidth(labels.map { it.size.width.toFloat() }, AXIS_GAP.dp.toPx())
        val padR = 8.dp.toPx()
        val padT = labels.last().size.height / 2f + 2.dp.toPx()
        val padB = start.size.height + 8.dp.toPx()
        val plotW = size.width - padL - padR
        val plotH = size.height - padT - padB
        val bottom = padT + plotH
        val lengthMs = p.range.lengthMs.toFloat()
        fun x(ms: Long) = padL + ms * plotW / lengthMs
        fun y(bpm: Int) = padT + (axis.high - bpm) * plotH / (axis.high - axis.low).toFloat()
        p.bands.forEach { b -> drawRect(K.Surface2, Offset(x(b.fromMs), padT), androidx.compose.ui.geometry.Size(x(b.toMs) - x(b.fromMs), plotH)) }
        lines.forEachIndexed { k, v ->
            val yy = y(v)
            drawLine(K.Divider, Offset(padL, yy), Offset(size.width - padR, yy), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
            drawText(labels[k], topLeft = Offset(padL - AXIS_GAP.dp.toPx() - labels[k].size.width, yy - labels[k].size.height / 2))
        }
        p.curve.forEach { seg ->
            val pts = seg.map { Offset(x(it.offsetSec * 1000L), y(it.bpm)) }
            if (pts.size == 1) drawCircle(K.Accent, 2.dp.toPx(), pts[0])
            else drawPath(Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }, K.Accent, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        p.markOffsetsMs.forEach { ms -> drawLine(K.Muted, Offset(x(ms), bottom), Offset(x(ms), bottom - 8.dp.toPx()), strokeWidth = 1.5.dp.toPx()) }
        drawText(start, topLeft = Offset(padL, size.height - start.size.height))
        drawText(end, topLeft = Offset(size.width - padR - end.size.width, size.height - end.size.height))
    }
}

@Composable
fun SetsInOrder(rows: List<SetMarkRow>) {
    if (rows.size < 2) return
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR)
    val density = LocalDensity.current
    fun column(values: List<String?>) = values.filterNotNull().maxOfOrNull { with(density) { measurer.measure(it, style).size.width.toDp() } + 1.dp }
    val bpm = rows.map { r -> r.maxBpm?.let { "$it bpm" } }
    val since = rows.map { r -> r.sinceSec?.let(::minSec) }
    val widths = remember(rows, style, density) { column(since) to column(bpm) }
    SectionTitle("Sets in order", Modifier.padding(top = 12.dp))
    GroupedList(Modifier.padding(bottom = 12.dp)) {
        rows.forEachIndexed { i, r ->
            if (i > 0) HorizontalDivider(Modifier.padding(start = 16.dp), color = K.Divider, thickness = 0.7.dp)
            Column(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp).semantics(mergeDescendants = true) { contentDescription = r.spoken },
                verticalArrangement = Arrangement.Center,
            ) {
                Row {
                    Text(r.name, style = MaterialTheme.typography.bodyLarge, color = K.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).alignByBaseline())
                    Spacer(Modifier.width(12.dp))
                    Text(r.clock, style = style, color = K.Muted, maxLines = 1, modifier = Modifier.alignByBaseline())
                }
                Row {
                    Text(r.set, style = style, color = K.Muted, modifier = Modifier.weight(1f).alignByBaseline())
                    listOf(since[i] to widths.first, bpm[i] to widths.second).forEach { (text, w) ->
                        if (w != null) {
                            Spacer(Modifier.width(12.dp))
                            Text(text ?: "", style = style, color = K.Muted, textAlign = TextAlign.End, maxLines = 1, modifier = Modifier.width(w).alignByBaseline())
                        }
                    }
                }
            }
        }
    }
}
