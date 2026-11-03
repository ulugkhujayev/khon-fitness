package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.Clock

/**
 * Every strength exercise, searchable (Progress' "All <n> exercises"): name and record, or "No sets yet". Each row
 * opens the exercise detail.
 */
@Composable
fun LiftListScreen(vm: AppViewModel, nav: NavHostController, clock: Clock = Clock.systemDefaultZone()) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val data = rememberHistoryData(vm, rememberToday(clock))
    val records = remember(data, exercises) { personalRecords(liftHistories(data.logs, data.sessions), exercises) }
    var query by rememberSaveable { mutableStateOf("") }
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        EditorTopBar("Back", "Exercises", onBack = { nav.popBackStack() }, scrollPx = { scroll.value })
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            SearchField(query, { query = it })
            Spacer(Modifier.height(16.dp))
            val list = liftEntries(records, exercises, data, query)
            GroupedList {
                list.forEachIndexed { i, e ->
                    // An archived exercise reads dim and says so, as in the library.
                    val secondary = listOfNotNull(if (e.archived) "Archived" else null, e.record?.let { setText(it.best) } ?: "No sets yet").joinToString(" · ")
                    ListRow(e.name, secondary = secondary, divider = i > 0, titleColor = if (e.archived) K.Dim else K.Text) { nav.navigate(Routes.exerciseDetail(e.exerciseId)) }
                }
                if (list.isEmpty()) SearchEmpty(query)
            }
        }
    }
}

/**
 * One exercise's progress: the Best card with the heaviest set per session (records green), then every session
 * newest first with every saved set (the record set green, skipped ones marked), each opening the session. An
 * exercise without sets shows its muscle and "No sets yet".
 */
@Composable
fun ExerciseDetailScreen(vm: AppViewModel, nav: NavHostController, exerciseId: String, clock: Clock = Clock.systemDefaultZone()) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val data = rememberHistoryData(vm, rememberToday(clock))
    val history = remember(data, exerciseId) { liftHistory(exerciseId, data.logs, data.sessions) }
    val ex = exercises.find { it.id == exerciseId }
    val name = ex?.name ?: data.logs.firstOrNull { it.exerciseId == exerciseId }?.exerciseName ?: "Exercise"
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        EditorTopBar("Back", name, onBack = { nav.popBackStack() }, scrollPx = { scroll.value })
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            val muscles = ex?.let { listOfNotNull(it.primaryMuscle) + it.secondaryList }.orEmpty().joinToString(", ") { it.replaceFirstChar(Char::uppercase) }
            if (muscles.isNotEmpty()) Text(muscles, style = MaterialTheme.typography.bodyMedium, color = K.Muted, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            if (history.isEmpty()) {
                GroupedList { EmptyLine("No sets yet") }
                return@Column
            }
            // Sessions where every set was skipped list below, with no best and no chart point.
            val record = history.lastOrNull { it.record }
            val recordBest = record?.best
            if (record != null && recordBest != null) Column(Modifier.fillMaxWidth().clip(GroupShape).background(K.Surface).padding(16.dp, 14.dp, 16.dp, 12.dp)) {
                Text("Best", style = MaterialTheme.typography.bodySmall, color = K.Muted)
                Text(
                    buildAnnotatedString {
                        append(setText(recordBest))
                        withStyle(SpanStyle(color = K.Muted, fontWeight = FontWeight.Medium, fontSize = 16.sp)) { append(" · ${monthDay(record.session.date.toDate())}") }
                    },
                    fontSize = 24.sp, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR),
                )
                Text("Heaviest set per session", style = MaterialTheme.typography.bodySmall, color = K.Muted, modifier = Modifier.padding(top = 12.dp))
                LineChart(history.mapNotNull { h -> h.best?.let { ChartPoint(h.session.date.toDate(), it.weightKg ?: 0.0, mark = h.record) } }, "kg")
            }
            SectionTitle("Sessions · newest first", Modifier.padding(top = if (record != null) 24.dp else 0.dp))
            GroupedList {
                val firstRecord = history.firstOrNull { it.record }
                history.asReversed().forEachIndexed { i, h ->
                    val first = h === firstRecord
                    val sets = buildAnnotatedString {
                        h.sets.forEachIndexed { k, l ->
                            if (k > 0) append(" · ")
                            when {
                                h.record && l === h.best -> withStyle(SpanStyle(color = K.Green, fontWeight = FontWeight.SemiBold)) { append(shortSet(l)) }
                                l.status == SetStatus.SKIPPED -> withStyle(SpanStyle(color = K.Dim)) { append(shortSet(l)) }
                                else -> append(shortSet(l))
                            }
                        }
                    }
                    SessionRow(h.session.date.toDate().format(shortDate), sets, chip = if (h.record && !first) "PR" else null, divider = i > 0) { openSession(nav, h.session) }
                }
            }
        }
    }
}

/**
 * One cardio type over all time: the charts the old History showed per cardio exercise (pace, average heart rate,
 * heart rate in work intervals, else time), then every session newest first. Swim pace is per 100 m.
 */
@Composable
fun CardioTypeScreen(vm: AppViewModel, nav: NavHostController, typeId: String, clock: Clock = Clock.systemDefaultZone()) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val sessions = rememberHistoryData(vm, rememberToday(clock)).sessions
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val intervalLogs by vm.allIntervalLogs.collectAsStateWithLifecycle()
    val type = typeId.takeIf { it != NO_TYPE } ?: ""
    val byId = exercises.associateBy { it.id }
    val cs = sessions.filter { it.finished && it.itemType == ItemType.CARDIO && (byId[it.exerciseId]?.modalityId ?: "") == type }
        .sortedWith(compareBy({ it.date }, { it.startedAt }))
    val swim = type == "swim"
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        EditorTopBar("Back", modalities.find { it.id == type }?.name ?: "Other", onBack = { nav.popBackStack() }, scrollPx = { scroll.value })
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            if (cs.isEmpty()) { GroupedList { EmptyLine("No sessions yet") }; return@Column }
            val pace = cs.mapNotNull { s ->
                val dist = meters(s); val t = s.timeSec
                if (dist == null || dist <= 0 || t == null || t <= 0) null else ChartPoint(s.date.toDate(), t / (dist / if (swim) 100.0 else 1000.0) / 60.0)
            }
            if (pace.isNotEmpty()) {
                ChartTitle("Pace ${if (swim) "/100 m" else "/km"}")
                LineChart(pace, "", format = { m -> val sec = Math.round(m * 60); "%d:%02d".format(sec / 60, sec % 60) })
            }
            val hr = cs.mapNotNull { s -> s.avgHr?.let { ChartPoint(s.date.toDate(), it.toDouble()) } }
            if (hr.isNotEmpty()) { ChartTitle("Average heart rate"); LineChart(hr, "bpm", decimals = 0) }
            val work = cs.filter { byId[it.exerciseId]?.intervals == true }.mapNotNull { s ->
                intervalLogs.filter { it.sessionId == s.id && it.avgHr != null }.map { it.avgHr!! }.takeIf { it.isNotEmpty() }?.let { ChartPoint(s.date.toDate(), it.average()) }
            }
            if (work.isNotEmpty()) { ChartTitle("Heart rate in work intervals"); LineChart(work, "bpm", decimals = 0) }
            val time = cs.mapNotNull { s -> s.timeSec?.let { ChartPoint(s.date.toDate(), it / 60.0) } }
            if (pace.isEmpty() && hr.isEmpty() && time.isNotEmpty()) { ChartTitle("Time, min"); LineChart(time, "min", decimals = 0) }
            SectionTitle("Sessions · newest first", Modifier.padding(top = 24.dp))
            GroupedList {
                cs.asReversed().forEachIndexed { i, s ->
                    val line = "${s.exerciseName ?: byId[s.exerciseId]?.name ?: "Cardio"} · ${sessionLine(s, emptyList(), byId[s.exerciseId])}"
                    SessionRow(s.date.toDate().format(shortDate), buildAnnotatedString { append(line) }, divider = i > 0) { openSession(nav, s) }
                }
            }
        }
    }
}

@Composable
private fun ChartTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = K.Muted, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
}

/** A session in a detail list: the date (with a [chip]) over its [line], a chevron; opens the session. */
@Composable
private fun SessionRow(date: String, line: androidx.compose.ui.text.AnnotatedString, chip: String? = null, divider: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        if (divider) HorizontalDivider(Modifier.padding(start = 16.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(start = 16.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (chip != null) { Spacer(Modifier.width(6.dp)); LabelChip(chip) }
                }
                Text(line, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR), color = K.Muted, modifier = Modifier.padding(top = 2.dp))
            }
            Chevron()
        }
    }
}
