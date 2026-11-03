package dev.mirzohidkhon.khonfitness.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.mirzohidkhon.khonfitness.data.*
import dev.mirzohidkhon.khonfitness.ui.AppViewModel
import dev.mirzohidkhon.khonfitness.ui.Routes
import dev.mirzohidkhon.khonfitness.ui.Icons
import dev.mirzohidkhon.khonfitness.timer.TimerService
import dev.mirzohidkhon.khonfitness.ui.components.*
import dev.mirzohidkhon.khonfitness.ui.theme.K
import java.time.LocalDate

/**
 * Now, read again at each minute boundary and on each resume, so "34 min", "closes in" and the date stay right while
 * the screen is open, also across midnight while the app was in the background.
 */
@Composable
fun rememberMinuteClock(): Long {
    val clock = LocalWallClock.current
    var now by remember { mutableLongStateOf(clock()) }
    LaunchedEffect(clock) { while (true) { kotlinx.coroutines.delay(60_000 - clock() % 60_000); now = clock() } }
    androidx.lifecycle.compose.LifecycleResumeEffect(clock) { now = clock(); onPauseOrDispose { } }
    return now
}

/** The wall clock the minute clock reads; a test sets its own to cross midnight. */
val LocalWallClock = staticCompositionLocalOf<() -> Long> { System::currentTimeMillis }

/**
 * Today: today's item with Change, Start, Resume or Review, one group of rows (a newer release to install, the
 * stretch routine, the eating window, band sessions waiting for import), and the bodyweight card.
 */
@Composable
fun TodayScreen(vm: AppViewModel, nav: NavHostController) {
    // The plan as read from the database: null until it is, so a first-use card never flashes over a real plan.
    val planRead by remember { vm.repo.planData }.collectAsStateWithLifecycle(null)
    val plan = planRead ?: KhonRepository.PlanData(emptyList(), emptyList(), emptyList(), emptyList())
    val modalities by vm.modalities.collectAsStateWithLifecycle()
    val exercises by vm.exercises.collectAsStateWithLifecycle()
    val blocks by vm.blocks.collectAsStateWithLifecycle()
    val blockExercises by vm.blockExercises.collectAsStateWithLifecycle()
    val bodyweights by vm.bodyweights.collectAsStateWithLifecycle()
    val unfinished by vm.unfinished.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val logs by vm.allSetLogs.collectAsStateWithLifecycle()
    val timer by TimerService.state.collectAsStateWithLifecycle()
    val now = rememberMinuteClock()
    val today = todayOf(now)
    val item = plan.itemFor(today)
    val loaded = planRead != null
    // A new day starts with the sheet closed.
    var changeSheet by remember(today) { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingBand by remember { mutableIntStateOf(0) }
    val discard = rememberConfirm<Session>()
    fun start() = vm.startItem(item) { s -> nav.navigate(when { s.itemType == ItemType.PROGRAM -> Routes.session(s.id); item.cardio?.intervals == true -> Routes.timer(s.id); else -> Routes.cardio(s.id) }) }
    fun resume(active: Session) = nav.navigate(if (active.itemType == ItemType.PROGRAM) Routes.session(active.id) else if (TimerService.state.value?.sessionId == active.id) Routes.timer(active.id) else Routes.cardio(active.id))
    val startRequested by dev.mirzohidkhon.khonfitness.MainActivity.startRequested.collectAsStateWithLifecycle()
    LaunchedEffect(startRequested, unfinished, item.name, loaded) {
        if (!startRequested || !loaded) return@LaunchedEffect
        dev.mirzohidkhon.khonfitness.MainActivity.startRequested.value = false
        val active = unfinished
        if (active != null) resume(active) else if (!item.isRest) start()
    }
    LaunchedEffect(item.name, unfinished?.id) { dev.mirzohidkhon.khonfitness.widget.TodayWidget.refresh(context) }
    val stretchRoutines by vm.stretchRoutines.collectAsStateWithLifecycle()
    val routineStretches by vm.routineStretches.collectAsStateWithLifecycle()
    val stretches by vm.stretches.collectAsStateWithLifecycle()
    val stretchSessions by vm.stretchSessions.collectAsStateWithLifecycle()
    val eatingWindow by vm.eatingWindow.collectAsStateWithLifecycle()
    val windowDays by vm.windowDays.collectAsStateWithLifecycle()
    var windowSheet by remember { mutableStateOf(false) }
    val windowRequested by dev.mirzohidkhon.khonfitness.MainActivity.windowRequested.collectAsStateWithLifecycle()
    LaunchedEffect(windowRequested) { if (windowRequested) { dev.mirzohidkhon.khonfitness.MainActivity.windowRequested.value = false; windowSheet = true } }
    val update by vm.update.collectAsStateWithLifecycle()
    val updateProgress by vm.updateProgress.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.checkUpdate() }
    val notifLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(Unit) { pendingBand = runCatching { dev.mirzohidkhon.khonfitness.health.HealthImport.pendingCount(context, vm.repo.importedSourceIds().toSet()) }.getOrDefault(0) }

    TabScreen("Today", trailing = { BarCircleButton(Icons.Gear, "Settings") { nav.navigate(Routes.SETTINGS) } }) {
        if (loaded) {
            val active = unfinished
            val done = doneToday(sessions, today)
            TodayCard(
                item, todayLine(item, blocks, blockExercises, modalities), itemColor(item, modalities)?.first ?: K.Muted,
                unfinished = active,
                running = active?.let { runningLine(it, logs, timer, now) }.orEmpty(),
                progress = active?.takeIf { it.itemType == ItemType.PROGRAM }?.let { setProgress(it.id, logs) },
                elapsed = active?.let { workoutTime(it.startedAt, now) }.orEmpty(),
                runningColor = active?.let { sessionColor(it, exercises, modalities).first } ?: K.Accent,
                done = done.session, doneText = done.session?.let { doneLine(it, logs) }, more = done.more,
                noPlan = hasNoPlan(plan),
                onStart = ::start, onResume = ::resume, onChange = { changeSheet = true },
                timerStopped = active != null && timerStopped(active, timer?.sessionId), onDiscard = { discard.ask(it) },
                onReview = { s -> nav.navigate(Routes.review(s.id)) },
                // History opens fresh, on this week.
                onMore = { nav.navigate(Routes.HISTORY) { popUpTo(Routes.TODAY) { saveState = true }; launchSingleTop = true } },
                onSetUp = { nav.navigate(Routes.PROGRAMS) { popUpTo(Routes.TODAY) { saveState = true }; launchSingleTop = true; restoreState = true } },
            )
        }
        Spacer(Modifier.height(16.dp))
        GroupedList {
            val updateRow = updateRow(update, updateProgress)
            if (updateRow != null) HomeRow(Icons.Download, K.Accent, updateRow.title, updateRow.line, K.Accent, divider = false) {
                // One download at a time: a tap while it runs does nothing.
                if (!updateRow.downloading) vm.runUpdate(context, onNeedPermission = { nav.navigate(Routes.SETTINGS) }, onError = { })
            }
            val routine = stretchRoutines.find { it.active } ?: stretchRoutines.firstOrNull()
            if (routine != null) {
                val stretchedToday = stretchSessions.any { it.date == today.iso() }
                val summary = routineSummary(routineSeconds(routine.id, routineStretches, stretches), routineStretches.count { it.routineId == routine.id })
                HomeRow(
                    Icons.Stretch, K.Green, routine.name, routineLine(summary, stretchedToday), if (stretchedToday) K.Green else K.Muted, divider = updateRow != null,
                    trailing = if (stretchedToday) { { DoneCheck() } } else null,
                ) { nav.navigate(Routes.stretch(routine.id)) }
            }
            val windowDay = windowDays.find { it.date == today.iso() }
            val clock = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), java.time.ZoneId.systemDefault())
            val open = windowSummary(eatingWindow, windowDay, clock).first
            HomeRow(Icons.ForkKnife, if (open) K.Green else K.Dim, "Eating window", windowStatus(eatingWindow, windowDay, clock), if (open) K.Green else K.Muted, divider = updateRow != null || routine != null) { windowSheet = true }
            if (pendingBand > 0) HomeRow(Icons.Heart, K.Red, pendingTitle(pendingBand), PENDING_LINE, divider = true) { nav.navigate(Routes.IMPORT) }
        }
        BodyweightCard(today, bodyweights) { kg -> vm.run { vm.repo.saveBodyweight(today.iso(), kg) } }
    }
    // A session that starts or saves while the sheet is open closes it for good: Change only applies before today's session.
    val canChange = loaded && unfinished == null && doneToday(sessions, today).session == null && !hasNoPlan(plan)
    LaunchedEffect(canChange) { if (!canChange) changeSheet = false }
    if (changeSheet && canChange) ChangeSheet(today, plan, modalities, onPick = { pick ->
        vm.run { when (pick) { is ChangePick.Override -> vm.repo.saveOverride(today.iso(), pick.itemType, pick.itemId); ChangePick.BackToPlan -> vm.repo.clearOverride(today.iso()) } }
        changeSheet = false
    }) { changeSheet = false }
    if (windowSheet) WindowSheet(vm) { windowSheet = false }
    ConfirmSheet(discard, title = { "Discard this session?" }, body = { "The timer's time and rounds are not kept." }, action = "Discard") { s -> vm.run { vm.repo.deleteSession(s.id) } }
}

/**
 * Change today only: "Today, Tue Nov 3", a line that the week plan stays, then Gym, Cardio and Other (Rest) with a
 * check on today's item. A pick is a one-day change ([changePick]); the week plan's own item clears it. While a change
 * holds, "Back to plan: <item>" clears it too.
 */
@Composable
fun ChangeSheet(today: LocalDate, plan: KhonRepository.PlanData, modalities: List<Modality>, onPick: (ChangePick) -> Unit, onDismiss: () -> Unit) {
    val current = plan.itemFor(today)
    val cardio = plan.exercises.filter { it.kind == Kind.CARDIO && !it.archived }
    fun pick(type: String, id: String?) = onPick(changePick(plan, today, type, id))
    Sheet(changeTitle(today), onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Only today changes. The week plan stays.", style = MaterialTheme.typography.bodyMedium, color = K.Muted, modifier = Modifier.padding(bottom = 4.dp))
            if (plan.programs.isNotEmpty()) {
                SheetGroupTitle("Gym")
                ChoiceList { plan.programs.forEachIndexed { i, p -> ChoiceRow(p.name, current.program?.id == p.id, K.Accent, divider = i > 0) { pick(ItemType.PROGRAM, p.id) } } }
            }
            if (cardio.isNotEmpty()) {
                SheetGroupTitle("Cardio")
                ChoiceList { cardio.forEachIndexed { i, e -> ChoiceRow(e.name, current.cardio?.id == e.id, exerciseColor(e, modalities), e.intensity == Intensity.HIGH, divider = i > 0) { pick(ItemType.CARDIO, e.id) } } }
            }
            SheetGroupTitle("Other")
            ChoiceList { ChoiceRow("Rest", current.isRest, K.Dim, divider = false) { pick(ItemType.REST, null) } }
            if (current.overridden) PrimaryButton("Back to plan: ${weekItem(plan, today).name}", Modifier.padding(top = 16.dp), color = K.Accent.copy(alpha = 0.2f), textColor = K.Accent) { onPick(ChangePick.BackToPlan) }
        }
    }
}

/** Change what a date holds: a program, a cardio exercise, or rest. */
@Composable
fun PlanSheet(vm: AppViewModel, date: LocalDate, plan: KhonRepository.PlanData, modalities: List<Modality>, onDismiss: () -> Unit) {
    val current = plan.itemFor(date)
    val cardio = plan.exercises.filter { it.kind == Kind.CARDIO && !it.archived }
    fun pick(type: String, id: String?) { vm.run { vm.repo.saveOverride(date.iso(), type, id) }; onDismiss() }
    Sheet(date.format(longDate), onDismiss) {
        SheetGroupTitle("Programs")
        ChoiceList { plan.programs.forEachIndexed { i, p -> ChoiceRow(p.name, current.program?.id == p.id, K.Accent, divider = i > 0) { pick(ItemType.PROGRAM, p.id) } } }
        SheetGroupTitle("Cardio exercises")
        ChoiceList { cardio.forEachIndexed { i, e -> ChoiceRow(e.name, current.cardio?.id == e.id, exerciseColor(e, modalities), e.intensity == Intensity.HIGH, divider = i > 0) { pick(ItemType.CARDIO, e.id) } } }
        SheetGroupTitle("Rest")
        ChoiceList { ChoiceRow("Rest", current.isRest, K.Dim, divider = false) { pick(ItemType.REST, null) } }
        if (current.overridden) Row(Modifier.padding(top = 12.dp)) { TextButton("Back to weekly template") { vm.run { vm.repo.clearOverride(date.iso()) }; onDismiss() } }
    }
}
