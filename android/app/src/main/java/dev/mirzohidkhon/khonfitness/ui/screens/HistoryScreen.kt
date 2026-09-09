package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(vm: AppViewModel, nav: NavHostController) {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val plan by vm.planData.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val bodyweights by vm.bodyweights.collectAsStateWithLifecycle()
    val allLogs by vm.allSetLogs.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val intervalLogs by vm.allIntervalLogs.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    var cardioId by remember { mutableStateOf<String?>(null) }
    var pickCardio by remember { mutableStateOf(false) }
    val cardioEx = exercises.filter { it.kind == Kind.CARDIO }
    val cardioSessions = sessions.filter { it.itemType == ItemType.CARDIO && it.finished }
    val chosenCardio = cardioId ?: cardioSessions.firstOrNull()?.exerciseId ?: cardioEx.firstOrNull()?.id
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }
    var planDate by remember { mutableStateOf<LocalDate?>(null) }
    var exerciseId by remember { mutableStateOf<String?>(null) }
    var pickExercise by remember { mutableStateOf(false) }
    val strength = exercises.filter { it.kind == Kind.STRENGTH }
    val loggedIds = allLogs.filter { it.status == SetStatus.DONE }.map { it.exerciseId }.toSet()
    val chosenId = exerciseId ?: strength.firstOrNull { it.id in loggedIds }?.id ?: strength.firstOrNull()?.id

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 24.dp)) {
        ScreenTitle("History") { TextButton("Import") { nav.navigate(Routes.IMPORT) } }
        Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton("‹") { month = month.minusMonths(1) }
            Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            TextButton("›") { month = month.plusMonths(1) }
        }
        Row(Modifier.fillMaxWidth()) { listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, color = K.Dim, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) } }
        val first = month.atDay(1).with(DayOfWeek.MONDAY).let { if (it > month.atDay(1)) it.minusWeeks(1) else it }
        val sessionsByDate = sessions.filter { it.finished }.groupBy { it.date }
        var d = first
        while (d <= month.atEndOfMonth() || d.dayOfWeek != DayOfWeek.MONDAY) {
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until 7) {
                    val date = d
                    val inMonth = YearMonth.from(date) == month
                    val done = sessionsByDate[date.iso()]
                    val item = plan.itemFor(date)
                    val color: Pair<Color, Boolean>? = if (done != null) sessionColor(done.first(), exercises, modalities) else itemColor(item, modalities)
                    val planned = done == null
                    Column(
                        Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(10.dp)).background(if (date == selected) K.Surface2 else Color.Transparent).clickable { selected = date }.padding(top = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(if (date == today) K.Accent else Color.Transparent), contentAlignment = Alignment.Center) {
                            Text(date.dayOfMonth.toString(), fontSize = 15.sp, fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal, color = if (date == today) K.AccentInk else if (inMonth) K.Text else K.Dim)
                        }
                        Box(Modifier.padding(top = 4.dp).height(8.dp), contentAlignment = Alignment.Center) {
                            if (color != null) Box(Modifier.alpha(if (planned) 0.4f else 1f)) { Dot(color.first, color.second, size = if (planned) 5 else 7) }
                        }
                    }
                    d = d.plusDays(1)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(selected.format(longDate), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton("Plan") { planDate = selected }
        }
        Spacer(Modifier.height(8.dp))
        val daySessions = sessions.filter { it.date == selected.iso() }
        GroupedList {
            if (daySessions.isEmpty()) {
                val item = plan.itemFor(selected)
                val c = itemColor(item, modalities)
                ListRow(item.name, secondary = if (item.isRest) null else "planned", dotColor = c?.first ?: K.Dim, dotFilled = c?.second ?: false, chevron = false, divider = false)
            } else daySessions.forEachIndexed { i, s ->
                val c = sessionColor(s, exercises, modalities)
                val logs = allLogs.filter { it.sessionId == s.id && it.status == SetStatus.DONE }
                val secondary = if (s.itemType == ItemType.PROGRAM) "${logs.size} sets · ${fmt(logs.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }, 0)} kg" else listOfNotNull(s.timeSec?.let { "${it / 60} min" }, s.distanceM?.let { "${fmt(it / 1000.0)} km" }).joinToString(" · ")
                ListRow(s.programName ?: s.exerciseName ?: "Session", secondary = if (s.finished) secondary else "in progress", dotColor = c.first, dotFilled = c.second, divider = i > 0) {
                    nav.navigate(if (s.itemType == ItemType.PROGRAM) Routes.session(s.id) else Routes.cardio(s.id))
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Bodyweight", style = MaterialTheme.typography.titleMedium, color = K.Muted, modifier = Modifier.weight(1f))
            bodyweights.lastOrNull()?.let { Text("${fmt(it.kg)} kg", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        }
        if (bodyweights.isEmpty()) Text("No entries yet. Add one on Today.", color = K.Dim, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        else LineChart(bodyweights.map { ChartPoint(it.date.toDate(), it.kg) }, "kg", Modifier.padding(top = 8.dp), height = 170)
        Spacer(Modifier.height(24.dp))
        GroupedList { FieldRow("Exercise", divider = false, onClick = { pickExercise = true }) { Text(strength.find { it.id == chosenId }?.name ?: "Choose", fontWeight = FontWeight.SemiBold); Chevron() } }
        val progress = remember(allLogs, sessions, chosenId) { progressFor(chosenId, allLogs, sessions) }
        Text("Estimated 1RM", style = MaterialTheme.typography.titleMedium, color = K.Muted, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
        if (progress.isEmpty()) Text("No finished sets for this exercise yet.", color = K.Dim, style = MaterialTheme.typography.bodyMedium)
        else LineChart(progress.map { ChartPoint(it.date, it.e1rm) }, "kg", height = 190)
        if (progress.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            GroupedList { progress.asReversed().forEachIndexed { i, p -> ListRow(p.date.format(shortDate), secondary = "${fmt(p.topW, 1)} kg × ${p.topR} reps", chevron = false, divider = i > 0, titleColor = K.Muted) } }
        }

        // ---- Sets per muscle, this week against last week
        Spacer(Modifier.height(28.dp))
        Text("Sets per muscle", style = MaterialTheme.typography.titleMedium, color = K.Muted)
        Text("This week, with last week in grey. Secondary muscles count half.", color = K.Dim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
        val volume = remember(allLogs, sessions, exercises, today) { muscleVolume(allLogs, sessions, exercises, today) }
        if (volume.isEmpty()) Text("No finished sets this week or last.", color = K.Dim, style = MaterialTheme.typography.bodyMedium)
        else GroupedList {
            val max = volume.maxOf { maxOf(it.thisWeek, it.lastWeek) }.coerceAtLeast(1.0)
            volume.forEachIndexed { i, m ->
                Column(Modifier.fillMaxWidth()) {
                    if (i > 0) androidx.compose.material3.HorizontalDivider(Modifier.padding(start = 16.dp), color = K.Divider)
                    Row(Modifier.fillMaxWidth().padding(16.dp, 10.dp, 16.dp, 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(m.muscle.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(fmt(m.thisWeek, 1), fontWeight = FontWeight.Bold)
                        Text("  ${fmt(m.lastWeek, 1)}", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
                        Box(Modifier.fillMaxWidth(fraction = (m.thisWeek / max).toFloat().coerceIn(0.02f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(K.Accent))
                        Box(Modifier.padding(top = 3.dp).fillMaxWidth(fraction = (m.lastWeek / max).toFloat().coerceIn(0.02f, 1f)).height(4.dp).clip(RoundedCornerShape(2.dp)).background(K.Surface3))
                    }
                }
            }
        }

        // ---- Cardio trends
        Spacer(Modifier.height(28.dp))
        GroupedList { FieldRow("Cardio", divider = false, onClick = { pickCardio = true }) { Text(cardioEx.find { it.id == chosenCardio }?.name ?: "Choose", fontWeight = FontWeight.SemiBold); Chevron() } }
        val cs = cardioSessions.filter { it.exerciseId == chosenCardio }.sortedBy { it.date }
        val cex = cardioEx.find { it.id == chosenCardio }
        if (cs.isEmpty()) Text("No finished sessions for this exercise yet.", color = K.Dim, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
        else {
            val paceUnit = if (cex?.modalityId == "swim") "/100 m" else "/km"
            val pace = cs.mapNotNull { s ->
                val dist = s.distanceM ?: (s.laps?.let { it * (s.poolLength ?: 25) }); val t = s.timeSec
                if (dist == null || dist <= 0 || t == null || t <= 0) null else ChartPoint(s.date.toDate(), if (cex?.modalityId == "swim") t / (dist / 100.0) / 60.0 else t / (dist / 1000.0) / 60.0)
            }
            if (pace.isNotEmpty()) {
                Text("Pace $paceUnit", style = MaterialTheme.typography.titleMedium, color = K.Muted, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                LineChart(pace, "", height = 170, format = { m -> val sec = Math.round(m * 60); "%d:%02d".format(sec / 60, sec % 60) })
            }
            val hr = cs.mapNotNull { s -> s.avgHr?.let { ChartPoint(s.date.toDate(), it.toDouble()) } }
            if (hr.isNotEmpty()) {
                Text("Average heart rate", style = MaterialTheme.typography.titleMedium, color = K.Muted, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                LineChart(hr, "bpm", height = 170, decimals = 0)
            }
            if (cex?.intervals == true) {
                val work = cs.mapNotNull { s -> intervalLogs.filter { it.sessionId == s.id && it.avgHr != null }.map { it.avgHr!! }.takeIf { it.isNotEmpty() }?.let { ChartPoint(s.date.toDate(), it.average()) } }
                if (work.isNotEmpty()) {
                    Text("Heart rate in work intervals", style = MaterialTheme.typography.titleMedium, color = K.Muted, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                    LineChart(work, "bpm", height = 170, decimals = 0)
                }
            }
            val time = cs.mapNotNull { s -> s.timeSec?.let { ChartPoint(s.date.toDate(), it / 60.0) } }
            if (pace.isEmpty() && hr.isEmpty() && time.isNotEmpty()) {
                Text("Time, min", style = MaterialTheme.typography.titleMedium, color = K.Muted, modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
                LineChart(time, "min", height = 170, decimals = 0)
            }
        }
    }
    if (pickCardio) Sheet("Cardio exercise", { pickCardio = false }) {
        ChoiceList { cardioEx.forEachIndexed { i, e -> ChoiceRow(e.name, e.id == chosenCardio, exerciseColor(e, modalities), e.intensity == Intensity.HIGH, divider = i > 0) { cardioId = e.id; pickCardio = false } } }
    }
    planDate?.let { PlanSheet(vm, it, plan, modalities) { planDate = null } }
    if (pickExercise) Sheet("Exercise", { pickExercise = false }) {
        ChoiceList { strength.forEachIndexed { i, e -> ChoiceRow(e.name, e.id == chosenId, divider = i > 0) { exerciseId = e.id; pickExercise = false } } }
    }
}

fun sessionColor(s: Session, exercises: List<Exercise>, modalities: List<Modality>): Pair<Color, Boolean> =
    if (s.itemType == ItemType.PROGRAM) K.Accent to true
    else exercises.find { it.id == s.exerciseId }?.let { ex -> exerciseColor(ex, modalities) to (ex.intensity == Intensity.HIGH) } ?: (K.Muted to true)

data class MuscleVolume(val muscle: String, val thisWeek: Double, val lastWeek: Double)

/** Sets per muscle group for the current week and the one before. Primary muscle counts 1, each secondary 0.5. */
fun muscleVolume(logs: List<SetLog>, sessions: List<Session>, exercises: List<Exercise>, today: LocalDate): List<MuscleVolume> {
    val monday = today.with(DayOfWeek.MONDAY); val lastMonday = monday.minusWeeks(1)
    val dates = sessions.filter { it.finished }.associate { it.id to it.date.toDate() }
    val exById = exercises.associateBy { it.id }
    val acc = HashMap<String, DoubleArray>()
    for (l in logs) {
        if (l.status != SetStatus.DONE) continue
        val d = dates[l.sessionId] ?: continue
        val idx = when { d >= monday -> 0; d >= lastMonday -> 1; else -> continue }
        val ex = exById[l.exerciseId] ?: continue
        ex.primaryMuscle?.let { acc.getOrPut(it) { DoubleArray(2) }[idx] += 1.0 }
        ex.secondaryList.forEach { acc.getOrPut(it) { DoubleArray(2) }[idx] += 0.5 }
    }
    return acc.map { (m, v) -> MuscleVolume(m, v[0], v[1]) }.sortedByDescending { maxOf(it.thisWeek, it.lastWeek) }
}

data class ProgressPoint(val date: LocalDate, val e1rm: Double, val topW: Double, val topR: Int)

fun progressFor(exerciseId: String?, logs: List<SetLog>, sessions: List<Session>): List<ProgressPoint> {
    if (exerciseId == null) return emptyList()
    val finished = sessions.filter { it.finished }.associateBy { it.id }
    return logs.filter { it.exerciseId == exerciseId && it.status == SetStatus.DONE && it.sessionId in finished }
        .groupBy { it.sessionId }.mapNotNull { (sid, ls) ->
            val best = ls.maxByOrNull { epley(it.weightKg ?: 0.0, it.reps ?: 0) } ?: return@mapNotNull null
            val top = ls.maxWithOrNull(compareBy({ it.weightKg ?: 0.0 }, { it.reps ?: 0 })) ?: best
            ProgressPoint(finished[sid]!!.date.toDate(), Math.round(epley(best.weightKg ?: 0.0, best.reps ?: 0) * 2) / 2.0, top.weightKg ?: 0.0, top.reps ?: 0)
        }.sortedBy { it.date }
}
