package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.DayOfWeek
import java.time.LocalDate

/** A day as a tile on the group surface: the week strip on Today, the month calendar on History (DESIGN.md, Today). */
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
 * Monday to Sunday of this week: each day a tile with the weekday letter, the number and a dot for the planned
 * item, past days muted, today filled in accent with a white dot. Tap a day to see or change it.
 */
@Composable
fun WeekStrip(today: LocalDate, plan: KhonRepository.PlanData, modalities: List<Modality>, onTap: (LocalDate) -> Unit) {
    val monday = today.with(DayOfWeek.MONDAY)
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until 7) {
            val date = monday.plusDays(i.toLong())
            val item = plan.itemFor(date)
            val c = itemColor(item, modalities)
            val isToday = date == today
            val past = date < today
            val dateDescription = "${date.format(longDate)}, ${item.name}" + if (isToday) ", today" else ""
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(DayTileShape).background(if (isToday) K.Accent else K.Surface)
                    .clickable(onClickLabel = "Change plan", role = Role.Button) { onTap(date) }
                    .clearAndSetSemantics { contentDescription = dateDescription }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(date.dayOfWeek.name.take(1), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = if (isToday) K.AccentInk else K.Muted, maxLines = 1, softWrap = false)
                Text(
                    date.dayOfMonth.toString(), style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR),
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium, color = when { isToday -> K.AccentInk; past -> K.Muted; else -> K.Text },
                    maxLines = 1, softWrap = false, modifier = Modifier.padding(top = 2.dp),
                )
                Box(Modifier.padding(top = 4.dp).height(8.dp), contentAlignment = Alignment.Center) {
                    if (c != null) Dot(if (isToday) K.AccentInk else if (past) c.first.copy(alpha = 0.5f) else c.first, c.second, size = 7)
                }
            }
        }
    }
}

/**
 * Today's item with its kind as a symbol in its color: a running session with the time since its start, its sets
 * done (a program) and Resume; else the item with Start, or its done line with Start again once today's session
 * is saved. Rest shows its line only.
 */
@Composable
fun TodayCard(
    item: PlanItem,
    line: String,
    unfinished: Session?,
    progress: SetProgress?,
    doneText: String?,
    modalities: List<Modality>,
    exercises: List<Exercise>,
    now: Long,
    onStart: () -> Unit,
    onResume: (Session) -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(GroupShape).background(K.Surface).padding(horizontal = 16.dp, vertical = 14.dp)) {
        when {
            unfinished != null -> {
                CardHead(
                    if (unfinished.itemType == ItemType.PROGRAM) Icons.Dumbbell else Icons.Runner, sessionColor(unfinished, exercises, modalities).first,
                    unfinished.programName ?: unfinished.exerciseName ?: "Session", progressLine(unfinished.startedAt, now), K.Accent,
                )
                if (progress != null) Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProgressBar(progress.done, progress.total, "${progress.done} of ${progress.total} sets done", Modifier.weight(1f), color = K.Accent)
                    Text("${progress.done} / ${progress.total}", style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR), color = K.Muted, modifier = Modifier.padding(start = 12.dp).clearAndSetSemantics {})
                }
                PrimaryButton("Resume", Modifier.padding(top = 14.dp), compact = true) { onResume(unfinished) }
            }
            doneText != null -> CardHead(Icons.Check, K.Green, item.name, doneText, K.Green) {
                Box(Modifier.padding(top = 2.dp)) { TextButton("Start again", onClick = onStart) }
            }
            else -> {
                CardHead(itemIcon(item), itemColor(item, modalities)?.first ?: K.Muted, item.name, line)
                if (!item.isRest) PrimaryButton("Start", Modifier.padding(top = 14.dp), compact = true, onClick = onStart)
            }
        }
    }
}

/** The symbol, then the name over its line (accent while a session runs, green once done) and anything under them. */
@Composable
private fun CardHead(icon: ImageVector, color: Color, title: String, line: String, lineColor: Color = K.Muted, below: (@Composable ColumnScope.() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SymbolCircle(icon, color)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(line, style = MaterialTheme.typography.bodyMedium, color = lineColor, modifier = Modifier.padding(top = 1.dp))
            below?.invoke(this)
        }
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

/** The chart covers the last 30 days, today included. */
private const val CHART_DAYS = 30L

/** Bodyweight on Today: the latest value, a chart of the last 30 days, and the entry with its ruler. */
@Composable
fun BodyweightSection(today: LocalDate, bodyweights: List<Bodyweight>, onSave: (Double) -> Unit) {
    val latest = bodyweights.lastOrNull()
    Row(Modifier.fillMaxWidth().padding(top = 28.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Bodyweight", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(end = 12.dp).semantics { heading() })
        Text(
            if (latest == null) "—" else "${fmt(latest.kg)} kg", style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TABULAR),
            modifier = if (latest == null) Modifier.semantics { contentDescription = "No entry yet" } else Modifier,
        )
    }
    val from = today.minusDays(CHART_DAYS - 1)
    val recent = bodyweights.filter { it.date.toDate() >= from }
    if (recent.isNotEmpty()) LineChart(recent.map { ChartPoint(it.date.toDate(), it.kg) }, "kg", Modifier.padding(top = 8.dp))
    BodyweightEntry(today, bodyweights, onSave)
}

/** What a screen reader says for each state of the entry button. */
private val SPOKEN = mapOf("Add" to "Add bodyweight", "Update" to "Update today's bodyweight", "Saved" to "Today's bodyweight is saved")

/**
 * Today's bodyweight entry on its own card, apart from the chart above: the picked value as a big figure (tap to
 * type), its change since the last day weighed, a ruler to drag, and a tinted Add, Update or Saved. The ruler
 * starts at the latest entry and stays on the saved value after a save. While the typed text is blank or not above 0
 * the button is off, so a tap never saves the value from before.
 */
@Composable
fun BodyweightEntry(today: LocalDate, bodyweights: List<Bodyweight>, onSave: (Double) -> Unit) {
    var kg by remember { mutableDoubleStateOf(startKg(bodyweights)) }
    var touched by remember { mutableStateOf(false) }
    // The entries load after the first frame: start at the latest one until the user picks a value.
    LaunchedEffect(bodyweights.lastOrNull()) { if (!touched) kg = startKg(bodyweights) }
    var valid by remember { mutableStateOf(true) }
    val pick: (Double) -> Unit = { kg = it; touched = true }
    val change = changeSince(bodyweights, today, kg)
    val action = entryAction(bodyweights, today, kg, valid)
    Column(Modifier.padding(top = 24.dp).fillMaxWidth().clip(GroupShape).background(K.Surface).padding(vertical = 14.dp)) {
        FigureField(kg, pick, "kg", "Bodyweight in kilograms", Modifier.padding(horizontal = 16.dp), onValid = { valid = it })
        if (change.isNotEmpty()) Text(
            change, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR), color = K.Muted, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        // The ruler runs to the card's edges and fades into it.
        RulerPicker(BODYWEIGHT_RULER, { kg }, pick, "Bodyweight ruler", "kg", K.Surface, Modifier.padding(top = 10.dp))
        TintedButton(action.text, Modifier.padding(top = 12.dp).padding(horizontal = 16.dp), enabled = action.enabled, description = SPOKEN[action.text]) { if (action.enabled) onSave(kg) }
    }
}
