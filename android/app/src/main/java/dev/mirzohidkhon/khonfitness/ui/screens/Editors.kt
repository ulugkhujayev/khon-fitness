package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K

val PRESET_COLORS = listOf(0xFFF0A35A, 0xFF71B7FF, 0xFF73D6A2, 0xFFB39DFF, 0xFFFF8FA3, 0xFFFFD166, 0xFF4CD28A, 0xFF7CE0A8, 0xFFFF9F68, 0xFF9AD0FF, 0xFFE0A3FF, 0xFF9B9BA3)

private fun cap(s: String) = s.replaceFirstChar { it.uppercase() }

@Composable
fun EditorScaffold(back: String, title: String, onBack: () -> Unit, onDone: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        EditorTopBar(back, title, onBack = onBack, onDone = onDone)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 32.dp), content = content)
    }
}

@Composable
fun BottomActions(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 28.dp, start = 2.dp), horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

// ---------- Program ----------

@Composable
fun ProgramEditorScreen(vm: AppViewModel, nav: NavHostController, programId: String) {
    val programs by vm.programs.collectAsStateWithLifecycle()
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val blockExercises by vm.blockExercises.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val program = programs.find { it.id == programId } ?: return
    var name by remember(program.id) { mutableStateOf(program.name) }
    val myBlocks = blocks.filter { it.programId == programId }.sortedBy { it.sortOrder }
    EditorScaffold("Programs", program.name, onBack = { nav.popBackStack() }, onDone = { vm.run { vm.repo.saveProgram(program.copy(name = name.ifBlank { program.name })) }; nav.popBackStack() }) {
        GroupedList {
            FieldRow("Name", divider = false) { InlineTextField(name, { name = it }, placeholder = "Program name") }
            FieldRow("Active") { Switch(program.active, { on -> if (on) vm.run { vm.repo.setActiveProgram(program.id) } }, colors = SwitchDefaults.colors(checkedTrackColor = K.Green, checkedThumbColor = Color.White, uncheckedTrackColor = K.Surface3, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent)) }
        }
        SectionTitle("Blocks", Modifier.padding(top = 24.dp)) { PlusButton() {
            val id = newId(); vm.run { vm.repo.saveBlock(Block(id, programId, myBlocks.size, true)) }; nav.navigate(Routes.block(programId, id))
        } }
        GroupedList {
            myBlocks.forEachIndexed { i, b ->
                val names = blockExercises.filter { it.blockId == b.id }.sortedBy { it.slot }.mapNotNull { be -> exercises.find { it.id == be.exerciseId }?.name }
                ListRow(names.joinToString(" · ").ifEmpty { "Empty block" }, secondary = "${blockExercises.filter { it.blockId == b.id }.sumOf { it.sets }} sets", divider = i > 0) { nav.navigate(Routes.block(programId, b.id)) }
            }
            if (myBlocks.isEmpty()) ListRow("No blocks yet", chevron = false, divider = false, titleColor = K.Muted)
        }
        BottomActions {
            if (programs.size > 1) TextButton("Delete program", K.Red) { vm.run { vm.repo.deleteProgram(programId) }; nav.popBackStack() }
        }
    }
}

@Composable
fun BlockEditorScreen(vm: AppViewModel, nav: NavHostController, programId: String, blockId: String) {
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val blockExercises by vm.blockExercises.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val block = blocks.find { it.id == blockId } ?: return
    val mine = blockExercises.filter { it.blockId == blockId }.sortedBy { it.slot }
    var picking by remember { mutableStateOf<Int?>(null) } // slot
    val siblings = blocks.filter { it.programId == programId }.sortedBy { it.sortOrder }
    val index = siblings.indexOfFirst { it.id == blockId }
    EditorScaffold("Program", "Block ${index + 1}", onBack = { nav.popBackStack() }, onDone = { nav.popBackStack() }) {
        GroupedList {
            FieldRow("Superset", divider = false) { Switch(block.superset, { on -> vm.run { vm.repo.saveBlock(block.copy(superset = on)) } }, colors = SwitchDefaults.colors(checkedTrackColor = K.Green, checkedThumbColor = Color.White, uncheckedTrackColor = K.Surface3, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent)) }
        }
        mine.forEachIndexed { i, be ->
            val ex = exercises.find { it.id == be.exerciseId }
            SectionTitle(('A' + be.slot).toString(), Modifier.padding(top = 20.dp)) { TextButton("Remove", K.Red) { vm.run { vm.repo.deleteBlockExercise(be.id) } } }
            GroupedList {
                FieldRow("Exercise", divider = false, onClick = { picking = be.slot }) { Text(ex?.name ?: "Choose", fontWeight = FontWeight.SemiBold); Chevron() }
                NumberRow("Sets", be.sets.toDouble(), "", 1.0, 0, true) { v -> vm.run { vm.repo.saveBlockExercise(be.copy(sets = (v ?: 1.0).toInt().coerceIn(1, 10))) } }
                NumberRow("Min reps", be.minReps.toDouble(), "", 1.0, 0, true) { v -> vm.run { vm.repo.saveBlockExercise(run { val m = (v ?: 1.0).toInt().coerceIn(1, 50); be.copy(minReps = m, maxReps = maxOf(be.maxReps, m)) }) } }
                NumberRow("Max reps", be.maxReps.toDouble(), "", 1.0, 0, true) { v -> vm.run { vm.repo.saveBlockExercise(run { val m = (v ?: 1.0).toInt().coerceIn(1, 50); be.copy(maxReps = m, minReps = minOf(be.minReps, m)) }) } }
            }
        }
        if (mine.size < 2) Row(Modifier.padding(top = 16.dp)) { TextButton(if (mine.isEmpty()) "Add exercise" else "Add second exercise") { picking = mine.size } }
        BottomActions {
            if (index > 0) TextButton("Move up") { vm.run { vm.repo.saveBlocks(listOf(block.copy(sortOrder = index - 1), siblings[index - 1].copy(sortOrder = index))) } }
            if (index < siblings.size - 1) TextButton("Move down") { vm.run { vm.repo.saveBlocks(listOf(block.copy(sortOrder = index + 1), siblings[index + 1].copy(sortOrder = index))) } }
            TextButton("Delete block", K.Red) { vm.run { vm.repo.deleteBlock(blockId) }; nav.popBackStack() }
        }
    }
    picking?.let { slot ->
        val existing = mine.find { it.slot == slot }
        Sheet("Exercise", { picking = null }) {
            ChoiceList {
                exercises.filter { it.kind == Kind.STRENGTH && !it.archived }.forEachIndexed { i, e ->
                    ChoiceRow(e.name, existing?.exerciseId == e.id, divider = i > 0) {
                        vm.run { vm.repo.saveBlockExercise(existing?.copy(exerciseId = e.id) ?: BlockExercise(newId(), blockId, slot, e.id)) }; picking = null
                    }
                }
            }
        }
    }
}

// ---------- Exercise ----------

@Composable
fun ExerciseEditorScreen(vm: AppViewModel, nav: NavHostController, exerciseId: String) {
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val isNew = exerciseId == "new"
    val original = exercises.find { it.id == exerciseId }
    if (!isNew && original == null) return
    var draft by remember(exerciseId, original == null) { mutableStateOf(original ?: Exercise(newId(), "", Kind.STRENGTH, primaryMuscle = "chest")) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var usage by remember { mutableStateOf(0) }
    LaunchedEffect(exerciseId) { if (!isNew) usage = vm.repo.exerciseUsage(exerciseId) }
    val isStrength = draft.kind == Kind.STRENGTH
    EditorScaffold("Library", if (isNew) "New exercise" else original!!.name, onBack = { nav.popBackStack() }, onDone = {
        if (draft.name.isNotBlank()) vm.run { vm.repo.saveExercise(draft) }; nav.popBackStack()
    }) {
        GroupedList {
            FieldRow("Name", divider = false) { InlineTextField(draft.name, { draft = draft.copy(name = it) }, placeholder = "Exercise name") }
            FieldRow("Kind", onClick = { if (usage == 0) sheet = "kind" }) { Text(cap(draft.kind), fontWeight = FontWeight.SemiBold); Chevron() }
            if (isStrength) {
                FieldRow("Primary muscle", onClick = { sheet = "primary" }) { Text(cap(draft.primaryMuscle ?: ""), fontWeight = FontWeight.SemiBold); Chevron() }
                FieldRow("Secondary muscles", onClick = { sheet = "secondary" }) { Text(draft.secondaryList.joinToString(", ") { cap(it) }.ifEmpty { "None" }, fontWeight = FontWeight.SemiBold, maxLines = 2); Chevron() }
                NumberRow("Weight step", draft.stepKg, "kg", 0.5, 1, true) { draft = draft.copy(stepKg = it ?: 2.5) }
            } else {
                FieldRow("Modality", onClick = { sheet = "modality" }) { Text(modalities.find { it.id == draft.modalityId }?.name ?: "Choose", fontWeight = FontWeight.SemiBold); Chevron() }
                FieldRow("Intensity", onClick = { sheet = "intensity" }) { Text(cap(draft.intensity ?: "low"), fontWeight = FontWeight.SemiBold); Chevron() }
                FieldRow("Intervals") { Switch(draft.intervals, { draft = draft.copy(intervals = it, rounds = if (it && draft.rounds == 0) 4 else draft.rounds, workSec = if (it && draft.workSec == 0) 240 else draft.workSec, restSec = if (it && draft.restSec == 0) 180 else draft.restSec) }, colors = SwitchDefaults.colors(checkedTrackColor = K.Green, checkedThumbColor = Color.White, uncheckedTrackColor = K.Surface3, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent)) }
                if (draft.intervals) {
                    NumberRow("Warm-up", draft.warmupSec / 60.0, "min", 1.0, 0, true) { draft = draft.copy(warmupSec = ((it ?: 0.0) * 60).toInt()) }
                    NumberRow("Work", draft.workSec.toDouble(), "s", 10.0, 0, true) { draft = draft.copy(workSec = (it ?: 0.0).toInt()) }
                    NumberRow("Rest", draft.restSec.toDouble(), "s", 10.0, 0, true) { draft = draft.copy(restSec = (it ?: 0.0).toInt()) }
                    NumberRow("Rounds", draft.rounds.toDouble(), "", 1.0, 0, true) { draft = draft.copy(rounds = (it ?: 1.0).toInt().coerceIn(1, 30)) }
                }
                FieldRow("Fields", onClick = { sheet = "fields" }) { Text(draft.fieldList.joinToString(", ") { fieldLabel(it) }.ifEmpty { "None" }, fontWeight = FontWeight.SemiBold, maxLines = 2); Chevron() }
            }
            FieldRow("Notes") { InlineTextField(draft.notes, { draft = draft.copy(notes = it) }, placeholder = "Seat height, grip…") }
        }
        if (!isNew) {
            BottomActions {
                TextButton(if (draft.archived) "Unarchive" else "Archive") { vm.run { vm.repo.saveExercise(draft.copy(archived = !draft.archived)) }; nav.popBackStack() }
                TextButton("Merge into…") { sheet = "merge" }
                TextButton("Delete", K.Red, enabled = usage == 0) { vm.run { vm.repo.deleteExercise(exerciseId) }; nav.popBackStack() }
            }
            if (usage > 0) Text("Used in $usage logged sets, so it can be archived or merged, not deleted.", color = K.Dim, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
    when (sheet) {
        "kind" -> Sheet("Kind", { sheet = null }) { ChoiceList {
            ChoiceRow("Strength", isStrength, divider = false) { draft = draft.copy(kind = Kind.STRENGTH, primaryMuscle = draft.primaryMuscle ?: "chest"); sheet = null }
            ChoiceRow("Cardio", !isStrength) { draft = draft.copy(kind = Kind.CARDIO, modalityId = draft.modalityId ?: modalities.firstOrNull()?.id, intensity = draft.intensity ?: Intensity.LOW, fields = draft.fields.ifEmpty { "time,distance,avgHr" }); sheet = null }
        } }
        "primary" -> Sheet("Primary muscle", { sheet = null }) { ChoiceList { MUSCLE_GROUPS.forEachIndexed { i, m -> ChoiceRow(cap(m), draft.primaryMuscle == m, divider = i > 0) { draft = draft.copy(primaryMuscle = m); sheet = null } } } }
        "secondary" -> Sheet("Secondary muscles", { sheet = null }) { ChoiceList { MUSCLE_GROUPS.filter { it != draft.primaryMuscle }.forEachIndexed { i, m ->
            val on = m in draft.secondaryList
            ChoiceRow(cap(m), on, divider = i > 0) { draft = draft.copy(secondaryMuscles = (if (on) draft.secondaryList - m else draft.secondaryList + m).joinToString(",")) }
        } } }
        "modality" -> Sheet("Modality", { sheet = null }) { ChoiceList { modalities.forEachIndexed { i, m -> ChoiceRow(m.name, draft.modalityId == m.id, Color(m.color), divider = i > 0) { draft = draft.copy(modalityId = m.id); sheet = null } } } }
        "intensity" -> Sheet("Intensity", { sheet = null }) { ChoiceList { listOf(Intensity.LOW, Intensity.MODERATE, Intensity.HIGH).forEachIndexed { i, v -> ChoiceRow(cap(v), draft.intensity == v, divider = i > 0) { draft = draft.copy(intensity = v); sheet = null } } } }
        "fields" -> Sheet("Fields", { sheet = null }) { ChoiceList { listOf("time", "distance", "avgHr", "watts", "laps", "poolLength", "stroke").forEachIndexed { i, f ->
            val on = f in draft.fieldList
            ChoiceRow(fieldLabel(f), on, divider = i > 0) { draft = draft.copy(fields = (if (on) draft.fieldList - f else draft.fieldList + f).joinToString(",")) }
        } } }
        "merge" -> Sheet("Merge ${draft.name} into", { sheet = null }) { ChoiceList { exercises.filter { it.id != exerciseId && it.kind == draft.kind }.forEachIndexed { i, e ->
            ChoiceRow(e.name, false, divider = i > 0) { vm.run { vm.repo.mergeExercise(exerciseId, e.id) }; sheet = null; nav.popBackStack() }
        } } }
    }
}

fun fieldLabel(f: String) = when (f) { "time" -> "Time"; "distance" -> "Distance"; "avgHr" -> "Avg heart rate"; "watts" -> "Watts"; "laps" -> "Laps"; "poolLength" -> "Pool length"; "stroke" -> "Stroke"; else -> f }

// ---------- Modality ----------

@Composable
fun ModalityEditorScreen(vm: AppViewModel, nav: NavHostController, modalityId: String) {
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val isNew = modalityId == "new"
    val original = modalities.find { it.id == modalityId }
    if (!isNew && original == null) return
    var draft by remember(modalityId, original == null) { mutableStateOf(original ?: Modality(newId(), "", PRESET_COLORS[3])) }
    var picking by remember { mutableStateOf(false) }
    EditorScaffold("Modalities", if (isNew) "New modality" else original!!.name, onBack = { nav.popBackStack() }, onDone = { if (draft.name.isNotBlank()) vm.run { vm.repo.saveModality(draft) }; nav.popBackStack() }) {
        GroupedList {
            FieldRow("Name", divider = false) { InlineTextField(draft.name, { draft = draft.copy(name = it) }, placeholder = "Bike, rowing…") }
            FieldRow("Color", onClick = { picking = true }) { Dot(Color(draft.color)); Chevron() }
        }
        if (!isNew) BottomActions { TextButton(if (draft.archived) "Unarchive" else "Archive") { vm.run { vm.repo.saveModality(draft.copy(archived = !draft.archived)) }; nav.popBackStack() } }
    }
    if (picking) Sheet("Color", { picking = false }) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            PRESET_COLORS.take(6).forEach { c -> ColorSwatch(c, draft.color == c) { draft = draft.copy(color = c); picking = false } }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            PRESET_COLORS.drop(6).forEach { c -> ColorSwatch(c, draft.color == c) { draft = draft.copy(color = c); picking = false } }
        }
    }
}

@Composable
private fun ColorSwatch(color: Long, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(if (selected) Color.White else Color.Transparent).padding(3.dp).clip(CircleShape).background(Color(color)).clickable(onClick = onClick))
}
