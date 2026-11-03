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
    // One flow for the screen's life: a new one on each recomposition would start its query again.
    val intervals by remember(sessionId) { vm.repo.intervalLogs(sessionId) }.collectAsStateWithLifecycle(emptyList())
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val s = session ?: return
    val ex = exercises.find { it.id == s.exerciseId }
    // Current exercise fields, plus any value this session already stores: a field removed later must not hide old data.
    val stored = listOfNotNull("time".takeIf { s.timeSec != null }, "distance".takeIf { s.distanceM != null }, "avgHr".takeIf { s.avgHr != null },
        "watts".takeIf { s.watts != null }, "laps".takeIf { s.laps != null }, "poolLength".takeIf { s.poolLength != null }, "stroke".takeIf { s.stroke != null })
    // Time always shows, so an exercise with only heart rate or watts still has a field that lets it save.
    val current = (ex?.fieldList ?: listOf("time", "distance", "avgHr")).let { if ("time" in it) it else listOf("time") + it }
    val fields = current + stored.filter { it !in current }
    // The row as the form opened it: Save writes only the fields that differ from it ([KhonRepository.saveCardioEdits]).
    val opened = remember(s.id) { s }
    var draft by remember(s.id) { mutableStateOf(opened) }
    // Only the interval fields the user typed, by interval id; every other value shows the current row, so an import
    // while the form is open shows up and Save writes only the typed fields. A change in the round count keeps them.
    var edits by remember(s.id) { mutableStateOf(mapOf<String, IntervalEdit>()) }
    fun edit(id: String, change: (IntervalEdit) -> IntervalEdit) { edits = edits + (id to change(edits[id] ?: IntervalEdit())) }
    var strokeSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Save, Discard and Back leave the form: a second tap in the same frame must not pop Today too.
    val leave = remember { Once() }
    // A new entry saves only with a time, a distance or laps, as the row would read after Save: the latest stored row
    // ([s], so a time the timer stores after the form opened counts) with the form's edits ([mergeCardioEdits]).
    // The data layer checks the same again when Save runs ([KhonDao.saveCardioEdits]); its refusal shows here.
    var refusal by remember(s.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(draft, s) { refusal = null }
    val missing = if (s.finished) null else cardioMissing(mergeCardioEdits(s, opened, draft, 0L))

    Column(Modifier.fillMaxSize()) {
        val barScroll = rememberScrollState()
        EditorTopBar(if (s.finished) "History" else "Today", s.exerciseName ?: "Cardio", scrollPx = { barScroll.value }, onBack = { leave { if (!nav.popBackStack()) nav.navigate(Routes.TODAY) } }, done = "Save", doneEnabled = missing == null) {
            leave {
                vm.run {
                    when (val r = vm.repo.saveCardioEdits(opened, draft, edits)) {
                        CardioSaveResult.Saved -> nav.popBackStack()
                        is CardioSaveResult.Refused -> { refusal = r.message; leave.release() }
                    }
                }
            }
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
                        "stroke" -> FieldRow("Stroke", divider = divider, onClick = { strokeSheet = true }) { FieldValue(draft.stroke ?: "Freestyle") }
                    }
                }
                if ("distance" in fields && (draft.distanceM ?: 0) > 0 && (draft.timeSec ?: 0) > 0) {
                    val paceSec = draft.timeSec!! / (draft.distanceM!! / 1000.0)
                    ComputedRow("Pace", "${(paceSec / 60).toInt()}:${"%02d".format((paceSec % 60).toInt())} /km")
                }
                if ("laps" in fields && (draft.laps ?: 0) > 0) {
                    val dist = (draft.laps ?: 0) * (draft.poolLength ?: 25)
                    ComputedRow("Distance", "$dist m")
                    if ((draft.timeSec ?: 0) > 0) { val p = draft.timeSec!! / (dist / 100.0); ComputedRow("Pace", "${(p / 60).toInt()}:${"%02d".format((p % 60).toInt())} /100 m") }
                }
            }
            (missing ?: refusal)?.let { GroupFooter(it, Modifier.padding(top = 8.dp)) }
            if (intervals.isNotEmpty()) {
                SectionTitle("Intervals", Modifier.padding(top = 24.dp))
                GroupedList {
                    intervals.forEachIndexed { i, iv ->
                        val e = edits[iv.id] ?: IntervalEdit()
                        FieldRow("Round ${i + 1}", divider = i > 0) {
                            Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                NumberField(e.avgHr.or(iv.avgHr)?.toDouble(), { v -> edit(iv.id) { it.copy(avgHr = Edit.Set(v?.toInt())) } }, 1.0, "bpm", width = 176.dp, decimals = 0, height = 38, label = "Round ${i + 1} heart rate")
                                Spacer(Modifier.height(6.dp))
                                NumberField(e.watts.or(iv.watts)?.toDouble(), { v -> edit(iv.id) { it.copy(watts = Edit.Set(v?.toInt())) } }, 5.0, "W", width = 176.dp, decimals = 0, height = 38, label = "Round ${i + 1} watts")
                            }
                        }
                    }
                }
            }
            SectionTitle("Notes", Modifier.padding(top = 24.dp))
            GroupedList {
                FieldRow("Effort", divider = false) { NumberField(draft.effort?.toDouble(), { draft = draft.copy(effort = it?.toInt()) }, 1.0, "/10", width = 176.dp, decimals = 0, height = 38, placeholder = "1–10", min = 1.0, max = 10.0, label = "Effort, 1 to 10") }
                FieldRow("Note") { InlineTextField(draft.note, { draft = draft.copy(note = it) }, placeholder = "Optional", label = "Note") }
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
    ) { leave { confirmDelete = false; vm.run { vm.repo.deleteSession(s.id) }; if (!nav.popBackStack()) nav.navigate(Routes.TODAY) } }
}

/** A value the form works out from other fields, in Muted: pace, swim distance. */
@Composable
private fun ComputedRow(label: String, value: String) {
    FieldRow(label) { Text(value, style = MaterialTheme.typography.bodyLarge, color = K.Muted) }
}

@Composable
private fun StrokeSheet(current: String?, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    Sheet("Stroke", onDismiss) { ChoiceList { listOf("Freestyle", "Breaststroke", "Backstroke", "Butterfly", "Mixed").forEachIndexed { i, st -> ChoiceRow(st, (current ?: "Freestyle") == st, divider = i > 0) { onPick(st) } } } }
}

@Composable
fun NumberRow(label: String, value: Double?, unit: String, step: Double, decimals: Int, divider: Boolean, min: Double = 0.0, max: Double = Double.POSITIVE_INFINITY, set: (Double?) -> Unit) {
    FieldRow(label, divider = divider) { NumberField(value, set, step, unit, width = 176.dp, decimals = decimals, height = 38, min = min, max = max) }
}
