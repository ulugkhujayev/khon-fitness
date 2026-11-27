package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mirzohidkhon.khonfitness.data.Exercise
import dev.mirzohidkhon.khonfitness.data.HrFacts
import dev.mirzohidkhon.khonfitness.data.IntervalLog
import dev.mirzohidkhon.khonfitness.data.Session
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.components.ChartPoint
import dev.mirzohidkhon.khonfitness.ui.components.GroupedList
import dev.mirzohidkhon.khonfitness.ui.components.LineChart
import dev.mirzohidkhon.khonfitness.ui.components.SectionTitle
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.ZoneId

const val SCORE_GROUP_TAG = "score-group"
const val HARD_CHART_TAG = "score-hard-chart"
const val ZONE_CHART_TAG = "score-zone-chart"

@Composable
fun rememberMeasuredMax(vm: AppViewModel): Int? {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val rows by vm.allSessionHr.collectAsStateWithLifecycle()
    val zone = ZoneId.systemDefault()
    val clock = LocalWallClock.current
    var resumedAt by remember { mutableLongStateOf(clock()) }
    LifecycleResumeEffect(clock) { resumedAt = clock(); onPauseOrDispose { } }
    return remember(sessions, rows, zone, resumedAt) { measuredMaxHr(sessions, rows, clock(), zone) }
}

@Composable
fun rememberMaxInUse(vm: AppViewModel): Int? {
    val typed by vm.typedMaxHr.collectAsStateWithLifecycle()
    return maxInUse(typed, rememberMeasuredMax(vm))
}

@Composable
fun ReviewScores(vm: AppViewModel, s: Session, exercise: Exercise?, facts: HrFacts, rounds: List<IntervalLog>) {
    val max = rememberMaxInUse(vm)
    val rows = remember(s, exercise, facts, rounds, max) { scoreRows(s, exercise, facts, rounds, max) }
    if (rows.isEmpty()) return
    SectionTitle("Heart rate", Modifier.padding(top = 24.dp))
    GroupedList(Modifier.testTag(SCORE_GROUP_TAG)) { rows.forEachIndexed { i, r -> ScoreRowView(r, divider = i > 0) } }
}

@Composable
private fun ScoreRowView(r: ScoreRow, divider: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        if (divider) HorizontalDivider(Modifier.padding(start = 16.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics(mergeDescendants = true) { contentDescription = r.spoken }.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(r.label, style = MaterialTheme.typography.bodyLarge, color = K.Text, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(r.value, style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR), color = K.Text)
        }
    }
}

@Composable
fun ScoreChartsView(vm: AppViewModel, sessions: List<Session>, exercises: Map<String, Exercise>, facts: Map<String, HrFacts>, title: @Composable (String) -> Unit) {
    val max = rememberMaxInUse(vm)
    val charts = remember(sessions, exercises, facts, max) { scoreCharts(sessions, exercises, facts, max) }
    if (charts.hardMinutes.isNotEmpty()) ScoreChart("At or above $HARD_PERCENT %", charts.hardMinutes, "min", HARD_CHART_TAG, 0.0..Double.POSITIVE_INFINITY, title) { minSec(Math.round(it * 60)) }
    if (charts.zonePercent.isNotEmpty()) ScoreChart("In zone", charts.zonePercent, "%", ZONE_CHART_TAG, 0.0..100.0, title) { "${Math.round(it)}" }
}

@Composable
private fun ScoreChart(name: String, points: List<ChartPoint>, unit: String, tag: String, domain: ClosedFloatingPointRange<Double>, title: @Composable (String) -> Unit, format: (Double) -> String) {
    title(name)
    LineChart(points, unit, Modifier.testTag(tag), format = format, domain = domain)
}
