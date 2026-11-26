package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.components.GroupedList
import dev.mirzohidkhon.khonfitness.ui.components.ListRow
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.Clock
import java.time.LocalDate

private val DEFAULT_LEFT = PeriodChoice.Fixed(Preset.BEFORE_4)
private val DEFAULT_RIGHT = PeriodChoice.Fixed(Preset.LAST_4)

/**
 * Two periods side by side: the two period rows, each opening a sheet of presets, programs and custom dates, then
 * the facts of both periods as list groups, the left period first in every row. Facts only, the same color in both
 * columns. The two choices are saved with the screen.
 */
@Composable
fun CompareScreen(vm: AppViewModel, nav: NavHostController, clock: Clock = Clock.systemDefaultZone()) {
    val today = rememberToday(clock)
    val data = rememberHistoryData(vm, today)
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val programs by vm.programs.collectAsStateWithLifecycle()
    var leftKey by rememberSaveable { mutableStateOf(choiceKey(DEFAULT_LEFT)) }
    var rightKey by rememberSaveable { mutableStateOf(choiceKey(DEFAULT_RIGHT)) }
    var editing by rememberSaveable { mutableIntStateOf(-1) }

    fun resolve(key: String, fallback: PeriodChoice): Pair<PeriodChoice, Period> {
        val c = parseChoice(key)?.takeIf { choicePeriod(it, today, data.sessions) != null } ?: fallback
        return c to choicePeriod(c, today, data.sessions)!!
    }
    val (leftChoice, left) = resolve(leftKey, DEFAULT_LEFT)
    val (rightChoice, right) = resolve(rightKey, DEFAULT_RIGHT)
    val choices = remember(programs, data) { programChoices(programs, data.sessions) }
    fun label(c: PeriodChoice) = when (c) {
        is PeriodChoice.Fixed -> c.preset.label
        is PeriodChoice.OfProgram -> programs.find { it.id == c.programId }?.name ?: "Program"
        is PeriodChoice.Custom -> "Custom dates"
    }

    val groups = remember(left, right, data, exercises, modalities) {
        fun stats(p: Period) = periodStats(p, data.sessions, data.logs, exercises, data.stretches, data.bodyweights)
        compareGroups(stats(left), stats(right), exercises, { id -> modalities.find { it.id == id }?.name ?: "Other" }, modalities.map { it.id })
    }
    val leftRange = periodRange(left, today)
    val rightRange = periodRange(right, today)
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        EditorTopBar("Back", "Compare", onBack = { nav.popBackStack() }, scrollPx = { scroll.value })
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            GroupedList {
                ListRow(leftRange, secondary = label(leftChoice), divider = false) { editing = 0 }
                ListRow(rightRange, secondary = label(rightChoice)) { editing = 1 }
            }
            FactGroups(groups, leftRange, rightRange)
        }
    }

    if (editing >= 0) {
        val isLeft = editing == 0
        PeriodSheet(
            title = if (isLeft) "Left column" else "Right column",
            current = if (isLeft) leftChoice else rightChoice,
            period = if (isLeft) left else right,
            today = today,
            programs = choices,
            onPick = { c, close ->
                if (isLeft) leftKey = choiceKey(c) else rightKey = choiceKey(c)
                if (close) editing = -1
            },
            onDismiss = { editing = -1 },
        )
    }
}

private val ValueStyle @Composable get() = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = TABULAR)

/**
 * A value cell breaks only between a set's weight and its reps ("80 kg" over "× 6") and before a pace's unit. Line
 * breaking never breaks before a slash, also after a space, so a zero-width space marks that break.
 */
private fun cellText(v: String) = v.replace(" × ", "\u0001").replace("\u00A0/", "\u0002").replace(' ', '\u00A0')
    .replace("\u0001", " ×\u00A0").replace("\u0002", "\u200B\u00A0/")

private val CELL_GAP = 12.dp

/**
 * The fact groups. Both value columns take the width of the widest value on the screen, up to 30 percent of a row
 * each, so every row lines up and a label never moves them; a wider value wraps at its break. When a part that cannot
 * break is wider than that (a large font on a narrow phone), each row puts the label on a line of its own over the
 * two columns.
 */
@Composable
private fun FactGroups(groups: List<CompareGroup>, leftRange: String, rightRange: String) {
    val measurer = rememberTextMeasurer()
    val style = ValueStyle
    val density = LocalDensity.current
    val (widest, widestPart) = remember(groups, style, density) {
        val values = groups.flatMap { g -> g.rows.flatMap { listOf(it.left, it.right) } }.map(::cellText)
        fun width(t: String) = measurer.measure(t, style, softWrap = false).size.width
        (values.maxOfOrNull(::width) ?: 0) to (values.flatMap { it.split(" ", "\u200B") }.maxOfOrNull(::width) ?: 0)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cap = (maxWidth - 32.dp) * 0.3f
        val column = minOf(with(density) { widest.toDp() } + 2.dp, cap)
        val stacked = with(density) { widestPart.toDp() } + 2.dp > cap
        Column {
            groups.forEach { g ->
                if (g.title != null) ProgressTitle(g.title) else Spacer(Modifier.height(24.dp))
                GroupedList {
                    g.rows.forEachIndexed { i, row -> FactRow(row, column, stacked, compareSpoken(row, leftRange, rightRange), divider = i > 0) }
                }
            }
        }
    }
}

@Composable
private fun FactRow(row: CompareRow, column: Dp, stacked: Boolean, spoken: String, divider: Boolean) {
    Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken }) {
        if (divider) HorizontalDivider(Modifier.padding(start = 16.dp), color = K.Divider, thickness = 0.7.dp)
        if (stacked) Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)) {
            Text(row.label, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                Cell(row.left, Modifier.weight(1f))
                Cell(row.right, Modifier.padding(start = CELL_GAP).weight(1f))
            }
        } else Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(row.label, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
            Cell(row.left, Modifier.padding(start = CELL_GAP).width(column))
            Cell(row.right, Modifier.padding(start = CELL_GAP).width(column))
        }
    }
}

@Composable
private fun Cell(value: String, modifier: Modifier) {
    Text(cellText(value), style = ValueStyle, color = K.Text, textAlign = TextAlign.End, modifier = modifier.padding(vertical = 2.dp))
}

/**
 * The sheet of one column's period: one choice list of the presets, the programs with a finished session (name over
 * date range) and "Custom dates", which keeps the sheet open and shows From and To under it.
 */
@Composable
private fun PeriodSheet(
    title: String, current: PeriodChoice, period: Period, today: LocalDate, programs: List<Pair<dev.mirzohidkhon.khonfitness.data.Program, Period>>,
    onPick: (PeriodChoice, close: Boolean) -> Unit, onDismiss: () -> Unit,
) {
    Sheet(title, onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            ChoiceList {
                Preset.entries.forEachIndexed { i, p ->
                    ChoiceRow(p.label, current == PeriodChoice.Fixed(p), divider = i > 0) { onPick(PeriodChoice.Fixed(p), true) }
                }
                programs.forEach { (program, range) ->
                    TwoLineChoiceRow(program.name, periodRange(range, today), current == PeriodChoice.OfProgram(program.id)) { onPick(PeriodChoice.OfProgram(program.id), true) }
                }
                val custom = current as? PeriodChoice.Custom
                ChoiceRow("Custom dates", custom != null) { if (custom == null) onPick(PeriodChoice.Custom(period), false) }
                if (custom != null) {
                    DateRow(custom.period.from, label = "From") { onPick(PeriodChoice.Custom(withFrom(custom.period, it)), false) }
                    DateRow(custom.period.to, label = "To") { onPick(PeriodChoice.Custom(withTo(custom.period, it)), false) }
                }
            }
        }
    }
}

/** A [ChoiceRow] with a muted second line under its title. */
@Composable
private fun TwoLineChoiceRow(title: String, line: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(Modifier.padding(start = 16.dp), color = K.Divider, thickness = 0.7.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(selected, role = Role.RadioButton, onClick = onClick).padding(start = 16.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(line, style = MaterialTheme.typography.bodyMedium, color = K.Muted, modifier = Modifier.padding(top = 2.dp))
            }
            Text(if (selected) "✓" else "", color = K.Accent, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}
