package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K

val PRESET_COLORS = listOf(0xFFF0A35A, 0xFF71B7FF, 0xFF73D6A2, 0xFFB39DFF, 0xFFFF8FA3, 0xFFFFD166, 0xFF4CD28A, 0xFF7CE0A8, 0xFFFF9F68, 0xFF9AD0FF, 0xFFE0A3FF, 0xFF9B9BA3)

private fun cap(s: String) = s.replaceFirstChar { it.uppercase() }

@Composable
fun EditorScaffold(back: String, title: String, onBack: () -> Unit, onDone: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        val barScroll = rememberScrollState()
        EditorTopBar(back, title, scrollPx = { barScroll.value }, onBack = onBack, onDone = onDone)
        Column(Modifier.weight(1f).verticalScroll(barScroll).padding(16.dp, 8.dp, 16.dp, 32.dp), content = content)
    }
}

// ---------- Program ----------

/**
 * Program editor: name, Active and what it does, the numbered blocks with Add block, Delete. A new program
 * (`new`) is a draft ([ProgramDraft]): nothing is saved until Done, or Add block, which saves it as Done would so
 * the block has a home; from then on it is a saved program, kept when left without Done.
 */
@Composable
fun ProgramEditorScreen(vm: AppViewModel, nav: NavHostController, programId: String) {
    val programs by vm.programs.collectAsStateWithLifecycle()
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val blockExercises by vm.blockExercises.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val draft = rememberSaveable(saver = ProgramDraft.Saver) { ProgramDraft(isNew = programId == "new") }
    val id = draft.programId(programId)
    val program = id?.let { pid -> programs.find { it.id == pid } }
    if (!draft.isNew && program == null) return
    val onlyProgram = programs.none { it.id != id }
    val savedActive = program?.active == true
    val myBlocks = if (id == null) emptyList() else blocks.filter { it.programId == id }.sortedBy { it.sortOrder }
    val confirmDelete = rememberConfirm<Program>()
    fun done() {
        val save = draft.done(newId(), program, onlyProgram)
        val target = draft.programId(programId) ?: return
        vm.run {
            save.create?.let { vm.repo.createProgram(it) }
            save.rename?.let { vm.repo.setProgramName(target, it) }
        }
        nav.popBackStack()
    }
    fun addBlock() {
        draft.programForBlock(newId(), onlyProgram)?.let { p -> vm.run { vm.repo.createProgram(p) } }
        draft.programId(programId)?.let { nav.navigate(Routes.block(it, "new")) }
    }
    EditorScaffold("Programs", program?.name ?: "New program", onBack = { nav.popBackStack() }, onDone = ::done) {
        GroupedList {
            FieldRow("Name", divider = false) { InlineTextField(draft.name ?: program?.name ?: "", { draft.name = it }, placeholder = "Program name", label = "Name") }
            // A saved active program stays on: turning another one on is what moves the mark.
            ToggleRow("Active", draft.shownActive(savedActive, onlyProgram), enabled = !draft.activeLocked(savedActive, onlyProgram)) { on ->
                if (draft.isDraft) draft.activeChoice = on else if (on && id != null) vm.run { vm.repo.setActiveProgram(id) }
            }
        }
        GroupFooter("One program is active: the one you train now. Turning another one on moves it there.")
        SectionTitle("Blocks", Modifier.padding(top = 24.dp))
        GroupedList {
            myBlocks.forEachIndexed { i, b ->
                val mine = blockExercises.filter { it.blockId == b.id }.sortedBy { it.slot }
                val names = mine.mapNotNull { be -> exercises.find { it.id == be.exerciseId }?.name }
                ListRow(names.joinToString(" · ").ifEmpty { "Empty block" }, secondary = setsText(mine.sumOf { it.sets }), divider = i > 0, leading = { IndexBadge(i) }) { nav.navigate(Routes.block(b.programId, b.id)) }
            }
            if (myBlocks.isEmpty()) ListRow("No blocks yet", chevron = false, divider = false, titleColor = K.Muted)
        }
        TintedButton("Add block", Modifier.padding(top = 12.dp), onClick = ::addBlock)
        // Nothing to delete before Done; the last program cannot go, Today always plans from one.
        if (draft.showDelete(programs.size) && program != null) EditorActions {
            GroupedList { ActionRow(Icons.Trash, "Delete program", K.Red, divider = false) { confirmDelete.ask(program) } }
        }
    }
    ConfirmSheet(confirmDelete, title = { "Delete ${it.name}?" }, body = { "The program and its blocks go away. Past sessions keep their sets." }) { p ->
        vm.run { vm.repo.deleteProgram(p.id) }; nav.popBackStack()
    }
}

/**
 * Block editor: Superset and what it does, one or two exercises with sets and rep range, order, Delete. A new block
 * (`new`) is a draft ([BlockDraft]) until its first exercise is picked, which saves the two together, so backing
 * out of it leaves no empty block behind. Each edit writes its own column onto the stored row.
 */
@Composable
fun BlockEditorScreen(vm: AppViewModel, nav: NavHostController, programId: String, blockId: String) {
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val blockExercises by vm.blockExercises.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val draft = rememberSaveable(saver = BlockDraft.Saver) { BlockDraft(isNew = blockId == "new") }
    var draftSuperset by rememberSaveable { mutableStateOf(true) }
    val id = draft.blockId(blockId)
    val block = id?.let { b -> blocks.find { it.id == b } }
    if (!draft.isNew && block == null) return
    // Saved by the first pick but not read back yet: no second pick until it is.
    val loading = id != null && block == null
    val siblings = blocks.filter { it.programId == programId }.sortedBy { it.sortOrder }
    val index = if (block != null) siblings.indexOf(block) else siblings.size
    val superset = block?.superset ?: draftSuperset
    val mine = if (id == null) emptyList() else blockExercises.filter { it.blockId == id }.sortedBy { it.slot }
    var picking by rememberSaveable { mutableStateOf<Int?>(null) } // slot
    val removal = rememberConfirm<BlockExercise>()
    val confirmDelete = rememberConfirm<Block>()
    fun name(be: BlockExercise) = exercises.find { it.id == be.exerciseId }?.name
    fun pick(slot: Int, exerciseId: String) {
        picking = null
        draft.firstPick(newId(), programId, superset, newId(), slot, exerciseId)?.let { (b, first) -> vm.run { vm.repo.createBlock(b, first) }; return }
        if (id == null || loading) return
        val existing = mine.find { it.slot == slot }
        vm.run { if (existing != null) vm.repo.setBlockExerciseExercise(existing.id, exerciseId) else vm.repo.saveBlockExercise(BlockExercise(newId(), id, slot, exerciseId)) }
    }
    EditorScaffold("Program", "Block ${index + 1}", onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() }) {
        GroupedList {
            ToggleRow("Superset", superset, divider = false) { on -> if (block == null) draftSuperset = on else vm.run { vm.repo.setBlockSuperset(block.id, on) } }
        }
        GroupFooter(supersetNote(superset))
        mine.forEach { be ->
            SectionTitle(slotTitle(be.slot), Modifier.padding(top = 20.dp)) { TextButton("Remove", K.Red, description = "Remove ${slotTitle(be.slot)}") { removal.ask(be) } }
            GroupedList {
                FieldRow("Exercise", divider = false, onClick = { picking = be.slot }) { FieldValue(name(be) ?: "Choose") }
                NumberRow("Sets", be.sets.toDouble(), "", 1.0, 0, true, min = 1.0, max = 10.0) { v -> vm.run { vm.repo.setBlockExerciseSets(be.id, (v ?: 1.0).toInt()) } }
                NumberRow("Min reps", be.minReps.toDouble(), "", 1.0, 0, true, min = 1.0, max = 50.0) { v -> vm.run { vm.repo.setBlockExerciseMinReps(be.id, (v ?: 1.0).toInt()) } }
                NumberRow("Max reps", be.maxReps.toDouble(), "", 1.0, 0, true, min = 1.0, max = 50.0) { v -> vm.run { vm.repo.setBlockExerciseMaxReps(be.id, (v ?: 1.0).toInt()) } }
            }
        }
        freeSlot(mine.map { it.slot })?.let { slot ->
            if (!loading) TintedButton(if (mine.isEmpty()) "Add exercise" else "Add second exercise", Modifier.padding(top = 12.dp)) { picking = slot }
        }
        // Order and Delete come once the block is saved.
        if (block != null) EditorActions {
            if (siblings.size > 1) GroupedList {
                if (index > 0) ActionRow(Icons.ArrowUp, "Move up", divider = false) { vm.run { vm.repo.moveBlock(block.id, -1) } }
                if (index < siblings.size - 1) ActionRow(Icons.ArrowDown, "Move down", divider = index > 0) { vm.run { vm.repo.moveBlock(block.id, 1) } }
            }
            GroupedList { ActionRow(Icons.Trash, "Delete block", K.Red, divider = false) { confirmDelete.ask(block) } }
        }
    }
    picking?.let { slot ->
        val existing = mine.find { it.slot == slot }
        ChoiceSheet(
            "Exercise", exercises.filter { it.kind == Kind.STRENGTH && !it.archived }.map { Choice(it.id, it.name) },
            onPick = { pick(slot, it) }, onDismiss = { picking = null }, isSelected = { it == existing?.exerciseId }, searchable = true,
        )
    }
    ConfirmSheet(removal, title = { "Remove ${name(it) ?: "this exercise"}?" }, body = { "Its sets and rep range leave this block. Past sessions keep their sets." }, action = "Remove") { be ->
        vm.run { vm.repo.deleteBlockExercise(be.id) }
    }
    ConfirmSheet(confirmDelete, title = { "Delete block ${index + 1}?" }, body = { "Its exercises go with it. Past sessions keep their sets." }) { b ->
        vm.run { vm.repo.deleteBlock(b.id) }; nav.popBackStack()
    }
}

// ---------- Exercise ----------

/**
 * Exercise editor: fields in groups (name and kind; muscles and details, or cardio, intervals and notes), Archive,
 * Merge into… and Delete at the bottom. The kind cannot change once the exercise is used ([kindLocked]); Delete
 * only while nothing uses it, and only after asking.
 */
@Composable
fun ExerciseEditorScreen(vm: AppViewModel, nav: NavHostController, exerciseId: String) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val isNew = exerciseId == "new"
    val original = exercises.find { it.id == exerciseId }
    if (!isNew && original == null) return
    // What the editor opened with: Done writes only the fields that differ from it, onto the row as it is then.
    val opened = remember(exerciseId, original == null) { original }
    var draft by remember(exerciseId, original == null) { mutableStateOf(opened ?: Exercise(newId(), "", Kind.STRENGTH, primaryMuscle = "chest")) }
    var sheet by remember { mutableStateOf<String?>(null) }
    // Null while the count loads, which keeps the kind locked and Delete off.
    var usage by remember { mutableStateOf<Int?>(if (isNew) 0 else null) }
    LaunchedEffect(exerciseId) { if (!isNew) usage = vm.repo.exerciseUsage(exerciseId) }
    val confirmDelete = rememberConfirm<Exercise>()
    val locked = kindLocked(isNew, usage)
    val isStrength = draft.kind == Kind.STRENGTH
    EditorScaffold("Library", if (isNew) "New exercise" else original!!.name, onBack = { nav.popBackStack() }, onDone = {
        if (draft.name.isNotBlank()) vm.run { vm.repo.saveExerciseEdits(opened, draft.copy(name = draft.name.trim())) }; nav.popBackStack()
    }) {
        GroupedList {
            FieldRow("Name", divider = false) { InlineTextField(draft.name, { draft = draft.copy(name = it) }, placeholder = "Exercise name", label = "Name") }
            FieldRow("Kind", onClick = if (locked) null else ({ sheet = "kind" })) { FieldValue(cap(draft.kind), locked = locked) }
        }
        if (locked && usage != null) GroupFooter("The kind is fixed: sessions, programs or the plan already use this exercise as it is.")
        val notes = @Composable { divider: Boolean -> FieldRow("Notes", divider = divider) { InlineTextField(draft.notes, { draft = draft.copy(notes = it) }, placeholder = "Seat height, grip…", label = "Notes") } }
        if (isStrength) {
            SectionTitle("Muscles", Modifier.padding(top = 24.dp))
            GroupedList {
                FieldRow("Primary", divider = false, onClick = { sheet = "primary" }) { FieldValue(cap(draft.primaryMuscle ?: "")) }
                FieldRow("Secondary", onClick = { sheet = "secondary" }) { FieldValue(draft.secondaryList.joinToString(", ") { cap(it) }.ifEmpty { "None" }, maxLines = 2) }
            }
            SectionTitle("Details", Modifier.padding(top = 24.dp))
            GroupedList {
                NumberRow("Weight step", draft.stepKg, "kg", 0.5, 1, false) { draft = draft.copy(stepKg = it ?: 2.5) }
                notes(true)
            }
        } else {
            SectionTitle("Cardio", Modifier.padding(top = 24.dp))
            GroupedList {
                FieldRow("Modality", divider = false, onClick = { sheet = "modality" }) { FieldValue(modalities.find { it.id == draft.modalityId }?.name ?: "Choose") }
                FieldRow("Intensity", onClick = { sheet = "intensity" }) { FieldValue(cap(draft.intensity ?: "low")) }
                FieldRow("Fields", onClick = { sheet = "fields" }) { FieldValue(draft.fieldList.joinToString(", ") { fieldLabel(it) }.ifEmpty { "None" }, maxLines = 2) }
            }
            GroupedList(Modifier.padding(top = 24.dp)) {
                ToggleRow("Intervals", draft.intervals, divider = false) { draft = draft.copy(intervals = it, rounds = if (it && draft.rounds == 0) 4 else draft.rounds, workSec = if (it && draft.workSec == 0) 240 else draft.workSec, restSec = if (it && draft.restSec == 0) 180 else draft.restSec) }
                if (draft.intervals) {
                    NumberRow("Warm-up", draft.warmupSec / 60.0, "min", 1.0, 0, true) { draft = draft.copy(warmupSec = scaled(it ?: 0.0, 60.0)) }
                    NumberRow("Work", draft.workSec.toDouble(), "s", 10.0, 0, true) { draft = draft.copy(workSec = (it ?: 0.0).toInt()) }
                    NumberRow("Rest", draft.restSec.toDouble(), "s", 10.0, 0, true) { draft = draft.copy(restSec = (it ?: 0.0).toInt()) }
                    NumberRow("Rounds", draft.rounds.toDouble(), "", 1.0, 0, true, min = 1.0, max = 30.0) { draft = draft.copy(rounds = (it ?: 1.0).toInt()) }
                }
            }
            GroupedList(Modifier.padding(top = 24.dp)) { notes(false) }
        }
        if (!isNew) EditorActions(note = if ((usage ?: 0) > 0) "Used in a session, program, or plan: archive or merge it instead." else null) {
            GroupedList {
                val archived = original!!.archived
                ActionRow(if (archived) Icons.Unarchive else Icons.Archive, if (archived) "Unarchive" else "Archive", divider = false) {
                    // Archive keeps the edits made on the page: they save first, then the one column.
                    vm.run { if (draft.name.isNotBlank()) vm.repo.saveExerciseEdits(opened, draft.copy(name = draft.name.trim())); vm.repo.setExerciseArchived(exerciseId, !archived) }; nav.popBackStack()
                }
                ActionRow(Icons.Merge, "Merge into…") { sheet = "merge" }
            }
            GroupedList { ActionRow(Icons.Trash, "Delete", K.Red, enabled = canDeleteExercise(usage), divider = false) { confirmDelete.ask(original!!) } }
        }
    }
    fun close() { sheet = null }
    when (sheet) {
        "kind" -> ChoiceSheet("Kind", listOf(Choice(Kind.STRENGTH, "Strength"), Choice(Kind.CARDIO, "Cardio")), onPick = { k ->
            draft = if (k == Kind.STRENGTH) draft.copy(kind = Kind.STRENGTH, primaryMuscle = draft.primaryMuscle ?: "chest")
            else draft.copy(kind = Kind.CARDIO, modalityId = draft.modalityId ?: modalities.firstOrNull()?.id, intensity = draft.intensity ?: Intensity.LOW, fields = draft.fields.ifEmpty { "time,distance,avgHr" })
            close()
        }, onDismiss = ::close, isSelected = { it == draft.kind })
        "primary" -> ChoiceSheet("Primary muscle", MUSCLE_GROUPS.map { Choice(it, cap(it)) }, onPick = { draft = draft.copy(primaryMuscle = it); close() }, onDismiss = ::close, isSelected = { it == draft.primaryMuscle })
        "secondary" -> ChoiceSheet("Secondary muscles", MUSCLE_GROUPS.filter { it != draft.primaryMuscle }.map { Choice(it, cap(it)) }, onPick = { m ->
            val on = m in draft.secondaryList
            draft = draft.copy(secondaryMuscles = (if (on) draft.secondaryList - m else draft.secondaryList + m).joinToString(","))
        }, onDismiss = ::close, isSelected = { it in draft.secondaryList })
        "modality" -> ChoiceSheet("Modality", modalities.map { Choice(it.id, it.name, Color(it.color)) }, onPick = { draft = draft.copy(modalityId = it); close() }, onDismiss = ::close, isSelected = { it == draft.modalityId })
        "intensity" -> ChoiceSheet("Intensity", listOf(Intensity.LOW, Intensity.MODERATE, Intensity.HIGH).map { Choice(it, cap(it)) }, onPick = { draft = draft.copy(intensity = it); close() }, onDismiss = ::close, isSelected = { it == draft.intensity })
        "fields" -> ChoiceSheet("Fields", listOf("time", "distance", "avgHr", "watts", "laps", "poolLength", "stroke").map { Choice(it, fieldLabel(it)) }, onPick = { f ->
            val on = f in draft.fieldList
            draft = draft.copy(fields = (if (on) draft.fieldList - f else draft.fieldList + f).joinToString(","))
        }, onDismiss = ::close, isSelected = { it in draft.fieldList })
        "merge" -> ChoiceSheet("Merge ${draft.name} into", exercises.filter { it.id != exerciseId && it.kind == draft.kind }.map { Choice(it.id, it.name) }, onPick = { into ->
            close(); vm.run { vm.repo.mergeExercise(exerciseId, into) }; nav.popBackStack()
        }, onDismiss = ::close, searchable = true)
    }
    ConfirmSheet(confirmDelete, title = { "Delete ${it.name}?" }, body = { "It goes from the library for good." }) { e ->
        vm.run { vm.repo.deleteExercise(e.id) }; nav.popBackStack()
    }
}

fun fieldLabel(f: String) = when (f) { "time" -> "Time"; "distance" -> "Distance"; "avgHr" -> "Avg heart rate"; "watts" -> "Watts"; "laps" -> "Laps"; "poolLength" -> "Pool length"; "stroke" -> "Stroke"; else -> f }

// ---------- Modality ----------

/** Modality editor: the name, its color picked right on the page from the presets, and Archive with what it does. */
@Composable
fun ModalityEditorScreen(vm: AppViewModel, nav: NavHostController, modalityId: String) {
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val isNew = modalityId == "new"
    val original = modalities.find { it.id == modalityId }
    if (!isNew && original == null) return
    val opened = remember(modalityId, original == null) { original }
    var draft by remember(modalityId, original == null) { mutableStateOf(opened ?: Modality(newId(), "", PRESET_COLORS[3])) }
    EditorScaffold("Modalities", if (isNew) "New modality" else original!!.name, onBack = { nav.popBackStack() }, onDone = {
        if (draft.name.isNotBlank()) vm.run { vm.repo.saveModalityEdits(opened, draft.copy(name = draft.name.trim())) }; nav.popBackStack()
    }) {
        GroupedList { FieldRow("Name", divider = false) { InlineTextField(draft.name, { draft = draft.copy(name = it) }, placeholder = "Bike, rowing…", label = "Name") } }
        SectionTitle("Color", Modifier.padding(top = 24.dp))
        ColorGrid(draft.color) { draft = draft.copy(color = it) }
        if (!isNew) EditorActions(note = "Archived, it moves to the archived group in Modalities; its exercises and past sessions keep it and its color.") {
            GroupedList {
                val archived = original!!.archived
                ActionRow(if (archived) Icons.Unarchive else Icons.Archive, if (archived) "Unarchive" else "Archive", divider = false) {
                    vm.run { if (draft.name.isNotBlank()) vm.repo.saveModalityEdits(opened, draft.copy(name = draft.name.trim())); vm.repo.setModalityArchived(modalityId, !archived) }; nav.popBackStack()
                }
            }
        }
    }
}

/**
 * The twelve preset colors in two rows of six on a card, right in the editor: one tap picks, the chosen one in a
 * white ring 2.5 dp wide with a 2 dp gap. Each 34 dp swatch sits on a 48 dp target.
 */
@Composable
private fun ColorGrid(selected: Long, onPick: (Long) -> Unit) {
    GroupedList {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            PRESET_COLORS.chunked(6).forEachIndexed { r, row ->
                Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEachIndexed { i, c -> ColorSwatch(c, c == selected, "Color ${r * 6 + i + 1}") { onPick(c) } }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Long, selected: Boolean, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).selectable(selected, role = Role.RadioButton, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(43.dp).border(2.5.dp, if (selected) Color.White else Color.Transparent, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Color(color)))
        }
    }
}
