package dev.mirzohidkhon.khonfitness.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K

private val switchColors @Composable get() = SwitchDefaults.colors(checkedTrackColor = K.Green, checkedThumbColor = Color.White, uncheckedTrackColor = K.Surface3, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent)

/** Routine list: one active. */
@Composable
fun RoutinesScreen(vm: AppViewModel, nav: NavHostController) {
    val routines by vm.stretchRoutines.collectAsStateWithLifecycle()
    val routineStretches by vm.routineStretches.collectAsStateWithLifecycle()
    val stretches by vm.stretches.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        EditorTopBar("Programs", "Stretching", onBack = { nav.popBackStack() }, done = "+") {
            val id = newId(); vm.run { vm.repo.saveRoutine(StretchRoutine(id, "New routine", active = routines.isEmpty(), sortOrder = routines.size)) }; nav.navigate(Routes.routine(id))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            GroupedList {
                routines.forEachIndexed { i, r ->
                    val secs = routineSeconds(r.id, routineStretches, stretches)
                    ListRow(r.name, secondary = "${(secs + 30) / 60} min · ${routineStretches.count { it.routineId == r.id }} stretches", dotColor = K.Green, dotFilled = r.active, divider = i > 0) { nav.navigate(Routes.routine(r.id)) }
                }
                if (routines.isEmpty()) ListRow("No routines yet", chevron = false, divider = false, titleColor = K.Muted)
            }
            Spacer(Modifier.height(24.dp))
            GroupedList { ListRow("Stretch library", secondary = "${stretches.count { !it.archived }}", divider = false) { nav.navigate(Routes.STRETCHES) } }
        }
    }
}

/** Routine editor: name, active, the ordered stretches with seconds, add from the library. */
@Composable
fun RoutineEditorScreen(vm: AppViewModel, nav: NavHostController, routineId: String) {
    val routines by vm.stretchRoutines.collectAsStateWithLifecycle()
    val routineStretches by vm.routineStretches.collectAsStateWithLifecycle()
    val stretches by vm.stretches.collectAsStateWithLifecycle()
    val routine = routines.find { it.id == routineId } ?: return
    val mine = routineStretches.filter { it.routineId == routineId }.sortedBy { it.sortOrder }
    var picking by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        EditorTopBar("Stretching", routine.name, onBack = { nav.popBackStack() }) { nav.popBackStack() }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            GroupedList {
                FieldRow("Name", divider = false) { InlineTextField(routine.name, { vm.run { vm.repo.saveRoutine(routine.copy(name = it)) } }) }
                FieldRow("Active") { Switch(routine.active, { on -> vm.run { vm.repo.saveRoutine(routine.copy(active = on)) } }, colors = switchColors) }
            }
            SectionTitle("Stretches", Modifier.padding(top = 20.dp)) { PlusButton { picking = true } }
            GroupedList {
                mine.forEachIndexed { i, rs ->
                    val st = stretches.find { it.id == rs.stretchId }
                    val name = st?.name ?: "Missing stretch"
                    val sec = rs.seconds ?: st?.seconds ?: 45
                    FieldRow(name, divider = i > 0) {
                        Text(if (st?.sided == true) "per side" else "", color = K.Muted, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(8.dp))
                        NumberField(sec.toDouble(), { v -> vm.run { vm.repo.saveRoutineStretch(rs.copy(seconds = (v ?: 45.0).toInt().coerceIn(5, 600))) } }, 5.0, "s", Modifier.width(176.dp), 0, 38)
                    }
                }
                if (mine.isEmpty()) ListRow("No stretches yet", chevron = false, divider = false, titleColor = K.Muted)
            }
            if (mine.isNotEmpty()) {
                SectionTitle("Order", Modifier.padding(top = 20.dp))
                GroupedList {
                    mine.forEachIndexed { i, rs ->
                        val st = stretches.find { it.id == rs.stretchId }
                        ListRow(st?.name ?: "Missing stretch", chevron = false, divider = i > 0, trailing = {
                            TextButton("↑", enabled = i > 0) { vm.run { vm.repo.saveRoutineStretches(listOf(rs.copy(sortOrder = i - 1), mine[i - 1].copy(sortOrder = i))) } }
                            TextButton("↓", enabled = i < mine.size - 1) { vm.run { vm.repo.saveRoutineStretches(listOf(rs.copy(sortOrder = i + 1), mine[i + 1].copy(sortOrder = i))) } }
                            TextButton("Remove", color = K.Red) { vm.run { vm.repo.deleteRoutineStretch(rs.id) } }
                        })
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            TextButton("Delete routine", color = K.Red) { confirmDelete = true }
        }
    }
    if (picking) Sheet("Add a stretch", { picking = false }) {
        ChoiceList {
            stretches.filter { !it.archived }.forEachIndexed { i, st ->
                ChoiceRow(st.name, mine.any { it.stretchId == st.id }, divider = i > 0) {
                    vm.run { vm.repo.saveRoutineStretch(RoutineStretch(newId(), routineId, st.id, mine.size)) }; picking = false
                }
            }
        }
    }
    if (confirmDelete) ConfirmSheet("Delete this routine?", "Its stretch history stays.", "Delete", { confirmDelete = false }) { vm.run { vm.repo.deleteRoutine(routineId) }; nav.popBackStack() }
}

/** The stretch library: figure thumbnail, name, sides, a search field. */
@Composable
fun StretchLibraryScreen(vm: AppViewModel, nav: NavHostController) {
    val stretches by vm.stretches.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        EditorTopBar("Stretching", "Stretches", onBack = { nav.popBackStack() }, done = "+") { nav.navigate(Routes.stretchEditor("new")) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            SearchField(query, { query = it })
            Spacer(Modifier.height(16.dp))
            val list = stretches.filter { !it.archived && (query.isBlank() || it.name.contains(query.trim(), true)) }
            GroupedList {
                list.forEachIndexed { i, st ->
                    ListRow(st.name, secondary = listOfNotNull(if (st.sided) "per side" else null, if (st.mode == StretchMode.REPS) "${st.reps} reps" else "${st.seconds} s").joinToString(" · "), divider = i > 0,
                        trailing = { Figure(st.figure, Modifier.size(36.dp).padding(end = 6.dp), color = K.Muted, animate = false) }) { nav.navigate(Routes.stretchEditor(st.id)) }
                }
                if (list.isEmpty()) ListRow("Nothing matches", chevron = false, divider = false, titleColor = K.Muted)
            }
        }
    }
}

/** Stretch editor: name, sides, mode, seconds or reps, figure, muscles, cue, reference link. */
@Composable
fun StretchEditorScreen(vm: AppViewModel, nav: NavHostController, id: String) {
    val stretches by vm.stretches.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val existing = stretches.find { it.id == id }
    var draft by remember(existing?.id) { mutableStateOf(existing ?: Stretch(newId(), "", figure = "hipflexor")) }
    var figureSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var useCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(id) { if (existing != null) useCount = vm.repo.stretchUseCount(id) }
    fun save() { vm.run { vm.repo.saveStretch(draft) } }
    Column(Modifier.fillMaxSize()) {
        EditorTopBar("Stretches", if (existing == null) "New stretch" else "Stretch", onBack = { nav.popBackStack() }) { save(); nav.popBackStack() }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { Figure(draft.figure, Modifier.size(160.dp)) }
            GroupedList {
                FieldRow("Name", divider = false) { InlineTextField(draft.name, { draft = draft.copy(name = it) }, placeholder = "Stretch name") }
                FieldRow("Figure", onClick = { figureSheet = true }) { Text(draft.figure, fontWeight = FontWeight.SemiBold); Chevron() }
                FieldRow("Per side") { Switch(draft.sided, { draft = draft.copy(sided = it) }, colors = switchColors) }
                FieldRow("Reps") { Switch(draft.mode == StretchMode.REPS, { draft = draft.copy(mode = if (it) StretchMode.REPS else StretchMode.HOLD, reps = if (it && draft.reps == 0) 10 else draft.reps) }, colors = switchColors) }
                if (draft.mode == StretchMode.REPS) FieldRow("Rep count") { NumberField(draft.reps.toDouble(), { draft = draft.copy(reps = (it ?: 1.0).toInt().coerceIn(1, 50)) }, 1.0, "", Modifier.width(150.dp), 0, 38) }
                FieldRow(if (draft.mode == StretchMode.REPS) "Time" else "Hold") { NumberField(draft.seconds.toDouble(), { draft = draft.copy(seconds = (it ?: 45.0).toInt().coerceIn(5, 600)) }, 5.0, "s", Modifier.width(176.dp), 0, 38) }
                FieldRow("Muscles") { InlineTextField(draft.muscles, { draft = draft.copy(muscles = it) }, placeholder = "hamstrings, glutes") }
                FieldRow("Cue") { InlineTextField(draft.cue, { draft = draft.copy(cue = it) }, placeholder = "How to do it") }
                FieldRow("Link") { InlineTextField(draft.link, { draft = draft.copy(link = it) }, placeholder = "YouTube URL") }
            }
            if (draft.link.isNotBlank()) { Spacer(Modifier.height(16.dp)); GroupedList { ListRow("Watch the reference", divider = false) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(draft.link))) } } } }
            if (existing != null) {
                Spacer(Modifier.height(28.dp))
                if (existing.builtin || useCount > 0) Text(if (existing.builtin) "Built-in stretches cannot be deleted." else "In use by $useCount routine rows.", color = K.Dim, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, bottom = 4.dp))
                TextButton("Delete stretch", color = K.Red, enabled = !existing.builtin && useCount == 0) { confirmDelete = true }
            }
        }
    }
    if (figureSheet) Sheet("Figure", { figureSheet = false }) {
        ChoiceList { Figures.keys.forEachIndexed { i, k -> ChoiceRow(k, draft.figure == k, divider = i > 0) { draft = draft.copy(figure = k); figureSheet = false } } }
    }
    if (confirmDelete) ConfirmSheet("Delete this stretch?", "", "Delete", { confirmDelete = false }) { vm.run { vm.repo.deleteStretch(id) }; nav.popBackStack() }
}
