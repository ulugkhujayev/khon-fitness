package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import dev.mirzohidkhon.khonfitness.ui.LeaveIfSessionMissing
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.map

/** Cardio session form: the fields come from the exercise, one row per work interval when it has intervals. */
@Composable
fun CardioScreen(vm: AppViewModel, nav: NavHostController, sessionId: String) {
    val sessionState by remember(sessionId) { vm.repo.session(sessionId).map { it to true } }.collectAsStateWithLifecycle(null to false)
    val session = sessionState.first
    LeaveIfSessionMissing(nav, "cardio/{id}", sessionId, loaded = sessionState.second, missing = session == null)
    val intervals by vm.repo.intervalLogs(sessionId).collectAsStateWithLifecycle(emptyList())
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val s = session ?: return
    val ex = exercises.find { it.id == s.exerciseId }
    // Current exercise fields, plus any value this session already stores: a field removed later must not hide old data.
    val stored = listOfNotNull("time".takeIf { s.timeSec != null }, "distance".takeIf { s.distanceM != null }, "avgHr".takeIf { s.avgHr != null },
        "watts".takeIf { s.watts != null }, "laps".takeIf { s.laps != null }, "poolLength".takeIf { s.poolLength != null }, "stroke".takeIf { s.stroke != null })
    val current = ex?.fieldList ?: listOf("time", "distance", "avgHr")
    val fields = current + stored.filter { it !in current }
    // The row as the form opened it: Save writes only the fields that differ from it ([KhonRepository.saveCardioEdits]).
    val opened = remember(s.id) { s }
    var draft by remember(s.id) { mutableStateOf(opened) }
    // Each interval as the form first saw it, by id (a round that arrives later joins with its first value), and the
    // user's edits over the current rows: Save writes only what differs. A change in the round count keeps both.
    val openedById = remember(s.id) { HashMap<String, IntervalLog>() }
    intervals.forEach { openedById.putIfAbsent(it.id, it) }
    var edits by remember(s.id) { mutableStateOf(mapOf<String, IntervalLog>()) }
    val ivs = intervals.map { edits[it.id] ?: it }
    fun edit(id: String, change: (IntervalLog) -> IntervalLog) { ivs.find { it.id == id }?.let { edits = edits + (id to change(it)) } }
    var strokeSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        val barScroll = rememberScrollState()
        EditorTopBar(if (s.finished) "History" else "Today", s.exerciseName ?: "Cardio", scrollPx = { barScroll.value }, onBack = { if (!nav.popBackStack()) nav.navigate(Routes.TODAY) }, done = "Save") {
            vm.run { vm.repo.saveCardioEdits(opened, draft, ivs.mapNotNull { openedById[it.id] }, ivs) }
            nav.popBackStack()
        }
        Column(Modifier.weight(1f).verticalScroll(barScroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            GroupedList {
                DateRow(draft.date.toDate(), divider = false) { d -> draft = draft.copy(date = d.iso()) }
                fields.forEachIndexed { i, f ->
                    val divider = true
                    when (f) {
                        "time" -> NumberRow("Time", draft.timeSec?.let { it / 60.0 }, "min", 1.0, 1, divider) { draft = draft.copy(timeSec = it?.let { scaled(it, 60.0) }) }
                        "distance" -> NumberRow("Distance", draft.distanceM?.let { it / 1000.0 }, "km", 0.1, 1, divider) { draft = draft.copy(distanceM = it?.let { scaled(it, 1000.0) }) }
                        "avgHr" -> NumberRow("Avg heart rate", draft.avgHr?.toDouble(), "bpm", 1.0, 0, divider) { draft = draft.copy(avgHr = it?.toInt()) }
                        "watts" -> NumberRow("Avg watts", draft.watts?.toDouble(), "W", 5.0, 0, divider) { draft = draft.copy(watts = it?.toInt()) }
                        "laps" -> NumberRow("Laps", draft.laps?.toDouble(), "", 1.0, 0, divider) { draft = draft.copy(laps = it?.toInt()) }
                        "poolLength" -> NumberRow("Pool length", (draft.poolLength ?: 25).toDouble(), "m", 5.0, 0, divider) { draft = draft.copy(poolLength = it?.toInt()) }
                        "stroke" -> FieldRow("Stroke", divider = divider, onClick = { strokeSheet = true }) { Text(draft.stroke ?: "Freestyle", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold); Chevron() }
                    }
                }
                if ("distance" in fields && (draft.distanceM ?: 0) > 0 && (draft.timeSec ?: 0) > 0) {
                    val paceSec = draft.timeSec!! / (draft.distanceM!! / 1000.0)
                    FieldRow("Pace") { Text("${(paceSec / 60).toInt()}:${"%02d".format((paceSec % 60).toInt())} /km", color = K.Muted) }
                }
                if ("laps" in fields && (draft.laps ?: 0) > 0) {
                    val dist = (draft.laps ?: 0) * (draft.poolLength ?: 25)
                    FieldRow("Distance") { Text("$dist m", color = K.Muted) }
                    if ((draft.timeSec ?: 0) > 0) { val p = draft.timeSec!! / (dist / 100.0); FieldRow("Pace") { Text("${(p / 60).toInt()}:${"%02d".format((p % 60).toInt())} /100 m", color = K.Muted) } }
                }
            }
            if (ivs.isNotEmpty()) {
                SectionTitle("Intervals", Modifier.padding(top = 24.dp))
                GroupedList {
                    ivs.forEachIndexed { i, iv ->
                        FieldRow("Round ${i + 1}", divider = i > 0) {
                            Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                NumberField(iv.avgHr?.toDouble(), { v -> edit(iv.id) { it.copy(avgHr = v?.toInt()) } }, 1.0, "bpm", Modifier.width(176.dp), 0, 38)
                                Spacer(Modifier.height(6.dp))
                                NumberField(iv.watts?.toDouble(), { v -> edit(iv.id) { it.copy(watts = v?.toInt()) } }, 5.0, "W", Modifier.width(176.dp), 0, 38)
                            }
                        }
                    }
                }
            }
            SectionTitle("Notes", Modifier.padding(top = 24.dp))
            GroupedList {
                FieldRow("Effort", divider = false) { NumberField(draft.effort?.toDouble(), { draft = draft.copy(effort = it?.toInt()) }, 1.0, "/10", Modifier.width(176.dp), 0, 38, placeholder = "1–10", min = 1.0, max = 10.0) }
                FieldRow("Note") { InlineTextField(draft.note, { draft = draft.copy(note = it) }, placeholder = "Optional") }
            }
            Spacer(Modifier.height(24.dp))
            TextButton(if (s.finished) "Delete session" else "Discard", K.Red) { confirmDelete = true }
        }
    }
    if (strokeSheet) StrokeSheet(draft.stroke, { draft = draft.copy(stroke = it); strokeSheet = false }) { strokeSheet = false }
    if (confirmDelete) ConfirmSheet(
        if (s.finished) "Delete this session?" else "Discard this session?",
        if (s.finished) "It goes away from History." else "Nothing is kept.",
        if (s.finished) "Delete" else "Discard",
        onDismiss = { confirmDelete = false },
    ) { confirmDelete = false; vm.run { vm.repo.deleteSession(s.id) }; if (!nav.popBackStack()) nav.navigate(Routes.TODAY) }
}

@Composable
private fun StrokeSheet(current: String?, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    Sheet("Stroke", onDismiss) { ChoiceList { listOf("Freestyle", "Breaststroke", "Backstroke", "Butterfly", "Mixed").forEachIndexed { i, st -> ChoiceRow(st, (current ?: "Freestyle") == st, divider = i > 0) { onPick(st) } } } }
}

@Composable
fun NumberRow(label: String, value: Double?, unit: String, step: Double, decimals: Int, divider: Boolean, min: Double = 0.0, max: Double = Double.POSITIVE_INFINITY, set: (Double?) -> Unit) {
    FieldRow(label, divider = divider) { NumberField(value, set, step, unit, Modifier.width(176.dp), decimals, 38, min = min, max = max) }
}
