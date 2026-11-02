package dev.mirzohidkhon.khonfitness.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.util.Locale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role

/** Inset grouped list radius. iOS 26 groups use a large concentric radius. */
val GroupShape = RoundedCornerShape(20.dp)
/** Capsule for full-width prominent buttons. */
val CapsuleShape = RoundedCornerShape(25.dp)

fun fmt(value: Double?, decimals: Int = 1): String {
    if (value == null) return ""
    val v = if (decimals == 0) value.toLong().toDouble() else value
    val s = String.format(Locale.US, "%.${decimals}f", v)
    return if (decimals > 0 && s.endsWith(".0")) s.dropLast(2) else s
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    // Grouped section header: Footnote in secondary label color, inset to the row text. Sentence case by owner rule.
    Row(modifier.fillMaxWidth().heightIn(min = 32.dp).padding(start = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = K.Muted, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun TextButton(text: String, color: Color = K.Accent, enabled: Boolean = true, description: String? = null, onClick: () -> Unit) {
    val spoken = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
    Box(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).clickable(enabled = enabled, role = Role.Button, onClick = onClick).then(spoken).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) color else K.Dim, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium,
            modifier = if (description != null) Modifier.clearAndSetSemantics {} else Modifier)
    }
}

/** Plus symbol as a bar or section action, 44 dp target. */
@Composable
fun PlusButton(label: String = "Add", onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Icon(dev.mirzohidkhon.khonfitness.ui.Icons.Plus, contentDescription = label, tint = K.Accent, modifier = Modifier.size(22.dp))
    }
}

/**
 * The one prominent button of a screen: a 50 dp (compact 44) full-width capsule in accent. [color] is [K.Red] for a
 * destructive confirm; [TintedButton] passes a tint. [description] is the spoken name when the text is not enough.
 */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = K.Accent,
    textColor: Color = K.AccentInk,
    compact: Boolean = false,
    description: String? = null,
    onClick: () -> Unit,
) {
    val spoken = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
    Box(
        modifier.fillMaxWidth().height(if (compact) 44.dp else 50.dp).clip(CapsuleShape).background(if (enabled) color else K.Surface2).clickable(enabled = enabled, role = Role.Button, onClick = onClick).then(spoken),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (enabled) textColor else K.Muted, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = if (description != null) Modifier.clearAndSetSemantics {} else Modifier) }
}

/** iOS-style grouped inset list. Rows draw their own inset dividers. */
@Composable
fun GroupedList(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().clip(GroupShape).background(K.Surface)) { content() }
}

@Composable
fun Dot(color: Color, filled: Boolean = true, size: Int = 11) {
    val m = Modifier.size(size.dp).clip(CircleShape)
    Box(if (filled) m.background(color) else m.border(2.dp, color, CircleShape))
}

@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    dotColor: Color? = null,
    dotFilled: Boolean = true,
    chevron: Boolean = true,
    divider: Boolean = true,
    titleColor: Color = K.Text,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    selected: Boolean? = null,
    onClick: (() -> Unit)? = null,
) {
    val largeText = androidx.compose.ui.platform.LocalConfiguration.current.fontScale >= 1.5f
    Column(modifier.fillMaxWidth()) {
        if (divider) HorizontalDivider(Modifier.padding(start = if (dotColor != null) 42.dp else 16.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).then(if (onClick == null) Modifier else if (selected != null) Modifier.selectable(selected, role = Role.RadioButton, onClick = onClick) else Modifier.clickable(onClick = onClick)).padding(start = 16.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dotColor != null) { Box(Modifier.width(26.dp), contentAlignment = Alignment.CenterStart) { Dot(dotColor, dotFilled) } }
            if (largeText && secondary != null) {
                Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(secondary, style = MaterialTheme.typography.bodyMedium, color = K.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor, modifier = Modifier.weight(1f).padding(vertical = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (secondary != null) Text(secondary, style = MaterialTheme.typography.bodyMedium, color = K.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
            }
            trailing?.invoke(this)
            if (chevron) Chevron()
        }
    }
}

@Composable
fun Chevron() { androidx.compose.material3.Icon(dev.mirzohidkhon.khonfitness.ui.Icons.ChevronRight, contentDescription = null, tint = K.Dim, modifier = Modifier.padding(start = 6.dp).size(14.dp)) }

/** Editor field row: label on the left, value or control on the right. [note] is a muted line under the label, e.g. "per side". */
@Composable
fun FieldRow(label: String, divider: Boolean = true, note: String? = null, onClick: (() -> Unit)? = null, value: @Composable RowScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        if (divider) HorizontalDivider(Modifier.padding(start = 16.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val narrow = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 400
            Column(Modifier.width(if (narrow) 104.dp else 128.dp).padding(vertical = 10.dp)) {
                Text(label, color = K.Text, style = MaterialTheme.typography.bodyLarge)
                if (note != null) Text(note, color = K.Muted, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(12.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { value() }
        }
    }
}

@Composable
fun InlineTextField(value: String, onValueChange: (String) -> Unit, placeholder: String = "", modifier: Modifier = Modifier, singleLine: Boolean = true) {
    BasicTextField(
        value = value, onValueChange = onValueChange, singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = K.Text, textAlign = TextAlign.End),
        cursorBrush = SolidColor(K.Accent),
        modifier = modifier.fillMaxWidth().padding(vertical = 14.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterEnd) {
                if (value.isEmpty()) Text(placeholder, color = K.Dim, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                inner()
            }
        },
    )
}

/**
 * Hybrid number widget: minus, typed value with unit, plus. Selects all text on focus. A tap on − or + steps once
 * with a light haptic click and saves; held, it repeats faster ([repeatPress]) and saves once on release.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberField(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    step: Double,
    unit: String,
    modifier: Modifier = Modifier,
    decimals: Int = 1,
    height: Int = 44,
    focusRequester: FocusRequester? = null,
    onFocused: (() -> Unit)? = null,
    placeholder: String? = null,
) {
    var text by remember { mutableStateOf(TextFieldValue(fmt(value, decimals))) }
    var hasFocus by remember { mutableStateOf(false) }
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    // Sync from outside (steppers, prefill) only when the typed text does not already mean this value. Keeps the cursor where the user left it.
    // While the user has cleared the field, leave it empty: the owner may push a minimum back that would prefix the next digit.
    LaunchedEffect(value) {
        val typed = text.text.trimEnd('.').toDoubleOrNull()
        if (typed != value && !(hasFocus && text.text.isEmpty())) { val t = fmt(value, decimals); text = TextFieldValue(t, TextRange(t.length)) }
    }
    fun commit(v: Double?) { onValueChange(v?.let { Math.max(0.0, it) }) }
    val tick = rememberTick()
    // While − or + is held the steps only show; the value is saved once on release, not on every step.
    val held = remember { arrayOfNulls<Double>(1) }
    fun stepBy(sign: Int, save: Boolean) {
        // The typed text first: a database write may not have come back into `value` yet.
        val base = held[0] ?: text.text.trimEnd('.').toDoubleOrNull() ?: value ?: 0.0
        val next = stepValue(base, sign, step)
        val t = fmt(next, decimals)
        text = TextFieldValue(t, TextRange(t.length))
        tick(false)
        if (save) commit(next) else held[0] = next
    }
    fun release() { held[0]?.let { commit(it) }; held[0] = null }
    // Narrow slots (two fields per row on a 360 dp phone) drop the unit and slim the steppers so the whole value stays visible.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
    val compact = maxWidth < 176.dp
    val stepWidth = if (compact) 36 else 44
    Row(
        Modifier.fillMaxWidth().height(height.dp).bringIntoViewRequester(bringIntoView).clip(RoundedCornerShape(10.dp)).background(K.Surface2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", "Decrease by ${fmt(step, decimals)} $unit".trim(), stepWidth, { stepBy(-1, it) }, ::release)
        Box(Modifier.width(1.dp).fillMaxHeight().background(K.Divider))
        BasicTextField(
            value = text,
            onValueChange = { new ->
                val cleaned = new.text.replace(',', '.').filter { it.isDigit() || it == '.' }
                text = new.copy(text = cleaned)
                commit(cleaned.toDoubleOrNull())
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (decimals > 0) KeyboardType.Decimal else KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = K.Text, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End),
            cursorBrush = SolidColor(K.Accent),
            decorationBox = { inner -> Box(contentAlignment = Alignment.CenterEnd) {
                if (text.text.isEmpty() && placeholder != null) Text(placeholder, color = K.Dim, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                inner()
            } },
            modifier = Modifier.weight(1f).padding(start = 8.dp)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { st ->
                    if (st.isFocused && !hasFocus) {
                        text = text.copy(selection = TextRange(0, text.text.length)); onFocused?.invoke()
                        // The keyboard is still rising when focus lands. Scroll again once it has settled.
                        scope.launch { delay(120); bringIntoView.bringIntoView(); delay(300); bringIntoView.bringIntoView() }
                    }
                    if (!st.isFocused && hasFocus && text.text.trimEnd('.').toDoubleOrNull() != value) text = TextFieldValue(fmt(value, decimals))
                    hasFocus = st.isFocused
                },
        )
        if (compact) Spacer(Modifier.width(8.dp))
        else Text(unit, color = K.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp, end = 8.dp))
        Box(Modifier.width(1.dp).fillMaxHeight().background(K.Divider))
        StepButton("+", "Increase by ${fmt(step, decimals)} $unit".trim(), stepWidth, { stepBy(1, it) }, ::release)
    }
    }
}

/** − or +: [onStep] gets `save = false` while held; the caller saves once in [onRelease]. */
@Composable
private fun RowScope.StepButton(glyph: String, label: String, width: Int, onStep: (save: Boolean) -> Unit, onRelease: () -> Unit) {
    Box(Modifier.width(width.dp).fillMaxHeight().repeatPress(label, onTap = { onStep(true) }, onRepeat = { onStep(false) }, onRelease = onRelease), contentAlignment = Alignment.Center) {
        Text(glyph, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = K.Text)
    }
}

/**
 * Press handling for a − or + button: a tap calls [onTap]; held past [REPEAT_AFTER_MS] it calls [onRepeat] again
 * and again, faster the longer it is held ([repeatDelay]), and [onRelease] once the finger lifts. A press shows a
 * Surface3 fill. Screen readers get one button that taps.
 */
@Composable
fun Modifier.repeatPress(label: String, onTap: () -> Unit, onRepeat: () -> Unit, onRelease: () -> Unit): Modifier {
    val tap by rememberUpdatedState(onTap)
    val repeat by rememberUpdatedState(onRepeat)
    val release by rememberUpdatedState(onRelease)
    var pressed by remember { mutableStateOf(false) }
    return this
        .background(if (pressed) K.Surface3 else Color.Transparent)
        .pointerInput(Unit) {
            coroutineScope {
                awaitEachGesture {
                    awaitFirstDown()
                    pressed = true
                    var ended = false
                    val up = withTimeoutOrNull(REPEAT_AFTER_MS) { waitForUpOrCancellation().also { ended = true } }
                    if (ended) {
                        pressed = false
                        if (up != null) { up.consume(); tap() }
                        return@awaitEachGesture
                    }
                    val stepping = launch { var n = 0; while (isActive) { repeat(); delay(repeatDelay(n++)) } }
                    waitForUpOrCancellation()?.consume()
                    stepping.cancel()
                    pressed = false
                    release()
                }
            }
        }
        .clearAndSetSemantics { contentDescription = label; role = Role.Button; onClick { tap(); true } }
}
