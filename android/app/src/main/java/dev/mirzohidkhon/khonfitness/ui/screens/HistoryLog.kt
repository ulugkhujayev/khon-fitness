package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weekday: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE", Locale.US)

/** Today's row in the Log: the accent at 12 percent over the group surface. */
private val TodayTint = K.Accent.copy(alpha = 0.12f)

/**
 * The Log page: the week header with its arrows, the week card (planned days done, a 7-part bar, totals), and one
 * row per session or planned day, Monday to Sunday. The week shown is saved with the tab; this week stays this week
 * when [today] moves past a Sunday midnight.
 */
@Composable
fun HistoryLog(vm: AppViewModel, nav: NavHostController, today: LocalDate) {
    val data = rememberHistoryData(vm, today)
    val (sessions, logs, stretchSessions, bodyweights) = data
    val plan by vm.planData.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val weeks = logWeeks(sessions, today)
    // The Monday shown as an epoch day; null is this week.
    var shown by rememberSaveable { mutableStateOf<Long?>(null) }
    val monday = (shown?.let(LocalDate::ofEpochDay) ?: weeks.endInclusive).coerceIn(weeks.start, weeks.endInclusive)
    fun show(m: LocalDate) { shown = if (m >= weeks.endInclusive) null else m.toEpochDay() }
    val planFor: (LocalDate) -> PlanItem = { historyPlanFor(plan, it, today) }
    val histories = remember(logs, sessions) { liftHistories(logs, sessions) }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        WeekArrow(Icons.ChevronLeft, "Previous week", monday > weeks.start) { show(monday.minusWeeks(1)) }
        Text(weekTitle(monday), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        WeekArrow(Icons.ChevronRight, "Next week", monday < weeks.endInclusive) { show(monday.plusWeeks(1)) }
    }
    Spacer(Modifier.height(6.dp))
    val week = weekSummary(monday, today, sessions, logs, planFor, stretchSessions)
    val tail = weekTail(week)
    Column(
        Modifier.fillMaxWidth().clip(GroupShape).background(K.Surface).padding(16.dp, 14.dp, 16.dp, 14.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${week.done} of ${week.planned} planned done", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (tail.isNotEmpty()) Text(tail, style = MaterialTheme.typography.bodyMedium, color = K.Muted, modifier = Modifier.padding(start = 8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp).clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            weekBar(monday, sessions, planFor).forEach { (part, s) ->
                val c = when (part) { BarPart.DONE -> sessionColor(s!!, exercises, modalities).first; BarPart.OPEN -> K.Surface3; BarPart.REST -> K.Bg }
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(c))
            }
        }
        Text(weekTotals(week), style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR), color = K.Muted)
    }
    Spacer(Modifier.height(12.dp))
    val rows = logRows(monday, today, sessions, planFor)
    GroupedList {
        rows.forEachIndexed { i, row ->
            // A day's stretch runs go under its last row.
            val stretches = if (rows.getOrNull(i + 1)?.date == row.date) emptyList() else stretchLines(row.date, stretchSessions)
            LogRowView(row, i > 0 && rows[i - 1].date == row.date, i > 0, row.date == today, logs, exercises, modalities, histories, bodyweights, stretches, nav)
        }
    }
}

/** ‹ or › beside the week title: a 48 dp target, dim at the first or last week. */
@Composable
private fun WeekArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = if (enabled) K.Accent else K.Surface3, modifier = Modifier.size(20.dp))
    }
}

/** Under a day without a session: its bodyweight, "83.0 kg". */
private fun dayWeight(date: LocalDate, bodyweights: List<Bodyweight>): String? =
    bodyweights.find { it.date == date.iso() }?.let { String.format(Locale.US, "%.1f kg", it.kg) }

@Composable
private fun LogRowView(
    row: LogRow, sameDay: Boolean, divider: Boolean, isToday: Boolean,
    logs: List<SetLog>, exercises: List<Exercise>, modalities: List<Modality>,
    histories: Map<String, List<LiftSession>>, bodyweights: List<Bodyweight>, stretches: List<String>, nav: NavHostController,
) {
    val s = row.session
    val planned = itemColor(row.item, modalities)?.first ?: K.Muted
    val extra = if (s == null) dayWeight(row.date, bodyweights) else null
    // Dot: filled in the item color for a session, a ring for rest and today's open item, gray for skipped or later.
    val dot: @Composable () -> Unit = {
        when (row.kind) {
            DayKind.DONE, DayKind.RUNNING -> Dot(sessionColor(s!!, exercises, modalities).first, true, size = 10)
            DayKind.REST -> Dot(if (extra != null || stretches.isNotEmpty()) K.Green else K.Surface3, false, size = 10)
            DayKind.TODAY -> Dot(planned, false, size = 10)
            DayKind.SKIPPED -> Dot(K.Surface3, true, size = 10)
            DayKind.PLANNED -> Dot(K.Surface2, true, size = 10)
        }
    }
    val name = s?.let { it.programName ?: it.exerciseName ?: "Session" } ?: planName(row.item)
    val (title, titleColor) = when (row.kind) {
        DayKind.DONE, DayKind.RUNNING -> name to K.Text
        DayKind.REST -> "Rest" to K.Muted
        DayKind.SKIPPED -> "Skipped · $name" to K.Muted
        DayKind.TODAY -> "Today: $name" to K.Text
        DayKind.PLANNED -> "Planned: $name" to K.Muted
    }
    val records = if (row.kind == DayKind.DONE && s != null) recordCount(s.id, histories) else 0
    val line = when (row.kind) {
        DayKind.DONE -> buildAnnotatedString {
            append(sessionLine(s!!, logs, exercises.find { it.id == s.exerciseId }))
            if (records > 0) { append(" · "); withStyle(SpanStyle(color = K.Green, fontWeight = FontWeight.SemiBold)) { append("$records PR") } }
        }
        DayKind.RUNNING -> buildAnnotatedString { append(if (s!!.itemType == ItemType.PROGRAM) "In progress · ${setsLine(s.id, logs)}" else "In progress") }
        DayKind.TODAY -> buildAnnotatedString { append(listOfNotNull("Not started", extra).joinToString(" · ")) }
        else -> extra?.let { buildAnnotatedString { append(it) } }
    }
    val onClick: (() -> Unit)? = when (row.kind) {
        DayKind.DONE, DayKind.RUNNING -> { { openSession(nav, s!!) } }
        // History's state is saved for its tab; no restoreState here: popUpTo maps the saved History stack to Today too,
        // so restoring it would bring History back.
        DayKind.TODAY -> { { nav.navigate(Routes.TODAY) { popUpTo(Routes.TODAY) { saveState = true }; launchSingleTop = true } } }
        else -> null
    }
    Column(Modifier.fillMaxWidth().then(if (isToday) Modifier.background(TodayTint) else Modifier)) {
        if (divider) HorizontalDivider(Modifier.padding(start = if (sameDay) 66.dp else 16.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier.semantics(mergeDescendants = true) {})
                .padding(start = 16.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(40.dp)) {
                Text(row.date.format(weekday), fontSize = 12.sp, color = K.Muted, lineHeight = 15.sp)
                Text(row.date.dayOfMonth.toString(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (isToday) K.Accent else K.Text, lineHeight = 18.sp,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR))
            }
            Box(Modifier.width(22.dp), contentAlignment = Alignment.CenterStart) { dot() }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (line != null) Text(line, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR), color = K.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                stretches.forEach { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR), color = K.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp)) }
            }
            if (onClick != null) Chevron()
        }
    }
}
