package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.mirzohidkhon.khonfitness.ui.LeaveIfSessionMissing
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.map

/** How long a block takes to open or close (ms), ease in and out. */
private const val BLOCK_MS = 250

/**
 * A program session: live logging while unfinished, read-only with an Edit toggle after. Every tap saves. One block is
 * open at a time; a check moves focus on, and after a block's last set the next block with work opens ([afterCheck]).
 * A running session's header sits over the list and folds as the sets scroll ([SessionHeader]).
 */
@Composable
fun SessionScreen(vm: AppViewModel, nav: NavHostController, sessionId: String) {
    val sessionState by remember(sessionId) { vm.repo.session(sessionId).map { it to true } }.collectAsStateWithLifecycle(null to false)
    val session = sessionState.first
    LeaveIfSessionMissing(nav, "session/{id}", sessionId, loaded = sessionState.second, missing = session == null)
    val logs by vm.repo.setLogs(sessionId).collectAsStateWithLifecycle(emptyList())
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    var previous by remember { mutableStateOf<Map<String, SetLog>>(emptyMap()) }
    var editing by remember { mutableStateOf(false) }
    // The open block: the first with work until the user opens or closes one, or a check moves on.
    var picked by rememberSaveable(sessionId) { mutableStateOf(false) }
    var chosen by rememberSaveable(sessionId) { mutableStateOf<Int?>(null) }
    var focusId by remember { mutableStateOf<String?>(null) }
    var swapLog by remember { mutableStateOf<SetLog?>(null) }
    var summary by remember { mutableStateOf<Summary?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // The bar is 56 dp, taller only when a large font makes its title taller; the progress row holds a footnote line.
    var navHeight by remember { mutableStateOf(SESSION_NAV.dp) }
    val rowHeight = with(density) { max(SESSION_ROW.dp, 18.sp.toDp() + 10.dp) }
    val s = session ?: return

    LaunchedEffect(s.id, s.programId) { previous = vm.repo.previousLogsByKey(s.programId, s.id) }
    val live = !s.finished || editing
    val running = !s.finished
    val blocks = remember(logs) { groupBlocks(logs) }
    val expanded = openBlock(blocks, picked, chosen)
    val topPadding = if (running) navHeight + rowHeight else 4.dp
    // What the folded header still hides: the bar and the progress line on its edge.
    val cover = if (running) navHeight + SESSION_LINE.dp else 0.dp
    LaunchedEffect(focusId) {
        val id = focusId ?: return@LaunchedEffect
        delay(60)
        runCatching { requesters[id]?.requestFocus() }
        focusId = null
    }

    // Opens block [i] (null: none). An opened block scrolls to the top of the list, under the header.
    fun open(i: Int?) {
        if (i != null && i != expanded) {
            val offset = with(density) { revealOffset(topPadding.value, cover.value, cardTop(i).value).dp.roundToPx() }
            scope.launch { listState.animateScrollToItem(i, offset) }
        }
        picked = true
        chosen = i
    }
    fun toggleSet(log: SetLog) {
        val next = if (log.status == SetStatus.DONE) SetStatus.PENDING else SetStatus.DONE
        vm.run { vm.repo.updateSetLog(log.copy(status = next)) }
        if (next != SetStatus.DONE) return
        val after = afterCheck(blocks, log)
        open(after.open)
        after.focus?.let { focusId = it.id }
    }
    val done = doneCount(logs)
    val openSummary = { summary = summarize(logs, vm.allSetLogs.value, s.id, s.startedAt, System.currentTimeMillis()) }

    Column(Modifier.fillMaxSize()) {
        if (s.finished) EditorTopBar("History", s.programName ?: "Session", scrollPx = { listState.barOffset() }, onBack = { nav.popBackStack() }, done = if (editing) "Done" else "Edit") { editing = !editing }
        Box(Modifier.weight(1f)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding, bottom = 24.dp)) {
                itemsIndexed(blocks, key = { _, b -> b.first().blockIndex }) { bi, blockLogs ->
                    BlockCard(bi, blockLogs, open = expanded == bi, onToggle = { open(if (expanded == bi) null else bi) }) {
                        blockLogs.forEach { log ->
                            HorizontalDivider(color = K.Divider)
                            SetRow(
                                log = log, prev = previous[log.key()], live = live,
                                requester = requesters.getOrPut(log.id) { FocusRequester() },
                                onToggle = { toggleSet(log) },
                                onWeight = { w -> vm.run { vm.repo.updateSetLog(log.copy(weightKg = w)) } },
                                onReps = { r -> vm.run { vm.repo.updateSetLog(log.copy(reps = r?.toInt())) } },
                                onSwap = { swapLog = log },
                            )
                        }
                    }
                }
                item {
                    // The end of the page: date, effort and note, then the red Discard / Delete. Finish lives in the bar.
                    Column(Modifier.padding(top = 20.dp)) {
                        SessionDetails(s, live) { vm.run { vm.repo.updateSession(it) } }
                        TintedButton(if (s.finished) "Delete session" else "Discard workout", Modifier.padding(top = 20.dp), color = K.Red) { confirmDelete = true }
                    }
                }
            }
            if (running) SessionHeader(
                s.programName ?: "Session", done, logs.size, navHeight, rowHeight,
                onNavHeight = { navHeight = max(SESSION_NAV.dp, it) },
                onBack = { nav.popBackStack() }, onFinish = openSummary, scrollPx = { listState.barOffset() },
            )
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
        // The figures as of opening, then effort and note (the page's own fields, same values) so they are asked for as the workout ends.
        GroupedList(elevated = true) {
            ListRow("Time", secondary = sum.time, chevron = false, divider = false)
            ListRow("Sets done", secondary = "${sum.done} of ${sum.total}", chevron = false)
            ListRow("Volume", secondary = "${fmt(sum.volume, 0)} kg", chevron = false)
            ListRow("New bests", secondary = sum.bests.toString(), chevron = false)
        }
        GroupedList(Modifier.padding(top = 12.dp), elevated = true) {
            FieldRow("Effort", divider = false) { EffortField(s.effort) { v -> vm.run { vm.repo.updateSession(s.copy(effort = v)) } } }
            FieldRow("Note") { InlineTextField(s.note, { vm.run { vm.repo.updateSession(s.copy(note = it)) } }, placeholder = "Optional") }
        }
        PrimaryButton("Save", Modifier.padding(top = 16.dp)) { vm.run { vm.repo.finishSession(s.id) }; summary = null; nav.popBackStack() }
    } }
}

/** Room above a block card inside its list item: 6 dp for the first, 10 between cards. */
private fun cardTop(index: Int): Dp = if (index == 0) 6.dp else 10.dp

/** Effort 1 to 10 as a number widget: steps and typing stay inside the range. [elevated] inside a sheet. */
@Composable
private fun EffortField(effort: Int?, elevated: Boolean = true, onChange: (Int?) -> Unit) {
    NumberField(effort?.toDouble(), { v -> onChange(v?.toInt()) }, 1.0, "/10", Modifier.width(176.dp), 0, 38, placeholder = "1–10", min = 1.0, max = 10.0, elevated = elevated)
}

/** Date, effort and note of a session: editable while [live], else only what was filled in. */
@Composable
private fun SessionDetails(s: Session, live: Boolean, onChange: (Session) -> Unit) {
    if (live) {
        GroupedList {
            DateRow(s.date.toDate(), divider = false) { d -> onChange(s.copy(date = d.iso())) }
            FieldRow("Effort") { EffortField(s.effort, elevated = false) { v -> onChange(s.copy(effort = v)) } }
            FieldRow("Note") { InlineTextField(s.note, { onChange(s.copy(note = it)) }, placeholder = "Optional") }
        }
    } else if (s.note.isNotBlank() || s.effort != null) {
        GroupedList {
            if (s.effort != null) FieldRow("Effort", divider = false) { Text("${s.effort} / 10") }
            if (s.note.isNotBlank()) FieldRow("Note", divider = s.effort != null) { Text(s.note, textAlign = androidx.compose.ui.text.style.TextAlign.End) }
        }
    }
}

/**
 * Top of a running session, over its list: Back, the program name and Finish in the bar; under it the progress bar
 * with sets done of total. As the sets scroll, the progress row slides up under the bar with them and fades, and a
 * 2 dp progress line fades in on the bar's bottom edge, so Finish and the progress stay in view. The scroll offset is
 * read in the draw layers only.
 */
@Composable
private fun SessionHeader(
    title: String, done: Int, total: Int, nav: Dp, row: Dp,
    onNavHeight: (Dp) -> Unit, onBack: () -> Unit, onFinish: () -> Unit, scrollPx: () -> Int,
) {
    val label = "$done of $total sets done"
    val local = LocalDensity.current
    Box(Modifier.fillMaxWidth()) {
        // Drawn before the bar, so it slides under it. Taps pass through to the list.
        Row(
            Modifier.padding(top = nav).fillMaxWidth().height(row)
                .graphicsLayer { val y = scrollPx() / density; translationY = -sessionRowShift(y, row.value).dp.toPx(); alpha = sessionRowAlpha(y, row.value) }
                .background(K.Bg).padding(start = 16.dp, end = 16.dp, bottom = row - 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProgressBar(done, total, label, Modifier.weight(1f))
            Text("$done / $total", style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR), color = K.Muted, modifier = Modifier.clearAndSetSemantics {})
        }
        // The bar takes its taps, so nothing under it is hit by mistake.
        Box(
            Modifier.fillMaxWidth().background(K.Bg).onSizeChanged { onNavHeight(with(local) { it.height.toDp() }) }
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        ) { EditorTopBar("Today", title, onBack, done = "Finish", onDone = onFinish) }
        Box(Modifier.padding(top = nav).fillMaxWidth().graphicsLayer { alpha = sessionLineAlpha(scrollPx() / density, row.value) }.clearAndSetSemantics {}) {
            ProgressBar(done, total, label, thin = true)
        }
    }
}

/**
 * One block of a session: a 52 dp header with the block number, the exercise names, sets done of total and a chevron
 * (a green check once every set is logged or skipped), the set rows under it while open. Opening and closing take
 * [BLOCK_MS] ease in and out: the card grows with its sets revealed from the top and fading in, the chevron turns down.
 */
@Composable
private fun BlockCard(index: Int, logs: List<SetLog>, open: Boolean, onToggle: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val title = blockTitle(logs)
    val doneSets = doneCount(logs)
    val blockDone = isBlockDone(logs)
    val turn by animateFloatAsState(if (open) 90f else 0f, tween(BLOCK_MS, easing = EaseInOut), label = "chevron")
    Column(Modifier.fillMaxWidth().padding(top = cardTop(index)).clip(GroupShape).background(K.Surface)) {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onToggle)
                .clearAndSetSemantics {
                    contentDescription = blockLabel(index, title, doneSets, logs.size, blockDone)
                    stateDescription = if (open) "Expanded" else "Collapsed"
                    role = Role.Button
                    onClick { onToggle(); true }
                }
                .heightIn(min = 52.dp).padding(start = 16.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IndexBadge(index)
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 12.dp))
            Text("$doneSets / ${logs.size}", color = K.Muted, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TABULAR))
            Box(Modifier.padding(start = 10.dp).widthIn(min = 18.dp), contentAlignment = Alignment.Center) {
                if (blockDone) Icon(Icons.Check, contentDescription = null, tint = K.Green, modifier = Modifier.size(18.dp))
                else Icon(Icons.ChevronRight, contentDescription = null, tint = K.Dim, modifier = Modifier.size(14.dp).rotate(turn))
            }
        }
        AnimatedVisibility(
            open,
            enter = expandVertically(tween(BLOCK_MS, easing = EaseInOut), expandFrom = Alignment.Top) + fadeIn(tween(BLOCK_MS, easing = EaseInOut)),
            exit = shrinkVertically(tween(BLOCK_MS, easing = EaseInOut), shrinkTowards = Alignment.Top) + fadeOut(tween(BLOCK_MS, easing = EaseInOut)),
        ) { Column(Modifier.fillMaxWidth(), content = content) }
    }
}

@Composable
private fun SetRow(log: SetLog, prev: SetLog?, live: Boolean, requester: FocusRequester, onToggle: () -> Unit, onWeight: (Double?) -> Unit, onReps: (Double?) -> Unit, onSwap: () -> Unit) {
    val tag = ('A' + log.slot).toString() + (log.setIndex + 1)
    val isDone = log.status == SetStatus.DONE
    // At least one decimal: a 62.5 kg set can be typed on a whole-kg step, as in 0.4.8.
    val decimals = maxOf(1, stepDecimals(log.stepKg))
    // Rows saved before sets started at 0 kg may hold no weight; they read as 0, and a first set starts there.
    val prevKg = fmt(prev?.weightKg ?: 0.0, decimals)
    Column(Modifier.fillMaxWidth().background(if (isDone) K.Green.copy(alpha = 0.05f) else androidx.compose.ui.graphics.Color.Transparent).padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .toggleable(isDone, enabled = live, role = Role.Checkbox) { onToggle() }
                    .semantics { contentDescription = "$tag ${log.exerciseName}" },
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
            if (prev != null) Text("$prevKg × ${prev.reps ?: 0}", color = K.Muted, style = MaterialTheme.typography.bodyMedium)
        }
        if (live) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    NumberField(log.weightKg, onWeight, step = log.stepKg.takeIf { it > 0 } ?: 1.0, unit = "kg", decimals = decimals, height = 44, focusRequester = requester, placeholder = prevKg)
                    Delta(prev?.weightKg, log.weightKg, decimals, suffix = " kg")
                }
                Column(Modifier.weight(1f)) {
                    NumberField(log.reps?.toDouble(), { onReps(it) }, step = 1.0, unit = "reps", decimals = 0, height = 44, placeholder = (prev?.reps ?: log.minReps).toString())
                    Delta(prev?.reps?.toDouble(), log.reps?.toDouble(), 0, suffix = " reps")
                }
            }
        } else {
            Text(if (log.status == SetStatus.SKIPPED) "skipped" else "${fmt(log.weightKg ?: 0.0, decimals)} kg × ${log.reps ?: 0} reps", color = if (log.status == SetStatus.SKIPPED) K.Dim else K.Text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 42.dp, top = 4.dp))
        }
    }
}

@Composable
private fun Delta(prev: Double?, now: Double?, decimals: Int, suffix: String = "") {
    // Under the widget: the gain since last time, or just the unit on narrow phones where the field hides it.
    // Two fields share a row on every phone width, so the field itself is compact and the unit lives here.
    val narrow = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 480
    if (prev != null && now != null && now > prev) Text("+${fmt(now - prev, decimals)}$suffix", color = K.Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 40.dp, top = 3.dp))
    else if (narrow) Text(suffix.trim(), color = K.Dim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 40.dp, top = 3.dp))
    else Spacer(Modifier.height(0.dp))
}

/** Swap an exercise: search and pick a replacement (the list stays short, 12), then the scope, this session only or the program too. */
@Composable
private fun SwapSheet(exercises: List<Exercise>, log: SetLog, onDismiss: () -> Unit, onPick: (Exercise, Boolean) -> Unit) {
    var query by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<Exercise?>(null) }
    Sheet(if (chosen == null) "Swap ${log.exerciseName}" else "Swap to ${chosen!!.name}", onDismiss) {
        val c = chosen
        if (c == null) {
            val found = exercises.filter { it.id != log.exerciseId && matchesQuery(it.name, query) }.take(12)
            SearchField(query, { query = it }, Modifier.padding(bottom = 12.dp), elevated = true)
            ChoiceList {
                if (found.isEmpty()) SearchEmpty(query)
                found.forEachIndexed { i, e -> ChoiceRow(e.name, false, divider = i > 0) { chosen = e } }
            }
        } else {
            ChoiceList {
                ChoiceRow("This session only", false, divider = false) { onPick(c, false) }
                ChoiceRow("Update the program too", false) { onPick(c, true) }
            }
        }
    }
}
