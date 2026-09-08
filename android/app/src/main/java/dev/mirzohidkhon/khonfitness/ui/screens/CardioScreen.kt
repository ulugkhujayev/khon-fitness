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

    Column(Modifier.fillMaxSize()) {
        EditorTopBar(if (s.finished) "History" else "Today", s.exerciseName ?: "Cardio", onBack = { nav.popBackStack() }, done = "Save") {
            vm.run { vm.repo.updateSession(draft.copy(finishedAt = draft.finishedAt ?: System.currentTimeMillis())); vm.repo.saveIntervals(ivs) }
            nav.popBackStack()
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            Text(s.date.toDate().format(longDate), color = K.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
            GroupedList {
                var first = true
                fun row(label: String, value: Double?, unit: String, step: Double, decimals: Int, set: (Double?) -> Unit) {
                    // rendered below
                }
                fields.forEachIndexed { i, f ->
                    val divider = i > 0
                    when (f) {
                        "time" -> NumberRow("Time", (draft.timeSec ?: 0) / 60.0, "min", 1.0, 0, divider) { draft = draft.copy(timeSec = it?.times(60)?.toInt()) }
                        "distance" -> NumberRow("Distance", (draft.distanceM ?: 0) / 1000.0, "km", 0.1, 1, divider) { draft = draft.copy(distanceM = it?.times(1000)?.toInt()) }
                        "avgHr" -> NumberRow("Avg heart rate", draft.avgHr?.toDouble(), "bpm", 1.0, 0, divider) { draft = draft.copy(avgHr = it?.toInt()) }
                        "watts" -> NumberRow("Avg watts", draft.watts?.toDouble(), "W", 5.0, 0, divider) { draft = draft.copy(watts = it?.toInt()) }
                        "laps" -> NumberRow("Laps", draft.laps?.toDouble(), "", 1.0, 0, divider) { draft = draft.copy(laps = it?.toInt()) }
                        "poolLength" -> NumberRow("Pool length", (draft.poolLength ?: 25).toDouble(), "m", 5.0, 0, divider) { draft = draft.copy(poolLength = it?.toInt()) }
                        "stroke" -> FieldRow("Stroke", divider = divider) { InlineTextField(draft.stroke ?: "", { draft = draft.copy(stroke = it) }, placeholder = "Freestyle") }
                    }
                }
                if ("distance" in fields && "time" in fields && (draft.distanceM ?: 0) > 0 && (draft.timeSec ?: 0) > 0) {
                    val paceSec = draft.timeSec!! / (draft.distanceM!! / 1000.0)
                    FieldRow("Pace") { Text("${(paceSec / 60).toInt()}:${"%02d".format((paceSec % 60).toInt())} /km", color = K.Text) }
                }
                if ("laps" in fields && (draft.laps ?: 0) > 0) {
                    val dist = (draft.laps ?: 0) * (draft.poolLength ?: 25)
                    FieldRow("Distance") { Text("$dist m", color = K.Text) }
                }
            }
            if (ivs.isNotEmpty()) {
                SectionTitle("Intervals", Modifier.padding(top = 24.dp))
                GroupedList {
                    ivs.forEachIndexed { i, iv ->
                        FieldRow("Round ${i + 1}", divider = i > 0) {
                            NumberField(iv.avgHr?.toDouble(), { v -> ivs = ivs.map { if (it.id == iv.id) it.copy(avgHr = v?.toInt()) else it } }, 1.0, "bpm", Modifier.weight(1f), 0, 38)
                            Spacer(Modifier.width(8.dp))
                            NumberField(iv.watts?.toDouble(), { v -> ivs = ivs.map { if (it.id == iv.id) it.copy(watts = v?.toInt()) else it } }, 5.0, "W", Modifier.weight(1f), 0, 38)
                        }
                    }
                }
            }
            SectionTitle("Notes", Modifier.padding(top = 24.dp))
            GroupedList {
                FieldRow("Effort", divider = false) { NumberField(draft.effort?.toDouble(), { draft = draft.copy(effort = it?.toInt()?.coerceIn(1, 10)) }, 1.0, "/10", Modifier.width(176.dp), 0, 38) }
                FieldRow("Note") { InlineTextField(draft.note, { draft = draft.copy(note = it) }, placeholder = "Optional") }
            }
        }
    }
}

@Composable
fun NumberRow(label: String, value: Double?, unit: String, step: Double, decimals: Int, divider: Boolean, set: (Double?) -> Unit) {
    FieldRow(label, divider = divider) { NumberField(value, set, step, unit, Modifier.width(176.dp), decimals, 38) }
}
