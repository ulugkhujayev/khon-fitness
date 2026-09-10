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

/** Cardio session form: the fields come from the exercise, one row per work interval when it has intervals. */
@Composable
fun CardioScreen(vm: AppViewModel, nav: NavHostController, sessionId: String) {
    val session by vm.repo.session(sessionId).collectAsStateWithLifecycle(null)
    val intervals by vm.repo.intervalLogs(sessionId).collectAsStateWithLifecycle(emptyList())
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val s = session ?: return
    val ex = exercises.find { it.id == s.exerciseId }
    val fields = ex?.fieldList ?: listOf("time", "distance", "avgHr")
    var draft by remember(s.id) { mutableStateOf(s) }
    var ivs by remember(intervals.size) { mutableStateOf(intervals) }
    var strokeSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        EditorTopBar(if (s.finished) "History" else "Today", s.exerciseName ?: "Cardio", onBack = { if (!nav.popBackStack()) nav.navigate(Routes.TODAY) }, done = "Save") {
            vm.run { vm.repo.updateSession(draft.copy(finishedAt = draft.finishedAt ?: System.currentTimeMillis())); vm.repo.saveIntervals(ivs) }
            nav.popBackStack()
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            GroupedList {
                DateRow(draft.date.toDate(), divider = false) { d -> draft = draft.copy(date = d.iso()) }
                fields.forEachIndexed { i, f ->
                    val divider = true
                    when (f) {
                        "time" -> NumberRow("Time", draft.timeSec?.let { it / 60.0 }, "min", 1.0, 1, divider) { draft = draft.copy(timeSec = it?.times(60)?.toInt()) }
                        "distance" -> NumberRow("Distance", draft.distanceM?.let { it / 1000.0 }, "km", 0.1, 1, divider) { draft = draft.copy(distanceM = it?.times(1000)?.toInt()) }
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
                                NumberField(iv.avgHr?.toDouble(), { v -> ivs = ivs.map { if (it.id == iv.id) it.copy(avgHr = v?.toInt()) else it } }, 1.0, "bpm", Modifier.width(176.dp), 0, 38)
                                Spacer(Modifier.height(6.dp))
                                NumberField(iv.watts?.toDouble(), { v -> ivs = ivs.map { if (it.id == iv.id) it.copy(watts = v?.toInt()) else it } }, 5.0, "W", Modifier.width(176.dp), 0, 38)
                            }
                        }
                    }
                }
            }
            SectionTitle("Notes", Modifier.padding(top = 24.dp))
            GroupedList {
                FieldRow("Effort", divider = false) { NumberField(draft.effort?.toDouble(), { draft = draft.copy(effort = it?.toInt()?.coerceIn(1, 10)) }, 1.0, "/10", Modifier.width(176.dp), 0, 38, placeholder = "1–10") }
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
fun NumberRow(label: String, value: Double?, unit: String, step: Double, decimals: Int, divider: Boolean, set: (Double?) -> Unit) {
    FieldRow(label, divider = divider) { NumberField(value, set, step, unit, Modifier.width(176.dp), decimals, 38) }
}
