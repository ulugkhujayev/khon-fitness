package dev.mirzohidkhon.khonfitness.ui.components

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import dev.mirzohidkhon.khonfitness.ui.theme.K
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor

/** Tick heights hang from the top (DESIGN.md, Today: 22 / 15 / 9), 2 dp wide; the labels sit under the tallest. */
private const val TICK_MAJOR = 22f
private const val TICK_HALF = 15f
private const val TICK_MINOR = 9f
private const val TICK_WIDTH = 2f
/** The accent center line: 3 dp wide, a little taller than a major tick. */
private const val LINE_WIDTH = 3f
private const val LABEL_TOP = TICK_MAJOR + 4

/**
 * Horizontal ruler picker: the ticks move under a fixed accent center line as they are dragged or flung, stop on
 * a tick, click on each one they pass (firmer on a labelled one) and report the value. A value set elsewhere moves
 * the ruler to it. The edges fade into [surface]. Screen readers get one adjustable element that steps a tick at
 * a time.
 */
@Composable
fun RulerPicker(ruler: Ruler, value: Double, onChange: (Double) -> Unit, label: String, unit: String, surface: Color, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val focus = LocalFocusManager.current
    val tick = rememberTick()
    val scope = rememberCoroutineScope()
    val change by rememberUpdatedState(onChange)
    val maxOffset = (ruler.tickCount() - 1) * ruler.gap
    // The offset (dp) of the value under the line; `shown` is that value, so a value that came from the ruler is not moved to again.
    var offset by remember(ruler) { mutableFloatStateOf(ruler.offsetOf(value)) }
    val shown = remember { doubleArrayOf(value) }
    var moving by remember { mutableStateOf<Job?>(null) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (value == shown[0] || dragging || moving != null) return@LaunchedEffect
        shown[0] = value
        offset = ruler.offsetOf(value)
    }

    fun moveTo(o: Float) {
        offset = o.coerceIn(0f, maxOffset)
        val v = ruler.valueAt(offset)
        if (v == shown[0]) return
        shown[0] = v
        tick(ruler.tickKind(ruler.tickIndex(v)) == TickKind.MAJOR)
        change(v)
    }

    val drag = rememberDraggableState { deltaPx -> moveTo(offset - deltaPx / density.density) }
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = K.Muted, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
    // Under the ticks, room for a label line, which grows with the font scale.
    val height = LABEL_TOP.dp + max(16.dp, with(density) { 16.sp.toDp() })

    Canvas(
        modifier.fillMaxWidth().height(height)
            .draggable(
                drag, Orientation.Horizontal,
                onDragStarted = {
                    moving?.cancel(); moving = null; dragging = true
                    // Picking with the ruler ends typing in the figure above it.
                    focus.clearFocus()
                },
                onDragStopped = { velocityPx ->
                    dragging = false
                    val job = scope.launch {
                        // Fling on, then settle on the nearest tick.
                        val d = density.density
                        AnimationState(offset * d, -velocityPx).animateDecay(splineBasedDecay(density)) {
                            val o = this.value / d
                            moveTo(o)
                            if (o <= 0f || o >= maxOffset) cancelAnimation()
                        }
                        animate(offset, ruler.snapOffset(offset), animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { o, _ -> moveTo(o) }
                    }
                    moving = job
                    job.invokeOnCompletion { if (moving === job) moving = null }
                },
            )
            .edgeFade(surface)
            .semantics {
                contentDescription = label
                stateDescription = "${fmt(value)} $unit"
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), ruler.min.toFloat()..ruler.max.toFloat(), steps = ruler.tickCount() - 2)
                setProgress { target -> change(ruler.tickValue(ruler.tickIndex(target.toDouble()))); true }
            },
    ) {
        val gap = ruler.gap.dp.toPx()
        val cx = size.width / 2
        val off = offset.dp.toPx()
        val first = floor((off - cx) / gap).toInt().coerceAtLeast(0)
        val last = ceil((off + cx) / gap).toInt().coerceAtMost(ruler.tickCount() - 1)
        val w = TICK_WIDTH.dp.toPx()
        for (i in first..last) {
            val x = cx + i * gap - off
            val kind = ruler.tickKind(i)
            val h = when (kind) { TickKind.MAJOR -> TICK_MAJOR; TickKind.HALF -> TICK_HALF; TickKind.MINOR -> TICK_MINOR }.dp.toPx()
            drawRoundRect(if (kind == TickKind.MINOR) K.Dim else K.Muted, Offset(x - w / 2, 0f), Size(w, h), CornerRadius(w / 2))
            if (kind == TickKind.MAJOR) {
                val text = measurer.measure(fmt(ruler.tickValue(i)), labelStyle)
                drawText(text, topLeft = Offset(x - text.size.width / 2f, LABEL_TOP.dp.toPx()))
            }
        }
        val lw = LINE_WIDTH.dp.toPx()
        drawRoundRect(K.Accent, Offset(cx - lw / 2, 0f), Size(lw, LABEL_TOP.dp.toPx()), CornerRadius(lw / 2))
    }
}

/**
 * A big centered figure with its unit, "80.6 kg", above a picker that sets it. A tap selects the number for typing.
 * Text that is not a positive number yet ("", "0.") leaves the value as it was; leaving the field shows the value.
 */
@Composable
fun FigureField(value: Double, onChange: (Double) -> Unit, unit: String, label: String, modifier: Modifier = Modifier, decimals: Int = 1) {
    var text by remember { mutableStateOf(TextFieldValue(fmt(value, decimals))) }
    var focused by remember { mutableStateOf(false) }
    val current by rememberUpdatedState(value)
    LaunchedEffect(value) { if (!focused) text = TextFieldValue(fmt(value, decimals)) }
    Row(modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center, verticalAlignment = Alignment.Bottom) {
        BasicTextField(
            value = text,
            onValueChange = { new ->
                val cleaned = cleanTyped(new.text, decimals)
                text = if (cleaned == new.text) new else TextFieldValue(
                    cleaned,
                    TextRange(cleanOffset(new.text, new.selection.start, decimals), cleanOffset(new.text, new.selection.end, decimals)),
                )
                typedNumber(cleaned)?.takeIf { it > 0 }?.let(onChange)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (decimals > 0) KeyboardType.Decimal else KeyboardType.Number),
            textStyle = MaterialTheme.typography.headlineLarge.copy(color = K.Text, letterSpacing = 0.sp, textAlign = TextAlign.Center, fontFeatureSettings = "tnum"),
            cursorBrush = SolidColor(K.Accent),
            modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 44.dp).alignByBaseline()
                .semantics { contentDescription = label }
                .onFocusChanged { st ->
                    if (st.isFocused && !focused) text = text.copy(selection = TextRange(0, text.text.length))
                    // The picker may have moved the value since, and "0" was never taken.
                    if (!st.isFocused && focused) text = TextFieldValue(fmt(current, decimals))
                    focused = st.isFocused
                },
        )
        Text(unit, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = K.Muted, modifier = Modifier.alignByBaseline().padding(start = 4.dp).clearAndSetSemantics {})
    }
}
