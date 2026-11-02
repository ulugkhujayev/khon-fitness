package dev.mirzohidkhon.khonfitness.ui.components

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
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
 * a tick, click on each one they pass (firmer on a labelled one) and report the value. [value] reads the parent's
 * state and [onChange] writes it at once. A value set elsewhere (typed, a reset) stops any fling and moves the
 * ruler to it with no report ([RulerMotion]). The edges fade into [surface]. Screen readers get one adjustable
 * element that steps a tick at a time.
 */
@Composable
fun RulerPicker(ruler: Ruler, value: () -> Double, onChange: (Double) -> Unit, label: String, unit: String, surface: Color, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val focus = LocalFocusManager.current
    val tick = rememberTick()
    val scope = rememberCoroutineScope()
    val change by rememberUpdatedState(onChange)
    val held by rememberUpdatedState(value)
    val motion = remember(ruler) { RulerMotion(ruler, value()) }
    var offset by remember(ruler) { mutableFloatStateOf(motion.offset) }
    // The drag's id, and its fling and settle.
    val drag = remember { intArrayOf(0) }
    var moving by remember { mutableStateOf<Job?>(null) }

    fun takeOutside(v: Double) {
        if (!motion.outside(v)) return
        moving?.cancel(); moving = null
        offset = motion.offset
    }

    val current = value()
    LaunchedEffect(current) { takeOutside(current) }

    /** One frame of gesture [id]; false once it lost the ruler. */
    fun step(id: Int, to: Float): Boolean {
        val v = motion.move(id, to, held())
        offset = motion.offset
        if (v != null) {
            tick(ruler.tickKind(ruler.tickIndex(v)) == TickKind.MAJOR)
            change(v)
        }
        return motion.owns(id)
    }

    val dragState = rememberDraggableState { deltaPx -> step(drag[0], motion.offset - deltaPx / density.density) }
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = K.Muted, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
    // Under the ticks, room for a label line, which grows with the font scale.
    val height = LABEL_TOP.dp + max(16.dp, with(density) { 16.sp.toDp() })

    Canvas(
        modifier.fillMaxWidth().height(height)
            .draggable(
                dragState, Orientation.Horizontal,
                onDragStarted = {
                    moving?.cancel(); moving = null
                    drag[0] = motion.start()
                    // Picking with the ruler ends typing in the figure above it.
                    focus.clearFocus()
                },
                onDragStopped = { velocityPx ->
                    val id = drag[0]
                    if (motion.owns(id)) {
                        val job = scope.launch {
                            // Fling on, then settle on the nearest tick; outside input stops both.
                            val d = density.density
                            AnimationState(motion.offset * d, -velocityPx).animateDecay(splineBasedDecay(density)) {
                                val o = this.value / d
                                if (!step(id, o) || ruler.atEnd(o)) cancelAnimation()
                            }
                            if (!motion.owns(id)) return@launch
                            AnimationState(motion.offset).animateTo(ruler.snapOffset(motion.offset), spring(stiffness = Spring.StiffnessMediumLow)) {
                                if (!step(id, this.value)) cancelAnimation()
                            }
                        }
                        moving = job
                        job.invokeOnCompletion { motion.end(id); if (moving === job) moving = null }
                    }
                },
            )
            .edgeFade(surface)
            .semantics {
                contentDescription = label
                stateDescription = "${fmt(current)} $unit"
                progressBarRangeInfo = ProgressBarRangeInfo(current.toFloat(), ruler.min.toFloat()..ruler.max.toFloat(), steps = ruler.tickCount() - 2)
                // A screen reader adjust is the user's own pick: it stops any fling, ends typing and reports once.
                setProgress { target ->
                    val v = ruler.tickValue(ruler.tickIndex(target.toDouble()))
                    takeOutside(v)
                    focus.clearFocus()
                    change(v)
                    true
                }
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
 * Text that is not a positive number yet ("", "0.") leaves the value as it was and reports [onValid] false, so a
 * save button can turn off; leaving the field shows the value again and reports true.
 */
@Composable
fun FigureField(value: Double, onChange: (Double) -> Unit, unit: String, label: String, modifier: Modifier = Modifier, decimals: Int = 1, onValid: (Boolean) -> Unit = {}) {
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
                val typed = typedFigure(cleaned)
                typed?.let(onChange)
                onValid(typed != null)
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
                    if (!st.isFocused && focused) { text = TextFieldValue(fmt(current, decimals)); onValid(true) }
                    focused = st.isFocused
                },
        )
        Text(unit, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = K.Muted, modifier = Modifier.alignByBaseline().padding(start = 4.dp).clearAndSetSemantics {})
    }
}
