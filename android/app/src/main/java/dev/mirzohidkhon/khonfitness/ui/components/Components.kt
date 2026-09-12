package dev.mirzohidkhon.khonfitness.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

/** Large title, as in a navigation bar at the top of a scroll view: 34 bold on the leading edge, bar items trailing. */
@Composable
fun ScreenTitle(text: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
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
fun TextButton(text: String, color: Color = K.Accent, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) color else K.Dim, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
    }
}

/** Plus symbol as a bar or section action, 44 dp target. */
@Composable
fun PlusButton(label: String = "Add", onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Icon(dev.mirzohidkhon.khonfitness.ui.Icons.Plus, contentDescription = label, tint = K.Accent, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(50.dp).clip(CapsuleShape).background(if (enabled) K.Accent else K.Surface2).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (enabled) K.AccentInk else K.Muted, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
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
    onClick: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        if (divider) HorizontalDivider(Modifier.padding(start = if (dotColor != null) 42.dp else 16.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(start = 16.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dotColor != null) { Box(Modifier.width(26.dp), contentAlignment = Alignment.CenterStart) { Dot(dotColor, dotFilled) } }
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor, modifier = Modifier.weight(1f).padding(vertical = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (secondary != null) Text(secondary, style = MaterialTheme.typography.bodyMedium, color = K.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
            trailing?.invoke(this)
            if (chevron) Chevron()
        }
    }
}

@Composable
fun Chevron() { androidx.compose.material3.Icon(dev.mirzohidkhon.khonfitness.ui.Icons.ChevronRight, contentDescription = null, tint = K.Dim, modifier = Modifier.padding(start = 6.dp).size(14.dp)) }

/** Editor field row: label on the left, value or control on the right. */
@Composable
fun FieldRow(label: String, divider: Boolean = true, onClick: (() -> Unit)? = null, value: @Composable RowScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        if (divider) HorizontalDivider(Modifier.padding(start = 16.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val narrow = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 400
            Text(label, color = K.Text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(if (narrow) 104.dp else 128.dp))
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

/** Hybrid number widget: minus, typed value with unit, plus. Selects all text on focus. */
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
    // Narrow slots (two fields per row on a 360 dp phone) drop the unit and slim the steppers so the whole value stays visible.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
    val compact = maxWidth < 176.dp
    val stepWidth = if (compact) 36 else 44
    Row(
        Modifier.fillMaxWidth().height(height.dp).bringIntoViewRequester(bringIntoView).clip(RoundedCornerShape(10.dp)).background(K.Surface2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", stepWidth) { commit(((value ?: 0.0) - step).coerceAtLeast(0.0)) }
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
        StepButton("+", stepWidth) { commit((value ?: 0.0) + step) }
    }
    }
}

@Composable
private fun RowScope.StepButton(glyph: String, width: Int = 44, onClick: () -> Unit) {
    Box(Modifier.width(width.dp).fillMaxHeight().clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(glyph, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = K.Text)
    }
}
