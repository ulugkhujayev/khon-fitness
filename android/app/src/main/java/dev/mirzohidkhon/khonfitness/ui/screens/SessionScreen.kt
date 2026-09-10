package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import kotlinx.coroutines.delay

/** A program session: live logging while unfinished, read-only with an Edit toggle after. */
@Composable
fun SessionScreen(vm: AppViewModel, nav: NavHostController, sessionId: String) {
    val session by vm.repo.session(sessionId).collectAsStateWithLifecycle(null)
    val logs by vm.repo.setLogs(sessionId).collectAsStateWithLifecycle(emptyList())
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var previous by remember { mutableStateOf<Map<String, SetLog>>(emptyMap()) }
    var editing by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf<Int?>(null) }
    var focusId by remember { mutableStateOf<String?>(null) }
    var swapLog by remember { mutableStateOf<SetLog?>(null) }
    var summary by remember { mutableStateOf<Summary?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    val listState = rememberLazyListState()
    val s = session ?: return

    LaunchedEffect(s.id, s.programId) { previous = vm.repo.previousLogsByKey(s.programId, s.id) }
    val live = !s.finished || editing
    val blocks = remember(logs) { logs.groupBy { it.blockIndex }.toSortedMap().values.toList() }
    LaunchedEffect(blocks.size) { if (expanded == null && blocks.isNotEmpty()) expanded = blocks.indexOfFirst { b -> b.any { it.status == SetStatus.PENDING } }.takeIf { it >= 0 } ?: 0 }
    LaunchedEffect(focusId) {
        val id = focusId ?: return@LaunchedEffect
        delay(60)
        requesters[id]?.requestFocus()
        focusId = null
    }
    val done = logs.count { it.status == SetStatus.DONE }

    Column(Modifier.fillMaxSize()) {
        if (s.finished) EditorTopBar("History", s.programName ?: "Session", onBack = { nav.popBackStack() }, done = if (editing) "Done" else "Edit") { editing = !editing }
        LazyColumn(state = listState, modifier = Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = if (s.finished) 4.dp else 12.dp, bottom = 24.dp)) {
            if (!s.finished) item {
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton("‹ Today") { nav.popBackStack() }
                    Spacer(Modifier.weight(1f))
                    TextButton("Finish") { summary = summarize(logs, vm.allSetLogs.value, s.id) }
                }
                Text(s.programName ?: "Session", style = MaterialTheme.typography.headlineMedium)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.date.toDate().format(shortDate), color = K.Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("$done / ${logs.size} sets", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
                }
                LinearProgressIndicator(progress = { if (logs.isEmpty()) 0f else done / logs.size.toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp).height(5.dp).clip(CircleShape), color = K.Green, trackColor = K.Surface2)
            }
            itemsIndexed(blocks, key = { i, _ -> i }) { bi, blockLogs ->
                val isOpen = expanded == bi
                val names = blockLogs.distinctBy { it.slot }.sortedBy { it.slot }.joinToString(" · ") { it.exerciseName }
                val blockDone = blockLogs.all { it.status != SetStatus.PENDING }
                Column(Modifier.fillMaxWidth().padding(top = if (bi == 0) 6.dp else 10.dp).clip(GroupShape).background(K.Surface)) {
                    Row(Modifier.fillMaxWidth().clickable { expanded = if (isOpen) null else bi }.heightIn(min = 56.dp).padding(start = 16.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(K.AccentSoft), contentAlignment = Alignment.Center) { Text("${bi + 1}", color = K.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        Text(names, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 12.dp))
                        Text("${blockLogs.size} sets", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
                        Text(if (blockDone) "✓" else if (isOpen) "⌃" else "›", color = if (blockDone) K.Green else K.Dim, fontSize = 20.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                    if (isOpen) {
                        HorizontalDivider(color = K.Divider)
                        blockLogs.forEachIndexed { li, log ->
                            if (li > 0) HorizontalDivider(color = K.Divider)
                            val prev = previous[log.key()]
                            SetRow(
                                log = log, prev = prev, live = live,
                                requester = requesters.getOrPut(log.id) { FocusRequester() },
                                onToggle = {
                                    val next = if (log.status == SetStatus.DONE) SetStatus.PENDING else SetStatus.DONE
                                    vm.run { vm.repo.updateSetLog(log.copy(status = next)) }
                                    if (next == SetStatus.DONE) {
                                        val nextLog = logs.filter { it.order > log.order && it.status == SetStatus.PENDING }.minByOrNull { it.order }
                                        if (nextLog != null) { if (nextLog.blockIndex != bi) expanded = nextLog.blockIndex; focusId = nextLog.id }
                                    }
                                },
                                onWeight = { w -> vm.run { vm.repo.updateSetLog(log.copy(weightKg = w)) } },
                                onReps = { r -> vm.run { vm.repo.updateSetLog(log.copy(reps = r?.toInt())) } },
                                onSwap = { swapLog = log },
                            )
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(20.dp))
                if (live) {
                    GroupedList {
                        DateRow(s.date.toDate(), divider = false) { d -> vm.run { vm.repo.updateSession(s.copy(date = d.iso())) } }
                        FieldRow("Effort") { NumberField(s.effort?.toDouble(), { v -> vm.run { vm.repo.updateSession(s.copy(effort = v?.toInt()?.coerceIn(1, 10))) } }, 1.0, "/10", Modifier.width(176.dp), 0, 38, placeholder = "1–10") }
                        FieldRow("Note") { InlineTextField(s.note, { vm.run { vm.repo.updateSession(s.copy(note = it)) } }, placeholder = "Optional") }
                    }
                } else if (s.note.isNotBlank() || s.effort != null) {
                    GroupedList {
                        if (s.effort != null) FieldRow("Effort", divider = false) { Text("${s.effort} / 10") }
                        if (s.note.isNotBlank()) FieldRow("Note", divider = s.effort != null) { Text(s.note, textAlign = androidx.compose.ui.text.style.TextAlign.End) }
                    }
                }
                Spacer(Modifier.height(20.dp))
                if (!s.finished) PrimaryButton("Finish workout") { summary = summarize(logs, vm.allSetLogs.value, s.id) }
                Spacer(Modifier.height(20.dp))
                TextButton(if (s.finished) "Delete session" else "Discard workout", K.Red) { confirmDelete = true }
            }
        }
    }
    swapLog?.let { log -> SwapSheet(exercises.filter { it.kind == Kind.STRENGTH && !it.archived }, log, onDismiss = { swapLog = null }) { ex, updateProgram ->
        vm.run { vm.repo.swapExerciseInSession(s.id, log.exerciseId, ex, updateProgram, s.programId) }; swapLog = null
    } }
    if (confirmDelete) ConfirmSheet(
        if (s.finished) "Delete this session?" else "Discard this workout?",
        if (s.finished) "The sets and the session go away. Charts recompute." else "Nothing from today is kept.",
        if (s.finished) "Delete" else "Discard",
        onDismiss = { confirmDelete = false },
    ) { confirmDelete = false; vm.run { vm.repo.deleteSession(s.id) }; nav.popBackStack() }
    summary?.let { sum -> Sheet("Workout done", onDismiss = { summary = null }) {
        GroupedList {
            ListRow("Sets done", secondary = "${sum.done} of ${sum.total}", chevron = false, divider = false)
            ListRow("Volume", secondary = "${fmt(sum.volume, 0)} kg", chevron = false)
            ListRow("New bests", secondary = sum.bests.toString(), chevron = false)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Save") { vm.run { vm.repo.finishSession(s.id) }; summary = null; nav.popBackStack() }
    } }
}

data class Summary(val done: Int, val total: Int, val volume: Double, val bests: Int)

private fun summarize(logs: List<SetLog>, all: List<SetLog>, sessionId: String): Summary {
    val done = logs.filter { it.status == SetStatus.DONE }
    val volume = done.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
    val before = all.filter { it.sessionId != sessionId && it.status == SetStatus.DONE }.groupBy { it.exerciseId }.mapValues { e -> e.value.maxOf { epley(it.weightKg ?: 0.0, it.reps ?: 0) } }
    val bests = done.groupBy { it.exerciseId }.count { (ex, ls) -> ls.maxOf { epley(it.weightKg ?: 0.0, it.reps ?: 0) } > (before[ex] ?: 0.0) }
    return Summary(done.size, logs.size, volume, bests)
}

@Composable
private fun SetRow(log: SetLog, prev: SetLog?, live: Boolean, requester: FocusRequester, onToggle: () -> Unit, onWeight: (Double?) -> Unit, onReps: (Double?) -> Unit, onSwap: () -> Unit) {
    val tag = ('A' + log.slot).toString() + (log.setIndex + 1)
    val isDone = log.status == SetStatus.DONE
    val decimals = if (log.stepKg % 1.0 != 0.0) 1 else 0
    Column(Modifier.fillMaxWidth().background(if (isDone) K.Green.copy(alpha = 0.05f) else androidx.compose.ui.graphics.Color.Transparent).padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).then(if (live) Modifier.clickable(onClick = onToggle) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(34.dp).clip(CircleShape).then(if (isDone) Modifier.background(K.Green) else Modifier.border(2.dp, K.Surface3, CircleShape)), contentAlignment = Alignment.Center) {
                    if (isDone) Icon(Icons.Check, contentDescription = "Done", tint = K.GreenInk, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(tag, color = K.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f).then(if (live) Modifier.clickable(onClick = onSwap) else Modifier)) {
                Text(log.exerciseName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("${log.minReps}–${log.maxReps} reps", color = K.Dim, fontSize = 12.sp)
            }
            if (prev != null) Text("${fmt(prev.weightKg, decimals)} × ${prev.reps ?: 0}", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
        }
        if (live) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    NumberField(log.weightKg, onWeight, step = log.stepKg.takeIf { it > 0 } ?: 1.0, unit = "kg", decimals = decimals, height = 44, focusRequester = requester, placeholder = prev?.let { fmt(it.weightKg, decimals) })
                    Delta(prev?.weightKg, log.weightKg, decimals, suffix = " kg")
                }
                Column(Modifier.weight(1f)) {
                    NumberField(log.reps?.toDouble(), { onReps(it) }, step = 1.0, unit = "reps", decimals = 0, height = 44, placeholder = (prev?.reps ?: log.minReps).toString())
                    Delta(prev?.reps?.toDouble(), log.reps?.toDouble(), 0, suffix = " reps")
                }
            }
        } else {
            Text(if (log.status == SetStatus.SKIPPED) "skipped" else "${fmt(log.weightKg, decimals)} kg × ${log.reps ?: 0} reps", color = if (log.status == SetStatus.SKIPPED) K.Dim else K.Text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 42.dp, top = 4.dp))
        }
    }
}

@Composable
private fun Delta(prev: Double?, now: Double?, decimals: Int, suffix: String = "") {
    // Under the widget: the gain since last time, or just the unit on narrow phones where the field hides it.
    val narrow = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 400
    if (prev != null && now != null && now > prev) Text("+${fmt(now - prev, decimals)}$suffix", color = K.Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 40.dp, top = 3.dp))
    else if (narrow) Text(suffix.trim(), color = K.Dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 40.dp, top = 3.dp))
    else Spacer(Modifier.height(0.dp))
}

@Composable
private fun SwapSheet(exercises: List<Exercise>, log: SetLog, onDismiss: () -> Unit, onPick: (Exercise, Boolean) -> Unit) {
    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<Exercise?>(null) }
    Sheet(if (chosen == null) "Swap ${log.exerciseName}" else "Swap to ${chosen!!.name}", onDismiss) {
        val c = chosen
        if (c == null) {
            BasicTextField(query, { query = it }, singleLine = true, textStyle = MaterialTheme.typography.bodyLarge.copy(color = K.Text), cursorBrush = SolidColor(K.Accent),
                modifier = Modifier.fillMaxWidth().clip(GroupShape).background(K.Surface2).padding(14.dp),
                decorationBox = { inner -> Box { if (query.isEmpty()) Text("Search", color = K.Dim); inner() } })
            Spacer(Modifier.height(12.dp))
            ChoiceList {
                exercises.filter { it.id != log.exerciseId && it.name.contains(query, true) }.take(12).forEachIndexed { i, e -> ChoiceRow(e.name, false, divider = i > 0) { chosen = e } }
            }
        } else {
            ChoiceList {
                ChoiceRow("This session only", false, divider = false) { onPick(c, false) }
                ChoiceRow("Update the program too", false) { onPick(c, true) }
            }
        }
    }
}
