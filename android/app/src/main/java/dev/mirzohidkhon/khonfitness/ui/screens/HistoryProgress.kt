package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.LocalDate
import java.util.Locale

/** Records shown before "All <n> exercises". */
private const val RECORDS_SHOWN = 5

/**
 * The Progress page: personal records (heaviest weight lifted), sets per muscle this week against last week,
 * bodyweight over a range, and cardio of the last 30 days by type. A section without data shows one line.
 */
@Composable
fun HistoryProgress(vm: AppViewModel, nav: NavHostController, today: LocalDate) {
    val data = rememberHistoryData(vm, today)
    val (sessions, logs, _, bodyweights) = data
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val monday = weekOf(today)

    // ---- Personal records
    val records = remember(logs, sessions, exercises) { personalRecords(liftHistories(logs, sessions), exercises) }
    val lifts = remember(records, exercises, data) { liftEntries(records, exercises, data).size }
    ProgressTitle("Personal records · heaviest weight lifted")
    GroupedList {
        if (records.isEmpty()) EmptyLine("No records yet. Save a gym session to see them.")
        records.take(RECORDS_SHOWN).forEachIndexed { i, r ->
            val isNew = r.session.date.toDate() in monday..today
            val date = monthDay(r.session.date.toDate())
            Column(Modifier.fillMaxWidth()) {
                if (i > 0) HorizontalDivider(Modifier.padding(start = 16.dp), color = K.Divider, thickness = 0.7.dp)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button) { nav.navigate(Routes.exerciseDetail(r.exerciseId)) }
                        .padding(start = 16.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(r.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Text(date, style = MaterialTheme.typography.bodySmall, color = K.Muted)
                            if (isNew) { Spacer(Modifier.width(6.dp)); LabelChip("New") }
                        }
                    }
                    Text(setText(r.best), style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    Chevron()
                }
            }
        }
        // Every strength exercise is one tap away, also one without sets.
        if (lifts > 0) {
            HorizontalDivider(Modifier.padding(start = 16.dp), color = K.Divider, thickness = 0.7.dp)
            Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { nav.navigate(Routes.LIFTS) }, contentAlignment = Alignment.Center) {
                Text("All $lifts exercises", color = K.Accent, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    // ---- Sets per muscle
    val volume = remember(logs, sessions, exercises, today) { muscleVolume(logs, sessions, exercises, today) }
    ProgressTitle("Sets per muscle · this week / last week")
    GroupedList {
        if (volume.isEmpty()) EmptyLine("No sets this week or last week.")
        else MuscleRows(volume)
    }

    // ---- Bodyweight
    var range by rememberSaveable { mutableStateOf(BodyRange.MONTH) }
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Bodyweight", style = MaterialTheme.typography.bodySmall, color = K.Muted, modifier = Modifier.weight(1f).padding(start = 16.dp).semantics { heading() })
        Row(Modifier.selectableGroup()) {
            BodyRange.entries.forEach { r ->
                val on = r == range
                Box(
                    Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).selectable(on, role = Role.Tab) { range = r }
                        .semantics { contentDescription = when (r) { BodyRange.MONTH -> "1 month"; BodyRange.QUARTER -> "3 months"; BodyRange.ALL -> "All time" } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(r.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (on) K.Text else K.Muted,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) K.Surface3 else Color.Transparent).padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
        }
    }
    val latest = latestBodyweight(bodyweights, today)
    Column(Modifier.fillMaxWidth().clip(GroupShape).background(K.Surface).padding(16.dp, 14.dp, 16.dp, 12.dp)) {
        if (latest == null) Text("No entries yet. Add one on Today.", style = MaterialTheme.typography.bodyMedium, color = K.Muted)
        else {
            val inRange = bodyweightsIn(range, bodyweights, today)
            val change = bodyweightChange(inRange)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${fmt(latest.kg)} kg", fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR), modifier = Modifier.padding(end = 8.dp))
                Spacer(Modifier.weight(1f))
                if (change != null) Text(change, style = MaterialTheme.typography.bodySmall, color = if (change.startsWith("+")) K.Muted else K.Green, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
            }
            if (inRange.isEmpty()) Text("No entries in this range", style = MaterialTheme.typography.bodyMedium, color = K.Muted, modifier = Modifier.padding(top = 12.dp))
            else LineChart(inRange.map { ChartPoint(it.date.toDate(), it.kg) }, "kg", Modifier.padding(top = 8.dp))
        }
    }

    // ---- Cardio by type: every type ever used, its last 30 days
    val types = cardioTypes(sessions, exercises, today, order = modalities.map { it.id })
    ProgressTitle("Cardio · last 30 days")
    GroupedList {
        if (types.isEmpty()) EmptyLine("No cardio in the last 30 days.")
        types.forEachIndexed { i, t ->
            val mod = modalities.find { it.id == t.id }
            CardioTypeRow(t, mod?.name ?: "Other", mod?.let { Color(it.color) } ?: K.Muted, divider = i > 0) { nav.navigate(Routes.cardioType(t.id.ifEmpty { NO_TYPE })) }
        }
    }
}

/** The route id of cardio whose exercise has no type. */
const val NO_TYPE = "none"

/** A section title over a group, in the grouped-list footnote style. */
@Composable
private fun ProgressTitle(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = K.Muted, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp).semantics { heading() })
}

/** The one line of a section without data. */
@Composable
fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = K.Muted, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 14.dp))
}

/** Thin bars per muscle: this week in accent over last week in gray, "8 / 6" at the end; a legend first. */
@Composable
private fun MuscleRows(volume: List<MuscleVolume>) {
    Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp).clearAndSetSemantics { contentDescription = "Accent bar this week, gray bar last week. Secondary muscles count half." }, verticalAlignment = Alignment.CenterVertically) {
        Legend(K.Accent, "This week"); Spacer(Modifier.width(14.dp)); Legend(K.Surface3, "Last week")
    }
    Text("Secondary muscles count half.", fontSize = 11.sp, color = K.Dim, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp).clearAndSetSemantics {})
    val shares = volumeShares(volume)
    // A large font gets a wider name column and two lines, so "Upper back" stays whole.
    val largeText = androidx.compose.ui.platform.LocalConfiguration.current.fontScale >= 1.5f
    Column(Modifier.padding(bottom = 8.dp)) {
        volume.forEachIndexed { i, m ->
            val name = m.muscle.replaceFirstChar { it.uppercase() }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(horizontal = 16.dp, vertical = 6.dp)
                    .clearAndSetSemantics { contentDescription = "$name, ${fmt(m.thisWeek)} sets this week, ${fmt(m.lastWeek)} last week" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(if (largeText) 112.dp else 88.dp))
                Column(Modifier.weight(1f).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    VolumeBar(shares[i].now, K.Accent)
                    VolumeBar(shares[i].last, K.Surface3)
                }
                Text("${fmt(m.thisWeek)} / ${fmt(m.lastWeek)}", style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR), color = K.Muted, textAlign = TextAlign.End, maxLines = 1, softWrap = false, modifier = Modifier.width(if (largeText) 96.dp else 60.dp))
            }
        }
    }
}

@Composable
private fun VolumeBar(share: Double, color: Color) {
    Box(Modifier.fillMaxWidth().height(6.dp)) {
        if (share > 0) Box(Modifier.fillMaxWidth(share.toFloat()).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color))
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(text, fontSize = 11.sp, color = K.Muted, modifier = Modifier.padding(start = 5.dp))
    }
}

/**
 * A cardio type of the last 30 days: dot, name, "n sessions · km · h m · pace", then the distance of each session as
 * a sparkline labeled "Distance per session" with the first and last value and their dates. Opens the type's detail.
 */
@Composable
private fun CardioTypeRow(t: CardioType, name: String, color: Color, divider: Boolean, onClick: () -> Unit) {
    val dist = t.sessions.mapNotNull { s -> meters(s)?.takeIf { it > 0 }?.let { s.date.toDate() to it / 1000.0 } }
    val line = cardioLine(t)
    Column(Modifier.fillMaxWidth()) {
        if (divider) HorizontalDivider(Modifier.padding(start = 42.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(26.dp).align(Alignment.Top).padding(top = 6.dp), contentAlignment = Alignment.CenterStart) { Dot(color, true, size = 10) }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                Text(line, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR), color = K.Muted, modifier = Modifier.padding(top = 2.dp))
                if (dist.isNotEmpty()) {
                    Text("Distance per session", fontSize = 11.sp, color = K.Dim, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    Sparkline(dist.map { it.second }, color, Modifier.fillMaxWidth().height(28.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                        Text("${fmt(dist.first().second, 1)} km · ${monthDay(dist.first().first)}", fontSize = 11.sp, color = K.Muted, modifier = Modifier.weight(1f))
                        if (dist.size > 1) Text("${fmt(dist.last().second, 1)} km · ${monthDay(dist.last().first)}", fontSize = 11.sp, color = K.Muted)
                    }
                }
            }
            Chevron()
        }
    }
}

/** A line through [values] from left to right, no axes; one value draws a dot. */
@Composable
private fun Sparkline(values: List<Double>, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val lo = values.min(); val hi = values.max()
        val pad = 3.dp.toPx()
        fun y(v: Double) = if (hi == lo) size.height / 2 else pad + ((hi - v) / (hi - lo) * (size.height - 2 * pad)).toFloat()
        fun x(i: Int) = if (values.size == 1) size.width / 2 else pad + i * (size.width - 2 * pad) / (values.size - 1)
        if (values.size == 1) { drawCircle(color, 3.dp.toPx(), Offset(x(0), y(values[0]))); return@Canvas }
        val path = Path().apply { moveTo(x(0), y(values[0])); for (i in 1 until values.size) lineTo(x(i), y(values[i])) }
        drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, 2.5.dp.toPx(), Offset(x(0), y(values[0])))
        drawCircle(color, 2.5.dp.toPx(), Offset(x(values.size - 1), y(values.last())))
    }
}
