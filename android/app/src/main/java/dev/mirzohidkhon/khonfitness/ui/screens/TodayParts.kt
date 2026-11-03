package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.LocalDate

/** A day as a tile on the group surface: the month calendar on History (DESIGN.md, Today). */
val DayTileShape = RoundedCornerShape(12.dp)

/** Tabular figures, so day numbers and counts keep their width. */
const val TABULAR = "tnum"

/** A plan item's symbol: a dumbbell for a program, a runner for cardio, a moon for rest. */
fun itemIcon(item: PlanItem): ImageVector = when {
    item.isRest -> Icons.Moon
    item.itemType == ItemType.PROGRAM -> Icons.Dumbbell
    else -> Icons.Runner
}

/**
 * Today's item on its card, with its kind as a symbol in its color. A running session: its line in accent ("In
 * progress · 3 of 12 sets", or the timer's state) and Resume; a program adds its progress bar with the time since
 * the start. Saved today: a green check, the done line, Review, and "+1 more today" when other sessions were saved
 * today. Otherwise the item with Change at the top right and Start (Rest has no Start), or, with no week plan at
 * all, "No plan for today" with "Set up your week". Change only shows while nothing ran today.
 */
@Composable
fun TodayCard(
    item: PlanItem,
    line: String,
    color: Color,
    unfinished: Session?,
    running: String,
    progress: SetProgress?,
    elapsed: String,
    runningColor: Color,
    done: Session?,
    doneText: String?,
    more: Int,
    noPlan: Boolean,
    onStart: () -> Unit,
    onResume: (Session) -> Unit,
    onChange: () -> Unit,
    onReview: (Session) -> Unit,
    onMore: () -> Unit,
    onSetUp: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(GroupShape).background(K.Surface).padding(horizontal = 16.dp, vertical = 14.dp)) {
        when {
            unfinished != null -> {
                CardHead(
                    if (unfinished.itemType == ItemType.PROGRAM) Icons.Dumbbell else Icons.Runner, runningColor,
                    unfinished.programName ?: unfinished.exerciseName ?: "Session", running, K.Accent,
                )
                if (progress != null) Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProgressBar(progress.done, progress.total, "${progress.done} of ${progress.total} sets done", Modifier.weight(1f), color = K.Accent)
                    Text(elapsed, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR), color = K.Muted, modifier = Modifier.padding(start = 12.dp))
                }
                PrimaryButton("Resume", Modifier.padding(top = 14.dp)) { onResume(unfinished) }
            }
            done != null && doneText != null -> {
                CardHead(Icons.Check, K.Green, item.name, doneText, K.Green)
                if (more > 0) CardLink(moreLine(more), "${moreLine(more)}, open History", Modifier.padding(start = 52.dp), onMore)
                PrimaryButton("Review", Modifier.padding(top = if (more > 0) 4.dp else 14.dp), color = K.Accent.copy(alpha = 0.2f), textColor = K.Accent, description = "Review today's session") { onReview(done) }
            }
            noPlan -> {
                CardHead(Icons.Today, K.Muted, "No plan for today", "Pick a workout for each weekday.")
                PrimaryButton("Set up your week", Modifier.padding(top = 14.dp), onClick = onSetUp)
            }
            item.isRest -> CardHead(Icons.Moon, K.Muted, "Rest day", line, trailing = { ChangeLink(onChange) })
            else -> {
                CardHead(itemIcon(item), color, item.name, line, trailing = { ChangeLink(onChange) })
                PrimaryButton("Start", Modifier.padding(top = 14.dp), onClick = onStart)
            }
        }
    }
}

/** "Change" at the card's top right: accent text on a 48 dp target. */
@Composable
private fun ChangeLink(onClick: () -> Unit) = CardLink("Change", "Change today's plan", Modifier.padding(start = 4.dp), onClick)

/** A link in accent on a 48 dp target, spoken as [spoken]. */
@Composable
private fun CardLink(text: String, spoken: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick).clearAndSetSemantics { contentDescription = spoken; role = Role.Button; onClick { onClick(); true } }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(text, color = K.Accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold) }
}

/** The symbol, then the name over its line (accent while a session runs, green once done), and [trailing] at the top right. */
@Composable
private fun CardHead(icon: ImageVector, color: Color, title: String, line: String, lineColor: Color = K.Muted, trailing: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 4.dp)) { SymbolCircle(icon, color) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(top = 2.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(line, style = MaterialTheme.typography.bodyMedium, color = lineColor, modifier = Modifier.padding(top = 1.dp))
        }
        trailing?.invoke()
    }
}

/**
 * A row of Today's group, sized for the home screen and matching the item card above it: the symbol in a 40 dp
 * circle of its color, the name in Headline over a line of state ([lineColor] when it is good news), a mark and
 * the chevron. 64 dp tall: room to tap on a screen people open many times a day. Large text wraps instead of cutting.
 */
@Composable
fun HomeRow(icon: ImageVector, color: Color, title: String, line: String, lineColor: Color = K.Muted, divider: Boolean, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val lines = if (LocalConfiguration.current.fontScale >= 1.5f) 3 else 1
    Column(Modifier.fillMaxWidth()) {
        if (divider) HorizontalDivider(Modifier.padding(start = 68.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolCircle(icon, color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = lines, overflow = TextOverflow.Ellipsis)
                Text(line, style = MaterialTheme.typography.bodyMedium, color = lineColor, maxLines = lines, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            trailing?.invoke()
            Chevron()
        }
    }
}

/** The green check after a row that is done for today. Decorative: the row's line says it. */
@Composable
fun DoneCheck() {
    Icon(Icons.Check, contentDescription = null, tint = K.Green, modifier = Modifier.padding(start = 6.dp).size(18.dp))
}

/** What a screen reader says for each state of the entry button. */
private val SPOKEN = mapOf("Save" to "Save bodyweight", "Update" to "Update today's bodyweight", "Saved" to "Today's bodyweight is saved")

/**
 * Bodyweight on Today, one card: the title over its status line ([bodyweightStatus]), then − and + 52 dp squares
 * around the value (tap it to type; 0.1 kg a step, a hold repeats, 20-250 kg), and Save, Update or Saved (off). A
 * typed value saves with one tap on the button or the keyboard's Done. Text that is not a weight stays as typed,
 * with "Enter 20-250 kg" under it and the button off. The entry starts at the latest value, 75 kg before the first.
 */
@Composable
fun BodyweightCard(today: LocalDate, bodyweights: List<Bodyweight>, onSave: (Double) -> Unit) {
    var text by remember(today) { mutableStateOf(TextFieldValue(kgText(startKg(bodyweights)))) }
    var touched by remember(today) { mutableStateOf(false) }
    // The entries load after the first frame: show the latest one until the user steps or types.
    LaunchedEffect(today, bodyweights.lastOrNull()) { if (!touched) text = TextFieldValue(kgText(startKg(bodyweights))) }
    val kg = parseKg(text.text)
    val action = entryAction(bodyweights, today, kg)
    val focus = LocalFocusManager.current
    val tick = rememberTick()
    fun step(sign: Int) {
        val t = kgText(stepKg(text.text, sign, startKg(bodyweights)))
        text = TextFieldValue(t, TextRange(t.length)); touched = true; tick(false)
    }
    fun save() {
        val v = parseKg(text.text) ?: return
        if (!entryAction(bodyweights, today, v).enabled) return
        onSave(v)
        text = TextFieldValue(kgText(v)); focus.clearFocus()
    }
    Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(GroupShape).background(K.Surface).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text("Bodyweight", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(bodyweightStatus(bodyweights, today), style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR), color = K.Muted, modifier = Modifier.padding(top = 2.dp))
        Row(Modifier.padding(top = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            KgStep("−", "Minus 0.1 kg") { step(-1) }
            Row(
                Modifier.padding(horizontal = 10.dp).weight(1f).heightIn(min = 52.dp).clip(RoundedCornerShape(12.dp)).background(K.Surface2).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = text,
                    // At most 7 characters, "250.0" and a slip; anything else stays as typed, so nothing changes silently.
                    onValueChange = { new -> if (new.text.length <= 7) { text = new; touched = true } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(color = K.Text, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, fontFeatureSettings = TABULAR),
                    cursorBrush = SolidColor(K.Accent),
                    modifier = Modifier.width(IntrinsicSize.Min).widthIn(min = 48.dp).semantics { contentDescription = "Bodyweight in kilograms" }
                        .onFocusChanged { if (it.isFocused) text = text.copy(selection = TextRange(0, text.text.length)) },
                )
                Text("kg", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = K.Muted, modifier = Modifier.padding(start = 6.dp).clearAndSetSemantics {})
            }
            KgStep("+", "Plus 0.1 kg") { step(1) }
        }
        if (kg == null) Text(KG_ERROR, style = MaterialTheme.typography.bodySmall, color = K.Red, modifier = Modifier.padding(top = 6.dp))
        PrimaryButton(action.text, Modifier.padding(top = 12.dp), enabled = action.enabled, description = SPOKEN[action.text]) { save() }
    }
}

/** A 52 dp square − or + in accent: a tap steps once, a hold repeats ([repeatPress]). */
@Composable
private fun KgStep(glyph: String, label: String, onStep: () -> Unit) {
    Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(K.Surface2).repeatPress(label, onTap = onStep, onRepeat = onStep, onRelease = {}), contentAlignment = Alignment.Center) {
        Text(glyph, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = K.Accent)
    }
}
