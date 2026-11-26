package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.components.LineChart
import dev.mirzohidkhon.khonfitness.ui.theme.Inter
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle as DateStyle
import java.util.Locale

const val LOAD_CHART_TAG = "load-chart"
const val LOAD_GRID_TAG = "load-grid"
fun loadCellTag(date: LocalDate) = "load-cell-$date"
fun loadWeekTag(monday: LocalDate) = "load-week-$monday"

val LOAD_STEP_ALPHAS = listOf(0.30f, 0.50f, 0.75f, 1f)
private val CELL_HEIGHT = 14.dp
private val CELL_GAP = 2.dp
private val CELL_SHAPE = RoundedCornerShape(3.dp)
private val COLUMN_GAP = 6.dp

@Composable
fun rememberLoadYear(vm: AppViewModel, today: LocalDate): LoadYear {
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val rows by vm.allSessionHr.collectAsStateWithLifecycle()
    val max = rememberMaxInUse(vm)
    val zone = ZoneId.systemDefault()
    return remember(sessions, rows, max, today, zone) { loadYear(sessions, factsById(sessions, rows, zone), max, today) }
}

@Composable
fun LoadScreen(vm: AppViewModel, nav: NavHostController, clock: Clock = Clock.systemDefaultZone()) {
    val today = rememberToday(clock)
    val year = rememberLoadYear(vm, today)
    val chart = remember(year) { loadChart(year) }
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        EditorTopBar("Back", "Load", onBack = { nav.popBackStack() }, scrollPx = { scroll.value })
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            if (chart.isNotEmpty()) {
                ChartTitle("Load per week")
                LineChart(chart, "", Modifier.testTag(LOAD_CHART_TAG), decimals = 0)
            }
            LoadGrid(year, Modifier.padding(top = 24.dp))
        }
    }
}

private val gridText = TextStyle(
    fontFamily = Inter, color = K.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp,
    fontFeatureSettings = TABULAR, lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
)

@Composable
private fun widest(texts: List<String>): Dp {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return with(density) { texts.maxOf { measurer.measure(it, gridText).size.width }.toDp() }
}

@Composable
private fun LoadGrid(year: LoadYear, modifier: Modifier = Modifier) {
    val monthWidth = widest(Month.entries.map { it.getDisplayName(DateStyle.SHORT, Locale.ENGLISH) })
    val loadWidth = widest(listOf("0000") + year.weeks.mapNotNull { it.load?.toString() })
    val highest = year.highest
    Column(modifier.fillMaxWidth().testTag(LOAD_GRID_TAG), verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp).clearAndSetSemantics {}, verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(monthWidth + COLUMN_GAP))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                DayOfWeek.entries.forEach { d ->
                    Text(d.getDisplayName(DateStyle.NARROW, Locale.ENGLISH), style = gridText, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.width(loadWidth + COLUMN_GAP))
        }
        year.weeks.forEach { w -> WeekRow(w, highest, monthWidth, loadWidth) }
    }
}

@Composable
private fun WeekRow(week: LoadWeek, highest: Int?, monthWidth: Dp, loadWidth: Dp) {
    Row(
        Modifier.fillMaxWidth().testTag(loadWeekTag(week.monday)).clearAndSetSemantics { contentDescription = loadWeekSpoken(week) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(loadMonth(week).orEmpty(), style = gridText, maxLines = 1, softWrap = false, modifier = Modifier.width(monthWidth))
        Spacer(Modifier.width(COLUMN_GAP))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
            (0 until 7).forEach { i ->
                val day = week.days.getOrNull(i)
                if (day == null) Spacer(Modifier.weight(1f).height(CELL_HEIGHT))
                else DayCell(day, highest, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.width(COLUMN_GAP))
        Text(week.load?.toString().orEmpty(), style = gridText, textAlign = TextAlign.End, maxLines = 1, softWrap = false, modifier = Modifier.width(loadWidth))
    }
}

@Composable
private fun DayCell(day: LoadDay, highest: Int?, modifier: Modifier) {
    val base = modifier.height(CELL_HEIGHT).testTag(loadCellTag(day.date)).clip(CELL_SHAPE)
    val load = day.load
    Box(
        when {
            load != null -> base.background(K.Accent.copy(alpha = LOAD_STEP_ALPHAS[loadStep(load, highest ?: load) - 1]))
            day.empty -> base.background(K.Surface)
            else -> base.background(K.Surface).border(1.dp, K.Muted, CELL_SHAPE)
        },
    )
}
