package dev.mirzohidkhon.khonfitness.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.ui.theme.K

/** A white symbol on a rounded square of its color, leading a list row like iOS Settings. Decorative: the row names it. */
@Composable
fun IconTile(icon: ImageVector, color: Color) {
    Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(color).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
    }
}

/** A symbol in its color on a soft circle of the same color (20 percent), e.g. a workout's kind. Decorative. */
@Composable
fun SymbolCircle(icon: ImageVector, color: Color) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)).clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
}

/** A block's number (zero-based [index], shown from 1) in a small accent-tinted circle: session blocks, a program's block list. */
@Composable
fun IndexBadge(index: Int) {
    Box(Modifier.size(24.dp).clip(CircleShape).background(K.AccentSoft), contentAlignment = Alignment.Center) {
        Text("${index + 1}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = K.Accent)
    }
}

/**
 * An action as a row in a [GroupedList], like the actions at the end of an iOS contact: a 20 dp symbol and the
 * action's name in its color, accent or [K.Red] for a destructive one. Off, both turn dim and the row does nothing.
 */
@Composable
fun ActionRow(icon: ImageVector, title: String, color: Color = K.Accent, enabled: Boolean = true, divider: Boolean = true, onClick: () -> Unit) {
    val tint = if (enabled) color else K.Dim
    Column(Modifier.fillMaxWidth()) {
        if (divider) HorizontalDivider(Modifier.padding(start = 52.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, color = tint, modifier = Modifier.padding(vertical = 12.dp))
        }
    }
}

/**
 * The actions at the bottom of an editor: groups of [ActionRow]s 20 dp apart, 32 dp under the fields, the
 * destructive one in a group of its own, and a [note] under them saying why an action is off.
 */
@Composable
fun EditorActions(note: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        content()
        if (note != null) GroupFooter(note)
    }
}

/** A muted note under a group that says what its setting does (a grouped list's footer); red for an error. */
@Composable
fun GroupFooter(text: String, modifier: Modifier = Modifier, error: Boolean = false) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = if (error) K.Red else K.Muted, modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp))
}

/**
 * A secondary action that should still read as a button: the primary capsule tinted, its color as text on that
 * color at 20 percent and 44 dp tall, so the screen's one primary button keeps the lead. [K.Red] for a destructive one.
 */
@Composable
fun TintedButton(text: String, modifier: Modifier = Modifier, color: Color = K.Accent, enabled: Boolean = true, description: String? = null, onClick: () -> Unit) {
    PrimaryButton(text, modifier, enabled = enabled, color = color.copy(alpha = 0.2f), textColor = color, compact = true, description = description, onClick = onClick)
}

/**
 * Nothing to show yet, after iOS ContentUnavailableView: a 32 dp symbol in its color, a title, a muted line saying
 * why or what to do, and any [actions] under them. On its own card unless [card] is off (inside a list's card).
 */
@Composable
fun ContentEmpty(icon: ImageVector, title: String, text: String, color: Color = K.Muted, card: Boolean = true, actions: (@Composable ColumnScope.() -> Unit)? = null) {
    val frame = if (card) Modifier.clip(GroupShape).background(K.Surface) else Modifier
    Column(frame.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = K.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
        actions?.invoke(this)
    }
}

/** What a list under a search shows when nothing matches, inside the list's card: a magnifying glass, "No Results for “row”" and what to try. */
@Composable
fun SearchEmpty(query: String) {
    val (title, hint) = noResults(query)
    ContentEmpty(Icons.Search, title, hint, card = false)
}

/** Search field: magnifier, the text, a clear button once there is text. [elevated] inside a sheet. */
@Composable
fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, elevated: Boolean = false) {
    Row(
        modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp)).background(if (elevated) K.Elevated else K.Surface).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Search, contentDescription = null, tint = K.Dim, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value, onChange, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = K.Text),
            cursorBrush = SolidColor(K.Accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false, imeAction = ImeAction.Search),
            modifier = Modifier.weight(1f).semantics { contentDescription = "Search" },
            decorationBox = { inner -> Box { if (value.isEmpty()) Text("Search", color = K.Dim, style = MaterialTheme.typography.bodyLarge); inner() } },
        )
        if (value.isNotEmpty()) {
            Box(Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button) { onChange("") }.semantics { contentDescription = "Clear search" }, contentAlignment = Alignment.Center) {
                Icon(Icons.ClearFill, contentDescription = null, tint = K.Muted, modifier = Modifier.size(18.dp))
            }
        } else Spacer(Modifier.width(12.dp))
    }
}

/** Fades the left and right edges of a strip into [color], the surface under it, so its content runs out instead of being cut. */
fun Modifier.edgeFade(color: Color, width: Dp = 40.dp): Modifier = drawWithContent {
    drawContent()
    val w = width.toPx().coerceAtMost(size.width / 2)
    drawRect(Brush.horizontalGradient(listOf(color, Color.Transparent), startX = 0f, endX = w), size = Size(w, size.height))
    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, color), startX = size.width - w, endX = size.width), topLeft = Offset(size.width - w, 0f), size = Size(w, size.height))
}

/**
 * A thin progress track: [done] of [total] filled in [color] on Surface2, 5 dp tall and rounded ([thin]: a 2 dp
 * square line, the folded session header). Screen readers get [label] and the count.
 */
@Composable
fun ProgressBar(done: Int, total: Int, label: String, modifier: Modifier = Modifier, color: Color = K.Green, thin: Boolean = false) {
    val share = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
    val corner = RoundedCornerShape(if (thin) 0.dp else 3.dp)
    Box(
        modifier.fillMaxWidth().height(if (thin) 2.dp else 5.dp).clip(corner).background(K.Surface2)
            .clearAndSetSemantics { contentDescription = label; progressBarRangeInfo = ProgressBarRangeInfo(done.toFloat(), 0f..total.coerceAtLeast(1).toFloat()) },
    ) {
        if (share > 0f) Box(Modifier.fillMaxWidth(share).fillMaxHeight().clip(corner).background(color))
    }
}

/** An on/off setting as an editor row: its label and a switch in the app's colors. [enabled] off fixes it where it is. */
@Composable
fun ToggleRow(label: String, checked: Boolean, divider: Boolean = true, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    FieldRow(label, divider = divider) {
        Switch(
            checked, onChange, enabled = enabled, modifier = Modifier.semantics { contentDescription = label },
            colors = SwitchDefaults.colors(
                checkedTrackColor = K.Green, checkedThumbColor = Color.White, uncheckedTrackColor = K.Surface3,
                uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent,
                disabledCheckedTrackColor = K.Green.copy(alpha = 0.45f), disabledCheckedThumbColor = Color.White,
                disabledUncheckedTrackColor = K.Surface3, disabledUncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                disabledUncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

/** A select's value on the right of a [FieldRow]: in Text with a chevron, or muted with a small lock when it cannot change. */
@Composable
fun FieldValue(text: String, locked: Boolean = false, maxLines: Int = 1) {
    Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = if (locked) K.Muted else K.Text, maxLines = maxLines, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
    if (locked) Icon(Icons.Lock, contentDescription = "Locked", tint = K.Dim, modifier = Modifier.padding(start = 6.dp).size(13.dp))
    else Chevron()
}

