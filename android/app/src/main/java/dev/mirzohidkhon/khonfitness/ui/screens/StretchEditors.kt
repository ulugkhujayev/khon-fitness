package dev.mirzohidkhon.khonfitness.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K

/** Stretching: the routines (the active one filled), then the stretch library. A new routine opens as a draft in its editor. */
@Composable
fun RoutinesScreen(vm: AppViewModel, nav: NavHostController) {
    val routines by vm.stretchRoutines.collectAsStateWithLifecycle()
    val routineStretches by vm.routineStretches.collectAsStateWithLifecycle()
    val stretches by vm.stretches.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        val barScroll = rememberScrollState()
        EditorTopBar("Programs", "Stretching", scrollPx = { barScroll.value }, onBack = { nav.popBackStack() }, done = "+", addLabel = "New routine") {
            nav.navigate(Routes.routine("new"))
        }
        Column(Modifier.weight(1f).verticalScroll(barScroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            SectionTitle("Routines")
            GroupedList {
                routines.forEachIndexed { i, r ->
                    ListRow(r.name, secondary = routineSummary(r.id, routineStretches, stretches), dotColor = K.Green, dotFilled = r.active, divider = i > 0) { nav.navigate(Routes.routine(r.id)) }
                }
                if (routines.isEmpty()) ListRow("No routines yet", chevron = false, divider = false, titleColor = K.Muted)
            }
            SectionTitle("Library", Modifier.padding(top = 24.dp))
            GroupedList {
                ListRow("Stretches", secondary = "${stretches.count { !it.archived }}", divider = false, leading = { IconTile(Icons.Stretch, K.Green) }) { nav.navigate(Routes.STRETCHES) }
            }
        }
    }
}

/**
 * Routine editor: name, Active and what it does, the stretches with their seconds, Add stretch, their order with up,
 * down and remove, Delete. A saved routine writes each edit as it is made. A new routine (`new`) is a draft
 * ([RoutineDraft]): nothing is saved until Done or its first stretch, so backing out leaves nothing behind.
 */
@Composable
fun RoutineEditorScreen(vm: AppViewModel, nav: NavHostController, routineId: String) {
    val routines by vm.stretchRoutines.collectAsStateWithLifecycle()
    val routineStretches by vm.routineStretches.collectAsStateWithLifecycle()
    val stretches by vm.stretches.collectAsStateWithLifecycle()
    val draft = rememberSaveable(saver = RoutineDraft.Saver) { RoutineDraft.open(routineId) }
    val id = draft.id
    // Saved or draft comes from the database: a new routine is a draft until the list holds its id.
    val routine = routines.find { it.id == id }
    if (!draft.isNew && routine == null) return
    val isDraft = draft.isDraft(routine)
    val noRoutines = routines.isEmpty()
    val mine = routineStretches.filter { it.routineId == id }.sortedWith(compareBy({ it.sortOrder }, { it.id }))
    var picking by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val removal = rememberConfirm<RoutineStretch>()
    fun name(rs: RoutineStretch) = stretches.find { it.id == rs.stretchId }?.name ?: "Missing stretch"
    fun pick(stretchId: String) {
        picking = false
        // In order with Done. Refused when the stretch went meanwhile: nothing is written and the editor stays a draft.
        // A pick while that create is pending finds the id stored and writes nothing.
        draft.forFirstStretch(routine, noRoutines)?.let { r -> vm.runSerial { vm.repo.createRoutine(r, stretchId) }; return }
        vm.run { vm.repo.addRoutineStretch(id, stretchId) }
    }
    fun done() {
        // After a pending first-stretch create: nothing when it saved the routine, the routine without stretches when refused.
        draft.done(routine, noRoutines)?.let { r -> vm.runSerial { vm.repo.createRoutine(r) } }
        nav.popBackStack()
    }
    EditorScaffold("Stretching", routine?.name ?: NEW_ROUTINE_NAME, onBack = { nav.popBackStack() }, onDone = ::done) {
        GroupedList {
            FieldRow("Name", divider = false) {
                if (isDraft) InlineTextField(draft.name ?: "", { draft.name = it }, placeholder = NEW_ROUTINE_NAME, label = "Routine name")
                else InlineTextField(routine?.name ?: "", { v -> vm.run { vm.repo.setRoutineName(id, v) } }, placeholder = NEW_ROUTINE_NAME, label = "Routine name")
            }
            ToggleRow("Active", draft.shownActive(routine, noRoutines)) { on ->
                if (isDraft) draft.activeChoice = on else vm.run { vm.repo.setRoutineActive(id, on) }
            }
        }
        GroupFooter("The active routine is the one Today offers to stretch.")
        SectionTitle("Stretches", Modifier.padding(top = 24.dp))
        GroupedList {
            mine.forEachIndexed { i, rs ->
                val st = stretches.find { it.id == rs.stretchId }
                FieldRow(name(rs), divider = i > 0, note = if (st?.sided == true) "per side" else null) {
                    NumberField((rs.seconds ?: st?.seconds ?: 45).toDouble(), { v -> vm.run { vm.repo.setRoutineStretchSeconds(rs.id, (v ?: 45.0).toInt()) } }, 5.0, "s", Modifier.width(176.dp), 0, 38, min = 5.0, max = 600.0)
                }
            }
            if (mine.isEmpty()) ListRow("No stretches yet", chevron = false, divider = false, titleColor = K.Muted)
        }
        TintedButton("Add stretch", Modifier.padding(top = 12.dp)) { picking = true }
        if (mine.isNotEmpty()) {
            SectionTitle("Order", Modifier.padding(top = 24.dp))
            GroupedList {
                mine.forEachIndexed { i, rs ->
                    val n = name(rs)
                    ListRow(n, chevron = false, divider = i > 0, trailing = {
                        SymbolButton(Icons.ArrowUp, "Move $n up", enabled = i > 0) { vm.run { vm.repo.moveRoutineStretch(rs.id, up = true) } }
                        SymbolButton(Icons.ArrowDown, "Move $n down", enabled = i < mine.size - 1) { vm.run { vm.repo.moveRoutineStretch(rs.id, up = false) } }
                        SymbolButton(Icons.Trash, "Remove $n", K.Red) { removal.ask(rs) }
                    })
                }
            }
        }
        // Nothing to delete before the routine is saved.
        if (routine != null) EditorActions {
            GroupedList { ActionRow(Icons.Trash, "Delete routine", K.Red, divider = false) { confirmDelete = true } }
        }
    }
    if (picking) ChoiceSheet(
        "Add a stretch", stretches.filter { !it.archived }.map { Choice(it.id, it.name) },
        onPick = ::pick, onDismiss = { picking = false }, isSelected = { s -> mine.any { it.stretchId == s } }, searchable = true,
    )
    ConfirmSheet(removal, title = { "Remove ${name(it)}?" }, body = { "It leaves this routine; the stretch stays in the library." }, action = "Remove") { rs ->
        vm.run { vm.repo.removeRoutineStretch(rs.id) }
    }
    if (confirmDelete) ConfirmSheet("Delete this routine?", "Its stretch history stays.", "Delete", { confirmDelete = false }) {
        confirmDelete = false
        vm.run { vm.repo.deleteRoutine(id) }; nav.popBackStack()
    }
}

/** The stretch library: a search field, each stretch with its sides and length and its pose thumbnail. */
@Composable
fun StretchLibraryScreen(vm: AppViewModel, nav: NavHostController) {
    val stretches by vm.stretches.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        val barScroll = rememberScrollState()
        EditorTopBar("Stretching", "Stretches", scrollPx = { barScroll.value }, onBack = { nav.popBackStack() }, done = "+", addLabel = "New stretch") { nav.navigate(Routes.stretchEditor("new")) }
        Column(Modifier.weight(1f).verticalScroll(barScroll).padding(16.dp, 8.dp, 16.dp, 32.dp)) {
            SearchField(query, { query = it })
            Spacer(Modifier.height(16.dp))
            val list = stretches.filter { !it.archived && matchesQuery(it.name, query) }
            GroupedList {
                list.forEachIndexed { i, st ->
                    ListRow(st.name, secondary = stretchLine(st), divider = i > 0,
                        trailing = { Figure(st.figure, Modifier.padding(start = 10.dp).size(36.dp)) }) { nav.navigate(Routes.stretchEditor(st.id)) }
                }
                if (list.isEmpty()) SearchEmpty(query)
            }
        }
    }
}

/**
 * Stretch editor: the guide's lesson on top, then name and guide; Timing (sides, hold or reps, seconds); Details
 * (muscles, cue, a reference link); Delete. Edits a draft; Done writes it. Built-in stretches and ones a routine row
 * uses cannot be deleted, and the reason shows under Delete.
 */
@Composable
fun StretchEditorScreen(vm: AppViewModel, nav: NavHostController, id: String) {
    val stretches by vm.stretches.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // The route alone sets the mode for the screen's whole life: "new" creates, any other id edits that stretch.
    val isNew = id == "new"
    val existing = if (isNew) null else stretches.find { it.id == id }
    // What the editor opened with, kept once seen even when the row goes: Done writes only the fields changed since
    // ([KhonDao.saveStretchEdits]), which is nothing for a deleted row, and never creates a stretch in edit mode.
    var opened by remember(id) { mutableStateOf(existing) }
    var draft by remember(id) { mutableStateOf(existing ?: Stretch(newId(), "", figure = "hipflexor")) }
    if (opened == null && existing != null) { opened = existing; draft = existing } // the row loaded after the screen opened
    var figureSheet by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Routine rows that use it, unknown (null) until counted; Delete stays off until then. Counted once the row is
    // there, which can be a few frames after the screen opens, and again after a refused delete.
    var useCount by remember { mutableStateOf<Int?>(null) }
    val loaded = existing != null
    LaunchedEffect(id, loaded) { if (loaded) useCount = vm.repo.stretchUseCount(id) }
    val reps = draft.mode == StretchMode.REPS
    fun save() {
        val d = draft; val o = opened
        // A new stretch with a blank name saves nothing.
        if (isNew) { if (d.name.isNotBlank()) vm.run { vm.repo.createStretch(d.copy(name = d.name.trim())) } }
        else if (o != null) vm.run { vm.repo.saveStretchEdits(o, d) }
    }
    EditorScaffold("Stretches", if (isNew) "New stretch" else opened?.name ?: "Stretch", onBack = { nav.popBackStack() }, onDone = { save(); nav.popBackStack() }) {
        StretchDemonstration(draft.figure, compact = true, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
        GroupedList {
            FieldRow("Name", divider = false) { InlineTextField(draft.name, { draft = draft.copy(name = it) }, placeholder = "Stretch name", label = "Name") }
            FieldRow("Guide", onClick = { figureSheet = true }) { FieldValue(Figures.name(draft.figure)) }
        }
        SectionTitle("Timing", Modifier.padding(top = 24.dp))
        GroupedList {
            ToggleRow("Per side", draft.sided, divider = false) { draft = draft.copy(sided = it) }
            ToggleRow("Reps", reps) { draft = draft.copy(mode = if (it) StretchMode.REPS else StretchMode.HOLD, reps = if (it && draft.reps == 0) 10 else draft.reps) }
            if (reps) NumberRow("Rep count", draft.reps.toDouble(), "", 1.0, 0, true, min = 1.0, max = 50.0) { draft = draft.copy(reps = (it ?: 1.0).toInt()) }
            NumberRow(if (reps) "Time" else "Hold", draft.seconds.toDouble(), "s", 5.0, 0, true, min = 5.0, max = 600.0) { draft = draft.copy(seconds = (it ?: 45.0).toInt()) }
        }
        SectionTitle("Details", Modifier.padding(top = 24.dp))
        GroupedList {
            FieldRow("Muscles", divider = false) { InlineTextField(draft.muscles, { draft = draft.copy(muscles = it) }, placeholder = "hamstrings, glutes", label = "Muscles") }
            FieldRow("Cue") { InlineTextField(draft.cue, { draft = draft.copy(cue = it) }, placeholder = "How to do it", label = "Cue") }
            FieldRow("Link") { InlineTextField(draft.link, { draft = draft.copy(link = it) }, placeholder = "YouTube URL", label = "Link") }
        }
        if (draft.link.isNotBlank()) GroupedList(Modifier.padding(top = 16.dp)) {
            ListRow("Watch the reference", divider = false) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(draft.link.trim()))) } }
        }
        if (existing != null) {
            val count = useCount
            val note = when { existing.builtin -> "Built-in stretches cannot be deleted."; count != null && count > 0 -> "In use by $count routine rows."; else -> null }
            EditorActions(note = note) {
                GroupedList { ActionRow(Icons.Trash, "Delete stretch", K.Red, enabled = !existing.builtin && count == 0, divider = false) { confirmDelete = true } }
            }
        }
    }
    if (figureSheet) ChoiceSheet("Guide", Figures.keys.map { Choice(it, Figures.name(it)) },
        onPick = { draft = draft.copy(figure = it); figureSheet = false }, onDismiss = { figureSheet = false }, isSelected = { it == draft.figure })
    if (confirmDelete) ConfirmSheet("Delete this stretch?", "It goes from the stretch library for good.", "Delete", { confirmDelete = false }) {
        // Checked again when it runs: a routine may use the stretch by now. A refusal keeps the editor open with the reason.
        confirmDelete = false
        vm.run { if (vm.repo.deleteStretchIfUnused(id)) nav.popBackStack() else useCount = vm.repo.stretchUseCount(id) }
    }
}
