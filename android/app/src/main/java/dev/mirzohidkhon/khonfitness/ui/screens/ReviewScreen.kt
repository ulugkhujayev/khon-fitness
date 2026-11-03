package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.LeaveIfSessionMissing
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import kotlinx.coroutines.flow.map

/**
 * A saved session from Today's Review, read-only: the date over its stats, then each exercise with its sets (a set
 * not done reads "skipped"), or a cardio session's values. No Edit and no Delete here; History's entry keeps them.
 */
@Composable
fun ReviewScreen(vm: AppViewModel, nav: NavHostController, sessionId: String) {
    val sessionState by remember(sessionId) { vm.repo.session(sessionId).map { it to true } }.collectAsStateWithLifecycle(null to false)
    val session = sessionState.first
    LeaveIfSessionMissing(nav, "review/{id}", sessionId, loaded = sessionState.second, missing = session == null)
    val logs by vm.repo.setLogs(sessionId).collectAsStateWithLifecycle(emptyList())
    val intervals by vm.repo.intervalLogs(sessionId).collectAsStateWithLifecycle(emptyList())
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val s = session ?: return
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        EditorTopBar("Today", s.programName ?: s.exerciseName ?: "Session", scrollPx = { scroll.value }, onBack = { nav.popBackStack() })
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            Text(s.date.toDate().format(shortDate), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(statLine(s, logs), style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR), color = K.Muted, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))
            if (s.itemType == ItemType.PROGRAM) reviewExercises(logs).forEach { sets -> ExerciseReview(sets) }
            else CardioReview(s, intervals, exercises.find { it.id == s.exerciseId }?.let { ex -> modalities.find { it.id == ex.modalityId }?.name })
            if (s.note.isNotBlank() || s.effort != null) {
                SectionTitle("Notes", Modifier.padding(top = 12.dp))
                GroupedList {
                    s.effort?.let { ReviewRow("Effort", "$it / 10", divider = false) }
                    if (s.note.isNotBlank()) ReviewRow("Note", s.note, divider = s.effort != null)
                }
            }
        }
    }
}

/** A program session's sets, one list per exercise, in the order the session ran them. */
fun reviewExercises(logs: List<SetLog>): List<List<SetLog>> =
    groupBlocks(logs).flatMap { block -> block.groupBy { it.slot }.toSortedMap().values.map { sets -> sets.sortedBy { it.setIndex } } }

/** A set as Review shows it: "80 kg × 10" when done, else "skipped". */
fun reviewSet(log: SetLog): String =
    if (log.status == SetStatus.DONE) "${fmt(log.weightKg ?: 0.0, 2)} kg × ${log.reps ?: 0}" else "skipped"

@Composable
private fun ExerciseReview(sets: List<SetLog>) {
    GroupedList(Modifier.padding(bottom = 12.dp)) {
        Text(sets.first().exerciseName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp))
        sets.forEachIndexed { i, log ->
            Row(Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(horizontal = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Set ${i + 1}", style = MaterialTheme.typography.bodyMedium, color = K.Muted, modifier = Modifier.width(56.dp))
                val done = log.status == SetStatus.DONE
                Text(reviewSet(log), style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR), color = if (done) K.Text else K.Dim)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CardioReview(s: Session, intervals: List<IntervalLog>, type: String?) {
    val rows = listOfNotNull(
        type?.let { "Type" to it },
        s.timeSec?.let { "Time" to "${it / 60} min" + if (it % 60 > 0) " ${it % 60} s" else "" },
        s.distanceM?.let { "Distance" to "${fmt(it / 1000.0, 2)} km" },
        paceLine(s.timeSec, s.distanceM)?.let { "Pace" to it },
        s.avgHr?.let { "Avg heart rate" to "$it bpm" },
        s.watts?.let { "Avg watts" to "$it W" },
        s.laps?.let { "Laps" to "$it" },
        s.poolLength?.takeIf { s.laps != null }?.let { "Pool length" to "$it m" },
        s.stroke?.let { "Stroke" to it },
    )
    if (rows.isNotEmpty()) GroupedList { rows.forEachIndexed { i, (k, v) -> ReviewRow(k, v, divider = i > 0) } }
    if (intervals.isNotEmpty()) {
        SectionTitle("Intervals", Modifier.padding(top = 24.dp))
        GroupedList {
            intervals.forEachIndexed { i, iv ->
                ReviewRow("Round ${i + 1}", listOfNotNull(iv.avgHr?.let { "$it bpm" }, iv.watts?.let { "$it W" }).joinToString(" · ").ifEmpty { "—" }, divider = i > 0)
            }
        }
    }
}

/** A label and its value, nothing to tap. */
@Composable
private fun ReviewRow(label: String, value: String, divider: Boolean) {
    FieldRow(label, divider = divider) { Text(value, style = MaterialTheme.typography.bodyLarge, color = K.Text) }
}
